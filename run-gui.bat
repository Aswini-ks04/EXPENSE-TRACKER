@echo off
setlocal
set "JAVA_BIN=C:\Users\aswin\Documents\Downloads\openjdk-21.0.2_windows-x64_bin\jdk-21.0.2\bin"
if exist "%JAVA_BIN%\java.exe" (
    set "PATH=%JAVA_BIN%;%PATH%"
)

echo Starting Expense Tracker GUI...
if exist "ExpenseTracker.jar" (
    start javaw -jar ExpenseTracker.jar --gui
) else (
    start javaw -cp bin com.expensetracker.Main --gui
)
exit /b 0
