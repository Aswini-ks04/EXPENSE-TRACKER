@echo off
setlocal

set "JAVA_BIN=C:\Users\aswin\Documents\Downloads\openjdk-21.0.2_windows-x64_bin\jdk-21.0.2\bin"
if exist "%JAVA_BIN%\javac.exe" (
    set "PATH=%JAVA_BIN%;%PATH%"
)

echo ========================================================
echo   Building Personal Expense Tracker (Java 21)
echo ========================================================

if not exist bin mkdir bin

echo Compiling Java source files...
javac -encoding UTF-8 -d bin -sourcepath src src/com/expensetracker/model/*.java src/com/expensetracker/repository/*.java src/com/expensetracker/service/*.java src/com/expensetracker/ui/cli/*.java src/com/expensetracker/ui/gui/*.java src/com/expensetracker/web/*.java src/com/expensetracker/*.java
if errorlevel 1 (
    echo [ERROR] Compilation failed!
    pause
    exit /b 1
)

echo Packaging ExpenseTracker.jar...
jar --create --file ExpenseTracker.jar --main-class com.expensetracker.Main -C bin .
if errorlevel 1 (
    echo [ERROR] JAR packaging failed!
    pause
    exit /b 1
)

echo.
echo ========================================================
echo   [SUCCESS] Build Complete! 
echo   Generated: ExpenseTracker.jar and compiled classes in bin/
echo ========================================================
pause
