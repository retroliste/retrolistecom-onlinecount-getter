package com.retroliste.plugin.converter;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.achievements.Achievement;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.plugin.events.users.UserEvent;
import com.google.gson.JsonObject;
import com.retroliste.plugin.MD5Generator;

public class UserJsonConverter {

    public static JsonObject convertUserDetailedToJson(Habbo user) {
        if (user == null || user.getHabboInfo() == null) {
            throw new IllegalArgumentException("User or user info cannot be null");
        }

        JsonObject userJson = new JsonObject();

        try {

            Achievement onlineTime = Emulator.getGameEnvironment().getAchievementManager().getAchievement("AllTimeHotelPresence");

            // Basic user information
            userJson.addProperty("userId", user.getHabboInfo().getId());
            userJson.addProperty("userName", user.getHabboInfo().getUsername());
            userJson.addProperty("firstVisit", user.getHabboInfo().firstVisit);
            userJson.addProperty("machineId", user.getHabboInfo().getMachineID());
            userJson.addProperty("accountCreated", user.getHabboInfo().getAccountCreated());
            userJson.addProperty("lastLogin", user.getHabboInfo().getLastOnline());
            userJson.addProperty("loginStreak", user.getHabboStats().loginStreak);
            userJson.addProperty("onlineTime", user.getHabboStats().getAchievementProgress(onlineTime));


            // Additional optional information - add null checks as needed
            if (user.getHabboInfo().getCurrentRoom() != null) {
                userJson.addProperty("currentRoom", user.getHabboInfo().getCurrentRoom().getId());
                if (user.getRoomUnit() != null) {
                    userJson.addProperty("isIdle", user.getRoomUnit().isIdle());
                    userJson.addProperty("isIdleTimer", user.getRoomUnit().getIdleTimer());
                }
            }

            if (user.getHabboInfo().getRank() != null) {
                userJson.addProperty("rank", user.getHabboInfo().getRank().getId());
            }

            // Add more properties as needed

        } catch (Exception e) {
            throw new RuntimeException("Error creating JSON object for user: " + e.getMessage());
        }

        return userJson;
    }

    public static JsonObject convertUserToJson(Habbo user) {
        if (user == null || user.getHabboInfo() == null) {
            throw new IllegalArgumentException("User or user info cannot be null");
        }

        JsonObject userJson = new JsonObject();

        try {
            // Basic user information
            userJson.addProperty("userId", user.getHabboInfo().getId());
            userJson.addProperty("userName", user.getHabboInfo().getUsername());
            userJson.addProperty("firstVisit", user.getHabboInfo().firstVisit);
            userJson.addProperty("ipLogin", MD5Generator.createMD5Hash(user.getHabboInfo().getIpLogin()));
            userJson.addProperty("ipRegister", MD5Generator.createMD5Hash(user.getHabboInfo().getIpRegister()));


            // Additional optional information - add null checks as needed
            if (user.getHabboInfo().getCurrentRoom() != null) {
                userJson.addProperty("currentRoom", user.getHabboInfo().getCurrentRoom().getId());
                if (user.getRoomUnit() != null) {
                    userJson.addProperty("isIdle", user.getRoomUnit().isIdle());
                    userJson.addProperty("isIdleTimer", user.getRoomUnit().getIdleTimer());
                }
            }

            if (user.getHabboInfo().getRank() != null) {
                userJson.addProperty("rank", user.getHabboInfo().getRank().getId());
            }
            // Add more properties as needed

        } catch (Exception e) {
            throw new RuntimeException("Error creating JSON object for user: " + e.getMessage());
        }

        return userJson;
    }


    public static JsonObject convertOfflineUserSimpleToJson(Integer userId) {

        JsonObject userJson = new JsonObject();

        HabboInfo user = Emulator.getGameEnvironment().getHabboManager().getHabboInfo(userId);
        try {
            // Basic user information
            userJson.addProperty("userId", user.getId());
            userJson.addProperty("userName", user.getUsername());
            userJson.addProperty("userName", user.getUsername());
            userJson.addProperty("machineId", user.getMachineID());
            userJson.addProperty("ipLogin", MD5Generator.createMD5Hash(user.getIpLogin()));
            userJson.addProperty("ipRegister", MD5Generator.createMD5Hash(user.getIpRegister()));

            userJson.addProperty("accountCreated", user.getAccountCreated());
            userJson.addProperty("lastLogin", user.getLastOnline());

            if (user.getRank() != null) {
                userJson.addProperty("rank", user.getRank().getId());
            }
            // Add more properties as needed

        } catch (Exception e) {
            throw new RuntimeException("Error creating JSON object for user: " + e.getMessage());
        }

        return userJson;


    }

    public static JsonObject convertUserSimpleToJson(Habbo user) {
        if (user == null || user.getHabboInfo() == null) {
            throw new IllegalArgumentException("User or user info cannot be null");
        }

        JsonObject userJson = new JsonObject();

        try {
            // Basic user information
            userJson.addProperty("userId", user.getHabboInfo().getId());
            userJson.addProperty("userName", user.getHabboInfo().getUsername());
            if (user.getHabboInfo().getRank() != null) {
                userJson.addProperty("rank", user.getHabboInfo().getRank().getId());
            }
            // Add more properties as needed

        } catch (Exception e) {
            throw new RuntimeException("Error creating JSON object for user: " + e.getMessage());
        }

        return userJson;
    }

    // Usage example
    public void handleEvent(UserEvent e) {
        JsonObject userJson = convertUserToJson(e.habbo);
        // Use the JSON object as needed
    }
}