package search.web;

import search.common.ExceptionHandler;
import search.common.JDBMManager;

import jdbm.helper.FastIterator;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * Enhancement 2: Keyword Browse page.
 * GET /keywords            → all stemmed keywords grouped A-Z
 * GET /keywords?letter=A   → only keywords starting with 'A'
 */
public class KeywordBrowseServlet extends HttpServlet {

    private static final long serialVersionUID = 2L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String letterParam = request.getParameter("letter");

        List<String> allStems = new ArrayList<String>();
        try {
            JDBMManager dbManager = JDBMManager.getInstance();
            FastIterator iter = dbManager.getBodyIndex().keys();
            Object key;
            while ((key = iter.next()) != null) {
                allStems.add((String) key);
            }
        } catch (Exception e) {
            ExceptionHandler.handleError("KeywordBrowse: failed to read index", e);
            request.setAttribute("error", "Failed to load keywords: " + e.getMessage());
            request.getRequestDispatcher("/keywords.jsp").forward(request, response);
            return;
        }

        Collections.sort(allStems, String.CASE_INSENSITIVE_ORDER);

        // Group by first letter into a TreeMap (auto-sorted A-Z)
        TreeMap<Character, List<String>> grouped = new TreeMap<Character, List<String>>();
        for (String stem : allStems) {
            char first = stem.isEmpty() ? '#' : Character.toUpperCase(stem.charAt(0));
            if (!Character.isLetter(first)) first = '#';
            if (!grouped.containsKey(first)) {
                grouped.put(first, new ArrayList<String>());
            }
            grouped.get(first).add(stem);
        }

        Character activeLetter = null;
        if (letterParam != null && !letterParam.trim().isEmpty()) {
            activeLetter = Character.toUpperCase(letterParam.trim().charAt(0));
        }

        request.setAttribute("grouped", grouped);
        request.setAttribute("activeLetter", activeLetter);
        request.setAttribute("totalCount", allStems.size());
        request.getRequestDispatcher("/keywords.jsp").forward(request, response);
    }
}
