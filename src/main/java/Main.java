import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
public class Main {
    public static void main(String[] args) throws IOException {
        // You can use print statements as follows for debugging, they'll be visible
        // when running tests.
        System.out.println("Logs from your program will appear here!");
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        DataOutputStream out = null;
        Map<String, String> httpRequest = new HashMap<>();
        try {
            serverSocket = new ServerSocket(4221);
            serverSocket.setReuseAddress(true);
            clientSocket = serverSocket.accept(); // Wait for connection from client.
            // use client request to determine if endpoint is valid
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
            int readableBytes = clientSocket.getInputStream().available();
            byte[] textAsBytes =
                    clientSocket.getInputStream().readNBytes(readableBytes);
            // parse text to see values
            parseRequest(httpRequest, textAsBytes);
            out = new DataOutputStream(clientSocket.getOutputStream());
            if ("/".equals(httpRequest.get("target"))) {
                out.write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
            } else if (httpRequest.get("target").startsWith("/echo/")) {
                String queryParam = httpRequest.get("target").split("/")[2];
                out.write(
                        ("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " +
                                queryParam.length() + "\r\n\r\n" + queryParam)
                                .getBytes());
            } else {
                out.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
            }
            out.flush();
            System.out.println("accepted new connection");
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private static void parseRequest(Map<String, String> ret, byte[] text) {
        String s = new String(text);
        String[] requestSplit = s.split("\r\n");
        parseRequestLine(ret, requestSplit[0]);
    }

    private static void parseRequestLine(Map<String, String> ret, String s) {
        String[] requestLineSplit = s.split(" ");
        ret.put("method", requestLineSplit[0].trim());
        ret.put("target", requestLineSplit[1].trim());
        ret.put("version", requestLineSplit[2].trim());
    }
}