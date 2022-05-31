package me.ultimate.tvhook;

import com.binance.api.client.domain.OrderSide;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.ultimate.tvhook.utils.HttpException;
import me.ultimate.tvhook.utils.Utils;
import me.ultimate.tvhook.utils.WebhookSignal;
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
        if (PORT != 80 && PORT != 443)
            LOGGER.warn("TradingView webhooks only support ports 80 and 443");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            LOGGER.info("Webhook server started on port " + PORT);
            String prefix = "http" + (PORT == 443 ? "s" : "") + "://";
            String suffix = (PORT != 80 && PORT != 443 ? ":" + PORT : "") + Main.getConfig().getString("server.signal-path");
            LOGGER.info("-- Server running on --");
            LOGGER.info("Local: " + prefix + Utils.getLocalIP() + suffix);
            LOGGER.info("Public: " + prefix + Utils.getPublicIP() + suffix);

            while (true) {
                try (Socket socket = serverSocket.accept()) {
                    if (!isIPAllowed(socket)) continue;

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

        if (!headers.get("path").equals(Main.getConfig().getString("server.signal-path"))) {
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
        } catch (Exception ex) {
            status = (ex instanceof HttpException) ? ((HttpException) ex).getStatus() : 500;
            response = "{ \"error\": \"" + ex.getMessage() + "\" }";
        }

        OutputStream writer = socket.getOutputStream();
        writer.write(("HTTP/1.1 " + status + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        writer.write(response.getBytes(StandardCharsets.UTF_8));
        writer.close();
    }

    private void onSignal(JsonObject message) throws HttpException {
        String action = message.get("action").getAsString().toUpperCase();
        String type = message.get("type").getAsString().toUpperCase();
        String currency = message.get("currency").getAsString();
        String priceStr = message.get("price").getAsString().toUpperCase();
        boolean isMkt = priceStr.equals("MKT") || priceStr.equals("MARKET");
        double price = isMkt ? -1 : message.get("price").getAsDouble();
        String token = message.get("token").getAsString();

        if (!Main.getConfig().getString("server.token").equals(token))
            throw new HttpException(401, "Not authorized");

        if ("FLAT".equals(type)) type = "LONG";
        if (!"LONG".equals(type))
            throw new HttpException(400, "Only LONG positions are supported right now");

        if (currency == null || currency.isEmpty())
            throw new HttpException(400, "Invalid parameters");

        message.remove("action");
        message.remove("type");
        message.remove("currency");
        message.remove("price");
        message.remove("token");

        WebhookSignal signal = new WebhookSignal(OrderSide.valueOf(action), type, currency, price, message.size() > 0 ? message : null);
        LOGGER.info("Received signal: " + signal);

        Main.getQueue().add(() -> Main.onSignal(signal));
    }

    private boolean isIPAllowed(Socket conn) {
        String ip = conn.getInetAddress().getHostAddress();

        for (String allowedIP : ALLOWED_IPS)
            if (allowedIP.equals(ip)) return true;

        return false;
    }
}
