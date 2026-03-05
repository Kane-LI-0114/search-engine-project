package hk.ust.csit5930.search.web;

import hk.ust.csit5930.search.common.ExceptionHandler;
import hk.ust.csit5930.search.enhancement.SimilarPageRecommender;
import hk.ust.csit5930.search.search.SearchEngine;
import hk.ust.csit5930.search.search.model.SearchResult;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Servlet handling search requests from the web interface.
 * Receives GET requests with the 'query' parameter, invokes the SearchEngine,
 * and forwards results to result.jsp for display.
 * Also handles "get similar pages" requests for the enhancement feature.
 */
@WebServlet(urlPatterns = {"/search"})
public class SearchServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private SearchEngine searchEngine;
    private SimilarPageRecommender recommender;

    /**
     * Initializes the SearchEngine and SimilarPageRecommender on servlet startup.
     */
    @Override
    public void init() throws ServletException {
        try {
            searchEngine = new SearchEngine();
            recommender = new SimilarPageRecommender();
        } catch (Exception e) {
            throw new ServletException("Failed to initialize SearchEngine", e);
        }
    }

    /**
     * Handles GET search requests.
     * Supports two modes:
     * 1. Normal search: parameter 'query' contains the search terms
     * 2. Similar pages: parameter 'similar' contains a PageID for recommendation
     *
     * @param request  the HTTP request
     * @param response the HTTP response
     * @throws ServletException if servlet processing fails
     * @throws IOException      if I/O errors occur
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");

        String queryStr = request.getParameter("query");
        String similarPageId = request.getParameter("similar");
        List<SearchResult> results = new ArrayList<>();

        try {
            if (similarPageId != null && !similarPageId.trim().isEmpty()) {
                // Enhancement: "get similar pages" mode
                int pageId = Integer.parseInt(similarPageId.trim());
                results = recommender.getSimilarPages(pageId);
                String keywordQuery = recommender.buildKeywordQuery(pageId);
                queryStr = "Similar pages for: " + keywordQuery;
            } else if (queryStr != null && !queryStr.trim().isEmpty()) {
                // Normal search mode
                results = searchEngine.search(queryStr.trim());
            }
        } catch (Exception e) {
            ExceptionHandler.handleError("Search error for query: " + queryStr, e);
            request.setAttribute("error",
                "An error occurred during search: " + e.getMessage());
        }

        // Forward results to JSP for rendering
        request.setAttribute("query", queryStr);
        request.setAttribute("results", results);
        request.setAttribute("resultCount", results.size());

        request.getRequestDispatcher("/result.jsp").forward(request, response);
    }
}
