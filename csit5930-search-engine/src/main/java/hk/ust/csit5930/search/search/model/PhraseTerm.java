package hk.ust.csit5930.search.search.model;

import java.util.List;

/**
 * Represents a phrase search term enclosed in double quotes.
 * Contains the stemmed words of the phrase for position-based matching.
 */
public class PhraseTerm {

    /** The original phrase text as entered by the user */
    private String originalPhrase;

    /** The stemmed words of the phrase in order */
    private List<String> stems;

    /**
     * Constructs a PhraseTerm.
     *
     * @param originalPhrase the original phrase text
     * @param stems          the stemmed words of the phrase in order
     */
    public PhraseTerm(String originalPhrase, List<String> stems) {
        this.originalPhrase = originalPhrase;
        this.stems = stems;
    }

    public String getOriginalPhrase() { return originalPhrase; }
    public List<String> getStems() { return stems; }

    @Override
    public String toString() {
        return "PhraseTerm{original='" + originalPhrase + "', stems=" + stems + "}";
    }
}
