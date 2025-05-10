package data.scripts.util;

import java.util.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;

import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.characters.PersonAPI;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;

import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import com.fs.starfarer.api.util.Misc;

import org.apache.log4j.Logger;

public class PresetUtils {
    private static final Logger logger = Logger.getLogger(PresetUtils.class);

    public static final String MEMORY_KEY = "$player_fleet_presets";

    public static class FleetPreset {
        public List<String> shipIds = new ArrayList<>();
        public Map<String, List<ShipVariantAPI>> variantsMap = new HashMap<>();
        public Map<String, List<List<Object>>> officersMap = new HashMap<>();
    }

    public static Map<String, FleetPreset> getFleetPresets() {
        return (Map<String, FleetPreset>) Global.getSector().getPersistentData().get(MEMORY_KEY);
    }

    @SuppressWarnings("unchecked")
    public static void saveFleetPreset(String name) {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        FleetPreset preset = new FleetPreset();
    
        for (FleetMemberAPI member : fleet.getFleetData().getMembersInPriorityOrder()) {
            String hullId = member.getHullId();
            ShipVariantAPI variant = member.getVariant();
            PersonAPI captain = member.getCaptain();

            preset.shipIds.add(hullId);

            if (captain != null) {
                List<Object> entry = new ArrayList<>();
                entry.add(captain);
                entry.add(variant);

                if (preset.officersMap.containsKey(hullId)) {
                    preset.officersMap.get(hullId).add(entry);
                } else {
                    List<List<Object>> pairs = new ArrayList<>();
                    pairs.add(entry);
                    preset.officersMap.put(hullId, pairs);
                }
            }
        }
        Map<String, FleetPreset> presets = (Map<String, FleetPreset>) Global.getSector().getPersistentData().get(MEMORY_KEY);
        presets.put(name, preset);
    }
    
    public static List<FleetMemberAPI> getFleetMembersOfPreset(String name) {
        Map<String, FleetPreset> presets = (Map<String, FleetPreset>) Global.getSector().getPersistentData().get(MEMORY_KEY);
        FleetPreset preset = presets.get(name);
        if (preset == null) return null;

        List<FleetMemberAPI> members = new ArrayList<>();
        List<String> doneHullIds = new ArrayList<>();

        for (String hullId : preset.shipIds) {
            if (doneHullIds.contains(hullId)) continue;

            for (ShipVariantAPI variant : preset.variantsMap.get(hullId)) {
                members.add(Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant));
            }

            doneHullIds.add(hullId);
        }

        return members;
    }

    @SuppressWarnings("unchecked")
    public static void restoreFleetFromPreset(String name) {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        MarketAPI market = playerFleet.getCommander().getMarket();
        if (market == null) return;

        Map<String, FleetPreset> presets = (Map<String, FleetPreset>) Global.getSector().getPersistentData().get(MEMORY_KEY);
        FleetPreset preset = presets.get(name);
        if (preset == null) return;
        
        SubmarketAPI storage = playerFleet.getCommander().getMarket().getSubmarket("storage");

        for (FleetMemberAPI member : playerFleet.getFleetData().getMembersInPriorityOrder()) {
            if (member.getCaptain() != null) {
                member.setCaptain(null);
            }
            storage.getCargo().getMothballedShips().addFleetMember(member);
        }
        playerFleet.getFleetData().clear();

        boolean allFound = true;
        List<String> doneHullIds = new ArrayList<>();
        for (String hullId : preset.shipIds) {
            List<ShipVariantAPI> variants = preset.variantsMap.get(hullId);

            for (ShipVariantAPI variant : variants) {
                
                // Check if the ship is in storage
                boolean found = false;
                for (FleetMemberAPI storedMember : storage.getCargo().getMothballedShips().getMembersInPriorityOrder()) {
                    if (storedMember.getHullId().equals(hullId) && storedMember.getVariant().equals(variant)) {
                        // Transfer from storage to fleet
                        storage.getCargo().getMothballedShips().removeFleetMember(storedMember);
                        if (!doneHullIds.contains(hullId)) {
                            playerFleet.getFleetData().addFleetMember(storedMember);
                        }
                        found = true;

                        if (preset.officersMap.containsKey(hullId)) {
                            List<List<Object>> pairs = preset.officersMap.get(hullId);
                            for (List<Object> pair : pairs) {
                                PersonAPI captain = (PersonAPI) pair.get(0);
                                ShipVariantAPI variant_ = (ShipVariantAPI) pair.get(1);

                                if (variant_.equals(storedMember.getVariant())) {
                                    storedMember.setCaptain(captain);
                                    break;
                                }
                            }
                        }
                    }
                }
                doneHullIds.add(hullId);

                if (!found) {
                    allFound = false;
                    Global.getSector().getCampaignUI().addMessage(
                        "Could not find " + hullId + " in storage to load for preset: " + name, 
                        Misc.getNegativeHighlightColor()
                    );
                }
            }
        }

        if (allFound) {
            Global.getSector().getCampaignUI().addMessage(
                "Successfully restored fleet preset: " + name,
                Misc.getPositiveHighlightColor()
            );
        }
    }

    @SuppressWarnings("unchecked")
    public static void deleteFleetPreset(String name) {
        Map<String, FleetPreset> presets = (Map<String, FleetPreset>) Global.getSector().getPersistentData().get(MEMORY_KEY);
        presets.remove(name);
    }

    public static void removeOfficerFromPresets(String officerId) {
        Map<String, FleetPreset> presets = (Map<String, FleetPreset>) Global.getSector().getPersistentData().get(MEMORY_KEY);

        for (Map.Entry<String, FleetPreset> entry : presets.entrySet()) {
            String fleetPresetName = entry.getKey();
            FleetPreset fleetPreset = entry.getValue();
        
            List<String> doneHullIds = new ArrayList<>();
            List<String> hullIds = fleetPreset.shipIds;

            for (String hullId : hullIds) {
                if (doneHullIds.contains(hullId)) continue;
        
                List<List<Object>> captainsAndVariants = fleetPreset.officersMap.get(hullId);
                if (captainsAndVariants == null) continue;

                int numCaptainsAndVariants = captainsAndVariants.size();
                for (int i = 0; i < numCaptainsAndVariants; i++) {
                    List<Object> captainAndVariant = captainsAndVariants.get(i);
                    PersonAPI captain = (PersonAPI) captainAndVariant.get(0);
                    if (captain.getId().equals(officerId)) {
                        captainsAndVariants.remove(i);
                        
                        if (numCaptainsAndVariants == 1) {
                            fleetPreset.officersMap.remove(hullId);
                        }
                        return;
                    }
                }
                doneHullIds.add(hullId);
            }
        }
    }
}