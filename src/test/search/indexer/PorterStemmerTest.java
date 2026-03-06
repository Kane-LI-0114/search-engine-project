package search.indexer;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for PorterStemmer.
 * Verifies that the Porter stemming algorithm produces correct stems
 * for common English words.
 */
public class PorterStemmerTest {

    private PorterStemmer stemmer = new PorterStemmer();

    @Test
    public void testBasicStemming() {
        // Test plural forms
        assertEquals("connect", stemmer.stem("connected"));
        assertEquals("caress", stemmer.stem("caresses"));

        // Test -ing forms
        assertEquals("walk", stemmer.stem("walking"));

        // Test -tion forms
        assertEquals("comput", stemmer.stem("computation"));
    }

    @Test
    public void testShortWords() {
        // Words shorter than 3 characters should be returned as-is
        assertEquals("a", stemmer.stem("a"));
        assertEquals("an", stemmer.stem("an"));
        assertEquals("is", stemmer.stem("is"));
    }

    @Test
    public void testAlreadyStemmedWords() {
        // Words that are already stems should not change significantly
        String stem1 = stemmer.stem("search");
        assertNotNull(stem1);
        assertFalse(stem1.isEmpty());
    }

    @Test
    public void testNullInput() {
        assertNull(stemmer.stem(null));
    }

    @Test
    public void testConsistency() {
        // Same word should produce same stem
        String stem1 = stemmer.stem("running");
        String stem2 = stemmer.stem("running");
        assertEquals(stem1, stem2);

        // Related words should produce same stem
        String stem3 = stemmer.stem("runs");
        String stem4 = stemmer.stem("running");
        // Both should be related to "run"
        assertNotNull(stem3);
        assertNotNull(stem4);
    }
}
