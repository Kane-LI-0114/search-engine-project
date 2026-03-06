# CSIT5930 Search Engine

A full-featured web search engine developed for the **CSIT5930** course at HKUST.

## Features

- **BFS Web Crawler** — crawls up to 300 pages from a configurable start URL
- **Dual Inverted Index** — separate title and body indexes stored via JDBM HTrees
- **TF-IDF + Cosine Similarity** ranking with configurable title boost factor
- **Phrase Search** — double-quoted terms (e.g. `"hong kong"`)
- **JSP Web Interface** — displays score, metadata, top keywords, parent/child links
- **Similar Pages** — relevance feedback via top-5 keyword re-query

---

## Project Structure

```
search-engine-project/
├── pom.xml                         # Maven build configuration
├── stopwords.txt                   # English stopwords list
├── src/
│   ├── main/
│   │   ├── search/                 # Java source code (root package: search.*)
│   │   │   ├── common/             # Config, JDBMManager, ExceptionHandler
│   │   │   ├── crawler/            # Spider, PageFetcher, URLValidator, PageIDMapper, LinkGraphManager
│   │   │   │   └── model/          # CrawledPage, LinkRelation
│   │   │   ├── indexer/            # Indexer, InvertedIndexManager, PorterStemmer, TextPreprocessor
│   │   │   │   └── model/          # PostingEntry, PostingList, PageMetadata
│   │   │   ├── search/             # SearchEngine, QueryParser, PhraseMatcher, Ranker, SimilarityCalculator
│   │   │   │   └── model/          # Query, PhraseTerm, SearchResult
│   │   │   ├── web/                # SearchServlet, CharacterEncodingFilter
│   │   │   └── enhancement/        # SimilarPageRecommender
│   │   ├── resources/
│   │   │   └── stopwords.txt
│   │   └── webapp/
│   │       ├── index.jsp
│   │       ├── result.jsp
│   │       └── WEB-INF/web.xml
│   └── test/
│       └── search/                 # JUnit test classes (7 files)
└── target/
    └── search-engine.war           # Deployable WAR (after build)
```

---

## Requirements

| Tool | Version |
|------|---------|
| Java JDK | 8 or higher |
| Apache Maven | 3.6 or higher |
| Apache Tomcat | 9.0 or higher |

---

## Build

```bash
# Compile only
mvn clean compile

# Package as deployable WAR (skip tests)
mvn clean package -DskipTests

# Package and run tests
mvn clean package
```

The WAR file is generated at `target/search-engine.war`.

---

## Dependency Installation

This project requires **htmlparser 2.1** and **JDBM 1.0**. If Maven cannot resolve them from Central, install them manually:

```bash
# Download from:
#   htmlparser: http://htmlparser.sourceforge.net/
#   JDBM:       http://jdbm.sourceforge.net/

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
```

---

## Running the Crawler

Before using the web interface, crawl and index the target site:

```bash
mvn exec:java -Dexec.mainClass="search.crawler.Spider"
```

This will:
1. BFS-crawl up to **300 pages** starting from `https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm`
2. Build title and body inverted indexes
3. Persist all data to JDBM database files (`searchengine_db.*`)

Crawling typically takes **5–15 minutes** depending on network speed.

---

## Deploying the Web Application

```bash
# 1. Build the WAR
mvn clean package -DskipTests

# 2. Deploy to Tomcat
cp target/search-engine.war $CATALINA_HOME/webapps/

# 3. Copy JDBM database files and stopwords to Tomcat's working directory
cp searchengine_db.* $CATALINA_HOME/bin/
cp stopwords.txt $CATALINA_HOME/bin/

# 4. Start Tomcat
$CATALINA_HOME/bin/startup.sh        # Linux / macOS
$CATALINA_HOME/bin/startup.bat       # Windows
```

Access the search engine at: **http://localhost:8080/search-engine/**

---

## Usage

| Feature | How to use |
|---------|-----------|
| Keyword search | Type terms and press **Search** |
| Phrase search | Wrap phrases in double quotes: `"hong kong"` |
| Mixed query | `"hong kong" university research` |
| Similar pages | Click **Get Similar Pages** on any result |

Results display: relevance score (6 decimal places), page title (link), URL, last-modified date, page size, top 5 keywords, parent and child links.

---

## Notes

- **Database files (`searchengine_db.*`) are not included** — run the crawler first.
- The Porter stemmer follows the Lab3 specification.
- The stopwords list is identical to the one provided in Lab3.
- All persistent data uses JDBM disk-based storage (8 HTrees).
