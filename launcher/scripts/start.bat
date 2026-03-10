@echo off
setlocal

set "SERVER_HOME=%~dp0"
set "MAIN_CLASS=com.muyi.core.bootstrap.Bootstrap"

:: JDK 路径（项目使用 JDK 25 编译，必须用对应版本运行）
set "JAVA_HOME=C:\Program Files\Java\jdk-25"
set "JAVA=%JAVA_HOME%\bin\java.exe"

:: JVM 参数（可根据实际情况调整）
set "JAVA_OPTS=-Xms512m -Xmx2g"
set "JAVA_OPTS=%JAVA_OPTS% -XX:+UseZGC"
set "JAVA_OPTS=%JAVA_OPTS% -XX:+HeapDumpOnOutOfMemoryError"
set "JAVA_OPTS=%JAVA_OPTS% -XX:HeapDumpPath=%SERVER_HOME%logs"
set "JAVA_OPTS=%JAVA_OPTS% -Dfile.encoding=UTF-8"

cd /d "%SERVER_HOME%"

echo ========================================
echo   SLG Server
echo   Home: %SERVER_HOME%
echo ========================================

"%JAVA%" %JAVA_OPTS% -cp "lib/*" %MAIN_CLASS% --config=serverconfig/server.yaml %*

if %errorlevel% neq 0 (
    echo.
    echo Server exited with error code: %errorlevel%
    pause
)

endlocal
