import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    private static String directory;

    public static void main(String[] args) {
        if (args.length != 2 || !args[0].equals("--directory")) {
            System.out.println("Usage: ./your_program.sh --directory <path>");
            return;
        }
        directory = args[1];

        System.out.println("Logs from your program will appear here!");
        try (ServerSocket serverSocket = new ServerSocket(4221)) {
            serverSocket.setReuseAddress(true);
            while (true) {
                Socket clientSocket = serverSocket.accept(); // Wait for connection from client.
                System.out.println("Accepted new connection");
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (InputStream input = clientSocket.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                 OutputStream output = clientSocket.getOutputStream()) {

                String line = reader.readLine();
                String[] HttpRequest = line.split(" ", 0);

                // Read headers
                while (!(line = reader.readLine()).isEmpty()) {
                    // No need to process headers for this task
                }

                if (HttpRequest[1].startsWith("/files/")) {
                    String filename = HttpRequest[1].substring(7);
                    Path filePath = Paths.get(directory, filename);
                    if (Files.exists(filePath)) {
                        byte[] fileContent = Files.readAllBytes(filePath);
                        String response = String.format(
                                "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: %d\r\n\r\n",
                                fileContent.length);
                        output.write(response.getBytes());
                        output.write(fileContent);
                    } else {
                        output.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
                    }
                } else {
                    output.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
                }
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.out.println("IOException: " + e.getMessage());
                }
            }
        }
    }
}