<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.*" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="hk.ust.csit5930.search.search.model.SearchResult" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Search Results - CSIT5930 Search Engine</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 20px 40px;
            background-color: #f5f5f5;
        }
        .header {
            padding: 10px 0;
            border-bottom: 1px solid #ccc;
            margin-bottom: 20px;
        }
        .header h1 {
            display: inline;
            font-size: 22px;
            color: #333;
        }
        .search-form {
            display: inline-block;
            margin-left: 20px;
        }
        .search-input {
            width: 400px;
            padding: 8px 12px;
            font-size: 14px;
            border: 1px solid #ccc;
            border-radius: 20px;
        }
        .search-btn {
            padding: 8px 16px;
            font-size: 14px;
            background-color: #4285f4;
            color: white;
            border: none;
            border-radius: 20px;
            cursor: pointer;
        }
        .result-summary {
            color: #666;
            font-size: 14px;
            margin-bottom: 20px;
        }
        .result-item {
            background: white;
            padding: 15px 20px;
            margin-bottom: 15px;
            border-radius: 8px;
            border: 1px solid #e0e0e0;
        }
        .result-score {
            color: #888;
            font-size: 13px;
            margin-bottom: 4px;
        }
        .result-title a {
            font-size: 18px;
            color: #1a0dab;
            text-decoration: none;
        }
        .result-title a:hover {
            text-decoration: underline;
        }
        .result-url {
            color: #006621;
            font-size: 13px;
            margin: 4px 0;
        }
        .result-meta {
            color: #555;
            font-size: 13px;
            margin: 5px 0;
        }
        .result-keywords {
            color: #333;
            font-size: 13px;
            margin: 5px 0;
        }
        .result-links {
            font-size: 12px;
            color: #555;
            margin: 5px 0;
            word-break: break-all;
        }
        .result-links a {
            color: #1a73e8;
            text-decoration: none;
        }
        .result-links a:hover {
            text-decoration: underline;
        }
        .similar-btn {
            display: inline-block;
            margin-top: 8px;
            padding: 4px 12px;
            font-size: 12px;
            color: #4285f4;
            border: 1px solid #4285f4;
            border-radius: 12px;
            text-decoration: none;
            background: white;
        }
        .similar-btn:hover {
            background-color: #4285f4;
            color: white;
        }
        .label {
            font-weight: bold;
            color: #444;
        }
        .error {
            color: #d32f2f;
            padding: 10px;
            background: #fce4ec;
            border-radius: 4px;
            margin: 10px 0;
        }
        .no-results {
            color: #666;
            font-size: 16px;
            padding: 20px;
        }
    </style>
</head>
<body>
    <!-- Header with search form -->
    <div class="header">
        <h1>CSIT5930 Search Engine</h1>
        <form class="search-form" action="search" method="GET">
            <input type="text" name="query" class="search-input"
                   value="<%= request.getAttribute("query") != null ? request.getAttribute("query").toString().replace("\"", "&quot;") : "" %>"
                   placeholder="Enter your search query...">
            <button type="submit" class="search-btn">Search</button>
        </form>
    </div>

    <%
        // Display error message if present
        String error = (String) request.getAttribute("error");
        if (error != null) {
    %>
        <div class="error"><%= error %></div>
    <% } %>

    <%
        String query = (String) request.getAttribute("query");
        @SuppressWarnings("unchecked")
        List<SearchResult> results = (List<SearchResult>) request.getAttribute("results");
        Integer resultCount = (Integer) request.getAttribute("resultCount");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    %>

    <% if (query != null && !query.isEmpty()) { %>
        <div class="result-summary">
            About <strong><%= resultCount != null ? resultCount : 0 %></strong> results found
            for "<strong><%= query %></strong>"
        </div>

        <% if (results != null && !results.isEmpty()) { %>
            <% for (int idx = 0; idx < results.size(); idx++) {
                SearchResult result = results.get(idx);
            %>
                <div class="result-item">
                    <!-- 1. Document score (6 decimal places) -->
                    <div class="result-score">
                        Score: <%= String.format("%.6f", result.getScore()) %>
                    </div>

                    <!-- 2. Page title as hyperlink to original URL -->
                    <div class="result-title">
                        <a href="<%= result.getUrl() %>" target="_blank">
                            <%= result.getTitle() %>
                        </a>
                    </div>

                    <!-- 3. Page URL plain text -->
                    <div class="result-url">
                        <%= result.getUrl() %>
                    </div>

                    <!-- 4. Last modified date and page size -->
                    <div class="result-meta">
                        Last Modified: <%= dateFormat.format(new java.util.Date(result.getLastModified())) %>
                        | Page Size: <%= result.getPageSize() %> characters
                    </div>

                    <!-- 5. Top keywords with frequencies -->
                    <div class="result-keywords">
                        <span class="label">Keywords:</span>
                        <%
                            LinkedHashMap<String, Integer> keywords = result.getTopKeywords();
                            if (keywords != null && !keywords.isEmpty()) {
                                StringBuilder kwBuilder = new StringBuilder();
                                for (Map.Entry<String, Integer> kwEntry : keywords.entrySet()) {
                                    if (kwBuilder.length() > 0) {
                                        kwBuilder.append("; ");
                                    }
                                    kwBuilder.append(kwEntry.getKey()).append(" ")
                                             .append(kwEntry.getValue());
                                }
                        %>
                            <%= kwBuilder.toString() %>
                        <% } else { %>
                            (none)
                        <% } %>
                    </div>

                    <!-- 6. Parent links -->
                    <div class="result-links">
                        <span class="label">Parent Links:</span>
                        <%
                            List<String> parentLinks = result.getParentLinks();
                            if (parentLinks != null && !parentLinks.isEmpty()) {
                                for (int p = 0; p < parentLinks.size(); p++) {
                                    String pLink = parentLinks.get(p);
                        %>
                            <a href="<%= pLink %>" target="_blank"><%= pLink %></a>
                            <% if (p < parentLinks.size() - 1) { %>, <% } %>
                        <%      }
                            } else { %>
                            (none)
                        <% } %>
                    </div>

                    <!-- 7. Child links -->
                    <div class="result-links">
                        <span class="label">Child Links:</span>
                        <%
                            List<String> childLinks = result.getChildLinks();
                            if (childLinks != null && !childLinks.isEmpty()) {
                                for (int c = 0; c < childLinks.size(); c++) {
                                    String cLink = childLinks.get(c);
                        %>
                            <a href="<%= cLink %>" target="_blank"><%= cLink %></a>
                            <% if (c < childLinks.size() - 1) { %>, <% } %>
                        <%      }
                            } else { %>
                            (none)
                        <% } %>
                    </div>

                    <!-- Enhancement: "Get Similar Pages" button -->
                    <a class="similar-btn" href="search?similar=<%= result.getPageId() %>">
                        Get Similar Pages
                    </a>
                </div>
            <% } %>
        <% } else { %>
            <div class="no-results">
                No results found for your query. Try different keywords or check your spelling.
            </div>
        <% } %>
    <% } %>
</body>
</html>
