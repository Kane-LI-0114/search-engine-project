package search.crawler;

import search.common.Config;
import search.common.ExceptionHandler;
import search.crawler.model.CrawledPage;

import org.htmlparser.Parser;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.lexer.Lexer;
import org.htmlparser.lexer.Page;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Handles HTTP page fetching, metadata extraction, and HTML parsing.
 * Uses HttpURLConnection for HTTP metadata (Last-Modified, Content-Length)
 * and the htmlparser library for content extraction (title, body, links).
 */
public class PageFetcher {

    /**
     * Fetches a web page and extracts all required data including title,
     * body text, outgoing links, and HTTP metadata.
     *
     * @param url the URL to fetch
     * @return a CrawledPage containing all extracted data
     * @throws Exception if the page cannot be fetched or parsed
     */
    public CrawledPage fetchPage(String url) throws Exception {
        // Step 1: Fetch page via HTTP and extract metadata
        HttpURLConnection conn = null;
        String htmlContent;
        long lastModified;
        long pageSize;

        try {
            URL urlObj = new URL(url);
            conn = (HttpURLConnection) urlObj.openConnection();
            conn.setConnectTimeout(Config.CRAWL_TIMEOUT);
            conn.setReadTimeout(Config.CRAWL_TIMEOUT);
            conn.setRequestProperty("User-Agent", "CSIT5930-SearchEngine/1.0");
            conn.setInstanceFollowRedirects(true);

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP response code: " + responseCode);
            }

            // Extract HTTP metadata from response headers
            lastModified = conn.getLastModified();
            pageSize = conn.getContentLength();

            // Read HTML content from response body
            htmlContent = readStream(conn.getInputStream());

            // Fallback: use current time if no Last-Modified header
            if (lastModified <= 0) {
                lastModified = System.currentTimeMillis();
            }
            // Fallback: use content string length if Content-Length not provided
            if (pageSize <= 0) {
                pageSize = htmlContent.length();
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        // Step 2: Parse HTML with htmlparser library
        Parser parser = new Parser(new Lexer(new Page(htmlContent, "UTF-8")));
        NodeList nodeList = parser.parse(null);

        // Extract page title from <title> tag
        String title = extractTitle(nodeList, url);

        // Extract plain text body content
        String body = extractBody(nodeList);

        // Extract and normalize child links
        List<String> childLinks = extractLinks(nodeList, url);

        return new CrawledPage(url, title, body, lastModified, pageSize, childLinks);
    }

    /**
     * Returns the HTTP Last-Modified timestamp for a URL via HEAD request.
     * Used for page update checking without downloading the full page.
     *
     * @param url the URL to check
     * @return the last modified timestamp, or 0 if unavailable
     */
    public long getLastModified(String url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(Config.CRAWL_TIMEOUT);
            conn.setReadTimeout(Config.CRAWL_TIMEOUT);
            conn.setRequestMethod("HEAD");
            long lastModified = conn.getLastModified();
            conn.disconnect();
            return lastModified;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Extracts the page title from parsed HTML nodes.
     * Falls back to the URL if no title tag is found.
     *
     * @param nodeList the parsed HTML node list
     * @param url      the page URL (used as fallback title)
     * @return the extracted title text
     */
    private String extractTitle(NodeList nodeList, String url) {
        try {
            NodeList titleNodes = nodeList.extractAllNodesThatMatch(
                new TagNameFilter("title"), true);
            if (titleNodes != null && titleNodes.size() > 0) {
                String title = titleNodes.elementAt(0).toPlainTextString().trim();
                if (!title.isEmpty()) {
                    return title;
                }
            }
        } catch (Exception e) {
            ExceptionHandler.warn("Failed to extract title from " + url);
        }
        return url; // Fallback to URL as title
    }

    /**
     * Extracts plain text body content from parsed HTML nodes.
     * Extracts from <body> tag if present, otherwise uses all text content.
     *
     * @param nodeList the parsed HTML node list
     * @return the extracted body text
     */
    private String extractBody(NodeList nodeList) {
        try {
            // Try to extract from <body> tag specifically
            NodeList bodyNodes = nodeList.extractAllNodesThatMatch(
                new TagNameFilter("body"), true);
            if (bodyNodes != null && bodyNodes.size() > 0) {
                return bodyNodes.elementAt(0).toPlainTextString().trim();
            }
            // Fallback: use all text content by visiting each node
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < nodeList.size(); i++) {
                sb.append(nodeList.elementAt(i).toPlainTextString());
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Extracts and normalizes all outgoing links from parsed HTML nodes.
     * Performs the following normalization:
     * - Converts relative URLs to absolute using the base URL
     * - Filters out non-HTTP/HTTPS protocols
     * - Removes fragment identifiers (#anchors)
     * - Skips invalid/malformed URLs
     *
     * @param nodeList the parsed HTML node list
     * @param baseUrl  the base URL for resolving relative links
     * @return list of normalized absolute URLs
     */
    private List<String> extractLinks(NodeList nodeList, String baseUrl) {
        List<String> links = new ArrayList<>();
        try {
            NodeList linkNodes = nodeList.extractAllNodesThatMatch(
                new NodeClassFilter(LinkTag.class), true);
            if (linkNodes == null) {
                return links;
            }

            URL base = new URL(baseUrl);
            for (int i = 0; i < linkNodes.size(); i++) {
                try {
                    LinkTag linkTag = (LinkTag) linkNodes.elementAt(i);
                    String href = linkTag.getLink();
                    if (href == null || href.trim().isEmpty()) {
                        continue;
                    }

                    // Resolve relative URL to absolute
                    String absoluteUrl = new URL(base, href).toString();

                    // Filter: only keep http/https URLs
                    if (!absoluteUrl.startsWith("http://") &&
                        !absoluteUrl.startsWith("https://")) {
                        continue;
                    }

                    // Remove fragment identifier (#)
                    int hashIndex = absoluteUrl.indexOf('#');
                    if (hashIndex > 0) {
                        absoluteUrl = absoluteUrl.substring(0, hashIndex);
                    }

                    if (!absoluteUrl.trim().isEmpty()) {
                        links.add(absoluteUrl);
                    }
                } catch (Exception e) {
                    // Skip invalid links silently
                }
            }
        } catch (Exception e) {
            ExceptionHandler.warn("Failed to extract links from " + baseUrl);
        }
        return links;
    }

    /**
     * Reads an InputStream into a String using UTF-8 encoding.
     *
     * @param is the input stream to read
     * @return the string content
     * @throws IOException if reading fails
     */
    private String readStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, "UTF-8"))) {
            char[] buffer = new char[4096];
            int charsRead;
            while ((charsRead = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, charsRead);
            }
        }
        return sb.toString();
    }
}
