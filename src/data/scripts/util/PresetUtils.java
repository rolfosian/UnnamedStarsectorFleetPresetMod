package data.scripts.util;

import java.util.*;
import java.util.stream.Collectors;
import java.awt.Color;
import java.io.Serializable;
import java.lang.invoke.VarHandle;
import java.lang.invoke.MethodHandles;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.SubmarketPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.fleet.FleetMember;
import com.fs.starfarer.campaign.fleet.FleetMemberStatus;
import com.fs.starfarer.campaign.fleet.RepairTracker;
// import com.fs.starfarer.campaign.fleet.FleetMember;
import com.fs.starfarer.loading.specs.HullVariantSpec;
import com.fs.starfarer.rpg.Person;

import static data.scripts.util.UiUtil.utils;

import data.scripts.util.BaseEveryFrameScript;
import data.scripts.util.PresetUtils.CampaignUIMessage;
import data.scripts.util.PresetUtils.FleetPreset;
import data.scripts.util.PresetUtils.NullIgnoringList;
import data.scripts.util.PresetUtils.OfficerVariantPair;
import data.scripts.util.PresetUtils.VariantWrapper;
import data.scripts.util.TreeTraverser.TreeNode;

import data.scripts.FleetPresetManagerPlugin;
import data.scripts.listeners.DockingListener;
// import data.scripts.util.CargoPresetUtils.CargoResourceRatios;

import static data.scripts.util.PresetMiscUtils.print;

@SuppressWarnings("unchecked")
public class PresetUtils {
    // Persistent data keys
    public static final String PRESETS_MEMORY_KEY = "$playerFleetPresets";
    public static final String IS_AUTO_UPDATE_KEY = "$isPresetAutoUpdate";
    // public static final String PRESET_MEMBERS_KEY = "$fleetPresetMembers";
    // public static final String STORED_PRESET_MEMBERIDS_KEY = "$storedFleetPresetMembers";
    public static final String KEEPCARGORATIOS_KEY = "$isPresetCargoRatios";

    // Non-persistent data keys
    public static final String UNDOCKED_PRESET_KEY = "$presetUndocked";
    public static final String PLAYERCURRENTMARKET_KEY = "$playerCurrentMarket";
    public static final String ISPLAYERPAIDFORSTORAGE_KEY = "$isPlayerPaidForStorage";
    // public static final String FLEET_TAB_KEY = "$fleetCoreUiTabe";
    // public static final String COREUI_KEY = "$coreUI";
    // public static final String OFFICER_AUTOASSIGN_BUTTON_KEY = "$officerAutoAssignButton";
    
    public static final String RESTOREMESSAGE_SUCCESS_PREFIX = "Successfully restored fleet preset: ";
    public static final String RESTOREMESSAGE_FAIL_PREFIX = "Could not find one or more of ";
    public static final String RESTOREMESSAGE_FAIL_SUFFIX = " in storage to load for preset: ";
    public static final String OFFICER_NULL_PORTRAIT_PATH = "graphics/portraits/portrait_generic_grayscale.png";

    // these are for the fluff buttons of the save dialog
    public static final String[][] FLEET_TYPES = {
        {"Combat", "graphics/icons/skills/strike_commander.png"},
        {"Carrier", "graphics/icons/skills/carrier_command.png"},
        {"Stealth", "graphics/icons/skills/phase_corps.png"},

        {"Invasion" , "graphics/icons/missions/tactical_bombardment.png"},
        {"Exploration", "graphics/icons/skills/sensors.png"},
        {"Automated", "graphics/icons/skills/automated_ships.png"},

        {"Salvage", "graphics/icons/skills/salvaging.png"},
        {"Trade", "graphics/icons/skills/recovery_ops.png"},
        {"Colony Expedition", "graphics/icons/skills/planetary_ops.png"}
    };

    private static final HullSize[] SIZE_ORDER_DESCENDING = {
        HullSize.CAPITAL_SHIP,
        HullSize.DEFAULT,
        HullSize.CRUISER,
        HullSize.DESTROYER,
        HullSize.FRIGATE,
        HullSize.FIGHTER
    };
    private static final HullSize[] SIZE_ORDER_ASCENDING = {
        HullSize.FIGHTER,
        HullSize.FRIGATE,
        HullSize.DESTROYER,
        HullSize.CRUISER,
        HullSize.DEFAULT,
        HullSize.CAPITAL_SHIP
    };

    public static DockingListener getDockingListener() {
        for (CampaignEventListener listener : Global.getSector().getAllListeners()) {
            if (listener instanceof DockingListener) {
                return (DockingListener) listener;
            }
        }
        return null;
    }

    public static Object[] getDeploymentPointsBreakdown() {
        Map<String, int[]> breakdownData = new HashMap<>();
        int dpPtsTotal = 0;

        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
            if (member.getVariant().getHullSpec().isCivilianNonCarrier()) continue;

            if (!breakdownData.containsKey(member.getHullSpec().getHullName())) {
                breakdownData.put(member.getHullSpec().getHullName(), new int[] {1, (int)member.getDeploymentPointsCost()});
            } else {
                int[] values = breakdownData.get(member.getHullSpec().getHullName());
                values[0] += 1;
                values[1] += (int)member.getDeploymentPointsCost();
            }
            dpPtsTotal += member.getDeploymentPointsCost();
        }

        // sorting the entries by deployment points (highest first)
        LinkedHashMap<String, int[]> sortedBreakdownData = breakdownData.entrySet()
            .stream()
            .sorted(Map.Entry.<String, int[]>comparingByValue((a, b) -> Integer.compare(b[1], a[1])))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));

        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, int[]> entry : sortedBreakdownData.entrySet()) {
            String ship = entry.getKey();
            int shipQty = entry.getValue()[0];
            int dpPts = entry.getValue()[1];

            result.put(ship + " x" + String.valueOf(shipQty), String.valueOf(dpPts));
        }
        
        return new Object[]{dpPtsTotal, result};
    }

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

    public static String reverseMemberId(FleetMemberAPI member) {
        return new StringBuilder(member.getId()).reverse().toString();
    }

    public static String reverseOfficerId(PersonAPI officer) {
        return new StringBuilder(officer.getId()).reverse().toString();
    }

    public static class OfficerVariantPair {
        private PersonAPI officer;
        private ShipVariantAPI variant;
        private int index;

        public OfficerVariantPair(PersonAPI officer, ShipVariantAPI variant, int index) {
            this.officer = officer;
            this.variant = variant;
            this.index = index;
        }

        public PersonAPI getOfficer() {
            return this.officer;
        }

        public ShipVariantAPI getVariant() {
            return this.variant;
        }

        public int getIndex() {
            return this.index;
        }

        public void setVariant(ShipVariantAPI newVariant) {
            this.variant = newVariant;
        }
    }

    public static class VariantWrapper {
        private String shipName;
        private ShipVariantAPI variant;
        private int index;
        private FleetPreset preset;

        public VariantWrapper(ShipVariantAPI variant, int index, FleetPreset preset, String shipName) {
            this.variant = variant;
            this.index = index;
            this.preset = preset;
            this.shipName = shipName;
        }

        public ShipVariantAPI getVariant() {
            return this.variant;
        }

        public int getIndex() {
            return this.index;
        }

        public String getShipName() {
            return this.shipName;
        }

        public FleetPreset getPreset() {
            return this.preset;
        }

        public void updateVariant() {
            this.preset.updateVariant(this.index, this.variant, this.shipName);
        }
    }

    // for after game save. i dont know why we need to do this but we do
    public static void updatePresetVariants() {
        for (FleetPreset preset : getFleetPresets().values()) {
            for (VariantWrapper variantWrapper : preset.getVariantWrappers()) {
                variantWrapper.updateVariant();
            }
        }
    }

    public static class FleetPreset implements Serializable {
        public static final VarHandle repairTrackerHandle;

        static {
            try {
                MethodHandles.Lookup lookup = MethodHandles.lookup();

                repairTrackerHandle = MethodHandles.privateLookupIn(FleetMember.class, lookup).findVarHandle(
                    FleetMember.class,
                    "repairTracker",
                    RepairTracker.class
                );

            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        private final String name;

        private transient List<FleetMemberAPI> members;
        private List<String> shipIds = new ArrayList<>(); // this is redundant since refactoring but i cant be btohered changing related logic

        private List<ShipVariantAPI> variants = new ArrayList<>();
        private List<VariantWrapper> variantWrappers = new ArrayList<>();

        private Map<Integer, OfficerVariantPair> officersMap = new HashMap<>(); // these are not copies, they are direct references to the officers including the player

        public FleetPreset(String name, List<FleetMemberAPI> fleetMembers) {
            this.name = name;

            for (int i = 0; i < fleetMembers.size(); i++) {
                FleetMemberAPI member = fleetMembers.get(i);

                String hullId = member.getHullSpec().getBaseHullId();

                ShipVariantAPI variant = member.getVariant().clone();
                variantWrappers.add(new VariantWrapper(variant, i, this, member.getShipName()));

                this.shipIds.add(hullId);
                this.variants.add(variant);
    
                PersonAPI captain = member.getCaptain();
                if (!captain.isDefault()) {
                    officersMap.put(i, new OfficerVariantPair(captain, variant, i));
                }
            }
            this.refreshMembers();
        }

        private Object readResolve() {
            this.initVariants();
            this.refreshMembers();
            return this;
        }

        private void initVariants() {
            for (int i = 0; i < variantWrappers.size(); i++) {
                variantWrappers.get(i).updateVariant();
            }
        }

        public void updateVariant(int index, ShipVariantAPI variant, String shipName) {
            this.variants.set(index, variant);
            this.variantWrappers.set(index, new VariantWrapper(variant, index, this, shipName));
            this.shipIds.set(index, variant.getHullSpec().getBaseHullId());

            for (int i = 0; i < this.shipIds.size(); i++) {
                OfficerVariantPair pair = this.officersMap.get(index);
                if (pair != null) {
                    pair.setVariant(variant);
                }
            }
            this.refreshMembers();
        }

        public void updateOfficer(int index, PersonAPI captain) {
            if (!captain.isDefault()) {
                this.officersMap.put(index, new OfficerVariantPair(captain, this.variants.get(index), index));
    
            } else {
                OfficerVariantPair pair = this.officersMap.get(index);
                if (pair != null) {
                    this.officersMap.remove(index);
                }
            }
            this.refreshMembers();
        }

        public String getName() {
            return this.name;
        }

        public List<String> getShipIds() {
            return this.shipIds;
        }

        public List<ShipVariantAPI> getVariants() {
            return this.variants;
        }

        public List<VariantWrapper> getVariantWrappers() {
            return this.variantWrappers;
        }

        public Map<Integer, OfficerVariantPair> getOfficersMap() {
            return this.officersMap;
        }

        public List<FleetMemberAPI> getMembers() {
            return this.members;
        }

        public List<FleetMemberAPI> getMembers(Map<Integer, FleetMemberAPI> whichMembersAvailable) {
            List<FleetMemberAPI> result = new ArrayList<>();
            result.addAll(this.members);

            for (int i = 0; i < result.size(); i++) {
                FleetMemberAPI replacement = whichMembersAvailable.get(i);
                if (replacement != null) {
                    result.set(i, replacement);
                }
            }
            return result;
        }

        private void refreshMembers() {
            this.members = new ArrayList<>();
            for (int i = 0; i < this.variants.size(); i++) {
                OfficerVariantPair pair = this.officersMap.get(i);
                FleetMemberAPI member;
                if (pair != null) {
                    PersonAPI officer = pair.officer;

                    Person tempOfficer = new Person("steady") {
                        @Override
                        public MutableCharacterStatsAPI getFleetCommanderStats() {
                            return Global.getSector().getPlayerStats();
                        }
                    };

                    member = new FleetMember(0, (HullVariantSpec)pair.variant, FleetMemberType.SHIP) {
                        @Override
                        public boolean isFlagship() {
                            return officer.isPlayer();
                        }

                        @Override
                        public PersonAPI getFleetCommanderForStats() {
                            return Global.getSector().getPlayerPerson();
                        }

                        @Override
                        public boolean canBeDeployedForCombat() {
                            return true;
                        }

                        @Override
                        public String getShipName() {
                            return "";
                        }

                        @Override
                        public float getCrewFraction() {
                            return 1f;
                        }
                    };

                    tempOfficer.setName(officer.getName());
                    tempOfficer.setRankId(officer.getRankId());
                    tempOfficer.setAICoreId(officer.getAICoreId());
                    tempOfficer.setFaction(Global.getSector().getPlayerFaction().getId());
                    tempOfficer.setPortraitSprite(officer.getPortraitSprite());
                    tempOfficer.setStats(officer.getStats());

                    member.setCaptain(tempOfficer);

                } else {
                    member = new FleetMember(0, (HullVariantSpec)this.variants.get(i), FleetMemberType.SHIP) {
                        @Override
                        public boolean isFlagship() {
                            return false;
                        }

                        @Override
                        public PersonAPI getFleetCommanderForStats() {
                            return Global.getSector().getPlayerPerson();
                        }

                        @Override
                        public boolean canBeDeployedForCombat() {
                            return true;
                        }

                        @Override
                        public String getShipName() {
                            return "";
                        }

                        @Override
                        public float getCrewFraction() {
                            return 1f;
                        }
                    };
                }
                repairTrackerHandle.set(member, new RepairTracker((FleetMember)member) {
                    @Override
                    public float getCR() {
                        return this.getMaxCR();
                    }

                    @Override
                    public float getMaxCR() {
                        return 0.7f;
                    }
                });
                member.setShipName(this.variantWrappers.get(i).getShipName());
                members.add(member);
            }
        }
    }

    public static Map<Integer, FleetMemberAPI> whichMembersAvailable(MarketAPI market, List<VariantWrapper> variantWrappers) {
        if (market == null) return whichMembersAvailable(variantWrappers);
        
        SubmarketAPI storage = CargoPresetUtils.getStorageSubmarket(market);
        if (storage == null || !isPlayerPaidForStorage(storage.getPlugin())) return whichMembersAvailable(variantWrappers);
        
        CargoAPI storageCargo = storage.getCargo();
        initMothballedShips(storageCargo);

        Map<Integer, FleetMemberAPI> seen = new HashMap<>();

        Map<Integer, FleetMemberAPI> seenPlayer = new HashMap<>();
        Map<Integer, FleetMemberAPI> seenStorage = new HashMap<>();
        for (int i = 0; i < variantWrappers.size(); i++) {
            boolean seent = false;
            for (FleetMemberAPI playerMember : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
                if (!seenPlayer.values().contains(playerMember) && areSameVariant(variantWrappers.get(i).getVariant(), playerMember.getVariant())) {
                    seenPlayer.put(i, playerMember);
                    seent = true;
                    break;
                }
            }
            if (seent) continue;

            for (FleetMemberAPI storedMember : storageCargo.getMothballedShips().getMembersListCopy()) {
                if (!seenStorage.values().contains(storedMember) && areSameVariant(variantWrappers.get(i).getVariant(), storedMember.getVariant())) {
                    seenStorage.put(i, storedMember);
                    break;
                }
            }
        }

        seen.putAll(seenPlayer);
        seen.putAll(seenStorage);
        return seen;
    }

    public static Map<Integer, FleetMemberAPI> whichMembersAvailable(List<VariantWrapper> variantWrappers) {
        Map<Integer, FleetMemberAPI> seen = new HashMap<>();
        for (int i = 0; i < variantWrappers.size(); i++) {
            for (FleetMemberAPI playerMember : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
                if (!seen.values().contains(playerMember) && areSameVariant(variantWrappers.get(i).getVariant(), playerMember.getVariant())) {
                    seen.put(i, playerMember);
                    break;
                }
            }
        }
        return seen;
    }

    public static boolean isPlayerFleetChanged(FleetPreset preset, List<FleetMemberAPI> playerFleetMembers) {
        if (playerFleetMembers.size() != preset.getVariants().size()) {
            return true;
        }
        
        for (int i = 0; i < preset.getVariants().size(); i++) {
            FleetMemberAPI playerFleetMember = playerFleetMembers.get(i);
            ShipVariantAPI presetVariant = preset.getVariants().get(i);

            if (!areSameVariant(playerFleetMember.getVariant(), presetVariant)
                || !isOfficerSameAsPresetMember(playerFleetMember, preset.getOfficersMap().get(i))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isOfficerNought(PersonAPI officer) {
        if (officer == null) return true;
        return officer.getPortraitSprite().equals(OFFICER_NULL_PORTRAIT_PATH);
    }
    
    public static boolean isOfficerSameAsPresetMember(FleetMemberAPI playerFleetMember, OfficerVariantPair pair) {
        if (isOfficerNought(playerFleetMember.getCaptain()) && pair == null || isOfficerNought(pair.getOfficer())) {
            return true;
        }
        if (isOfficerNought(playerFleetMember.getCaptain()) || isOfficerNought(pair.getOfficer())) {
            return false;
        }
        return playerFleetMember.getCaptain().getId().equals(pair.getOfficer().getId());
    }

    public static void initMothballedShips(CargoAPI storageCargo) {
        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            storageCargo.initMothballedShips(faction.getId());
        }
    }

    public static FleetPreset getPresetOfMembers(List<FleetMemberAPI> targetMembers) {
        Map<String, FleetPreset> presets = getFleetPresets();
        if (presets == null) presets = (Map<String, FleetPreset>) Global.getSector().getPersistentData().computeIfAbsent(PRESETS_MEMORY_KEY, k -> new HashMap<>());

        for (FleetPreset preset : presets.values()) {
            if (targetMembers.size() != preset.getShipIds().size()) {
                continue;
            }

            boolean allShipsMatched = true;

            for (int i = 0; i < targetMembers.size(); i++) {
                FleetMemberAPI playerMember = targetMembers.get(i);
                ShipVariantAPI variant = playerMember.getVariant();
                PersonAPI captain = playerMember.getCaptain();

                ShipVariantAPI presetVariant = preset.getVariants().get(i);
                if (presetVariant == null) {
                    allShipsMatched = false;
                    break;
                }

                boolean variantMatched = false;
                if (areSameVariant(presetVariant, variant)) {
                    OfficerVariantPair pair = preset.getOfficersMap().get(i);
                    if (pair != null) {
                        boolean officerMatched = false;

                        if (areSameVariant(pair.getVariant(), variant) && (captain != null && pair.getOfficer().getId().equals(captain.getId()))) officerMatched = true;

                        if (!officerMatched) {
                            allShipsMatched = false;
                            break;
                        }
                    }
                    variantMatched = true;
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

    public static List<FleetPreset> getPresetsOfMembers(List<FleetMemberAPI> targetMembers) {
        Map<String, FleetPreset> presets = getFleetPresets();
        List<FleetPreset> result = new ArrayList<>();

        for (FleetPreset preset : presets.values()) {
            if (targetMembers.size() != preset.getShipIds().size()) {
                continue;
            }

            boolean allShipsMatched = true;

            for (int i = 0; i < targetMembers.size(); i++) {
                FleetMemberAPI playerMember = targetMembers.get(i);
                ShipVariantAPI variant = playerMember.getVariant();
                PersonAPI captain = playerMember.getCaptain();

                ShipVariantAPI presetVariant = preset.getVariants().get(i);
                if (presetVariant == null) {
                    allShipsMatched = false;
                    break;
                }

                boolean variantMatched = false;
                if (areSameVariant(presetVariant, variant)) {
                    OfficerVariantPair pair = preset.getOfficersMap().get(i);
                    if (pair != null) {
                        boolean officerMatched = false;

                        if (areSameVariant(pair.getVariant(), variant) && areSameOfficerMinusId(captain, pair.getOfficer())) officerMatched = true;

                        if (!officerMatched) {
                            allShipsMatched = false;
                            break;
                        }
                    }
                    variantMatched = true;
                }
                
                if (!variantMatched) {
                    allShipsMatched = false;
                    break;
                }
            }

            if (allShipsMatched) {
                result.add(preset);
            }
        }
        return result;
    }

    public static boolean isPresetPlayerFleet(FleetPreset preset) {
        if (preset == null) return false;

        List<FleetMemberAPI> playerFleetMembers = Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy();
        if (playerFleetMembers.size() != preset.getShipIds().size()) {
            return false;
        }

        boolean allShipsMatched = true;
        for (int i = 0; i < playerFleetMembers.size(); i++) {
            FleetMemberAPI member = playerFleetMembers.get(i);
            String hullId = member.getHullSpec().getBaseHullId();
            ShipVariantAPI variant = member.getVariant();
            PersonAPI captain = member.getCaptain();

            if (!preset.getShipIds().contains(hullId)) {
                allShipsMatched = false;
                break;
            }

            ShipVariantAPI presetVariant = preset.getVariants().get(i);
            if (presetVariant == null) {
                allShipsMatched = false;
                break;
            }
            boolean variantMatched = false;

            if (areSameVariant(presetVariant, variant)) {
                OfficerVariantPair pair = preset.getOfficersMap().get(i);

                if (pair != null) {
                    boolean officerMatched = false;
                    if (areSameVariant(pair.getVariant(), variant) && pair.getOfficer().getId().equals(captain.getId())) {
                        officerMatched = true;
                    }
                    if (!officerMatched) {
                        allShipsMatched = false;
                        break;
                    }
                }
                variantMatched = true;
            }
            
            if (!variantMatched) {
                allShipsMatched = false;
                break;
            }
        }
        return allShipsMatched;
    }

    public static boolean isPresetPlayerFleetOfficerAgnostic(FleetPreset preset) {
        if (preset == null) return false;

        List<FleetMemberAPI> playerFleetMembers = Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy();
        if (playerFleetMembers.size() != preset.getShipIds().size()) {
            return false;
        }

        boolean allShipsMatched = true;
        for (int i = 0; i < playerFleetMembers.size(); i++) {
            FleetMemberAPI member = playerFleetMembers.get(i);
            String hullId = member.getHullSpec().getBaseHullId();
            ShipVariantAPI variant = member.getVariant();

            if (!preset.getShipIds().contains(hullId)) {
                allShipsMatched = false;
                break;
            }

            ShipVariantAPI presetVariant = preset.getVariants().get(i);
            if (presetVariant == null) {
                allShipsMatched = false;
                break;
            }
            boolean variantMatched = false;

            if (areSameVariant(presetVariant, variant)) {
                variantMatched = true;
            }
            
            if (!variantMatched) {
                allShipsMatched = false;
                break;
            }
        }

        return allShipsMatched;
    }

    public static boolean isPresetContainedInPlayerFleet(FleetPreset preset) {
        if (preset == null) return false;

        List<FleetMemberAPI> playerFleetMembers = Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy();
        boolean[] matched = new boolean[playerFleetMembers.size()];

        for (int i = 0; i < preset.getShipIds().size(); i++) {
            String presetHullId = preset.getShipIds().get(i);
            ShipVariantAPI presetVariant = preset.getVariants().get(i);
            OfficerVariantPair presetOfficerPair = preset.getOfficersMap().get(i);
            boolean found = false;

            for (int j = 0; j < playerFleetMembers.size(); j++) {
                if (matched[j]) continue;
                FleetMemberAPI member = playerFleetMembers.get(j);
                String hullId = member.getHullSpec().getBaseHullId();
                ShipVariantAPI variant = member.getVariant();
                PersonAPI captain = member.getCaptain();

                if (!presetHullId.equals(hullId)) continue;
                if (!areSameVariant(presetVariant, variant)) continue;

                if (presetOfficerPair != null) {
                    if (captain.isDefault()) continue;
                    if (!areSameVariant(presetOfficerPair.getVariant(), variant)) continue;
                    if (!presetOfficerPair.getOfficer().getId().equals(captain.getId())) continue;
                }

                matched[j] = true;
                found = true;
                break;
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private static Map<String, Integer> findNeededShips(FleetPreset preset, List<FleetMemberAPI> playerCurrentFleet) {
        Map<String, Integer> requiredShips = new HashMap<>();
        Map<String, Integer> foundShips = new HashMap<>();

        for (String hullId : preset.getShipIds()) {
            requiredShips.put(hullId, requiredShips.getOrDefault(hullId, 0) + 1);
        }

        if (playerCurrentFleet != null) {
            for (FleetMemberAPI member : playerCurrentFleet) {
                String hullId = member.getHullSpec().getBaseHullId();
                if (!requiredShips.containsKey(hullId)) continue;

                for (ShipVariantAPI presetVariant : preset.getVariants()) {
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

    // member id agnostic
    public static boolean isPresetAvailableAtCurrentMarket(MarketAPI market, String presetName, List<FleetMemberAPI> currentPlayerFleet) {
        if (market == null) return false;
        FleetPreset preset = getFleetPresets().get(presetName);
        if (preset == null) return false;
        SubmarketAPI storage = CargoPresetUtils.getStorageSubmarket(market);
        if (storage == null) return false;

        SubmarketPlugin storagePlugin = storage.getPlugin();
        if (!isPlayerPaidForStorage(storagePlugin)) return false;

        CargoAPI storageCargo = storage.getCargo();
        initMothballedShips(storageCargo);
        FleetDataAPI mothballedShipsFleetData = storageCargo.getMothballedShips();

        Map<String, Integer> neededShips = findNeededShips(preset, currentPlayerFleet);
        
        if (neededShips.isEmpty()) return true;

        Map<String, Integer> foundShips = new HashMap<>();

        for (FleetMemberAPI storedMember : mothballedShipsFleetData.getMembersListCopy()) {
            String hullId = storedMember.getHullSpec().getBaseHullId();
            
            if (!neededShips.containsKey(hullId)) continue;
            if (foundShips.getOrDefault(hullId, 0) >= neededShips.get(hullId)) continue;

            List<ShipVariantAPI> presetVariants = preset.getVariants();

            for (ShipVariantAPI indexedVariant : presetVariants) {
                if (areSameVariant(indexedVariant, storedMember.getVariant())) {
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

    public static boolean isMemberFromPreset(FleetMemberAPI member, FleetPreset preset) {
        for (ShipVariantAPI variant : preset.getVariants()) {
            if (areSameVariant(member.getVariant(), variant)) {
                return true;
            }
        }
        return false;
    }

    // should only be called if preset has no officers
    // needs testing
    public static void assignofficersToPreset(FleetPreset preset, List<FleetMemberAPI> playerFleetMembers) {
        for (String hullId: preset.getShipIds()) {
            for (int i = 0; i < playerFleetMembers.size(); i++) {
                FleetMemberAPI fleetMember = playerFleetMembers.get(i);
                if (hullId.equals(fleetMember.getHullSpec().getBaseHullId())) {
                    PersonAPI captain = fleetMember.getCaptain();

                    if (!captain.isDefault()) {
                        preset.getOfficersMap().put(i, new OfficerVariantPair(captain, fleetMember.getVariant(), i));
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
            if (captain.getId().equals(officerId)) return true;
        }
        return false;
    }

    public static boolean isOfficerInPreset(String officerId, FleetPreset preset) {
        for (int i = 0; i < preset.getShipIds().size(); i++) {
            OfficerVariantPair pair = preset.getOfficersMap().get(i);
            if (pair != null && pair.getOfficer().getId().equals(officerId)) return true;
        }
        return false;
    }

    public static boolean isVariantInOfficerPairs(ShipVariantAPI variant, List<OfficerVariantPair> pairs) {
        for (OfficerVariantPair pair : pairs) {
            if (areSameVariant((pair.getVariant()), variant)) return true;
        }
        return false;
    }

    public static boolean areOfficersInPlayerFleet(List<FleetMemberAPI> fleetMembers) {
        for (FleetMemberAPI member : fleetMembers) {
            if (!member.getCaptain().getName().getFullName().equals("") && !member.getCaptain().isPlayer()) return true;
        }
        return false;
    }

    public static MarketAPI getPlayerCurrentMarket() {
        return (MarketAPI) Global.getSector().getMemoryWithoutUpdate().get(PLAYERCURRENTMARKET_KEY);
    }

    public static Map<String, FleetPreset> getFleetPresets() {
        return (Map<String, FleetPreset>) Global.getSector().getPersistentData().get(PRESETS_MEMORY_KEY);
    }

    public static boolean isPlayerPaidForStorage(SubmarketPlugin storagePlugin) {
        return (boolean) UiUtil.playerPaidToUnlockStorageHandle.get(storagePlugin);
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

        for (FleetMemberAPI member : mothballedShipsFleetData.getMembersListCopy()) {
            ShipVariantAPI variant = member.getVariant();
            return variant;
        }
        return null;
    }

    public static List<FleetMemberAPI> getMothBalledShips(MarketAPI market) {
        if (market == null) return null;
        SubmarketAPI storage = CargoPresetUtils.getStorageSubmarket(market);
        if (storage == null) return null;
        SubmarketPlugin storagePlugin = storage.getPlugin();
        if (!isPlayerPaidForStorage(storagePlugin)) return null;

        CargoAPI storageCargo = storage.getCargo();
        initMothballedShips(storageCargo);

        return storageCargo.getMothballedShips().getMembersListCopy();
    }

    public static FleetDataAPI getMothBalledShipsData(MarketAPI market) {
        if (market == null) return null;
        SubmarketAPI storage = CargoPresetUtils.getStorageSubmarket(market);
        if (storage == null) return null;
        SubmarketPlugin storagePlugin = storage.getPlugin();
        if (!isPlayerPaidForStorage(storagePlugin)) return null;

        CargoAPI storageCargo = storage.getCargo();
        initMothballedShips(storageCargo);

        return storageCargo.getMothballedShips();
    }

    public static void autoAssignOfficers() {
        UtilUi.clickButton(FleetPresetManagerPlugin.fleetPanelInjector.getOfficerAutoAssignButton());
    }

    public static void takeAllShipsFromStorage() {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        MarketAPI market = getPlayerCurrentMarket();
        if (market == null) return;

        SubmarketAPI storage = CargoPresetUtils.getStorageSubmarket(market);
        if (storage == null) return;

        SubmarketPlugin storagePlugin = storage.getPlugin();
        if (!isPlayerPaidForStorage(storagePlugin)) return;

        CargoAPI storageCargo = storage.getCargo();
        initMothballedShips(storageCargo);

        FleetDataAPI mothballedShipsFleetData = storageCargo.getMothballedShips();

        for (FleetMemberAPI member : mothballedShipsFleetData.getMembersListCopy()) {
            mothballedShipsFleetData.removeFleetMember(member);
            playerFleet.getFleetData().addFleetMember(member);
        }

        autoAssignOfficers();
        refreshFleetUI();
    }

    public static void storeFleetInStorage() {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        MarketAPI market = getPlayerCurrentMarket();
        if (market == null) return;

        SubmarketAPI storage = CargoPresetUtils.getStorageSubmarket(market);
        // CargoResourceRatios cargoRatios = new CargoResourceRatios(playerFleet.getFleetData().getMembersListCopy(), playerFleet.getCargo());
        SubmarketPlugin storagePlugin = storage.getPlugin();
        if (!isPlayerPaidForStorage(storagePlugin)) return;
        
        CargoAPI storageCargo = storage.getCargo();
        // CargoAPI playerCargo = playerFleet.getCargo();
        initMothballedShips(storageCargo);
        
        FleetDataAPI playerFleetData = playerFleet.getFleetData();
        FleetDataAPI mothballedShipsFleetData = storageCargo.getMothballedShips();

        for (FleetMemberAPI member : playerFleetData.getMembersListCopy()) {
            if (member.getCaptain().isPlayer()) continue;

            member.setCaptain(null);
            playerFleetData.removeFleetMember(member);
            mothballedShipsFleetData.addFleetMember(member);
        }
        refreshFleetUI();

        // this needs more work and conditional logic with options
        // CargoPresetUtils.MaxFuelSuppliesAndCrew(playerCargo, storageCargo);
    }

    public static void saveFleetPreset(String name) {
        List<FleetMemberAPI> fleetMembers = Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy();
        if (getFleetPresets().get(name) != null) {
            deleteFleetPreset(name);
        }
        // sortFleetMembers(fleetMembers, SIZE_ORDER_DESCENDING);
        getFleetPresets().put(name, new FleetPreset(name, fleetMembers));
    }

    public static FleetMemberAPI getPlayerFleetMember(FleetDataAPI playerFleetData) {
        for (FleetMemberAPI member : playerFleetData.getMembersListCopy()) {
            if (member.getCaptain().isPlayer()) return member;
        }
        return null;
    }

    public static boolean areSameWeaponSlots(Collection<String> slots1, Collection<String> slots2) {
        return new HashSet<>(slots1).equals(new HashSet<>(slots2));
    }

    public static boolean areSameVariantPrinted(ShipVariantAPI variant1, ShipVariantAPI variant2) {
        // xstream serializer mangles weapon groups on game save/load or something? so we need to do this
        // List<WeaponGroupSpec> variant1WeaponGroups = variant1.getWeaponGroups();
        // List<WeaponGroupSpec> variant2WeaponGroups = variant2.getWeaponGroups();

        print("-------------------------------------------------------");
        print(variant1.getHullSpec().getBaseHullId());

        // if (variant1WeaponGroups.size() != variant2WeaponGroups.size()) return false;
        // for (int i = 0; i < variant1WeaponGroups.size(); i++) {
        //     List<String> slots1 = variant1WeaponGroups.get(i).getSlots();
        //     List<String> slots2 = variant2WeaponGroups.get(i).getSlots();
        //     // slots1.equals(slots2) doesnt work either, we actually have to go through it and compare each directly

        //     if (slots1.size() != slots2.size()) return false;
        //     for (int j = 0; j < slots1.size(); j++) {
        //         if (!slots1.get(j).equals(slots2.get(j))) return false;
        //     }
        // }

        // print("Weapon groups are the same");
        print("hullId match:", variant1.getHullSpec().getBaseHullId().equals(variant2.getHullSpec().getBaseHullId()));
        print(variant1.getHullSpec().getBaseHullId(), variant2.getHullSpec().getBaseHullId());
        print("smods match:", variant1.getSMods().equals(variant2.getSMods()));
        print("hullmods match:", variant1.getHullMods().equals(variant2.getHullMods()));
        print("wings match:", variant1.getWings().equals(variant2.getWings()));
        print("fittedweaponslots match:", areSameWeaponSlots(variant1.getFittedWeaponSlots(), variant2.getFittedWeaponSlots()));
        // print("fittedweaponslots match raw:", variant1.getFittedWeaponSlots().equals(variant2.getFittedWeaponSlots()));
        print("smoddedbuiltins match:", variant1.getSModdedBuiltIns().equals(variant2.getSModdedBuiltIns()));
        print("permaMods match:", variant1.getPermaMods().equals(variant2.getPermaMods()));
        print("fluxcapacitors match:", variant1.getNumFluxCapacitors() == variant2.getNumFluxCapacitors(), variant1.getNumFluxCapacitors(), variant2.getNumFluxCapacitors());
        print("fluxvents match:", variant1.getNumFluxVents() == variant2.getNumFluxVents(), variant1.getNumFluxVents(), variant2.getNumFluxVents());
        print("-------------------------------------------------------");

        return (variant1.getHullSpec().getBaseHullId().equals(variant2.getHullSpec().getBaseHullId())
            && variant1.getSMods().equals(variant2.getSMods())
            && variant1.getHullMods().equals(variant2.getHullMods())
            && variant1.getWings().equals(variant2.getWings())
            && variant1.getPermaMods().equals(variant2.getPermaMods())
            // && variant1.getFittedWeaponSlots().equals(variant2.getFittedWeaponSlots()) // THIS DOESNT WORK AFTER GAME SAVE I DONT FUCKING KNOW WHY
            && areSameWeaponSlots(variant1.getFittedWeaponSlots(), variant2.getFittedWeaponSlots()) // this inexplicably works though
            && variant1.getSModdedBuiltIns().equals(variant2.getSModdedBuiltIns())
            // && variant1.getWeaponGroups().equals(variant2.getWeaponGroups()) // fuck you xstream
            && variant1.getNumFluxCapacitors() == variant2.getNumFluxCapacitors()
            && variant1.getNumFluxVents() == variant2.getNumFluxVents());
    }

    // this is because variant1.equals(variant2) doesnt always work
    // xstream is mangling half of this shit and i do not want to make rules for it
    public static boolean areSameVariant(ShipVariantAPI variant1, ShipVariantAPI variant2) {
        // xstream serializer mangles weapon groups on game save/load or something? so we need to do this
        // List<WeaponGroupSpec> variant1WeaponGroups = variant1.getWeaponGroups();
        // List<WeaponGroupSpec> variant2WeaponGroups = variant2.getWeaponGroups();

        // print("-------------------------------------------------------");
        // print(variant1.getHullSpec().getHullSpec().getBaseHullId());

        // if (variant1WeaponGroups.size() != variant2WeaponGroups.size()) return false;
        // for (int i = 0; i < variant1WeaponGroups.size(); i++) {
        //     List<String> slots1 = variant1WeaponGroups.get(i).getSlots();
        //     List<String> slots2 = variant2WeaponGroups.get(i).getSlots();
        //     // slots1.equals(slots2) doesnt work either, we actually have to go through it and compare each directly

        //     if (slots1.size() != slots2.size()) return false;
        //     for (int j = 0; j < slots1.size(); j++) {
        //         if (!slots1.get(j).equals(slots2.get(j))) return false;
        //     }
        // }

        return (variant1.getHullSpec().getBaseHullId().equals(variant2.getHullSpec().getBaseHullId())
            && variant1.getSMods().equals(variant2.getSMods())
            && variant1.getHullMods().equals(variant2.getHullMods())
            && variant1.getWings().equals(variant2.getWings())
            && variant1.getPermaMods().equals(variant2.getPermaMods())
            // && variant1.getFittedWeaponSlots().equals(variant2.getFittedWeaponSlots()) // THIS DOESNT WORK AFTER GAME SAVE I DONT FUCKING KNOW WHY
            && areSameWeaponSlots(variant1.getFittedWeaponSlots(), variant2.getFittedWeaponSlots()) // this inexplicably works though
            && variant1.getSModdedBuiltIns().equals(variant2.getSModdedBuiltIns())
            // && variant1.getWeaponGroups().equals(variant2.getWeaponGroups()) // fuck you xstream and ai shits
            && variant1.getNumFluxCapacitors() == variant2.getNumFluxCapacitors()
            && variant1.getNumFluxVents() == variant2.getNumFluxVents());
    }

    public static boolean areSameOfficerMinusIdPrinted(PersonAPI officer1, PersonAPI officer2) {
        print("-------------------------------------------------------");
        print("officer1:", officer1.getName().getFullName());
        print("officer2:", officer2.getName().getFullName());
        
        print("both default:", officer1.isDefault() && officer2.isDefault());
        print("officer1 isDefault:", officer1.isDefault());
        print("officer2 isDefault:", officer2.isDefault());
        
        print("stats match:", officer1.getStats() == officer2.getStats());
        print("portrait match:", officer1.getPortraitSprite().equals(officer2.getPortraitSprite()));
        print("name match:", officer1.getName().getFullName().equals(officer2.getName().getFullName()));
        
        print("-------------------------------------------------------");
        
        return((officer1.isDefault() && officer2.isDefault())
                || 
                (officer1.getStats() == officer2.getStats() && officer1.getPortraitSprite().equals(officer2.getPortraitSprite())
                && officer1.getName().getFullName().equals(officer2.getName().getFullName())));
    }

    public static boolean areSameOfficerMinusId(PersonAPI officer1, PersonAPI officer2) {
        return((officer1.isDefault() && officer2.isDefault())
                || 
                (officer1.getStats() == officer2.getStats() && officer1.getPortraitSprite().equals(officer2.getPortraitSprite())
                && officer1.getName().getFullName().equals(officer2.getName().getFullName())));
    }


    public static boolean areSameHullMods(ShipVariantAPI variant1, ShipVariantAPI variant2) {
        return (variant1.getHullMods().equals(variant2.getHullMods()));
    }

    public static boolean isPlayerInFleet(List<FleetMemberAPI> fleetMembers) {
        for (FleetMemberAPI member : fleetMembers) {
            if (member.getCaptain().getId().equals(Global.getSector().getPlayerPerson().getId())) return true;
        }
        return false;
    }

    public static void partRestorePreset(List<FleetMemberAPI> membersToRestore, Map<Integer, FleetMemberAPI> whichMembersAreAvailable, FleetPreset preset) { // TODO
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        MarketAPI market = getPlayerCurrentMarket();
        if (market == null) return;
        
        SubmarketAPI storage = CargoPresetUtils.getStorageSubmarket(market);
        if (storage == null) return;

        CargoAPI storageCargo = storage.getCargo();
        initMothballedShips(storageCargo);

        FleetDataAPI playerFleetData = playerFleet.getFleetData();
        List<FleetMemberAPI> playerFleetMembers = playerFleet.getFleetData().getMembersListCopy();

        for (FleetMemberAPI member : playerFleetMembers) {
            member.setCaptain(null);
            playerFleetData.removeFleetMember(member);
            storageCargo.getMothballedShips().addFleetMember(member);
        }
        initMothballedShips(storageCargo);

        Set<FleetMemberAPI> done = new HashSet<>();
        Set<PersonAPI> doneOfficers = new HashSet<>();
        for (FleetMemberAPI memberToRestore : membersToRestore) {
            for (FleetMemberAPI availableMember : whichMembersAreAvailable.values()) {
                if (!done.contains(availableMember) && areSameVariant(memberToRestore.getVariant(), availableMember.getVariant())) {
                    storageCargo.getMothballedShips().removeFleetMember(availableMember);
                    playerFleetData.addFleetMember(availableMember);

                    for (OfficerVariantPair pair : preset.getOfficersMap().values()) {
                        PersonAPI officer = pair.getOfficer();
                        if (areSameVariant(pair.getVariant(), memberToRestore.getVariant()) && !doneOfficers.contains(officer)) {
                            availableMember.setCaptain(officer);
                            doneOfficers.add(officer);
                            break;
                        }
                    }
                    done.add(availableMember);
                    break;
                }
            }
        }
        
        if (!isPlayerInFleet(playerFleetData.getMembersListCopy())) {
            for (FleetMemberAPI member : playerFleetData.getMembersListCopy()) {
                if (member.getCaptain().isDefault()) {
                    member.setCaptain(Global.getSector().getPlayerPerson());
                    break;
                }
            }
            playerFleetData.ensureHasFlagship();;
        }

        playerFleetData.setSyncNeeded();
        playerFleetData.syncIfNeeded();
        if (preset.getShipIds().size() != playerFleetData.getMembersListCopy().size()) getDockingListener().setUndockedPreset(null);

        // sortToMatchOrder(playerFleetData, preset.getCampaignFleet().getFleetData().getMembersListCopy()); // TODO
        refreshFleetUI();
    }
    
    public static List<PersonAPI> getOfficersOfPlayerFleet() {
        List<PersonAPI> result = new ArrayList<>();
        for (OfficerDataAPI data : Global.getSector().getPlayerFleet().getFleetData().getOfficersCopy()) result.add(data.getPerson());
        return result;
    }

    public static boolean isMatchVariantAndOfficer(ShipVariantAPI variant1, ShipVariantAPI variant2, PersonAPI officer1, PersonAPI officer2) {
        return areSameVariant(variant1, variant2) && areSameOfficerMinusId(officer1, officer2);
    }

    public static void sortToMatchOrder(FleetDataAPI fleetData, List<FleetMemberAPI> order) {
        fleetData.setSyncNeeded();
        fleetData.syncIfNeeded();

        List<FleetMemberAPI> fleetMembers = fleetData.getMembersListCopy();
        Map<Integer, FleetMemberAPI> preOrderedFleetMembers = new HashMap<>();
        for (int i = 0; i < fleetMembers.size(); i++) {
            FleetMemberAPI member = fleetMembers.get(i);
            preOrderedFleetMembers.put(i, member);
            fleetData.removeFleetMember(member);
        }
        List<FleetMemberAPI> newOrder = new ArrayList<>();

        for (FleetMemberAPI orderedMember : order) {
            for (int i : new ArrayList<>(preOrderedFleetMembers.keySet())) {
                FleetMemberAPI preOrderedMember = preOrderedFleetMembers.get(i);

                if (areSameVariant(preOrderedMember.getVariant(), orderedMember.getVariant())) {
                // if (isMatchVariantAndOfficer(preOrderedMember.getVariant(), orderedMember.getVariant(), preOrderedMember.getCaptain(), orderedMember.getCaptain())) {
                    newOrder.add(preOrderedFleetMembers.remove(i));
                    break;
                }
            }
        }

        if (!preOrderedFleetMembers.isEmpty()) {
            for (Map.Entry<Integer, FleetMemberAPI> entry : preOrderedFleetMembers.entrySet()) {
                int index = Math.min(entry.getKey(), newOrder.size());
                newOrder.add(index, entry.getValue());
            }
        }

        for (FleetMemberAPI member : newOrder) fleetData.addFleetMember(member);
        fleetData.setSyncNeeded();
        fleetData.syncIfNeeded();
     }

    // TODO Make D/SMOD Agnostic Settings, New Hulls? 
    // WHAT IF PLAYER WANTS VERY SPECIFIC DMOD/OFFICER VARIANTS?
    public static void restoreFleetFromPreset(String name) {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        MarketAPI market = getPlayerCurrentMarket();
        if (market == null) return;

        FleetPreset preset = getFleetPresets().get(name);
        if (preset == null) return;
        
        SubmarketAPI storage = CargoPresetUtils.getStorageSubmarket(market);
        if (storage == null) return;

        CargoAPI storageCargo = storage.getCargo();
        initMothballedShips(storageCargo);

        FleetDataAPI playerFleetData = playerFleet.getFleetData();
        List<FleetMemberAPI> playerFleetMembers = playerFleet.getFleetData().getMembersListCopy();

        // boolean isEqualizeCargo = (boolean)Global.getSector().getPersistentData().get(KEEPCARGORATIOS_KEY);
        // CargoAPI playerCargo = null;
        // CargoResourceRatios cargoRatios = null;
        // if (isEqualizeCargo) {
        //     playerCargo = playerFleet.getCargo();
        //     cargoRatios = new CargoResourceRatios(playerFleetMembers, playerCargo);
        // }
        
        FleetMemberAPI playerFleetMember = playerFleet.getFlagship();

        initMothballedShips(storageCargo);
        for (FleetMemberAPI member : playerFleetMembers) {
            member.setCaptain(null);
            playerFleetData.removeFleetMember(member);
            storageCargo.getMothballedShips().addFleetMember(member);
        }

        List<CampaignUIMessage> messageQueue = new ArrayList<>();
        List<FleetMemberAPI> membersDone = new ArrayList<>();
        boolean allFound = true;

        for (int i = 0; i < preset.getShipIds().size(); i++) {
            String hullId = preset.getShipIds().get(i);
            ShipVariantAPI variant = preset.getVariants().get(i);

            boolean found = false;
            for (FleetMemberAPI storedMember : storageCargo.getMothballedShips().getMembersListCopy()) {
                if (storedMember.getHullSpec().getBaseHullId().equals(hullId)) {
                    OfficerVariantPair pair = preset.getOfficersMap().get(i);

                    if (pair != null && areSameVariant(pair.getVariant(), storedMember.getVariant())) {
                        storageCargo.getMothballedShips().removeFleetMember(storedMember);
                        membersDone.add(storedMember);
                        storedMember.setCaptain(pair.getOfficer());

                        found = true;
                        break;
                    }
                    
                    if (areSameVariant(variant, storedMember.getVariant())) {
                        storageCargo.getMothballedShips().removeFleetMember(storedMember);
                        membersDone.add(storedMember);

                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                allFound = false;
                CampaignUIMessage msg = new CampaignUIMessage(RESTOREMESSAGE_FAIL_PREFIX + variant.getFullDesignationWithHullName() + RESTOREMESSAGE_FAIL_SUFFIX + name, Misc.getNegativeHighlightColor());

                if (!messageQueue.contains(msg)){
                    messageQueue.add(msg);
                }
            }
            
        }

        for (FleetMemberAPI member : membersDone) {
            playerFleetData.addFleetMember(member);
        }

        if (!isPlayerInFleet(playerFleetData.getMembersInPriorityOrder())) {
            initMothballedShips(storageCargo);
            for (FleetMemberAPI storageMember : storageCargo.getMothballedShips().getMembersListCopy()) {
                if (areSameVariant(storageMember.getVariant(), playerFleetMember.getVariant())) {
                    storageMember.setCaptain(Global.getSector().getPlayerPerson());
                    playerFleetData.addFleetMember(storageMember);
                }
            }
        }
        playerFleetData.setSyncNeeded();
        playerFleetData.syncIfNeeded();
        refreshFleetUI();

        // if (isEqualizeCargo) CargoPresetUtils.equalizeCargo(playerFleetData.getMembersListCopy(), playerCargo, storageCargo, cargoRatios);

        if (allFound) {
            CampaignUIMessage msg = new CampaignUIMessage(RESTOREMESSAGE_SUCCESS_PREFIX + name, Misc.getPositiveHighlightColor());
            if (messageQueue.contains(msg)) messageQueue.remove(msg);
            messageQueue.add(msg);
        }
        addMessagesToCampaignUI(messageQueue);
    }

    public static boolean isAutoUpdatePresets() {
        return (boolean) Global.getSector().getPersistentData().get(PresetUtils.IS_AUTO_UPDATE_KEY);
    }

    public static class CampaignUIMessage {
        private String message;
        private Color color;
    
        public CampaignUIMessage(String message, Color color) {
            this.message = message;
            this.color = color;
        }

        public String getMessage() {return this.message;}
        public Color getColor() {return this.color;}
    
        @Override
        public boolean equals(Object obj) {
            return (this.hashCode() == obj.hashCode());
        }
    
        @Override
        public int hashCode() {
            return Objects.hash(message);
        }
    }

    public static void addMessagesToCampaignUI(List<CampaignUIMessage> messageQueue) {
        CampaignUIMessage msg = messageQueue.remove(messageQueue.size()-1);
        Global.getSector().getCampaignUI().getMessageDisplay().addMessage(msg.getMessage(), msg.getColor());

        Global.getSector().addTransientScript(new BaseEveryFrameScript(true) {
            private IntervalUtil interval = new IntervalUtil(1.5f, 1.5f);

            @Override
            public void advance(float arg0) {
                interval.advance(arg0);
                if (interval.intervalElapsed()) {
                    if (messageQueue.isEmpty()) {
                        this.isDone = true;
                        Global.getSector().removeTransientScript(this);
                        return;
                    }
                    CampaignUIMessage msg = messageQueue.remove(messageQueue.size()-1);
                    Global.getSector().getCampaignUI().getMessageDisplay().addMessage(msg.getMessage(), msg.getColor());
                }
            }
        });
    }

    public static void suppressFleetPanelTooltips(UIPanelAPI fleetPanel) {
        UIPanelAPI list = utils.fleetPanelGetList(fleetPanel);

        Global.getSector().addTransientScript(new BaseEveryFrameScript(true) {
            private int frameCount = 0;

            @Override
            public void advance(float arg0) {
                for (UIComponentAPI item : utils.listPanelGetItems(list)) {
                    for (TreeNode node :  new TreeTraverser((UIPanelAPI)item).getNodes()) { // if we dont reinstantiate the traverser every frame then it doesnt work for some reason, maybe this could be fixed by using getChildrenNonCopy instead of getChildenCopy, but I don't feel like fixing this that isn't broken right now
                        for (Object child : node.getChildren()) {
                            if (UiUtil.uiComponentClass.isInstance(child)) {
                                Object tt = utils.getTooltip(child);
                                if (tt != null) {
                                    utils.hideTooltip(child, tt);
                                }
                            }
                        }
                    }
                }
                if (frameCount == 1) { // dont ask me why it needs to run for 2 frames for it to keep them down until they stay down, i wouldnt know
                    Global.getSector().removeTransientScript(this);
                    isDone = true;
                }
                frameCount++;
            }
        });
    }

    public static void refreshFleetUI() {
        UIPanelAPI fleetPanel = utils.fleetTabGetFleetPanel(FleetPresetManagerPlugin.fleetPanelInjector.getFleetTab());
        if (fleetPanel == null) return;
        utils.fleetPanelRecreateUI(fleetPanel, false);
        suppressFleetPanelTooltips(fleetPanel);
    }

    public static void deleteFleetPreset(String name) {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        if (mem != null && mem.get(UNDOCKED_PRESET_KEY) != null && ((FleetPreset)mem.get(UNDOCKED_PRESET_KEY)).getName().equals(name)) mem.unset(UNDOCKED_PRESET_KEY);
        getFleetPresets().remove(name);
    }

    public static void removeOfficerFromPresets(String officerId) {
        Map<String, FleetPreset> presets = getFleetPresets();

        for (FleetPreset fleetPreset : presets.values()) {
            for (Map.Entry<Integer, OfficerVariantPair> entry : new ArrayList<>(fleetPreset.getOfficersMap().entrySet())) {
                if (entry.getValue().getOfficer().getId().equals(officerId)) {
                    fleetPreset.getOfficersMap().remove(entry.getKey());
                }
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

    public static LinkedHashMap<String, FleetPreset> getFleetPresetsMapForTable(boolean ascendingNames, boolean ascendingShips) {
        Map<String, FleetPreset> presets = getFleetPresets();
        LinkedHashMap<String, FleetPreset> sortedMap = new LinkedHashMap<>();
        
        // HullSize[] shipOrder = ascendingShips ? SIZE_ORDER_ASCENDING : SIZE_ORDER_DESCENDING;
        // for (FleetPreset preset : presets.values()) {
        //     if (preset.getFleetMembers() != null) {
        //         // sortFleetMembers(preset.getFleetMembers(), shipOrder);
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