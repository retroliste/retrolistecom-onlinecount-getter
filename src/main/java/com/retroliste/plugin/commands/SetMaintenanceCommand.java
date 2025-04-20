package com.retroliste.plugin.commands;


import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.Command;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.plugin.EventListener;
import com.retroliste.plugin.main;


public class SetMaintenanceCommand extends Command implements EventListener {


    public SetMaintenanceCommand(String permission, String[] keys) {
        super(permission, keys);
    }


    @Override
    public boolean handle(GameClient gameClient, String[] strings) throws Exception {

        String currKey = Emulator.getConfig().getValue("retroliste.apiKey", "null");


        if (currKey.equals("null")) {
            gameClient.getHabbo().whisper("Please set the api key with :rl_apikey %apiKey%!", RoomChatMessageBubbles.ALERT);
            return true;
        }


        boolean newMode = Boolean.parseBoolean(strings[2]);

        boolean status = main.setMaintenanceMode(newMode);
        if (!status) {
            gameClient.getHabbo().whisper("An error occurred. Please visit the logs!", RoomChatMessageBubbles.ALERT);
            return true;
        }

        if (newMode)
            gameClient.getHabbo().whisper("Maintenance mode enabled!", RoomChatMessageBubbles.ALERT);
        else
            gameClient.getHabbo().whisper("Maintenance mode disabled!", RoomChatMessageBubbles.ALERT);


        return true;
        //main.getUserSocket(gameClient).writeTextMessage("Test von " + gameClient.getHabbo().getHabboInfo().getUsername());
    }

}
