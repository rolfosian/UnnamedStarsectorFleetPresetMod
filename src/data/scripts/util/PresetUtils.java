package data.scripts.util;

import java.util.*;
import java.util.stream.Collectors;
import java.awt.Color;

import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.SubmarketPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.characters.PersonAPI;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;

import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.fleet.FleetData;

import org.apache.log4j.Logger;

public class PresetUtils {
    public static final Logger logger = Logger.getLogger(PresetUtils.class);

    public static final String PRESETS_MEMORY_KEY = "$player_fleet_presets";
    public static final String FLEETINFOPANEL_KEY = "$fleetInfoPanel";
    public static final String PLAYERCURRENTMARKET_KEY = "$playerCurrentMarket";
    public static final String COREUI_KEY = "$coreUI";
    public static final String ISPLAYERPAIDFORSTORAGE_KEY = "$isPlayerPaidForStorage";
    public static final String MESSAGEQUEUE_KEY = "$presetsMessageQueue";

    public static final String RESTOREMESSAGE_SUCCESS_PREFIX = "Successfully restored fleet preset: ";
    public static final String RESTOREMESSAGE_FAIL_PREFIX = "Could not find one or more of ";
    public static final String RESTOREMESSAGE_FAIL_SUFFIX = " in storage to load for preset: ";


    public static final HullSize[] SIZE_ORDER_DESCENDING = {
        HullSize.CAPITAL_SHIP,
        HullSize.CRUISER,
        HullSize.DEFAULT,
        HullSize.DESTROYER,
        HullSize.FIGHTER,
        HullSize.FRIGATE
    };
    public static final HullSize[] SIZE_ORDER_ASCENDING = {
        HullSize.FRIGATE,
        HullSize.FIGHTER,
        HullSize.DESTROYER,
        HullSize.DEFAULT,
        HullSize.CRUISER,
        HullSize.CAPITAL_SHIP
    };

    // TODO IMPLEMENT PLAYER CHOSEN CUSTOM ORDERING
    // NO CLUE HOW TO MAKE IT WORK, PERHAPS REFLECTION ON FLEETPANELUI TREE? MUCH INVESTIGATION REQUIRED. I DONT REALLY KNOW HOW REFLECTION WORKS HONESTLY.

    // sorts while shunting civilian members to the bottom
    public static void sortFleetMembers(List<FleetMemberAPI> fleetMembers, HullSize[] order) {
        fleetMembers.sort((a, b) -> {
            HullSize sizeA = a.getVariant().getHullSize();
            HullSize sizeB = b.getVariant().getHullSize();
    
            boolean aIsCivilian = a.getHullSpec().isCivilianNonCarrier();
            boolean bIsCivilian = b.getHullSpec().isCivilianNonCarrier();
    
            if (aIsCivilian && !bIsCivilian) return 1;
            if (!aIsCivilian && bIsCivilian) return -1;
    
            int indexA = Arrays.asList(order).indexOf(sizeA);
            int indexB = Arrays.asList(order).indexOf(sizeB);
            return Integer.compare(indexA, indexB);
        });
    }
    
    public static class OfficerVariantPair {
        public final PersonAPI officer;
        public final ShipVariantAPI variant;
    
        public OfficerVariantPair(PersonAPI officer, ShipVariantAPI variant) {
            this.officer = officer;
            this.variant = variant;
        }
    }

    public static class FleetPreset {
        public List<String> shipIds = new ArrayList<>();
        public Map<String, List<ShipVariantAPI>> variantsMap = new HashMap<>();

        // maps to list of officer and variant pairs, with index 0 being the officer
        public Map<String, List<OfficerVariantPair>> officersMap = new HashMap<>();


        // JEEPERS FEATURE CREEPERS???
        // public void exportPresetasJSON() {
        //     // TODO implement this and corresponding import function
                
        //     // PROBLEM: How to handle officers
        // }

        // public String deserializePreset() {
        //     
        //     return "";
        // }

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

    // JEEPERS FEATURE CREEPERS???
    // public static FleetPreset serializeFleetPreset(String deserializedPreset) {
    //     // TODO implement this for use in import function
    //     // FleetPreset presetSerialized = new FleetPreset();

    //     // return presetSerialized;
    // }

    // public static void importPreset(String presetName) {
    //     // TODO implement

    //     // PROBLEM: How to handle officers from different save and apply new ones
    // }

    public static void initMothballedShips(CargoAPI storageCargo) {
        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            // i dont know what this does but the javadocs say to do it before calling getmothballedships and i think it stopped some crashing
            storageCargo.initMothballedShips(faction.getId());
        }
    }

    public static boolean isPlayerFleetAPreset() {
        // TODO implement
        return false;
    }

    // public static boolean isPresetAvailableAtCurrentMarket(MarketAPI market, String presetName) {
    //     FleetPreset preset = getFleetPresets().get(presetName);
    //     return false;
    // }

    // should only be called if preset has no officers
    // needs testing
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
            PersonAPI captain = fleetMember.getCaptain();
            if (captain != null && !captain.isPlayer()) fleetMember.setCaptain(null);
        }
    }

    public static boolean isOfficerInFleet(String officerId, List<FleetMemberAPI> fleetMembers) {
        for (FleetMemberAPI fleetMember : fleetMembers) {
            PersonAPI captain = fleetMember.getCaptain();
            if (captain != null && captain.getId() == officerId) return true;
        }
        return false;
    }

    public static boolean isOfficerInPreset(String officerId, FleetPreset preset) {
        for (String hullId : preset.shipIds) {
            List<OfficerVariantPair> pairs = preset.officersMap.get(hullId);
            if (pairs != null) {
                for (OfficerVariantPair pair : pairs) {
                    PersonAPI officer = pair.officer;
                    if (officer.getId() == officerId) return true;
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

    public static boolean areOfficersInPlayerFleet(List<FleetMemberAPI> fleetMembers) {
        for (FleetMemberAPI member : fleetMembers) {
            if (member.getCaptain() != null && !member.getCaptain().isPlayer()) return true;
        }
        return false;
    }

    // requires current fleet to be a saved preset
    // officers need to be assigned by player to fleet members before this function is called
    // needs testing
    public static void applyOfficerChangesToPreset(String presetName) {
        FleetPreset preset = (FleetPreset) getFleetPresets().get(presetName);
        List<FleetMemberAPI> playerFleetMembers = Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder();

        if (areOfficersInPlayerFleet(playerFleetMembers) && preset.officersMap.isEmpty()) {
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

    public static MarketAPI getPlayerCurrentMarket() {
        return (MarketAPI) Global.getSector().getMemoryWithoutUpdate().get(PLAYERCURRENTMARKET_KEY);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, FleetPreset> getFleetPresets() {
        return (Map<String, FleetPreset>) Global.getSector().getPersistentData().get(PRESETS_MEMORY_KEY);
    }

    public static boolean isPlayerPaidForStorage(SubmarketPlugin storagePlugin) {
        // Object fleetInfoPanel = Global.getSector().getMemoryWithoutUpdate().get(FLEETINFOPANEL_KEY);
        // Object infoPanelParent = ReflectionUtilis.invokeMethod("getParent", fleetInfoPanel);
        // CoreUIAPI coreUI = (CoreUIAPI) ReflectionUtilis.invokeMethod("getCoreUI", infoPanelParent);
        return storagePlugin.getOnClickAction((CoreUIAPI)Global.getSector().getMemoryWithoutUpdate().get(COREUI_KEY)).equals(SubmarketPlugin.OnClickAction.OPEN_SUBMARKET);
    }


    public static void storeFleetInStorage(String name) {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        MarketAPI market = getPlayerCurrentMarket();
        if (market == null) return;

        SubmarketAPI storage = market.getSubmarket(Submarkets.SUBMARKET_STORAGE);
        SubmarketPlugin storagePlugin = storage.getPlugin();
        if (!isPlayerPaidForStorage(storagePlugin)) return;

        
        CargoAPI storageCargo = storage.getCargo();
        
        FleetDataAPI playerFleetData = playerFleet.getFleetData();

        for (FleetMemberAPI member : playerFleetData.getMembersInPriorityOrder()) {
            if (member.getCaptain() != null) {
                if (member.getCaptain().isPlayer()) continue;
                member.setCaptain(null);
            }
            playerFleetData.removeFleetMember(member);

            // i dont know what this does but according to the javadocs you need to call it before calling GetMothballedShips
            initMothballedShips(storageCargo);
            storage.getCargo().getMothballedShips().addFleetMember(member);
        }
        refreshFleetUI();
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
        List<FleetMemberAPI> fleetMembers = Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy();

        // sortFleetMembers(fleetMembers, SIZE_ORDER_DESCENDING);
        FleetPreset preset = new FleetPreset();
    
        for (FleetMemberAPI member : fleetMembers) {
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

    public static FleetMemberAPI getPlayerFleetMemberCopy(FleetDataAPI playerFleetData) {
        for (FleetMemberAPI member : playerFleetData.getMembersListCopy()) {
            if (member.getCaptain() != null && member.getCaptain().isPlayer()) return member;
        }
        return null;
    }

    // this is because variant1.equals(variant2) doesnt always work, idk why and i dont really care
    
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
        
        SubmarketAPI storage = market.getSubmarket(Submarkets.SUBMARKET_STORAGE);
        CargoAPI storageCargo = storage.getCargo();
        
        FleetDataAPI playerFleetData = playerFleet.getFleetData();
        FleetMemberAPI playerFleetMember = getPlayerFleetMemberCopy(playerFleetData);

        for (FleetMemberAPI member : playerFleetData.getMembersInPriorityOrder()) {
            if (member.getCaptain() != null) {
                member.setCaptain(null);
            }
            playerFleetData.removeFleetMember(member);

            // i dont know what this does but according to the javadocs you need to call it before calling GetMothballedShips
            initMothballedShips(storageCargo);
            storageCargo.getMothballedShips().addFleetMember(member);
        }

        List<CampaignUIMessage> messageQueue = (List<CampaignUIMessage>) Global.getSector().getMemoryWithoutUpdate().get(MESSAGEQUEUE_KEY);

        List<String> doneHullIds = new ArrayList<>();
        boolean allFound = true;
        for (String hullId : preset.shipIds) {
            if (doneHullIds.contains(hullId)) continue;

            List<ShipVariantAPI> variants = preset.variantsMap.get(hullId);
            
            int variantsToGet = Collections.frequency(preset.shipIds, hullId);
            int found = 0;
            for (ShipVariantAPI variant : variants) {                
                initMothballedShips(storageCargo);
                
                for (FleetMemberAPI storedMember : storageCargo.getMothballedShips().getMembersInPriorityOrder()) {
                    if (found == variantsToGet) break;

                    if (storedMember.getHullId().equals(hullId)) {
                        if (preset.officersMap.containsKey(hullId)) {
                            List<OfficerVariantPair> pairs = preset.officersMap.get(hullId);

                            boolean officerVariantFound = false;
                            for (OfficerVariantPair pair : pairs) {
                                if (areSameVariant(pair.variant, storedMember.getVariant())) {
                                    storedMember.setCaptain(pair.officer);

                                    initMothballedShips(storageCargo);
                                    storageCargo.getMothballedShips().removeFleetMember(storedMember);
                                    playerFleet.getFleetData().addFleetMember(storedMember);

                                    officerVariantFound = true;
                                    found++;
                                    break;
                                }
                                if (officerVariantFound) break;
                            }
                            if (officerVariantFound) continue;

                            initMothballedShips(storageCargo);
                            storageCargo.getMothballedShips().removeFleetMember(storedMember);
                            playerFleet.getFleetData().addFleetMember(storedMember);
                            found++;
                        }
                    }
                }
                if (found < variantsToGet) {
                    allFound = false;
                    CampaignUIMessage msg = new CampaignUIMessage(RESTOREMESSAGE_FAIL_PREFIX + variant.getFullDesignationWithHullName() + RESTOREMESSAGE_FAIL_SUFFIX + name, Misc.getNegativeHighlightColor());

                    if (!messageQueue.contains(msg)){
                        messageQueue.add(msg);
                    }
                }
            }
            doneHullIds.add(hullId);
        }

        if (playerFleetData.getMembersInPriorityOrder().size() < 1) {
            initMothballedShips(storageCargo);
            for (FleetMemberAPI storageMember : storageCargo.getMothballedShips().getMembersInPriorityOrder()) {
                if (areSameVariant(storageMember.getVariant(), playerFleetMember.getVariant())) {
                    storageMember.setCaptain(Global.getSector().getPlayerPerson());
                    playerFleetData.addFleetMember(storageMember);
                }
            }
        }

        refreshFleetUI();

        if (allFound) {
            CampaignUIMessage msg = new CampaignUIMessage(PresetUtils.RESTOREMESSAGE_SUCCESS_PREFIX + name, Misc.getPositiveHighlightColor());
            if (messageQueue.contains(msg)) messageQueue.remove(msg);
            messageQueue.add(msg);
        }
    }

    public static class CampaignUIMessage {
        public String message;
        public Color color;
    
        public CampaignUIMessage(String message, Color color) {
            this.message = message;
            this.color = color;
        }
    
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof CampaignUIMessage)) return false;
            CampaignUIMessage other = (CampaignUIMessage) obj;
            return Objects.equals(message, other.message) && Objects.equals(color, other.color);
        }
    
        @Override
        public int hashCode() {
            return Objects.hash(message, color);
        }
    }

    public static void addMessagesToCampaignUI() {
        List<PresetUtils.CampaignUIMessage> messageQueue = (List<PresetUtils.CampaignUIMessage>) Global.getSector().getMemoryWithoutUpdate().get(PresetUtils.MESSAGEQUEUE_KEY);
        for (PresetUtils.CampaignUIMessage message : messageQueue) {
            Global.getSector().getCampaignUI().addMessage(message.message, message.color);
        }
        messageQueue.clear();
    }

    // this is kinda jank but it works well enough for now
    // need a more seamless solution at some point
    public static void refreshFleetUI() {
        // Object fleetInfoPanel = Global.getSector().getMemoryWithoutUpdate().get(FLEETINFOPANEL_KEY);
        // Object infoPanelParent = ReflectionUtilis.invokeMethod("getParent", fleetInfoPanel);
        ReflectionUtilis.invokeMethod("recreateUI", ReflectionUtilis.invokeMethod("getParent", Global.getSector().getMemoryWithoutUpdate().get(FLEETINFOPANEL_KEY)));
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
    
            map.put(fleetPresetName, ships);
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
                    int cmp = Long.compare(Long.parseLong(num1), Long.parseLong(num2));
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