# CSIT5930 Search Engine - Presentation Slides Outline

## Slide 1: Title Slide
- **Title:** CSIT5930 Web Search Engine Project
- **Subtitle:** A Full-Featured Search Engine with BFS Crawling, TF-IDF Ranking, and Phrase Search
- **Course:** CSIT5930 - Search Engine Technologies
- **Institution:** The Hong Kong University of Science and Technology (HKUST)

---

## Slide 2: System Architecture Overview
- **High-level architecture diagram** showing four main modules:
  1. **Crawler Module** - BFS web crawling from seed URL (300 pages)
  2. **Indexer Module** - Text preprocessing and dual inverted index construction
  3. **Search Module** - Query parsing, TF-IDF scoring, cosine similarity ranking
  4. **Web Module** - JSP-based user interface for query and result display
- **Data flow:** Crawler → Indexer → JDBM Storage ← Search Engine ← Web Interface
- **JDBM as the central data store** - all core data persisted to disk (no in-memory storage)

---

## Slide 3: Index Database File Structure
- **JDBM-based persistent storage** with 8 separate HTrees:
  - `url2id` - URL to PageID mapping (bidirectional with id2url)
  - `id2url` - PageID to URL mapping
  - `parentLinks` - Child-to-parent page relationships
  - `childLinks` - Parent-to-child page relationships
  - `titleIndex` - Title inverted index (stem → PostingList)
  - `bodyIndex` - Body inverted index (stem → PostingList)
  - `pageMetadata` - Page metadata for result display
  - `systemConfig` - System counters (totalDocs, nextPageId)
- **PostingList structure:** HashMap<PageID, PostingEntry>
- **PostingEntry fields:** pageId, termFrequency, maxTermFrequency, positions[]
- **Two completely independent inverted indexes** for title and body (assignment requirement)

---

## Slide 4: Core Algorithms - Text Preprocessing
- **Pipeline (applied consistently for both indexing and querying):**
  1. Convert to lowercase
  2. Tokenize by non-alphabetic characters
  3. Filter stopwords using course-provided stopwords.txt
  4. Apply Porter Stemming Algorithm (Lab3 implementation)
  5. Record position information for phrase search
- **Position tracking** enables phrase search across both title and body

---

## Slide 5: Core Algorithms - TF-IDF and Cosine Similarity
- **TF-IDF Formula (assignment specification):**
  - `TF-IDF = (tf × idf) / max(tf)`
  - `idf = ln(N / df)`
  - tf = term frequency in document
  - max(tf) = maximum term frequency across all terms in document
  - N = total number of documents, df = document frequency
- **Cosine Similarity:**
  - `cos(q, d) = (q · d) / (|q| × |d|)`
  - Query and document represented as TF-IDF weight vectors
- **Title Boost Mechanism (assignment requirement):**
  - Effective weight = body_tfidf + title_tfidf × TITLE_BOOST_FACTOR (3.0)
  - Title matches significantly boost page ranking

---

## Slide 6: Core Algorithms - Phrase Search
- **Double-quoted phrase support:** e.g., `"hong kong" universities`
- **Implementation using position-based matching:**
  1. Parse quoted phrases from query
  2. Look up posting lists for each phrase term
  3. Find candidate documents (intersection of posting lists)
  4. Verify consecutive positions: term[i+1] at position(term[i]) + 1
- **Searches both title and body indexes** for phrase matches
- **Combined with regular term matching** for mixed queries

---

## Slide 7: BFS Crawling Strategy
- **Breadth-First Search** starting from seed URL
- **Three mandatory pre-crawl checks:**
  1. Circular link protection (skip already-processed URLs)
  2. Index existence check (allow new URLs)
  3. Page update check (re-crawl if modified since last index)
- **Metadata extraction:** title, body, links, Last-Modified, page size
- **Fallback logic:** current time if no Last-Modified header; content length from HTML if no Content-Length
- **Periodic JDBM commits** (every 10 pages) for crash recovery

---

## Slide 8: Installation and Running Steps
1. **Prerequisites:** Java 8+, Maven 3.6+, Tomcat 9+
2. **Build:** `mvn clean package`
3. **Install dependencies:** htmlparser 2.1, JDBM 1.0 (via Maven or local install)
4. **Run crawler:** `mvn exec:java -Dexec.mainClass="hk.ust.csit5930.search.crawler.Spider"`
5. **Deploy WAR:** Copy `target/search-engine.war` + DB files to Tomcat
6. **Access:** http://localhost:8080/search-engine/

---

## Slide 9: Enhancement - Get Similar Pages
- **Relevance feedback feature** (assignment bonus option 1)
- **How it works:**
  1. User clicks "Get Similar Pages" button on any search result
  2. System extracts top 5 keywords (stems) from that page's metadata
  3. Keywords are used to construct an automatic search query
  4. Results show pages similar to the selected page
- **Design principle:** Reuses existing SearchEngine logic, no code duplication
- **Practical use:** Helps users discover related content without reformulating queries

---

## Slide 10: Test Results and Demo
- **Test scenarios:**
  - Single keyword search: "algorithm" → relevant results with scores
  - Multi-keyword search: "computer science" → combined ranking
  - Phrase search: `"hong kong"` → position-verified consecutive matches
  - Mixed queries: `"data mining" research` → phrase + individual term
  - Similar pages: click button → related pages displayed
- **Performance metrics:**
  - 300 pages crawled successfully
  - Index built with dual inverted indexes
  - Sub-second search response time
- **(Include screenshots of actual search results)**

---

## Slide 11: Conclusion
- **System Strengths:**
  - Complete implementation of all assignment requirements
  - Efficient JDBM-based disk storage (no memory-only data structures)
  - Accurate phrase search with position-based matching
  - Title boost mechanism improves result relevance
  - Clean modular architecture with clear separation of concerns
- **Limitations:**
  - Single-threaded crawler (could be parallelized)
  - No incremental index updates (full re-index required)
  - Basic web UI without advanced features (pagination, autocomplete)
- **Future Improvements:**
  - Multi-threaded crawling for faster data collection
  - PageRank or HITS algorithm for link-based ranking
  - Query suggestion and auto-completion
  - Snippet generation with keyword highlighting
  - Support for non-English content and multi-language search
