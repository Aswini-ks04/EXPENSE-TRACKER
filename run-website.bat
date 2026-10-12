@echo off
setlocal
set "JAVA_BIN=C:\Users\aswin\Documents\Downloads\openjdk-21.0.2_windows-x64_bin\jdk-21.0.2\bin"
if exist "%JAVA_BIN%\java.exe" (
    set "PATH=%JAVA_BIN%;%PATH%"
)

echo ========================================================
echo   Starting Expense Tracker Web Application...
echo   Opening in your default browser at http://localhost:8088
echo ========================================================

if exist "ExpenseTracker.jar" (
    java -jar ExpenseTracker.jar --port 8088
) else (
    java -cp bin com.expensetracker.Main --port 8088
)
pause
