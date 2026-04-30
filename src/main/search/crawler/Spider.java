package search.crawler;

import search.common.Config;
import search.common.ExceptionHandler;
import search.common.JDBMManager;
import search.crawler.model.CrawledPage;
import search.enhancement.PageRankCalculator;
import search.indexer.Indexer;

import java.util.*;

/**
 * Core web crawler implementing BFS (Breadth-First Search) strategy.
 * Entry point for the crawling process. Starting from the configured URL,
 * crawls up to MAX_CRAWL_PAGES pages, building link relationships and
 * delegating page indexing to the Indexer module.
 */
public class Spider {

    private PageFetcher pageFetcher;
    private PageIDMapper pageIdMapper;
    private LinkGraphManager linkGraphManager;
    private URLValidator urlValidator;
    private Indexer indexer;
    private Set<String> processedUrls;
    private JDBMManager dbManager;

    // Profiling fields
    private long totalValidationTime = 0;
    private long totalLastModifiedCheckTime = 0;
    private long totalReindexCheckTime = 0;
    private long totalFetchTime = 0;
    private long totalIdMappingTime = 0;
    private long totalIndexTime = 0;
    private long totalLinkProcessingTime = 0;
    private long totalCommitTime = 0;
    private long totalErrorTime = 0;
    private long totalMagnitudeTime = 0;
    private long totalPageRankTime = 0;
    private long startTime;

    // Page categorization counters
    private int trulyNewPages = 0;          // Never seen before (new URL)
    private int queuedPagesNowIndexed = 0;  // Discovered earlier, now indexing
    private int recrawledPages = 0;         // Already in index, being updated
    private int skippedPages = 0;           // Skipped by validator
    private int notModifiedPages = 0;       // Skipped due to Last-Modified check
    private int errorPages = 0;             // Failed during processing

    // Link statistics
    private int totalChildLinksFound = 0;
    private int newLinksAddedToQueue = 0;

    /**
     * Constructs a Spider and initializes all dependent components.
     *
     * @throws Exception if initialization fails
     */
    public Spider() throws Exception {
        this.dbManager = JDBMManager.getInstance();
        this.pageFetcher = new PageFetcher();
        this.pageIdMapper = new PageIDMapper();
        this.processedUrls = new HashSet<>();
        this.urlValidator = new URLValidator(processedUrls);
        this.linkGraphManager = new LinkGraphManager();
        this.indexer = new Indexer();
    }

    /**
     * Starts the BFS crawling process from the configured start URL.
     * Uses a queue for BFS traversal, processes each page through validation,
     * indexing, and link extraction. Crawls until reaching MAX_CRAWL_PAGES
     * or exhausting the URL queue.
     *
     * @throws Exception if a critical crawling error occurs
     */
    public void startCrawl() throws Exception {
        ExceptionHandler.info("Starting BFS crawl from: " + Config.START_URL);
        ExceptionHandler.info("Target pages: " + Config.MAX_CRAWL_PAGES);

        startTime = System.currentTimeMillis();

        // BFS queue for URL traversal (assignment requirement: must use BFS)
        Queue<String> urlQueue = new LinkedList<>();
        urlQueue.add(Config.START_URL);

        int crawledCount = 0;
        long phaseStartTime;

        while (!urlQueue.isEmpty() && crawledCount < Config.MAX_CRAWL_PAGES) {
            String currentUrl = urlQueue.poll();
            long pageStartTime = System.currentTimeMillis();

            // PHASE 1: Pre-crawl validation
            phaseStartTime = System.nanoTime();
            boolean shouldFetch = false;
            try {
                shouldFetch = urlValidator.needFetch(currentUrl);
            } catch (Exception e) {
                ExceptionHandler.warn("Validation error for: " + currentUrl + " - " + e.getMessage());
            }
            totalValidationTime += (System.nanoTime() - phaseStartTime);

            if (!shouldFetch) {
                skippedPages++;
                continue;
            }

            // Mark URL as processed to prevent re-crawling (circular link protection)
            processedUrls.add(currentUrl);

            try {
                // Determine page status before fetching
                boolean hasPageId = pageIdMapper.hasUrl(currentUrl);
                boolean hasMetadata = false;

                if (hasPageId) {
                    int existingId = pageIdMapper.getPageId(currentUrl);
                    try {
                        hasMetadata = dbManager.getPageMetadata().get(existingId) != null;
                    } catch (Exception e) {
                        // Ignore metadata check errors
                    }
                }

                // PHASE 2: Check Last-Modified via HEAD request before full fetch
                // Only HEAD-check pages that already exist in the index (have metadata).
                // First-time URLs will always need fetching, so skip the HEAD round-trip.
                boolean needsReindex = true;
                if (hasMetadata) {
                    phaseStartTime = System.nanoTime();
                    long remoteLastModified = pageFetcher.getLastModified(currentUrl);
                    totalLastModifiedCheckTime += (System.nanoTime() - phaseStartTime);

                    phaseStartTime = System.nanoTime();
                    needsReindex = urlValidator.needReindex(currentUrl, remoteLastModified);
                    totalReindexCheckTime += (System.nanoTime() - phaseStartTime);
                }

                if (!needsReindex) {
                    ExceptionHandler.info("[skip] Not modified: " + currentUrl);
                    notModifiedPages++;

                    // Still need to process child links for BFS completeness
                    // Use cached child links from link graph if available
                    int pageId = pageIdMapper.getOrCreatePageId(currentUrl);
                    List<Integer> cachedChildIds = linkGraphManager.getChildPageIds(pageId);
                    for (Integer childId : cachedChildIds) {
                        String childUrl = pageIdMapper.getUrl(childId);
                        if (childUrl != null && !processedUrls.contains(childUrl) && !urlQueue.contains(childUrl)) {
                            urlQueue.add(childUrl);
                        }
                    }

                    // Categorize the page (not modified, but was previously indexed)
                    recrawledPages++;

                    // Log timing for slow pages (>2 seconds)
                    long pageElapsed = System.currentTimeMillis() - pageStartTime;
                    if (pageElapsed > 2000) {
                        ExceptionHandler.info(String.format(
                                "Slow page: %s took %dms (not modified, skipped reindex)", currentUrl, pageElapsed));
                    }

                    continue;
                }

                // PHASE 3: Fetch the page via HTTP
                phaseStartTime = System.nanoTime();
                ExceptionHandler.info("[" + (crawledCount + 1) + "/" +
                        Config.MAX_CRAWL_PAGES + "] Crawling: " + currentUrl);
                CrawledPage page = pageFetcher.fetchPage(currentUrl);
                totalFetchTime += (System.nanoTime() - phaseStartTime);

                // PHASE 4: Assign PageID for this URL
                phaseStartTime = System.nanoTime();
                int pageId = pageIdMapper.getOrCreatePageId(currentUrl);
                totalIdMappingTime += (System.nanoTime() - phaseStartTime);

                // PHASE 5: Index the page
                phaseStartTime = System.nanoTime();
                indexer.indexPage(pageId, page);
                totalIndexTime += (System.nanoTime() - phaseStartTime);

                // PHASE 6: Process child links
                phaseStartTime = System.nanoTime();
                List<String> childLinks = page.getChildLinks();
                totalChildLinksFound += childLinks.size();
                int newLinksFound = 0;

                for (String childUrl : childLinks) {
                    // Assign PageID for child URL (creates mapping if new)
                    int childPageId = pageIdMapper.getOrCreatePageId(childUrl);

                    // Record parent-child link relationship in JDBM
                    linkGraphManager.addLink(pageId, childPageId);

                    // Add to BFS queue if not already processed
                    if (!processedUrls.contains(childUrl) && !urlQueue.contains(childUrl)) {
                        urlQueue.add(childUrl);
                        newLinksFound++;
                    }
                }
                newLinksAddedToQueue += newLinksFound;
                totalLinkProcessingTime += (System.nanoTime() - phaseStartTime);

                // Categorize the page
                if (!hasPageId) {
                    trulyNewPages++;
                } else if (!hasMetadata) {
                    queuedPagesNowIndexed++;
                } else {
                    recrawledPages++;
                }

                crawledCount++;

                // PHASE 7: Commit JDBM transaction periodically
                if (crawledCount % 50 == 0) {
                    phaseStartTime = System.nanoTime();
                    dbManager.commit();
                    totalCommitTime += (System.nanoTime() - phaseStartTime);

                    long elapsed = System.currentTimeMillis() - startTime;
                    double pagesPerSec = crawledCount * 1000.0 / elapsed;
                    ExceptionHandler.info(String.format(
                            "Progress: %d/%d pages (%.1f pages/sec), Queue: %d URLs",
                            crawledCount, Config.MAX_CRAWL_PAGES, pagesPerSec, urlQueue.size()));
                }

                // Log timing for slow pages (>2 seconds)
                long pageElapsed = System.currentTimeMillis() - pageStartTime;
                if (pageElapsed > 2000) {
                    ExceptionHandler.info(String.format(
                            "Slow page: %s took %dms", currentUrl, pageElapsed));
                }

            } catch (Exception e) {
                // Log error and continue with next URL (do not interrupt overall crawl)
                errorPages++;
                phaseStartTime = System.nanoTime();
                ExceptionHandler.handleError("Failed to crawl: " + currentUrl, e);
                totalErrorTime += (System.nanoTime() - phaseStartTime);
            }
        }

        // Store total document count for IDF calculation
        dbManager.getSystemConfig().put("totalDocs", String.valueOf(crawledCount));

        // Enhancement 8: Pre-compute document magnitudes for cosine normalization
        phaseStartTime = System.nanoTime();
        try {
            indexer.computeDocumentMagnitudes(crawledCount);
            ExceptionHandler.info("Document magnitudes computed for " +
                    crawledCount + " pages.");
        } catch (Exception e) {
            ExceptionHandler.handleError(
                    "Document magnitude computation failed (non-fatal)", e);
        }
        totalMagnitudeTime += (System.nanoTime() - phaseStartTime);

        // Enhancement 6: Compute PageRank after crawling completes
        phaseStartTime = System.nanoTime();
        try {
            PageRankCalculator pageRankCalc = new PageRankCalculator();
            pageRankCalc.calculate(pageIdMapper.getTotalPages());
        } catch (Exception e) {
            ExceptionHandler.handleError("PageRank calculation failed (non-fatal)", e);
        }
        totalPageRankTime += (System.nanoTime() - phaseStartTime);

        // Final commit to persist all remaining data
        long commitStart = System.nanoTime();
        dbManager.commit();
        totalCommitTime += (System.nanoTime() - commitStart);

        // Print profiling report
        printProfilingReport(crawledCount);

        ExceptionHandler.info("Crawling completed. Total pages crawled: " + crawledCount);
    }

    /**
     * Prints a detailed profiling report of the crawling session.
     */
    private void printProfilingReport(int crawledCount) {
        long totalTime = System.currentTimeMillis() - startTime;

        // Convert nanoseconds to milliseconds for display
        long validationMs = totalValidationTime / 1_000_000;
        long lastModifiedMs = totalLastModifiedCheckTime / 1_000_000;
        long reindexCheckMs = totalReindexCheckTime / 1_000_000;
        long fetchMs = totalFetchTime / 1_000_000;
        long idMappingMs = totalIdMappingTime / 1_000_000;
        long indexMs = totalIndexTime / 1_000_000;
        long linkProcessingMs = totalLinkProcessingTime / 1_000_000;
        long commitMs = totalCommitTime / 1_000_000;
        long errorMs = totalErrorTime / 1_000_000;
        long magnitudeMs = totalMagnitudeTime / 1_000_000;
        long pageRankMs = totalPageRankTime / 1_000_000;

        // Calculate unaccounted time (overhead)
        long accountedMs = validationMs + lastModifiedMs + reindexCheckMs +
                fetchMs + idMappingMs + indexMs + linkProcessingMs +
                commitMs + errorMs + magnitudeMs + pageRankMs;
        long overheadMs = totalTime - accountedMs;

        // Total pages processed (including skipped/errors/not-modified)
        int totalProcessed = crawledCount + skippedPages + notModifiedPages + errorPages;

        ExceptionHandler.info("\n========================================");
        ExceptionHandler.info("          CRAWLER PROFILING REPORT");
        ExceptionHandler.info("========================================");
        ExceptionHandler.info(String.format("Total time:        %6d ms (%d sec)",
                totalTime, totalTime / 1000));
        ExceptionHandler.info(String.format("Pages crawled:     %6d", crawledCount));
        ExceptionHandler.info(String.format("Total processed:   %6d", totalProcessed));
        ExceptionHandler.info(String.format("Throughput:        %6.1f pages/sec",
                crawledCount * 1000.0 / Math.max(totalTime, 1)));
        ExceptionHandler.info("----------------------------------------");
        ExceptionHandler.info("Page Categorization:");
        ExceptionHandler.info(String.format("  Truly new URLs:        %6d", trulyNewPages));
        ExceptionHandler.info(String.format("  Queued, now indexed:   %6d", queuedPagesNowIndexed));
        ExceptionHandler.info(String.format("  Re-crawled (updated):  %6d", recrawledPages));
        ExceptionHandler.info(String.format("  Skipped (validation):  %6d", skippedPages));
        ExceptionHandler.info(String.format("  Not modified (HEAD):   %6d", notModifiedPages));
        ExceptionHandler.info(String.format("  Errors:                %6d", errorPages));
        ExceptionHandler.info("----------------------------------------");
        ExceptionHandler.info("Link Statistics:");
        ExceptionHandler.info(String.format("  Total child links found: %6d", totalChildLinksFound));
        ExceptionHandler.info(String.format("  New URLs added to queue: %6d", newLinksAddedToQueue));
        ExceptionHandler.info(String.format("  Avg links per page:      %6.1f",
                crawledCount > 0 ? (double) totalChildLinksFound / crawledCount : 0));
        ExceptionHandler.info("----------------------------------------");
        ExceptionHandler.info("Phase breakdown:");
        printPhase("URL Validation", validationMs, totalTime);
        printPhase("Last-Modified Check", lastModifiedMs, totalTime);
        printPhase("Reindex Decision", reindexCheckMs, totalTime);
        printPhase("HTTP Fetching", fetchMs, totalTime);
        printPhase("ID Mapping", idMappingMs, totalTime);
        printPhase("Page Indexing", indexMs, totalTime);
        printPhase("Link Processing", linkProcessingMs, totalTime);
        printPhase("DB Commits", commitMs, totalTime);
        printPhase("Error Handling", errorMs, totalTime);
        printPhase("Doc Magnitudes", magnitudeMs, totalTime);
        printPhase("PageRank Calc", pageRankMs, totalTime);
        printPhase("Other Overhead", overheadMs, totalTime);
        ExceptionHandler.info("========================================\n");
    }

    /**
     * Helper to print a single phase with timing and percentage.
     */
    private void printPhase(String phaseName, long timeMs, long totalTimeMs) {
        double percentage = totalTimeMs > 0 ? (timeMs * 100.0 / totalTimeMs) : 0;
        ExceptionHandler.info(String.format("  %-20s %6d ms (%5.1f%%)",
                phaseName, timeMs, percentage));
    }

    /**
     * Main entry point for running the crawler as a standalone process.
     * Initializes the crawler, starts BFS crawling, and closes resources.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        try {
            Spider spider = new Spider();
            spider.startCrawl();
            JDBMManager.getInstance().close();
            ExceptionHandler.info("Crawler finished and database closed.");
        } catch (Exception e) {
            ExceptionHandler.handleError("Crawler fatal error", e);
            System.exit(1);
        }
    }
}