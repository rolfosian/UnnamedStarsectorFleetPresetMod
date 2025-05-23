package data.scripts.listeners;

import java.util.*;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.PresetUtils;
import data.scripts.util.PresetUtils.FleetMemberWrapper;
import data.scripts.util.PresetUtils.FleetPreset;
import data.scripts.util.PresetMiscUtils;


public class FleetMonitor implements EveryFrameScript {
    private static void print(Object... args) {
        PresetMiscUtils.print(args);
    }

    @Override
    public void advance(float arg0) {
        checkFleetAgainstPreset();
    }

    public static boolean isPlayerFleetChanged(FleetPreset preset, List<FleetMemberAPI> playerFleetMembers) {
        if (playerFleetMembers.size() != preset.fleetMembers.size()) {
            return true;
        }

        for (PresetUtils.FleetMemberWrapper member : preset.fleetMembers) {
            FleetMemberAPI playerFleetMember = playerFleetMembers.get(member.index);

            if (!PresetUtils.areSameVariant(playerFleetMember.getVariant(), member.member.getVariant()) 
                || (playerFleetMember.getCaptain() != null && !(playerFleetMember.getCaptain().getId() == member.captainId))) {
                return true;
            }

        }
        return false;
    }

    public static void checkFleetAgainstPreset() {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        FleetPreset preset = (FleetPreset) mem.get(PresetUtils.UNDOCKED_PRESET_KEY);
        if (preset == null) return;

        boolean isAutoUpdate = (boolean)Global.getSector().getPersistentData().get(PresetUtils.IS_AUTO_UPDATE_KEY);

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        List<FleetMemberAPI> playerFleetMembers = playerFleet.getFleetData().getMembersListCopy();

        if (isAutoUpdate) {
            boolean updated = false;

            if (playerFleetMembers.size() != preset.fleetMembers.size()) {
                Map<String, List<FleetMemberAPI>> currentFleetByHull = new HashMap<>();
                Map<String, List<FleetMemberAPI>> presetFleetByHull = new HashMap<>();

                for (FleetMemberAPI member : playerFleetMembers) {
                    String hullId = member.getHullId();
                    if (!currentFleetByHull.containsKey(hullId)) {
                        currentFleetByHull.put(hullId, new ArrayList<>());
                    }
                    currentFleetByHull.get(hullId).add(member);
                }

                for (PresetUtils.FleetMemberWrapper wrapper : preset.fleetMembers) {
                    String hullId = wrapper.member.getHullId();
                    if (!presetFleetByHull.containsKey(hullId)) {
                        presetFleetByHull.put(hullId, new ArrayList<>());
                    }
                    presetFleetByHull.get(hullId).add(wrapper.member);
                }
                
                preset.fleetMembers.clear();
                preset.shipIds.clear();
                preset.variantsMap.clear();
                preset.officersMap.clear();
                
                for (int i = 0; i < playerFleetMembers.size(); i++) {
                    FleetMemberAPI member = playerFleetMembers.get(i);
                    String hullId = member.getHullId();
                    
                    preset.fleetMembers.add(new FleetMemberWrapper(member, member.getCaptain(), i));
                    
                    preset.shipIds.add(hullId);
                    preset.variantsMap.put(i, member.getVariant());
                    
                    if (member.getCaptain() != null) {
                        PresetUtils.OfficerVariantPair pair = new PresetUtils.OfficerVariantPair(member.getCaptain(), member.getVariant(), i);
                        if (!preset.officersMap.containsKey(hullId)) {
                            preset.officersMap.put(hullId, new ArrayList<>());
                        }
                        preset.officersMap.get(hullId).add(pair);
                    }
                }
                Global.getSector().getCampaignUI().addMessage("The fleet composition has changed and the fleet preset has been updated.", Misc.getBasePlayerColor());

            } else {
                for (PresetUtils.FleetMemberWrapper member : preset.fleetMembers) {
                    FleetMemberAPI playerFleetMember = playerFleetMembers.get(member.index);

                    if (!PresetUtils.areSameVariant(playerFleetMember.getVariant(), member.member.getVariant())) {
                        preset.updateVariant(member.index, playerFleetMember.getVariant());
                        updated = true; 
                    }
                }
            }

            if (updated) {
                Global.getSector().getCampaignUI().addMessage("The fleet composition has changed and the fleet preset has been updated.", Misc.getBasePlayerColor());
            }

        } else {
            if (isPlayerFleetChanged(preset, playerFleetMembers)) {
                Global.getSector().getCampaignUI().addMessage("The fleet composition has changed. Consider updating the fleet preset you're using to match the current fleet.", Misc.getNegativeHighlightColor());
                mem.unset(PresetUtils.UNDOCKED_PRESET_KEY);
            }
        }
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }
    
}
