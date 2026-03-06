package search.search.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a parsed search query containing both individual terms and phrase terms.
 * Individual terms are stemmed single words; phrase terms are quoted multi-word expressions.
 */
public class Query {

    /** Individual stemmed query terms (non-phrase) */
    private List<String> terms;

    /** Phrase terms extracted from double-quoted expressions */
    private List<PhraseTerm> phrases;

    /**
     * Constructs a Query with the given terms and phrases.
     *
     * @param terms   list of individual stemmed terms
     * @param phrases list of phrase terms
     */
    public Query(List<String> terms, List<PhraseTerm> phrases) {
        this.terms = terms;
        this.phrases = phrases;
    }

    public List<String> getTerms() { return terms; }
    public List<PhraseTerm> getPhrases() { return phrases; }

    /**
     * Returns all individual stems including those from phrases.
     * Used for TF-IDF vector construction.
     *
     * @return combined list of all stems
     */
    public List<String> getAllStems() {
        List<String> allStems = new ArrayList<>(terms);
        for (PhraseTerm phrase : phrases) {
            allStems.addAll(phrase.getStems());
        }
        return allStems;
    }

    /**
     * Checks if the query has no terms or phrases.
     *
     * @return true if query is empty
     */
    public boolean isEmpty() {
        return terms.isEmpty() && phrases.isEmpty();
    }

    @Override
    public String toString() {
        return "Query{terms=" + terms + ", phrases=" + phrases + "}";
    }
}
