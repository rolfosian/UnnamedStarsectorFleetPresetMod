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

// import org.lazywizard.lazylib.JSONUtils;

// import org.apache.log4j.Logger;

public class PresetUtils {
    // public static final Logger logger = Global.getlogger();

    public static final String MEMORY_KEY = "$player_fleet_presets";
    
    public static final List<HullSize> SIZE_ORDER = Arrays.asList(HullSize.CAPITAL_SHIP, HullSize.CRUISER, HullSize.DEFAULT, HullSize.DESTROYER, HullSize.FIGHTER, HullSize.FRIGATE);
    // TODO SORT FUNCTION FOR LARGEST TO SMALLEST WHILE SHUNTING CIVILIAN HULLS TO THE BOTTOM
    // - will use ShipVariantAPI.getHullSize()
    // getHullMods() quick and dirty to check if civilian, although there are some 'civilian' hulls with the hullmod idk?

    public static class FleetPreset {
        public List<String> shipIds = new ArrayList<>();
        public Map<String, List<ShipVariantAPI>> variantsMap = new HashMap<>();

        // maps to list of officer and variant pairs, with index 0 being the officer
        public Map<String, List<OfficerVariantPair>> officersMap = new HashMap<>();

        public static FleetPreset serializeFleetPreset(String deserializedPreset) {
            // TODO implement this for use in import function
            // FleetPreset presetSerialized = this;

            // return presetSerialized;
            return new FleetPreset();
        }

        public static void exportPresetasJSON() {
            // TODO implement this and corresponding import function

            // PROBLEM: How to handle officers
        }

        public static String deserializePreset() {
            // TODO implement this for use in export function
            return "";
        }

        // public Map<String, List<ShipVariantAPI>> getVariantsMapString(String name) {
        //     return this.variantsMap.get(name);
        // }
        // public List<String> getShipIds() {
        //     return this.shipIds;
        // }
        // public Map<String, List<List<Object>>> getOfficersMap() {
        //     return this.officersMap;
        // }
    }

    public static class OfficerVariantPair {
        public final PersonAPI officer;
        public final ShipVariantAPI variant;
    
        public OfficerVariantPair(PersonAPI officer, ShipVariantAPI variant) {
            this.officer = officer;
            this.variant = variant;
        }
    }

    public static void importPreset(String presetName) {
        // TODO implement

        // PROBLEM: How to handle officers from different save and apply new ones
    }

    public static boolean isPlayerFleetAPreset() {
        // TODO implement
        return false;
    }

    // should only be called if preset has no officers
    public static void assignofficersToPreset(FleetPreset preset, List<FleetMemberAPI> playerFleetMembers) {
        for (String hullId: preset.shipIds) {
            for (FleetMemberAPI fleetMember : playerFleetMembers) {
                if (hullId.equals(fleetMember.getHullId())) {
                    PersonAPI captain = fleetMember.getCaptain();

                    if (captain != null) {
                        OfficerVariantPair pair = new OfficerVariantPair(captain, fleetMember.getVariant());
                        
                        if (preset.officersMap.get(hullId) == null) {
                            preset.officersMap.put(hullId, new ArrayList<>());
                        }
                        preset.officersMap.get(hullId).add(pair);
                    }
                }
            }
        }
    }

    public static void stripofficersFromPlayerFleet(List<FleetMemberAPI> fleetMembers) {
        for (FleetMemberAPI fleetMember : fleetMembers) {
            if (!fleetMember.getCaptain().isPlayer()) fleetMember.setCaptain(null);
        }
    }

    public static boolean isOfficerInFleet(String officerId, List<FleetMemberAPI> fleetMembers) {
        for (FleetMemberAPI fleetMember : fleetMembers) {
            if (fleetMember.getCaptain() != null && fleetMember.getCaptain().getId() == officerId) return true;
        }
        return false;
    }

    public static boolean isOfficerInPreset(String officerId, FleetPreset preset) {
        for (String hullId : preset.shipIds) {
            List<OfficerVariantPair> pairs = preset.officersMap.get(hullId);
            if (pairs != null) {
                for (OfficerVariantPair pair : pairs) {
                    PersonAPI officer = pair.officer;
                    if (officer != null && officer.getId() == officerId) return true;
                }
            }
        }
        return false;
    }

    public static boolean isVariantInOfficerPairs(ShipVariantAPI variant, List<OfficerVariantPair> pairs) {
        for (OfficerVariantPair pair : pairs) {
            if (areSameVariant((pair.variant), variant)) return true;
        }
        return false;
    }

    // requires current fleet to be a preset and preset to be imported/saved
    // officers need to be assigned by player to fleet members before this function is called
    public static void applyOfficerChangesToPreset(String presetName, boolean isNewlyImportedPreset) {
        FleetPreset preset = (FleetPreset) getFleetPresets().get(presetName);
        List<FleetMemberAPI> playerFleetMembers = Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder();

        if (isNewlyImportedPreset) {
            // fresh preset with no officers
            assignofficersToPreset(preset, playerFleetMembers);
        } else {            
            for (FleetMemberAPI fleetMember : playerFleetMembers) {
                for (Map.Entry<String, List<OfficerVariantPair>> entry : preset.officersMap.entrySet()) {
                    String hullId = entry.getKey();
                    if (fleetMember.getHullId() == hullId) {

                        List<OfficerVariantPair> pairs = entry.getValue();

                        for (int i = 0; i < pairs.size(); i++) {
                            OfficerVariantPair pair = pairs.get(i);

                            if (areSameVariant(pair.variant, fleetMember.getVariant())) {
                                if (!isOfficerInFleet(pair.officer.getId(), playerFleetMembers)) {
                                    pairs.remove(i);
                                    pairs.set(i, new OfficerVariantPair(fleetMember.getCaptain(), pair.variant));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        //     for (String hullId: preset.shipIds) {
        //         List<List<Object>> newOfficerVariants = newOfficerMap.get(hullId)
        //         if (newOfficerMap.get(hullId) == null) continue;

        //         for (ShipVariantAPI variant : preset.variantsMap.get(hullId)) {
        //             if (preset.officersMap.containsKey(hullId)) {
        //                 List<List<Object>> pairs = preset.officersMap.get(hullId);

        //                 for (OfficerVariantPair pair : pairs) {
        //                     PersonAPI captain = (PersonAPI) pair.get(0);
        //                     ShipVariantAPI variant_ = (ShipVariantAPI) pair.get(1);

        //                     if (areSameVariant(variant_, variant) {
        //                         storedMember.setCaptain(captain);
        //                         break;
        //                     }
        //                 }
        //         }
        //     }
        // }

            // WHAT WAS I THINKING???????????
            // for (String hullId: preset.shipIds) {
            //     if (newOfficerPairs.get(hullId) == null) continue;

            //     for (FleetMemberAPI fleetMember : playerFleetMembers) {
            //         if (hullId.equals(fleetMember.getHullId())) {
            //             ShipVariantAPI variant = (ShipVariantAPI) newOfficerPairs.get(hullId).get(1);
            //             if (!areSameVariant(fleetMember.getVariant(), variant)) continue;

            //             PersonAPI newOfficer = (PersonAPI) newOfficerPairs.get(hullId).get(0);

            //             List<List<Object>> currentPresetPairs = preset.officersMap.get(hullId);
            //             if (currentPresetPairs != null) {
            //                 int index = 0;
            //                 if (newOfficer != null) {
            //                     for (OfficerVariantPair pair : currentPresetPairs) {
            //                         PersonAPI potentiallyReplacedO = (PersonAPI) pair.get(0);
            //                         ShipVariantAPI potentiallyReplacedV = (ShipVariantAPI) pair.get(1);
    
            //                         if (areSameVariant(variant, potentiallyReplacedV)) {
            //                             if (isOfficerInFleet(potentiallyReplacedO.getId(), playerFleetMembers) 
            //                             && !isOfficerInPreset(potentiallyReplacedO.getId(), preset)) {
            //                                 currentPresetPairs.remove(index);
            //                                 currentPresetPairs.add(newOfficerPairs.get(hullId));
            //                                 break;
            //                             }
    
            //                         }
            //                         index ++;
            //                     }
            //                 } else {
            //                     for (OfficerVariantPair pair : currentPresetPairs) {
            //                         PersonAPI potentiallyRemovedO = (PersonAPI) pair.get(0);
    
            //                         if (!isOfficerInPreset(potentiallyRemovedO.getId(), preset)) {
            //                             currentPresetPairs.remove(index);
            //                             fleetMember.setCaptain(null);
            //                             break;
            //                         }
            //                         index ++;
            //                     }
            //                 }
            //             } else {
            //                 preset.officersMap.put(hullId, new ArrayList<>());
            //                 preset.officersMap.get(hullId).add(newOfficerPairs.get(hullId));
            //             }
            //         }
            //     }
            // }
        // }
    // }

    public static MarketAPI getPlayerCurrentMarket() {
        return (MarketAPI) Global.getSector().getMemoryWithoutUpdate().get("$playerCurrentMarket");
    }

    @SuppressWarnings("unchecked")
    public static Map<String, FleetPreset> getFleetPresets() {
        return (Map<String, FleetPreset>) Global.getSector().getPersistentData().get(MEMORY_KEY);
    }

    public static void storeFleetInStorage(String name) {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        MarketAPI market = getPlayerCurrentMarket();
        if (market == null) return;
        
        SubmarketAPI storage = market.getSubmarket("storage");
        // ReflectionUtilis.logMethods(storage);
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
                OfficerVariantPair pair = new OfficerVariantPair(captain, variant);

                if (preset.officersMap.containsKey(hullId)) {
                    preset.officersMap.get(hullId).add(pair);
                } else {
                    List<OfficerVariantPair> pairs = new ArrayList<>();
                    pairs.add(pair);
                    preset.officersMap.put(hullId, pairs);
                }
            }
        }
        getFleetPresets().put(name, preset);
    }
    
    // unused - cant remember why i wrote this maybe it will be useful at some point
    public static List<FleetMemberAPI> getFleetMembersOfPreset(String name) {
        FleetPreset preset = getFleetPresets().get(name);
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

    // variant1.equals(variant2) doesnt always work, idk why and i dont really care
    public static boolean areSameVariant(ShipVariantAPI variant1, ShipVariantAPI variant2) {
        return (variant1.getFullDesignationWithHullNameForShip().equals(variant2.getFullDesignationWithHullNameForShip())
            && variant1.getSMods().equals(variant2.getSMods())
            && variant1.getHullMods().equals(variant2.getHullMods())
            && variant1.getWings().equals(variant2.getWings())
            && variant1.getFittedWeaponSlots().equals(variant2.getFittedWeaponSlots())
            && variant1.getSModdedBuiltIns().equals(variant2.getSModdedBuiltIns())
            && variant1.getWeaponGroups().equals(variant2.getWeaponGroups())
            && variant1.getNumFluxCapacitors() == variant2.getNumFluxCapacitors()
            && variant1.getNumFluxVents() == variant2.getNumFluxVents());
    }

    public static void restoreFleetFromPreset(String name) {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        MarketAPI market = getPlayerCurrentMarket();
        if (market == null) return;

        Map<String, FleetPreset> presets = getFleetPresets();
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
                    if (count == variantsToGet) {
                        found = true;
                        break;
                    }

                        if (preset.officersMap.containsKey(hullId)) {
                            List<OfficerVariantPair> pairs = preset.officersMap.get(hullId);

                            for (OfficerVariantPair pair : pairs) {
                                if (areSameVariant(pair.variant, storedMember.getVariant())) {
                                    storedMember.setCaptain(pair.officer);
                                    break;
                                }
                            }

                        if (storedMember.getHullId().equals(hullId)) {
                            for (FactionAPI faction : factions) {
                                storageCargo.initMothballedShips(faction.getId());
                            }
                        storageCargo.getMothballedShips().removeFleetMember(storedMember);
                        playerFleet.getFleetData().addFleetMember(storedMember);
                        found = true;
                        count ++;

                        }
                    }
                }
                doneHullIds.add(hullId);

                if (!found) {
                    allFound = false;
                    Global.getSector().getCampaignUI().addMessage(
                        "Could not find one or more of " + hullId + " in storage to load for preset: " + name, 
                        Misc.getNegativeHighlightColor()
                    );
                }
            }
        }
        // WE HAVE NOTHING TO AUTOMATICALLY UPDATE THE FLEET PANEL MEMBER SPRITES FOR THE NEW FLEET HULLS AAAAAAAAAAAAAAAAAAA
        // TODO:
        // HOW DO YOU ADVANCE THE FLEET PANEL UI CHILDREN????
        // SWITCHING TABS MANUALLY WILL UPDATE IT FOR US - FIND MECHANISM OF ACTION??????
        // playerFleetData.syncIfNeeded(); - DOES NOTHING
        // UIPanelAPI fleetInfoPanel = (UIPanelAPI) Global.getSector().getMemoryWithoutUpdate().get("$fleetInfoPanel");
        // fleetInfoPanel.advance(0.0166667f); - DOES NOTHING

        // ReflectionUtilis.logMethods(fleetInfoPanel);  // - GET CHILDREN????

        if (allFound) {
            Global.getSector().getCampaignUI().addMessage(
                "Successfully restored fleet preset: " + name,
                Misc.getPositiveHighlightColor()
            );
        }
    }

    public static void deleteFleetPreset(String name) {
        getFleetPresets().remove(name);
    }

    public static void removeOfficerFromPresets(String officerId) {
        Map<String, FleetPreset> presets = getFleetPresets();

        for (FleetPreset fleetPreset : presets.values()) {

            List<String> doneHullIds = new ArrayList<>();
            for (String hullId : fleetPreset.shipIds) {
                if (doneHullIds.contains(hullId)) continue;
        
                List<OfficerVariantPair> captainsAndVariants = fleetPreset.officersMap.get(hullId);
                if (captainsAndVariants == null) continue;

                int numCaptainsAndVariants = captainsAndVariants.size();
                for (int i = 0; i < numCaptainsAndVariants; i++) {
                    OfficerVariantPair captainAndVariant = captainsAndVariants.get(i);
                    if (captainAndVariant.officer.getId().equals(officerId)) {
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

        return sortedMap;
    }
}