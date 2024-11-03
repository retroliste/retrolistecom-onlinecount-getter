package com.retroliste.plugin.converter;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.plugin.events.rooms.RoomEvent;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class RoomJsonConverter {
    public static JsonObject convertRoomToJson(Room room) {
        if (room == null) {
            throw new IllegalArgumentException("Room cannot be null");
        }

        JsonObject roomJson = new JsonObject();

        try {
            // Basic room information
            roomJson.addProperty("roomId", room.getId());
            roomJson.addProperty("roomName", room.getName());
            roomJson.addProperty("description", room.getDescription());
            roomJson.addProperty("ownerId", room.getOwnerId());
            roomJson.addProperty("ownerName", room.getOwnerName());

            // Room state
            roomJson.addProperty("usersMax", room.getUsersMax());
            roomJson.addProperty("usersNow", room.getUserCount());
            roomJson.addProperty("state", room.getState().toString());

            // Room settings
            roomJson.addProperty("access", room.getState().getState());
            roomJson.addProperty("score", room.getScore());

            // Room categories
            roomJson.addProperty("category", room.getCategory());
            roomJson.addProperty("tags", String.join(",", room.getTags()));

            // Add users list if needed
            JsonArray users = new JsonArray();
            for (Habbo habbo : room.getHabbos()) {
                users.add(UserJsonConverter.convertUserSimpleToJson(habbo));
            }
            roomJson.add("currentUsers", users);

        } catch (Exception e) {
            throw new RuntimeException("Error creating JSON object for room: " + e.getMessage());
        }

        return roomJson;
    }

    // Usage example
    public void handleRoomEvent(RoomEvent e) {
        JsonObject roomJson = convertRoomToJson(e.room);
        // Use the JSON object as needed
    }
}