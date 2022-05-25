package me.ultimate.tvhook.utils;

import com.binance.api.client.domain.OrderSide;
import com.google.gson.JsonObject;

public class WebhookSignal {
    private final OrderSide action;
    private final String type;
    private final String currency;
    private final double price;
    private final JsonObject customPayload;

    public WebhookSignal(OrderSide action, String type, String currency, double price, JsonObject customPayload) {
        this.action = action;
        this.type = type;
        this.currency = currency;
        this.price = price;
        this.customPayload = customPayload;
    }

    public OrderSide getAction() {
        return action;
    }

    public String getType() {
        return type;
    }

    public String getCurrency() {
        return currency;
    }

    public String getCrypto() {
        return currency.substring(0, 3);
    }

    public String getFiat() {
        return currency.substring(3);
    }

    public double getPrice() {
        return price;
    }

    /** May be null if no extra data was sent in the request */
    public JsonObject getCustomPayload() {
        return customPayload;
    }

    public String toString() {
        return "WebhookSignal: " + action + " " + type + " " + currency + " " + price;
    }
}
