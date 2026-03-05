package hk.ust.csit5930.search.crawler;

import hk.ust.csit5930.search.common.JDBMManager;
import hk.ust.csit5930.search.indexer.model.PageMetadata;

import java.io.IOException;
import java.util.Set;

/**
 * Validates URLs before crawling to enforce the three mandatory pre-crawl checks:
 * 1. Circular link protection (skip already-processed URLs)
 * 2. Index existence check (allow new URLs)
 * 3. Page update check (re-crawl if page modified since last index)
 */
public class URLValidator {

    /** Set of already-processed URLs (maintained by Spider) */
    private Set<String> processedUrls;

    /** JDBM manager for index lookup */
    private JDBMManager dbManager;

    /**
     * Constructs a URLValidator with a reference to the set of processed URLs.
     *
     * @param processedUrls the set of already-processed URLs for circular link detection
     * @throws IOException if database access fails
     */
    public URLValidator(Set<String> processedUrls) throws IOException {
        this.processedUrls = processedUrls;
        this.dbManager = JDBMManager.getInstance();
    }

    /**
     * Determines whether a URL needs to be fetched based on three mandatory checks:
     * Check 1: Circular link protection - skip if already processed in this crawl session
     * Check 2: Index existence check - allow if URL is not yet in the index
     * Check 3: Page update check - allow if the page has been modified since last index time
     *
     * @param url the URL to validate
     * @return true if the URL should be fetched
     * @throws Exception if validation fails
     */
    public boolean needFetch(String url) throws Exception {
        // Check 1: Circular link protection - skip if already processed
        if (processedUrls.contains(url)) {
            return false;
        }

        // Check 2: Index existence check - allow if not in index
        Integer pageId = (Integer) dbManager.getUrl2Id().get(url);
        if (pageId == null) {
            return true; // URL not in index, should be fetched
        }

        // Check 3: Page update check - check if page has been modified since last index
        PageMetadata metadata = (PageMetadata) dbManager.getPageMetadata().get(pageId);
        if (metadata == null) {
            return true; // No metadata stored, should be fetched
        }

        // Allow re-crawl so Spider can compare HTTP Last-Modified with stored timestamp
        return true;
    }

    /**
     * Checks if a fetched page needs to be re-indexed based on its last modified time.
     * Called after fetching the page headers to compare timestamps.
     *
     * @param url          the page URL
     * @param lastModified the HTTP Last-Modified timestamp from the response
     * @return true if the page should be re-indexed
     * @throws IOException if database access fails
     */
    public boolean needReindex(String url, long lastModified) throws IOException {
        Integer pageId = (Integer) dbManager.getUrl2Id().get(url);
        if (pageId == null) {
            return true; // Not indexed yet, needs indexing
        }

        PageMetadata metadata = (PageMetadata) dbManager.getPageMetadata().get(pageId);
        if (metadata == null) {
            return true; // No metadata, needs indexing
        }

        // Re-index if the page has been modified since last index time
        return lastModified > metadata.getLastModified();
    }
}
