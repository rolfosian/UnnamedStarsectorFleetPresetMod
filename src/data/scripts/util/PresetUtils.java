package data.scripts.util;

import java.util.*;
import java.util.stream.Collectors;

import java.awt.Color;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.SubmarketPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.input.InputEventClass;
import com.fs.starfarer.api.input.InputEventType;

import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import com.fs.starfarer.api.loading.WeaponGroupSpec;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

import data.scripts.ClassRefs;
import data.scripts.listeners.DockingListener;
import data.scripts.ui.TreeTraverser;
import data.scripts.ui.UIPanel;
import data.scripts.ui.TreeTraverser.TreeNode;
import data.scripts.util.CargoPresetUtils.CargoResourceRatios;

@SuppressWarnings("unchecked")
public class PresetUtils {
    public static void print(Object... args) {
        PresetMiscUtils.print(args);
    }

    // Persistent data keys
    public static final String PRESETS_MEMORY_KEY = "$playerFleetPresets";
    public static final String IS_AUTO_UPDATE_KEY = "$isPresetAutoUpdate";
    public static final String PRESET_MEMBERS_KEY = "$fleetPresetMembers";
    public static final String STORED_PRESET_MEMBERIDS_KEY = "$storedFleetPresetMembers";
    public static final String KEEPCARGORATIOS_KEY = "$isPresetCargoRatios";

    // Non-persistent data keys
    public static final String FLEET_TAB_KEY = "$fleetCoreUiTabe";
    public static final String UNDOCKED_PRESET_KEY = "$presetUndocked";
    public static final String EXTRANEOUS_MEMBERS_KEY = "$extraneousPresetMembers";
    public static final String PLAYERCURRENTMARKET_KEY = "$playerCurrentMarket";
    public static final String COREUI_KEY = "$coreUI";
    public static final String ISPLAYERPAIDFORSTORAGE_KEY = "$isPlayerPaidForStorage";
    public static final String VISUALFLEETINFOPANEL_KEY = "$visualFleetInfoPanelClass";
    public static final String OFFICER_AUTOASSIGN_BUTTON_KEY = "$officerAutoAssignButton";
    
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

    public static class RunningMembers extends HashMap<FleetMemberAPI, PersonAPI> {
        public RunningMembers(List<FleetMemberAPI> fleetMembers) {
            for (FleetMemberAPI fleetMember : fleetMembers) {
                this.put(fleetMember, fleetMember.getCaptain());
            }
        }
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
        private ShipVariantAPI variant;
        private int index;
        private FleetPreset preset;

        public VariantWrapper(ShipVariantAPI variant, int index, FleetPreset preset) {
            this.variant = variant;
            this.index = index;
            this.preset = preset;
        }

        public ShipVariantAPI getVariant() {
            return this.variant;
        }

        public int getIndex() {
            return this.index;
        }

        public FleetPreset getPreset() {
            return this.preset;
        }

        public void updateVariant() {
            this.preset.updateVariant(this.index, this.variant);
        }
    }

    public static class FleetMemberWrapper {
        private final FleetPreset preset;
        private final FleetMemberAPI member;
        private final String id;

        private int index;
        private PersonAPI captainCopy;
        private String captainId;
        private FleetMemberAPI parentMember;

        public FleetMemberWrapper(FleetPreset preset, FleetMemberAPI member, ShipVariantAPI variant, PersonAPI captain, int index) {
            this.member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant);
            this.member.setShipName(member.getShipName());
            this.member.getRepairTracker().setCR(member.getRepairTracker().getCR());
            this.member.getStatus().setHullFraction(member.getStatus().getHullFraction());
            this.id = member.getId();

            if (!captain.isDefault()) {
                this.captainId = captain.getId();
                this.captainCopy =  createCaptainCopy(captain);
                this.member.setCaptain(captainCopy);

            } else {
                this.captainId = null;
                this.captainCopy = null;
            }

            this.index = index;
            this.parentMember = member;
            this.preset = preset;
        }

        public static PersonAPI createCaptainCopy(PersonAPI captain) {
            PersonAPI captainCopy = Global.getFactory().createPerson();
            captainCopy.setPersonality(captain.getPersonalityAPI().getId());
            captainCopy.setRankId(captain.getRankId());
            captainCopy.setFaction(Global.getSector().getPlayerFaction().getId());
            captainCopy.setStats(captain.getStats());
            captainCopy.setName(captain.getName());
            captainCopy.setPortraitSprite(captain.getPortraitSprite());

            if (captain.isAICore()) {
                captainCopy.setAICoreId(captain.getAICoreId());
            }
            return captainCopy;
        }

        /**not to be confused with updateOfficer */
        public void updateCaptain(PersonAPI captain) {
            if (captain == null || captain.isDefault()) {
                this.captainId = null;
                this.captainCopy = null;
                this.preset.getOfficersMap().remove(this.index);
                this.preset.getCampaignFleet().getFleetData().getMembersListCopy().get(this.index).setCaptain(null);
                this.member.setCaptain(null);
                return;
            }
            this.captainId = captain.getId();

            this.captainCopy = createCaptainCopy(captain);

            this.member.setCaptain(captainCopy);
            this.preset.getCampaignFleet().getFleetData().getMembersListCopy().get(this.index).setCaptain(captainCopy);

            if (captainId.equals(Global.getSector().getPlayerPerson().getId())) {
                this.preset.getCampaignFleet().setCommander(this.captainCopy);
                this.preset.getCampaignFleet().getFleetData().setFlagship(this.member);
            }

            for (FleetMemberWrapper member : this.preset.getFleetMembers()) {
                member.getMember().setFleetCommanderForStats(Global.getSector().getPlayerPerson(), null);
            }
        }

        public void removeFrompreset() {
            Global.getSector().getPlayerFleet().getFleetData().ensureHasFlagship();
            this.preset.rebuildPresetWithoutMember(this.id);
        }

        public FleetPreset getPreset() {
            return this.preset;
        }

        public int getIndex() {
            return this.index;
        }

        public String getId() {
            return this.id;
        }

        public FleetMemberAPI getMember() {
            return this.member;
        }

        public String getCaptainId() {
            return this.captainId;
        }

        public PersonAPI getCaptainCopy() {
            return this.captainCopy;
        }

        public FleetMemberAPI getParentMember() {
            return this.parentMember;
        }

        public void setIndex(int i) {
            this.index = i;
        }
    }

    public static CampaignFleetAPI createDummyPresetFleet() {
        CampaignFleetAPI campaignFleet = Global.getFactory().createEmptyFleet(Global.getSector().getPlayerFaction(), true);
        campaignFleet.setHidden(true);
        campaignFleet.setNoAutoDespawn(true);
        campaignFleet.setDoNotAdvanceAI(true);
        campaignFleet.setInflated(true);
        campaignFleet.setNoFactionInName(true);
        return campaignFleet;
    }

    public static CampaignFleetAPI createTempFleetCopy(List<FleetMemberAPI> members) {
        CampaignFleetAPI tempFleet = createDummyPresetFleet();

        for (int i = 0; i < members.size(); i++) {
            FleetMemberAPI member = members.get(i);

            FleetMemberWrapper wrapper = new FleetMemberWrapper(null, member, member.getVariant().clone(), member.getCaptain(), i);
            wrapper.getMember().setId(reverseMemberId(member));

            tempFleet.getFleetData().addFleetMember(wrapper.getMember());
            if (wrapper.getCaptainId() != null && wrapper.getCaptainId().equals(Global.getSector().getPlayerPerson().getId())) {
                tempFleet.setCommander(wrapper.getCaptainCopy());
                tempFleet.getFleetData().setFlagship(wrapper.getMember());
            }
        }
        tempFleet.getFleetData().setSyncNeeded();
        tempFleet.getFleetData().syncIfNeeded();

        for (FleetMemberAPI member : tempFleet.getFleetData().getMembersListCopy()) {
            member.setFleetCommanderForStats(Global.getSector().getPlayerPerson(), null);
        }

        return tempFleet;
    }

    // for after game save. i dont know why we need to do this but we do
    public static void updatePresetVariants() {
        for (FleetPreset preset : getFleetPresets().values()) {
            for (VariantWrapper variantWrapper : preset.getVariantWrappers().values()) {
                variantWrapper.updateVariant();
            }
        }
    }

    public static class FleetPreset {
        private final String name;
        private CampaignFleetAPI campaignFleet;

        private List<FleetMemberWrapper> fleetMembers = new ArrayList<>();
        private List<String> shipIds = new ArrayList<>(); // this is redundant since refactoring but i cant be btohered changing related logic

        private Map<Integer, ShipVariantAPI> variantsMap = new HashMap<>();
        private Map<Integer, OfficerVariantPair> officersMap = new HashMap<>(); // these are not copies, they are direct references to the officers including the player
        private Map<Integer, VariantWrapper> variantWrappers = new HashMap<>();

        public FleetPreset(String name, List<FleetMemberAPI> fleetMembers) {
            this.name = name;
            this.campaignFleet = createDummyPresetFleet();

            Map<String, List<FleetMemberWrapper>> presetsMembers = getFleetPresetsMembers();
            for (int i = 0; i < fleetMembers.size(); i++) {
                FleetMemberAPI member = fleetMembers.get(i);

                String hullId = member.getHullSpec().getBaseHullId();

                ShipVariantAPI variant = member.getVariant().clone();
                variantWrappers.put(i, new VariantWrapper(variant, i, this));

                this.shipIds.add(hullId);
                this.variantsMap.put(i, variant);
    
                PersonAPI captain = member.getCaptain();
                if (!captain.isDefault()) {
                    officersMap.put(i, new OfficerVariantPair(captain, variant, i));
                }

                FleetMemberWrapper wrappedMember = new FleetMemberWrapper(this, member, variant, captain, i);
                this.fleetMembers.add(wrappedMember);

                this.campaignFleet.getFleetData().addFleetMember(wrappedMember.getMember());
                if (wrappedMember.getCaptainId() != null && wrappedMember.getCaptainId().equals(Global.getSector().getPlayerPerson().getId())) {
                    this.campaignFleet.setCommander(wrappedMember.getCaptainCopy());
                    this.campaignFleet.getFleetData().setFlagship(wrappedMember.getMember());
                }

                if (presetsMembers.get(member.getId()) == null) presetsMembers.put(member.getId(), new ArrayList<>());
                presetsMembers.get(member.getId()).add(wrappedMember);
            }
            this.campaignFleet.forceSync();

            for (FleetMemberAPI member : this.campaignFleet.getFleetData().getMembersListCopy()) {
                member.setFleetCommanderForStats(Global.getSector().getPlayerPerson(), null);
            }
        }

        private void reorderCampaignFleet() {
            List<FleetMemberAPI> order = new ArrayList<>();
            for (FleetMemberWrapper member : this.fleetMembers) {
                order.add(member.getMember());
            }
            this.getCampaignFleet().getFleetData().sortToMatchOrder(order);
        }

        public void remakeCampaignFleet() {
            if (this.campaignFleet != null) {
                this.campaignFleet.despawn();
                this.campaignFleet = null;
            }
            this.campaignFleet = createDummyPresetFleet();

            for (FleetMemberWrapper member : this.fleetMembers) {
                if (member.getCaptainCopy() != null) member.getMember().setCaptain(member.getCaptainCopy());
                
                this.campaignFleet.getFleetData().addFleetMember(member.getMember());

                if (member.getCaptainId() != null && member.getCaptainId().equals(Global.getSector().getPlayerPerson().getId())) {
                    this.campaignFleet.setCommander(member.getCaptainCopy());
                    this.campaignFleet.getFleetData().setFlagship(member.getMember());
                }
            }

            for (FleetMemberWrapper member : this.fleetMembers) {
                member.getMember().setFleetCommanderForStats(Global.getSector().getPlayerPerson(), null);
            }
        }

        public void updateWrappedMember(int index, FleetMemberAPI newMember) {
            FleetMemberWrapper oldWrappedMember = this.fleetMembers.get(index);
            Map<String, List<FleetMemberWrapper>> presetsMembersMap = getFleetPresetsMembers();
            if (presetsMembersMap.get(newMember.getId()) == null) {
                presetsMembersMap.put(newMember.getId(), new ArrayList<>());
            }
            presetsMembersMap.get(oldWrappedMember.getId()).remove(oldWrappedMember);

            ShipVariantAPI newVariant = newMember.getVariant().clone();
            FleetMemberWrapper newWrappedMember = new FleetMemberWrapper(this, newMember, newVariant, newMember.getCaptain(), index);

            this.getCampaignFleet().getFleetData().removeFleetMember(this.fleetMembers.get(index).getMember());

            this.fleetMembers.set(index, newWrappedMember);
            this.shipIds.set(index, newMember.getVariant().getHullSpec().getBaseHullId());
            this.variantsMap.put(index, newVariant);
            this.variantWrappers.put(index, new VariantWrapper(newVariant, index, this));

            if (!newMember.getCaptain().isDefault()) this.officersMap.put(index, new OfficerVariantPair(newMember.getCaptain(), newVariant, index));

            this.getCampaignFleet().getFleetData().addFleetMember(this.fleetMembers.get(index).getMember());
            if (newWrappedMember.getCaptainId() != null && newWrappedMember.getCaptainId().equals(Global.getSector().getPlayerPerson().getId())) {

                this.getCampaignFleet().getFleetData().setFlagship(this.fleetMembers.get(index).getMember());
                this.getCampaignFleet().setCommander(this.fleetMembers.get(index).getMember().getCaptain());
            }

            for (FleetMemberWrapper member : this.fleetMembers) {
                member.getMember().setFleetCommanderForStats(Global.getSector().getPlayerPerson(), null);
            }

            presetsMembersMap.get(newMember.getId()).add(newWrappedMember);
            reorderCampaignFleet();
            oldWrappedMember = null;
        }

        public void updateVariant(int index, ShipVariantAPI variant) {
            this.fleetMembers.get(index).getMember().setVariant(variant, true, true);
            this.variantsMap.put(index, variant);
            this.variantWrappers.put(index, new VariantWrapper(variant, index, this));
            this.shipIds.set(index, variant.getHullSpec().getBaseHullId());

            for (int i = 0; i < this.shipIds.size(); i++) {
                OfficerVariantPair pair = this.officersMap.get(index);
                if (pair != null) {
                    pair.setVariant(variant);
                }
            }
        }

        public void updateOfficer(int index, PersonAPI captain) {
            FleetMemberWrapper member = this.fleetMembers.get(index);
            member.updateCaptain(captain);

            if (!captain.isDefault()) {
                this.officersMap.put(index, new OfficerVariantPair(captain, member.getMember().getVariant(), index));

                if (captain.isPlayer()) {
                    this.getCampaignFleet().setCommander(member.getCaptainCopy());
                    this.getCampaignFleet().getFleetData().setFlagship(member.getMember());

                    for (FleetMemberWrapper wrappedMember : this.fleetMembers) {
                        wrappedMember.getMember().setFleetCommanderForStats(Global.getSector().getPlayerPerson(), null);
                    }
                }
    
            } else {
                OfficerVariantPair pair = this.officersMap.get(index);
                if (pair != null) {
                    this.officersMap.remove(index);
                }
            }
        }

        public void rebuildPresetWithoutMember(String idOfmemberToLose) {
            List<FleetMemberWrapper> wrappedMembers = new ArrayList<>();
            Map<String, List<FleetMemberWrapper>> presetsMembers = getFleetPresetsMembers();

            int numMembers = this.fleetMembers.size();
            for (int i = 0; i < numMembers; i++) {
                FleetMemberWrapper wrappedMember = this.fleetMembers.get(i);

                List<FleetMemberWrapper> siblingMembers = presetsMembers.get(wrappedMember.getId());
                if (siblingMembers != null)  {
                    siblingMembers.remove(wrappedMember);
                }

                if (!wrappedMember.getId().equals(idOfmemberToLose)) {
                    wrappedMembers.add(wrappedMember);

                } else if (wrappedMember.getCaptainId() != null && wrappedMember.getCaptainId().equals(Global.getSector().getPlayerPerson().getId())) {
                    this.updateOfficer(i > 0 ? 0 : 1, Global.getSector().getPlayerPerson());
                }
            }

            this.fleetMembers.clear();
            this.shipIds.clear();
            this.variantsMap.clear();
            this.variantWrappers.clear();
            this.officersMap.clear();

            for (int i = 0; i < wrappedMembers.size(); i++) {
                FleetMemberWrapper wrappedMember = wrappedMembers.get(i);
                String hullId = wrappedMember.getMember().getHullSpec().getBaseHullId();
                wrappedMember.setIndex(i);

                if (presetsMembers.get(wrappedMember.getId()) == null) presetsMembers.put(wrappedMember.getId(), new ArrayList<>());
                presetsMembers.get(wrappedMember.getId()).add(wrappedMember);
                
                this.shipIds.add(hullId);
                this.fleetMembers.add(wrappedMember);

                this.variantsMap.put(i, wrappedMember.getMember().getVariant());
                this.variantWrappers.put(i, new VariantWrapper(wrappedMember.getMember().getVariant(), i, this));

                if (!wrappedMember.getParentMember().getCaptain().isDefault()) this.officersMap.put(i, new OfficerVariantPair(wrappedMember.getParentMember().getCaptain(), wrappedMember.getMember().getVariant(), i));
                
            }

            if (this.campaignFleet != null) {
                for (FleetMemberAPI member : this.campaignFleet.getFleetData().getMembersListCopy()) this.campaignFleet.getFleetData().removeFleetMember(member);
                this.campaignFleet.despawn();
                this.campaignFleet = null;
            }

            Global.getSector().getCampaignUI().addMessage("The fleet composition has changed and the " + this.name + " fleet preset has been updated. Reason: Ship Lost", Misc.getBasePlayerColor());
        }

        public String getName() {
            return this.name;
        }

        public List<String> getShipIds() {
            return this.shipIds;
        }

        public Map<Integer, ShipVariantAPI> getVariantsMap() {
            return this.variantsMap;
        }

        public Map<Integer, OfficerVariantPair> getOfficersMap() {
            return this.officersMap;
        }

        public List<FleetMemberWrapper> getFleetMembers() {
            return this.fleetMembers;
        }

        public CampaignFleetAPI getCampaignFleet() {
            if (this.campaignFleet == null) {
                this.campaignFleet = createDummyPresetFleet();

                for (FleetMemberWrapper member : this.fleetMembers) {
                    this.campaignFleet.getFleetData().addFleetMember(member.getMember());

                    if (member.getCaptainId() != null && member.getCaptainId().equals(Global.getSector().getPlayerPerson().getId())) {
                        this.campaignFleet.setCommander(member.getCaptainCopy());
                        this.campaignFleet.getFleetData().setFlagship(member.getMember());
                    }
                }

                for (FleetMemberWrapper member : this.fleetMembers) {
                    member.getMember().setFleetCommanderForStats(Global.getSector().getPlayerPerson(), null);
                }
            }
            return this.campaignFleet;
        }

        public void setCampaignFleet(CampaignFleetAPI newFleet) {
            if (this.campaignFleet != null) {
                this.campaignFleet.despawn();
                this.campaignFleet = null;
            }
            this.campaignFleet = newFleet;
        }

        public Map<Integer, VariantWrapper> getVariantWrappers() {
            return this.variantWrappers;
        }
    }

    // if preset member perished we need to remove it
    public static void cleanUpPerishedPresetMembers() {
        Collection<Set<String>> allStoredMembers = getStoredFleetPresetsMemberIds().values();
        Map<String, List<FleetMemberWrapper>> presetMembers = getFleetPresetsMembers();
    
        List<String> toRemoveKeys = new ArrayList<>();
        List<FleetMemberWrapper> toRemoveMembers = new ArrayList<>();
    
        for (Map.Entry<String, List<FleetMemberWrapper>> entry : presetMembers.entrySet()) {
            List<FleetMemberWrapper> wrappedMembers = entry.getValue();
            if (wrappedMembers.isEmpty()) {
                toRemoveKeys.add(entry.getKey());
                continue;
            }
            String memberId = entry.getKey();
    
            boolean isStored = false;
    
            for (Set<String> storedMembers : allStoredMembers) {
                if (storedMembers.contains(memberId)) {
                    isStored = true;
                    break;
                }
            }
    
            if (isMemberPerished(wrappedMembers.get(0).getParentMember(), isStored)) {
                toRemoveKeys.add(memberId);
                toRemoveMembers.addAll(wrappedMembers);
            }
        }
    
        for (FleetMemberWrapper wrappedMember : toRemoveMembers) {
            wrappedMember.removeFrompreset();
        }

        for (FleetMemberWrapper wrappedMember : toRemoveMembers) {
            List<FleetPreset> presets = getPresetsOfMembers(wrappedMember.getPreset().getCampaignFleet().getFleetData().getMembersListCopy());

            if (presets.size() > 1) {
                String toRemoveName = null;
                for (int i = 0; i < presets.size(); i++) {
                    toRemoveName = presets.get(i).getName();
                    if (getUndockedFleetPreset() != null && getUndockedFleetPreset().getName().equals(toRemoveName)) continue;
                    break;
                }

                deleteFleetPreset(toRemoveName);
                Global.getSector().getCampaignUI().addMessage("Perished member resulted in duplicate fleet preset: " + toRemoveName + ". It has been removed.", Misc.getNegativeHighlightColor());
            }
        }

        for (String key : toRemoveKeys) {
            presetMembers.remove(key);
        }
    }

    // note: this will return true if it's called after a player has picked up a fleet member with the mouse in the fleet tab of the coreui and before it is put back down again, so contingencies are required for that
    public static boolean isMemberPerished(FleetMemberAPI member, boolean isStored) {
        if (isStored) {
            return false;
        } else if (member.getFleetData() == null) {
            return true;
        } else if (member.getFleetData().getFleet() == null) {
            // edge case - initmothballedships was called on this member's market shortly before it was decivilized and its fleetData hasn't been nulled yet
            return true;
        }
        return !Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder().contains(member); // FleetData can still remain non-null for member in the case of recovery dialog opened and member not recovered
    }

    public static List<FleetMemberWrapper> getMemberCopiesFromPresets(FleetMemberAPI member) {
        List<FleetMemberWrapper> list = new ArrayList<>();

        for (FleetPreset preset : getFleetPresets().values()) {
            for (FleetMemberWrapper wrappedMember : preset.getFleetMembers()) {
                if (wrappedMember.getId().equals(member.getId())) {
                    list.add(wrappedMember);
                }
            }
        }

        return !list.isEmpty() ? list : null;
    }

    public static boolean isMemberFromAnyPreset(FleetMemberAPI member) {
        for (FleetPreset preset : getFleetPresets().values()) {
            for (FleetMemberWrapper wrappedMember : preset.getFleetMembers()) {
                if (wrappedMember.getParentMember().getId().equals(member.getId())) return true;
            }
        }
        return false;
    }

    public static Map<Integer, FleetMemberAPI> whichMembersAvailable(MarketAPI market, List<FleetMemberAPI> membersToCheck) {
        if (market == null) return whichMembersAvailable(membersToCheck);
        SubmarketAPI storage = CargoPresetUtils.getStorageSubmarket(market);
        if (!isPlayerPaidForStorage(storage.getPlugin())) return whichMembersAvailable(membersToCheck);
        
        CargoAPI storageCargo = storage.getCargo();
        initMothballedShips(storageCargo);

        Map<Integer, FleetMemberAPI> seen = new HashMap<>();

        Map<Integer, FleetMemberAPI> seenPlayer = new HashMap<>();
        Map<Integer, FleetMemberAPI> seenStorage = new HashMap<>();
        for (int i = 0; i < membersToCheck.size(); i++) {
            boolean seent = false;
            for (FleetMemberAPI playerMember : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
                if (!seenPlayer.values().contains(playerMember) && areSameVariant(membersToCheck.get(i).getVariant(), playerMember.getVariant())) {
                    seenPlayer.put(i, playerMember);
                    seent = true;
                    break;
                }
            }
            if (seent) continue;

            for (FleetMemberAPI storedMember : storageCargo.getMothballedShips().getMembersListCopy()) {
                if (!seenStorage.values().contains(storedMember) && areSameVariant(membersToCheck.get(i).getVariant(), storedMember.getVariant())) {
                    seenStorage.put(i, storedMember);
                    break;
                }
            }
        }

        seen.putAll(seenPlayer);
        seen.putAll(seenStorage);
        return seen;
    }

    public static Map<Integer, FleetMemberAPI> whichMembersAvailable(List<FleetMemberAPI> membersToCheck) {
        Map<Integer, FleetMemberAPI> seen = new HashMap<>();
        for (int i = 0; i < membersToCheck.size(); i++) {
            for (FleetMemberAPI playerMember : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
                if (!seen.values().contains(playerMember) && areSameVariant(membersToCheck.get(i).getVariant(), playerMember.getVariant())) {
                    seen.put(i, playerMember);
                    break;
                }
            }
        }
        return seen;
    }

    public static boolean isPlayerFleetChanged(FleetPreset preset, List<FleetMemberAPI> playerFleetMembers) {
        if (playerFleetMembers.size() != preset.getFleetMembers().size()) {
            return true;
        }
        
        for (int i = 0; i < preset.getFleetMembers().size(); i++) {
            FleetMemberAPI playerFleetMember = playerFleetMembers.get(i);
            FleetMemberWrapper member = preset.getFleetMembers().get(i);

            if (!areSameVariant(playerFleetMember.getVariant(), member.getMember().getVariant())
                || !isOfficerSameAsPresetMember(playerFleetMember, member) || playerFleetMember.getId() != member.getId()) {
                return true;
            }
        }
        return false;
    }
    
    public static Map<String, List<FleetMemberWrapper>> getFleetPresetsMembers() {
        return (Map<String, List<FleetMemberWrapper>>) Global.getSector().getPersistentData().get(PRESET_MEMBERS_KEY);
    }

    public static Map<String, Set<String>> getStoredFleetPresetsMemberIds() {
        return (Map<String, Set<String>>) Global.getSector().getPersistentData().get(STORED_PRESET_MEMBERIDS_KEY);
    }

    public static FleetPreset getUndockedFleetPreset() {
        return (FleetPreset) Global.getSector().getMemoryWithoutUpdate().get(UNDOCKED_PRESET_KEY);
    }

    public static void updateFleetPresetStats(List<FleetMemberAPI> playerFleet) {
        Map<String, List<FleetMemberWrapper>> presetMembers = getFleetPresetsMembers();
        if (presetMembers.isEmpty()) return;

        for (int i = 0; i < playerFleet.size(); i++) {
            FleetMemberAPI playerMember = playerFleet.get(i);
            List<FleetMemberWrapper> membersToUpdate = presetMembers.get(playerMember.getId());
            if (membersToUpdate == null) continue;

            float playerMemberCR = playerMember.getRepairTracker().getCR();
            float playerMemberHullFraction = playerMember.getStatus().getHullFraction();
            
            for (int j = 0; j < membersToUpdate.size(); j++) {
                FleetMemberWrapper memberToUpdate = membersToUpdate.get(j);

                float presetCR = memberToUpdate.getMember().getRepairTracker().getCR();
                float presetHullFraction = memberToUpdate.getMember().getStatus().getHullFraction();
                
                if (playerMemberCR != presetCR) {
                    memberToUpdate.getPreset().getCampaignFleet().getFleetData().getMembersListCopy().get(memberToUpdate.getIndex()).getRepairTracker().setCR(playerMemberCR);
                    memberToUpdate.getMember().getRepairTracker().setCR(playerMemberCR);
                }
                if (playerMemberHullFraction != presetHullFraction) {
                    memberToUpdate.getPreset().getCampaignFleet().getFleetData().getMembersListCopy().get(memberToUpdate.getIndex()).getStatus().setHullFraction(playerMemberHullFraction);
                    memberToUpdate.getMember().getStatus().setHullFraction(playerMemberHullFraction);
                }
            }
        }
    }

    public static void rebuildPreset(RunningMembers runningMembers, List<FleetMemberAPI> playerFleetMembers, FleetPreset preset) {
        // for some reason the officers arent updated immediately before the FleetMonitor calls this function if ships are scuttled (or just destroyed?) with officers in them so we have to do this
        List<PersonAPI> officersToReassign = new ArrayList<>();
        for (Map.Entry<FleetMemberAPI, PersonAPI> entry : runningMembers.entrySet()) {
            FleetMemberAPI member = entry.getKey();
            PersonAPI officer = entry.getValue();

            if (!officer.isDefault() && !playerFleetMembers.contains(member)) {
                officersToReassign.add(officer);
            }
        }

        preset.getFleetMembers().clear();

        preset.getShipIds().clear();
        preset.getVariantWrappers().clear();
        preset.getVariantsMap().clear();
        preset.getOfficersMap().clear();

        Map<String, List<FleetMemberWrapper>> presetsMembers = getFleetPresetsMembers();

        for (int i = 0; i < playerFleetMembers.size(); i++) {
            FleetMemberAPI member = playerFleetMembers.get(i);
            
            ShipVariantAPI variant = member.getVariant().clone();
            FleetMemberWrapper wrappedMember = new FleetMemberWrapper(preset, member, variant, member.getCaptain(), i);
            preset.getCampaignFleet().getFleetData().addFleetMember(wrappedMember.getMember());

            if (presetsMembers.get(member.getId()) == null) presetsMembers.put(member.getId(), new ArrayList<>());
            if (!presetsMembers.get(member.getId()).contains(wrappedMember)) presetsMembers.get(member.getId()).add(wrappedMember);

            List<FleetMemberWrapper> presetMembers = presetsMembers.get(member.getId());
            if (presetMembers != null) {
                for (int j = 0; j < presetMembers.size(); j++) {
                    FleetMemberWrapper presetMember = presetMembers.get(j);

                    if (preset.getName().equals(presetMember.getPreset().getName())) {
                        presetMembers.set(j, wrappedMember);
                    } else {
                        presetMember.getPreset().updateWrappedMember(presetMember.getIndex(), member);
                    }
                }
                
            } else {
                presetsMembers.put(member.getId(), new ArrayList<>());
                presetsMembers.get(member.getId()).add(wrappedMember);
            }
            preset.getFleetMembers().add(wrappedMember);
            
            preset.getShipIds().add(member.getHullSpec().getBaseHullId());
            preset.getVariantsMap().put(i, variant);
            preset.getVariantWrappers().put(i, new VariantWrapper(variant, i, preset));
        }

        for (FleetMemberAPI member : preset.getCampaignFleet().getFleetData().getMembersListCopy()) preset.getCampaignFleet().getFleetData().removeFleetMember(member);
        preset.getCampaignFleet().despawn();
        preset.setCampaignFleet(null);

        if (officersToReassign != null) {
            for (PersonAPI officer : officersToReassign) {
                for (int i = 0; i < playerFleetMembers.size(); i++) {
                    if (playerFleetMembers.get(i).getCaptain().isDefault()) {
                        playerFleetMembers.get(i).setCaptain(officer);
                        preset.getFleetMembers().get(i).updateCaptain(officer);
                        break;
                    }
                }
            }
        }
    }

    public static RunningMembers checkFleetAgainstPreset(RunningMembers runningMembers) {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        FleetPreset preset = (FleetPreset) mem.get(UNDOCKED_PRESET_KEY);
        if (preset == null) return new RunningMembers(Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy());

        boolean isAutoUpdate = isAutoUpdatePresets();

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        List<FleetMemberAPI> playerFleetMembers = playerFleet.getFleetData().getMembersListCopy();
        cleanUpPerishedPresetMembers();

        if (isAutoUpdate) {
            if (playerFleetMembers.size() != preset.getFleetMembers().size()) {
                String reason = playerFleetMembers.size() > preset.getFleetMembers().size() ? "Fleet Member(s) were gained" : "Fleet Member(s) were lost";
                rebuildPreset(runningMembers, playerFleetMembers, preset);

                Global.getSector().getCampaignUI().addMessage("The fleet composition has changed and the " + preset.getName() + " fleet preset has been updated. Reason: " + reason, Misc.getBasePlayerColor());

            } else {
                Set<String> reasons = new HashSet<>();

                for (FleetMemberWrapper memberWrapper : preset.getFleetMembers()) {
                    FleetMemberAPI playerFleetMember = playerFleetMembers.get(memberWrapper.getIndex());

                    if (!areSameVariant(playerFleetMember.getVariant(), memberWrapper.getMember().getVariant())) {
                        for (FleetMemberWrapper wrappedMember : getFleetPresetsMembers().get(memberWrapper.getId())) {
                            wrappedMember.getPreset().updateVariant(wrappedMember.getIndex(), playerFleetMember.getVariant());
                        }
                        reasons.add("Fleet Member ship variant(s) changed");
                    }

                    if (!playerFleetMember.getId().equals(memberWrapper.getId()) || !getFleetPresetsMembers().containsKey(memberWrapper.getId())) {
                        preset.updateWrappedMember(memberWrapper.getIndex(), playerFleetMember);
                        reasons.add("Fleet Member(s) changed");
                    }

                    if (!isOfficerSameAsPresetMember(playerFleetMember, memberWrapper)) {
                        preset.updateOfficer(memberWrapper.getIndex(), playerFleetMember.getCaptain());
                        reasons.add("Officer assignment(s) changed");
                    }
                }

                if (!reasons.isEmpty()) {
                    StringBuilder reason = new StringBuilder();
                    for (String reason_ : reasons) {
                        reason.append(reason_);
                        if (reason.length() != 0) {
                            reason.append(", ");
                        }
                    }
                    Global.getSector().getCampaignUI().addMessage("The fleet composition has changed and the " + preset.getName() + " fleet preset has been updated. Reason: " + reason, Misc.getBasePlayerColor());
                }
            }

        } else {
            if (isPlayerFleetChanged(preset, playerFleetMembers)) {
                Global.getSector().getCampaignUI().addMessage("The fleet composition has changed. Consider updating the " + preset.getName() + " fleet preset to match the current fleet.", Misc.getBasePlayerColor());
                mem.unset(UNDOCKED_PRESET_KEY);
            }
        }
        return new RunningMembers(Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy());
    }

    public static boolean isOfficerNought(PersonAPI officer) {
        if (officer == null) return true;
        return officer.getPortraitSprite().equals(OFFICER_NULL_PORTRAIT_PATH);
    }
    
    public static boolean isOfficerSameAsPresetMember(FleetMemberAPI playerFleetMember, PresetUtils.FleetMemberWrapper presetMember) {
        if (isOfficerNought(playerFleetMember.getCaptain()) && isOfficerNought(presetMember.getCaptainCopy())) {
            return true;
        }
        if (isOfficerNought(playerFleetMember.getCaptain()) || isOfficerNought(presetMember.getCaptainCopy())) {
            return false;
        }
        return playerFleetMember.getCaptain().getId().equals(presetMember.getCaptainId());
    }

    public static void initMothballedShips(CargoAPI storageCargo) {
        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            storageCargo.initMothballedShips(faction.getId());
        }
    }

    public static FleetPreset getPresetOfMembers(List<FleetMemberAPI> targetMembers) {
        Map<String, FleetPreset> presets = getFleetPresets();

        for (FleetPreset preset : presets.values()) {
            if (targetMembers.size() != preset.getShipIds().size()) {
                continue;
            }

            boolean allShipsMatched = true;

            for (int i = 0; i < targetMembers.size(); i++) {
                FleetMemberAPI playerMember = targetMembers.get(i);
                ShipVariantAPI variant = playerMember.getVariant();
                PersonAPI captain = playerMember.getCaptain();

                ShipVariantAPI presetVariant = preset.getVariantsMap().get(i);
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

                ShipVariantAPI presetVariant = preset.getVariantsMap().get(i);
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

            ShipVariantAPI presetVariant = preset.getVariantsMap().get(i);
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

            ShipVariantAPI presetVariant = preset.getVariantsMap().get(i);
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
            ShipVariantAPI presetVariant = preset.getVariantsMap().get(i);
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

                for (ShipVariantAPI presetVariant : preset.getVariantsMap().values()) {
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

            Collection<ShipVariantAPI> presetVariants = preset.getVariantsMap().values();
            if (presetVariants == null) continue;

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
        for (FleetMemberWrapper wrappedMember : preset.getFleetMembers()) {
            if (wrappedMember.getId().equals(member.getId())) {
                return true;
            }
        }
        return false;
    }

    public static Map<String, List<FleetMemberWrapper>> findNeededShipsWrappedNonIdMatching(FleetPreset preset, List<FleetMemberAPI> playerCurrentFleet) {
        Map<String, Integer> requiredShips = new HashMap<>();
        Map<String, List<FleetMemberWrapper>> foundShips = new HashMap<>();

        for (String hullId : preset.getShipIds()) {
            requiredShips.put(hullId, requiredShips.getOrDefault(hullId, 0) + 1);
        }

        if (playerCurrentFleet != null) {
            for (FleetMemberAPI member : playerCurrentFleet) {
                String hullId = member.getHullSpec().getBaseHullId();
                if (!requiredShips.containsKey(hullId)) continue;

                for (int i=0; i < preset.getVariantsMap().size(); i++) {
                    ShipVariantAPI presetVariant = preset.getVariantsMap().get(i);
                    if (areSameVariant(presetVariant, member.getVariant()) && preset.getFleetMembers().get(i).getId() != member.getId()) {
                        if (foundShips.get(hullId) == null) {
                            foundShips.put(hullId, new ArrayList<>());
                        }
                        foundShips.get(hullId).add(preset.getFleetMembers().get(i));
                        break;
                    }
                }
            }
        }

        Map<String, List<FleetMemberWrapper>> neededShips = new HashMap<>();
        for (Map.Entry<String, Integer> entry : requiredShips.entrySet()) {
            int needed = entry.getValue() - (foundShips.getOrDefault(entry.getKey(), new ArrayList<>()).size());
            if (needed > 0) {
                neededShips.put(entry.getKey(), foundShips.getOrDefault(entry.getKey(), new ArrayList<>()));
            }
        }

        return neededShips;
    }
    
    // should only be called if isPresetAvailableAtCurrentMarket returns true
    public static Map<FleetMemberWrapper, FleetMemberAPI> getIdAgnosticRequiredMembers(MarketAPI market, String presetName) {
        if (market == null) return null;
        if (CargoPresetUtils.getStorageSubmarket(market) == null) return null;

        FleetPreset preset = getFleetPresets().get(presetName);
        if (preset == null) return null;
        
        Map<String, List<FleetMemberWrapper>> neededShips = findNeededShipsWrappedNonIdMatching(preset, Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy());
        if (neededShips.isEmpty()) return null;

        CargoAPI storageCargo = CargoPresetUtils.getStorageSubmarket(market).getCargo();
        initMothballedShips(storageCargo);

        Map<FleetMemberWrapper, FleetMemberAPI> neededMembers = new HashMap<>();

        for (FleetMemberAPI storedMember : storageCargo.getMothballedShips().getMembersListCopy()) {
            if (neededShips.containsKey(storedMember.getHullSpec().getBaseHullId())) {
                if (!isMemberFromPreset(storedMember, preset)) {
                    for (FleetMemberWrapper member : neededShips.get(storedMember.getHullSpec().getBaseHullId())) {
                        if (areSameVariant(member.getMember().getVariant(), storedMember.getVariant())) {
                            neededMembers.put(member, storedMember);
                            neededShips.get(storedMember.getHullSpec().getBaseHullId()).remove(member);
                            break;
                        }
                    }
                    if (neededShips.get(storedMember.getHullSpec().getBaseHullId()).size() == 0) neededShips.remove(storedMember.getHullSpec().getBaseHullId());
                }

            }
        }
        return neededMembers.size() > 0 ? neededMembers : null;
    }

    public static boolean isMemberWrappedInPresets(FleetMemberAPI memberToCheck) {
        Map<String, List<FleetMemberWrapper>> presetMembers = getFleetPresetsMembers();
        if (presetMembers.get(memberToCheck.getId()) == null) return false;

        for (FleetMemberWrapper wrappedMember : presetMembers.get(memberToCheck.getId())) {
            if (wrappedMember.getId().equals(memberToCheck.getId())) return true;
        }
        return false;
    }

    public static CampaignFleetAPI mangleFleet(Map<FleetMemberWrapper, FleetMemberAPI> neededMembers, CampaignFleetAPI fleetToBeMangled) {
        CampaignFleetAPI mangledFleet = createDummyPresetFleet();
        
        List<FleetMemberAPI> members = fleetToBeMangled.getFleetData().getMembersListCopy();
        Map<Integer, FleetMemberAPI> indexedMembers = new HashMap<>();

        for (FleetMemberWrapper wrappedMember : neededMembers.keySet()) {
            for (int i = 0; i < members.size(); i++) {
                if (i == wrappedMember.getIndex()) {
                    indexedMembers.put(i, neededMembers.get(wrappedMember));
                    break;
                }
            }
        }

        for (int i = 0; i < members.size(); i++) {
            if (indexedMembers.containsKey(i)) {
                mangledFleet.getFleetData().addFleetMember(indexedMembers.get(i));
                FleetMemberAPI indexedMember = indexedMembers.get(i);

                if (indexedMember.getCaptain().getStats() == Global.getSector().getPlayerPerson().getStats()) {
                    mangledFleet.setCommander(indexedMember.getCaptain());
                    mangledFleet.getFleetData().setFlagship(indexedMember);
                }

            } else {
                FleetMemberAPI member = members.get(i);
                mangledFleet.getFleetData().addFleetMember(member);

                if (member.getCaptain().getStats() == Global.getSector().getPlayerPerson().getStats()) {
                    mangledFleet.setCommander(FleetMemberWrapper.createCaptainCopy(member.getCaptain()));
                    mangledFleet.getFleetData().setFlagship(member);
                }
            }
        }
        for (FleetMemberAPI member : mangledFleet.getFleetData().getMembersListCopy()) {
            member.setFleetCommanderForStats(Global.getSector().getPlayerPerson(), null);
        }
        return mangledFleet;
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
        CoreUIAPI coreUI = (CoreUIAPI) Global.getSector().getMemoryWithoutUpdate().get(COREUI_KEY);
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
        UtilReflection.clickButton(Global.getSector().getMemoryWithoutUpdate().get(OFFICER_AUTOASSIGN_BUTTON_KEY));
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

    public static void partRestorePreset(List<FleetMemberAPI> membersToRestore, Map<Integer, FleetMemberAPI> whichMembersAreAvailable, FleetPreset preset) {
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

        preset.getCampaignFleet().forceSync();
        sortToMatchOrder(playerFleetData, preset.getCampaignFleet().getFleetData().getMembersListCopy());
        updateFleetPresetStats(playerFleetData.getMembersListCopy());
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
        
        FleetMemberAPI playerFleetMember = getPlayerFleetMember(playerFleetData);

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
            ShipVariantAPI variant = preset.getVariantsMap().get(i);

            boolean found = false;
            for (FleetMemberAPI storedMember : storageCargo.getMothballedShips().getMembersListCopy()) {
                if (storedMember.getHullSpec().getBaseHullId().equals(hullId)) {
                    OfficerVariantPair pair = preset.getOfficersMap().get(i);

                    if (pair != null && areSameVariant(pair.getVariant(), storedMember.getVariant())) {
                        storageCargo.getMothballedShips().removeFleetMember(storedMember);
                        membersDone.add(storedMember);
                        storedMember.setCaptain(pair.getOfficer());

                        if (!storedMember.getId().equals(preset.getFleetMembers().get(i).getId())) {
                            preset.updateWrappedMember(i, storedMember);
                        }
                        found = true;
                        break;
                    }
                    
                    if (areSameVariant(variant, storedMember.getVariant())) {
                        storageCargo.getMothballedShips().removeFleetMember(storedMember);
                        membersDone.add(storedMember);

                        if (!storedMember.getId().equals(preset.getFleetMembers().get(i).getId())) {
                            preset.updateWrappedMember(i, storedMember);
                        }
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
        updateFleetPresetStats(playerFleetData.getMembersInPriorityOrder());
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

        Global.getSector().addTransientScript(new EveryFrameScript() {
            private boolean isDone = false;
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

            @Override
            public boolean isDone() {
                return this.isDone;
            }

            @Override
            public boolean runWhilePaused() {
                return true;
            }
            
        });
    }

    public static void suppressFleetPanelTooltips(Object fleetPanel) {
        Object list = ReflectionUtilis.invokeMethodDirectly(ClassRefs.fleetPanelGetListMethod, fleetPanel);
        List<UIPanelAPI> items = (List<UIPanelAPI>)  ReflectionUtilis.invokeMethodDirectly(ClassRefs.fleetPanelListGetItemsMethod, list);

        Global.getSector().addTransientScript(new EveryFrameScript() {
            private boolean isDone;
            private int frameCount = 0;

            @Override
            public void advance(float arg0) {
                for (UIPanelAPI item : items) {
                    for (TreeNode node :  new TreeTraverser(item).getNodes()) { // if we dont reinstantiate the traverser every frame then it doesnt work for some reason, maybe this could be fixed by using getChildrenNonCopy instead of getChildenCopy, but I don't feel like fixing this that isn't broken right now
                        for (Object child : node.getChildren()) {
                            if (ClassRefs.uiPanelSuperClass.isInstance(child)) {
                                Object tt = ReflectionUtilis.invokeMethodDirectly(ClassRefs.uiPanelGetTooltipMethod, child);
                                if (tt != null) {
                                    ReflectionUtilis.invokeMethodDirectly(ClassRefs.uiPanelHideTooltipMethod, child, tt);
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

            @Override
            public boolean isDone() {
                return isDone;
            }

            @Override
            public boolean runWhilePaused() {
                return true;
            }
        });
    }

    public static void refreshFleetUI() {
        Object fleetPanel = ReflectionUtilis.invokeMethodDirectly(ClassRefs.fleetTabGetFleetPanelMethod, Global.getSector().getMemoryWithoutUpdate().get(FLEET_TAB_KEY));
        if (fleetPanel == null) return;

        ReflectionUtilis.invokeMethodDirectly(ClassRefs.fleetPanelRecreateUIMethod, fleetPanel, false);
        suppressFleetPanelTooltips(fleetPanel); // WE FINALLY FIGURED IT OUT
    }

    public static boolean isMemberInAnyOtherPreset(String memberId, String nameOfPresetFrom) {
        for (FleetPreset preset : getFleetPresets().values()) {
            if (preset.getName().equals(nameOfPresetFrom)) continue;

            for (FleetMemberWrapper member : preset.getFleetMembers()) {
                if (member.getId().equals(memberId)) return true;
            }
        }
        return false;
    }

    public static void deleteFleetPreset(String name) {
        FleetPreset preset = getFleetPresets().get(name);
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        if (mem.get(UNDOCKED_PRESET_KEY) != null && ((FleetPreset)mem.get(UNDOCKED_PRESET_KEY)).getName().equals(name)) mem.unset(UNDOCKED_PRESET_KEY);

        Map<String, List<FleetMemberWrapper>> presetsMembersLists = getFleetPresetsMembers();
        for (FleetMemberWrapper member : preset.getFleetMembers()) {
            List<FleetMemberWrapper> presetMembers = presetsMembersLists.get(member.getId());
            if (presetMembers == null) continue;

            if (!isMemberInAnyOtherPreset(member.getId(), name)) {
                for (Set<String> storedMemberIds : getStoredFleetPresetsMemberIds().values()) {
                    storedMemberIds.remove(member.getId());
                }
            }

            presetMembers.remove(member);
            if (presetMembers.size() == 0) presetsMembersLists.remove(member.getId());
        }

        preset.getCampaignFleet().despawn();
        preset.setCampaignFleet(null);

        getFleetPresets().remove(name);
    }

    public static void removeOfficerFromPresets(String officerId) {
        Map<String, FleetPreset> presets = getFleetPresets();

        for (FleetPreset fleetPreset : presets.values()) {
            List<FleetMemberWrapper> wrappedMembers = fleetPreset.getFleetMembers();

            for (int i = 0; i < fleetPreset.getShipIds().size(); i++) {
                FleetMemberWrapper wrappedMember = wrappedMembers.get(i);

                if (officerId.equals(wrappedMember.getCaptainId())) {
                    wrappedMember.updateCaptain(null);
                    fleetPreset.getOfficersMap().remove(i);
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