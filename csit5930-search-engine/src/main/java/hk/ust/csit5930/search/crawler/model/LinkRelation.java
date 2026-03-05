package hk.ust.csit5930.search.crawler.model;

import java.io.Serializable;

/**
 * Data model representing a parent-child link relationship between two pages.
 */
public class LinkRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    /** The PageID of the source (parent) page */
    private int parentPageId;

    /** The PageID of the target (child) page */
    private int childPageId;

    /**
     * Constructs a LinkRelation between a parent and child page.
     *
     * @param parentPageId the PageID of the source (parent) page
     * @param childPageId  the PageID of the target (child) page
     */
    public LinkRelation(int parentPageId, int childPageId) {
        this.parentPageId = parentPageId;
        this.childPageId = childPageId;
    }

    public int getParentPageId() { return parentPageId; }
    public int getChildPageId() { return childPageId; }

    @Override
    public String toString() {
        return "LinkRelation{parent=" + parentPageId + ", child=" + childPageId + "}";
    }
}
