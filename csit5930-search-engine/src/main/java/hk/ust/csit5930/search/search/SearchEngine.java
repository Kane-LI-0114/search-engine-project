package hk.ust.csit5930.search.search;

import hk.ust.csit5930.search.common.ExceptionHandler;
import hk.ust.csit5930.search.indexer.InvertedIndexManager;
import hk.ust.csit5930.search.indexer.model.PostingList;
import hk.ust.csit5930.search.search.model.PhraseTerm;
import hk.ust.csit5930.search.search.model.Query;
import hk.ust.csit5930.search.search.model.SearchResult;

import java.util.*;

/**
 * Main search engine class providing the unified search interface.
 * Coordinates the complete search pipeline:
 * 1. Query parsing (phrase extraction and text preprocessing)
 * 2. Candidate document retrieval from inverted indexes
 * 3. Phrase matching filter (position-based verification)
 * 4. TF-IDF scoring with title boost and cosine similarity
 * 5. Result ranking and truncation
 */
public class SearchEngine {

    private QueryParser queryParser;
    private PhraseMatcher phraseMatcher;
    private Ranker ranker;
    private InvertedIndexManager indexManager;

    /**
     * Constructs a SearchEngine and initializes all search components.
     *
     * @throws Exception if initialization fails
     */
    public SearchEngine() throws Exception {
        this.queryParser = new QueryParser();
        this.phraseMatcher = new PhraseMatcher();
        this.ranker = new Ranker();
        this.indexManager = new InvertedIndexManager();
    }

    /**
     * Executes a search query and returns ranked results.
     * This is the unified entry point for all search operations.
     *
     * @param queryStr the raw query string from the user
     * @return list of search results sorted by score descending, up to 50 results
     * @throws Exception if any search operation fails
     */
    public List<SearchResult> search(String queryStr) throws Exception {
        // Step 1: Parse the query into terms and phrases
        Query query = queryParser.parse(queryStr);
        if (query.isEmpty()) {
            return new ArrayList<>();
        }

        ExceptionHandler.info("Parsed query: " + query);

        // Step 2: Retrieve candidate documents from inverted indexes
        Set<Integer> candidateIds = getCandidateDocuments(query);
        if (candidateIds.isEmpty()) {
            return new ArrayList<>();
        }

        // Step 3: Apply phrase matching filter
        candidateIds = applyPhraseFilter(query, candidateIds);
        if (candidateIds.isEmpty()) {
            return new ArrayList<>();
        }

        ExceptionHandler.info("Candidate documents after filtering: " +
            candidateIds.size());

        // Step 4: Rank candidates and return top results
        return ranker.rank(query, candidateIds);
    }

    /**
     * Retrieves candidate documents that contain at least one query term.
     * Searches both body and title inverted indexes.
     *
     * @param query the parsed query
     * @return set of candidate page IDs
     * @throws Exception if index access fails
     */
    private Set<Integer> getCandidateDocuments(Query query) throws Exception {
        Set<Integer> candidates = new HashSet<>();

        // Collect documents containing individual terms
        for (String stem : query.getTerms()) {
            addDocumentsForTerm(stem, candidates);
        }

        // Collect documents containing phrase terms
        for (PhraseTerm phrase : query.getPhrases()) {
            for (String stem : phrase.getStems()) {
                addDocumentsForTerm(stem, candidates);
            }
        }

        return candidates;
    }

    /**
     * Adds all documents containing a given term to the candidate set.
     * Searches both body and title indexes.
     *
     * @param stem       the stemmed term
     * @param candidates the candidate set to add to
     * @throws Exception if index access fails
     */
    private void addDocumentsForTerm(String stem, Set<Integer> candidates)
            throws Exception {
        PostingList bodyPl = indexManager.getBodyPostingList(stem);
        if (bodyPl != null) {
            candidates.addAll(bodyPl.getEntries().keySet());
        }

        PostingList titlePl = indexManager.getTitlePostingList(stem);
        if (titlePl != null) {
            candidates.addAll(titlePl.getEntries().keySet());
        }
    }

    /**
     * Applies phrase matching to filter candidate documents.
     * For each phrase in the query, retains only documents where the phrase
     * appears as consecutive terms in either body or title.
     *
     * @param query      the parsed query
     * @param candidates the initial candidate set
     * @return filtered candidate set
     * @throws Exception if phrase matching fails
     */
    private Set<Integer> applyPhraseFilter(Query query, Set<Integer> candidates)
            throws Exception {
        if (query.getPhrases().isEmpty()) {
            return candidates; // No phrases to filter
        }

        Set<Integer> filtered = new HashSet<>(candidates);

        for (PhraseTerm phrase : query.getPhrases()) {
            // Get pages matching the phrase in body OR title
            Set<Integer> bodyMatches =
                phraseMatcher.getPhraseMatchedPageIds(phrase, false);
            Set<Integer> titleMatches =
                phraseMatcher.getPhraseMatchedPageIds(phrase, true);

            Set<Integer> phraseMatches = new HashSet<>();
            phraseMatches.addAll(bodyMatches);
            phraseMatches.addAll(titleMatches);

            // Retain only candidates that match this phrase
            filtered.retainAll(phraseMatches);
        }

        return filtered;
    }
}
