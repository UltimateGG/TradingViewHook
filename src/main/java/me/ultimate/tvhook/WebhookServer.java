package me.ultimate.tvhook;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class WebhookServer extends Thread {
    /** TradingView IPs */
    private static final String[] ALLOWED_IPS = {
            "52.89.214.238",
            "34.212.75.30",
            "54.218.53.128",
            "52.32.178.7",
            Main.DEVELOPMENT_MODE ? "0:0:0:0:0:0:0:1" : "no-ip"
    };
    private static final int PORT = Main.getConfig().getInt("server.port", 80);
    private static final Logger LOGGER = LogManager.getLogger("Server");


    public WebhookServer() {
        super("WebhookServer");
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            LOGGER.info("Webhook server started on port " + PORT);

            while (true) {
                try (Socket socket = serverSocket.accept()) {
                    if (!isIPAllowed(socket)) {
                        socket.close();
                        continue;
                    }

                    LOGGER.info("Accepted connection from " + socket.getInetAddress().getHostAddress());

                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                        onHit(socket, parseHeaders(socket, reader), reader);
                    }
                } catch (Exception e) { // Malformed request, etc.
                    LOGGER.error("Exception while handling request", e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to start server", e);
            System.exit(1);
        }
    }

    private HashMap<String, String> parseHeaders(Socket socket, BufferedReader reader) throws Exception {
        HashMap<String, String> headers = new HashMap<>();
        String rawHttp = reader.readLine();

        while (!rawHttp.isEmpty()) {
            if (rawHttp.length() > 1024) {
                LOGGER.error("Request too long");
                socket.close();
                return headers;
            }

            if (headers.size() == 0) {
                headers.put("method", rawHttp.split(" ")[0]);
                headers.put("path", rawHttp.split(" ")[1]);
                rawHttp = reader.readLine();
                continue;
            }

            String[] header = rawHttp.split(": ");
            headers.put(header[0].toLowerCase(), header[1]);
            rawHttp = reader.readLine();
        }

        return headers;
    }

    private void onHit(Socket socket, HashMap<String, String> headers, BufferedReader reader) throws Exception {
        if (!headers.containsKey("method") || !"POST".equals(headers.get("method")) || !headers.containsKey("content-length") || !headers.containsKey("content-type")
                || !headers.get("content-type").contains("application/json")) {
            LOGGER.warn("Received an invalid request");
            return;
        }

        if (!headers.get("path").equals(Main.getConfig().getString("server.path"))) {
            LOGGER.warn("Received invalid request with path of: " + headers.get("path"));
            return;
        }

        int contentLength = Integer.parseInt(headers.get("content-length"));
        if (contentLength > 1024 * 1024) return;
        char[] content = new char[contentLength];

        reader.read(content);

        int status = 200;
        String response = "{ \"submitted\": true }";
        JsonObject jsonObject = new JsonParser().parse(new String(content)).getAsJsonObject();

        try {
            onSignal(jsonObject);
        } catch (Exception msg) {
            status = 500;
            response = "{ \"error\": \"" + msg.getMessage() + "\" }";
        }

        OutputStream writer = socket.getOutputStream();
        writer.write(("HTTP/1.1 " + status + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        writer.write(response.getBytes(StandardCharsets.UTF_8));
        writer.close();
    }

    private void onSignal(JsonObject message) throws Exception {

    }

    private boolean isIPAllowed(Socket conn) {
        String ip = conn.getInetAddress().getHostAddress();

        for (String allowedIP : ALLOWED_IPS)
            if (allowedIP.equals(ip)) return true;

        return false;
    }
}
