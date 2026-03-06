package search.indexer.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a complete posting list for a single term in the inverted index.
 * Maps document PageIDs to their corresponding PostingEntry objects.
 * The document frequency (df) is the number of entries in this list.
 */
public class PostingList implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Map from PageID to PostingEntry */
    private HashMap<Integer, PostingEntry> entries;

    /**
     * Constructs an empty PostingList.
     */
    public PostingList() {
        this.entries = new HashMap<>();
    }

    /**
     * Adds or updates a posting entry for a document.
     *
     * @param entry the PostingEntry to add
     */
    public void addEntry(PostingEntry entry) {
        entries.put(entry.getPageId(), entry);
    }

    /**
     * Gets the posting entry for a specific document.
     *
     * @param pageId the document PageID
     * @return the PostingEntry, or null if not found
     */
    public PostingEntry getEntry(int pageId) {
        return entries.get(pageId);
    }

    /**
     * Returns all entries in this posting list.
     *
     * @return map of PageID to PostingEntry
     */
    public Map<Integer, PostingEntry> getEntries() {
        return entries;
    }

    /**
     * Returns the document frequency (number of documents containing this term).
     *
     * @return the document frequency
     */
    public int getDocumentFrequency() {
        return entries.size();
    }

    @Override
    public String toString() {
        return "PostingList{df=" + entries.size() + "}";
    }
}
