package data.scripts.listeners;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.IntervalUtil;

import data.scripts.util.PresetMiscUtils;
import data.scripts.util.PresetUtils;

public class FleetMonitor implements EveryFrameScript {
    IntervalUtil interval = new IntervalUtil(0.2f, 0.3f);

    @Override
    public void advance(float amount) {
        interval.advance(amount);
        if (interval.intervalElapsed()) {
            PresetUtils.checkFleetAgainstPreset();
        }
    }
    @Override public boolean isDone() { return false; }
    @Override public boolean runWhilePaused() { return false; }
}