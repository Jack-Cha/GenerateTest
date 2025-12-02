<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Test Generator</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            display: flex;
            justify-content: center;
            align-items: center;
            padding: 20px;
        }

        .container {
            background: white;
            border-radius: 15px;
            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
            max-width: 700px;
            width: 100%;
            padding: 40px;
        }

        h1 {
            color: #333;
            margin-bottom: 10px;
            font-size: 2em;
        }

        .subtitle {
            color: #666;
            margin-bottom: 30px;
            font-size: 0.95em;
        }

        .form-group {
            margin-bottom: 25px;
        }

        label {
            display: block;
            margin-bottom: 8px;
            color: #444;
            font-weight: 600;
            font-size: 0.95em;
        }

        input[type="text"] {
            width: 100%;
            padding: 12px 15px;
            border: 2px solid #e0e0e0;
            border-radius: 8px;
            font-size: 1em;
            transition: all 0.3s ease;
        }

        input[type="text"]:focus {
            outline: none;
            border-color: #667eea;
            box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
        }

        .help-text {
            font-size: 0.85em;
            color: #888;
            margin-top: 5px;
        }

        button {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border: none;
            padding: 14px 30px;
            border-radius: 8px;
            font-size: 1em;
            font-weight: 600;
            cursor: pointer;
            width: 100%;
            transition: transform 0.2s ease, box-shadow 0.2s ease;
        }

        button:hover {
            transform: translateY(-2px);
            box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
        }

        button:active {
            transform: translateY(0);
        }

        .alert {
            padding: 15px 20px;
            border-radius: 8px;
            margin-bottom: 25px;
            font-size: 0.95em;
        }

        .alert-success {
            background-color: #d4edda;
            border: 1px solid #c3e6cb;
            color: #155724;
        }

        .alert-error {
            background-color: #f8d7da;
            border: 1px solid #f5c6cb;
            color: #721c24;
        }

        .result-summary {
            margin-top: 15px;
            padding: 15px;
            background-color: #f8f9fa;
            border-radius: 8px;
        }

        .result-summary h3 {
            margin-bottom: 10px;
            color: #333;
            font-size: 1.1em;
        }

        .result-list {
            list-style: none;
            padding: 0;
        }

        .result-list li {
            padding: 8px 0;
            border-bottom: 1px solid #e0e0e0;
            font-size: 0.9em;
        }

        .result-list li:last-child {
            border-bottom: none;
        }

        .stats {
            display: flex;
            gap: 20px;
            margin-bottom: 15px;
        }

        .stat {
            flex: 1;
            padding: 10px;
            background: white;
            border-radius: 6px;
            text-align: center;
        }

        .stat-value {
            font-size: 1.8em;
            font-weight: bold;
            color: #667eea;
        }

        .stat-label {
            font-size: 0.85em;
            color: #666;
            margin-top: 5px;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>Test Generator <span style="font-size: 0.6em; color: #888;">(version: <%= request.getAttribute("version") != null ? request.getAttribute("version") : "2.3" %>)</span></h1>
        <p class="subtitle">Java 소스 코드에서 JUnit 테스트를 자동 생성합니다</p>

        <% if (request.getAttribute("error") != null) { %>
        <div class="alert alert-error">
            <strong>오류!</strong> <%= request.getAttribute("error") %>
        </div>
        <% } %>

        <% if (request.getAttribute("success") != null && (Boolean)request.getAttribute("success")) { %>
        <div class="alert alert-success">
            <strong>완료!</strong> 테스트 생성이 완료되었습니다.
            <div class="result-summary">
                <div class="stats">
                    <div class="stat">
                        <div class="stat-value"><%= request.getAttribute("successCount") %></div>
                        <div class="stat-label">성공</div>
                    </div>
                    <div class="stat">
                        <div class="stat-value"><%= request.getAttribute("failCount") %></div>
                        <div class="stat-label">실패</div>
                    </div>
                </div>
                <h3>테스트 디렉토리:</h3>
                <p><%= request.getAttribute("testPath") %></p>
                <h3 style="margin-top: 15px;">처리 결과:</h3>
                <ul class="result-list">
                    <% 
                    java.util.List<String> results = (java.util.List<String>)request.getAttribute("results");
                    if (results != null) {
                        for (String result : results) {
                    %>
                        <li><%= result %></li>
                    <% 
                        }
                    }
                    %>
                </ul>

                <!-- 테스트 실행 버튼 -->
                <form action="<%= request.getContextPath() %>/generateTest" method="post" style="margin-top: 20px;">
                    <input type="hidden" name="action" value="runTest">
                    <input type="hidden" name="testPath" value="<%= request.getAttribute("testPath") %>">
                    <button type="submit" style="background: linear-gradient(135deg, #27ae60 0%, #229954 100%);">
                        ▶ 테스트 실행
                    </button>
                </form>
            </div>
        </div>
        <% } %>

        <% if (request.getAttribute("testRunSuccess") != null && (Boolean)request.getAttribute("testRunSuccess")) { %>
        <div class="alert alert-success">
            <strong>테스트 실행 완료!</strong>
            <div class="result-summary">
                <div class="stats">
                    <div class="stat">
                        <div class="stat-value"><%= request.getAttribute("totalTests") %></div>
                        <div class="stat-label">전체</div>
                    </div>
                    <div class="stat">
                        <div class="stat-value" style="color: #27ae60;"><%= request.getAttribute("passedTests") %></div>
                        <div class="stat-label">통과</div>
                    </div>
                    <div class="stat">
                        <div class="stat-value" style="color: #e74c3c;"><%= request.getAttribute("failedTests") %></div>
                        <div class="stat-label">실패</div>
                    </div>
                </div>
                <h3 style="margin-top: 15px;">테스트 결과:</h3>
                <ul class="result-list">
                    <% 
                    java.util.List<String> testResults = (java.util.List<String>)request.getAttribute("testResults");
                    if (testResults != null) {
                        for (String result : testResults) {
                    %>
                        <li><%= result %></li>
                    <% 
                        }
                    }
                    %>
                </ul>
            </div>
        </div>
        <% } %>

        <form action="<%= request.getContextPath() %>/generateTest" method="post">
            <input type="hidden" name="action" value="generateTest">
            <div class="form-group">
                <label for="sourcePath">소스 파일 또는 디렉토리 경로</label>
                <input type="text"
                       id="sourcePath"
                       name="sourcePath"
                       value="D:\GitHub\GenerateTest\GenerateTest\src\com\example\Calculator.java"
                       placeholder="예: D:\GitHub\TestGenerator\src\main\java"
                       required>
                <div class="help-text">
                    개별 .java 파일 경로 또는 디렉토리 경로를 입력하세요
                </div>
            </div>

            <div class="form-group">
                <label for="testPath">테스트 디렉토리 경로</label>
                <input type="text"
                       id="testPath"
                       name="testPath"
                       value="D:\GitHub\GenerateTest\GenerateTest\src"
                       placeholder="예: D:\GitHub\TestGenerator\src\test\java"
                       required>
                <div class="help-text">
                    생성된 테스트 파일이 저장될 디렉토리 경로를 입력하세요
                </div>
            </div>

            <button type="submit">테스트 생성</button>
        </form>
    </div>
</body>
</html>
