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

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import com.fs.starfarer.api.util.Misc;

import org.apache.log4j.Logger;

import data.scripts.util.CargoPresetUtils.CargoResourceRatios;

public class PresetUtils {
    public static final Logger logger = Logger.getLogger(PresetUtils.class);

    public static void print(Object... args) {
        MiscUtils.print(args);
    }

    public static final String PRESETS_MEMORY_KEY = "$player_fleet_presets";
    public static final String FLEETINFOPANEL_KEY = "$fleetInfoPanel";
    public static final String UNDOCKED_PRESET_KEY = "$presetUndocked";
    public static final String PLAYERCURRENTMARKET_KEY = "$playerCurrentMarket";
    public static final String COREUI_KEY = "$coreUI";
    public static final String ISPLAYERPAIDFORSTORAGE_KEY = "$isPlayerPaidForStorage";
    public static final String MESSAGEQUEUE_KEY = "$presetsMessageQueue";
    public static final String IS_UPDATE_KEY = "$isPresetUpdate";

    public static final String RESTOREMESSAGE_SUCCESS_PREFIX = "Successfully restored fleet preset: ";
    public static final String RESTOREMESSAGE_FAIL_PREFIX = "Could not find one or more of ";
    public static final String RESTOREMESSAGE_FAIL_SUFFIX = " in storage to load for preset: ";

    
    private static final HullSize[] SIZE_ORDER_DESCENDING = {
        HullSize.CAPITAL_SHIP,
        HullSize.DEFAULT,
        HullSize.CRUISER,
        HullSize.DESTROYER,
        HullSize.FIGHTER,
        HullSize.FRIGATE
    };
    private static final HullSize[] SIZE_ORDER_ASCENDING = {
        HullSize.FRIGATE,
        HullSize.FIGHTER,
        HullSize.DESTROYER,
        HullSize.CRUISER,
        HullSize.DEFAULT,
        HullSize.CAPITAL_SHIP
    };

    // TODO IMPLEMENT PLAYER CHOSEN CUSTOM ORDERING
    // No clue how to make it work, perhaps reflection on fleetpanelui tree? much investigation required. dont really know how reflection wroks honestly

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

    public static void sortShips(List<ShipVariantAPI> ships, HullSize[] order) {
        ships.sort((a, b) -> {
            HullSize sizeA = a.getHullSize();
            HullSize sizeB = b.getHullSize();
    
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
        public PersonAPI officer;
        public ShipVariantAPI variant;
        public int index;

        public OfficerVariantPair(PersonAPI officer, ShipVariantAPI variant, int index) {
            this.officer = officer;
            this.variant = variant;
            this.index = index;
        }
    }

    public static class FleetMemberWrapper {
        public final FleetMemberAPI member;
        public final PersonAPI captain;
        public final String captainId;
        public final int index;

        public FleetMemberWrapper(FleetMemberAPI member, PersonAPI captain, int index) {
            this.member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, member.getVariant());

            if (captain != null) {
                this.captain = captain;
                this.captainId = captain.getId();
                PersonAPI captainCopy = Global.getFactory().createPerson();
                captainCopy.setPortraitSprite(captain.getPortraitSprite());
                this.member.setCaptain(captainCopy);

            } else {
                this.captain = null;
                this.captainId = null;
            }

            this.index = index;
        }
    }

    public static FleetMemberWrapper wrapFleetMember(FleetMemberAPI member, int index) {
        if (member.getCaptain() != null) {
            return new FleetMemberWrapper(member, member.getCaptain(), index);
        }
        return new FleetMemberWrapper(member, null, index);
    }

    public static class FleetPreset {
        public final String name;
        public List<String> shipIds = new ArrayList<>();
        public Map<String, List<IndexedVariant>> variantsMap = new HashMap<>();
        public Map<String, List<OfficerVariantPair>> officersMap = new HashMap<>();
        public List<FleetMemberWrapper> fleetMembers = new ArrayList<>();
        public FleetPreset(String name, List<FleetMemberAPI> fleetMembers) {
            this.name = name;

            for (int i = 0; i < fleetMembers.size(); i++) {
                this.fleetMembers.add(new FleetMemberWrapper(fleetMembers.get(i), fleetMembers.get(i).getCaptain(), i));
            }
        }

        public void updateVariant(int index, ShipVariantAPI variant) {
            this.fleetMembers.get(index).member.setVariant(variant, true, true);
            for (IndexedVariant indexedVariant : this.variantsMap.get(variant.getHullSpec().getHullId())) {
                if (indexedVariant.index == index) {
                    indexedVariant.variant = variant;
                    break;
                }
            }

            List<OfficerVariantPair> pairs = this.officersMap.get(variant.getHullSpec().getHullId());
            if (pairs != null) {
                for (OfficerVariantPair pair : pairs) {
                    if (pair.index == index) {
                        pair.variant = variant;
                        break;
                    }
                }
            }
        }
    }


        // JEEPERS FEATURE CREEPERS???
        // will require variant d/smod agnostic implementation for comparisons in restoreFleetFromPreset functions
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
    // }

    // JEEPERS FEATURE CREEPERS???
    // public static FleetPreset serializeFleetPreset(String deserializedPreset) {
    //     // TODO implement this for use in import function
    //     // FleetPreset presetSerialized = new FleetPreset();

    //     // return presetSerialized;
    // }

    // would require logic for refit functions to check player ordnance point bonuses? i personally cant be bothered
    // public static void importPreset(String presetName) {
    //     // TODO implement

    //     // PROBLEM: How to handle officers from different save and apply new ones
    // }



    public static void initMothballedShips(CargoAPI storageCargo) {
        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            // i dont know what this does but the javadocs say to do it before calling getmothballedships and i think it stopped some crashing?
            storageCargo.initMothballedShips(faction.getId());
        }
    }

    // needs testing
    public static FleetPreset getPresetOfPlayerFleet(List<FleetMemberAPI> playerFleetMembers) {
        Map<String, FleetPreset> presets = getFleetPresets();
        
        for (FleetPreset preset : presets.values()) {
            if (playerFleetMembers.size() != preset.shipIds.size()) {
                continue;
            }

            Map<String, Integer> matchedShips = new HashMap<>();
            boolean allShipsMatched = true;

            for (FleetMemberAPI playerMember : playerFleetMembers) {
                String hullId = playerMember.getHullId();
                ShipVariantAPI variant = playerMember.getVariant();
                PersonAPI captain = playerMember.getCaptain();

                matchedShips.put(hullId, matchedShips.getOrDefault(hullId, 0) + 1);
                int count = matchedShips.get(hullId);


                List<IndexedVariant> presetVariants = preset.variantsMap.get(hullId);
                if (presetVariants == null || count > presetVariants.size()) {
                    allShipsMatched = false;
                    break;
                }

                boolean variantMatched = false;
                for (IndexedVariant indexedVariant : presetVariants) {
                    ShipVariantAPI presetVariant = indexedVariant.variant;

                    if (areSameVariant(presetVariant, variant)) {

                        if (preset.officersMap.containsKey(hullId)) {
                            List<OfficerVariantPair> pairs = preset.officersMap.get(hullId);
                            boolean officerMatched = false;
                            for (OfficerVariantPair pair : pairs) {
                                if (areSameVariant(pair.variant, variant)) {
                                    if (captain != null && pair.officer.getId().equals(captain.getId())) {
                                        officerMatched = true;
                                        break;
                                    }
                                }
                            }
                            if (!officerMatched) {
                                allShipsMatched = false;
                                break;
                            }
                        }
                        variantMatched = true;
                        break;
                    }
                }
                if (!variantMatched) {
                    allShipsMatched = false;
                    break;
                }
            }

            if (allShipsMatched) {
                return preset;
            }
        }
        return null;
    }

    public static boolean isPresetPlayerFleet(String presetName) {
        FleetPreset preset = getFleetPresets().get(presetName);
        if (preset == null) return false;

        List<FleetMemberAPI> playerFleetMembers = Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder();
        if (playerFleetMembers.size() != preset.shipIds.size()) {
            return false;
        }

        boolean allShipsMatched = true;
        for (FleetMemberAPI member : playerFleetMembers) {
            String hullId = member.getHullId();
            ShipVariantAPI variant = member.getVariant();
            PersonAPI captain = member.getCaptain();

            if (!preset.shipIds.contains(hullId)) {
                allShipsMatched = false;
                break;
            }

            List<IndexedVariant> presetVariants = preset.variantsMap.get(hullId);
            if (presetVariants == null) {
                allShipsMatched = false;
                break;
            }

            boolean variantMatched = false;
            for (IndexedVariant indexedVariant : presetVariants) {
                ShipVariantAPI presetVariant = indexedVariant.variant;

                if (areSameVariant(presetVariant, variant)) {
                    if (preset.officersMap.containsKey(hullId)) {
                        List<OfficerVariantPair> pairs = preset.officersMap.get(hullId);
                        boolean officerMatched = false;
                        for (OfficerVariantPair pair : pairs) {
                            if (areSameVariant(pair.variant, variant)) {
                                if (captain != null && pair.officer.getId().equals(captain.getId())) {
                                    officerMatched = true;
                                    break;
                                }
                            }
                        }
                        if (!officerMatched) {
                            allShipsMatched = false;
                            break;
                        }
                    }
                    variantMatched = true;
                    break;
                }
            }

            if (!variantMatched) {
                allShipsMatched = false;
                break;
            }
        }

        return allShipsMatched;
    }

    private static Map<String, Integer> findNeededShips(FleetPreset preset, List<FleetMemberAPI> playerCurrentFleet) {
        Map<String, Integer> requiredShips = new HashMap<>();
        Map<String, Integer> foundShips = new HashMap<>();

        // First count required ships from preset
        for (String hullId : preset.shipIds) {
            requiredShips.put(hullId, requiredShips.getOrDefault(hullId, 0) + 1);
        }

        // Then count ships already in player's fleet
        if (playerCurrentFleet != null) {
            for (FleetMemberAPI member : playerCurrentFleet) {
                String hullId = member.getHullId();
                if (!requiredShips.containsKey(hullId)) continue;

                List<IndexedVariant> presetVariants = preset.variantsMap.get(hullId);
                if (presetVariants == null) continue;

                for (IndexedVariant indexedVariant : presetVariants) {
                    ShipVariantAPI presetVariant = indexedVariant.variant;

                    if (areSameVariant(presetVariant, member.getVariant())) {
                        foundShips.put(hullId, foundShips.getOrDefault(hullId, 0) + 1);
                        break;
                    }
                }
            }
        }


        Map<String, Integer> neededShips = new HashMap<>();
        for (Map.Entry<String, Integer> entry : requiredShips.entrySet()) {
            int needed = entry.getValue() - foundShips.getOrDefault(entry.getKey(), 0);
            if (needed > 0) {
                neededShips.put(entry.getKey(), needed);
            }
        }

        return neededShips;
    }

    public static boolean isPresetAvailableAtCurrentMarket(MarketAPI market, String presetName, List<FleetMemberAPI> currentPlayerFleet) {
        if (market == null) return false;
        FleetPreset preset = getFleetPresets().get(presetName);
        if (preset == null) return false;

        SubmarketAPI storage = market.getSubmarket(Submarkets.SUBMARKET_STORAGE);
        if (storage == null) return false;

        SubmarketPlugin storagePlugin = storage.getPlugin();
        if (!isPlayerPaidForStorage(storagePlugin)) return false;

        CargoAPI storageCargo = storage.getCargo();
        initMothballedShips(storageCargo);
        FleetDataAPI mothballedShipsFleetData = storageCargo.getMothballedShips();

        Map<String, Integer> neededShips = findNeededShips(preset, currentPlayerFleet);
        if (neededShips.isEmpty()) return true;

        Map<String, Integer> foundShips = new HashMap<>();

        for (FleetMemberAPI storedMember : mothballedShipsFleetData.getMembersInPriorityOrder()) {
            String hullId = storedMember.getHullId();
            
            if (!neededShips.containsKey(hullId)) continue;

            if (foundShips.getOrDefault(hullId, 0) >= neededShips.get(hullId)) continue;

            List<IndexedVariant> presetVariants = preset.variantsMap.get(hullId);
            if (presetVariants == null) continue;

            for (IndexedVariant indexedVariant : presetVariants) {
                ShipVariantAPI presetVariant = indexedVariant.variant;

                if (areSameVariant(presetVariant, storedMember.getVariant())) {
                    foundShips.put(hullId, foundShips.getOrDefault(hullId, 0) + 1);
                    break;
                }
            }
        }

        for (Map.Entry<String, Integer> entry : neededShips.entrySet()) {
            if (foundShips.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return false;
            }
        }

        return true;
    }

    // should only be called if preset has no officers
    // needs testing
    public static void assignofficersToPreset(FleetPreset preset, List<FleetMemberAPI> playerFleetMembers) {
        for (String hullId: preset.shipIds) {
            for (int i = 0; i < playerFleetMembers.size(); i++) {
                FleetMemberAPI fleetMember = playerFleetMembers.get(i);
                if (hullId.equals(fleetMember.getHullId())) {
                    PersonAPI captain = fleetMember.getCaptain();

                    if (captain != null) {
                        OfficerVariantPair pair = new OfficerVariantPair(captain, fleetMember.getVariant(), i);
                        
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
            for (int i = 0; i < playerFleetMembers.size(); i++) {
                FleetMemberAPI fleetMember = playerFleetMembers.get(i);

                for (Map.Entry<String, List<OfficerVariantPair>> entry : preset.officersMap.entrySet()) {
                    String hullId = entry.getKey();
                    if (fleetMember.getHullId() == hullId) {

                        List<OfficerVariantPair> pairs = entry.getValue();

                        for (int j = 0; j < pairs.size(); j++) {
                            OfficerVariantPair pair = pairs.get(j);

                            if (areSameVariant(pair.variant, fleetMember.getVariant())) {
                                if (!isOfficerInFleet(pair.officer.getId(), playerFleetMembers)) {
                                    pairs.remove(j);
                                    pairs.set(j, new OfficerVariantPair(fleetMember.getCaptain(), pair.variant, i));
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
        CoreUIAPI coreUI = (CoreUIAPI) Global.getSector().getMemoryWithoutUpdate().get(PresetUtils.COREUI_KEY);
        return storagePlugin.getOnClickAction(coreUI).equals(SubmarketPlugin.OnClickAction.OPEN_SUBMARKET);
    }

    public static class NullIgnoringList<E> extends ArrayList<E> {
        @Override
        public boolean isEmpty() {
            for (E element : this) {
                if (element != null) return false;
            }
            return true;
        }
    }
    public static boolean isVariantHullBare (ShipVariantAPI sourceVariant) {
        NullIgnoringList<Object> fittedList = new NullIgnoringList<>();

        fittedList.add(sourceVariant.getNonBuiltInHullmods());
        fittedList.add(sourceVariant.getSModdedBuiltIns());
        fittedList.add(sourceVariant.getPermaMods());
        fittedList.add(sourceVariant.getSMods());
        fittedList.add(sourceVariant.getNonBuiltInWings());
        fittedList.add(sourceVariant.getNonBuiltInWeaponSlots());
        fittedList.add(sourceVariant.getWeaponGroups());
        fittedList.add(sourceVariant.getNumFluxCapacitors());
        fittedList.add(sourceVariant.getNumFluxVents());
        fittedList.add(sourceVariant.getNumFluxVents());

        return fittedList.isEmpty();
    }
    // prolly make sure you call PresetUtils.initMothballedShips before calling this
    public static ShipVariantAPI findBareHullVariantInStorage(CargoAPI storageCargo) {
        FleetDataAPI mothballedShipsFleetData = storageCargo.getMothballedShips();

        for (FleetMemberAPI member : mothballedShipsFleetData.getMembersInPriorityOrder()) {
            ShipVariantAPI variant = member.getVariant();
            return variant;
        }
        return null;
    }

    public static void storeFleetInStorage(String name) {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        MarketAPI market = getPlayerCurrentMarket();
        if (market == null) return;

        SubmarketAPI storage = market.getSubmarket(Submarkets.SUBMARKET_STORAGE);
        // CargoResourceRatios cargoRatios = new CargoResourceRatios(playerFleet.getFleetData().getMembersInPriorityOrder(), playerFleet.getCargo());
        SubmarketPlugin storagePlugin = storage.getPlugin();
        if (!isPlayerPaidForStorage(storagePlugin)) return;
        
        CargoAPI storageCargo = storage.getCargo();
        // CargoAPI playerCargo = playerFleet.getCargo();
        initMothballedShips(storageCargo);
        
        FleetDataAPI playerFleetData = playerFleet.getFleetData();
        FleetDataAPI mothballedShipsFleetData = storageCargo.getMothballedShips();

        for (FleetMemberAPI member : playerFleetData.getMembersInPriorityOrder()) {
            if (member.getCaptain() != null) {
                if (member.getCaptain().isPlayer()) continue;
                member.setCaptain(null);
            }
            playerFleetData.removeFleetMember(member);

            mothballedShipsFleetData.addFleetMember(member);
        }
        refreshFleetUI();

        // this needs more work and conditional logic with options
        // CargoPresetUtils.MaxFuelSuppliesAndCrew(playerCargo, storageCargo);
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

    public static class IndexedVariant {
        public int index;
        public ShipVariantAPI variant;

        public IndexedVariant(int index, ShipVariantAPI variant) {
            this.index = index;
            this.variant = variant;
        }
    }

    public static void saveFleetPreset(String name) {
        List<FleetMemberAPI> fleetMembers = Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder();

        // sortFleetMembers(fleetMembers, SIZE_ORDER_DESCENDING);
        FleetPreset preset = new FleetPreset(name, fleetMembers);

        for (int i = 0; i < fleetMembers.size(); i++) {
            FleetMemberAPI member = fleetMembers.get(i);

            String hullId = member.getHullId();
            ShipVariantAPI variant = member.getVariant();
            PersonAPI captain = member.getCaptain();

            preset.shipIds.add(hullId);
            
            if (!preset.variantsMap.containsKey(hullId)) {
                List<IndexedVariant> variants = new ArrayList<>();
                preset.variantsMap.put(hullId, variants);
            }
            preset.variantsMap.get(hullId).add(new IndexedVariant(i, variant));

            if (captain != null) {
                OfficerVariantPair pair = new OfficerVariantPair(captain, variant, i);

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
    
    // unused
    public static List<FleetMemberAPI> getFleetMembersOfPreset(String name) {
        FleetPreset preset = getFleetPresets().get(name);
        if (preset == null) return null;

        List<FleetMemberAPI> members = new ArrayList<>();
        List<String> doneHullIds = new ArrayList<>();

        for (String hullId : preset.shipIds) {
            if (doneHullIds.contains(hullId)) continue;

            for (IndexedVariant indexedVariant : preset.variantsMap.get(hullId)) {
                members.add(Global.getFactory().createFleetMember(FleetMemberType.SHIP, indexedVariant.variant));
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

    public static boolean areSameHullMods(ShipVariantAPI variant1, ShipVariantAPI variant2) {
        return (variant1.getHullMods().equals(variant2.getHullMods()));
    }

    // TODO make D/SMOD AGNOSTIC SETTINGS, NEW HULLS? 
    // WHAT IF PLAYER WANTS VERY SPECIFIC DMOD/OFFICER VARIANTS?
    @SuppressWarnings("unchecked")
    public static void restoreFleetFromPreset(String name) {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        MarketAPI market = getPlayerCurrentMarket();
        if (market == null) return;

        FleetPreset preset = getFleetPresets().get(name);
        if (preset == null) return;
        
        SubmarketAPI storage = market.getSubmarket(Submarkets.SUBMARKET_STORAGE);
        CargoAPI storageCargo = storage.getCargo();
        initMothballedShips(storageCargo);
        
        FleetDataAPI playerFleetData = playerFleet.getFleetData();
        List<FleetMemberAPI> playerFleetMembers = playerFleet.getFleetData().getMembersInPriorityOrder();

        // CargoAPI playerCargo = playerFleet.getCargo();
        // CargoResourceRatios cargoRatios = new CargoResourceRatios(playerFleetMembers, playerCargo);

        FleetMemberAPI playerFleetMember = getPlayerFleetMemberCopy(playerFleetData);

        for (FleetMemberAPI member : playerFleetMembers) {
            if (member.getCaptain() != null) {
                member.setCaptain(null);
            }
            playerFleetData.removeFleetMember(member);
            storageCargo.getMothballedShips().addFleetMember(member);
        }
        initMothballedShips(storageCargo);

        List<CampaignUIMessage> messageQueue = (List<CampaignUIMessage>) Global.getSector().getMemoryWithoutUpdate().get(MESSAGEQUEUE_KEY);

        List<String> doneofficers = new ArrayList<>();
        List<FleetMemberAPI> membersDone = new ArrayList<>();
        boolean allFound = true;

        for (String hullId : preset.shipIds) {
            List<IndexedVariant> variants = preset.variantsMap.get(hullId);
            int variantsToGet = Collections.frequency(preset.shipIds, hullId);
            
            int found = 0;
            for (IndexedVariant indexedVariant : variants) {
                ShipVariantAPI variant = indexedVariant.variant;

                for (FleetMemberAPI storedMember : storageCargo.getMothballedShips().getMembersInPriorityOrder()) {
                    if (found == variantsToGet) break;

                    if (storedMember.getHullId().equals(hullId)) {
                        if (preset.officersMap.containsKey(hullId)) {
                            
                            List<OfficerVariantPair> pairs = preset.officersMap.get(hullId);
                            boolean officerVariantFound = false;
                            for (OfficerVariantPair pair : pairs) {
                                if (areSameVariant(pair.variant, variant) && !doneofficers.contains(pair.officer.getId())) {
                                    storedMember.setCaptain(pair.officer);

                                    storageCargo.getMothballedShips().removeFleetMember(storedMember);
                                    membersDone.add(storedMember);
                                    // CargoPresetUtils.refit(storedMember, variant, playerCargo, storageCargo);

                                    doneofficers.add(pair.officer.getId());
                                    officerVariantFound = true;
                                    found++;

                                    ;
                                    break;
                                }
                                if (officerVariantFound) break;
                            }
                            if (officerVariantFound) continue;
                        }
                        
                        if (areSameVariant(variant, storedMember.getVariant())) {
                            storageCargo.getMothballedShips().removeFleetMember(storedMember);
                            membersDone.add(storedMember);
                            // CargoPresetUtils.refit(storedMember, variant, playerCargo, storageCargo);
                            found++;

                            membersDone.add(storedMember);
                            break;
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
        }

        for (FleetMemberAPI member : membersDone) {
            playerFleetData.addFleetMember(member);
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

        // this needs more work and conditional logic with options
        // CargoPresetUtils.equalizeCargo(playerFleetData.getMembersInPriorityOrder(), playerCargo, storageCargo, cargoRatios);

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
            return (this.hashCode() == obj.hashCode());
        }
    
        @Override
        public int hashCode() {
            return Objects.hash(message);
        }
    }

    @SuppressWarnings("unchecked")
    public static void addMessagesToCampaignUI() {
        List<PresetUtils.CampaignUIMessage> messageQueue = (List<PresetUtils.CampaignUIMessage>) Global.getSector().getMemoryWithoutUpdate().get(PresetUtils.MESSAGEQUEUE_KEY);
        for (PresetUtils.CampaignUIMessage message : messageQueue) {
            Global.getSector().getCampaignUI().addMessage(message.message, message.color);
        }
        messageQueue.clear();
    }

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

    public static String createShipCountString(Map<String, Integer> shipCountMap, Map<String, HullSize> shipHullSizes, Map<String, ShipHullSpecAPI> shipHullSpecs, HullSize[] shipOrder) {
        return sortShipCountMap(shipCountMap, shipHullSizes, shipHullSpecs, shipOrder)
            .stream()
            .map(PresetUtils::formatShipCount)
            .collect(Collectors.joining(", "));
    }

    private static String formatShipCount(Map.Entry<String, Integer> entry) {
        return entry.getValue() > 1 ? entry.getKey() + " x" + entry.getValue() : entry.getKey();
    }

    public static List<Map.Entry<String, Integer>> sortShipCountMap(Map<String, Integer> shipCountMap, Map<String, HullSize> shipHullSizes, Map<String, ShipHullSpecAPI> shipHullSpecs, HullSize[] shipOrder) {
        return shipCountMap.entrySet()
            .stream()
            .sorted((e1, e2) -> {
                HullSize size1 = shipHullSizes.get(e1.getKey());
                HullSize size2 = shipHullSizes.get(e2.getKey());
                
                boolean aIsCivilian = shipHullSpecs.get(e1.getKey()).isCivilianNonCarrier();
                boolean bIsCivilian = shipHullSpecs.get(e2.getKey()).isCivilianNonCarrier();
        
                if (aIsCivilian && !bIsCivilian) return 1;
                if (!aIsCivilian && bIsCivilian) return -1;
        
                int index1 = Arrays.asList(shipOrder).indexOf(size1);
                int index2 = Arrays.asList(shipOrder).indexOf(size2);
                return Integer.compare(index1, index2);
            })
            .collect(Collectors.toList());
    }

    public static LinkedHashMap<String, String> getFleetPresetsMapForTable_STRINGSONLY(boolean ascendingNames, boolean ascendingShips) {
        HashMap<String, String> map = new HashMap<>();
        HullSize[] shipOrder;
        if (ascendingShips) {
            shipOrder = SIZE_ORDER_ASCENDING;
        } else {
            shipOrder = SIZE_ORDER_DESCENDING;
        }
    
        Map<String, FleetPreset> presets = getFleetPresets();
        for (Map.Entry<String, FleetPreset> entry : presets.entrySet()) {
            String fleetPresetName = entry.getKey();
            FleetPreset fleetPreset = entry.getValue();
        
            Map<String, Integer> shipCountMap = new LinkedHashMap<>();
            Map<String, HullSize> shipHullSizes = new HashMap<>();
            Map<String, ShipHullSpecAPI> shipHullSpecs = new HashMap<>();

            for (Map.Entry<String, List<IndexedVariant>> variantsEntry : fleetPreset.variantsMap.entrySet()) {
                List<IndexedVariant> variantList = variantsEntry.getValue();

                for (IndexedVariant variant_ : variantList) {
                    ShipVariantAPI variant = variant_.variant;

                    String name = variant.getHullSpec().getHullName();
                    shipCountMap.put(name, shipCountMap.getOrDefault(name, 0) + 1);
                    shipHullSizes.put(name, variant.getHullSize());
                    shipHullSpecs.put(name, variant.getHullSpec());
                }
            }

            String ships = createShipCountString(shipCountMap, shipHullSizes, shipHullSpecs, shipOrder);
            map.put(fleetPresetName, ships);
        }
        return MiscUtils.sortByKeyAlphanumerically(map, ascendingNames);
    }

    public static LinkedHashMap<String, FleetPreset> getFleetPresetsMapForTable(boolean ascendingNames, boolean ascendingShips) {
        Map<String, FleetPreset> presets = getFleetPresets();
        LinkedHashMap<String, FleetPreset> sortedMap = new LinkedHashMap<>();
        
        // HullSize[] shipOrder = ascendingShips ? SIZE_ORDER_ASCENDING : SIZE_ORDER_DESCENDING;
        // for (FleetPreset preset : presets.values()) {
        //     if (preset.fleetMembers != null) {
        //         // sortFleetMembers(preset.fleetMembers, shipOrder);
        //     }
        // }
        
        // alphanumeric sorting by key
        List<String> keys = new ArrayList<>(presets.keySet());
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
                    if (cmp != 0) return ascendingNames ? cmp : -cmp;
                } else {
                    int cmp = Character.compare(
                        Character.toLowerCase(c1),
                        Character.toLowerCase(c2)
                    );
                    if (cmp != 0) return ascendingNames ? cmp : -cmp;
                    i++;
                    j++;
                }
            }
            return ascendingNames ? Integer.compare(s1.length(), s2.length()) : Integer.compare(s2.length(), s1.length());
        });

        for (String key : keys) {
            sortedMap.put(key, presets.get(key));
        }
        
        return sortedMap;
    }
}