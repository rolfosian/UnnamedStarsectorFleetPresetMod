package data.scripts.util;

import java.util.*;
import java.util.stream.Collectors;
import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
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
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;

import data.scripts.util.CargoPresetUtils.CargoResourceRatios;

@SuppressWarnings("unchecked")
public class PresetUtils {
    public static void print(Object... args) {
        PresetMiscUtils.print(args);
    }

    public static final String nexerelinVersion;
    public static final String RATVersion;
    
    static {

        ModSpecAPI nexerelinModSpec = Global.getSettings().getModManager().getModSpec("nexerelin");
        if (nexerelinModSpec != null) {
            nexerelinVersion = nexerelinModSpec.getVersion();
        } else {
            nexerelinVersion = null;
        }

        ModSpecAPI RATModSpec = Global.getSettings().getModManager().getModSpec("assortment_of_things");
        if (RATModSpec != null) {
            RATVersion = RATModSpec.getVersion();
        } else {
            RATVersion = null;
        }
        
    }

    // Persistent data keys
    public static final String PRESETS_MEMORY_KEY = "$playerFleetPresets";
    public static final String IS_AUTO_UPDATE_KEY = "$isPresetAutoUpdate";
    public static final String PRESET_MEMBERS_KEY = "$fleetPresetMembers";
    public static final String STORED_PRESET_MEMBERIDS_KEY = "$storedFleetPresetMembers";
    public static final String KEEPCARGORATIOS_KEY = "$isPresetCargoRatios";

    // Non-persistent data keys
    public static final String FLEETINFOPANEL_KEY = "$fleetInfoPanel";
    public static final String UNDOCKED_PRESET_KEY = "$presetUndocked";
    public static final String EXTRANEOUS_MEMBERS_KEY = "$extraneousPresetMembers";
    public static final String PLAYERCURRENTMARKET_KEY = "$playerCurrentMarket";
    public static final String COREUI_KEY = "$coreUI";
    public static final String ISPLAYERPAIDFORSTORAGE_KEY = "$isPlayerPaidForStorage";
    public static final String MESSAGEQUEUE_KEY = "$presetsMessageQueue";
    public static final String VISUALFLEETINFOPANEL_KEY = "$visualFleetInfoPanelClass";
    public static final String OFFICER_AUTOASSIGN_BUTTON_KEY = "$officerAutoAssignButton";
    
    public static final String RESTOREMESSAGE_SUCCESS_PREFIX = "Successfully restored fleet preset: ";
    public static final String RESTOREMESSAGE_FAIL_PREFIX = "Could not find one or more of ";
    public static final String RESTOREMESSAGE_FAIL_SUFFIX = " in storage to load for preset: ";
    public static final String OFFICER_NULL_PORTRAIT_PATH = "graphics/portraits/portrait_generic_grayscale.png";

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

    public static String[] getFleetType(List<FleetMemberAPI> fleetMembers) {
        int carriers = 0;
        int combats = 0;
        int civilians = 0; 
        int explorationShips = 0;

        for (FleetMemberAPI member : fleetMembers) {
            if (member.getVariant().getHullSpec().isCarrier()) {
                carriers++;
            } else if (member.getVariant().getHullSpec().isCivilianNonCarrier()) {
                civilians++;
            } else if (!member.getVariant().getHullSpec().isCivilianNonCarrier()) {
                combats++;
            }

            if (member.getVariant().hasHullMod("hiressensors") || member.getVariant().hasHullMod("surveying_equipment")) {
                explorationShips++;
            }
        }
        int max = Math.max(Math.max(carriers, combats), Math.max(civilians, explorationShips));

        String type = "Mixed Fleet";
        if (max == explorationShips) type = "Exploration";
        if (max == carriers) type = "Carrier";
        if (max == combats) type = "Combat";
        if (max == civilians) type = "Salvage/Trade";

        String iconPath = "graphics/icons/skills/leadership2.png";
        if (max == explorationShips) iconPath = "graphics/icons/skills/sensors.png";
        if (max == carriers) iconPath = "graphics/icons/skills/carrier_command.png";
        if (max == combats) iconPath = "graphics/icons/skills/strike_commander.png";
        if (max == civilians) iconPath = "graphics/icons/skills/industrial_planning.png";

        return new String[] {iconPath, type};
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

        public PersonAPI getOfficer() {return this.officer;}
        public ShipVariantAPI getVariant() {return this.variant;}
        public int getIndex() {return this.index;}

        public void setVariant(ShipVariantAPI newVariant) {this.variant = newVariant;}
    }

    public static class VariantWrapper {
        private ShipVariantAPI variant;
        private int index;
        private FleetPreset preset;

        public ShipVariantAPI getVariant() {return this.variant;}
        public int getIndex() {return this.index;}
        public FleetPreset getPreset() {return this.preset;}

        public VariantWrapper(ShipVariantAPI variant, int index, FleetPreset preset) {
            this.variant = variant;
            this.index = index;
            this.preset = preset;
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

        public FleetMemberWrapper(FleetPreset preset, FleetMemberAPI member, PersonAPI captain, int index) {
            this.member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, member.getVariant());
            this.member.setShipName(member.getShipName());
            this.member.getRepairTracker().setCR(member.getRepairTracker().getCR());
            this.member.getStatus().setHullFraction(member.getStatus().getHullFraction());
            this.id = member.getId();
            this.member.setId(member.getId());

            if (captain != null) {
                this.captainId = captain.getId();

                this.captainCopy =  Global.getFactory().createPerson();
                captainCopy.setPortraitSprite(captain.getPortraitSprite());
                captainCopy.setFaction(Global.getSector().getPlayerFaction().getId());
                captainCopy.setStats(captain.getStats());
                captainCopy.setName(captain.getName());
                captainCopy.setId(captain.getId());
                this.member.setCaptain(captainCopy);

            } else {
                this.captainId = null;
            }

            this.index = index;
            this.parentMember = member;
            this.preset = preset;
        }

        public void updateCaptain(PersonAPI captain) {
            if (captain == null) {
                this.captainId = null;
                this.captainCopy = null;
                this.preset.getCampaignFleet().getFleetData().getMembersInPriorityOrder().get(this.index).setCaptain(null);
                this.member.setCaptain(null);
                return;
            }

            // this.captain = captain;
            this.captainId = captain.getId();

            this.captainCopy = Global.getFactory().createPerson();
            this.captainCopy.setPersonality(captain.getPersonalityAPI().getId());
            this.captainCopy.setRankId(captain.getRankId());
            this.captainCopy.setFaction(Global.getSector().getPlayerFaction().getId());
            this.captainCopy.getStats().setLevel(captain.getStats().getLevel());
            this.captainCopy.setName(captain.getName());
            this.captainCopy.setPortraitSprite(captain.getPortraitSprite());
            this.captainCopy.setId(captain.getId());
            if (captain.isAICore()) {
                this.captainCopy.setAICoreId(captain.getAICoreId());
            }

            this.member.setCaptain(captainCopy);
            this.preset.getCampaignFleet().getFleetData().getMembersInPriorityOrder().get(this.index).setCaptain(captainCopy);

            if (captainId.equals(Global.getSector().getPlayerPerson().getId())) {
                this.preset.getCampaignFleet().setCommander(this.captainCopy);
                this.preset.getCampaignFleet().getFleetData().setFlagship(this.member);
            }
        }

        // i dont remember why i made this and where it was used
        public void removeFrompreset() {
            this.preset.getCampaignFleet().getFleetData().removeFleetMember(this.member);
            this.preset.getShipIds().remove(this.index);
            this.preset.getVariantsMap().remove(this.index);
            this.preset.getVariantWrappers().remove(this.index);
            this.preset.getFleetMembers().remove(this.index);
            this.preset.getOfficersMap().remove(this.index);
            this.preset.updateIndexesAfterMemberRemoved(index);
            // this.preset.rebuildPresetAfterMemberRemoved();

            this.captainCopy = null;
        }

        public FleetPreset getPreset() {return this.preset;}
        public int getIndex() {return this.index;}
        public String getId() {return this.id;}
        public FleetMemberAPI getMember() {return this.member;}
        public String getCaptainId() {return this.captainId;}
        public PersonAPI getCaptainCopy() {return this.captainCopy;}
        public FleetMemberAPI getParentMember() {return this.parentMember;}

        public void setIndex(int i) {this.index = i;}
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
        private List<String> shipIds = new ArrayList<>(); // this is redundant since refactoring but i cant be btohered changing related logic
        private Map<Integer, ShipVariantAPI> variantsMap = new HashMap<>();
        private Map<Integer, OfficerVariantPair> officersMap = new HashMap<>();
        private List<FleetMemberWrapper> fleetMembers = new ArrayList<>();
        private CampaignFleetAPI campaignFleet;
        private Map<Integer, VariantWrapper> variantWrappers = new HashMap<>();

        public FleetPreset(String name, List<FleetMemberAPI> fleetMembers) {
            this.name = name;
            this.campaignFleet = Global.getFactory().createEmptyFleet(Global.getSector().getPlayerFaction(), true);
            this.campaignFleet.setHidden(true);
            this.campaignFleet.setNoAutoDespawn(true);
            this.campaignFleet.setDoNotAdvanceAI(true);
            this.campaignFleet.setInflated(true);
            this.campaignFleet.setNoFactionInName(true);

            Map<String, List<FleetMemberWrapper>> presetsMembers = getFleetPresetsMembers();
            for (int i = 0; i < fleetMembers.size(); i++) {
                FleetMemberAPI member = fleetMembers.get(i);

                String hullId = member.getHullId();

                ShipVariantAPI variant = member.getVariant();
                variantWrappers.put(i, new VariantWrapper(variant, i, this));

                this.shipIds.add(hullId);
                this.variantsMap.put(i, variant);
    
                PersonAPI captain = member.getCaptain();
                if (captain != null) {
                    officersMap.put(i, new OfficerVariantPair(captain, variant, i));
                }

                FleetMemberWrapper wrappedMember = new FleetMemberWrapper(this, member, captain, i);
                this.fleetMembers.add(wrappedMember);

                this.campaignFleet.getFleetData().addFleetMember(wrappedMember.getMember());
                if (wrappedMember.getCaptainId().equals(Global.getSector().getPlayerPerson().getId())) {
                    this.campaignFleet.setCommander(wrappedMember.getCaptainCopy());
                    this.campaignFleet.getFleetData().setFlagship(wrappedMember.getMember());
                }

                if (presetsMembers.get(member.getId()) == null) getFleetPresetsMembers().put(member.getId(), new ArrayList<>());
                presetsMembers.get(member.getId()).add(wrappedMember);
            }
        }

        private void reorderCampaignFleet() {
            List<FleetMemberAPI> order = new ArrayList<>();
            for (FleetMemberWrapper member : this.fleetMembers) {
                order.add(member.getMember());
            }
            this.campaignFleet.getFleetData().sortToMatchOrder(order);
        }

        public void updateWrappedMember(int index, FleetMemberAPI newMember) {
            FleetMemberWrapper oldWrappedMember = this.fleetMembers.get(index);
            Map<String, List<FleetMemberWrapper>> presetsMembersMap = getFleetPresetsMembers();
            if (presetsMembersMap.get(newMember.getId()) == null) {
                presetsMembersMap.put(newMember.getId(), new ArrayList<>());
            } else {
                presetsMembersMap.get(newMember.getId()).remove(oldWrappedMember);
            }

            FleetMemberWrapper newWrappedMember = new FleetMemberWrapper(this, newMember, newMember.getCaptain(), index);
            this.campaignFleet.getFleetData().removeFleetMember(this.fleetMembers.get(index).getMember());
            this.fleetMembers.set(index, newWrappedMember);
            this.variantsMap.put(index, newMember.getVariant());
            this.variantWrappers.put(index, new VariantWrapper(newMember.getVariant(), index, this));
            this.officersMap.put(index, new OfficerVariantPair(newMember.getCaptain(), newMember.getVariant(), index));

            this.campaignFleet.getFleetData().addFleetMember(this.fleetMembers.get(index).getMember());
            if (newWrappedMember.getCaptainId().equals(Global.getSector().getPlayerPerson().getId())) {
                this.campaignFleet.getFleetData().setFlagship(this.fleetMembers.get(index).getMember());
                this.campaignFleet.setCommander(this.fleetMembers.get(index).getMember().getCaptain());
            }

            presetsMembersMap.get(newMember.getId()).add(newWrappedMember);
            reorderCampaignFleet();
            oldWrappedMember = null;
        }

        // i dont remember why i made this and where it was used
        public void updateIndexesAfterMemberRemoved(int removedIndex) {
            this.shipIds.remove(removedIndex);
            this.variantsMap.remove(removedIndex);
            this.variantWrappers.remove(removedIndex);
            this.officersMap.remove(removedIndex);
        
            Map<Integer, ShipVariantAPI> updatedVariantsMap = new HashMap<>();
            for (Map.Entry<Integer, ShipVariantAPI> entry : this.variantsMap.entrySet()) {
                int index = entry.getKey();
                updatedVariantsMap.put(index < removedIndex ? index : index - 1, entry.getValue());
            }
            this.variantsMap = updatedVariantsMap;

            Map<Integer, VariantWrapper> updatedVariantWrappersMap = new HashMap<>();
            for (Map.Entry<Integer, VariantWrapper> entry : this.variantWrappers.entrySet()) {
                int index = entry.getKey();
                updatedVariantWrappersMap.put(index < removedIndex ? index : index - 1, entry.getValue());
            }
            this.variantsMap = updatedVariantsMap;
        
        
            Map<Integer, OfficerVariantPair> updatedOfficersMap = new HashMap<>();
            for (Map.Entry<Integer, OfficerVariantPair> entry : this.officersMap.entrySet()) {
                int index = entry.getKey();
                OfficerVariantPair pair = entry.getValue();
                updatedOfficersMap.put(index < removedIndex ? index : index - 1, new OfficerVariantPair(pair.getOfficer(), pair.getVariant(), index < removedIndex ? index : index - 1));
            }
            this.officersMap = updatedOfficersMap;
        
            for (int i = 0; i < this.fleetMembers.size(); i++) {
                this.fleetMembers.get(i).setIndex(i);
            }
            reorderCampaignFleet();
        }

        // i dont remember why i made this and where it was used
        public void rebuildPresetAfterMemberRemoved() {
            List<FleetMemberWrapper> wrappedMembers = new ArrayList<>();

            for (FleetMemberWrapper wrappedMember : this.fleetMembers) {
                wrappedMembers.add(wrappedMember);
            }

            this.fleetMembers.clear();
            this.shipIds.clear();
            this.variantsMap.clear();
            this.variantWrappers.clear();
            this.officersMap.clear();

            for (int i = 0; i < wrappedMembers.size(); i++) {
                FleetMemberWrapper member = wrappedMembers.get(i);
                String hullId = member.getMember().getHullId();
                
                FleetMemberWrapper wrappedMember = new FleetMemberWrapper(this, member.getMember(), member.getMember().getCaptain(), i);
                this.campaignFleet.getFleetData().addFleetMember(wrappedMember.getMember());
                
                this.fleetMembers.add(wrappedMember);
                
                this.shipIds.add(hullId);
                this.variantsMap.put(i, member.getMember().getVariant());
                this.variantWrappers.put(i, new VariantWrapper(member.getMember().getVariant(), i, this));

                
                if (!isOfficerNought(member.getMember().getCaptain())) {
                    this.officersMap.put(i, new OfficerVariantPair(member.getMember().getCaptain(), member.getMember().getVariant(), i));
                    if (Global.getSector().getPlayerPerson().getId().equals(member.getCaptainId())) {
                        this.campaignFleet.setCommander(wrappedMember.getMember().getCaptain());
                        this.campaignFleet.getFleetData().setFlagship(wrappedMember.getMember());
                    }
                }
            }
            Global.getSector().getCampaignUI().addMessage("The fleet composition has changed and the " + this.name + " fleet preset has been updated.", Misc.getBasePlayerColor());
        }

        public void updateVariant(int index, ShipVariantAPI variant) {
            this.fleetMembers.get(index).getMember().setVariant(variant, true, true);
            this.variantsMap.put(index, variant);
            this.variantWrappers.put(index, new VariantWrapper(variant, index, this));
            this.shipIds.set(index, variant.getHullSpec().getHullId());

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
            if (captain.isPlayer()) {
                this.campaignFleet.setCommander(member.getCaptainCopy());
                this.campaignFleet.getFleetData().setFlagship(member.getMember());
            }

            if (!captain.getName().getFullName().equals("")) {
                this.officersMap.put(index, new OfficerVariantPair(captain, member.getMember().getVariant(), index));
            } else {
                OfficerVariantPair pair = this.officersMap.get(index);
                if (pair != null) {
                    this.officersMap.remove(index);
                }
            }
        }

        public String getName() {return this.name;}
        public List<String> getShipIds() {return this.shipIds;}
        public Map<Integer, ShipVariantAPI> getVariantsMap() {return this.variantsMap;}
        public Map<Integer, OfficerVariantPair> getOfficersMap() {return this.officersMap;}
        public List<FleetMemberWrapper> getFleetMembers() {return this.fleetMembers;}
        public CampaignFleetAPI getCampaignFleet() {return this.campaignFleet;}
        public Map<Integer, VariantWrapper> getVariantWrappers() {return this.variantWrappers;}

        public void setCampaignFleet(CampaignFleetAPI newFleet) {this.campaignFleet = newFleet;}
    }

    // if preset member perished while preset was not active or auto update was disabled we need to remove it from the cache to save a few kb of memory xd
    public static void cleanUpPerishedPresetMembers() {
        Collection<Set<String>> allStoredMembers = getStoredFleetPresetsMemberIds().values();
        Map<String, List<FleetMemberWrapper>> presetMembers = getFleetPresetsMembers();
    
        Set<String> toRemove = new HashSet<>();
        for (Map.Entry<String, List<FleetMemberWrapper>> entry : presetMembers.entrySet()) {
            List<FleetMemberWrapper> wrappedMembers = entry.getValue();
    
            for (FleetMemberWrapper wrappedMember : wrappedMembers) {
                boolean isStored = false;
    
                for (Set<String> storedMembers : allStoredMembers) {
                    if (storedMembers.contains(wrappedMember.getParentMember().getId())) {
                        isStored = true;
                        break;
                    }
                }
                if (isPresetMemberPerished(wrappedMember.getParentMember(), isStored)) {
                    toRemove.add(wrappedMember.getMember().getId());
                    break;
                }
            }
        }
    
        for (String id : toRemove) {
            presetMembers.remove(id);
        }
    }

    public static boolean isPresetMemberPerished(FleetMemberAPI member, boolean isStored) {
        if (isStored) {
            return false;
        } else if (member.getFleetData() == null) {
            return true;
        } else if (member.getFleetData().getFleet() == null) {
            // edge case - initmothballedships was called on this member's  market shortly before it was decivilized and it hasn't been nulled yet
            return true;
        }
        return false;
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

    public static boolean isMemberinFleet(CampaignFleetAPI targetFleet, FleetMemberAPI fleetMember) {
        for (FleetMemberAPI member : targetFleet.getFleetData().getMembersInPriorityOrder()) {
            if (member.getId().equals(fleetMember.getId())) return true;
        }
        return false;
    }

    public static void updateFleetPresetStats(List<FleetMemberAPI> playerFleet) {
        Map<String, List<FleetMemberWrapper>> presetMembers = getFleetPresetsMembers();
        if (presetMembers.size() == 0) return;

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
                    memberToUpdate.getPreset().getCampaignFleet().getFleetData().getMembersInPriorityOrder().get(memberToUpdate.getIndex()).getRepairTracker().setCR(playerMemberCR);
                    memberToUpdate.getMember().getRepairTracker().setCR(playerMemberCR);
                }
                if (playerMemberHullFraction != presetHullFraction) {
                    memberToUpdate.getPreset().getCampaignFleet().getFleetData().getMembersInPriorityOrder().get(memberToUpdate.getIndex()).getStatus().setHullFraction(playerMemberHullFraction);
                    memberToUpdate.getMember().getStatus().setHullFraction(playerMemberHullFraction);
                }

                if (!isOfficerNought(memberToUpdate.getCaptainCopy())) {
                    PersonAPI fleetMemberCaptain = memberToUpdate.getPreset().getCampaignFleet().getFleetData().getMembersInPriorityOrder().get(memberToUpdate.getIndex()).getCaptain();
                    if (fleetMemberCaptain.getStats().getLevel() != playerMember.getCaptain().getStats().getLevel()) {
                        memberToUpdate.getCaptainCopy().setStats(playerMember.getCaptain().getStats());
                        fleetMemberCaptain.setStats(playerMember.getCaptain().getStats());
                    }
                }
            }
        }
    }

    public static RunningMembers checkFleetAgainstPreset(RunningMembers runningMembers) {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        FleetPreset preset = (FleetPreset) mem.get(UNDOCKED_PRESET_KEY);
        if (preset == null) return new RunningMembers(Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder());

        boolean isAutoUpdate = (boolean) Global.getSector().getPersistentData().get(IS_AUTO_UPDATE_KEY);

        Set<String> reasons;

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        List<FleetMemberAPI> playerFleetMembers = playerFleet.getFleetData().getMembersInPriorityOrder();

        if (isAutoUpdate) {
            if (playerFleetMembers.size() != preset.getFleetMembers().size()) {
                String reason;
                if (playerFleetMembers.size() > preset.getFleetMembers().size()) {
                    reason =  "Fleet Member(s) were gained";
                } else {
                    reason = "Fleet Member(s) were lost";
                }

                // for some reason the officers arent updated immediately before the FleetMonitor calls this function if ships are scuttled (or just destroyed?) with officers in them so we have to do THIS FUCKING BULLSHIT
                List<PersonAPI> officersToReassign = new ArrayList<>();
                for (Map.Entry<FleetMemberAPI, PersonAPI> entry : runningMembers.entrySet()) {
                    FleetMemberAPI member = entry.getKey();
                    PersonAPI officer = entry.getValue();

                    if (!isOfficerNought(officer) && !playerFleetMembers.contains(member)) {
                        officersToReassign.add(officer);
                    }
                }

                if (preset.getCampaignFleet() == null) preset.setCampaignFleet(createDummyPresetFleet());
                for (FleetMemberWrapper wrappedMember : preset.getFleetMembers()) {
                    preset.getCampaignFleet().getFleetData().removeFleetMember(wrappedMember.getMember());
                }
                preset.getFleetMembers().clear();

                preset.getShipIds().clear();
                preset.getVariantWrappers().clear();
                preset.getVariantsMap().clear();
                preset.getOfficersMap().clear();

                for (int i = 0; i < playerFleetMembers.size(); i++) {
                    FleetMemberAPI member = playerFleetMembers.get(i);
                    
                    FleetMemberWrapper wrappedMember = new FleetMemberWrapper(preset, member, member.getCaptain(), i);
                    preset.getCampaignFleet().getFleetData().addFleetMember(wrappedMember.getMember());

                    List<FleetMemberWrapper> presetMembers = getFleetPresetsMembers().get(member.getId());
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
                        getFleetPresetsMembers().put(member.getId(), new ArrayList<>());
                        getFleetPresetsMembers().get(member.getId()).add(wrappedMember);
                    }
                    preset.getFleetMembers().add(wrappedMember);
                    
                    preset.getShipIds().add(member.getHullId());
                    preset.getVariantsMap().put(i, member.getVariant());
                    preset.getVariantWrappers().put(i, new VariantWrapper(member.getVariant(), i, preset));
                    
                    if (!isOfficerNought(member.getCaptain())) {
                        preset.getOfficersMap().put(i, new OfficerVariantPair(member.getCaptain(), member.getVariant(), i));

                        if (wrappedMember.getCaptainId().equals(Global.getSector().getPlayerPerson().getId())) {
                            preset.getCampaignFleet().setCommander(wrappedMember.getMember().getCaptain());
                            preset.getCampaignFleet().getFleetData().setFlagship(wrappedMember.getMember());
                        }
                    }
                }

                if (officersToReassign != null) {
                    for (PersonAPI officer : officersToReassign) {
                        for (int i = 0; i < playerFleetMembers.size(); i++) {
                            if (isOfficerNought(playerFleetMembers.get(i).getCaptain())) {
                                playerFleetMembers.get(i).setCaptain(officer);
                                preset.getFleetMembers().get(i).updateCaptain(officer);
                                break;
                            }
                        }
                    }
                }
                Global.getSector().getCampaignUI().addMessage("The fleet composition has changed and the " + preset.getName() + " fleet preset has been updated. Reason: " + reason, Misc.getBasePlayerColor());

            } else {
                reasons = new HashSet<>();

                for (FleetMemberWrapper member : preset.getFleetMembers()) {
                    FleetMemberAPI playerFleetMember = playerFleetMembers.get(member.getIndex());

                    if (!areSameVariant(playerFleetMember.getVariant(), member.getMember().getVariant())) {
                        preset.updateVariant(member.getIndex(), playerFleetMember.getVariant());
                        reasons.add("Fleet Member ship variant(s) changed");
                    }

                    if (!playerFleetMember.getId().equals(member.getId()) || !getFleetPresetsMembers().containsKey(member.getId())) {
                        preset.updateWrappedMember(member.getIndex(), playerFleetMember);
                        reasons.add("Fleet Member ID(s) changed");
                    }

                    if (!isOfficerSameAsPresetMember(playerFleetMember, member)) {
                        preset.updateOfficer(member.getIndex(), playerFleetMember.getCaptain());
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
        return new RunningMembers(Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder());
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
            // i dont know what this does but the javadocs say to do it before calling getmothballedships and i think it stopped some crashing?
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
                    if (!isOfficerNought(captain) && preset.getOfficersMap().containsKey(i)) {
                        OfficerVariantPair pair = preset.getOfficersMap().get(i);
                        boolean officerMatched = false;

                        if (areSameVariant(pair.getVariant(), variant) && (captain != null && pair.getOfficer().getId().equals(captain.getId()))) officerMatched = true;

                        if (!officerMatched) {
                            allShipsMatched = false;
                            break;
                        }
                    }
                    variantMatched = true;
                    break;
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
        if (playerFleetMembers.size() != preset.getShipIds().size()) {
            return false;
        }

        boolean allShipsMatched = true;
        for (int i = 0; i < playerFleetMembers.size(); i++) {
            FleetMemberAPI member = playerFleetMembers.get(i);
            String hullId = member.getHullId();
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
                if (!isOfficerNought(captain) && preset.getOfficersMap().containsKey(i)) {
                    OfficerVariantPair pair = preset.getOfficersMap().get(i);
                    boolean officerMatched = false;

                    if (areSameVariant(pair.getVariant(), variant) && (captain != null && pair.getOfficer().getId().equals(captain.getId()))) {
                        officerMatched = true;
                    }
                    if (!officerMatched) {
                        allShipsMatched = false;
                        break;
                    }
                }
                variantMatched = true;
                break;
            }
            
            if (!variantMatched) {
                allShipsMatched = false;
                break;
            }
        }

        return allShipsMatched;
    }

    public static boolean isPresetPlayerFleetOfficerAgnostic(String presetName) {
        FleetPreset preset = getFleetPresets().get(presetName);
        if (preset == null) return false;

        List<FleetMemberAPI> playerFleetMembers = Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder();
        if (playerFleetMembers.size() != preset.getShipIds().size()) {
            return false;
        }

        boolean allShipsMatched = true;
        for (int i = 0; i < playerFleetMembers.size(); i++) {
            FleetMemberAPI member = playerFleetMembers.get(i);
            String hullId = member.getHullId();
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
                break;
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

        for (String hullId : preset.getShipIds()) {
            requiredShips.put(hullId, requiredShips.getOrDefault(hullId, 0) + 1);
        }

        if (playerCurrentFleet != null) {
            for (FleetMemberAPI member : playerCurrentFleet) {
                String hullId = member.getHullId();
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

        for (FleetMemberAPI storedMember : mothballedShipsFleetData.getMembersInPriorityOrder()) {
            String hullId = storedMember.getHullId();
            
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
                String hullId = member.getHullId();
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
        Map<String, List<FleetMemberWrapper>> neededShips = findNeededShipsWrappedNonIdMatching(preset, Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder());
        if (neededShips.isEmpty()) return null;

        CargoAPI storageCargo = CargoPresetUtils.getStorageSubmarket(market).getCargo();
        initMothballedShips(storageCargo);

        Map<FleetMemberWrapper, FleetMemberAPI> neededMembers = new HashMap<>();

        for (FleetMemberAPI storedMember : storageCargo.getMothballedShips().getMembersInPriorityOrder()) {
            if (neededShips.containsKey(storedMember.getHullId())) {
                if (!isMemberFromPreset(storedMember, preset)) {
                    for (FleetMemberWrapper member : neededShips.get(storedMember.getHullId())) {
                        if (areSameVariant(member.getMember().getVariant(), storedMember.getVariant())) {
                            neededMembers.put(member, storedMember);
                            neededShips.get(storedMember.getHullId()).remove(member);
                            break;
                        }
                    }
                    if (neededShips.get(storedMember.getHullId()).size() == 0) neededShips.remove(storedMember.getHullId());
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
        CampaignFleetAPI mangledFleet = Global.getFactory().createEmptyFleet(Global.getSector().getPlayerFaction(), true);
        mangledFleet.setHidden(true);
        mangledFleet.setNoAutoDespawn(true);
        mangledFleet.setDoNotAdvanceAI(true);
        mangledFleet.setInflated(true);
        mangledFleet.setNoFactionInName(true);
        
        List<FleetMemberAPI> members = fleetToBeMangled.getFleetData().getMembersInPriorityOrder();
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

                if (indexedMembers.get(i).getCaptain().getId().equals(Global.getSector().getPlayerPerson().getId())) {
                    mangledFleet.setCommander(indexedMembers.get(i).getCaptain());
                    mangledFleet.getFleetData().setFlagship(indexedMembers.get(i));
                }

            } else {
                mangledFleet.getFleetData().addFleetMember(members.get(i));

                if (members.get(i).getCaptain().getId().equals(Global.getSector().getPlayerPerson().getId())) {
                    mangledFleet.setCommander(indexedMembers.get(i).getCaptain());
                    mangledFleet.getFleetData().setFlagship(members.get(i));
                }
            }
        }
        return mangledFleet;
    }

    // should only be called if preset has no officers
    // needs testing
    public static void assignofficersToPreset(FleetPreset preset, List<FleetMemberAPI> playerFleetMembers) {
        for (String hullId: preset.getShipIds()) {
            for (int i = 0; i < playerFleetMembers.size(); i++) {
                FleetMemberAPI fleetMember = playerFleetMembers.get(i);
                if (hullId.equals(fleetMember.getHullId())) {
                    PersonAPI captain = fleetMember.getCaptain();

                    if (captain != null) {
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

    // requires current fleet to be a saved preset
    // officers need to be assigned by player to fleet members before this function is called
    // needs testing
    public static void applyOfficerChangesToPreset(String presetName) {
        FleetPreset preset = (FleetPreset) getFleetPresets().get(presetName);
        List<FleetMemberAPI> playerFleetMembers = Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder();

        if (areOfficersInPlayerFleet(playerFleetMembers) && preset.getOfficersMap().isEmpty()) {
            assignofficersToPreset(preset, playerFleetMembers);
        } else {
            for (int i = 0; i < playerFleetMembers.size(); i++) {
                FleetMemberAPI fleetMember = playerFleetMembers.get(i);
                OfficerVariantPair pair = preset.getOfficersMap().get(i);

                if (pair != null) {
                    if (areSameVariant(pair.getVariant(), fleetMember.getVariant())) {
                        if (!isOfficerInFleet(pair.getOfficer().getId(), playerFleetMembers)) {
                            preset.getOfficersMap().remove(i);
                            preset.getOfficersMap().put(i, new OfficerVariantPair(fleetMember.getCaptain(), pair.getVariant(), i));
                            preset.updateOfficer(i, fleetMember.getCaptain());
                        }
                    }
                }
            }
        }
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

        for (FleetMemberAPI member : mothballedShipsFleetData.getMembersInPriorityOrder()) {
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

        return storageCargo.getMothballedShips().getMembersInPriorityOrder();
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

        for (FleetMemberAPI member : mothballedShipsFleetData.getMembersInPriorityOrder()) {
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
        // CargoResourceRatios cargoRatios = new CargoResourceRatios(playerFleet.getFleetData().getMembersInPriorityOrder(), playerFleet.getCargo());
        SubmarketPlugin storagePlugin = storage.getPlugin();
        if (!isPlayerPaidForStorage(storagePlugin)) return;
        
        CargoAPI storageCargo = storage.getCargo();
        // CargoAPI playerCargo = playerFleet.getCargo();
        initMothballedShips(storageCargo);
        
        FleetDataAPI playerFleetData = playerFleet.getFleetData();
        FleetDataAPI mothballedShipsFleetData = storageCargo.getMothballedShips();

        for (FleetMemberAPI member : playerFleetData.getMembersInPriorityOrder()) {
            if (member.getCaptain().isPlayer()) continue;

            member.setCaptain(null);
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
                areSameVariant(member.getVariant(), memberToCheck.getVariant())) {

                PersonAPI captain = member.getCaptain();
                PersonAPI captainToCheck = memberToCheck.getCaptain();
    
                if (captain.getName().getFullName().equals("") && captainToCheck.getName().getFullName().equals("")) return true;
    
                if (captain.getId().equals(captainToCheck.getId())) return true;
            }
        }
        return false;
    }

    public static void saveFleetPreset(String name) {
        List<FleetMemberAPI> fleetMembers = Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder();
        if (getFleetPresets().get(name) != null) {
            deleteFleetPreset(name);
        }
        // sortFleetMembers(fleetMembers, SIZE_ORDER_DESCENDING);
        getFleetPresets().put(name, new FleetPreset(name, fleetMembers));
    }

    public static FleetMemberAPI getPlayerFleetMember(FleetDataAPI playerFleetData) {
        for (FleetMemberAPI member : playerFleetData.getMembersInPriorityOrder()) {
            if (member.getCaptain().isPlayer()) return member;
        }
        return null;
    }

    public static boolean areSameWeaponSlots(Collection<String> slots1, Collection<String> slots2) {
        return new HashSet<>(slots1).equals(new HashSet<>(slots2));
    }

    // this is because variant1.equals(variant2) doesnt always work, idk why and i dont really care
    // xstream is mangling half of this shit and i dont know what to do about it
    public static boolean areSameVariant(ShipVariantAPI variant1, ShipVariantAPI variant2) {
        // xstream serializer mangles weapon groups on game save/load or something? so we need to do this
        List<WeaponGroupSpec> variant1WeaponGroups = variant1.getWeaponGroups();
        List<WeaponGroupSpec> variant2WeaponGroups = variant2.getWeaponGroups();

        // print("-------------------------------------------------------");
        // print(variant1.getHullSpec().getHullId());

        if (variant1WeaponGroups.size() != variant2WeaponGroups.size()) return false;
        for (int i = 0; i < variant1WeaponGroups.size(); i++) {
            List<String> slots1 = variant1WeaponGroups.get(i).getSlots();
            List<String> slots2 = variant2WeaponGroups.get(i).getSlots();
            // slots1.equals(slots2) doesnt work either, we actually have to go through it and compare each directly

            if (slots1.size() != slots2.size()) return false;
            for (int j = 0; j < slots1.size(); j++) {
                if (!slots1.get(j).equals(slots2.get(j))) return false;
            }
        }

        // print("Weapon groups are the same");
        // print("hullId match:", variant1.getHullSpec().getHullId().equals(variant2.getHullSpec().getHullId()));
        // print(variant1.getHullSpec().getHullId(), variant2.getHullSpec().getHullId());
        // print("smods match:", variant1.getSMods().equals(variant2.getSMods()));
        // print("hullmods match:", variant1.getHullMods().equals(variant2.getHullMods()));
        // print("wings match:", variant1.getWings().equals(variant2.getWings()));
        // print("fittedweaponslots match:", areSameWeaponSlots(variant1.getFittedWeaponSlots(), variant2.getFittedWeaponSlots()));
        // // print("fittedweaponslots match raw:", variant1.getFittedWeaponSlots().equals(variant2.getFittedWeaponSlots()));
        // print("smoddedbuiltins match:", variant1.getSModdedBuiltIns().equals(variant2.getSModdedBuiltIns()));
        // print("fluxcapacitors match:", variant1.getNumFluxCapacitors() == variant2.getNumFluxCapacitors(), variant1.getNumFluxCapacitors(), variant2.getNumFluxCapacitors());
        // print("fluxvents match:", variant1.getNumFluxVents() == variant2.getNumFluxVents(), variant1.getNumFluxVents(), variant2.getNumFluxVents());
        // print("-------------------------------------------------------");

        return (variant1.getHullSpec().getHullId().equals(variant2.getHullSpec().getHullId())
            && variant1.getSMods().equals(variant2.getSMods())
            && variant1.getHullMods().equals(variant2.getHullMods())
            && variant1.getWings().equals(variant2.getWings())
            // && variant1.getFittedWeaponSlots().equals(variant2.getFittedWeaponSlots()) // THIS DOESNT WORK AFTER GAME SAVE I DONT FUCKING KNOW WHY
            && areSameWeaponSlots(variant1.getFittedWeaponSlots(), variant2.getFittedWeaponSlots()) // this inexplicably works though
            && variant1.getSModdedBuiltIns().equals(variant2.getSModdedBuiltIns())
            // && variant1.getWeaponGroups().equals(variant2.getWeaponGroups()) // fuck you xstream
            && variant1.getNumFluxCapacitors() == variant2.getNumFluxCapacitors()
            && variant1.getNumFluxVents() == variant2.getNumFluxVents());
    }

    public static boolean areSameHullMods(ShipVariantAPI variant1, ShipVariantAPI variant2) {
        return (variant1.getHullMods().equals(variant2.getHullMods()));
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
        List<FleetMemberAPI> playerFleetMembers = playerFleet.getFleetData().getMembersInPriorityOrder();

        // boolean isEqualizeCargo = (boolean)Global.getSector().getPersistentData().get(KEEPCARGORATIOS_KEY);
        // CargoAPI playerCargo = null;
        // CargoResourceRatios cargoRatios = null;
        // if (isEqualizeCargo) {
        //     playerCargo = playerFleet.getCargo();
        //     cargoRatios = new CargoResourceRatios(playerFleetMembers, playerCargo);
        // }
        
        FleetMemberAPI playerFleetMember = getPlayerFleetMember(playerFleetData);

        for (FleetMemberAPI member : playerFleetMembers) {
            member.setCaptain(null);
            playerFleetData.removeFleetMember(member);
            storageCargo.getMothballedShips().addFleetMember(member);
        }
        initMothballedShips(storageCargo);

        List<CampaignUIMessage> messageQueue = (List<CampaignUIMessage>) Global.getSector().getMemoryWithoutUpdate().get(MESSAGEQUEUE_KEY);
        List<FleetMemberAPI> membersDone = new ArrayList<>();
        boolean allFound = true;

        for (int i = 0; i < preset.getShipIds().size(); i++) {
            String hullId = preset.getShipIds().get(i);
            ShipVariantAPI variant = preset.getVariantsMap().get(i);

            boolean found = false;
            for (FleetMemberAPI storedMember : storageCargo.getMothballedShips().getMembersInPriorityOrder()) {
                if (storedMember.getHullId().equals(hullId)) {
                    if (preset.getOfficersMap().containsKey(i)) {
                        OfficerVariantPair pair = preset.getOfficersMap().get(i);

                        if (areSameVariant(pair.getVariant(), variant)) {
                            storedMember.setCaptain(pair.getOfficer());

                            storageCargo.getMothballedShips().removeFleetMember(storedMember);
                            membersDone.add(storedMember);

                            if (!storedMember.getId().equals(preset.getFleetMembers().get(i).getId())) {
                                preset.updateWrappedMember(i, storedMember);
                            }
                            // CargoPresetUtils.refit(storedMember, variant, playerCargo, storageCargo);
                            found = true;
                            break;
                        }
                    }
                    
                    if (areSameVariant(variant, storedMember.getVariant())) {
                        storageCargo.getMothballedShips().removeFleetMember(storedMember);
                        membersDone.add(storedMember);

                        if (!storedMember.getId().equals(preset.getFleetMembers().get(i).getId())) {
                            preset.updateWrappedMember(i, storedMember);
                        }
                        // CargoPresetUtils.refit(storedMember, variant, playerCargo, storageCargo);
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


        if (playerFleetData.getMembersInPriorityOrder().size() < 1) {
            initMothballedShips(storageCargo);
            for (FleetMemberAPI storageMember : storageCargo.getMothballedShips().getMembersInPriorityOrder()) {
                if (areSameVariant(storageMember.getVariant(), playerFleetMember.getVariant())) {
                    storageMember.setCaptain(Global.getSector().getPlayerPerson());
                    playerFleetData.addFleetMember(storageMember);
                }
            }
        }
        updateFleetPresetStats(playerFleetData.getMembersInPriorityOrder());
        refreshFleetUI();
        // if (isEqualizeCargo) CargoPresetUtils.equalizeCargo(playerFleetData.getMembersInPriorityOrder(), playerCargo, storageCargo, cargoRatios);

        if (allFound) {
            CampaignUIMessage msg = new CampaignUIMessage(RESTOREMESSAGE_SUCCESS_PREFIX + name, Misc.getPositiveHighlightColor());
            if (messageQueue.contains(msg)) messageQueue.remove(msg);
            messageQueue.add(msg);
        }
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

    public static void addMessagesToCampaignUI() {
        List<CampaignUIMessage> messageQueue = (List<CampaignUIMessage>) Global.getSector().getMemoryWithoutUpdate().get(MESSAGEQUEUE_KEY);
        for (CampaignUIMessage message : messageQueue) {
            Global.getSector().getCampaignUI().addMessage(message.getMessage(), message.getColor());
        }
        messageQueue.clear();
    }

    public static void refreshFleetUI() {
        Object fleetInfoPanel = Global.getSector().getMemoryWithoutUpdate().get(FLEETINFOPANEL_KEY);
        if (fleetInfoPanel == null) return;

        Object infoPanelParent = ReflectionUtilis.invokeMethod("getParent", fleetInfoPanel);
        Object fleetPanel = ReflectionUtilis.invokeMethod("getFleetPanel", infoPanelParent);

        ReflectionUtilis.getMethodAndInvokeDirectly("recreateUI", fleetPanel, 1, false);

        // ReflectionUtilis.logFields(fleetPanel);

        // for (Object child : UtilReflection.getChildrenRecursive(fleetPanel)) {
        //     // I DONT KNOW HOW TO RESET THE THING AFTER IT ADVANCES ONCE AFTER REBUILDING TO FIX THE TOOLTIP BULLSHIT AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
        //     // ReflectionUtilis.logFields(child);

        //     Object field = ReflectionUtilis.getFieldAtIndex(child, 1);
        //     if (field instanceof List) {
        //         for (Object o : (List<Object>) field) {
        //             for (Object child1 : (List<Object>) ReflectionUtilis.getMethodAndInvokeDirectly("getChildrenNonCopy", o, 0)) {
        //                 ReflectionUtilis.logMethods(child1);
        //             }
        //             List<Object> lst = new ArrayList<>();
        //             lst.add(UtilReflection.createInputEventInstance(InputEventClass.MOUSE_EVENT, InputEventType.MOUSE_MOVE, -1, -1, -1, '\0'));
        //             lst.add(UtilReflection.createInputEventInstance(InputEventClass.MOUSE_EVENT, InputEventType.MOUSE_MOVE, -1, -1, -1, '\0'));
        //             lst.add(UtilReflection.createInputEventInstance(InputEventClass.MOUSE_EVENT, InputEventType.MOUSE_MOVE, -1, -1, -1, '\0'));
        //             ReflectionUtilis.getMethodExplicitAndInvokeDirectly("processInput", o, new Class<?>[]{List.class}, lst);
        //             // ReflectionUtilis.logMethods(o);
        //             // ReflectionUtilis.logFields(o);
        //             break;
        //         }
        //     }
            
        //     // ReflectionUtilis.logMethods(child);
        //     List<Object> lst = new ArrayList<>();
        //     lst.add(UtilReflection.createInputEventInstance(InputEventClass.MOUSE_EVENT, InputEventType.MOUSE_MOVE, 1, 1, -1, '\0'));
        //     lst.add(UtilReflection.createInputEventInstance(InputEventClass.MOUSE_EVENT, InputEventType.MOUSE_MOVE, 1, 1, -1, '\0'));
        //     lst.add(UtilReflection.createInputEventInstance(InputEventClass.MOUSE_EVENT, InputEventType.MOUSE_MOVE, 1, 1, -1, '\0'));
        //     ReflectionUtilis.getMethodExplicitAndInvokeDirectly("processInput", child, new Class<?>[]{List.class}, lst);
        // }
    }

    public static void deleteFleetPreset(String name) {
        FleetPreset preset = getFleetPresets().get(name);
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        if (mem.get(UNDOCKED_PRESET_KEY) != null && ((FleetPreset)mem.get(UNDOCKED_PRESET_KEY)).getName().equals(name)) mem.unset(UNDOCKED_PRESET_KEY);

        preset.getCampaignFleet().despawn();
        preset.setCampaignFleet(null);

        Map<String, List<FleetMemberWrapper>> presetsMembers = getFleetPresetsMembers();
        for (FleetMemberWrapper member : preset.getFleetMembers()) {
            presetsMembers.get(member.getId()).remove(member);
        }

        getFleetPresets().remove(name);
    }

    public static void removeOfficerFromPresets(String officerId) {
        Map<String, FleetPreset> presets = getFleetPresets();

        for (FleetPreset fleetPreset : presets.values()) {
            for (int i = 0; i < fleetPreset.getShipIds().size(); i++) {
                fleetPreset.getOfficersMap().remove(i);
            }

            for (FleetMemberWrapper member : fleetPreset.getFleetMembers()) {
                if (member.getCaptainId() != null && member.getCaptainId().equals(officerId)) {
                    member.updateCaptain(null);
                    break;
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