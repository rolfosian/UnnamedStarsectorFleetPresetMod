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
    private final Map<String, PersonAPI> knownOfficers = new HashMap<>();
    IntervalUtil interval = new IntervalUtil(0.2f, 0.3f);

    public OfficerTracker() {
        for (OfficerDataAPI officerData : Global.getSector().getPlayerFleet().getFleetData().getOfficersCopy()) {
            knownOfficers.put(officerData.getPerson().getId(), officerData.getPerson());
        };
    }

    @Override
    public void advance(float amount) {
        interval.advance(amount);
        if (interval.intervalElapsed()) {
            Map<String, PersonAPI> currentOfficers = new HashMap<>();
        
            for (OfficerDataAPI officerData : Global.getSector().getPlayerFleet().getFleetData().getOfficersCopy()) {
                currentOfficers.put(officerData.getPerson().getId(), officerData.getPerson());
            };
            currentOfficers.put(Global.getSector().getPlayerPerson().getId(), Global.getSector().getPlayerPerson());
            
            for (String officerId : knownOfficers.keySet()) {
                if (!currentOfficers.containsKey(officerId)) {
                    onOfficerDismissed(officerId);
                }
            }
    
            Map<String, FleetPreset> presets = PresetUtils.getFleetPresets();
            for (FleetPreset preset : presets.values()) {
                for (int i = 0; i < preset.fleetMembers.size(); i++) {
                    OfficerVariantPair pair = preset.officersMap.get(i);
    
                    if (pair != null && !PresetUtils.isOfficerNought(pair.officer)) {
                        int currentOfficerLevel = currentOfficers.get(pair.officer.getId()).getStats().getLevel();
    
                        preset.fleetMembers.get(i).captain.getStats().setLevel(currentOfficerLevel);
                        preset.campaignFleet.getFleetData().getMembersInPriorityOrder().get(i).getCaptain().getStats().setLevel(currentOfficerLevel);
                    }  
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
