package search.crawler;

import search.common.JDBMManager;

import java.io.IOException;

/**
 * Manages bidirectional mapping between URLs and unique integer PageIDs.
 * All mappings are persisted to JDBM for disk-based storage.
 * Each unique URL is assigned exactly one PageID (monotonically increasing).
 */
public class PageIDMapper {

    private JDBMManager dbManager;
    private int nextPageId;

    /**
     * Constructs a PageIDMapper and loads the next available PageID from JDBM.
     *
     * @throws IOException if database access fails
     */
    public PageIDMapper() throws IOException {
        this.dbManager = JDBMManager.getInstance();
        // Load next page ID counter from persistent storage
        String nextIdStr = (String) dbManager.getSystemConfig().get("nextPageId");
        this.nextPageId = (nextIdStr != null) ? Integer.parseInt(nextIdStr) : 0;
    }

    /**
     * Gets the PageID for a URL. If the URL has not been seen before,
     * assigns a new unique PageID and persists the mapping.
     *
     * @param url the URL to look up or register
     * @return the PageID assigned to this URL
     * @throws IOException if database access fails
     */
    public synchronized int getOrCreatePageId(String url) throws IOException {
        Integer existingId = (Integer) dbManager.getUrl2Id().get(url);
        if (existingId != null) {
            return existingId;
        }

        // Assign new PageID and persist both directions
        int pageId = nextPageId++;
        dbManager.getUrl2Id().put(url, pageId);
        dbManager.getId2Url().put(pageId, url);

        // Persist the counter for recovery
        dbManager.getSystemConfig().put("nextPageId", String.valueOf(nextPageId));
        return pageId;
    }

    /**
     * Gets the PageID for a URL, or -1 if not registered.
     *
     * @param url the URL to look up
     * @return the PageID, or -1 if not found
     * @throws IOException if database access fails
     */
    public int getPageId(String url) throws IOException {
        Integer id = (Integer) dbManager.getUrl2Id().get(url);
        return (id != null) ? id : -1;
    }

    /**
     * Gets the URL for a PageID, or null if not registered.
     *
     * @param pageId the PageID to look up
     * @return the URL, or null if not found
     * @throws IOException if database access fails
     */
    public String getUrl(int pageId) throws IOException {
        return (String) dbManager.getId2Url().get(pageId);
    }

    /**
     * Checks if a URL has already been assigned a PageID.
     *
     * @param url the URL to check
     * @return true if the URL is registered
     * @throws IOException if database access fails
     */
    public boolean hasUrl(String url) throws IOException {
        return dbManager.getUrl2Id().get(url) != null;
    }

    /**
     * Returns the total number of registered pages.
     *
     * @return the count of registered pages
     */
    public int getTotalPages() {
        return nextPageId;
    }
}
