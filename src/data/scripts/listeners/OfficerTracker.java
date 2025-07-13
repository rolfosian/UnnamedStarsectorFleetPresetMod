package data.scripts.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.util.IntervalUtil;

import data.scripts.util.PresetUtils;
import data.scripts.util.PresetUtils.FleetPreset;
import data.scripts.util.PresetUtils.OfficerVariantPair;
import data.scripts.util.PresetMiscUtils;

import java.util.*;

public class OfficerTracker implements EveryFrameScript {
    private class Officers extends HashMap<String, PersonAPI> {
        public Officers(List<OfficerDataAPI> officerDataList) {
            super();
            for (OfficerDataAPI officerData : officerDataList) {
                this.put(officerData.getPerson().getId(), officerData.getPerson());
            }
        }
    }

    private final Officers knownOfficers;
    private IntervalUtil interval = new IntervalUtil(0.2f, 0.3f);

    public OfficerTracker() {
        this.knownOfficers = new Officers(Global.getSector().getPlayerFleet().getFleetData().getOfficersCopy());
    }

    @Override
    public void advance(float amount) {
        interval.advance(amount);
        if (interval.intervalElapsed()) {
            Officers currentOfficers = new Officers(Global.getSector().getPlayerFleet().getFleetData().getOfficersCopy());
            
            for (String officerId : knownOfficers.keySet()) {
                if (!currentOfficers.containsKey(officerId)) {
                    onOfficerDismissed(officerId);
                }
            }

            knownOfficers.clear();
            knownOfficers.putAll(currentOfficers);
        }
    }

    private void onOfficerDismissed(String officerId) {
        PresetUtils.removeOfficerFromPresets(officerId);
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }
}
