package search.search;

import search.common.Config;
import search.common.JDBMManager;
import search.crawler.LinkGraphManager;
import search.crawler.PageIDMapper;
import search.indexer.InvertedIndexManager;
import search.indexer.model.PageMetadata;
import search.indexer.model.PostingEntry;
import search.indexer.model.PostingList;
import search.search.model.Query;
import search.search.model.SearchResult;

import java.util.*;

/**
 * Ranks search results using TF-IDF weighting with title boost and cosine similarity.
 * Implements the complete ranking pipeline:
 * 1. Build query TF-IDF vector
 * 2. Build document TF-IDF vectors with title boost
 * 3. Compute cosine similarity
 * 4. Sort and truncate to top N results
 */
public class Ranker {

    private InvertedIndexManager indexManager;
    private SimilarityCalculator calculator;
    private LinkGraphManager linkGraphManager;
    private PageIDMapper pageIdMapper;
    private JDBMManager dbManager;

    /**
     * Constructs a Ranker with all required components.
     *
     * @throws Exception if initialization fails
     */
    public Ranker() throws Exception {
        this.indexManager = new InvertedIndexManager();
        this.calculator = new SimilarityCalculator();
        this.linkGraphManager = new LinkGraphManager();
        this.pageIdMapper = new PageIDMapper();
        this.dbManager = JDBMManager.getInstance();
    }

    /**
     * Ranks candidate documents for a parsed query.
     * Computes TF-IDF vectors with title boost, calculates cosine similarity,
     * and returns sorted results truncated to MAX_SEARCH_RESULTS.
     *
     * @param query        the parsed query
     * @param candidateIds the set of candidate page IDs (after phrase filtering)
     * @return sorted list of search results, up to MAX_SEARCH_RESULTS
     * @throws Exception if ranking fails
     */
    public List<SearchResult> rank(Query query, Set<Integer> candidateIds)
            throws Exception {
        // Get total document count for IDF calculation
        String totalDocsStr = (String) dbManager.getSystemConfig().get("totalDocs");
        int totalDocs = (totalDocsStr != null) ? Integer.parseInt(totalDocsStr) : 1;

        // Build query TF-IDF vector
        List<String> allStems = query.getAllStems();
        Map<String, Integer> queryTermFreq = new HashMap<>();
        for (String stem : allStems) {
            queryTermFreq.merge(stem, 1, Integer::sum);
        }
        int queryMaxTf = queryTermFreq.values().stream()
            .max(Integer::compareTo).orElse(1);

        Map<String, Double> queryVector = new HashMap<>();
        for (Map.Entry<String, Integer> entry : queryTermFreq.entrySet()) {
            String stem = entry.getKey();
            int tf = entry.getValue();

            // Document frequency across both title and body indexes
            int df = getDocumentFrequency(stem);
            if (df > 0) {
                double idf = Math.log((double) totalDocs / df);
                double queryWeight = (tf * idf) / queryMaxTf;
                queryVector.put(stem, queryWeight);
            }
        }

        // Score each candidate document
        List<SearchResult> results = new ArrayList<>();
        for (int pageId : candidateIds) {
            // Build document vector with title boost
            Map<String, Double> docVector = buildDocumentVector(
                pageId, queryTermFreq.keySet(), totalDocs);
            if (docVector.isEmpty()) {
                continue;
            }

            // Calculate cosine similarity between query and document vectors
            double score = calculator.calculateCosineSimilarity(
                queryVector, docVector);
            if (score > 0) {
                SearchResult result = buildSearchResult(pageId, score);
                if (result != null) {
                    results.add(result);
                }
            }
        }

        // Sort by score descending
        Collections.sort(results);

        // Truncate to MAX_SEARCH_RESULTS (assignment requirement: top 50)
        if (results.size() > Config.MAX_SEARCH_RESULTS) {
            results = new ArrayList<>(
                results.subList(0, Config.MAX_SEARCH_RESULTS));
        }

        return results;
    }

    /**
     * Builds the document weight vector with title boost.
     * For each query term, the effective weight is:
     * body_tfidf + title_tfidf * TITLE_BOOST_FACTOR
     *
     * @param pageId     the document PageID
     * @param queryTerms the set of query term stems
     * @param totalDocs  the total number of documents
     * @return the document weight vector (stem -> weight)
     * @throws Exception if index access fails
     */
    private Map<String, Double> buildDocumentVector(
            int pageId, Set<String> queryTerms, int totalDocs) throws Exception {
        Map<String, Double> docVector = new HashMap<>();

        for (String stem : queryTerms) {
            double weight = 0.0;
            int df = getDocumentFrequency(stem);

            // Body TF-IDF component
            PostingList bodyPl = indexManager.getBodyPostingList(stem);
            if (bodyPl != null) {
                PostingEntry bodyEntry = bodyPl.getEntry(pageId);
                if (bodyEntry != null) {
                    double bodyTfIdf = calculator.calculateTfIdf(
                        bodyEntry.getTermFrequency(),
                        bodyEntry.getMaxTermFrequency(),
                        totalDocs, df);
                    weight += bodyTfIdf;
                }
            }

            // Title TF-IDF component with boost factor
            PostingList titlePl = indexManager.getTitlePostingList(stem);
            if (titlePl != null) {
                PostingEntry titleEntry = titlePl.getEntry(pageId);
                if (titleEntry != null) {
                    double titleTfIdf = calculator.calculateTfIdf(
                        titleEntry.getTermFrequency(),
                        titleEntry.getMaxTermFrequency(),
                        totalDocs, df);
                    // Apply title boost (assignment requirement)
                    weight += titleTfIdf * Config.TITLE_BOOST_FACTOR;
                }
            }

            if (weight > 0) {
                docVector.put(stem, weight);
            }
        }

        return docVector;
    }

    /**
     * Gets the combined document frequency for a term across both indexes.
     * A document is counted once even if the term appears in both title and body.
     *
     * @param stem the stemmed term
     * @return the document frequency
     * @throws Exception if index access fails
     */
    private int getDocumentFrequency(String stem) throws Exception {
        Set<Integer> docsWithTerm = new HashSet<>();

        PostingList bodyPl = indexManager.getBodyPostingList(stem);
        if (bodyPl != null) {
            docsWithTerm.addAll(bodyPl.getEntries().keySet());
        }

        PostingList titlePl = indexManager.getTitlePostingList(stem);
        if (titlePl != null) {
            docsWithTerm.addAll(titlePl.getEntries().keySet());
        }

        return docsWithTerm.size();
    }

    /**
     * Builds a SearchResult object with all required display fields,
     * including parent and child link URLs.
     *
     * @param pageId the document PageID
     * @param score  the computed similarity score
     * @return a SearchResult, or null if metadata is unavailable
     * @throws Exception if data access fails
     */
    private SearchResult buildSearchResult(int pageId, double score)
            throws Exception {
        PageMetadata metadata = (PageMetadata)
            dbManager.getPageMetadata().get(pageId);
        if (metadata == null) {
            return null;
        }

        // Get parent and child link URLs from the link graph
        List<String> parentLinks = getUrlsForPageIds(
            linkGraphManager.getParentPageIds(pageId));
        List<String> childLinks = getUrlsForPageIds(
            linkGraphManager.getChildPageIds(pageId));

        return new SearchResult(
            pageId, score, metadata.getTitle(), metadata.getUrl(),
            metadata.getLastModified(), metadata.getPageSize(),
            metadata.getTopKeywords(), parentLinks, childLinks
        );
    }

    /**
     * Converts a list of PageIDs to their corresponding URLs.
     *
     * @param pageIds the list of PageIDs
     * @return list of URLs
     * @throws Exception if lookup fails
     */
    private List<String> getUrlsForPageIds(List<Integer> pageIds)
            throws Exception {
        List<String> urls = new ArrayList<>();
        for (int id : pageIds) {
            String url = pageIdMapper.getUrl(id);
            if (url != null) {
                urls.add(url);
            }
        }
        return urls;
    }
}
