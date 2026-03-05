package hk.ust.csit5930.search.indexer;

import hk.ust.csit5930.search.indexer.model.PostingEntry;
import hk.ust.csit5930.search.indexer.model.PostingList;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Unit tests for PostingList and PostingEntry model classes.
 * Verifies data structure operations used by the inverted index.
 */
public class PostingListTest {

    @Test
    public void testPostingEntryCreation() {
        PostingEntry entry = new PostingEntry(
            1, 5, 10, Arrays.asList(0, 3, 7, 12, 20));
        assertEquals(1, entry.getPageId());
        assertEquals(5, entry.getTermFrequency());
        assertEquals(10, entry.getMaxTermFrequency());
        assertEquals(5, entry.getPositions().size());
    }

    @Test
    public void testPostingListAddAndGet() {
        PostingList list = new PostingList();

        PostingEntry entry1 = new PostingEntry(
            1, 3, 5, Arrays.asList(0, 4, 8));
        PostingEntry entry2 = new PostingEntry(
            2, 7, 10, Arrays.asList(1, 2, 3, 5, 9, 12, 15));

        list.addEntry(entry1);
        list.addEntry(entry2);

        assertEquals(2, list.getDocumentFrequency());
        assertNotNull(list.getEntry(1));
        assertNotNull(list.getEntry(2));
        assertNull(list.getEntry(3));

        assertEquals(3, list.getEntry(1).getTermFrequency());
        assertEquals(7, list.getEntry(2).getTermFrequency());
    }

    @Test
    public void testPostingListUpdate() {
        PostingList list = new PostingList();

        PostingEntry entry1 = new PostingEntry(
            1, 3, 5, Arrays.asList(0, 4, 8));
        list.addEntry(entry1);

        // Update with new entry for same page
        PostingEntry entry2 = new PostingEntry(
            1, 5, 5, Arrays.asList(0, 4, 8, 10, 15));
        list.addEntry(entry2);

        // Should still be 1 document (updated)
        assertEquals(1, list.getDocumentFrequency());
        assertEquals(5, list.getEntry(1).getTermFrequency());
    }

    @Test
    public void testEmptyPostingList() {
        PostingList list = new PostingList();
        assertEquals(0, list.getDocumentFrequency());
        assertNull(list.getEntry(1));
    }
}
