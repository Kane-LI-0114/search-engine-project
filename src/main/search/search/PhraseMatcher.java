package search.search;

import search.indexer.InvertedIndexManager;
import search.indexer.model.PostingEntry;
import search.indexer.model.PostingList;
import search.search.model.PhraseTerm;

import java.util.*;

/**
 * Implements phrase matching using position information stored in the inverted index.
 * Verifies that all terms in a phrase appear at consecutive positions in a document,
 * which is the core mechanism for supporting double-quoted phrase searches.
 */
public class PhraseMatcher {

    private InvertedIndexManager indexManager;

    /**
     * Constructs a PhraseMatcher.
     *
     * @throws Exception if initialization fails
     */
    public PhraseMatcher() throws Exception {
        this.indexManager = new InvertedIndexManager();
    }

    /**
     * Finds all page IDs where the given phrase appears as consecutive terms
     * in either the body or title text.
     *
     * @param phrase the phrase term to match
     * @return set of PageIDs that contain the phrase
     * @throws Exception if index access fails
     */
    public Set<Integer> getPhraseMatchedPageIds(PhraseTerm phrase) throws Exception {
        return getPhraseMatchedPageIds(phrase, false);
    }

    /**
     * Finds all page IDs where the given phrase appears as consecutive terms.
     *
     * @param phrase  the phrase term to match
     * @param isTitle true to search in the title index, false for body index
     * @return set of PageIDs that contain the phrase
     * @throws Exception if index access fails
     */
    public Set<Integer> getPhraseMatchedPageIds(PhraseTerm phrase, boolean isTitle)
            throws Exception {
        List<String> stems = phrase.getStems();
        if (stems.isEmpty()) {
            return new HashSet<>();
        }

        // Single-word phrase: just check term existence
        if (stems.size() == 1) {
            PostingList pl = isTitle ?
                indexManager.getTitlePostingList(stems.get(0)) :
                indexManager.getBodyPostingList(stems.get(0));
            if (pl == null) {
                return new HashSet<>();
            }
            return new HashSet<>(pl.getEntries().keySet());
        }

        // Multi-word phrase: need position-based matching
        // Step 1: Get posting lists for all stems in the phrase
        List<PostingList> postingLists = new ArrayList<>();
        for (String stem : stems) {
            PostingList pl = isTitle ?
                indexManager.getTitlePostingList(stem) :
                indexManager.getBodyPostingList(stem);
            if (pl == null) {
                return new HashSet<>(); // If any term is missing, no phrase match
            }
            postingLists.add(pl);
        }

        // Step 2: Find candidate documents (intersection of all posting lists)
        Set<Integer> candidates = new HashSet<>(
            postingLists.get(0).getEntries().keySet());
        for (int i = 1; i < postingLists.size(); i++) {
            candidates.retainAll(postingLists.get(i).getEntries().keySet());
        }

        // Step 3: Verify position-based consecutive matching for each candidate
        Set<Integer> matchedPageIds = new HashSet<>();
        for (int pageId : candidates) {
            if (checkPositionMatch(pageId, stems, postingLists)) {
                matchedPageIds.add(pageId);
            }
        }

        return matchedPageIds;
    }

    /**
     * Checks if phrase terms appear at consecutive positions in a specific document.
     * For each starting position of the first term, verifies that term[i] appears
     * at position (startPos + i) for all subsequent terms.
     *
     * @param pageId       the document PageID
     * @param stems        the ordered list of stems in the phrase
     * @param postingLists the posting lists for each stem
     * @return true if the phrase matches in this document
     */
    private boolean checkPositionMatch(int pageId, List<String> stems,
                                       List<PostingList> postingLists) {
        // Get positions for the first term
        PostingEntry firstEntry = postingLists.get(0).getEntry(pageId);
        if (firstEntry == null) {
            return false;
        }

        // For each starting position of the first term
        for (int startPos : firstEntry.getPositions()) {
            boolean match = true;

            // Check if each subsequent term appears at the expected consecutive position
            for (int i = 1; i < stems.size(); i++) {
                PostingEntry entry = postingLists.get(i).getEntry(pageId);
                if (entry == null ||
                    !entry.getPositions().contains(startPos + i)) {
                    match = false;
                    break;
                }
            }

            if (match) {
                return true; // Found a valid consecutive phrase occurrence
            }
        }
        return false;
    }
}
