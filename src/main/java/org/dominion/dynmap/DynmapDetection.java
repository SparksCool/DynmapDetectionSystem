package org.dominion.dynmap;

import org.dominion.Main;

import java.util.TimerTask;

public class DynmapDetection extends TimerTask {
    @Override
    public void run() {

        try{
            doRun();
        }catch (RuntimeException e){
            System.out.println("Runtime error" + e);
        }catch (Throwable e){
            System.out.println("Unrecoverable error" + e);
            throw e;
        }
    }

    public void doRun() {
        Main.checkAndCreateJsonConfig("./config.json", "{}");
        Main.getDiscordBot().sendDetectionUpdate();
    }
}
