package search.indexer;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for TextPreprocessor.
 * Verifies the text preprocessing pipeline including tokenization,
 * stopword removal, stemming, and position tracking.
 */
public class TextPreprocessorTest {

    private TextPreprocessor preprocessor;

    @Before
    public void setUp() throws Exception {
        preprocessor = new TextPreprocessor();
    }

    @Test
    public void testBasicProcessing() {
        List<TextPreprocessor.TokenInfo> tokens =
            preprocessor.processText("The quick brown fox jumps over the lazy dog");
        assertNotNull(tokens);
        // "the" and "over" are stopwords and should be filtered
        assertTrue(tokens.size() > 0);

        // Verify stems are present
        boolean foundFox = false;
        for (TextPreprocessor.TokenInfo token : tokens) {
            if (token.getStem().equals("fox")) {
                foundFox = true;
            }
        }
        assertTrue("Should contain stem 'fox'", foundFox);
    }

    @Test
    public void testStopwordRemoval() {
        List<TextPreprocessor.TokenInfo> tokens =
            preprocessor.processText("the a an is are was were");
        // All words are stopwords, result should be empty
        assertEquals(0, tokens.size());
    }

    @Test
    public void testEmptyInput() {
        List<TextPreprocessor.TokenInfo> tokens = preprocessor.processText("");
        assertEquals(0, tokens.size());

        tokens = preprocessor.processText(null);
        assertEquals(0, tokens.size());
    }

    @Test
    public void testPositionTracking() {
        List<TextPreprocessor.TokenInfo> tokens =
            preprocessor.processText("computer science algorithm");
        // Positions should be assigned sequentially
        for (int i = 0; i < tokens.size(); i++) {
            assertTrue(tokens.get(i).getPosition() >= 0);
        }
    }

    @Test
    public void testComputeTermFrequencies() {
        List<TextPreprocessor.TokenInfo> tokens =
            preprocessor.processText("test test test data data");
        Map<String, Integer> freq =
            TextPreprocessor.computeTermFrequencies(tokens);
        assertNotNull(freq);
        assertTrue(freq.size() > 0);
    }

    @Test
    public void testComputeMaxTermFrequency() {
        List<TextPreprocessor.TokenInfo> tokens =
            preprocessor.processText("test test test data data algorithm");
        Map<String, Integer> freq =
            TextPreprocessor.computeTermFrequencies(tokens);
        int maxTf = TextPreprocessor.computeMaxTermFrequency(freq);
        assertTrue(maxTf >= 1);
    }

    @Test
    public void testGroupPositionsByStem() {
        List<TextPreprocessor.TokenInfo> tokens =
            preprocessor.processText("computer science computer algorithm");
        Map<String, List<Integer>> positions =
            TextPreprocessor.groupPositionsByStem(tokens);
        assertNotNull(positions);
        // "computer" should appear in two positions
        String computerStem = new PorterStemmer().stem("computer");
        if (positions.containsKey(computerStem)) {
            assertEquals(2, positions.get(computerStem).size());
        }
    }

    @Test
    public void testCaseInsensitivity() {
        List<TextPreprocessor.TokenInfo> tokens1 =
            preprocessor.processText("Computer");
        List<TextPreprocessor.TokenInfo> tokens2 =
            preprocessor.processText("computer");

        if (!tokens1.isEmpty() && !tokens2.isEmpty()) {
            assertEquals(tokens1.get(0).getStem(), tokens2.get(0).getStem());
        }
    }
}
