import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        try {
            serverSocket = new ServerSocket(4221);
            serverSocket.setReuseAddress(true);
            clientSocket = serverSocket.accept(); // Wait for connection from client.
            System.out.println("accepted new connection");
            InputStream input = clientSocket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String line = reader.readLine();
            String[] HttpRequest = line.split(" ", 0);
            String userAgent = null;

            // Read headers
            while (!(line = reader.readLine()).isEmpty()) {
                if (line.startsWith("User-Agent:")) {
                    userAgent = line.substring("User-Agent:".length()).trim();
                }
            }

            if (HttpRequest[1].equals("/")) {
                clientSocket.getOutputStream().write(
                        "HTTP/1.1 200 OK\r\n\r\n".getBytes());
            } else if (HttpRequest[1].startsWith("/echo/")) {
                String msg = HttpRequest[1].substring(6);
                String header = String.format(
                        "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %d\r\n\r\n%s",
                        msg.length(), msg);
                clientSocket.getOutputStream().write(header.getBytes());
            } else if (HttpRequest[1].equals("/user-agent") && userAgent != null) {
                String response = String.format(
                        "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %d\r\n\r\n%s",
                        userAgent.length(), userAgent);
                clientSocket.getOutputStream().write(response.getBytes());
            } else {
                clientSocket.getOutputStream().write(
                        "HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } finally {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
            }
        }
    }
}