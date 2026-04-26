package search.indexer;

import search.common.Config;
import search.common.JDBMManager;
import search.crawler.model.CrawledPage;
import search.indexer.model.PageMetadata;
import search.indexer.model.PostingEntry;
import search.indexer.model.PostingList;

import java.util.*;

/**
 * Core indexer that processes crawled pages and builds inverted indexes.
 * For each page, processes both title and body text through the preprocessing pipeline,
 * constructs posting entries with term frequencies and positions, and stores
 * metadata for result display. All data is persisted to JDBM.
 */
public class Indexer {

    private TextPreprocessor preprocessor;
    private InvertedIndexManager indexManager;
    private JDBMManager dbManager;

    /**
     * Constructs an Indexer and initializes the text preprocessor and index manager.
     *
     * @throws Exception if initialization fails
     */
    public Indexer() throws Exception {
        this.preprocessor = new TextPreprocessor();
        this.indexManager = new InvertedIndexManager();
        this.dbManager = JDBMManager.getInstance();
    }

    /**
     * Indexes a single crawled page. Processes both title and body text,
     * builds inverted index entries with position data for phrase search,
     * computes top keywords, and stores page metadata.
     *
     * @param pageId the unique PageID assigned to this page
     * @param page   the crawled page data to index
     * @throws Exception if indexing fails
     */
    public void indexPage(int pageId, CrawledPage page) throws Exception {
        // Process body text through preprocessing pipeline
        List<TextPreprocessor.TokenInfo> bodyTokens =
            preprocessor.processText(page.getBody());
        Map<String, Integer> bodyFrequencies =
            TextPreprocessor.computeTermFrequencies(bodyTokens);
        int bodyMaxTf = TextPreprocessor.computeMaxTermFrequency(bodyFrequencies);
        Map<String, List<Integer>> bodyPositions =
            TextPreprocessor.groupPositionsByStem(bodyTokens);

        // Process title text through preprocessing pipeline
        List<TextPreprocessor.TokenInfo> titleTokens =
            preprocessor.processText(page.getTitle());
        Map<String, Integer> titleFrequencies =
            TextPreprocessor.computeTermFrequencies(titleTokens);
        int titleMaxTf = TextPreprocessor.computeMaxTermFrequency(titleFrequencies);
        Map<String, List<Integer>> titlePositions =
            TextPreprocessor.groupPositionsByStem(titleTokens);

        // Build body inverted index entries
        for (Map.Entry<String, Integer> entry : bodyFrequencies.entrySet()) {
            String stem = entry.getKey();
            int tf = entry.getValue();
            List<Integer> positions = bodyPositions.get(stem);
            PostingEntry postingEntry = new PostingEntry(
                pageId, tf, bodyMaxTf, positions);
            indexManager.addPosting(stem, postingEntry, false); // body index
        }

        // Build title inverted index entries
        for (Map.Entry<String, Integer> entry : titleFrequencies.entrySet()) {
            String stem = entry.getKey();
            int tf = entry.getValue();
            List<Integer> positions = titlePositions.get(stem);
            PostingEntry postingEntry = new PostingEntry(
                pageId, tf, titleMaxTf, positions);
            indexManager.addPosting(stem, postingEntry, true); // title index
        }

        // Compute top 5 keywords from body text (by frequency, descending)
        LinkedHashMap<String, Integer> topKeywords =
            computeTopKeywords(bodyFrequencies);

        // Store page metadata for search result display
        PageMetadata metadata = new PageMetadata(
            pageId, page.getUrl(), page.getTitle(),
            page.getLastModified(), page.getPageSize(), topKeywords
        );
        dbManager.getPageMetadata().put(pageId, metadata);
    }

    /**
     * Computes pre-computed document magnitudes for all indexed documents.
     * The magnitude |d| = sqrt(Σ(w(t,d)²)) includes ALL terms in each document,
     * where w(t,d) = body_tfidf(t,d) + title_tfidf(t,d) * TITLE_BOOST_FACTOR.
     * This enables correct cosine normalization without needing to build
     * full document vectors at search time.
     *
     * @param totalDocs the total number of crawled documents
     * @throws Exception if index access fails
     */
    public void computeDocumentMagnitudes(int totalDocs) throws Exception {
        jdbm.htree.HTree bodyIndex = dbManager.getBodyIndex();
        jdbm.htree.HTree titleIndex = dbManager.getTitleIndex();

        // Map pageId -> sum of squared combined TF-IDF weights
        Map<Integer, Double> squaredSums = new HashMap<>();

        // Collect all unique stems from both indexes
        Set<String> allStems = new HashSet<>();
        jdbm.helper.FastIterator bodyKeys = bodyIndex.keys();
        String stem;
        while ((stem = (String) bodyKeys.next()) != null) {
            allStems.add(stem);
        }
        jdbm.helper.FastIterator titleKeys = titleIndex.keys();
        while ((stem = (String) titleKeys.next()) != null) {
            allStems.add(stem);
        }

        // For each unique stem, compute its contribution to document magnitudes
        for (String term : allStems) {
            // Compute combined document frequency (df) for this stem
            Set<Integer> docsWithTerm = new HashSet<>();
            PostingList bodyPl = (PostingList) bodyIndex.get(term);
            PostingList titlePl = (PostingList) titleIndex.get(term);
            if (bodyPl != null) docsWithTerm.addAll(bodyPl.getEntries().keySet());
            if (titlePl != null) docsWithTerm.addAll(titlePl.getEntries().keySet());

            int df = docsWithTerm.size();
            if (df == 0) continue;

            double idf = Math.log((double) totalDocs / df);

            // For each document containing this term, compute combined weight
            for (int pageId : docsWithTerm) {
                double weight = 0.0;

                // Body TF-IDF component: tfidf = (tf * idf) / maxTf
                if (bodyPl != null) {
                    PostingEntry bodyEntry = bodyPl.getEntry(pageId);
                    if (bodyEntry != null) {
                        weight += (bodyEntry.getTermFrequency() * idf)
                                / bodyEntry.getMaxTermFrequency();
                    }
                }

                // Title TF-IDF component with boost
                if (titlePl != null) {
                    PostingEntry titleEntry = titlePl.getEntry(pageId);
                    if (titleEntry != null) {
                        weight += (titleEntry.getTermFrequency() * idf)
                                / titleEntry.getMaxTermFrequency()
                                * Config.TITLE_BOOST_FACTOR;
                    }
                }

                if (weight > 0) {
                    squaredSums.merge(pageId, weight * weight, Double::sum);
                }
            }
        }

        // Store magnitudes: |d| = sqrt(Σ(w²))
        jdbm.htree.HTree magnitudesTree = dbManager.getDocMagnitudes();
        for (Map.Entry<Integer, Double> entry : squaredSums.entrySet()) {
            double magnitude = Math.sqrt(entry.getValue());
            magnitudesTree.put(entry.getKey(), magnitude);
        }
    }

    /**
     * Computes the top N keywords from a term frequency map.
     * Returns a LinkedHashMap ordered by frequency descending.
     *
     * @param frequencies the term frequency map
     * @return ordered map of top keywords (stem -> frequency)
     */
    private LinkedHashMap<String, Integer> computeTopKeywords(
            Map<String, Integer> frequencies) {
        // Sort entries by frequency in descending order
        List<Map.Entry<String, Integer>> sortedEntries =
            new ArrayList<>(frequencies.entrySet());
        sortedEntries.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        // Take top N keywords
        LinkedHashMap<String, Integer> topKeywords = new LinkedHashMap<>();
        int count = 0;
        for (Map.Entry<String, Integer> entry : sortedEntries) {
            if (count >= Config.TOP_KEYWORDS_COUNT) {
                break;
            }
            topKeywords.put(entry.getKey(), entry.getValue());
            count++;
        }
        return topKeywords;
    }
}
