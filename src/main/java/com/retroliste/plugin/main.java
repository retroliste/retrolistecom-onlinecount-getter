package com.retroliste.plugin;


import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.CommandHandler;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.plugin.EventHandler;
import com.eu.habbo.plugin.EventListener;
import com.eu.habbo.plugin.HabboPlugin;
import com.eu.habbo.plugin.events.emulator.EmulatorLoadedEvent;
import com.eu.habbo.plugin.events.users.*;

import java.io.BufferedReader;
import java.io.DataOutputStream;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.gson.*;
import com.retroliste.plugin.commands.SetApiKeyCommand;
import com.retroliste.plugin.commands.SetMaintenanceCommand;
import com.retroliste.plugin.converter.RoomJsonConverter;
import com.retroliste.plugin.converter.UserJsonConverter;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONObject;
import sun.security.provider.MD5;


public class main extends HabboPlugin implements EventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(Emulator.class);
    private final Gson gson = new GsonBuilder()
            .addSerializationExclusionStrategy(new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes f) {
                    // Ignoriere das Feld "written" in DataOutputStream
                    return f.getName().equals("written") && f.getDeclaringClass() == java.io.DataOutputStream.class;
                }

                @Override
                public boolean shouldSkipClass(Class<?> clazz) {
                    return false;
                }
            })
            .create();


    private static final Queue<JsonObject> eventQueue = new ConcurrentLinkedQueue<>();
    private static final int BATCH_SIZE = 50; // Anzahl der Events pro Batch
    private static final int BATCH_INTERVAL = 300000; // 5 Minuten in Millisekunden

    private ScheduledExecutorService scheduler;


    @Override
    public void onEnable() throws Exception {
        Emulator.getPluginManager().registerEvents(this, this);


        if (Emulator.isReady && !Emulator.isShuttingDown) {
            this.onEmulatorLoadedEvent(null);
        }
    }


    private boolean registerPermission(String name, String options, String defaultValue, boolean defaultReturn) {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("ALTER TABLE  `permissions` ADD  `" + name + "` ENUM(  " + options + " ) NOT NULL DEFAULT  '" + defaultValue + "'")) {
                statement.execute();
                return true;
            }
        } catch (SQLException ignored) {

        }

        return defaultReturn;
    }

    @EventHandler
    public void onEmulatorLoadedEvent(EmulatorLoadedEvent event) throws Exception {
        Emulator.getConfig().register("retroliste.apiKey", "null");
        Emulator.getConfig().register("retroliste.hotelId", "0");
        Emulator.getConfig().register("retroliste.apiEndpoint", "https://retroliste.com/v1/update/");

        boolean reloadPermissions = false;

        reloadPermissions = this.registerPermission("cmd_rl_maintenance", "'0', '1'", "0", reloadPermissions);
        reloadPermissions = this.registerPermission("cmd_rl_apikey", "'0', '1'", "1", reloadPermissions);


        if (reloadPermissions) {
            Emulator.getGameEnvironment().getPermissionsManager().reload();
        }


        CommandHandler.addCommand(new SetMaintenanceCommand("cmd_rl_maintenance", Emulator.getTexts().getValue("rl.maintenance", "rl_maintenance").split(";")));
        CommandHandler.addCommand(new SetApiKeyCommand("cmd_rl_apikey", Emulator.getTexts().getValue("rl.apikey", "rl_apikey").split(";")));


        int onlinecount = Emulator.getGameEnvironment().getHabboManager().getOnlineCount();
        int activeRooms = Emulator.getGameEnvironment().getRoomManager().getActiveRooms().size();
        int upTime = Emulator.getIntUnixTimestamp() - Emulator.getTimeStarted();
        String e = "{\"event\": \"onEmulatorLoadedEvent\", \"onlinecount\": " + onlinecount + "," +
                "\"activeRooms\": " + activeRooms + ", \"uptime\": " + upTime + "}";
        sendEventToRetroList(e);
        LOGGER.info("[RetroListe] LOADED!");

        // Emulator.getThreading().run(new OnlineCountUpdater(), 10000);

        // Initialize scheduler for batch processing
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime nextRun = now.withSecond(0).withNano(0);
        if (now.getMinute() % 5 != 0) {
            nextRun = nextRun.plusMinutes(5 - (now.getMinute() % 5));
        }
        long initialDelay = ChronoUnit.MILLIS.between(now, nextRun);
        scheduler.scheduleAtFixedRate(this::processAndPing, initialDelay, 5 * 60 * 1000, TimeUnit.MILLISECONDS);


    }

    @Override
    public void onDisable() throws Exception {
        // Process remaining events before shutdown
        JsonObject shutdownEvent = createBaseEventData("onDisable");
        sendEventToRetroList(gson.toJson(shutdownEvent));

        processAndPing();
        scheduler.shutdown();

        // Send final shutdown event
    }

    private JsonObject createBaseEventData(String eventName) {
        JsonObject eventData = new JsonObject();
        eventData.addProperty("event", eventName);
        eventData.addProperty("uptime", Emulator.getIntUnixTimestamp() - Emulator.getTimeStarted());
        eventData.addProperty("activeRooms", Emulator.getGameEnvironment().getRoomManager().getActiveRooms().size());
        eventData.addProperty("onlinecount", Emulator.getGameEnvironment().getHabboManager().getOnlineCount());


        if (Emulator.getGameEnvironment().getHabboManager().getOnlineCount() > 0) {
            JsonArray users = new JsonArray();
            for (Habbo habbo : Emulator.getGameEnvironment().getHabboManager().getOnlineHabbos().values()) {
                JsonObject userJson = UserJsonConverter.convertUserToJson(habbo);
                users.add(userJson);
            }

            if (users.size() > 0)
                eventData.add("onlineUsers", users);
        }

        if (!Emulator.getGameEnvironment().getRoomManager().getActiveRooms().isEmpty()) {
            JsonArray rooms = new JsonArray();

            for (Room room : Emulator.getGameEnvironment().getRoomManager().getActiveRooms()) {
                JsonObject userJson = RoomJsonConverter.convertRoomToJson(room);
                rooms.add(userJson);
            }

            if (rooms.size() > 0)
                eventData.add("loadedRooms", rooms);
        }


        return eventData;
    }

    private void processAndPing() {
        try {
            JsonArray batch = new JsonArray();
            JsonObject batchData = createBaseEventData("batchUpdate").deepCopy();

            // Collect events from queue up to BATCH_SIZE
            while (!eventQueue.isEmpty() && batch.size() < BATCH_SIZE) {
                batch.add(eventQueue.poll());
            }

            // Wenn keine Events vorhanden sind, erstelle ein onAutoPing Event
            if (batch.size() == 0) {
                JsonObject pingEvent = createBaseEventData("onAutoPing");
                batch.add(pingEvent);
            }

            // Füge die Batch-Informationen hinzu
            batchData.add("events", batch);
            batchData.addProperty("timestamp", System.currentTimeMillis());
            batchData.addProperty("batchSize", batch.size());


            // Sende den Batch
            sendEventToRetroList(gson.toJson(batchData));


        } catch (Exception e) {
            LOGGER.error("[RetroListe] Failed to send batch", e);

        }
    }


    public void sendEvent(String eventName, JsonObject eventData) {
        JsonObject event = new JsonObject();
        event.addProperty("eventName", eventName);
        event.add("eventData", eventData);
        event.addProperty("timestamp", System.currentTimeMillis());

        eventQueue.offer(event);

        // Trigger immediate send if queue gets too large
        if (eventQueue.size() >= BATCH_SIZE) {
            processAndPing();
        }
    }


    @Override
    public boolean hasPermission(Habbo habbo, String s) {
        return false;
    }


    @EventHandler
    public void onUserGoOnline(UserLoginEvent e) {
        JsonObject userJson = UserJsonConverter.convertUserDetailedToJson(e.habbo);
        try {
            userJson.addProperty("ip", MD5Generator.createMD5Hash(e.ip));
        } catch (Exception ignored) {

        }
        sendEvent(userJson);


        if (e.habbo.getHabboInfo().getRank().getId() > 4) {
            String key = Emulator.getConfig().getValue("retroliste.apiKey", "null");

            if (key.equals("null")) {
                e.habbo.alert("Please setup the RetroListe Plugin. Just use the command :rl_apikey YourApiKey to set the key.");

            }

        }

    }

    @EventHandler
    public void onUserGoOffline(UserDisconnectEvent e) {
        JsonObject event = UserJsonConverter.convertUserToJson(e.habbo);
        sendEvent(event);
    }


    @EventHandler
    public void onUserRegistered(UserRegisteredEvent e) {
        JsonObject event = UserJsonConverter.convertUserToJson(e.habbo);
        sendEvent(event);
    }

    public void sendEvent(JsonObject e) {

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        // Den Namen der Methode ermitteln, die sendEvent aufgerufen hat
        String callerMethodName = stackTrace[2].getMethodName(); // stackTrace[2] ist der Aufrufer von sendEvent

        sendEvent(callerMethodName, e);

    }


    public static void sendEventToRetroList(String e) {

        LOGGER.debug(e);
        String key = Emulator.getConfig().getValue("retroliste.apiKey", "null");
        String hotelId = Emulator.getConfig().getValue("retroliste.hotelId", "0");
        String apiEndpoint = Emulator.getConfig().getValue("retroliste.apiEndpoint", "https://retroliste.com/v1/update/");

        if (key.equals("null") || hotelId.equals("0"))
            return;

        Thread newThread = new Thread(() -> {
            try {
                String answer = executePost(apiEndpoint + hotelId, e, key);
                LOGGER.info(answer);
            } catch (Exception x) {
                LOGGER.error(x.getMessage());
            }
        });
        newThread.start();
    }


    public static boolean setMaintenanceMode(boolean status) {
        String key = Emulator.getConfig().getValue("retroliste.apiKey", "null");
        String hotelId = Emulator.getConfig().getValue("retroliste.hotelId", "0");
        String apiEndpoint = Emulator.getConfig().getValue("retroliste.apiEndpoint", "https://retroliste.com/v1/update/");

        if (key.equals("null") || hotelId.equals("0"))
            return false;


        try {
            executePost(apiEndpoint + hotelId, "{\"maintenance\": \"" + status + "\"}", key);
            return true;
        } catch (Exception x) {
            LOGGER.error(x.getMessage());
        }
        return false;
    }

    public static boolean checkApiKey(String key, String hotelId, Habbo habbo) {


        String apiEndpoint = Emulator.getConfig().getValue("retroliste.apiEndpoint", "https://retroliste.com/v1/update/");

        if (key.equals("null") || hotelId.equals("0"))
            return false;


        try {
            executePost(apiEndpoint + hotelId, "{\"event\": \"onCheckApiKey\"}", key);
            return true;
        } catch (Exception x) {
            LOGGER.error(x.getMessage());
            habbo.alert("The API Key is invalid!\r\n" + x.getMessage());
        }

        return false;
    }

    public static String executePost(String targetURL, String urlParameters, String key) {
        HttpURLConnection connection = null;

        try {
            //Create connection
            URL url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type",
                    "application/json");
            connection.setRequestProperty(HttpHeaders.ACCEPT, "application/json");
            connection.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer " + key);
            connection.setRequestProperty("Content-Length",
                    Integer.toString(urlParameters.getBytes().length));

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            //Send request
            DataOutputStream wr = new DataOutputStream(
                    connection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.close();

            //Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return response.toString();
        } catch (Exception e) {
            LOGGER.debug(e.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

}