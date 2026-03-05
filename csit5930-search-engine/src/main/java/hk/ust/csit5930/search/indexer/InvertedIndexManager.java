package hk.ust.csit5930.search.indexer;

import hk.ust.csit5930.search.common.JDBMManager;
import hk.ust.csit5930.search.indexer.model.PostingEntry;
import hk.ust.csit5930.search.indexer.model.PostingList;

import java.io.IOException;

/**
 * Manages the dual inverted indexes (title and body) for the search engine.
 * Both indexes are stored independently in separate JDBM HTrees
 * (assignment requirement: two completely independent inverted index files).
 */
public class InvertedIndexManager {

    private JDBMManager dbManager;

    /**
     * Constructs an InvertedIndexManager.
     *
     * @throws IOException if database access fails
     */
    public InvertedIndexManager() throws IOException {
        this.dbManager = JDBMManager.getInstance();
    }

    /**
     * Adds a posting entry for a term to either the title or body inverted index.
     * If the term already has a posting list, the new entry is appended.
     * If not, a new posting list is created.
     *
     * @param stem    the stemmed term
     * @param entry   the posting entry (pageId, tf, maxTf, positions)
     * @param isTitle true for the title index, false for the body index
     * @throws Exception if database operation fails
     */
    public void addPosting(String stem, PostingEntry entry, boolean isTitle)
            throws Exception {
        jdbm.htree.HTree index = isTitle ?
            dbManager.getTitleIndex() : dbManager.getBodyIndex();

        // Load existing posting list or create a new one
        PostingList postingList = (PostingList) index.get(stem);
        if (postingList == null) {
            postingList = new PostingList();
        }

        // Add the new posting entry
        postingList.addEntry(entry);

        // Persist updated posting list back to JDBM
        index.put(stem, postingList);
    }

    /**
     * Retrieves the posting list for a term from the body inverted index.
     *
     * @param stem the stemmed term to look up
     * @return the PostingList, or null if the term is not indexed
     * @throws IOException if database access fails
     */
    public PostingList getBodyPostingList(String stem) throws IOException {
        return (PostingList) dbManager.getBodyIndex().get(stem);
    }

    /**
     * Retrieves the posting list for a term from the title inverted index.
     *
     * @param stem the stemmed term to look up
     * @return the PostingList, or null if the term is not indexed
     * @throws IOException if database access fails
     */
    public PostingList getTitlePostingList(String stem) throws IOException {
        return (PostingList) dbManager.getTitleIndex().get(stem);
    }
}
