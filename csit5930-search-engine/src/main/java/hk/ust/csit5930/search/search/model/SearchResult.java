package hk.ust.csit5930.search.search.model;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Represents a single search result with all fields required for display.
 * Fields are ordered according to the assignment specification:
 * score, title (hyperlink), URL, last modified/size, keywords, parent links, child links.
 */
public class SearchResult implements Comparable<SearchResult> {

    /** The document PageID */
    private int pageId;

    /** The similarity score (6 decimal places for display) */
    private double score;

    /** The page title */
    private String title;

    /** The page URL */
    private String url;

    /** The last modified timestamp (epoch millis) */
    private long lastModified;

    /** The page size in characters */
    private long pageSize;

    /** Top keywords with frequencies (stem -> frequency), ordered by frequency desc */
    private LinkedHashMap<String, Integer> topKeywords;

    /** List of parent page URLs */
    private List<String> parentLinks;

    /** List of child page URLs */
    private List<String> childLinks;

    /**
     * Constructs a SearchResult with all required display fields.
     *
     * @param pageId       the document PageID
     * @param score        the similarity score
     * @param title        the page title
     * @param url          the page URL
     * @param lastModified the last modified timestamp
     * @param pageSize     the page size
     * @param topKeywords  the top keywords map
     * @param parentLinks  list of parent URLs
     * @param childLinks   list of child URLs
     */
    public SearchResult(int pageId, double score, String title, String url,
                        long lastModified, long pageSize,
                        LinkedHashMap<String, Integer> topKeywords,
                        List<String> parentLinks, List<String> childLinks) {
        this.pageId = pageId;
        this.score = score;
        this.title = title;
        this.url = url;
        this.lastModified = lastModified;
        this.pageSize = pageSize;
        this.topKeywords = topKeywords;
        this.parentLinks = parentLinks;
        this.childLinks = childLinks;
    }

    public int getPageId() { return pageId; }
    public double getScore() { return score; }
    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public long getLastModified() { return lastModified; }
    public long getPageSize() { return pageSize; }
    public LinkedHashMap<String, Integer> getTopKeywords() { return topKeywords; }
    public List<String> getParentLinks() { return parentLinks; }
    public List<String> getChildLinks() { return childLinks; }

    /**
     * Compares by score in descending order for ranking.
     */
    @Override
    public int compareTo(SearchResult other) {
        return Double.compare(other.score, this.score);
    }

    @Override
    public String toString() {
        return String.format("SearchResult{score=%.6f, title='%s', url='%s'}", score, title, url);
    }
}
