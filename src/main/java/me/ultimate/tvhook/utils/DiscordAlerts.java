package me.ultimate.tvhook.utils;

import com.binance.api.client.domain.OrderStatus;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.ultimate.tvhook.Main;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DiscordAlerts {
    public static void sendAlert(OrderStatus reason, PlaceholderMap map) {
        JsonObject json = new JsonObject();
        JsonArray embeds = new JsonArray();

        JsonObject embed = new JsonObject();
        String key = "trading.alerts.on-order-" + (reason == OrderStatus.NEW ? "placed" : reason.toString().toLowerCase());
        Configuration config = Main.getConfig();

        if (!config.getBoolean(key + ".enabled", true)) return;

        embed.addProperty("title", map.apply(config.getString(key + ".title")));

        String desc = map.apply(config.getString(key + ".description"));
        if ("false".equals(map.get("isbuy"))) desc += map.apply(config.getString(key + ".description-if-sell"));
        embed.addProperty("description", desc.replace("\\n", "\n"));

        String col = map.apply(config.getString(key + ".color"));
        int parsedCol = Utils.getColor(col);
        embed.addProperty("color", parsedCol > 0 ? parsedCol : Integer.parseInt(col));

        embeds.add(embed);
        json.add("embeds", embeds);

        String content = config.getString(key + ".content");
        if (content != null && !content.isEmpty()) json.addProperty("content", content);
        sendAlert(json);
    }

    public static void sendAlert(JsonObject message) {
        if (!Main.getConfig().getBoolean("trading.alerts.enabled", false)) return;

        try {
            URI url = new URL(Main.getConfig().getString("trading.alerts.webhook-url")).toURI();

            HttpRequest req = HttpRequest.newBuilder(url)
            .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(message.toString()))
            .build();

            HttpClient.newHttpClient().sendAsync(req, HttpResponse.BodyHandlers.ofString());
            Main.getLogger().info("Sent Discord alert");
        } catch (Exception e) {
            Main.getLogger().error("Error sending discord alert: " + e.getMessage());
        }
    }
}
