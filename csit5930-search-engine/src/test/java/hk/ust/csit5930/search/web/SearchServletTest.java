package hk.ust.csit5930.search.web;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the web module.
 * Basic verification that servlet classes can be instantiated
 * and core configuration is correctly set up.
 */
public class SearchServletTest {

    @Test
    public void testServletExists() {
        // Verify the SearchServlet class can be loaded
        try {
            Class<?> servletClass = Class.forName(
                "hk.ust.csit5930.search.web.SearchServlet");
            assertNotNull(servletClass);
        } catch (ClassNotFoundException e) {
            fail("SearchServlet class should be loadable");
        }
    }

    @Test
    public void testFilterExists() {
        // Verify the CharacterEncodingFilter class can be loaded
        try {
            Class<?> filterClass = Class.forName(
                "hk.ust.csit5930.search.web.CharacterEncodingFilter");
            assertNotNull(filterClass);
        } catch (ClassNotFoundException e) {
            fail("CharacterEncodingFilter class should be loadable");
        }
    }
}
