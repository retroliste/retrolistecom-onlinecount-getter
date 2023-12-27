package com.retroliste.plugin.commands;


import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.Command;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.*;

import com.eu.habbo.habbohotel.users.Habbo;

import com.eu.habbo.plugin.EventListener;
import com.retroliste.plugin.main;


public class SetApiKeyCommand extends Command implements EventListener {

    public SetApiKeyCommand(String permission, String[] keys) {
        super(permission, keys);
    }


    @Override
    public boolean handle(GameClient gameClient, String[] strings) throws Exception {


        if (strings.length == 1) {
            return false;
        }

        String currKey = Emulator.getConfig().getValue("retroliste.apiKey", "null");

        if (!currKey.equals("null")) {
            gameClient.getHabbo().whisper("The key is already set!", RoomChatMessageBubbles.ALERT);
            return true;
        }

        // split strings[1] by comma
        String[] apikeyHotelid = strings[1].split("RETROLISTE");

        if (apikeyHotelid.length != 2) {
            gameClient.getHabbo().whisper("The key is invalid!", RoomChatMessageBubbles.ALERT);
            return true;
        }

        String apiKey = apikeyHotelid[0];
        String hotelId = apikeyHotelid[1];


        boolean check = main.checkApiKey(apiKey, hotelId, gameClient.getHabbo());
        if (check) {
            Emulator.getConfig().update("retroliste.apiKey", apiKey);
            Emulator.getConfig().update("retroliste.hotelId", hotelId);
            Emulator.getConfig().saveToDatabase();
            gameClient.getHabbo().whisper("API Key successfully set. Please disable the cmd_rl_apikey permission.", RoomChatMessageBubbles.ALERT);

        }

        return true;
        //main.getUserSocket(gameClient).writeTextMessage("Test von " + gameClient.getHabbo().getHabboInfo().getUsername());
    }

}
