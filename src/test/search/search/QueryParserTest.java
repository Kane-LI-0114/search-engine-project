package search.search;

import search.search.model.Query;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for QueryParser.
 * Verifies query parsing including phrase extraction, term processing,
 * and mixed query handling.
 */
public class QueryParserTest {

    private QueryParser parser;

    @Before
    public void setUp() throws Exception {
        parser = new QueryParser();
    }

    @Test
    public void testSimpleQuery() throws Exception {
        Query query = parser.parse("computer science");
        assertNotNull(query);
        assertFalse(query.isEmpty());
        assertTrue(query.getTerms().size() > 0);
        assertEquals(0, query.getPhrases().size());
    }

    @Test
    public void testPhraseQuery() throws Exception {
        Query query = parser.parse("\"hong kong\"");
        assertNotNull(query);
        assertFalse(query.isEmpty());
        assertEquals(1, query.getPhrases().size());
        assertTrue(query.getPhrases().get(0).getStems().size() > 0);
    }

    @Test
    public void testMixedQuery() throws Exception {
        Query query = parser.parse("\"hong kong\" universities");
        assertNotNull(query);
        assertFalse(query.isEmpty());
        assertEquals(1, query.getPhrases().size());
        assertTrue(query.getTerms().size() > 0);
    }

    @Test
    public void testEmptyQuery() throws Exception {
        Query query = parser.parse("");
        assertNotNull(query);
        assertTrue(query.isEmpty());

        query = parser.parse(null);
        assertNotNull(query);
        assertTrue(query.isEmpty());
    }

    @Test
    public void testMultiplePhrases() throws Exception {
        Query query = parser.parse("\"data mining\" \"machine learning\"");
        assertNotNull(query);
        assertEquals(2, query.getPhrases().size());
    }

    @Test
    public void testStopwordsOnlyQuery() throws Exception {
        Query query = parser.parse("the a an");
        assertNotNull(query);
        // All words are stopwords, should be filtered
        assertTrue(query.getTerms().isEmpty());
    }

    @Test
    public void testGetAllStems() throws Exception {
        Query query = parser.parse("\"hong kong\" algorithm");
        assertNotNull(query);
        assertTrue(query.getAllStems().size() > 0);
    }
}
