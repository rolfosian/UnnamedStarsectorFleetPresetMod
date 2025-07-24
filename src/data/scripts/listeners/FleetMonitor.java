package data.scripts.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.util.IntervalUtil;

import data.scripts.util.PresetMiscUtils;
import data.scripts.util.PresetUtils;
import data.scripts.util.PresetUtils.RunningMembers;

import java.util.*;

public class FleetMonitor implements EveryFrameScript {
    public void print(Object... args) {
        PresetMiscUtils.print(args);
    }
    
    IntervalUtil checkFleetInterval = new IntervalUtil(0.2f, 0.3f);
    IntervalUtil checkPerishedMembersInterval = new IntervalUtil(599f, 600f);

    RunningMembers runningMembers;
    public FleetMonitor() {
        this.runningMembers = new RunningMembers(Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy());
    }

    @Override
    public void advance(float amount) {
        if (Global.getSector().getMemoryWithoutUpdate().get(PresetUtils.PLAYERCURRENTMARKET_KEY) != null) return;
        checkFleetInterval.advance(amount);
        checkPerishedMembersInterval.advance(amount);

        if (checkFleetInterval.intervalElapsed()) {
        runningMembers = PresetUtils.checkFleetAgainstPreset(runningMembers);

            if (checkPerishedMembersInterval.intervalElapsed()) {
                // if garbage needs collecting
                PresetUtils.cleanUpPerishedPresetMembers();
            }
        }
    }

    @Override public boolean isDone() { return false; }
    @Override public boolean runWhilePaused() { return false; }
}