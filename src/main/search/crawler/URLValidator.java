package search.crawler;

import search.common.JDBMManager;
import search.indexer.model.PageMetadata;

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
    /**
     * Determines whether a URL needs to be re-indexed given an already-fetched
     * Last-Modified timestamp, avoiding a redundant HEAD request.
     *
     * @param url                the URL to check
     * @param remoteLastModified the Last-Modified value already retrieved via HEAD
     * @return true if the page should be fetched and re-indexed
     * @throws IOException if database access fails
     */
    public boolean needReindex(String url, long remoteLastModified) throws IOException {
        // Not yet in index — must fetch
        Integer pageId = (Integer) dbManager.getUrl2Id().get(url);
        if (pageId == null) {
            return true;
        }

        // No stored metadata — must fetch
        PageMetadata metadata = (PageMetadata) dbManager.getPageMetadata().get(pageId);
        if (metadata == null) {
            return true;
        }

        // Re-index if remote is newer (or unknown, i.e. 0)
        return remoteLastModified == 0 || remoteLastModified > metadata.getLastModified();
    }

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

        // Check if page has been modified
        PageFetcher fetcher = new PageFetcher();
        long currentLastModified = fetcher.getLastModified(url);
        return currentLastModified > metadata.getLastModified() || currentLastModified == 0;
    }
}
