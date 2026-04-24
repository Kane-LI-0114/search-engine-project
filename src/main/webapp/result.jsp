<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.*" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="search.search.model.SearchResult" %>

<%!
    // Simple HTML escaper to avoid introducing dependency on external libs.
    public static String esc(Object o) {
        if (o == null) return "";
        String s = String.valueOf(o);
        StringBuilder sb = new StringBuilder(s.length() * 2);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#39;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
%>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Search Results - CSIT5930 Search Engine</title>
    <meta name="viewport" content="width=device-width,initial-scale=1">

    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;600&display=swap" rel="stylesheet">
    <style>
        :root{
            --bg: #f5f7fb;
            --card: #ffffff;
            --text: #0f1724;
            --muted: #6b7280;
            --accent: #2563eb;
            --input-bg: #f8fafc;
            --radius: 14px;
        }
        .theme-dark {
            --bg: #071028;
            --card: rgba(255,255,255,0.03);
            --text: #e6eef8;
            --muted: #9aa4b2;
            --accent: #4f8cff;
            --input-bg: rgba(255,255,255,0.02);
        }

        *{box-sizing:border-box}
        html,body{height:100%;margin:0;font-family:"Inter",system-ui,-apple-system,Segoe UI,Roboto,Helvetica,Arial; -webkit-font-smoothing:antialiased; -moz-osx-font-smoothing:grayscale;}
        body{
            background: var(--bg);
            color: var(--text);
            display:flex;
            align-items:flex-start;
            justify-content:center;
            padding:36px;
            transition: background .25s ease, color .25s ease;
        }

        .container{
            width:100%;
            max-width:980px;
            background: linear-gradient(180deg, rgba(255,255,255,0.85), rgba(255,255,255,0.8));
            border-radius: 16px;
            padding:22px;
            box-shadow: 0 10px 30px rgba(16,24,40,0.08);
            position:relative;
        }
        .theme-dark .container{
            background: linear-gradient(180deg, rgba(255,255,255,0.02), rgba(255,255,255,0.01));
            box-shadow: 0 12px 40px rgba(2,8,23,0.6);
        }

        header{
            display:flex;
            align-items:center;
            justify-content:space-between;
            gap:12px;
            margin-bottom:18px;
        }
        .brand{
            display:flex;
            align-items:center;
            gap:12px;
        }
        .logo{
            width:56px;height:56px;border-radius:12px;
            background: linear-gradient(135deg,var(--accent), #7cc3ff 100%);
            display:flex;align-items:center;justify-content:center;
            font-weight:700;color:#042033;font-size:18px;
            box-shadow: 0 8px 20px rgba(37,99,235,0.12);
        }
        .title h1{margin:0;font-size:20px;}
        .title p{margin:2px 0 0;color:var(--muted);font-size:13px;}
        .theme-toggle{
            display:inline-flex;
            align-items:center;
            gap:8px;
            background:transparent;
            border:2px solid rgba(0,0,0,0.06);
            padding:8px 12px;
            border-radius:999px;
            cursor:pointer;
            font-size:13px;
            color:var(--text);
        }
        .theme-dark .theme-toggle{
            border:2px solid rgba(255,255,255,0.06);
            color:var(--text);
        }

        .search-area{
            display:flex;
            align-items:center;
            gap:12px;
            margin-top:8px;
            margin-bottom:18px;
        }
        .input{ position:relative; flex:1; }
        .search-input{
            width:100%;
            padding:12px 46px 12px 46px;
            border-radius:12px;
            border:1px solid rgba(15,23,36,0.06);
            background: var(--input-bg);
            color: var(--text);
            font-size:15px;
            outline:none;
        }
        .theme-dark .search-input{ border:1px solid rgba(255,255,255,0.04); }

        .icon-left, .icon-right {
            position:absolute;
            top:50%;
            transform:translateY(-50%);
            display:flex;
            align-items:center;
            justify-content:center;
            color:var(--muted);
            pointer-events:none;
        }
        .icon-left{ left:12px; }
        .icon-right{ right:12px; }

        .search-btn{
            padding:10px 16px;
            border-radius:12px;
            border:none;
            background: linear-gradient(90deg,var(--accent), #7dd3fc 120%);
            color:#042033;
            font-weight:600;
            cursor:pointer;
            box-shadow: 0 8px 26px rgba(37,99,235,0.12);
        }

        .result-summary{ color:var(--muted); font-size:14px; margin-bottom:12px; }
        .results-list{ display:flex; flex-direction:column; gap:14px; }

        .result-item{
            background: var(--card);
            padding:14px 18px;
            border-radius:12px;
            border:3px solid rgba(0,0,0,0.04);
            transition: box-shadow .12s ease;
        }
        .theme-dark .result-item{ border:2px solid rgba(255,255,255,0.03); }

        .result-score{ color:var(--muted); font-size:13px; margin-bottom:6px; }
        .result-title a{ font-size:18px; color: #1a0dab; text-decoration:none; }
        .theme-dark .result-title a{ color: #9cc4ff; }
        .result-title a:hover{text-decoration:underline;}
        .result-url{ color:#006621; font-size:13px; margin:6px 0; word-break:break-all; }
        .theme-dark .result-url{ color:#7be3a8; }

        .result-meta{ color:var(--muted); font-size:13px; margin:6px 0; }
        .result-keywords{ color:var(--text); font-size:13px; margin:6px 0; }
        .result-links{ font-size:13px; color:var(--muted); margin:6px 0; word-break:break-all; display:flex; flex-wrap:wrap; align-items:center; gap:8px; }
        .result-links .label { font-weight:600; color:var(--muted); margin-right:6px; flex:0 0 auto; }
        .link-list { display:inline-flex; flex-wrap:wrap; gap:8px; align-items:center; }
        .link-item { display:inline-block; margin-right:6px; }
        .link-item a { color:#1a73e8; text-decoration:none; font-size:13px; }
        .theme-dark .link-item a { color:#9cc4ff; }
        .link-toggle-btn {
            background: transparent;
            border: 2px solid rgba(0,0,0,0.1);
            color: var(--muted);
            padding: 6px 10px;
            border-radius: 10px;
            font-size: 13px;
            cursor: pointer;
            margin-left:6px;
        }
        .theme-dark .link-toggle-btn {
            border-color: rgba(255,255,255,0.08);
            color: var(--muted);
        }
        .link-list.collapsed .link-item:nth-child(n+5) { display: none; }
        .link-toggle-btn[aria-hidden="true"] { display: none; }

        .similar-btn{
            display:inline-block;
            margin-top:8px;
            padding:6px 12px;
            font-size:13px;
            color:var(--accent);
            border:1px solid var(--accent);
            border-radius:12px;
            text-decoration:none;
            background:transparent;
        }
        .theme-dark .similar-btn:hover{
            background:var(--accent);
            color:#042033;
        }

        .similar-btn:hover{
            background: rgb(37 99 235 / 0.6);
            color:#042033;
        }

        .no-results{ color:var(--muted); font-size:16px; padding:18px; }
        .error{ color:#d32f2f; padding:10px; background:#fce4ec; border-radius:6px; margin:10px 0; }

        @media (max-width:640px){
            .container{ padding:14px; border-radius:12px; }
            .logo{ width:48px;height:48px;font-size:16px; }
            .search-input{ padding:10px 44px 10px 44px; font-size:14px; }
            header{flex-direction:column;align-items:flex-start;gap:10px;}
            .result-links { gap:6px; }
            .link-toggle-btn { padding:5px 8px; font-size:12px; }
        }

        /* simple theme transition: only active while html has .theme-animating */
        html.theme-animating body,
        html.theme-animating .container,
        html.theme-animating .search-input,
        html.theme-animating .result-item,
        html.theme-animating .logo,
        html.theme-animating .search-btn,
        html.theme-animating .theme-toggle,
        html.theme-animating .result-title a,
        html.theme-animating .result-links a {
            transition: background-color 1s ease, color 1s ease, border-color 1s ease, box-shadow 1s ease, fill 1s ease;
        }
    </style></head>
<body>
<div class="container" role="main" aria-labelledby="main-title">
    <header>
        <div class="brand" aria-hidden="false">
            <div class="logo">CS</div>
            <div class="title">
                <h1 id="main-title">CSIT5930 Search Engine</h1>
                <p>HKUST - Web Search Engine Project</p>
            </div>
        </div>

        <button id="themeToggle" class="theme-toggle" aria-pressed="false" title="Toggle light/dark">
            <span id="themeIcon">🌙</span>
            <span id="themeLabel">Dark</span>
        </button>
    </header>

    <!-- Search form -->
    <div class="search-area">
        <form action="search" method="GET" style="width:100%;display:flex;gap:12px;align-items:center;">
            <div class="input" style="flex:1;">
<span class="icon-left" aria-hidden="true">
<svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
<path d="M21 21L16.65 16.65" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
<circle cx="11" cy="11" r="6" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
</svg>
</span>

                <input type="text"
                       name="query"
                       class="search-input"
                       placeholder="Enter your search query..."
                       autofocus
                       aria-label="Search query"
                       value="<%= esc(request.getAttribute("query")) %>" />

                <%--                <span class="icon-right" aria-hidden="true">--%>
                <%--<svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">--%>
                <%--<path d="M12 17v.01" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>--%>
                <%--<path d="M11 7h1v6" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>--%>
                <%--<circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>--%>
                <%--</svg>--%>
                <%--</span>--%>
            </div>

            <button type="submit" class="search-btn">Search</button>
        </form>
    </div>

    <% // Error message (if any)
        String error = (String) request.getAttribute("error");
        if (error != null) {
    %>
    <div class="error"><%= esc(error) %></div>
    <% } %>

    <%
        String query = (String) request.getAttribute("query");
        @SuppressWarnings("unchecked")
        List<SearchResult> results = (List<SearchResult>) request.getAttribute("results");
        Integer resultCount = (Integer) request.getAttribute("resultCount");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    %>

    <% if (query != null && !query.trim().isEmpty()) { %>
    <div class="result-summary">
        About <strong><%= resultCount != null ? resultCount : 0 %></strong> results found for "<strong><%= esc(query) %></strong>"
    </div>

    <div class="results-list">
        <% if (results != null && !results.isEmpty()) {
            for (int i = 0; i < results.size(); i++) {
                SearchResult r = results.get(i);
        %>
        <div class="result-item" role="article" aria-label="Search result">
            <div class="result-score">Score: <%= String.format("%.6f", r.getScore()) %></div>

            <div class="result-title">
                <a href="<%= esc(r.getUrl()) %>" target="_blank" rel="noopener noreferrer">
                    <%= esc(r.getTitle()) %>
                </a>
            </div>

            <div class="result-url"><%= esc(r.getUrl()) %></div>

            <div class="result-meta">
                Last Modified: <%= esc(dateFormat.format(new java.util.Date(r.getLastModified()))) %>
                | Page Size: <%= r.getPageSize() %> characters
            </div>

            <div class="result-keywords">
                <span style="font-weight:600;color:var(--muted);">Keywords:</span>
                <%
                    LinkedHashMap<String, Integer> keywords = r.getTopKeywords();
                    if (keywords != null && !keywords.isEmpty()) {
                        StringBuilder kb = new StringBuilder();
                        for (Map.Entry<String,Integer> e : keywords.entrySet()) {
                            if (kb.length() > 0) kb.append("; ");
                            kb.append(esc(e.getKey())).append(" ").append(e.getValue());
                        }
                %>
                <%= kb.toString() %>
                <% } else { %>
                (none)
                <% } %>
            </div>

            <div class="result-links" data-type="parents">
                <span class="label">Parent Links:</span>
                <%
                    List<String> parents = r.getParentLinks();
                    if (parents != null && !parents.isEmpty()) {
                %>
                <span class="link-list" aria-live="polite">
                        <%
                            for (int p=0; p<parents.size(); p++) {
                                String pl = parents.get(p);
                        %>
<span class="link-item">
<a href="<%= esc(pl) %>" target="_blank" rel="noopener noreferrer"><%= esc(pl) %></a>
</span>
<% } %>
</span>
                <button type="button" class="link-toggle-btn" aria-expanded="false" aria-hidden="true">Show more</button>
                <% } else { %>
                (none)
                <% } %>
            </div>

            <div class="result-links" data-type="children">
                <span class="label">Child Links:</span>
                <%
                    List<String> childs = r.getChildLinks();
                    if (childs != null && !childs.isEmpty()) {
                %>
                <span class="link-list" aria-live="polite">
<% for (int c=0; c<childs.size(); c++) {
    String cl = childs.get(c);
%>
<span class="link-item">
<a href="<%= esc(cl) %>" target="_blank" rel="noopener noreferrer"><%= esc(cl) %></a>
</span>
<% } %>
</span>
                <button type="button" class="link-toggle-btn" aria-expanded="false" aria-hidden="true">Show more</button>
                <% } else { %>
                (none)
                <% } %>
            </div>

            <!-- Keep "Get Similar Pages" link as it existed in original output (no new behaviors added) -->
            <a class="similar-btn" href="search?similar=<%= esc(r.getPageId()) %>">Get Similar Pages</a>
        </div>
        <% } // end for
        } else { %>
        <div class="no-results">No results found for your query. Try different keywords or check your spelling.</div>
        <% } %>
    </div>
    <% } %></div>

<script>
    (function(){
        const rootEl = document.documentElement;
        const toggle = document.getElementById('themeToggle');
        const icon = document.getElementById('themeIcon');
        const label = document.getElementById('themeLabel');
        const LS_KEY = 'csit5930_theme_v1';
        const ANIM_CLASS = 'theme-animating';
        const ANIM_MS = 1100; // should be >= CSS transition duration

        function updateToggleUI(isDark) {
            if (toggle) toggle.setAttribute('aria-pressed', isDark ? 'true' : 'false');
            if (icon) icon.textContent = isDark ? '☀️' : '🌙';
            if (label) label.textContent = isDark ? 'Light' : 'Dark';
        }

        // apply theme, animate: boolean (true for user toggle).
        function applyTheme(theme, animate) {
            try {
                if (animate) {
                    // ensure transition rules are active before changing theme
                    rootEl.classList.add(ANIM_CLASS);
                    // next frame perform actual theme change so transitions take effect
                    requestAnimationFrame(function(){
                        if (theme === 'dark') rootEl.classList.add('theme-dark');
                        else rootEl.classList.remove('theme-dark');
                        updateToggleUI(theme === 'dark');
                    });
                    // remove animating class after animation finishes
                    setTimeout(function(){ rootEl.classList.remove(ANIM_CLASS); }, ANIM_MS);
                } else {
                    // no animation: just set theme immediately
                    if (theme === 'dark') rootEl.classList.add('theme-dark');
                    else rootEl.classList.remove('theme-dark');
                    updateToggleUI(theme === 'dark');
                }
                try { localStorage.setItem(LS_KEY, theme); } catch(e){}
            } catch(e){}
        }

        // init without animation (to avoid flicker on page load or bfcache restore)
        const saved = (function(){ try { return localStorage.getItem(LS_KEY); } catch(e){ return null; }})();
        if (saved) applyTheme(saved === 'dark' ? 'dark' : 'light', false);
        else if(window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) applyTheme('dark', false);
        else applyTheme('light', false);

        // pageshow - reapply without animation (bfcache restore)
        window.addEventListener('pageshow', function(){
            try {
                const s = localStorage.getItem(LS_KEY);
                applyTheme(s === 'dark' ? 'dark' : 'light', false);
            } catch(e){}
        });

        if (toggle) {
            toggle.addEventListener('click', function(){
                const isDark = rootEl.classList.contains('theme-dark');
                applyTheme(isDark ? 'light' : 'dark', true); // animate on user toggle
            });
        }

// ---- Link list collapse/expand logic ----
        function initLinkToggles() {
// For each .result-links container that has a .link-list
            const containers = document.querySelectorAll('.result-links');
            containers.forEach(function(container){
                const list = container.querySelector('.link-list');
                const btn = container.querySelector('.link-toggle-btn');
                if (!list || !btn) return;

                const items = list.querySelectorAll('.link-item');
                const total = items.length;
                const visibleCount = 4;

                if (total <= visibleCount) {
// nothing to toggle
                    btn.setAttribute('aria-hidden', 'true');
                    btn.style.display = 'none';
                    list.classList.remove('collapsed');
                    return;
                }

// default collapsed
                list.classList.add('collapsed');
                btn.setAttribute('aria-hidden', 'false');
                btn.setAttribute('aria-expanded', 'false');
                btn.textContent = 'Show ' + (total - visibleCount) + ' more';

                btn.addEventListener('click', function(){
                    const expanded = btn.getAttribute('aria-expanded') === 'true';
                    if (expanded) {
// collapse
                        list.classList.add('collapsed');
                        btn.setAttribute('aria-expanded','false');
                        btn.textContent = 'Show ' + (total - visibleCount) + ' more';
                    } else {
// expand
                        list.classList.remove('collapsed');
                        btn.setAttribute('aria-expanded','true');
                        btn.textContent = 'Show less';
                    }
                });
            });
        }

// initialize on DOMContentLoaded (or immediately if already loaded)
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', initLinkToggles);
        } else {
            initLinkToggles();
        }
    })();
</script>
</body>
</html>