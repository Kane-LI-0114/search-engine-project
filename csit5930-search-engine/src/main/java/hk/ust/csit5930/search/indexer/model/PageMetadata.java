package hk.ust.csit5930.search.indexer.model;

import java.io.Serializable;
import java.util.LinkedHashMap;

/**
 * Stores metadata for a crawled and indexed page.
 * Used for search result display, including top keywords, timestamps, and page info.
 * Persisted to JDBM for disk-based storage.
 */
public class PageMetadata implements Serializable {

    private static final long serialVersionUID = 1L;

    /** The unique PageID */
    private int pageId;

    /** The page URL */
    private String url;

    /** The page title */
    private String title;

    /** The last modified timestamp (epoch millis) */
    private long lastModified;

    /** The page size in characters */
    private long pageSize;

    /** Top keywords ordered by frequency descending (stem -> frequency) */
    private LinkedHashMap<String, Integer> topKeywords;

    /**
     * Constructs PageMetadata with all required fields.
     *
     * @param pageId       the unique PageID
     * @param url          the page URL
     * @param title        the page title
     * @param lastModified the last modified timestamp (epoch millis)
     * @param pageSize     the page size in characters
     * @param topKeywords  the top keywords (stem -> frequency), ordered by frequency descending
     */
    public PageMetadata(int pageId, String url, String title,
                        long lastModified, long pageSize,
                        LinkedHashMap<String, Integer> topKeywords) {
        this.pageId = pageId;
        this.url = url;
        this.title = title;
        this.lastModified = lastModified;
        this.pageSize = pageSize;
        this.topKeywords = topKeywords;
    }

    public int getPageId() { return pageId; }
    public String getUrl() { return url; }
    public String getTitle() { return title; }
    public long getLastModified() { return lastModified; }
    public long getPageSize() { return pageSize; }
    public LinkedHashMap<String, Integer> getTopKeywords() { return topKeywords; }

    @Override
    public String toString() {
        return "PageMetadata{pageId=" + pageId + ", url='" + url + "', title='" + title + "'}";
    }
}
