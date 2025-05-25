package data.scripts.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.EveryFrameScript;

import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;

import data.scripts.util.PresetUtils;

import java.util.*;

public class OfficerDismissalTracker implements EveryFrameScript {
    private final Set<String> knownOfficers = new HashSet<>();

    public OfficerDismissalTracker() {
        List<String> currentOfficers = new ArrayList<>();
        for (OfficerDataAPI officerData : Global.getSector().getPlayerFleet().getFleetData().getOfficersCopy()) {
            currentOfficers.add(officerData.getPerson().getId());
        };
        knownOfficers.addAll(currentOfficers);
    }

    @Override
    public void advance(float amount) {
        List<String> currentOfficers = new ArrayList<>();
        
        for (OfficerDataAPI officerData : Global.getSector().getPlayerFleet().getFleetData().getOfficersCopy()) {
            currentOfficers.add(officerData.getPerson().getId());
        };
        
        for (String officer : knownOfficers) {
            if (!currentOfficers.contains(officer)) {
                onOfficerDismissed(officer);
            }
        }

        knownOfficers.clear();
        knownOfficers.addAll(currentOfficers);
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
