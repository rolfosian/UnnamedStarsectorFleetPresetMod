package data.scripts.listeners;

import com.fs.starfarer.api.EveryFrameScript;

import data.scripts.util.PresetMiscUtils;
import data.scripts.util.PresetUtils;

public class FleetMonitor implements EveryFrameScript {
    @Override
    public void advance(float arg0) {
        PresetUtils.checkFleetAgainstPreset();
    }
    @Override public boolean isDone() {return false;}
    @Override public boolean runWhilePaused() {return true;}
}
