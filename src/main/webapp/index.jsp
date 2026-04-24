<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>

<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>CSIT5930 Search Engine</title>

    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;600&display=swap" rel="stylesheet">
    <style>
        :root{
            --bg: #f5f7fb;
            --card: #ffffff;
            --text: #0f1724;
            --muted: #6b7280;
            --accent: #2563eb;
            --input-bg: #f8fafc;
            --glass: rgba(255,255,255,0.6);
            --radius: 16px;
        }
        .theme-dark {
            --bg: #071028;
            /*--bg: linear-gradient(180deg,#071028 0%, #100931 100%);*/
            --card: rgba(255,255,255,0.03);
            --text: #e6eef8;
            --muted: #9aa4b2;
            --accent: #4f8cff;
            --input-bg: rgba(255,255,255,0.02);
            --glass: rgba(255,255,255,0.02);
        }
        *{box-sizing:border-box}
        html,body{height:100%;margin:0;font-family:"Inter",system-ui,-apple-system,Segoe UI,Roboto,Helvetica,Arial; -webkit-font-smoothing:antialiased; -moz-osx-font-smoothing:grayscale;}
        body{
            background: var(--bg);
            color: var(--text);
            display:flex;
            align-items:center;
            justify-content:center;
            padding:36px;
            transition: background .25s ease, color .25s ease;
        }
        .container{
            width:100%;
            max-width:920px;
            background: linear-gradient(180deg, rgba(255,255,255,0.7), rgba(255,255,255,0.6));
            border-radius: var(--radius);
            padding:28px;
            box-shadow: 0 8px 30px rgba(16,24,40,0.08);
            position:relative;
        }
        .theme-dark .container{
            background: linear-gradient(180deg, rgba(255,255,255,0.02), rgba(255,255,255,0.01));
            box-shadow: 0 12px 40px rgba(2,8,23,0.6);
        }
        header {
            display:flex;
            align-items:center;
            justify-content:space-between;
            gap:16px;
            margin-bottom:18px;
        }
        .brand {
            display:flex;
            align-items:center;
            gap:14px;
        }
        .logo {
            width:56px;height:56px;border-radius:12px;
            background: linear-gradient(135deg,var(--accent), #7cc3ff 100%);
            display:flex;align-items:center;justify-content:center;
            font-weight:700;color:#042033;font-size:18px;
            box-shadow: 0 8px 20px rgba(37,99,235,0.12);
        }
        .title { line-height:1; }
        .title h1{margin:0;font-size:20px;}
        .title p{margin:2px 0 0;color:var(--muted);font-size:13px;}
        .theme-toggle {
            display:inline-flex;
            align-items:center;
            gap:8px;
            background:transparent;
            border:1px solid rgba(0,0,0,0.06);
            padding:8px 12px;
            border-radius:999px;
            cursor:pointer;
            font-size:13px;
            color:var(--text);
        }
        .theme-dark .theme-toggle{
            border:1px solid rgba(255,255,255,0.06);
            color:var(--text);
        }
        .search-card{ margin-top:6px; }
        .search-form{ display:flex; gap:12px; align-items:center; }
        .input { position:relative; flex:1; }
        .search-input{
            width:100%;
            padding:14px 46px 14px 46px;
            border-radius:12px;
            border:1px solid rgba(15,23,36,0.06);
            background: var(--input-bg);
            color: var(--text);
            font-size:16px;
            outline:none;
            transition: box-shadow .18s ease, border-color .18s ease;
        }
        .theme-dark .search-input{ border:1px solid rgba(255,255,255,0.04); }
        .search-input::placeholder{ color: rgba(100,116,139,0.7); }
        .search-input:focus{
            box-shadow: 0 10px 30px rgba(37,99,235,0.12);
            border-color: var(--accent);
        }
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
            padding:12px 18px;
            border-radius:12px;
            border:none;
            background: linear-gradient(90deg,var(--accent), #7dd3fc 120%);
            color:#042033;
            font-weight:600;
            cursor:pointer;
            box-shadow: 0 8px 26px rgba(37,99,235,0.12);
        }
        .hint{
            margin-top:12px;
            color:var(--muted);
            font-size:13px;
        }
        @media (max-width:640px){
            .container{ padding:18px; border-radius:12px; }
            .logo{ width:48px;height:48px;font-size:16px; }
            .search-input{ padding:12px 44px 12px 44px; font-size:15px; }
            .search-btn{ padding:10px 14px; font-size:14px; }
            header{flex-direction:column;align-items:flex-start;gap:10px;}
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
        <div class="brand">
            <div class="logo" aria-hidden="true">SE</div>
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

    <div class="search-card">
        <form class="search-form" action="search" method="GET">
            <div class="input">
<span class="icon-left" aria-hidden="true">
<svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
<path d="M21 21L16.65 16.65" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
<circle cx="11" cy="11" r="6" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
</svg>
</span>
                <input type="text" name="query" class="search-input" placeholder="Enter your search query..." autofocus aria-label="Search query">
            </div>
            <button type="submit" class="search-btn">Search</button>
        </form>

        <p class="hint">
            Supports phrase search with double quotes. Example: <code style="background:transparent;padding:2px 6px;border-radius:4px;border:1px solid rgba(0,0,0,0.04);">"hong kong" universities</code>
        </p>
    </div></div>

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
    })();
</script>
</body>
</html>