package org.dominion.dynmap;

import org.dominion.Main;

import java.io.IOException;
import java.util.TimerTask;

public class DynmapDetection extends TimerTask {
    @Override
    public void run() {

        try{
            doRun();
        }catch (RuntimeException e){
            System.out.println("Runtime error" + e);
            e.printStackTrace();
        }
        catch (IOException e) {
            System.out.println("Unrecoverable error " + e);
            e.printStackTrace();
        }
        catch (Throwable e){
            System.out.println("Unrecoverable error " + e);
            e.printStackTrace();
            throw e;
        }
    }

    public void doRun() throws IOException {
        Main.getDiscordBot().sendDetectionUpdate();
        Main.getDynmapParser().checkLogOffs();
    }
}
