# CSIT5930 Search Engine - Presentation Slides Outline

## Slide 1: Title Slide
- **Title:** CSIT5930 Web Search Engine Project
- **Subtitle:** BFS Crawling · Dual Inverted Index · TF-IDF Ranking · Phrase Search
- **Course:** CSIT5930 - Search Engine Technologies
- **Institution:** The Hong Kong University of Science and Technology (HKUST)

---

## Slide 2: System Architecture Overview
- **High-level architecture diagram** showing four main modules:
  1. **Crawler Module** — BFS crawling from seed URL (up to 300 pages)
  2. **Indexer Module** — Text preprocessing and dual inverted index construction
  3. **Search Module** — Query parsing, TF-IDF scoring, cosine similarity ranking
  4. **Web Module** — JSP-based user interface for query and result display
- **Data flow:** Crawler → Indexer → JDBM Storage ← Search Engine ← Web Interface
- **JDBM as the central data store** — 8 HTrees, all data persisted to disk

---

## — INDEXER (Slides 3–6) —

## Slide 3: Index Database File Structure
- **JDBM-based persistent storage** with 8 separate HTrees:
  - `url2id` / `id2url` — bidirectional URL ↔ PageID mapping
  - `parentLinks` / `childLinks` — link graph (child-to-parent, parent-to-child)
  - `titleIndex` — Title inverted index (stem → PostingList)
  - `bodyIndex` — Body inverted index (stem → PostingList)
  - `pageMetadata` — page title, size, last-modified, top-keywords
  - `systemConfig` — counters (totalDocs, nextPageId)
- **PostingList structure:** `HashMap<PageID, PostingEntry>`
- **PostingEntry fields:** pageId, termFrequency, maxTermFrequency, positions[ ]
- **Two completely independent inverted indexes** for title and body (assignment requirement)

---

## Slide 4: Core Algorithms — Text Preprocessing
- **Pipeline (applied consistently for both indexing and querying):**
  1. Convert to lowercase
  2. Tokenize by non-alphabetic characters
  3. Filter stopwords using course-provided `stopwords.txt`
  4. Apply Porter Stemming Algorithm (Lab3 implementation)
  5. Record position information for phrase search
- **Position tracking** enables phrase search across both title and body
- **Dual pipeline** — same preprocessing used at index time and query time, guaranteeing stem consistency

---

## Slide 5: Core Algorithms — TF-IDF and Cosine Similarity
- **TF-IDF Formula (assignment specification):**
  - `TF-IDF = (tf / max_tf) × ln(N / df)`
  - tf = term frequency in document; max_tf = max term frequency across all terms in document
  - N = total documents; df = document frequency
- **Cosine Similarity:**
  - `cos(q, d) = (q · d) / (|q| × |d|)`
  - Query and document represented as TF-IDF weight vectors
- **Title Boost Mechanism (assignment requirement):**
  - `effective_weight = body_tfidf + title_tfidf × TITLE_BOOST_FACTOR (3.0)`
  - Title matches are weighted 3× over body matches to improve precision

---

## Slide 6: Core Algorithms — Phrase Search
- **Double-quoted phrase support:** e.g. `"hong kong" university`
- **Implementation — position-based adjacency verification:**
  1. Parse quoted phrases and bare keywords from query
  2. Look up posting lists for each phrase token
  3. Find candidate documents (intersection of all phrase-term posting lists)
  4. Verify consecutive positions: `pos(term[i+1]) == pos(term[i]) + 1`
- **Searches both title and body indexes** for phrase matches
- **Combined ranking:** phrase matches + regular term TF-IDF scores merged by Ranker

---

## — CRAWLER (Slides 7–11) —

## Slide 7: BFS Crawling Strategy
- **Breadth-First Search** starting from a configurable seed URL
- **Queue-based traversal** ensures breadth-first ordering
- **Stops when** MAX_CRAWL_PAGES (300) reached or URL queue exhausted
- **Periodic JDBM commits** every 10 pages for crash recovery
- **Process per page:**
  1. Validate URL (3 pre-crawl checks)
  2. HEAD request to check Last-Modified
  3. Fetch page body (HTML)
  4. Index page content
  5. Enqueue child links

---

## Slide 8: Pre-Crawl Validation — Three Mandatory Checks
- **Check 1 — Circular Link Protection:** skip if URL already processed in this session (prevents infinite loops)
- **Check 2 — Index Existence:** allow if URL not yet in JDBM index (new page → always fetch)
- **Check 3 — Page Update Check:** compare HTTP `Last-Modified` with stored timestamp; skip if unchanged
- **Fallback logic:**
  - No `Last-Modified` header → use current time (always re-index)
  - No `Content-Length` → derive page size from HTML content length

---

## Slide 9: Metadata Extraction and Link Graph
- **Metadata stored per page:** title, last-modified timestamp, page size (bytes), top-5 stemmed keywords
- **Link extraction:** HTMLParser 2.1 parses `<a href>` tags; relative URLs resolved to absolute
- **Link Graph structure:**
  - `childLinks`: parent PageID → list of child PageIDs
  - `parentLinks`: child PageID → list of parent PageIDs
- **PageID assignment:** sequential integers via `PageIDMapper`; bidirectional URL↔ID maps in JDBM

---

## Slide 10: Crawler Profiling
- *(performance table / chart from original slides — 300 pages, timing data, network stats)*

---

## Slide 11: Enhancement — PageRank
- **Link-based authority scoring** computed after crawl completes
- **Algorithm:** iterative power-iteration PageRank on the crawled link graph
  - Damping factor: 0.85 (standard)
  - Convergence threshold: 1e-6
- **Blended into final score:** `final_score = cosine_similarity × (1 - PR_WEIGHT) + pagerank × PR_WEIGHT`
- **PR_WEIGHT configurable** in `Config.java`; default blends TF-IDF and link authority
- **Practical effect:** authority pages (many inbound links) rank higher for relevant queries

---

## — FRONTEND (Slides 12–16) —

## Slide 12: Web Interface — Servlets and JSP
- **`SearchServlet`** handles `GET /search`:
  - Parses `query` parameter → invokes `SearchEngine.search()`
  - Parses `similar` parameter → invokes `SimilarPageRecommender`
  - Forwards `results`, `query`, `resultCount` attributes to `result.jsp`
- **`KeywordBrowseServlet`** handles `GET /keywords` — paginated keyword index
- **`SuggestServlet`** handles `GET /suggest` — JSON autocomplete endpoint
- **`CharacterEncodingFilter`** — enforces UTF-8 on all requests and responses
- **JSP pages:** `index.jsp` (search form), `result.jsp` (results), `keywords.jsp` (browser)

---

## Slide 13: Search Result Display
- **Each result card shows:**
  - Relevance score (TF-IDF + PageRank blend)
  - Page title (clickable link) and URL
  - Last-modified date and page size
  - Top 5 keywords (stemmed)
  - Parent links and child links
- **Result count** displayed above the list
- **"Get Similar Pages"** button on each result card triggers relevance feedback

---

## Slide 14: Enhancement — Autocomplete
- **Keyword suggestions** as user types in the search box
- **`SuggestServlet`** queries the JDBM `titleIndex` and `bodyIndex` for prefix matches
- **Returns JSON array** of matching stems, displayed as a dropdown
- **Improves discoverability** — users can see indexed vocabulary before committing to a query

---

## Slide 15: Enhancement — Similar Pages
- **Relevance feedback** via top-5 keyword re-query
- **How it works:**
  1. User clicks **"Get Similar Pages"** on any result
  2. `SimilarPageRecommender` reads the page's top-5 stored keywords from `pageMetadata`
  3. Keywords are joined into an automatic search query
  4. Results display pages similar to the selected page
- **Design principle:** fully reuses `SearchEngine.search()` — zero code duplication
- **Display query:** `"Similar pages for: <keyword1> <keyword2> ..."`

---

## Slide 16: Enhancement — Query History
- **Recent searches stored locally** in the browser (`localStorage`)
- **Displayed on the homepage** below the search box for quick re-access
- **Client-side only** — no server storage, respects user privacy
- **Up to N recent queries** shown; oldest queries drop off automatically

---

## — WRAP-UP —

## Slide 17: Demo
- *(Live demo)*
- **Scenarios to walk through:**
  - Keyword search: `algorithm`
  - Phrase search: `"hong kong"`
  - Mixed query: `"data mining" research`
  - Autocomplete: start typing `comp...`
  - Similar pages: click "Get Similar Pages"
  - Keyword browser: `/keywords`

---

## Slide 18: Conclusion
- **System Strengths:**
  - Complete implementation of all assignment requirements
  - Efficient JDBM-based disk storage — no in-memory-only data structures
  - Accurate phrase search with position-based adjacency verification
  - Title boost + PageRank blend for improved result relevance
  - Clean modular architecture: Crawler / Indexer / Search / Web
- **Limitations:**
  - Single-threaded crawler (could be parallelized for speed)
  - No incremental index updates (re-crawl required for changes)
- **Future Improvements:**
  - Multi-threaded / distributed crawling
  - HITS algorithm for hub/authority scoring
  - Snippet generation with keyword highlighting
  - Multi-language support
