package me.ultimate.tvhook.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.ultimate.tvhook.Main;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DiscordAlerts {
    public static void sendAlert(String message) {
        JsonObject json = new JsonObject();
        json.addProperty("content", message);

        sendAlert(json);
    }

    public static void sendEmbed(String title, String description, int color, boolean mention) {
        JsonObject json = new JsonObject();
        JsonArray embeds = new JsonArray();

        JsonObject embed = new JsonObject();
        embed.addProperty("title", title);
        embed.addProperty("description", description);
        embed.addProperty("color", color);
        embeds.add(embed);

        json.add("embeds", embeds);
        if (mention) json.addProperty("content", "@everyone");
        sendAlert(json);
    }

    public static void sendAlert(JsonObject message) {
        if (!Main.getConfig().getBoolean("trading.alerts.discord.enabled", false)) return;

        try {
            URI url = new URL(Main.getConfig().getString("trading.alerts.discord.webhook-url")).toURI();

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
