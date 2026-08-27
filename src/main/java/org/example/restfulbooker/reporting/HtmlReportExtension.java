package org.example.restfulbooker.reporting;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Creates a lightweight suite report without coupling the JUnit 5 framework to
 * an obsolete reporting dependency.
 */
public final class HtmlReportExtension implements BeforeAllCallback, AfterAllCallback, TestWatcher {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(HtmlReportExtension.class);
    private static final String RESOURCE_KEY = "suite-report";
    private static final Path REPORT_DIRECTORY = Path.of("target", "failure-logs");
    private static final Path REPORT_FILE = REPORT_DIRECTORY.resolve("index.html");

    @Override
    public void beforeAll(ExtensionContext context) {
        getReport(context);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        getReport(context).write();
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        getReport(context).add(context, "PASS", Optional.empty());
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        getReport(context).add(context, "FAIL", Optional.of(cause));
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        getReport(context).add(context, "ABORTED", Optional.of(cause));
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        getReport(context).add(context, "SKIPPED", reason.map(IllegalStateException::new));
    }

    private SuiteReport getReport(ExtensionContext context) {
        return context.getRoot()
                .getStore(NAMESPACE)
                .getOrComputeIfAbsent(RESOURCE_KEY, key -> new SuiteReport(), SuiteReport.class);
    }

    private static final class SuiteReport implements ExtensionContext.Store.CloseableResource {
        private final List<Result> results = new ArrayList<>();

        synchronized void add(ExtensionContext context, String status, Optional<Throwable> failure) {
            results.add(new Result(context.getDisplayName(), status,
                    failure.map(Throwable::toString).orElse("")));
        }

        synchronized void write() {
            try {
                Files.createDirectories(REPORT_DIRECTORY);
                StringBuilder html = new StringBuilder("""
                        <!doctype html>
                        <html lang="en">
                        <head>
                          <meta charset="utf-8">
                          <title>Restful Booker API Test Report</title>
                          <style>
                            body { font-family: sans-serif; margin: 2rem; }
                            table { border-collapse: collapse; width: 100%; }
                            th, td { border: 1px solid #ddd; padding: .6rem; text-align: left; }
                            th { background: #f3f4f6; }
                            .PASS { color: #16803c; } .FAIL { color: #b42318; }
                            .ABORTED, .SKIPPED { color: #b54708; }
                          </style>
                        </head>
                        <body>
                        <h1>Restful Booker API Test Report</h1>
                        <p>Generated: """);
                html.append(escape(Instant.now().toString())).append("""
                        </p>
                        <table><thead><tr><th>Test</th><th>Status</th><th>Details</th></tr></thead><tbody>
                        """);
                for (Result result : results) {
                    html.append("<tr><td>").append(escape(result.name()))
                            .append("</td><td class=\"").append(result.status()).append("\">")
                            .append(result.status()).append("</td><td>")
                            .append(escape(result.details())).append("</td></tr>");
                }
                html.append("</tbody></table></body></html>");
                Files.writeString(REPORT_FILE, html, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException("Unable to write test report: " + REPORT_FILE, e);
            }
        }

        @Override
        public void close() {
            write();
        }
    }

    private record Result(String name, String status, String details) {
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
