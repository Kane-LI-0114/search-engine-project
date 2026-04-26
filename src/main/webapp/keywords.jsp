<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.*" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Keyword Browser - CSIT5930 Search Engine</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;600&display=swap" rel="stylesheet">
    <style>
        :root{--bg:#f5f7fb;--card:#ffffff;--text:#0f1724;--muted:#6b7280;--accent:#2563eb;--input-bg:#f8fafc;--radius:16px;}
        .theme-dark{--bg:#071028;--card:rgba(255,255,255,0.03);--text:#e6eef8;--muted:#9aa4b2;--accent:#4f8cff;--input-bg:rgba(255,255,255,0.02);}
        *{box-sizing:border-box}
        html,body{height:100%;margin:0;font-family:"Inter",system-ui,-apple-system,sans-serif;}
        body{background:var(--bg);color:var(--text);padding:36px;transition:background .25s,color .25s;}
        .container{max-width:980px;margin:0 auto;background:linear-gradient(180deg,rgba(255,255,255,0.85),rgba(255,255,255,0.8));border-radius:var(--radius);padding:28px;box-shadow:0 8px 30px rgba(16,24,40,0.08);}
        .theme-dark .container{background:linear-gradient(180deg,rgba(255,255,255,0.02),rgba(255,255,255,0.01));box-shadow:0 12px 40px rgba(2,8,23,0.6);}
        header{display:flex;align-items:center;justify-content:space-between;gap:16px;margin-bottom:20px;}
        .brand{display:flex;align-items:center;gap:14px;}
        .logo{width:56px;height:56px;border-radius:12px;background:linear-gradient(135deg,var(--accent),#7cc3ff);display:flex;align-items:center;justify-content:center;font-weight:700;color:#042033;font-size:18px;box-shadow:0 8px 20px rgba(37,99,235,0.12);}
        .title h1{margin:0;font-size:20px;}
        .title p{margin:2px 0 0;color:var(--muted);font-size:13px;}
        .nav-links{display:flex;gap:12px;font-size:14px;}
        .nav-links a{color:var(--accent);text-decoration:none;}
        .nav-links a:hover{text-decoration:underline;}
        .letter-bar{display:flex;flex-wrap:wrap;gap:6px;margin-bottom:20px;}
        .letter-btn{display:inline-block;padding:6px 11px;border-radius:8px;border:1px solid rgba(37,99,235,0.2);text-decoration:none;font-size:13px;font-weight:600;color:var(--accent);transition:background .15s,color .15s;}
        .letter-btn:hover,.letter-btn.active{background:var(--accent);color:#fff;border-color:var(--accent);}
        .letter-btn.inactive{color:var(--muted);border-color:rgba(0,0,0,0.06);cursor:default;pointer-events:none;}
        .theme-dark .letter-btn.inactive{border-color:rgba(255,255,255,0.06);}
        .kw-group{margin-bottom:24px;}
        .kw-group-header{font-size:16px;font-weight:600;color:var(--muted);margin-bottom:10px;border-bottom:1px solid rgba(0,0,0,0.06);padding-bottom:6px;}
        .theme-dark .kw-group-header{border-color:rgba(255,255,255,0.06);}
        .kw-list{display:flex;flex-wrap:wrap;gap:8px;}
        .kw-pill{display:inline-block;padding:5px 12px;background:var(--card);border:1px solid rgba(37,99,235,0.12);border-radius:999px;font-size:13px;text-decoration:none;color:var(--text);transition:background .15s,color .15s,border-color .15s;}
        .kw-pill:hover{background:var(--accent);color:#fff;border-color:var(--accent);}
        .theme-dark .kw-pill{border-color:rgba(255,255,255,0.1);}
        .summary{font-size:14px;color:var(--muted);margin-bottom:16px;}
        .error{color:#d32f2f;padding:10px;background:#fce4ec;border-radius:6px;}
        .theme-toggle{display:inline-flex;align-items:center;gap:8px;background:transparent;border:2px solid rgba(0,0,0,0.06);padding:8px 12px;border-radius:999px;cursor:pointer;font-size:13px;color:var(--text);}
        .theme-dark .theme-toggle{border:2px solid rgba(255,255,255,0.06);}
    </style>
</head>
<body>
<%!
    public static String escKw(Object o) {
        if (o == null) return "";
        String s = String.valueOf(o);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch(c){
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
%>
<%
    @SuppressWarnings("unchecked")
    TreeMap<Character, List<String>> grouped =
            (TreeMap<Character, List<String>>) request.getAttribute("grouped");
    Character activeLetter = (Character) request.getAttribute("activeLetter");
    Integer totalCount = (Integer) request.getAttribute("totalCount");
    String error = (String) request.getAttribute("error");
    Set<Character> availableLetters = (grouped != null) ? grouped.keySet() : new TreeSet<Character>();
%>
<div class="container">
    <header>
        <div class="brand">
            <div class="logo">SE</div>
            <div class="title">
                <h1>Keyword Browser</h1>
                <p>CSIT5930 Search Engine — HKUST</p>
            </div>
        </div>
        <div style="display:flex;gap:12px;align-items:center;">
            <button onclick="window.location.href='index.jsp'" class="theme-toggle" aria-label="Homepage" title="Homepage">
                    <span>🏠</span>
                    Homepage
            </button>
            <button id="themeToggle" class="theme-toggle" aria-pressed="false" title="Toggle light/dark">
                <span id="themeIcon">&#127769;</span>
                <span id="themeLabel">Dark</span>
            </button>
        </div>
    </header>

    <% if (error != null) { %>
    <div class="error"><%= escKw(error) %></div>
    <% } else { %>

    <div class="summary">
        Total indexed keywords: <strong><%= totalCount != null ? totalCount : 0 %></strong>
        &mdash; Click any keyword to run a search.
    </div>

    <div class="letter-bar">
        <a href="keywords" class="letter-btn <%= activeLetter == null ? "active" : "" %>">ALL</a>
        <% for (char c = 'A'; c <= 'Z'; c++) {
            boolean avail = availableLetters.contains(c);
            boolean active = activeLetter != null && activeLetter == c;
        %>
        <% if (avail) { %>
        <a href="keywords?letter=<%= c %>" class="letter-btn <%= active ? "active" : "" %>"><%= c %></a>
        <% } else { %>
        <span class="letter-btn inactive"><%= c %></span>
        <% } %>
        <% } %>
    </div>

    <% if (grouped != null && !grouped.isEmpty()) {
        for (Map.Entry<Character, List<String>> entry : grouped.entrySet()) {
            char letter = entry.getKey();
            List<String> stems = entry.getValue();
            if (activeLetter != null && activeLetter != letter) continue;
    %>
    <div class="kw-group">
        <div class="kw-group-header"><%= letter %></div>
        <div class="kw-list">
            <% for (String stem : stems) { %>
            <a href="search?query=<%= escKw(stem) %>" class="kw-pill" title="Search for '<%= escKw(stem) %>'"><%= escKw(stem) %></a>
            <% } %>
        </div>
    </div>
    <% } } else { %>
    <p style="color:var(--muted);">No keywords indexed yet. Run the crawler first.</p>
    <% } %>
    <% } %>
</div>

<script>
    (function(){
        var rootEl = document.documentElement;
        var toggle = document.getElementById('themeToggle');
        var icon   = document.getElementById('themeIcon');
        var label  = document.getElementById('themeLabel');
        var LS_KEY = 'csit5930_theme_v1';
        function applyTheme(t){
            if(t==='dark') rootEl.classList.add('theme-dark');
            else rootEl.classList.remove('theme-dark');
            if(toggle) toggle.setAttribute('aria-pressed', t==='dark'?'true':'false');
            if(icon)  icon.textContent  = t==='dark'?'\u2600\uFE0F':'\uD83C\uDF19';
            if(label) label.textContent = t==='dark'?'Light':'Dark';
            try{ localStorage.setItem(LS_KEY,t); }catch(e){}
        }
        var saved=(function(){ try{ return localStorage.getItem(LS_KEY); }catch(e){ return null; }})();
        if(saved) applyTheme(saved==='dark'?'dark':'light');
        else if(window.matchMedia&&window.matchMedia('(prefers-color-scheme: dark)').matches) applyTheme('dark');
        else applyTheme('light');
        if(toggle) toggle.addEventListener('click',function(){
            applyTheme(rootEl.classList.contains('theme-dark')?'light':'dark');
        });
    })();
</script>
</body>
</html>
