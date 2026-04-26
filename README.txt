===============================================================================
                CSIT5930 Search Engine - README
                HKUST - Web Search Engine Project
===============================================================================

1. PROJECT OVERVIEW
-------------------
A full-featured web search engine developed for the CSIT5930 course at HKUST.
The system implements a complete search pipeline including:
- BFS web crawler (300 pages from a configurable start URL)
- Dual inverted index construction (title and body, independently stored)
- TF-IDF + Cosine Similarity ranking with configurable title boost (3.0x)
- PageRank blended into final score (weight configurable via Config.java)
- Phrase search support (double-quoted queries: "hong kong")
- Autocomplete keyword suggestions as you type
- Keyword Browser — browse all indexed keywords at /keywords
- Query History — recent searches stored locally in the browser
- Similar Pages — relevance feedback via top-5 keyword re-query
- JSP-based web interface with result display

2. ENVIRONMENT REQUIREMENTS
----------------------------
- Java JDK 8 or higher
- Apache Maven 3.6 or higher
- Apache Tomcat 9.0 or higher (Tomcat deployment only)
- Internet connection (for crawling the target website)

3. QUICK START
--------------
A deployment script (deploy.sh) is provided that handles all steps:

  ./deploy.sh          # Interactive menu
  ./deploy.sh docker   # Crawl (if needed) -> build -> Docker run
  ./deploy.sh tomcat   # Crawl (if needed) -> build -> deploy to Tomcat
  ./deploy.sh crawl    # Run crawler only
  ./deploy.sh build    # Build WAR only
  ./deploy.sh stop     # Stop and remove the running Docker container

The script checks prerequisites, installs local Maven dependencies, runs the
crawler if no database files exist, builds the WAR, and deploys.

4. MANUAL DEPLOYMENT
---------------------

4.1 Prerequisites

  | Tool           | Version        |
  |----------------|----------------|
  | Java JDK       | 8 or higher    |
  | Apache Maven   | 3.6 or higher  |
  | Apache Tomcat  | 9.0 or higher  |

4.2 Install Local Dependencies

  This project requires htmlparser 2.1 and JDBM 1.0, which are not on Maven
  Central. Install them once:

    mvn install:install-file \
      -Dfile=htmlparser.jar \
      -DgroupId=org.htmlparser -DartifactId=htmlparser \
      -Dversion=2.1 -Dpackaging=jar

    mvn install:install-file \
      -Dfile=htmllexer.jar \
      -DgroupId=org.htmlparser -DartifactId=htmllexer \
      -Dversion=2.1 -Dpackaging=jar

    mvn install:install-file \
      -Dfile=jdbm-1.0.jar \
      -DgroupId=jdbm -DartifactId=jdbm \
      -Dversion=1.0 -Dpackaging=jar

  Download links: htmlparser (http://htmlparser.sourceforge.net/)
                  JDBM (http://jdbm.sourceforge.net/)

4.3 Build

    mvn clean package -DskipTests

  The WAR is generated at: target/search-engine.war

4.4 Crawl and Index

  Run the crawler BEFORE deploying the web app. It performs a BFS crawl of up
  to 300 pages and builds the search index:

    mvn clean compile
    mvn exec:java -Dexec.mainClass="search.crawler.Spider"

  This produces searchengine_db.* files in the current directory.
  Crawling typically takes 5-15 minutes.

4.5 Deploy

  Option A -- Docker (recommended)

    Ensure the crawler has already run and searchengine_db.* files exist, then:

      docker build -t search-engine .
      docker run -p 8080:8080 search-engine

    Access at: http://localhost:8080/

  Option B -- Tomcat (manual)

    1. Copy WAR to Tomcat:
       cp target/search-engine.war $CATALINA_HOME/webapps/

    2. Copy database files and stopwords to Tomcat's working directory:
       cp searchengine_db.* $CATALINA_HOME/bin/
       cp src/main/resources/stopwords.txt $CATALINA_HOME/bin/

    3. Start Tomcat:
       $CATALINA_HOME/bin/startup.sh      (Linux / macOS)
       $CATALINA_HOME/bin/startup.bat     (Windows)

    Access at: http://localhost:8080/search-engine/

5. FEATURES
-----------
  - BFS Web Crawler -- crawls up to 300 pages from a configurable start URL
  - Dual Inverted Index -- separate title and body indexes stored via JDBM
  - TF-IDF + Cosine Similarity ranking with configurable title boost (3.0x)
    and full-document magnitude normalization
  - PageRank -- blended into the final score (weight configurable in Config.java)
  - Phrase Search -- double-quoted terms, e.g. "hong kong"
  - Autocomplete -- keyword suggestions as you type
  - Keyword Browser -- browse all indexed keywords at /keywords
  - Query History -- recent searches stored locally in the browser
  - Similar Pages -- relevance feedback via top-5 keyword re-query
  - JSP Web Interface -- score, metadata, top keywords, parent/child links

6. USAGE GUIDE
--------------
  | Feature           | How to use                                    |
  |-------------------|-----------------------------------------------|
  | Keyword search    | Type terms and press Search                   |
  | Phrase search     | Wrap phrases in double quotes: "hong kong"    |
  | Mixed query       | "hong kong" university research               |
  | Autocomplete      | Suggestions appear as you type                |
  | Browse keywords   | Click Browse Keywords on the home page        |
  | Similar pages     | Click Get Similar Pages on any result         |

  Results display: relevance score, page title (link), URL, last-modified date,
  page size, top 5 keywords, parent and child links.

7. PROJECT STRUCTURE
--------------------
  search-engine-project/
    pom.xml
    deploy.sh
    Dockerfile
    src/
      main/
        search/
          common/             Config, JDBMManager, ExceptionHandler
          crawler/            Spider, PageFetcher, URLValidator, PageIDMapper
            model/            CrawledPage, LinkRelation
          indexer/            Indexer, InvertedIndexManager, PorterStemmer
            model/            PostingEntry, PostingList, PageMetadata
          search/             SearchEngine, QueryParser, PhraseMatcher
            model/            Query, PhraseTerm, SearchResult
          web/                SearchServlet, KeywordBrowseServlet
                              SuggestServlet, CharacterEncodingFilter
          enhancement/        PageRankCalculator, SimilarPageRecommender
        resources/
          stopwords.txt
        webapp/
          index.jsp
          result.jsp
          keywords.jsp
          WEB-INF/
            web.xml
      test/
        search/               JUnit test classes
    target/
      search-engine.war       Deployable WAR (after build)

8. NOTES
--------
  - Database files (searchengine_db.*) are NOT committed -- run crawler first.
  - The Porter stemmer implementation follows the Lab3 specification.
  - The stopwords list is identical to the one provided in Lab3.
  - All persistent data uses JDBM disk-based storage (10 HTrees).
  - Document magnitudes are pre-computed at indexing time for correct cosine
    normalization (all terms, not just query terms).
  - The crawler must be run before the web interface can be used.

===============================================================================
