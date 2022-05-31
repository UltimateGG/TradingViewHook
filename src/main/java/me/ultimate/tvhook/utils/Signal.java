package me.ultimate.tvhook.utils;

import com.binance.api.client.domain.OrderSide;
import com.google.gson.JsonObject;
import me.ultimate.tvhook.Main;

public class Signal {
    private final OrderSide action;
    private final String type;
    private final String currency;
    private final double price;
    private final JsonObject customPayload;

    public Signal(OrderSide action, String type, String currency, double price, JsonObject customPayload) {
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
        return currency.replace(getFiat(), "");
    }

    public String getFiat() {
        return Main.getConfig().getString("trading.fiat");
    }

    public double getPrice() {
        return price;
    }

    public boolean isMarketOrder() {
        return price < 0.0;
    }

    /** May be null if no extra data was sent in the request */
    public JsonObject getCustomPayload() {
        return customPayload;
    }

    public String toString() {
        return "Signal: " + action + " " + type + " " + currency + " " + price;
    }
}
