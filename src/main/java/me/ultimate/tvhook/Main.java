package me.ultimate.tvhook;

import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.event.OrderTradeUpdateEvent;
import com.binance.api.client.domain.event.UserDataUpdateEvent;
import me.ultimate.tvhook.utils.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main {
    public static final String VERSION = "0.0.3";
    public static final boolean DEVELOPMENT_MODE = "dev".equals(System.getProperty("env"));
    private static final Logger LOGGER = LogManager.getLogger("Main");
    public static final String DATA_FOLDER = System.getProperty("user.dir") + (DEVELOPMENT_MODE ? "\\src\\main\\resources\\" : "\\");
    private static final Configuration CONFIG = new Configuration("config.yml");
    private static final LinkedBlockingQueue<Runnable> QUEUE = new LinkedBlockingQueue<>();
    private static final ScheduledExecutorService SCHEDULER = new ScheduledThreadPoolExecutor(1, r -> new Thread(r, "Scheduler"));
    private static BinanceClient API;
    private static double prevBalance = -1;


    public static void main(String[] args) {
        LOGGER.info("TradingViewHook v" + VERSION);
        if (!CONFIG.exists()) {
            Utils.extractConfigFiles();
            LOGGER.info("Extracted config files, please edit them and restart the program.");
            return;
        }

        LOGGER.info("Starting server...");
        WebhookServer server = new WebhookServer();
        server.start();

        // Login to Binance
        String loginFile = (DEVELOPMENT_MODE ? "../../../dev-" : "") + "login.yml";
        API = new BinanceClient(new Configuration(loginFile));

        LOGGER.info("Started with balance of {} {}", Utils.round(API.getBalance(CONFIG.getString("trading.fiat")), 2), CONFIG.getString("trading.fiat"));

        startListenStream();

        while (true) {
            try {
                QUEUE.take().run();
            } catch (Exception e) {
                LOGGER.error("Error while executing task", e);
            }
        }
    }

    private static void startListenStream() {
        final String listenKey = API.getClient().startUserDataStream();
        API.getWebsocket().onUserDataUpdateEvent(listenKey, (UserDataUpdateEvent event) -> {
            if (Thread.currentThread().getName().contains("OkHttp")) Thread.currentThread().setName("UserDataUpdateEvent");
            if (event.getEventType() != UserDataUpdateEvent.UserDataUpdateEventType.ORDER_TRADE_UPDATE) return;
            OrderTradeUpdateEvent e = event.getOrderTradeUpdateEvent();

            double fiat = Double.parseDouble(e.getOriginalQuantity()) * Double.parseDouble(e.getPrice());
            PlaceholderMap map = PlaceholderMap.builder()
                    .add("side", e.getSide().toString())
                    .add("isbuy", e.getSide() == OrderSide.BUY ? "true" : "false")
                    .add("quantity", e.getOriginalQuantity())
                    .add("limit", e.getPrice())
                    .add("symbol", e.getSymbol())
                    .add("bal_fiat", Utils.round(fiat, 2))
                    .add("fiat", CONFIG.getString("trading.fiat"));
            if (e.getSide() == OrderSide.SELL) addProfitMapping(map);

            String order = map.apply("[{side}] {quantity} {symbol} ({bal_fiat} {fiat})");
            if (e.getOrderStatus() == OrderStatus.FILLED) {
                LOGGER.info("Order filled! " + order);
                DiscordAlerts.sendAlert(OrderStatus.FILLED, map);

                if (e.getSide() == OrderSide.SELL && prevBalance > 0)
                    LOGGER.info(map.apply("Profit: {profit} {fiat} **({profit_percent})**"));
            } else if (e.getOrderStatus() == OrderStatus.EXPIRED) {
                LOGGER.warn("Order expired! " + order);
                DiscordAlerts.sendAlert(OrderStatus.EXPIRED, map);
            } else if (e.getOrderStatus() == OrderStatus.REJECTED) {
                map.add("reason", e.getOrderRejectReason().toString());
                LOGGER.error("Order rejected! {} Reason: {}", order, e.getOrderRejectReason());
                DiscordAlerts.sendAlert(OrderStatus.REJECTED, map);
            }
        });

        SCHEDULER.scheduleAtFixedRate(() -> API.getClient().keepAliveUserDataStream(listenKey), 0, 59, TimeUnit.MINUTES);
    }

    public static void onSignal(Signal signal) {
        boolean isBuy = signal.getAction() == OrderSide.BUY;
        double balance = API.getBalance(isBuy ? signal.getFiat() : signal.getCrypto());

        if (balance <= 0.0D) {
            LOGGER.warn("Not enough {} balance to place order", signal.getFiat());
            return;
        }

        double quantity = balance * (CONFIG.getDouble("trading."
                + signal.getAction().toString().toLowerCase() + ".percent-of-balance",
                isBuy ? 20.0D : 100.0D) / 100.0D);
        if (isBuy)
            quantity /= signal.isMarketOrder() ? Double.parseDouble(API.getClient().getPrice(signal.getCurrency()).getPrice()) : signal.getPrice();

        NewOrderResponse res = API.placeOrder(signal.getAction(), signal.getCurrency(), quantity, signal.getPrice(), signal.isMarketOrder());
        if (res == null) return;

        double newBal = API.getBalance(signal.getFiat());

        LOGGER.info("Placed {}-{} order for {} {} at {} {}, order status is {}", signal.isMarketOrder() ? "MARKET" : "LIMIT", signal.getAction(), Utils.round(quantity, 6), signal.getCrypto(), signal.getPrice(), signal.getFiat(), res.getStatus());
        LOGGER.info("New Balance: {} {}", newBal, signal.getFiat());

        PlaceholderMap map = PlaceholderMap.builder()
                .add("type", signal.isMarketOrder() ? "MARKET" : "LIMIT")
                .add("side", signal.getAction().toString())
                .add("isbuy", isBuy ? "true" : "false")
                .add("quantity", Utils.round(quantity, 6))
                .add("crypto", signal.getCrypto())
                .add("price", Utils.round(newBal - prevBalance, 2))
                .add("fiat", signal.getFiat())
                .add("bal_fiat", Utils.round(newBal, 2))
                .add("limit", Utils.decToStr(signal.getPrice()));

        if (signal.getAction() == OrderSide.SELL) addProfitMapping(map);

        DiscordAlerts.sendAlert(res.getStatus(), map);
        if (isBuy) prevBalance = balance;
    }

    private static void addProfitMapping(PlaceholderMap map) {
        double newBalance = API.getBalance(CONFIG.getString("trading.fiat"));
        double profit = newBalance - (prevBalance < 0 ? newBalance : prevBalance);

        map.add("profit", Utils.round(profit, 2));
        map.add("profit_color", profit > 0 ? String.valueOf(Utils.GREEN) : String.valueOf(Utils.RED));
        map.add("profit_percent", (profit > 0 ? "+" : "-") + Utils.round(Math.abs(profit / prevBalance * 100.0D), 2) + "%");
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public static Configuration getConfig() {
        return CONFIG;
    }

    public static LinkedBlockingQueue<Runnable> getQueue() {
        return QUEUE;
    }

    public static ScheduledExecutorService getScheduler() {
        return SCHEDULER;
    }

    public static BinanceClient getAPI() {
        return API;
    }
}
