package hk.ust.csit5930.search.crawler;

import hk.ust.csit5930.search.common.JDBMManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages parent-child link relationships between pages.
 * Supports bidirectional queries:
 * - Given a parent PageID, retrieve all child PageIDs
 * - Given a child PageID, retrieve all parent PageIDs
 * All link data is persisted to JDBM (no in-memory storage).
 */
public class LinkGraphManager {

    private JDBMManager dbManager;

    /**
     * Constructs a LinkGraphManager.
     *
     * @throws IOException if database access fails
     */
    public LinkGraphManager() throws IOException {
        this.dbManager = JDBMManager.getInstance();
    }

    /**
     * Adds a parent-child link relationship between two pages.
     * Updates both forward (parent->child) and backward (child->parent) indexes.
     * Prevents duplicate link entries.
     *
     * @param parentPageId the PageID of the source page
     * @param childPageId  the PageID of the target page
     * @throws IOException if database operation fails
     */
    @SuppressWarnings("unchecked")
    public void addLink(int parentPageId, int childPageId) throws IOException {
        // Update parent -> children mapping (forward index)
        ArrayList<Integer> children = (ArrayList<Integer>)
            dbManager.getChildLinks().get(parentPageId);
        if (children == null) {
            children = new ArrayList<>();
        }
        if (!children.contains(childPageId)) {
            children.add(childPageId);
            dbManager.getChildLinks().put(parentPageId, children);
        }

        // Update child -> parents mapping (backward index)
        ArrayList<Integer> parents = (ArrayList<Integer>)
            dbManager.getParentLinks().get(childPageId);
        if (parents == null) {
            parents = new ArrayList<>();
        }
        if (!parents.contains(parentPageId)) {
            parents.add(parentPageId);
            dbManager.getParentLinks().put(childPageId, parents);
        }
    }

    /**
     * Returns all child page IDs linked from a parent page.
     *
     * @param parentPageId the parent PageID
     * @return list of child PageIDs, empty list if none
     */
    @SuppressWarnings("unchecked")
    public List<Integer> getChildPageIds(int parentPageId) throws IOException {
        ArrayList<Integer> children = (ArrayList<Integer>)
            dbManager.getChildLinks().get(parentPageId);
        return (children != null) ? children : new ArrayList<>();
    }

    /**
     * Returns all parent page IDs that link to a child page.
     *
     * @param childPageId the child PageID
     * @return list of parent PageIDs, empty list if none
     */
    @SuppressWarnings("unchecked")
    public List<Integer> getParentPageIds(int childPageId) throws IOException {
        ArrayList<Integer> parents = (ArrayList<Integer>)
            dbManager.getParentLinks().get(childPageId);
        return (parents != null) ? parents : new ArrayList<>();
    }
}
