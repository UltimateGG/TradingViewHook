package me.ultimate.tvhook;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.config.BinanceApiConfig;
import me.ultimate.tvhook.utils.Configuration;

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
}
