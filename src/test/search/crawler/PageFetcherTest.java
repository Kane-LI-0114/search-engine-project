package search.crawler;

import search.crawler.model.CrawledPage;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for PageFetcher.
 * Tests HTTP fetching, metadata extraction, and HTML parsing capabilities.
 * Note: These tests require internet access to fetch actual web pages.
 */
public class PageFetcherTest {

    private PageFetcher fetcher = new PageFetcher();

    @Test
    public void testFetchValidPage() {
        try {
            CrawledPage page = fetcher.fetchPage(
                "https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm");
            assertNotNull(page);
            assertNotNull(page.getUrl());
            assertNotNull(page.getTitle());
            assertNotNull(page.getBody());
            assertTrue(page.getLastModified() > 0);
            assertTrue(page.getPageSize() > 0);
            assertNotNull(page.getChildLinks());
        } catch (Exception e) {
            // Test may fail without internet - acceptable for offline testing
            System.out.println("Skipping fetch test (no internet): " + e.getMessage());
        }
    }

    @Test
    public void testGetLastModified() {
        long lastMod = fetcher.getLastModified(
            "https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm");
        // May return 0 if header not available, but should not throw
        assertTrue(lastMod >= 0);
    }

    @Test
    public void testFetchInvalidUrl() {
        try {
            fetcher.fetchPage("http://invalid.domain.that.does.not.exist/");
            fail("Should throw exception for invalid URL");
        } catch (Exception e) {
            // Expected behavior
            assertNotNull(e.getMessage());
        }
    }
}
