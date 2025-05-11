package data.scripts.util;

import java.util.*;
import java.util.stream.Collectors;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;

import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.characters.PersonAPI;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;

import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import com.fs.starfarer.api.util.Misc;

import org.apache.log4j.Logger;

public class PresetUtils {
    public static final Logger logger = Logger.getLogger(PresetUtils.class);

    public static final String MEMORY_KEY = "$player_fleet_presets";
    
    public static final List<HullSize> SIZE_ORDER = Arrays.asList(HullSize.CAPITAL_SHIP, HullSize.CRUISER, HullSize.DEFAULT, HullSize.DESTROYER, HullSize.FIGHTER, HullSize.FRIGATE);
    // TODO SORT FUNCTION FOR LARGEST TO SMALLEST WHILE SHUNTING CIVILIAN HULLS TO THE BOTTOM
    // - will use ShipVariantAPI.getHullSize()
    // getHullMods() quick and dirty to check if civilian, although there are some 'civilian' hulls with the hullmod idk?

    public static class FleetPreset {
        public List<String> shipIds = new ArrayList<>();
        public Map<String, List<ShipVariantAPI>> variantsMap = new HashMap<>();
        public Map<String, List<List<Object>>> officersMap = new HashMap<>();

        // public Map<String, List<ShipVariantAPI>> getVariantsMap() {
        //     return this.variantsMap.get;
        // }
        // public List<String> getShipIds() {
        //     return this.shipIds;
        // }
        // public Map<String, List<List<Object>>> getOfficersMap() {
        //     return this.officersMap;
        // }
    }

    public static MarketAPI getPlayerCurrentMarket() {
        return (MarketAPI) Global.getSector().getMemoryWithoutUpdate().get("$playerCurrentMarket");
    }

    public static Map<String, FleetPreset> getFleetPresets() {
        return (Map<String, FleetPreset>) Global.getSector().getPersistentData().get(MEMORY_KEY);
    }

    public static void storeFleetInStorage(String name) {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        MarketAPI market = getPlayerCurrentMarket();
        if (market == null) return;
        
        SubmarketAPI storage = market.getSubmarket("storage");
        ReflectionUtilis.logMethods(storage);
        CargoAPI storageCargo = storage.getCargo();
        List<FactionAPI> factions = Global.getSector().getAllFactions();
        
        FleetDataAPI playerFleetData = playerFleet.getFleetData();

        for (FleetMemberAPI member : playerFleetData.getMembersInPriorityOrder()) {
            if (member.getCaptain() != null) {
                if (member.getCaptain().isPlayer()) continue;
                member.setCaptain(null);
            }
            playerFleetData.removeFleetMember(member);

            // i dont know what this does but according to the javadocs you need to call it before calling GetMothballedShips
            for (FactionAPI faction : factions) {
                storageCargo.initMothballedShips(faction.getId());
            }
            storage.getCargo().getMothballedShips().addFleetMember(member);
        }
    }

    public static boolean isMemberInFleet(FleetDataAPI fleetData, FleetMemberAPI memberToCheck) {
        for (FleetMemberAPI member : fleetData.getMembersInPriorityOrder()) {
            if (member.getHullId().equals(memberToCheck.getHullId()) &&
                member.getVariant().equals(memberToCheck.getVariant())) {
    
                PersonAPI captain = member.getCaptain();
                PersonAPI captainToCheck = memberToCheck.getCaptain();
    
                if (captain == null && captainToCheck == null) {
                    return true;
                }
    
                if (captain != null && captainToCheck != null &&
                    captain.getId().equals(captainToCheck.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public static void saveFleetPreset(String name) {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        FleetPreset preset = new FleetPreset();
    
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            String hullId = member.getHullId();
            ShipVariantAPI variant = member.getVariant();
            PersonAPI captain = member.getCaptain();

            preset.shipIds.add(hullId);
            
            if (!preset.variantsMap.containsKey(hullId)) {
                List<ShipVariantAPI> variants = new ArrayList<>();
                preset.variantsMap.put(hullId, variants);
            }
            preset.variantsMap.get(hullId).add(variant);

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
    
    // unused - cant remember why i wrote this maybe it will be useful at some point
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
        MarketAPI market = getPlayerCurrentMarket();
        if (market == null) return;

        Map<String, FleetPreset> presets = (Map<String, FleetPreset>) Global.getSector().getPersistentData().get(MEMORY_KEY);
        FleetPreset preset = presets.get(name);
        if (preset == null) return;
        
        SubmarketAPI storage = market.getSubmarket("storage");
        CargoAPI storageCargo = storage.getCargo();
        List<FactionAPI> factions = Global.getSector().getAllFactions();
        
        FleetDataAPI playerFleetData = playerFleet.getFleetData();

        for (FleetMemberAPI member : playerFleetData.getMembersInPriorityOrder()) {
            if (member.getCaptain() != null) {
                member.setCaptain(null);
            }
            playerFleetData.removeFleetMember(member);

            // i dont know what this does but according to the javadocs you need to call it before calling GetMothballedShips
            for (FactionAPI faction : factions) {
                storageCargo.initMothballedShips(faction.getId());
            }
            storage.getCargo().getMothballedShips().addFleetMember(member);
        }

        boolean allFound = true;
        List<String> doneHullIds = new ArrayList<>();
        for (String hullId : preset.shipIds) {
            if (doneHullIds.contains(hullId)) continue;

            List<ShipVariantAPI> variants = preset.variantsMap.get(hullId);
            
            int variantsToGet = Collections.frequency(preset.shipIds, hullId);
            for (ShipVariantAPI variant : variants) {

                // Check if the ship is in storage
                boolean found = false;

                // i dont know what this does but according to the javadocs you need to call it before calling GetMothballedShips
                for (FactionAPI faction : factions) {
                    storageCargo.initMothballedShips(faction.getId());
                }
                int count = 0;
                for (FleetMemberAPI storedMember : storage.getCargo().getMothballedShips().getMembersInPriorityOrder()) {
                    if (count == variantsToGet) break;
                    
                    if (storedMember.getHullId().equals(hullId)) {
                        for (FactionAPI faction : factions) {
                            storageCargo.initMothballedShips(faction.getId());
                        }

                        storageCargo.getMothballedShips().removeFleetMember(storedMember);
                        playerFleet.getFleetData().addFleetMember(storedMember);
                        found = true;
                        count ++;

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
        // THESE DONT WORK TO AUTOMATICALLY UPDATE THE FLEET PANEL FOR THE NEW FLEET AAAAAAAAAAAAAAAAAAA
        // HOW DO YOU ADVANCE THE FLEET PANEL UI
        // SWITCHING TABS MANUALLY WILL UPDATE IT FOR US
        // playerFleetData.syncIfNeeded();
        // UIPanelAPI fleetInfoPanel = (UIPanelAPI) Global.getSector().getMemoryWithoutUpdate().get("$fleetInfoPanel");
        // fleetInfoPanel.advance(0.0166667f);

        // ReflectionUtilis.logMethods(fleetInfoPanel);

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

    public static String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        char firstChar = input.charAt(0);
        if (Character.isLetter(firstChar)) {
            return Character.toUpperCase(firstChar) + input.substring(1);
        }
        return input;
    }

    public static Map<String, String> getFleetPresetsMapForTable() {
        Map<String, String> map = new HashMap<>();
    
        Map<String, FleetPreset> presets = getFleetPresets();
        for (Map.Entry<String, FleetPreset> entry : presets.entrySet()) {
            String fleetPresetName = entry.getKey();
            FleetPreset fleetPreset = entry.getValue();
    
            Map<String, Integer> shipCountMap = new LinkedHashMap<>();

            for (Map.Entry<String, List<ShipVariantAPI>> variantsEntry : fleetPreset.variantsMap.entrySet()) {
                List<ShipVariantAPI> variantList = variantsEntry.getValue();

                for (ShipVariantAPI variant : variantList) {
                    String name = variant.getHullSpec().getHullName();
                    shipCountMap.put(name, shipCountMap.getOrDefault(name, 0) + 1);
                }
            }
    
            String ships = shipCountMap.entrySet()
                .stream()
                .map(e -> e.getValue() > 1 ? e.getKey() + " x" + e.getValue() : e.getKey())
                .collect(Collectors.joining(", "));
    
            map.put(capitalizeFirstLetter(fleetPresetName), ships);
        }
        return sortByKeyAlphanumerically(map);
    }

    public static Map<String, String> sortByKeyAlphanumerically(Map<String, String> input) {
        List<String> keys = new ArrayList<>(input.keySet());

        keys.sort((s1, s2) -> {
            int i = 0, j = 0;
            while (i < s1.length() && j < s2.length()) {
                char c1 = s1.charAt(i);
                char c2 = s2.charAt(j);

                if (Character.isDigit(c1) && Character.isDigit(c2)) {
                    int start1 = i, start2 = j;
                    while (i < s1.length() && Character.isDigit(s1.charAt(i))) i++;
                    while (j < s2.length() && Character.isDigit(s2.charAt(j))) j++;
                    String num1 = s1.substring(start1, i);
                    String num2 = s2.substring(start2, j);
                    int cmp = Integer.compare(Integer.parseInt(num1), Integer.parseInt(num2));
                    if (cmp != 0) return cmp;
                } else {
                    int cmp = Character.compare(
                        Character.toLowerCase(c1),
                        Character.toLowerCase(c2)
                    );
                    if (cmp != 0) return cmp;
                    i++;
                    j++;
                }
            }
            return Integer.compare(s1.length(), s2.length());
        });

        LinkedHashMap<String, String> sortedMap = new LinkedHashMap<>();
        for (String key : keys) {
            sortedMap.put(key, input.get(key));
        }

        return sortedMap; // Returning LinkedHashMap, which preserves insertion order
    }
}