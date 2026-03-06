package search.crawler.model;

import java.io.Serializable;
import java.util.List;

/**
 * Data model representing a crawled web page with all extracted metadata.
 * Encapsulates the standardized page data passed from the crawler to the indexer.
 */
public class CrawledPage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** The page URL */
    private String url;

    /** The page title extracted from the HTML title tag */
    private String title;

    /** The plain text body content of the page */
    private String body;

    /** The last modified timestamp in epoch milliseconds */
    private long lastModified;

    /** The page size in bytes or characters */
    private long pageSize;

    /** List of outgoing (child) URLs found on this page */
    private List<String> childLinks;

    /**
     * Constructs a CrawledPage with all required metadata.
     *
     * @param url          the page URL
     * @param title        the page title extracted from HTML
     * @param body         the plain text body content
     * @param lastModified the last modified timestamp (epoch millis)
     * @param pageSize     the page size in bytes/characters
     * @param childLinks   list of outgoing URLs found on this page
     */
    public CrawledPage(String url, String title, String body,
                       long lastModified, long pageSize, List<String> childLinks) {
        this.url = url;
        this.title = title;
        this.body = body;
        this.lastModified = lastModified;
        this.pageSize = pageSize;
        this.childLinks = childLinks;
    }

    public String getUrl() { return url; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public long getLastModified() { return lastModified; }
    public long getPageSize() { return pageSize; }
    public List<String> getChildLinks() { return childLinks; }

    @Override
    public String toString() {
        return "CrawledPage{url='" + url + "', title='" + title + "', size=" + pageSize + "}";
    }
}
