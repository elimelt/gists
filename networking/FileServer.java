package networking;

import com.sun.net.httpserver.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import java.nio.file.*;

public class FileServer {
    private static final int PORT = 8080;
    private static final String FILE_PATH = "/Users/emm12/repos/gists/BloomFilter.java"; // Replace with your file path

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", new FileHandler());
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        System.out.println("Reverse proxy started on port " + PORT + ", serving file: " + FILE_PATH);
    }

    static class FileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            File file = new File(FILE_PATH);
            if (!file.exists()) {
                String response = "404 (Not Found)\n";
                exchange.sendResponseHeaders(404, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }

            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, file.length());

            try (OutputStream os = exchange.getResponseBody()) {
                Files.copy(file.toPath(), os);
            }
        }
    }

    static class ProxyHandler implements HttpHandler {
      @Override
      public void handle(HttpExchange exchange) throws IOException {
          String path = exchange.getRequestURI().toString();
          String method = exchange.getRequestMethod();
    
        URI uri = URI.create("http://localhost:8080" + path);
          HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
          conn.setRequestMethod(method);
    
          // Copy request headers
          for (Map.Entry<String, List<String>> header : exchange.getRequestHeaders().entrySet()) {
              conn.setRequestProperty(header.getKey(), String.join(",", header.getValue()));
          }
    
          // Copy request body for POST, PUT, etc.
          if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
              conn.setDoOutput(true);
              exchange.getRequestBody().transferTo(conn.getOutputStream());
          }
    
          // Get the response from the backend server
          int responseCode = conn.getResponseCode();
          exchange.sendResponseHeaders(responseCode, 0);
    
          // Copy response headers
          for (Map.Entry<String, List<String>> header : conn.getHeaderFields().entrySet()) {
              if (header.getKey() != null) {
                  exchange.getResponseHeaders().put(header.getKey(), header.getValue());
              }
          }
    
          // Copy response body
          try (OutputStream os = exchange.getResponseBody()) {
              conn.getInputStream().transferTo(os);
          }
    
          conn.disconnect();
      }
    }
}
