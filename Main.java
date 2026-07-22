import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.isBlank()) {
            port = Integer.parseInt(envPort);
        }
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/generate-bill", new BillHandler());
        server.createContext("/", new StaticHandler());
        server.setExecutor(null);
        System.out.println("FreshMart server started at http://localhost:" + port);
        server.start();
    }
}

class StaticHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath();
        if (requestPath == null || requestPath.equals("/")) {
            requestPath = "/index.html";
        } else if (requestPath.equals("/billing")) {
            requestPath = "/billing.html";
        }

        Path filePath = Path.of("." + requestPath).normalize();
        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            sendResponse(exchange, 404, "text/plain; charset=UTF-8", "404 - File not found");
            return;
        }

        String contentType = getContentType(filePath.toString());
        byte[] response = Files.readAllBytes(filePath);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private String getContentType(String fileName) {
        if (fileName.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        }
        if (fileName.endsWith(".js")) {
            return "application/javascript; charset=UTF-8";
        }
        if (fileName.endsWith(".svg")) {
            return "image/svg+xml; charset=UTF-8";
        }
        if (fileName.endsWith(".html")) {
            return "text/html; charset=UTF-8";
        }
        return "text/plain; charset=UTF-8";
    }

    private void sendResponse(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
}

class BillHandler implements HttpHandler {
    private static final DecimalFormat MONEY = new DecimalFormat("0.00");
    private static final DateTimeFormatter BILL_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Location", "/billing");
            exchange.sendResponseHeaders(302, -1);
            return;
        }

        String formData;
        try (InputStream is = exchange.getRequestBody()) {
            formData = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        Map<String, String> params = parseForm(formData);

        Map<String, Double> prices = new LinkedHashMap<>();
        prices.put("Milk", 30.0);
        prices.put("Bread", 25.0);
        prices.put("Butter", 45.0);
        prices.put("Cheese", 50.0);
        prices.put("Cake", 100.0);
        prices.put("Tea", 20.0);
        prices.put("Coffee", 25.0);
        prices.put("Rice", 60.0);

        List<String> rows = new ArrayList<>();
        double subtotal = 0;
        int totalQuantity = 0;

        for (Map.Entry<String, Double> entry : prices.entrySet()) {
            String itemName = entry.getKey();
            String fieldName = itemName.toLowerCase();
            int quantity = parseInt(params.get(fieldName));
            if (quantity <= 0) {
                continue;
            }

            double price = entry.getValue();
            double lineTotal = quantity * price;
            subtotal += lineTotal;
            totalQuantity += quantity;

            rows.add(
                "<tr><td>" + escapeHtml(itemName) + "</td><td>" + quantity + "</td><td>Rs. "
                    + MONEY.format(price) + "</td><td>Rs. " + MONEY.format(lineTotal) + "</td></tr>"
            );
        }

        double discountPercent = Math.max(0, Math.min(50, parseDouble(params.get("discount"))));
        double discountAmount = subtotal * (discountPercent / 100.0);
        double taxableAmount = subtotal - discountAmount;
        double gst = taxableAmount * 0.05;
        double grandTotal = taxableAmount + gst;

        String customerName = defaultValue(params.get("customerName"), "Walk-in Customer");
        String phone = defaultValue(params.get("phone"), "Not Provided");
        String operator = defaultValue(params.get("operator"), "Cashier");
        String paymentMethod = defaultValue(params.get("paymentMethod"), "Cash");
        String billNo = defaultValue(params.get("billNo"), "FM-0000");
        String generatedAt = LocalDateTime.now().format(BILL_TIME);

        String invoiceRows = rows.isEmpty()
            ? "<tr><td colspan=\"4\">No products were selected. Please go back and add at least one item.</td></tr>"
            : String.join("", rows);

        String html = "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
            + "<title>FreshMart Invoice</title>"
            + "<style>"
            + "body{margin:0;font-family:Segoe UI,Tahoma,sans-serif;background:linear-gradient(135deg,#fff7ea,#edf6ec);color:#1b2417;padding:32px;}"
            + ".invoice{max-width:920px;margin:0 auto;background:#fff;border-radius:26px;padding:32px;box-shadow:0 24px 60px rgba(32,44,23,.14);}"
            + ".top{display:flex;justify-content:space-between;gap:24px;flex-wrap:wrap;}"
            + ".brand h1{margin:0;font-size:36px;} .brand p,.meta p,.summary p{margin:8px 0;color:#56654b;line-height:1.6;}"
            + ".badge{display:inline-block;padding:8px 12px;border-radius:999px;background:#fff2d8;color:#b0621c;font-weight:700;font-size:12px;letter-spacing:.08em;text-transform:uppercase;}"
            + ".grid{display:grid;grid-template-columns:repeat(3,1fr);gap:16px;margin:28px 0;}"
            + ".card{background:#f8fbf7;border:1px solid #e2ecdf;border-radius:18px;padding:18px;}"
            + "table{width:100%;border-collapse:collapse;margin-top:20px;overflow:hidden;border-radius:18px;}"
            + "th,td{padding:14px;border-bottom:1px solid #e8eee4;text-align:left;}th{background:#eff6ec;text-transform:uppercase;font-size:13px;letter-spacing:.06em;}"
            + ".summary{margin-top:24px;margin-left:auto;max-width:320px;background:#f8fbf7;border:1px solid #e2ecdf;border-radius:18px;padding:20px;}"
            + ".summary-row{display:flex;justify-content:space-between;margin:12px 0;gap:12px;}"
            + ".grand{font-size:20px;font-weight:700;color:#204f2c;}"
            + ".footer{margin-top:28px;display:flex;justify-content:space-between;gap:12px;flex-wrap:wrap;}"
            + ".print-btn,.back-btn{display:inline-block;padding:12px 16px;border-radius:14px;text-decoration:none;font-weight:700;}"
            + ".print-btn{background:#2f6f3e;color:#fff;} .back-btn{background:#f2f3ef;color:#1b2417;}"
            + "@media (max-width:720px){body{padding:16px;}.invoice{padding:20px;}.grid{grid-template-columns:1fr;}.summary{max-width:none;}}"
            + "</style></head><body><section class=\"invoice\">"
            + "<div class=\"top\"><div class=\"brand\"><span class=\"badge\">FreshMart Invoice</span><h1>Supermarket Billing Receipt</h1>"
            + "<p>Professional college project billing output with GST, discount, payment mode, and cashier details.</p></div>"
            + "<div class=\"meta\"><p><strong>Bill No:</strong> " + escapeHtml(billNo) + "</p>"
            + "<p><strong>Generated:</strong> " + escapeHtml(generatedAt) + "</p>"
            + "<p><strong>Operator:</strong> " + escapeHtml(operator) + "</p></div></div>"
            + "<div class=\"grid\">"
            + "<div class=\"card\"><strong>Customer Name</strong><p>" + escapeHtml(customerName) + "</p></div>"
            + "<div class=\"card\"><strong>Phone Number</strong><p>" + escapeHtml(phone) + "</p></div>"
            + "<div class=\"card\"><strong>Payment Method</strong><p>" + escapeHtml(paymentMethod) + "</p></div>"
            + "</div>"
            + "<table><thead><tr><th>Item</th><th>Quantity</th><th>Unit Price</th><th>Total</th></tr></thead><tbody>"
            + invoiceRows
            + "</tbody></table>"
            + "<div class=\"summary\">"
            + "<div class=\"summary-row\"><span>Total Quantity</span><strong>" + totalQuantity + "</strong></div>"
            + "<div class=\"summary-row\"><span>Subtotal</span><strong>Rs. " + MONEY.format(subtotal) + "</strong></div>"
            + "<div class=\"summary-row\"><span>Discount (" + MONEY.format(discountPercent) + "%)</span><strong>Rs. " + MONEY.format(discountAmount) + "</strong></div>"
            + "<div class=\"summary-row\"><span>GST (5%)</span><strong>Rs. " + MONEY.format(gst) + "</strong></div>"
            + "<div class=\"summary-row grand\"><span>Grand Total</span><strong>Rs. " + MONEY.format(grandTotal) + "</strong></div>"
            + "</div>"
            + "<div class=\"footer\"><a class=\"back-btn\" href=\"/billing\">Create Another Bill</a>"
            + "<a class=\"print-btn\" href=\"#\" onclick=\"window.print();return false;\">Print Invoice</a></div>"
            + "</section></body></html>";

        byte[] response = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private Map<String, String> parseForm(String formData) throws IOException {
        Map<String, String> map = new HashMap<>();
        if (formData == null || formData.isBlank()) {
            return map;
        }

        String[] pairs = formData.split("&");
        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            map.put(key, value);
        }
        return map;
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String escapeHtml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
