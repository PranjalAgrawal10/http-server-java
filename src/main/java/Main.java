import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        // Your
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.out.println("Logs from your program will appear here!");

        try {
            ServerSocket serverSocket = new ServerSocket(4221);

            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);

            Socket clientSocket = serverSocket.accept(); // Wait for connection from client.
            System.out.println("accepted new connection");

            // Read HTTP request
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String requestLine = reader.readLine();
            System.out.println("Request Line: " + requestLine);

            // Extract URL path from request line
            String[] requestParts = requestLine.split(" ");
            String urlPath = requestParts[1];
            System.out.println("URL Path: " + urlPath);

            // Determine response based on URL path
            String httpResponse;
            if (urlPath.startsWith("/echo/")) {
                String echoString = urlPath.substring(6); // Extract the string after /echo/
                String contentType = "text/plain";
                int contentLength = echoString.length();
                httpResponse = "HTTP/1.1 200 OK\r\n" +
                               "Content-Type: " + contentType + "\r\n" +
                               "Content-Length: " + contentLength + "\r\n" +
                               "\r\n" +
                               echoString;
            } else {
                httpResponse = "HTTP/1.1 404 Not Found\r\n\r\n";
            }

            // Send HTTP response
            OutputStream outputStream = clientSocket.getOutputStream();
            outputStream.write(httpResponse.getBytes("UTF-8"));
            outputStream.flush();

            clientSocket.close();
            serverSocket.close();
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}