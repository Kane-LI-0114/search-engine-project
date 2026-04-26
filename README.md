# CSIT5930 Search Engine

A full-featured web search engine developed for the **CSIT5930** course at HKUST.

---

## Quick Start

### Automated deployment (recommended)

A deployment script is provided that handles all steps automatically.

```bash
./deploy.sh          # Interactive menu
```

Or specify a mode directly:

```bash
./deploy.sh docker   # Crawl (if needed) → build → Docker run
./deploy.sh tomcat   # Crawl (if needed) → build → deploy to Tomcat
./deploy.sh crawl    # Run crawler only
./deploy.sh build    # Build WAR only
./deploy.sh stop     # Stop and remove the running Docker container
```

The script will check prerequisites, install local Maven dependencies, run the crawler if no database files exist, build the WAR, and deploy.

---

### Manual deployment

#### 1. Prerequisites

| Tool | Version |
|------|---------|
| Java JDK | 8 or higher |
| Apache Maven | 3.6 or higher |
| Apache Tomcat | 9.0 or higher |

#### 2. Install local dependencies

This project requires **htmlparser 2.1** and **JDBM 1.0**, which are not on Maven Central. Install them once:

```bash
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

> Download links: [htmlparser](http://htmlparser.sourceforge.net/) · [JDBM](http://jdbm.sourceforge.net/)

#### 3. Build

```bash
mvn clean package -DskipTests
```

The WAR is generated at `target/search-engine.war`.

#### 4. Crawl and index

Run the crawler **before** deploying the web app. It performs a BFS crawl of up to 300 pages and builds the search index:

```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="search.crawler.Spider"
```

This produces `searchengine_db.*` files in the current directory. Crawling typically takes **5–15 minutes**.

#### 5. Deploy

##### Option A — Docker (recommended)

Make sure the crawler has already run and `searchengine_db.*` files exist in the project root, then:

```bash
docker build -t search-engine .
docker run -p 8080:8080 search-engine
```

The app is deployed as the ROOT context inside the container, so it is accessible at:

```
http://localhost:8080/
```

##### Option B — Tomcat (manual)

```bash
# Copy WAR to Tomcat
cp target/search-engine.war $CATALINA_HOME/webapps/

# Copy database files and stopwords to Tomcat's working directory
cp searchengine_db.* $CATALINA_HOME/bin/
cp src/main/resources/stopwords.txt $CATALINA_HOME/bin/

# Start Tomcat
$CATALINA_HOME/bin/startup.sh        # Linux / macOS
$CATALINA_HOME/bin/startup.bat       # Windows
```

Access at:

```
http://localhost:8080/search-engine/
```

---

## Features

- **BFS Web Crawler** — crawls up to 300 pages from a configurable start URL
- **Dual Inverted Index** — separate title and body indexes stored via JDBM HTrees
- **TF-IDF + Cosine Similarity** ranking with configurable title boost factor and full-document magnitude normalization
- **PageRank** — blended into the final score (weight configurable in `Config.java`)
- **Phrase Search** — double-quoted terms, e.g. `"hong kong"`
- **Autocomplete** — keyword suggestions as you type
- **Keyword Browser** — browse all indexed keywords at `/keywords`
- **Query History** — recent searches stored locally in the browser
- **Similar Pages** — relevance feedback via top-5 keyword re-query
- **JSP Web Interface** — score, metadata, top keywords, parent/child links

---

## Usage

| Feature | How to use |
|---------|-----------|
| Keyword search | Type terms and press **Search** |
| Phrase search | Wrap phrases in double quotes: `"hong kong"` |
| Mixed query | `"hong kong" university research` |
| Autocomplete | Suggestions appear as you type |
| Browse keywords | Click **Browse Keywords** on the home page |
| Similar pages | Click **Get Similar Pages** on any result |

Results display: relevance score, page title (link), URL, last-modified date, page size, top 5 keywords, parent and child links.

---

## Project Structure

```
search-engine-project/
├── pom.xml
├── deploy.sh
├── Dockerfile
├── src/
│   ├── main/
│   │   ├── search/
│   │   │   ├── common/             # Config, JDBMManager, ExceptionHandler
│   │   │   ├── crawler/            # Spider, PageFetcher, URLValidator, PageIDMapper, LinkGraphManager
│   │   │   │   └── model/          # CrawledPage, LinkRelation
│   │   │   ├── indexer/            # Indexer, InvertedIndexManager, PorterStemmer, TextPreprocessor
│   │   │   │   └── model/          # PostingEntry, PostingList, PageMetadata
│   │   │   ├── search/             # SearchEngine, QueryParser, PhraseMatcher, Ranker, SimilarityCalculator
│   │   │   │   └── model/          # Query, PhraseTerm, SearchResult
│   │   │   ├── web/                # SearchServlet, KeywordBrowseServlet, SuggestServlet, CharacterEncodingFilter
│   │   │   └── enhancement/        # PageRankCalculator, SimilarPageRecommender
│   │   ├── resources/
│   │   │   └── stopwords.txt
│   │   └── webapp/
│   │       ├── index.jsp
│   │       ├── result.jsp
│   │       ├── keywords.jsp
│   │       └── WEB-INF/web.xml
│   └── test/
│       └── search/                 # JUnit test classes
└── target/
    └── search-engine.war           # Deployable WAR (after build)
```

---

## Notes

- **Database files (`searchengine_db.*`) are not committed** — run the crawler first.
- The Porter stemmer follows the Lab3 specification.
- The stopwords list is identical to the one provided in Lab3.
- All persistent data uses JDBM disk-based storage (10 HTrees).
- Document magnitudes are pre-computed at indexing time for correct cosine normalization (all terms, not just query terms).
