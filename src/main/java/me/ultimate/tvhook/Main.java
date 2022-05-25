package me.ultimate.tvhook;

import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.account.NewOrderResponse;
import me.ultimate.tvhook.utils.Configuration;
import me.ultimate.tvhook.utils.Utils;
import me.ultimate.tvhook.utils.WebhookSignal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class Main {
    public static final boolean DEVELOPMENT_MODE = "dev".equals(System.getProperty("env"));
    private static final Logger LOGGER = LogManager.getLogger("Main");
    public static final String DATA_FOLDER = System.getProperty("user.dir") + (DEVELOPMENT_MODE ? "\\src\\main\\resources\\" : "\\");
    private static final Configuration CONFIG = new Configuration("config.yml");
    private static final LinkedBlockingQueue<Runnable> QUEUE = new LinkedBlockingQueue<>();
    private static final ScheduledExecutorService SCHEDULER = new ScheduledThreadPoolExecutor(1, r -> new Thread(r, "Scheduler"));
    private static BinanceClient API;


    public static void main(String[] args) {
        LOGGER.info("Starting server...");

        WebhookServer server = new WebhookServer();
        server.start();

        // Login to Binance
        String loginFile = (DEVELOPMENT_MODE ? "dev-" : "") + "login.yml";
        API = new BinanceClient(new Configuration(loginFile));

        while (true) {
            try {
                QUEUE.take().run();
            } catch (Exception e) {
                LOGGER.error("Error while executing task", e);
            }
        }
    }

    public static void onSignal(WebhookSignal signal) {
        double balance = API.getBalance(signal.getAction() == OrderSide.BUY ? signal.getFiat() : signal.getCrypto());

        if (balance <= 0.0D) {
            LOGGER.warn("Not enough {} balance to place order", signal.getFiat());
            return;
        }

        double quantity = balance * (CONFIG.getDouble("trading."
                + signal.getAction().toString().toLowerCase() + ".percent-of-balance",
                signal.getAction() == OrderSide.BUY ? 20.0 : 100.0D) / 100.0D);
        quantity /= signal.getPrice();

        NewOrderResponse res = (signal.getAction() == OrderSide.BUY) ?
                API.limitBuy(signal.getCurrency(), quantity, signal.getPrice())
                : API.limitSell(signal.getCurrency(), quantity, signal.getPrice());

        LOGGER.info("Placed {} order for {} {} at {} {}, order status is {}", signal.getAction(), Utils.decToStr(quantity), signal.getCrypto(), signal.getPrice(), signal.getFiat(), res.getStatus());
        LOGGER.info("New Balance: {} {}", API.getBalance(signal.getFiat()), signal.getFiat());
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
