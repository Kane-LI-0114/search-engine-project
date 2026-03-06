package search.enhancement;

import search.common.ExceptionHandler;
import search.common.JDBMManager;
import search.indexer.model.PageMetadata;
import search.search.SearchEngine;
import search.search.model.SearchResult;

import java.util.*;

/**
 * Enhancement feature: "Get Similar Pages" functionality (assignment bonus option 1).
 * Implements relevance feedback by extracting top keywords from a selected page
 * and using them as a new search query to discover related pages.
 * Reuses the existing SearchEngine core logic (no duplicate development).
 */
public class SimilarPageRecommender {

    private SearchEngine searchEngine;
    private JDBMManager dbManager;

    /**
     * Constructs a SimilarPageRecommender.
     *
     * @throws Exception if initialization fails
     */
    public SimilarPageRecommender() throws Exception {
        this.searchEngine = new SearchEngine();
        this.dbManager = JDBMManager.getInstance();
    }

    /**
     * Finds pages similar to the specified page.
     * Process:
     * 1. Extract top 5 keywords from the page's metadata (stopwords already excluded)
     * 2. Construct a new search query from these keywords
     * 3. Execute the search using the existing SearchEngine
     * 4. Remove the original page from the results
     *
     * @param pageId the PageID of the reference page
     * @return list of similar pages ranked by relevance
     * @throws Exception if recommendation fails
     */
    public List<SearchResult> getSimilarPages(int pageId) throws Exception {
        // Step 1: Get page metadata to extract top keywords
        PageMetadata metadata = (PageMetadata)
            dbManager.getPageMetadata().get(pageId);
        if (metadata == null) {
            ExceptionHandler.warn("No metadata found for pageId: " + pageId);
            return new ArrayList<>();
        }

        // Step 2: Build query from top 5 keywords
        LinkedHashMap<String, Integer> topKeywords = metadata.getTopKeywords();
        if (topKeywords == null || topKeywords.isEmpty()) {
            return new ArrayList<>();
        }

        StringBuilder queryBuilder = new StringBuilder();
        for (String keyword : topKeywords.keySet()) {
            if (queryBuilder.length() > 0) {
                queryBuilder.append(" ");
            }
            queryBuilder.append(keyword);
        }

        // Step 3: Execute search with the keyword query
        String queryStr = queryBuilder.toString();
        ExceptionHandler.info("Similar pages query for pageId " +
            pageId + ": " + queryStr);

        List<SearchResult> results = searchEngine.search(queryStr);

        // Step 4: Remove the original page from results
        results.removeIf(r -> r.getPageId() == pageId);

        return results;
    }

    /**
     * Builds a query string from a page's top keywords.
     * Can be used to display the auto-generated query to the user.
     *
     * @param pageId the PageID
     * @return the keyword query string, or empty string if unavailable
     * @throws Exception if metadata access fails
     */
    public String buildKeywordQuery(int pageId) throws Exception {
        PageMetadata metadata = (PageMetadata)
            dbManager.getPageMetadata().get(pageId);
        if (metadata == null || metadata.getTopKeywords() == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (String keyword : metadata.getTopKeywords().keySet()) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(keyword);
        }
        return sb.toString();
    }
}
