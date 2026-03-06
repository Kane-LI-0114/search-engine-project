package search.indexer.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single posting entry in the inverted index.
 * Stores term frequency, max term frequency, and position information
 * for one term in one document. Supports phrase search via positions.
 */
public class PostingEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    /** The document PageID */
    private int pageId;

    /** The frequency of this term in the document */
    private int termFrequency;

    /** The maximum term frequency across all terms in the document (for normalization) */
    private int maxTermFrequency;

    /** The list of positions where this term appears in the document */
    private ArrayList<Integer> positions;

    /**
     * Constructs a PostingEntry for a term-document pair.
     *
     * @param pageId           the document PageID
     * @param termFrequency    the frequency of this term in the document
     * @param maxTermFrequency the maximum term frequency across all terms in the document
     * @param positions        the list of positions where this term appears
     */
    public PostingEntry(int pageId, int termFrequency, int maxTermFrequency,
                        List<Integer> positions) {
        this.pageId = pageId;
        this.termFrequency = termFrequency;
        this.maxTermFrequency = maxTermFrequency;
        this.positions = new ArrayList<>(positions);
    }

    public int getPageId() { return pageId; }
    public int getTermFrequency() { return termFrequency; }
    public int getMaxTermFrequency() { return maxTermFrequency; }
    public List<Integer> getPositions() { return positions; }

    @Override
    public String toString() {
        return "PostingEntry{pageId=" + pageId + ", tf=" + termFrequency +
               ", maxTf=" + maxTermFrequency + ", positions=" + positions + "}";
    }
}
