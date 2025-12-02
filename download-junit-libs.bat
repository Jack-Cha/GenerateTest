@echo off
echo ========================================
echo JUnit Platform 라이브러리 다운로드
echo ========================================
echo.

set LIB_DIR=D:\GitHub\GenerateTest\GenerateTest\webapp\WEB-INF\lib
set VERSION=1.11.0

echo 다운로드 디렉토리: %LIB_DIR%
echo 버전: %VERSION%
echo.

:: URL 설정
set BASE_URL=https://repo1.maven.org/maven2/org/junit/platform

set ENGINE_URL=%BASE_URL%/junit-platform-engine/%VERSION%/junit-platform-engine-%VERSION%.jar
set LAUNCHER_URL=%BASE_URL%/junit-platform-launcher/%VERSION%/junit-platform-launcher-%VERSION%.jar

echo 1. junit-platform-engine-%VERSION%.jar 다운로드 중...
powershell -Command "& {Invoke-WebRequest -Uri '%ENGINE_URL%' -OutFile '%LIB_DIR%\junit-platform-engine-%VERSION%.jar'}"

if %ERRORLEVEL% EQU 0 (
    echo    ? 다운로드 완료
) else (
    echo    ? 다운로드 실패
    goto :error
)

echo.
echo 2. junit-platform-launcher-%VERSION%.jar 다운로드 중...
powershell -Command "& {Invoke-WebRequest -Uri '%LAUNCHER_URL%' -OutFile '%LIB_DIR%\junit-platform-launcher-%VERSION%.jar'}"

if %ERRORLEVEL% EQU 0 (
    echo    ? 다운로드 완료
) else (
    echo    ? 다운로드 실패
    goto :error
)

echo.
echo 3. 구버전 파일 삭제 중...
if exist "%LIB_DIR%\junit-platform-engine-1.8.0.jar" (
    del "%LIB_DIR%\junit-platform-engine-1.8.0.jar"
    echo    ? junit-platform-engine-1.8.0.jar 삭제됨
)

if exist "%LIB_DIR%\junit-platform-launcher-1.8.0.jar" (
    del "%LIB_DIR%\junit-platform-launcher-1.8.0.jar"
    echo    ? junit-platform-launcher-1.8.0.jar 삭제됨
)

echo.
echo ========================================
echo 다운로드 및 설치 완료!
echo ========================================
echo.
echo 다음 단계:
echo 1. Eclipse에서 Project ^> Clean...
echo 2. Project ^> Build Project
echo 3. Tomcat 서버 재시작
echo 4. 테스트 재실행
echo.
echo 설치된 파일:
dir "%LIB_DIR%\junit*.jar" /b
echo.
pause
exit /b 0

:error
echo.
echo ========================================
echo 오류 발생!
echo ========================================
echo.
echo 수동 다운로드가 필요합니다:
echo.
echo 1. 브라우저에서 다음 URL을 열어 파일을 다운로드하세요:
echo.
echo    %ENGINE_URL%
echo    %LAUNCHER_URL%
echo.
echo 2. 다운로드한 파일을 다음 폴더에 복사하세요:
echo    %LIB_DIR%
echo.
echo 3. 구버전 파일을 삭제하세요:
echo    junit-platform-engine-1.8.0.jar
echo    junit-platform-launcher-1.8.0.jar
echo.
pause
exit /b 1
