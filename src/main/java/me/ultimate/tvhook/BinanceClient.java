package me.ultimate.tvhook;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.config.BinanceApiConfig;
import com.binance.api.client.domain.TimeInForce;
import com.binance.api.client.domain.account.NewOrder;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.account.request.CancelOrderRequest;
import com.binance.api.client.domain.account.request.OrderRequest;
import me.ultimate.tvhook.utils.Configuration;
import me.ultimate.tvhook.utils.Utils;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class BinanceClient {
    private BinanceApiClientFactory factory;
    private final BinanceApiRestClient CLIENT;
    private final BinanceApiWebSocketClient WEBSOCKET; // TODO: Not sure if we need live data


    public BinanceClient(Configuration config) {
        if (config.getBoolean("binance.isUS", false))
            BinanceApiConfig.setBaseDomain("binance.us");

        CLIENT = login(config.getString("binance.api-key"), config.getString("binance.api-secret"));
        WEBSOCKET = factory.newWebSocketClient();
    }

    private BinanceApiRestClient login(String apiKey, String secretKey) {
        Main.getLogger().info("Logging into Binance...");

        factory = BinanceApiClientFactory.newInstance(apiKey, secretKey);
        BinanceApiRestClient newClient = factory.newRestClient();

        try {
            if (!newClient.getAccount().isCanTrade())
                throw new Exception("Binance account cannot trade");
        } catch (Exception e) {
            Main.getLogger().error("Failed to login to Binance", e);
            System.exit(1);
        }

        Main.getLogger().info("Login successful");
        return newClient;
    }

    public BinanceApiRestClient getClient() {
        return CLIENT;
    }

    public BinanceApiWebSocketClient getWebsocket() {
        return WEBSOCKET;
    }

    public double getBalance(String symbol) {
        return Double.parseDouble(CLIENT.getAccount().getAssetBalance(symbol).getFree());
    }

    /** @param price The limit/price to purchase at */
    public NewOrderResponse limitBuy(String symbol, double quantity, double price) {
        String quantityString = Utils.convertTradeAmount(quantity, price, symbol);
        String priceString = Utils.decToStr(price);

        NewOrderResponse res = CLIENT.newOrder(NewOrder.limitBuy(symbol, TimeInForce.GTC, quantityString, priceString));

        Main.getScheduler().schedule(() -> checkForExpiredOrders(symbol), Main.getConfig().getInt("trading.max-open-order-time-seconds", 180) + 1, TimeUnit.SECONDS);
        return res;
    }

    /** @param price The limit/price to sell at */
    public NewOrderResponse limitSell(String symbol, double quantity, double price) {
        String quantityString = Utils.convertTradeAmount(quantity, price, symbol);
        String priceString = Utils.decToStr(price);

        NewOrderResponse res = CLIENT.newOrder(NewOrder.limitSell(symbol, TimeInForce.GTC, quantityString, priceString));

        Main.getScheduler().schedule(() -> checkForExpiredOrders(symbol), Main.getConfig().getInt("trading.max-open-order-time-seconds", 180) + 1, TimeUnit.SECONDS);
        return res;
    }

    private void checkForExpiredOrders(String symbol) {
        Main.getLogger().info("Checking for expired orders...");
        List<Order> openOrders = CLIENT.getOpenOrders(new OrderRequest(symbol));

        for (Order o : openOrders) {
            if ((System.currentTimeMillis() - o.getTime()) >= (Main.getConfig().getInt("trading.max-open-order-time-seconds", 180) * 1000L)) {
                Main.getLogger().warn("Cancelling order {} (Open for too long)", o.getOrderId());
                CLIENT.cancelOrder(new CancelOrderRequest(o.getSymbol(), o.getOrderId()));
            }
        }
    }
}
