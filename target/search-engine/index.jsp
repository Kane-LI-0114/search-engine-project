<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>CSIT5930 Search Engine</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 40px;
            background-color: #f5f5f5;
        }
        .search-container {
            text-align: center;
            margin-top: 100px;
        }
        h1 {
            color: #333;
            font-size: 36px;
        }
        .subtitle {
            color: #666;
            font-size: 16px;
            margin-bottom: 30px;
        }
        .search-form {
            margin: 20px 0;
        }
        .search-input {
            width: 500px;
            padding: 12px 16px;
            font-size: 16px;
            border: 1px solid #ccc;
            border-radius: 24px;
            outline: none;
        }
        .search-input:focus {
            border-color: #4285f4;
            box-shadow: 0 1px 6px rgba(66, 133, 244, 0.3);
        }
        .search-btn {
            padding: 12px 24px;
            font-size: 16px;
            background-color: #4285f4;
            color: white;
            border: none;
            border-radius: 24px;
            cursor: pointer;
            margin-left: 8px;
        }
        .search-btn:hover {
            background-color: #357abd;
        }
        .hint {
            color: #888;
            font-size: 14px;
            margin-top: 15px;
        }
        code {
            background-color: #e8e8e8;
            padding: 2px 6px;
            border-radius: 3px;
            font-size: 13px;
        }
    </style>
</head>
<body>
    <div class="search-container">
        <h1>CSIT5930 Search Engine</h1>
        <p class="subtitle">HKUST - Web Search Engine Project</p>

        <form class="search-form" action="search" method="GET">
            <input type="text" name="query" class="search-input"
                   placeholder="Enter your search query..." autofocus>
            <button type="submit" class="search-btn">Search</button>
        </form>

        <p class="hint">
            Supports phrase search with double quotes.<br>
            Example: <code>"hong kong" universities</code>
        </p>
    </div>
</body>
</html>
