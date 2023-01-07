package com.retroliste.plugin;

import com.eu.habbo.Emulator;

public class OnlineCountUpdater implements Runnable {

    private static int LAST_RELOAD = Emulator.getIntUnixTimestamp();
    @Override
    public void run() {

        Emulator.getThreading().run(this, 10000);

        int time = Emulator.getIntUnixTimestamp();
        if (time - LAST_RELOAD > 300) {

            int onlinecount = Emulator.getGameEnvironment().getHabboManager().getOnlineCount();
            int activeRooms = Emulator.getGameEnvironment().getRoomManager().getActiveRooms().size();
            int upTime = Emulator.getIntUnixTimestamp() - Emulator.getTimeStarted();
            String e = "{\"event\": \"onAutoPing\", \"onlinecount\": " + onlinecount + "," +
                    "\"activeRooms\": " + activeRooms + ", \"uptime\": " + upTime + "}";
            main.sendEventToRetroList(e);
            LAST_RELOAD = time;

        }
    }
}
