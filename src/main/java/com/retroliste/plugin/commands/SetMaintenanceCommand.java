package com.retroliste.plugin.commands;


import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.Command;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.*;

import com.eu.habbo.habbohotel.users.Habbo;

import com.eu.habbo.plugin.EventListener;


public class SetMaintenanceCommand extends Command implements EventListener {


    public SetMaintenanceCommand(String permission, String[] keys) {
        super(permission, keys);
    }


    @Override
    public boolean handle(GameClient gameClient, String[] strings) throws Exception {



        // API KEY setzen, dann POST Request an die API, dann in DB speichern wenn alles OK ist



        return true;
        //main.getUserSocket(gameClient).writeTextMessage("Test von " + gameClient.getHabbo().getHabboInfo().getUsername());
    }

}
