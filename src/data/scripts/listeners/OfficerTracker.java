package data.scripts.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.util.IntervalUtil;

import data.scripts.util.PresetUtils;

import java.util.*;

public class OfficerTracker implements EveryFrameScript {
    private class Officers extends HashSet<String> {
        public Officers(List<OfficerDataAPI> officerDataList) {
            super();
            for (OfficerDataAPI officerData : officerDataList) {
                this.add(officerData.getPerson().getId());
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
            
            for (String officerId : knownOfficers) {
                if (!currentOfficers.contains(officerId)) {
                    onOfficerDismissed(officerId);
                }
            }

            knownOfficers.clear();
            knownOfficers.addAll(currentOfficers);
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
