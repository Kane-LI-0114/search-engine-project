package hk.ust.csit5930.search.common;

/**
 * Global configuration constants for the CSIT5930 search engine.
 * All configurable parameters are centralized here to avoid hard-coding.
 */
public final class Config {

    private Config() {
        // Prevent instantiation
    }

    /** Starting URL for the web crawler (assignment specification) */
    public static final String START_URL = "https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm";

    /** Maximum number of pages to crawl (assignment specification) */
    public static final int MAX_CRAWL_PAGES = 300;

    /** HTTP connection timeout in milliseconds */
    public static final int CRAWL_TIMEOUT = 10000;

    /** Title match boost factor for ranking */
    public static final double TITLE_BOOST_FACTOR = 3.0;

    /** Maximum number of search results to return */
    public static final int MAX_SEARCH_RESULTS = 50;

    /** Number of top keywords to display per page */
    public static final int TOP_KEYWORDS_COUNT = 5;

    /** JDBM database file path (without file extension) */
    public static final String DB_PATH = "searchengine_db";

    /** Stopwords file path */
    public static final String STOPWORDS_PATH = "stopwords.txt";

    /** Date format for display */
    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
}
