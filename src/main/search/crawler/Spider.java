package search.crawler;

import search.common.Config;
import search.common.ExceptionHandler;
import search.common.JDBMManager;
import search.crawler.model.CrawledPage;
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

        // BFS queue for URL traversal (assignment requirement: must use BFS)
        Queue<String> urlQueue = new LinkedList<>();
        urlQueue.add(Config.START_URL);

        int crawledCount = 0;

        while (!urlQueue.isEmpty() && crawledCount < Config.MAX_CRAWL_PAGES) {
            String currentUrl = urlQueue.poll();

            // Pre-crawl validation: circular link check, index existence, update check
            if (!urlValidator.needFetch(currentUrl)) {
                continue;
            }

            // Mark URL as processed to prevent re-crawling (circular link protection)
            processedUrls.add(currentUrl);

            try {
                // Fetch the page via HTTP
                ExceptionHandler.info("[" + (crawledCount + 1) + "/" +
                    Config.MAX_CRAWL_PAGES + "] Crawling: " + currentUrl);
                CrawledPage page = pageFetcher.fetchPage(currentUrl);

                // Assign PageID for this URL
                int pageId = pageIdMapper.getOrCreatePageId(currentUrl);

                // Index the page: build inverted indexes and store metadata
                indexer.indexPage(pageId, page);

                // Process child links: assign IDs, record relationships, enqueue
                List<String> childLinks = page.getChildLinks();
                for (String childUrl : childLinks) {
                    // Assign PageID for child URL (creates mapping if new)
                    int childPageId = pageIdMapper.getOrCreatePageId(childUrl);

                    // Record parent-child link relationship in JDBM
                    linkGraphManager.addLink(pageId, childPageId);

                    // Add to BFS queue if not already processed
                    if (!processedUrls.contains(childUrl)) {
                        urlQueue.add(childUrl);
                    }
                }

                crawledCount++;

                // Commit JDBM transaction periodically (every 10 pages)
                if (crawledCount % 10 == 0) {
                    dbManager.commit();
                    ExceptionHandler.info("Progress: " + crawledCount +
                        " pages crawled, committed to disk.");
                }
            } catch (Exception e) {
                // Log error and continue with next URL (do not interrupt overall crawl)
                ExceptionHandler.handleError("Failed to crawl: " + currentUrl, e);
            }
        }

        // Store total document count for IDF calculation
        dbManager.getSystemConfig().put("totalDocs", String.valueOf(crawledCount));

        // Final commit to persist all remaining data
        dbManager.commit();
        ExceptionHandler.info("Crawling completed. Total pages crawled: " + crawledCount);
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
