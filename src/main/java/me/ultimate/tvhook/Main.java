package me.ultimate.tvhook;

import me.ultimate.tvhook.utils.Configuration;
import me.ultimate.tvhook.utils.WebhookSignal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.LinkedBlockingQueue;

public class Main {
    public static final boolean DEVELOPMENT_MODE = "dev".equals(System.getProperty("env"));
    private static final Logger LOGGER = LogManager.getLogger("Main");
    public static final String DATA_FOLDER = System.getProperty("user.dir") + (DEVELOPMENT_MODE ? "\\src\\main\\resources\\" : "\\");
    private static final Configuration CONFIG = new Configuration("config.yml");
    private static final LinkedBlockingQueue<Runnable> QUEUE = new LinkedBlockingQueue<>();


    public static void main(String[] args) {
        LOGGER.info("Starting server...");

        WebhookServer server = new WebhookServer();
        server.start();

        while (true) {
            try {
                QUEUE.take().run();
            } catch (Exception e) {
                LOGGER.error("Error while executing task", e);
            }
        }
    }

    public static void onSignal(WebhookSignal signal) {
        // TODO: Implement
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
}
