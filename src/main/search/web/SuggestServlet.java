package search.web;

import search.common.ExceptionHandler;
import search.common.JDBMManager;

import jdbm.helper.FastIterator;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Enhancement 3: Autocomplete suggestions endpoint.
 * GET /suggest?q=<prefix>
 * Returns a JSON array of up to 10 matching stemmed keywords.
 */
public class SuggestServlet extends HttpServlet {

    private static final long serialVersionUID = 3L;
    private static final int MAX_SUGGESTIONS = 10;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");

        String prefix = request.getParameter("q");
        PrintWriter out = response.getWriter();

        if (prefix == null || prefix.trim().isEmpty()) {
            out.print("[]");
            return;
        }

        String lowerPrefix = prefix.trim().toLowerCase();
        List<String> matches = new ArrayList<String>();

        try {
            JDBMManager dbManager = JDBMManager.getInstance();
            FastIterator iter = dbManager.getBodyIndex().keys();
            Object key;
            while ((key = iter.next()) != null) {
                String stem = (String) key;
                if (stem.toLowerCase().startsWith(lowerPrefix)) {
                    matches.add(stem);
                    if (matches.size() >= MAX_SUGGESTIONS * 3) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            ExceptionHandler.handleError("SuggestServlet error", e);
            out.print("[]");
            return;
        }

        Collections.sort(matches);
        if (matches.size() > MAX_SUGGESTIONS) {
            matches = matches.subList(0, MAX_SUGGESTIONS);
        }

        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < matches.size(); i++) {
            if (i > 0) json.append(",");
            json.append("\"").append(jsonEscape(matches.get(i))).append("\"");
        }
        json.append("]");
        out.print(json.toString());
    }

    private String jsonEscape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
