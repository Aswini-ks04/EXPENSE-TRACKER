@echo off
chcp 65001 >nul
setlocal
set "JAVA_BIN=C:\Users\aswin\Documents\Downloads\openjdk-21.0.2_windows-x64_bin\jdk-21.0.2\bin"
if exist "%JAVA_BIN%\java.exe" (
    set "PATH=%JAVA_BIN%;%PATH%"
)

if exist "ExpenseTracker.jar" (
    java -jar ExpenseTracker.jar --cli
) else (
    java -cp bin com.expensetracker.Main --cli
)
pause
