package me.ultimate.tvhook;

import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.event.OrderTradeUpdateEvent;
import com.binance.api.client.domain.event.UserDataUpdateEvent;
import me.ultimate.tvhook.utils.Configuration;
import me.ultimate.tvhook.utils.DiscordAlerts;
import me.ultimate.tvhook.utils.Utils;
import me.ultimate.tvhook.utils.WebhookSignal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main {
    public static final boolean DEVELOPMENT_MODE = "dev".equals(System.getProperty("env"));
    private static final Logger LOGGER = LogManager.getLogger("Main");
    public static final String DATA_FOLDER = System.getProperty("user.dir") + (DEVELOPMENT_MODE ? "\\src\\main\\resources\\" : "\\");
    private static final Configuration CONFIG = new Configuration("config.yml");
    private static final LinkedBlockingQueue<Runnable> QUEUE = new LinkedBlockingQueue<>();
    private static final ScheduledExecutorService SCHEDULER = new ScheduledThreadPoolExecutor(1, r -> new Thread(r, "Scheduler"));
    private static BinanceClient API;


    public static void main(String[] args) {
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
            if (event.getEventType() != UserDataUpdateEvent.UserDataUpdateEventType.ORDER_TRADE_UPDATE) return;
            OrderTradeUpdateEvent e = event.getOrderTradeUpdateEvent();

            double fiat = Double.parseDouble(e.getOriginalQuantity()) * Double.parseDouble(e.getPrice());
            String order = String.format("[%s] %s %s (%s %s)", e.getSide(), e.getOriginalQuantity(), e.getSymbol(), fiat, CONFIG.getString("trading.fiat"));

            if (e.getOrderStatus() == OrderStatus.FILLED) {
                LOGGER.info("Order filled! " + order);
            } else if (e.getOrderStatus() == OrderStatus.EXPIRED) {
                LOGGER.warn("Order expired! " + order);
            } else if (e.getOrderStatus() == OrderStatus.REJECTED) {
                LOGGER.error("Order rejected! {} Reason: {}", order, e.getOrderRejectReason());
            }
        });

        SCHEDULER.scheduleAtFixedRate(() -> API.getClient().keepAliveUserDataStream(listenKey), 0, 59, TimeUnit.MINUTES);
    }

    public static void onSignal(WebhookSignal signal) {
        boolean isBuy = signal.getAction() == OrderSide.BUY;
        double balance = API.getBalance(isBuy ? signal.getFiat() : signal.getCrypto());

        if (balance <= 0.0D) {
            LOGGER.warn("Not enough {} balance to place order", signal.getFiat());
            return;
        }

        double quantity = balance * (CONFIG.getDouble("trading."
                + signal.getAction().toString().toLowerCase() + ".percent-of-balance",
                isBuy ? 20.0 : 100.0D) / 100.0D);
        if (isBuy) quantity /= signal.getPrice();

        NewOrderResponse res = isBuy ? API.limitBuy(signal.getCurrency(), quantity, signal.getPrice())
            : API.limitSell(signal.getCurrency(), quantity, signal.getPrice());

        if (res == null) return;
        String order = String.format("Placed %s order for %s %s at %s %s, order status is %s", signal.getAction(), Utils.round(quantity, 6), signal.getCrypto(), signal.getPrice(), signal.getFiat(), res.getStatus());
        String newBal = String.format("New Balance: %s %s", API.getBalance(signal.getFiat()), signal.getFiat());

        LOGGER.info(order);
        LOGGER.info(newBal);
        DiscordAlerts.sendEmbed("Trade Placed", order + "\n`" + newBal + "`", isBuy ? Color.GREEN.getRGB() : Color.RED.getRGB(), CONFIG.getBoolean("trading.alerts.discord.mention-everyone", true));
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
