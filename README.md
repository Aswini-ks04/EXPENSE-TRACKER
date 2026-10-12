# 💰 SpendWise - Personal Expense Tracker (Web Application)

A full-stack, zero-dependency **Personal Expense Tracker and Budget Website** built with pure Java and modern web technologies (HTML5, CSS3, JavaScript, Chart.js).

When you run this project, it starts an embedded web server and opens the web application in your browser at **`http://localhost:8088`** (with automatic port detection if 8088 is occupied).

---

## 🌟 Features

- **🌐 Modern Web Dashboard**:
  - Interactive dashboard with stat cards: Total Spending, Current Month Spending, Monthly Budget Remaining, Total Transactions.
  - Dynamic budget status alerts (**On Track**, **Near Limit (≥80%)**, **Exceeded**).
  - Visual animated budget progress bar.
- **📊 Interactive Charts (Chart.js)**:
  - **Spending by Category**: Doughnut chart with category breakdown.
  - **Monthly Spending Trend**: Bar chart tracking your monthly expenditures.
- **💸 Full Transaction Management**:
  - Add, edit, and delete expenses through responsive modal windows.
  - Filter by category, time range (This Month, Last 30 Days, This Year), or instant live text search.
- **💾 Persistent Storage & Export**:
  - All data is automatically saved to `data/expenses.csv` and `data/budget.properties`.
  - One-click **"Export CSV"** button to download transaction backups.
- **⚡ Zero External Setup Required**:
  - Uses Java's built-in HTTP server (`com.sun.net.httpserver.HttpServer`).
  - No complex database installation needed.

---

## 🚀 How to Run in VS Code (For You & Your Friend)

### Step 1: Open the Project in VS Code
1. Clone or download this repository.
2. Open VS Code, click **File > Open Folder...**, and select the **`EXPENSE TRACKER`** folder.
3. Make sure the **Extension Pack for Java** is installed in VS Code.

### Step 2: Run the Web Application
Choose any of these easy options:

- **Option A (F5 / Run Button)**:
  - Press **`F5`** (or go to the **Run & Debug** tab on the left and click **"Run Website (Web Server)"**).
- **Option B (From `Main.java`)**:
  - Open [`src/com/expensetracker/Main.java`](file:///c:/EXPENSE%20TRACKER/src/com/expensetracker/Main.java) and click the **"Run"** button above `public static void main`.
- **Option C (One-Click Windows Script)**:
  - Double-click [`run-website.bat`](file:///c:/EXPENSE%20TRACKER/run-website.bat).
- **Option D (VS Code Terminal)**:
  ```powershell
  java -jar ExpenseTracker.jar --web
  ```

### Step 3: Open in Browser
Once running, open your web browser and go to:
👉 **`http://localhost:8088`**

---

## 📤 How to Upload to GitHub

In your VS Code terminal (or PowerShell in this folder), run:

```bash
git init
git add .
git commit -m "Initial commit: Personal Expense Tracker Web App"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPOSITORY.git
git push -u origin main
```

*(A [`.gitignore`](file:///c:/EXPENSE%20TRACKER/.gitignore) file is already included so temporary files and caches are automatically excluded).*

---

## 📁 Project Structure

```
EXPENSE TRACKER/
├── .vscode/
│   ├── launch.json          # Pre-configured F5 run button for VS Code
│   └── settings.json        # Java classpath & source settings
├── src/
│   └── com/expensetracker/
│       ├── Main.java        # Main entry point (starts web server)
│       ├── web/
│       │   ├── WebServer.java # Built-in HTTP server & REST API
│       │   └── JsonUtil.java  # Lightweight JSON serializer
│       ├── model/           # Expense, Category, PaymentMethod, Budget
│       ├── repository/      # CsvExpenseRepository & data persistence
│       └── service/         # Analytics, filtering, and business logic
├── web/
│   ├── index.html           # Modern dashboard frontend
│   ├── style.css            # Responsive layout & styling
│   └── app.js               # Dynamic charts, API calls & modals
├── data/
│   ├── expenses.csv         # Persistent CSV storage
│   └── budget.properties    # Monthly budget settings
├── run-website.bat          # One-click web runner
├── ExpenseTracker.jar       # Runnable standalone JAR
├── pom.xml                  # Standard Maven configuration
└── README.md                # Project documentation
```
