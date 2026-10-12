package com.expensetracker;

import com.expensetracker.repository.CsvExpenseRepository;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.ui.cli.ExpenseTrackerCLI;
import com.expensetracker.ui.gui.ExpenseTrackerGUI;
import com.expensetracker.web.WebServer;

import java.awt.Desktop;
import java.net.URI;

public class Main {
    private static final int DEFAULT_PORT = 8088;

    public static void main(String[] args) {
        ExpenseRepository repository = new CsvExpenseRepository();
        ExpenseService service = new ExpenseService(repository);

        boolean forceCli = false;
        boolean forceGui = false;
        int port = DEFAULT_PORT;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--cli".equalsIgnoreCase(arg) || "-c".equalsIgnoreCase(arg)) {
                forceCli = true;
            } else if ("--gui".equalsIgnoreCase(arg) || "-g".equalsIgnoreCase(arg)) {
                forceGui = true;
            } else if ("--port".equalsIgnoreCase(arg) || "-p".equalsIgnoreCase(arg)) {
                if (i + 1 < args.length) {
                    try {
                        port = Integer.parseInt(args[++i]);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        if (forceCli) {
            startCli(service);
        } else if (forceGui) {
            startGui(service);
        } else {
            // Default to Web Server mode so it runs as a website
            startWeb(service, port);
        }
    }

    private static void startWeb(ExpenseService service, int port) {
        try {
            WebServer webServer = new WebServer(service, port);
            int actualPort = webServer.start();

            // Auto-launch the web browser
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI("http://localhost:" + actualPort));
                }
            } catch (Throwable ignored) {}

            // Keep main thread alive so server stays active
            System.out.println("Server is running. Press Ctrl+C in this window to stop.");
            Thread.currentThread().join();

        } catch (InterruptedException e) {
            System.out.println("Web server stopped.");
        } catch (Exception e) {
            System.err.println("Failed to start web server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void startCli(ExpenseService service) {
        ExpenseTrackerCLI cli = new ExpenseTrackerCLI(service);
        cli.start();
    }

    private static void startGui(ExpenseService service) {
        ExpenseTrackerGUI.launch(service);
    }
}
