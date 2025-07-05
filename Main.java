import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new PageHandler());
        server.createContext("/bill", new BillHandler());
        server.setExecutor(null);
        System.out.println(" Server started at http://localhost:8080");
        server.start();
    }
}

class PageHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        File file = new File("index.html");
        byte[] response = Files.readAllBytes(file.toPath());
        exchange.getResponseHeaders().add("Content-Type", "text/html");
        exchange.sendResponseHeaders(200, response.length);
        OutputStream os = exchange.getResponseBody();
        os.write(response);
        os.close();
    }
}

class BillHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            InputStream is = exchange.getRequestBody();
            String formData = new String(is.readAllBytes());
            Map<String, String> params = parseForm(formData);

            int milkQty = Integer.parseInt(params.getOrDefault("milk", "0"));
            int breadQty = Integer.parseInt(params.getOrDefault("bread", "0"));
            int butterQty = Integer.parseInt(params.getOrDefault("butter", "0"));
            int cheeseQty = Integer.parseInt(params.getOrDefault("cheese", "0"));
            int cakeQty = Integer.parseInt(params.getOrDefault("cake", "0"));
            int teaQty = Integer.parseInt(params.getOrDefault("tea", "0"));
            int coffeeQty = Integer.parseInt(params.getOrDefault("coffee", "0"));

            double milkPrice = 30, breadPrice = 25, butterPrice = 45;
            double cheesePrice = 50, cakePrice = 100, teaPrice = 20, coffeePrice = 25;

            double total = milkQty * milkPrice + breadQty * breadPrice + butterQty * butterPrice +
                           cheeseQty * cheesePrice + cakeQty * cakePrice +
                           teaQty * teaPrice + coffeeQty * coffeePrice;

            double gst = total * 0.05;
            double grand = total + gst;

            String html = "<html><head><title>Bill</title><style>" +
                    "body{font-family:Arial;} table{margin:auto;border-collapse:collapse;}" +
                    "th,td{border:1px solid #ccc;padding:10px;}</style></head><body>" +
                    "<h2>🧾 Supermarket Bill</h2><table><tr><th>Item</th><th>Qty</th><th>Price</th><th>Total</th></tr>";

            if (milkQty > 0) html += "<tr><td>Milk</td><td>" + milkQty + "</td><td>₹30</td><td>₹" + (milkQty * milkPrice) + "</td></tr>";
            if (breadQty > 0) html += "<tr><td>Bread</td><td>" + breadQty + "</td><td>₹25</td><td>₹" + (breadQty * breadPrice) + "</td></tr>";
            if (butterQty > 0) html += "<tr><td>Butter</td><td>" + butterQty + "</td><td>₹45</td><td>₹" + (butterQty * butterPrice) + "</td></tr>";
            if (cheeseQty > 0) html += "<tr><td>Cheese</td><td>" + cheeseQty + "</td><td>₹50</td><td>₹" + (cheeseQty * cheesePrice) + "</td></tr>";
            if (cakeQty > 0) html += "<tr><td>Cake</td><td>" + cakeQty + "</td><td>₹100</td><td>₹" + (cakeQty * cakePrice) + "</td></tr>";
            if (teaQty > 0) html += "<tr><td>Tea</td><td>" + teaQty + "</td><td>₹20</td><td>₹" + (teaQty * teaPrice) + "</td></tr>";
            if (coffeeQty > 0) html += "<tr><td>Coffee</td><td>" + coffeeQty + "</td><td>₹25</td><td>₹" + (coffeeQty * coffeePrice) + "</td></tr>";

            html += "</table><p>Subtotal: ₹" + total + "</p><p>GST (5%): ₹" + gst +
                    "</p><p><b>Grand Total: ₹" + grand + "</b></p></body></html>";

            exchange.getResponseHeaders().add("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, html.length());
            OutputStream os = exchange.getResponseBody();
            os.write(html.getBytes());
            os.close();
        }
    }

    private Map<String, String> parseForm(String formData) throws UnsupportedEncodingException {
        Map<String, String> map = new HashMap<>();
        String[] pairs = formData.split("&");
        for (String pair : pairs) {
            String[] parts = pair.split("=");
            if (parts.length == 2) {
                map.put(URLDecoder.decode(parts[0], "UTF-8"), URLDecoder.decode(parts[1], "UTF-8"));
            }
        }
        return map;
    }
}
