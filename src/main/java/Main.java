import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class Main {
    private static String directory;

    public static void main(String[] args) {
        // Parse command line arguments
        if (args.length > 1 && args[0].equals("--directory")) {
            directory = args[1];
        }
        System.out.println("Logs from your program will appear here!");
        try (ServerSocket serverSocket = new ServerSocket(4221)) {
            serverSocket.setReuseAddress(true);
            while (true) {
                Socket clientSocket = serverSocket.accept(); // Wait for connection from client.
                System.out.println("accepted new connection");
                // Handle each client connection in a separate thread.
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private static void handleClient(Socket clientSocket) {
        try {
            BufferedReader inputStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            // Read the request line
            String requestLine = inputStream.readLine();
            // Read all the headers from the HTTP request.
            Map<String, String> headers = new HashMap<>();
            String headerLine;
            while (!(headerLine = inputStream.readLine()).isEmpty()) {
                String[] headerParts = headerLine.split(": ");
                headers.put(headerParts[0], headerParts[1]);
            }
            // Extract the URL path from the request line.
            String[] requestParts = requestLine.split(" ");
            String method = requestParts[0];
            String urlPath = requestParts[1];
            OutputStream outputStream = clientSocket.getOutputStream();
            // Handle the request based on the method and URL path.
            String httpResponse;
            if ("POST".equals(method) && urlPath.startsWith("/files/")) {
                String filename = urlPath.substring(7); // Extract the filename after "/files/"
                int contentLength = Integer.parseInt(headers.get("Content-Length"));
                char[] body = new char[contentLength];
                inputStream.read(body, 0, contentLength);
                File file = new File(directory, filename);
                try (FileWriter fileWriter = new FileWriter(file)) {
                    fileWriter.write(body);
                }
                httpResponse = "HTTP/1.1 201 Created\r\n\r\n";
            } else {
                httpResponse = getHttpResponse(method, urlPath, headers);
            }
            // Write the HTTP response to the output stream.
            outputStream.write(httpResponse.getBytes("UTF-8"));
            // Close the input and output streams.
            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } finally {
            // Close the client socket.
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
            }
        }
    }

    private static String getHttpResponse(String method, String urlPath, Map<String, String> headers) throws IOException {
        String httpResponse;
        boolean gzipSupported = headers.getOrDefault("Accept-Encoding", "").contains("gzip");
        if ("GET".equals(method) && "/".equals(urlPath)) {
            httpResponse = "HTTP/1.1 200 OK\r\n\r\n";
        } else if ("GET".equals(method) && urlPath.startsWith("/echo/")) {
            String echoStr = urlPath.substring(6); // Extract the string after "/echo/"
            byte[] responseBody = echoStr.getBytes("UTF-8");
            if (gzipSupported) {
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                try (GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {
                    gzipStream.write(responseBody);
                }
                responseBody = byteStream.toByteArray();
                httpResponse = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Encoding: gzip\r\nContent-Length: " + responseBody.length + "\r\n\r\n";
            } else {
                httpResponse = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " + responseBody.length + "\r\n\r\n";
            }
            httpResponse += new String(responseBody, "ISO-8859-1");
        } else if ("GET".equals(method) && "/user-agent".equals(urlPath)) {
            String userAgent = headers.get("User-Agent");
            httpResponse = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " + userAgent.length() + "\r\n";
            if (gzipSupported) {
                httpResponse += "Content-Encoding: gzip\r\n";
            }
            httpResponse += "\r\n" + userAgent;
        } else if ("GET".equals(method) && urlPath.startsWith("/files/")) {
            String filename = urlPath.substring(7); // Extract the filename after "/files/"
            File file = new File(directory, filename);
            if (file.exists()) {
                byte[] fileContent = Files.readAllBytes(file.toPath());
                if (gzipSupported) {
                    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                    try (GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {
                        gzipStream.write(fileContent);
                    }
                    fileContent = byteStream.toByteArray();
                    httpResponse = "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Encoding: gzip\r\nContent-Length: " + fileContent.length + "\r\n\r\n";
                } else {
                    httpResponse = "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: " + fileContent.length + "\r\n\r\n";
                }
                httpResponse += new String(fileContent, "ISO-8859-1");
            } else {
                httpResponse = "HTTP/1.1 404 Not Found\r\n\r\n";
            }
        } else {
            httpResponse = "HTTP/1.1 404 Not Found\r\n\r\n";
        }
        return httpResponse;
    }
}