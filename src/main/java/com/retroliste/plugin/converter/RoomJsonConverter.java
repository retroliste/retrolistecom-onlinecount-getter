package com.retroliste.plugin.converter;

import com.eu.habbo.habbohotel.rooms.Room;
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


        } catch (Exception e) {
            throw new RuntimeException("Error creating JSON object for room: " + e.getMessage());
        }

        return roomJson;
    }

}