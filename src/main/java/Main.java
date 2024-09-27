import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
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
                Socket clientSocket =
                        serverSocket.accept(); // Wait for connection from client.
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
            BufferedReader inputStream = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));
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
            String urlPath = requestLine.split(" ")[1];
            OutputStream outputStream = clientSocket.getOutputStream();
            // Write the HTTP response to the output stream.
            String httpResponse = getHttpResponse(urlPath, headers);
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

    private static String getHttpResponse(String urlPath, Map<String, String> headers)
            throws IOException {
        String httpResponse;
        if ("/".equals(urlPath)) {
            httpResponse = "HTTP/1.1 200 OK\r\n\r\n";
        } else if (urlPath.startsWith("/echo/")) {
            String echoStr =
                    urlPath.substring(6); // Extract the string after "/echo/"
            httpResponse =
                    "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " +
                            echoStr.length() + "\r\n\r\n" + echoStr;
        } else if ("/user-agent".equals(urlPath)) {
            String userAgent = headers.get("User-Agent");
            httpResponse =
                    "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " +
                            userAgent.length() + "\r\n\r\n" + userAgent;
        } else if (urlPath.startsWith("/files/")) {
            String filename =
                    urlPath.substring(7); // Extract the filename after "/files/"
            File file = new File(directory, filename);
            if (file.exists()) {
                byte[] fileContent = Files.readAllBytes(file.toPath());
                httpResponse =
                        "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: " +
                                fileContent.length + "\r\n\r\n" + new String(fileContent);
            } else {
                httpResponse = "HTTP/1.1 404 Not Found\r\n\r\n";
            }
        } else {
            httpResponse = "HTTP/1.1 404 Not Found\r\n\r\n";
        }
        return httpResponse;
    }
}