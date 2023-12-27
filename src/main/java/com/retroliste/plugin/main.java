package com.retroliste.plugin;


import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.CommandHandler;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.plugin.EventHandler;
import com.eu.habbo.plugin.EventListener;
import com.eu.habbo.plugin.HabboPlugin;
import com.eu.habbo.plugin.events.emulator.EmulatorLoadedEvent;
import com.eu.habbo.plugin.events.users.UserDisconnectEvent;
import com.eu.habbo.plugin.events.users.UserLoginEvent;

import java.io.BufferedReader;
import java.io.DataOutputStream;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

import java.net.URL;

import com.retroliste.plugin.commands.SetMaintenanceCommand;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class main extends HabboPlugin implements EventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(Emulator.class);


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

        CommandHandler.addCommand(new SetMaintenanceCommand("cmd_rl_maintenance", Emulator.getTexts().getValue("rl.maintenance", "maintenance").split(";")));
        CommandHandler.addCommand(new SetMaintenanceCommand("cmd_rl_apikey", Emulator.getTexts().getValue("rl.apikey", "apikey").split(";")));


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
        String event = "{\"event\": \"onUserGoOffline\", \"onlinecount\": " + (onlinecount - 1) + "," +
                "\"activeRooms\": " + activeRooms + ", \"uptime\": " + upTime + "}";

        sendEventToRetroList(event);
    }


    public static void sendEventToRetroList(String e) {

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


    public static boolean checkApiKey(String key, String hotelId, Habbo habbo) {


        String apiEndpoint = Emulator.getConfig().getValue("retroliste.apiEndpoint", "https://retroliste.com/v1/update/");

        if (key.equals("null") || hotelId.equals("0"))
            return false;


        try {
            executePost(apiEndpoint + hotelId, "{\"event\": \"onCheckApiKey\"}", key);
            return true;
        } catch (Exception x) {
            LOGGER.error(x.getMessage());
            habbo.alert("API Key ungültig!\r\n" + x.getMessage());
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
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

}