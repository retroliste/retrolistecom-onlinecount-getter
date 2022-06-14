package com.retroliste.plugin;


import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.plugin.EventHandler;
import com.eu.habbo.plugin.EventListener;
import com.eu.habbo.plugin.HabboPlugin;
import com.eu.habbo.plugin.events.emulator.EmulatorLoadedEvent;
import com.eu.habbo.plugin.events.users.UserDisconnectEvent;
import com.eu.habbo.plugin.events.users.UserLoginEvent;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.eu.habbo.threading.runnables.AchievementUpdater;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class main extends HabboPlugin implements EventListener {
    private final Logger LOGGER = LoggerFactory.getLogger(Emulator.class);


    @Override
    public void onEnable() throws Exception {
        Emulator.getPluginManager().registerEvents(this, this);


        if (Emulator.isReady && !Emulator.isShuttingDown) {
            this.onEmulatorLoadedEvent(null);


        }

    }


    @EventHandler
    public void onEmulatorLoadedEvent(EmulatorLoadedEvent event) throws Exception {
        Emulator.getConfig().register("retroliste.apiKey", "null");
        Emulator.getConfig().register("retroliste.hotelId", "0");
        Emulator.getConfig().register("retroliste.apiEndpoint", "https://retroliste.com/v1/update/");



        int onlinecount = Emulator.getGameEnvironment().getHabboManager().getOnlineCount();
        int activeRooms = Emulator.getGameEnvironment().getRoomManager().getActiveRooms().size();
        int upTime = Emulator.getIntUnixTimestamp() - Emulator.getTimeStarted();
        String e = "{\"event\": \"onEmulatorLoadedEvent\", \"onlinecount\": " + onlinecount + "," +
                "\"activeRooms\": " + activeRooms + ", \"uptime\": " + upTime + "}";
        sendEventToRetroList(e);
        LOGGER.info("[RetroListe] LOADED!");

        Emulator.getThreading().run(new OnlineCountUpdater(), 10000);


    }

    @Override
    public void onDisable() throws Exception {
        int onlinecount = Emulator.getGameEnvironment().getHabboManager().getOnlineCount();
        int activeRooms = Emulator.getGameEnvironment().getRoomManager().getActiveRooms().size();
        int upTime = Emulator.getIntUnixTimestamp() - Emulator.getTimeStarted();
        String event = "{\"event\": \"onDisable\", \"onlinecount\": " + onlinecount + "," +
                "\"activeRooms\": " + activeRooms + ", \"uptime\": " + upTime + "}";
        sendEventToRetroList(event);
        LOGGER.info("[GameCenter] Good Bye!");


    }

    @Override
    public boolean hasPermission(Habbo habbo, String s) {
        return false;
    }

    @EventHandler
    public void onUserGoOnline(UserLoginEvent e) {
        int onlinecount = Emulator.getGameEnvironment().getHabboManager().getOnlineCount();
        int activeRooms = Emulator.getGameEnvironment().getRoomManager().getActiveRooms().size();
        int upTime = Emulator.getIntUnixTimestamp() - Emulator.getTimeStarted();
        String event = "{\"event\": \"onUserGoOnline\", \"onlinecount\": " + (onlinecount) + "," +
                "\"activeRooms\": " + activeRooms + ", \"uptime\": " + upTime + "}";
        sendEventToRetroList(event);

    }

    @EventHandler
    public void onUserGoOffline(UserDisconnectEvent e) {

        int onlinecount = Emulator.getGameEnvironment().getHabboManager().getOnlineCount();
        int activeRooms = Emulator.getGameEnvironment().getRoomManager().getActiveRooms().size();
        int upTime = Emulator.getIntUnixTimestamp() - Emulator.getTimeStarted();
        String event = "{\"event\": \"onUserGoOffline\", \"onlinecount\": " + (onlinecount-1) + "," +
                "\"activeRooms\": " + activeRooms + ", \"uptime\": " + upTime + "}";

        sendEventToRetroList(event);
    }



    public static void sendEventToRetroList(String e) {

        String key = Emulator.getConfig().getValue("retroliste.apiKey", "null");
        String hotelId = Emulator.getConfig().getValue("retroliste.hotelId", "0");
        String apiEndpoint = Emulator.getConfig().getValue("retroliste.apiEndpoint", "https://retroliste.com/v1/update/");

        if(key.equals("null") || hotelId.equals("0"))
            return;

        Thread newThread = new Thread(() -> {

            HttpClient client = HttpClient.newHttpClient();

            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(apiEndpoint + hotelId))
                        .header("content-type", "application/json")
                        .setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + key)
                        .setHeader(HttpHeaders.ACCEPT, "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(e))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            } catch (Exception ignored) {

            }

        });

        newThread.start();


    }
}