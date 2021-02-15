package com.lauriethefish.betterportals.bukkit;


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.lauriethefish.betterportals.bukkit.config.MessageConfig;
import com.lauriethefish.betterportals.bukkit.util.VersionUtil;
import com.lauriethefish.betterportals.shared.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Handles nagging the user to update with a log message (you can turn it off).
 */
public class UpdateManager {
    private static final int RESOURCE_ID = 75409;
    private static final String LATEST_UPDATE_ENDPOINT = "https://api.spiget.org/v2/resources/%s/versions/latest";
    private static final String UPDATE_DOWNLOAD_URL_FORMAT = "https://www.spigotmc.org/resources/betterportals.75409/download?version=%d";

    private static final String USER_AGENT = "BetterPortals-Update";

    private static final int READ_TIMEOUT = 5000;
    private static final int WRITE_TIMEOUT = 5000;

    private final JavaPlugin pl;
    private final Logger logger;
    private final MessageConfig messageConfig;

    private String latestVersionStr;
    private int latestVersionId;

    @Inject
    public UpdateManager(JavaPlugin pl, Logger logger, MessageConfig messageConfig) {
        this.pl = pl;
        this.logger = logger;
        this.messageConfig = messageConfig;
    }

    /**
     * Uses Spiget to find the latest update of the plugin.
     * Saves it to {@link UpdateManager#latestVersionStr} and {@link UpdateManager#latestVersionId}.
     * @throws IOException If the HTTPS request failed.
     */
    private void fetchLatestVersion() throws IOException {
        URL url;
        try {
            url = new URL(String.format(LATEST_UPDATE_ENDPOINT, RESOURCE_ID));
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setConnectTimeout(WRITE_TIMEOUT);
        connection.addRequestProperty("User-Agent", USER_AGENT);

        JsonObject obj = new JsonParser().parse(new InputStreamReader(connection.getInputStream())).getAsJsonObject();
        latestVersionStr = obj.get("name").getAsString();
        latestVersionId = obj.get("id").getAsInt();
    }

    /**
     * Starts a new thread to check for plugin updates.
     * Prints a log message if an update is found, and a warning if there was an error while fetching the update.
     */
    public void checkForUpdates() {
        new Thread(() -> {
            try {
                checkForUpdatesInternal();
            } catch (Throwable ex) {
                logger.warning("An error occurred while checking for updates (%s)", ex.getMessage());
            }
        }).start();
    }

    private void checkForUpdatesInternal() throws IOException   {
        fetchLatestVersion();

        String currentVersion = pl.getDescription().getVersion();

        if (!VersionUtil.isVersionGreaterOrEq(currentVersion, latestVersionStr)) {
            String downloadUrl = String.format(UPDATE_DOWNLOAD_URL_FORMAT, latestVersionId);

            String msg = messageConfig.getRawMessage("outOfDate");
            msg = msg.replace("{url}", downloadUrl);
            msg = msg.replace("{current}", currentVersion);
            msg = msg.replace("{new}", latestVersionStr);

            logger.info(msg);
        }
    }
}
