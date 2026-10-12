package com.expensetracker.web;

import com.expensetracker.model.*;
import com.expensetracker.service.CategorySummary;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.service.MonthlySummary;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.Executors;

public class WebServer {
    private final ExpenseService service;
    private int port;
    private HttpServer server;

    public WebServer(ExpenseService service, int initialPort) {
        this.service = service;
        this.port = initialPort;
    }

    public int start() throws IOException {
        int maxAttempts = 20;
        IOException lastEx = null;

        for (int i = 0; i < maxAttempts; i++) {
            int currentPort = port + i;
            try {
                // Bind to 0.0.0.0 for universal compatibility
                server = HttpServer.create(new InetSocketAddress("0.0.0.0", currentPort), 0);
                this.port = currentPort;
                break;
            } catch (IOException e) {
                lastEx = e;
                System.out.println("Port " + currentPort + " is in use, trying " + (currentPort + 1) + "...");
            }
        }

        if (server == null) {
            throw new IOException("Could not find an available port to start web server.", lastEx);
        }

        server.setExecutor(Executors.newFixedThreadPool(10));

        // API routes
        server.createContext("/api/expenses", new ExpensesHandler());
        server.createContext("/api/summary", new SummaryHandler());
        server.createContext("/api/budget", new BudgetHandler());
        server.createContext("/api/export", new ExportHandler());
        server.createContext("/api/reset", new ResetHandler());

        // Static file routes
        server.createContext("/", new StaticFileHandler());

        server.start();
        System.out.println();
        System.out.println("===============================================================");
        System.out.println("  🚀 Personal Expense Tracker Web Server is Running!");
        System.out.println("  👉 Open in your web browser: http://localhost:" + port);
        System.out.println("===============================================================");
        System.out.println();

        return this.port;
    }

    public int getPort() {
        return port;
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    /**
     * Handler for /api/expenses (GET, POST, PUT, DELETE)
     */
    private class ExpensesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                addCorsHeaders(exchange);
                String method = exchange.getRequestMethod().toUpperCase();
                String path = exchange.getRequestURI().getPath();

                if ("OPTIONS".equals(method)) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                // Check for /api/expenses/{id}
                String idParam = null;
                if (path.length() > "/api/expenses/".length() && path.startsWith("/api/expenses/")) {
                    idParam = path.substring("/api/expenses/".length()).trim();
                }

                if ("GET".equals(method)) {
                    Map<String, String> query = parseQuery(exchange.getRequestURI().getQuery());
                    List<Expense> list = service.getAllExpenses();

                    if (query.containsKey("search")) {
                        list = service.search(query.get("search"));
                    }
                    if (query.containsKey("category")) {
                        Category cat = Category.fromString(query.get("category"));
                        list = list.stream().filter(e -> e.getCategory() == cat).toList();
                    }
                    if (query.containsKey("timePeriod")) {
                        String tp = query.get("timePeriod");
                        LocalDate now = LocalDate.now();
                        if ("this_month".equalsIgnoreCase(tp)) {
                            list = service.filterByDateRange(now.withDayOfMonth(1), now.plusDays(31));
                        } else if ("last_30".equalsIgnoreCase(tp)) {
                            list = service.filterByDateRange(now.minusDays(30), now);
                        } else if ("this_year".equalsIgnoreCase(tp)) {
                            list = service.filterByDateRange(now.withDayOfYear(1), now.plusDays(365));
                        }
                    }

                    sendJsonResponse(exchange, 200, JsonUtil.toJson(list));

                } else if ("POST".equals(method)) {
                    String body = readBody(exchange);
                    Map<String, String> fields = JsonUtil.parseSimpleJson(body);

                    String title = fields.getOrDefault("title", "Untitled");
                    double amount = Double.parseDouble(fields.getOrDefault("amount", "0"));
                    Category cat = Category.fromString(fields.getOrDefault("category", "OTHER"));
                    LocalDate date = LocalDate.now();
                    if (fields.containsKey("date") && !fields.get("date").isEmpty()) {
                        date = LocalDate.parse(fields.get("date"), Expense.DATE_FORMATTER);
                    }
                    PaymentMethod pm = PaymentMethod.fromString(fields.getOrDefault("paymentMethod", "CASH"));
                    String notes = fields.getOrDefault("notes", "");

                    Expense created = service.addExpense(date, title, cat, amount, pm, notes);
                    sendJsonResponse(exchange, 201, JsonUtil.toJson(created));

                } else if ("PUT".equals(method)) {
                    String body = readBody(exchange);
                    Map<String, String> fields = JsonUtil.parseSimpleJson(body);
                    String id = idParam != null ? idParam : fields.get("id");

                    Optional<Expense> opt = service.getExpenseById(id);
                    if (opt.isEmpty()) {
                        sendJsonResponse(exchange, 404, "{\"error\":\"Expense not found\"}");
                        return;
                    }

                    Expense exp = opt.get();
                    if (fields.containsKey("title")) exp.setTitle(fields.get("title"));
                    if (fields.containsKey("amount")) exp.setAmount(Double.parseDouble(fields.get("amount")));
                    if (fields.containsKey("category")) exp.setCategory(Category.fromString(fields.get("category")));
                    if (fields.containsKey("date")) exp.setDate(LocalDate.parse(fields.get("date"), Expense.DATE_FORMATTER));
                    if (fields.containsKey("paymentMethod")) exp.setPaymentMethod(PaymentMethod.fromString(fields.get("paymentMethod")));
                    if (fields.containsKey("notes")) exp.setNotes(fields.get("notes"));

                    service.updateExpense(exp);
                    sendJsonResponse(exchange, 200, JsonUtil.toJson(exp));

                } else if ("DELETE".equals(method)) {
                    if (idParam == null || idParam.isEmpty()) {
                        sendJsonResponse(exchange, 400, "{\"error\":\"Expense ID required\"}");
                        return;
                    }
                    boolean deleted = service.deleteExpense(idParam);
                    if (deleted) {
                        sendJsonResponse(exchange, 200, "{\"success\":true}");
                    } else {
                        sendJsonResponse(exchange, 404, "{\"error\":\"Expense not found\"}");
                    }
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            } finally {
                exchange.close();
            }
        }
    }

    /**
     * Handler for /api/summary
     */
    private class SummaryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                addCorsHeaders(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                List<Expense> all = service.getAllExpenses();
                double totalAllTime = service.calculateTotal(all);
                YearMonth thisMonth = YearMonth.now();
                double monthTotal = service.getTotalSpentForMonth(thisMonth);
                Budget budget = service.getBudget(thisMonth);
                int txCount = all.size();
                double dailyAvg = service.getAverageDailyExpense(thisMonth);
                Expense highest = service.getHighestExpense().orElse(null);
                List<CategorySummary> catSummaries = service.getCategorySummaries(all);
                List<MonthlySummary> monthlySummaries = service.getMonthlySummaries(LocalDate.now().getYear());

                String json = JsonUtil.summaryToJson(totalAllTime, monthTotal, budget, txCount, dailyAvg, highest, catSummaries, monthlySummaries);
                sendJsonResponse(exchange, 200, json);
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            } finally {
                exchange.close();
            }
        }
    }

    /**
     * Handler for /api/budget
     */
    private class BudgetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                addCorsHeaders(exchange);
                String method = exchange.getRequestMethod().toUpperCase();
                YearMonth now = YearMonth.now();

                if ("GET".equals(method)) {
                    Budget b = service.getBudget(now);
                    String json = String.format(Locale.US,
                            "{\"month\":\"%s\",\"limit\":%.2f,\"spent\":%.2f,\"remaining\":%.2f,\"percentage\":%.1f,\"status\":\"%s\"}",
                            now, b.getMonthlyLimit(), b.getCurrentSpent(), b.getRemaining(), b.getPercentageUsed(), b.getStatus().name());
                    sendJsonResponse(exchange, 200, json);
                } else if ("POST".equals(method)) {
                    String body = readBody(exchange);
                    Map<String, String> map = JsonUtil.parseSimpleJson(body);
                    double limit = Double.parseDouble(map.getOrDefault("limit", "2000"));
                    service.setBudget(now, limit);
                    Budget b = service.getBudget(now);
                    String json = String.format(Locale.US,
                            "{\"success\":true,\"limit\":%.2f,\"status\":\"%s\"}",
                            b.getMonthlyLimit(), b.getStatus().name());
                    sendJsonResponse(exchange, 200, json);
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            } finally {
                exchange.close();
            }
        }
    }

    /**
     * Handler for /api/export
     */
    private class ExportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                addCorsHeaders(exchange);
                StringBuilder sb = new StringBuilder("ID,Date,Title,Category,Amount,PaymentMethod,Notes\r\n");
                for (Expense exp : service.getAllExpenses()) {
                    sb.append(exp.toCsvLine()).append("\r\n");
                }
                byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
                exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"expenses.csv\"");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                    os.flush();
                }
            } finally {
                exchange.close();
            }
        }
    }

    /**
     * Handler for /api/reset (clears all transactions & budget for fresh start)
     */
    private class ResetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                addCorsHeaders(exchange);
                String method = exchange.getRequestMethod().toUpperCase();
                if ("POST".equals(method)) {
                    service.clearAll();
                    sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"All data has been reset to a clean state.\"}");
                } else if ("OPTIONS".equals(method)) {
                    exchange.sendResponseHeaders(204, -1);
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            } finally {
                exchange.close();
            }
        }
    }

    /**
     * Serves static assets from web/ folder
     */
    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String pathStr = exchange.getRequestURI().getPath();
                if ("/".equals(pathStr) || pathStr.isEmpty()) {
                    pathStr = "/index.html";
                }

                // Check relative to cwd or relative to execution directory
                Path filePath = Paths.get("web", pathStr.substring(1));
                if (!Files.exists(filePath)) {
                    // Try checking parent or absolute
                    filePath = Paths.get(".", "web", pathStr.substring(1));
                }

                if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                    String msg = "404 Not Found: " + pathStr;
                    exchange.sendResponseHeaders(404, msg.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(msg.getBytes(StandardCharsets.UTF_8));
                        os.flush();
                    }
                    return;
                }

                String contentType = "text/plain";
                if (pathStr.endsWith(".html")) contentType = "text/html; charset=UTF-8";
                else if (pathStr.endsWith(".css")) contentType = "text/css; charset=UTF-8";
                else if (pathStr.endsWith(".js")) contentType = "application/javascript; charset=UTF-8";
                else if (pathStr.endsWith(".json")) contentType = "application/json; charset=UTF-8";
                else if (pathStr.endsWith(".png")) contentType = "image/png";
                else if (pathStr.endsWith(".svg")) contentType = "image/svg+xml";

                byte[] bytes = Files.readAllBytes(filePath);
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                    os.flush();
                }
            } finally {
                exchange.close();
            }
        }
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void sendJsonResponse(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
            os.flush();
        }
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            try {
                if (idx > 0) {
                    map.put(URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8),
                            URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8));
                } else if (idx < 0) {
                    map.put(URLDecoder.decode(pair, StandardCharsets.UTF_8), "");
                }
            } catch (Exception ignored) {}
        }
        return map;
    }
}
