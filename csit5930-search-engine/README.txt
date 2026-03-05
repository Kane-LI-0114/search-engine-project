===============================================================================
                CSIT5930 Search Engine - README
                HKUST - Web Search Engine Project
===============================================================================

1. PROJECT OVERVIEW
-------------------
A full-featured web search engine developed for the CSIT5930 course at HKUST.
The system implements a complete search pipeline including:
- BFS web crawler (300 pages from the specified start URL)
- Dual inverted index construction (title and body, independently stored)
- TF-IDF + Cosine Similarity based ranking with title boost
- Phrase search support (double-quoted queries)
- JSP-based web interface with result display
- Enhancement: "Get Similar Pages" relevance feedback feature

2. ENVIRONMENT REQUIREMENTS
----------------------------
- Java JDK 8 or higher
- Apache Maven 3.6 or higher
- Apache Tomcat 9.0 or higher
- Internet connection (for crawling the target website)

3. DEPENDENCY INSTALLATION
---------------------------
This project uses htmlparser 2.1 and JDBM 1.0 libraries. If these are not
available from Maven Central, install them manually:

Step 1: Download the jar files:
  - htmlparser: http://htmlparser.sourceforge.net/
  - JDBM: http://jdbm.sourceforge.net/

Step 2: Install to local Maven repository:

  mvn install:install-file \
    -Dfile=htmlparser.jar \
    -DgroupId=org.htmlparser \
    -DartifactId=htmlparser \
    -Dversion=2.1 \
    -Dpackaging=jar

  mvn install:install-file \
    -Dfile=htmllexer.jar \
    -DgroupId=org.htmlparser \
    -DartifactId=htmllexer \
    -Dversion=2.1 \
    -Dpackaging=jar

  mvn install:install-file \
    -Dfile=jdbm-1.0.jar \
    -DgroupId=jdbm \
    -DartifactId=jdbm \
    -Dversion=1.0 \
    -Dpackaging=jar

4. BUILD INSTRUCTIONS
----------------------
Navigate to the project root directory (where pom.xml is located):

  cd csit5930-search-engine

Compile the project:

  mvn clean compile

Package as WAR for deployment:

  mvn clean package

The WAR file will be generated at: target/search-engine.war

5. RUNNING THE CRAWLER
-----------------------
Before starting the web interface, you must run the crawler to build the index.

Step 1: Ensure stopwords.txt is in the project root directory.

Step 2: Run the crawler:

  mvn exec:java -Dexec.mainClass="hk.ust.csit5930.search.crawler.Spider"

This will:
- Start BFS crawling from https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm
- Crawl up to 300 pages
- Build inverted indexes (title and body)
- Store all data in JDBM database files (searchengine_db.*)

The crawling process takes approximately 5-15 minutes depending on network speed.
Progress is logged to the console.

6. DEPLOYING THE WEB APPLICATION
----------------------------------
Step 1: Build the WAR file (if not already done):
  mvn clean package

Step 2: Copy the WAR file to Tomcat's webapps directory:
  cp target/search-engine.war $CATALINA_HOME/webapps/

Step 3: Copy the JDBM database files to Tomcat's working directory:
  cp searchengine_db.* $CATALINA_HOME/bin/

Step 4: Copy stopwords.txt to Tomcat's working directory:
  cp stopwords.txt $CATALINA_HOME/bin/

Step 5: Start Tomcat:
  $CATALINA_HOME/bin/startup.sh    (Linux/Mac)
  $CATALINA_HOME/bin/startup.bat   (Windows)

Step 6: Open the search engine in a browser:
  http://localhost:8080/search-engine/

7. USAGE GUIDE
---------------
- Enter search keywords in the search box and press "Search"
- Use double quotes for phrase search: "hong kong" universities
- Results show: score, title (clickable), URL, metadata, keywords, links
- Click "Get Similar Pages" on any result to find related pages
- All results are ranked by cosine similarity score (descending)
- Maximum 50 results are displayed per query

8. RESULT REPRODUCTION STEPS
------------------------------
Step 1: Run the crawler to crawl 300 pages (see Section 5)
Step 2: Deploy the web application (see Section 6)
Step 3: Test queries:
  - Single word: "algorithm"
  - Multiple words: "computer science research"
  - Phrase search: "hong kong"
  - Mixed: "hong kong" university
Step 4: Verify result format matches assignment specification
Step 5: Test "Get Similar Pages" button on any result

9. ENHANCEMENT FEATURE
------------------------
"Get Similar Pages" (Relevance Feedback):
- Each search result has a "Get Similar Pages" button
- Clicking it extracts the top 5 keywords from that page
- These keywords are used as a new search query
- Results show pages similar to the selected page
- Reuses existing search engine logic (no duplicate code)

10. PROJECT STRUCTURE
----------------------
csit5930-search-engine/
  pom.xml                 - Maven build configuration
  stopwords.txt           - English stopwords list (Lab3)
  README.txt              - This file
  src/main/java/          - Java source code
    hk/ust/csit5930/search/
      common/             - Config, JDBMManager, ExceptionHandler
      crawler/            - Spider, PageFetcher, LinkGraphManager, etc.
      indexer/            - Indexer, TextPreprocessor, PorterStemmer, etc.
      search/             - SearchEngine, QueryParser, Ranker, etc.
      web/                - SearchServlet, CharacterEncodingFilter
      enhancement/        - SimilarPageRecommender
  src/main/webapp/        - Web resources (JSP, web.xml)
  src/test/java/          - Unit tests

11. NOTES
----------
- Database files (searchengine_db.*) are NOT included in submission
- The crawler must be run before the web interface can be used
- All core data is stored via JDBM (disk-based, not in-memory)
- The Porter stemmer implementation follows the Lab3 specification
- Stopwords list is the same as provided in Lab3

===============================================================================
