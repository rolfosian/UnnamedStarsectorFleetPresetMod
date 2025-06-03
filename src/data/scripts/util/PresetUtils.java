package data.scripts.util;

import java.util.*;
import java.util.stream.Collectors;
import java.awt.Color;

import com.fs.starfarer.api.Global;

import com.fs.starfarer.campaign.fleet.CampaignFleet;
import com.fs.starfarer.campaign.fleet.FleetMember;
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

import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import com.fs.starfarer.api.loading.WeaponGroupSpec;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;

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

    // Non-persistent data keys
    public static final String FLEETINFOPANEL_KEY = "$fleetInfoPanel";
    public static final String UNDOCKED_PRESET_KEY = "$presetUndocked";
    public static final String EXTRANEOUS_MEMBERS_KEY = "$extraneousPresetMembers";
    public static final String PLAYERCURRENTMARKET_KEY = "$playerCurrentMarket";
    public static final String COREUI_KEY = "$coreUI";
    public static final String ISPLAYERPAIDFORSTORAGE_KEY = "$isPlayerPaidForStorage";
    public static final String MESSAGEQUEUE_KEY = "$presetsMessageQueue";
    public static final String VISUALFLEETINFOPANEL_KEY = "$visualFleetInfoPanelClass";

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

    public static class RunningMembers extends HashMap<FleetMemberAPI, PersonAPI> {
        public RunningMembers(List<FleetMemberAPI> fleetMembers) {
            for (FleetMemberAPI fleetMember : fleetMembers) {
                this.put(fleetMember, fleetMember.getCaptain());
            }
        }
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
        public final FleetPreset preset;
        public final FleetMemberAPI member;
        public final String id;

        public int index;
        public PersonAPI captainCopy;
        public PersonAPI captain;
        public String captainId;
        public FleetMemberAPI parentMember;


        public FleetMemberWrapper(FleetPreset preset, FleetMemberAPI member, PersonAPI captain, int index) {
            this.member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, member.getVariant());
            this.member.getRepairTracker().setCR(member.getRepairTracker().getCR());
            this.member.getStatus().setHullFraction(member.getStatus().getHullFraction());
            this.id = member.getId();
            this.member.setId(member.getId());

            if (captain != null) {
                this.captain = captain;
                this.captainId = captain.getId();

                this.captainCopy =  Global.getFactory().createPerson();
                captainCopy.setPortraitSprite(captain.getPortraitSprite());
                captainCopy.setFaction(Global.getSector().getPlayerFaction().getId());
                captainCopy.setStats(captain.getStats());
                captainCopy.setName(captain.getName());
                this.member.setCaptain(captainCopy);

            } else {
                this.captain = null;
                this.captainId = null;
            }

            this.index = index;
            this.parentMember = member;
            this.preset = preset;
        }

        public void updateCaptain(PersonAPI captain) {
            if (captain == null) {
                this.captain = null;
                this.captainId = null;
                this.captainCopy = null;
                this.member.setCaptain(null);
                return;
            }

            this.captain = captain;
            this.captainId = captain.getId();

            this.captainCopy = Global.getFactory().createPerson();
            this.captainCopy.setRankId(captain.getRankId());
            this.captainCopy.setFaction(Global.getSector().getPlayerFaction().getId());
            this.captainCopy.getStats().setLevel(captain.getStats().getLevel());
            this.captainCopy.setName(captain.getName());
            this.captainCopy.setPortraitSprite(captain.getPortraitSprite());
            this.member.setCaptain(captainCopy);

            if (captainId.equals(Global.getSector().getPlayerPerson().getId())) {
                this.preset.campaignFleet.setCommander(this.captainCopy);
                this.preset.campaignFleet.getFleetData().setFlagship(this.member);
            }
        }

        // i dont even remember where this was used
        public void removeFrompreset() {
            this.preset.campaignFleet.getFleetData().removeFleetMember(this.member);
            this.preset.shipIds.remove(this.index);
            this.preset.variantsMap.remove(this.index);
            this.preset.fleetMembers.remove(this.index);
            this.preset.officersMap.remove(this.index);
            this.preset.updateIndexesAfterMemberRemoved(index);
            // this.preset.rebuildPresetAfterMemberRemoved();

            this.captainCopy = null;
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

    public static class FleetPreset {
        public final String name;
        public List<String> shipIds = new ArrayList<>(); // this is redundant since refactoring but i cant be btohered changing related logic
        public Map<Integer, ShipVariantAPI> variantsMap = new HashMap<>();
        public Map<Integer, OfficerVariantPair> officersMap = new HashMap<>();
        public List<FleetMemberWrapper> fleetMembers = new ArrayList<>();
        public CampaignFleetAPI campaignFleet;

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
                PersonAPI captain = member.getCaptain();
    
                this.shipIds.add(hullId);
                this.variantsMap.put(i, variant);
    
                if (captain != null) {
                    officersMap.put(i, new OfficerVariantPair(captain, variant, i));
                }

                FleetMemberWrapper wrappedMember = new FleetMemberWrapper(this, member, captain, i);
                this.fleetMembers.add(wrappedMember);

                this.campaignFleet.getFleetData().addFleetMember(wrappedMember.member);
                if (wrappedMember.captainId.equals(Global.getSector().getPlayerPerson().getId())) {
                    this.campaignFleet.setCommander(wrappedMember.captainCopy);
                    this.campaignFleet.getFleetData().setFlagship(wrappedMember.member);
                }

                if (presetsMembers.get(member.getId()) == null) getFleetPresetsMembers().put(member.getId(), new ArrayList<>());
                presetsMembers.get(member.getId()).add(wrappedMember);
            }
        }

        public void reorderCampaignFleet() {
            List<FleetMemberAPI> order = new ArrayList<>();
            for (FleetMemberWrapper member : this.fleetMembers) {
                order.add(member.member);
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
            this.campaignFleet.getFleetData().removeFleetMember(this.fleetMembers.get(index).member);
            this.fleetMembers.set(index, newWrappedMember);
            this.variantsMap.put(index, newMember.getVariant());
            this.officersMap.put(index, new OfficerVariantPair(newMember.getCaptain(), newMember.getVariant(), index));

            this.campaignFleet.getFleetData().addFleetMember(this.fleetMembers.get(index).member);
            if (newWrappedMember.captainId.equals(Global.getSector().getPlayerPerson().getId())) {
                this.campaignFleet.getFleetData().setFlagship(this.fleetMembers.get(index).member);
                this.campaignFleet.setCommander(this.fleetMembers.get(index).member.getCaptain());
            }

            presetsMembersMap.get(newMember.getId()).add(newWrappedMember);

            reorderCampaignFleet();
        }

        public void updateIndexesAfterMemberRemoved(int removedIndex) {
            this.shipIds.remove(removedIndex);
            this.variantsMap.remove(removedIndex);
            this.officersMap.remove(removedIndex);
        
            Map<Integer, ShipVariantAPI> updatedVariantsMap = new HashMap<>();
            for (Map.Entry<Integer, ShipVariantAPI> entry : this.variantsMap.entrySet()) {
                int index = entry.getKey();
                updatedVariantsMap.put(index < removedIndex ? index : index - 1, entry.getValue());
            }
            this.variantsMap = updatedVariantsMap;
        
            Map<Integer, OfficerVariantPair> updatedOfficersMap = new HashMap<>();
            for (Map.Entry<Integer, OfficerVariantPair> entry : this.officersMap.entrySet()) {
                int index = entry.getKey();
                OfficerVariantPair pair = entry.getValue();
                updatedOfficersMap.put(index < removedIndex ? index : index - 1, new OfficerVariantPair(pair.officer, pair.variant, index < removedIndex ? index : index - 1));
            }
            this.officersMap = updatedOfficersMap;
        
            for (int i = 0; i < this.fleetMembers.size(); i++) {
                this.fleetMembers.get(i).index = i;
            }
            reorderCampaignFleet();
        }

        public void rebuildPresetAfterMemberRemoved() {
            List<FleetMemberWrapper> wrappedMembers = new ArrayList<>();

            for (FleetMemberWrapper wrappedMember : this.fleetMembers) {
                wrappedMembers.add(wrappedMember);
            }

            this.fleetMembers.clear();
            this.shipIds.clear();
            this.variantsMap.clear();
            this.officersMap.clear();

            for (int i = 0; i < wrappedMembers.size(); i++) {
                FleetMemberWrapper member = wrappedMembers.get(i);
                String hullId = member.member.getHullId();
                
                FleetMemberWrapper wrappedMember = new FleetMemberWrapper(this, member.member, member.member.getCaptain(), i);
                this.campaignFleet.getFleetData().addFleetMember(wrappedMember.member);
                
                this.fleetMembers.add(wrappedMember);
                
                this.shipIds.add(hullId);
                this.variantsMap.put(i, member.member.getVariant());
                
                if (!isOfficerNought(member.member.getCaptain())) {
                    this.officersMap.put(i, new OfficerVariantPair(member.member.getCaptain(), member.member.getVariant(), i));
                    if (Global.getSector().getPlayerPerson().getId().equals(member.member.getCaptain().getId())) {
                        this.campaignFleet.setCommander(wrappedMember.captainCopy);
                        this.campaignFleet.getFleetData().setFlagship(wrappedMember.member);
                    }
                }
            }
            Global.getSector().getCampaignUI().addMessage("The fleet composition has changed and the " + this.name + " fleet preset has been updated.", Misc.getBasePlayerColor());
        }

        public void updateVariant(int index, ShipVariantAPI variant) {
            this.fleetMembers.get(index).member.setVariant(variant, true, true);
            this.variantsMap.put(index, variant);
            this.shipIds.set(index, variant.getHullSpec().getHullId());

            for (int i = 0; i < this.shipIds.size(); i++) {
                OfficerVariantPair pair = this.officersMap.get(index);
                if (pair != null) {
                    pair.variant = variant;
                }
            }
        }

        public void updateOfficer(int index, PersonAPI captain) {
            FleetMemberWrapper member = this.fleetMembers.get(index);
            member.updateCaptain(captain);
            if (captain.isPlayer()) {
                this.campaignFleet.setCommander(member.captainCopy);
                this.campaignFleet.getFleetData().setFlagship(member.member);
            }

            if (!captain.getName().getFullName().equals("")) {
                this.officersMap.put(index, new OfficerVariantPair(captain, member.member.getVariant(), index));
            } else {
                OfficerVariantPair pair = this.officersMap.get(index);
                if (pair != null) {
                    this.officersMap.remove(index);
                }
            }
        }
    }

    public static void handlePerishedPresetMembers() {
        Collection<FleetPreset> presets = getFleetPresets().values();
        List<String> storedMembers = getStoredFleetPresetsMemberIds();
        Map<String, List<FleetMemberWrapper>> presetMembers = getFleetPresetsMembers();

        for (FleetPreset preset : presets) {
            for (FleetMemberWrapper wrappedMember : preset.fleetMembers) {
                
                if (isPresetMemberPerished(wrappedMember.parentMember, storedMembers) && getFleetPresetsMembers().get(wrappedMember.member.getId()) != null) {
                    presetMembers.remove(wrappedMember.member.getId());
                }
            }
        }
    }

    public static boolean isPresetMemberPerished(FleetMemberAPI member, List<String> storedMemberIds) {
        if (storedMemberIds.contains(member.getId()) || isMemberFromAnyPreset(member)) {
            return false;
        } else if (member.getFleetData() == null) {
            return true;
        }
        return false;
    }

    public static boolean isMemberFromAnyPreset(FleetMemberAPI member) {
        for (FleetPreset preset : getFleetPresets().values()) {
            for (FleetMemberWrapper wrappedMember : preset.fleetMembers) {
                if (wrappedMember.member.getId().equals(member.getId())) return true;
            }
        }
        return false;
    }

    public static boolean isPlayerFleetChanged(FleetPreset preset, List<FleetMemberAPI> playerFleetMembers) {
        if (playerFleetMembers.size() != preset.fleetMembers.size()) {
            return true;
        }
        
        for (int i = 0; i < preset.fleetMembers.size(); i++) {
            FleetMemberAPI playerFleetMember = playerFleetMembers.get(i);
            FleetMemberWrapper member = preset.fleetMembers.get(i);

            if (!areSameVariant(playerFleetMember.getVariant(), member.member.getVariant())
                || !isOfficerSameAsPresetMember(playerFleetMember, member) || playerFleetMember.getId() != member.id) {
                return true;
            }
        }
        return false;
    }
    
    public static Map<String, List<FleetMemberWrapper>> getFleetPresetsMembers() {
        return (Map<String, List<FleetMemberWrapper>>) Global.getSector().getPersistentData().get(PRESET_MEMBERS_KEY);
    }

    public static List<String> getStoredFleetPresetsMemberIds() {
        return (List<String>) Global.getSector().getPersistentData().get(STORED_PRESET_MEMBERIDS_KEY);
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

                float presetCR = memberToUpdate.member.getRepairTracker().getCR();
                float presetHullFraction = memberToUpdate.member.getStatus().getHullFraction();
                
                if (playerMemberCR != presetCR) {
                    memberToUpdate.member.getRepairTracker().setCR(playerMemberCR);
                    memberToUpdate.preset.campaignFleet.getFleetData().getMembersInPriorityOrder().get(memberToUpdate.index).getRepairTracker().setCR(playerMemberCR);
                }
                if (playerMemberHullFraction != presetHullFraction) {
                    memberToUpdate.member.getStatus().setHullFraction(playerMemberHullFraction);
                    memberToUpdate.preset.campaignFleet.getFleetData().getMembersInPriorityOrder().get(memberToUpdate.index).getStatus().setHullFraction(playerMemberHullFraction);
                }

                if (!isOfficerNought(memberToUpdate.captainCopy)) {
                    PersonAPI fleetMemberCaptain = memberToUpdate.preset.campaignFleet.getFleetData().getMembersInPriorityOrder().get(memberToUpdate.index).getCaptain();
                    if (fleetMemberCaptain.getStats().getLevel() != playerMember.getCaptain().getStats().getLevel()) {
                        memberToUpdate.captainCopy.setStats(playerMember.getCaptain().getStats());
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

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        List<FleetMemberAPI> playerFleetMembers = playerFleet.getFleetData().getMembersInPriorityOrder();

        if (isAutoUpdate) {
            boolean updated = false;

            if (playerFleetMembers.size() != preset.fleetMembers.size()) {
                int sizeDifference = preset.fleetMembers.size() - playerFleetMembers.size();
                // for some reason the officers arent updated immediately before the FleetMonitor calls this function if ships are scuttled with officers in them so we have to do THIS FUCKING BULLSHIT
                List<PersonAPI> officersToReassign = null;
                if (sizeDifference >= 1) {
                    officersToReassign = new ArrayList<>();

                    for (Map.Entry<FleetMemberAPI, PersonAPI> entry : runningMembers.entrySet()) {
                        FleetMemberAPI member = entry.getKey();
                        PersonAPI officer = entry.getValue();
    
                        if (!isOfficerNought(officer) && !playerFleetMembers.contains(member)) {
                            officersToReassign.add(officer);
                        }
                    }
                }

                if (preset.campaignFleet == null) preset.campaignFleet = createDummyPresetFleet();
                for (FleetMemberWrapper wrappedMember : preset.fleetMembers) {
                    preset.campaignFleet.getFleetData().removeFleetMember(wrappedMember.member);
                    getFleetPresetsMembers().get(wrappedMember.id).remove(wrappedMember);
                }
                preset.fleetMembers.clear();

                preset.shipIds.clear();
                preset.variantsMap.clear();
                preset.officersMap.clear();
                Set<FleetPreset> presetsToUpdate = new HashSet<>();
                presetsToUpdate.add(preset);

                for (int i = 0; i < playerFleetMembers.size(); i++) {
                    FleetMemberAPI member = playerFleetMembers.get(i);
                    String hullId = member.getHullId();
                    
                    FleetMemberWrapper wrappedMember = new FleetMemberWrapper(preset, member, member.getCaptain(), i);
                    preset.campaignFleet.getFleetData().addFleetMember(wrappedMember.member);

                    List<FleetMemberWrapper> presetMembers = getFleetPresetsMembers().get(member.getId());
                    if (presetMembers != null) {
                        for (int j = 0; j < presetMembers.size(); j++) {
                            FleetMemberWrapper presetMember = presetMembers.get(j);
    
                            if (preset.name.equals(presetMember.preset.name)) {
                                presetMembers.set(j, wrappedMember);
                            } else {
                                presetMembers.set(j, new FleetMemberWrapper(presetMember.preset, member, member.getCaptain(), presetMember.index));
                            }
                            presetsToUpdate.add(presetMember.preset);
                        }
                    } else {
                        getFleetPresetsMembers().put(member.getId(), new ArrayList<>());
                        getFleetPresetsMembers().get(member.getId()).add(wrappedMember);
                    }
                    preset.fleetMembers.add(wrappedMember);
                    
                    preset.shipIds.add(hullId);
                    preset.variantsMap.put(i, member.getVariant());
                    
                    if (!isOfficerNought(member.getCaptain())) {
                        preset.officersMap.put(i, new OfficerVariantPair(member.getCaptain(), member.getVariant(), i));

                        if (wrappedMember.captainId.equals(Global.getSector().getPlayerPerson().getId())) {
                            preset.campaignFleet.setCommander(wrappedMember.member.getCaptain());
                            preset.campaignFleet.getFleetData().setFlagship(wrappedMember.member);
                        }
                    }
                }

                if (officersToReassign != null) {
                    for (PersonAPI officer : officersToReassign) {
                        for (int i = 0; i < playerFleetMembers.size(); i++) {
                            if (isOfficerNought(playerFleetMembers.get(i).getCaptain())) {
                                playerFleetMembers.get(i).setCaptain(officer);
                                preset.fleetMembers.get(i).updateCaptain(officer);
                                break;
                            }
                        }
                    }
                }
                Global.getSector().getCampaignUI().addMessage("The fleet composition has changed and the " + preset.name + " fleet preset has been updated.", Misc.getBasePlayerColor());

            } else {
                for (FleetMemberWrapper member : preset.fleetMembers) {
                    FleetMemberAPI playerFleetMember = playerFleetMembers.get(member.index);

                    if (!areSameVariant(playerFleetMember.getVariant(), member.member.getVariant())) {
                        preset.updateVariant(member.index, playerFleetMember.getVariant());
                        updated = true; 
                    }

                    if (!playerFleetMember.getId().equals(member.id) || !getFleetPresetsMembers().containsKey(member.id)) {
                        preset.updateWrappedMember(member.index, playerFleetMember);
                        updated = true;
                    }

                    if (!isOfficerSameAsPresetMember(playerFleetMember, member)) {
                        preset.updateOfficer(member.index, playerFleetMember.getCaptain());
                        updated = true;
                    }
                }
            }

            if (updated) {
                Global.getSector().getCampaignUI().addMessage("The fleet composition has changed and the " + preset.name + " fleet preset has been updated.", Misc.getBasePlayerColor());
            }

        } else {
            if (isPlayerFleetChanged(preset, playerFleetMembers)) {
                Global.getSector().getCampaignUI().addMessage("The fleet composition has changed. Consider updating the " + preset.name + " fleet preset to match the current fleet.", Misc.getBasePlayerColor());
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
        if (isOfficerNought(playerFleetMember.getCaptain()) && isOfficerNought(presetMember.captainCopy)) {
            return true;
        }
        if (isOfficerNought(playerFleetMember.getCaptain()) || isOfficerNought(presetMember.captainCopy)) {
            return false;
        }
        return playerFleetMember.getCaptain().getId().equals(presetMember.captainId);
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
            if (targetMembers.size() != preset.shipIds.size()) {
                continue;
            }

            boolean allShipsMatched = true;

            for (int i = 0; i < targetMembers.size(); i++) {
                FleetMemberAPI playerMember = targetMembers.get(i);
                ShipVariantAPI variant = playerMember.getVariant();
                PersonAPI captain = playerMember.getCaptain();

                ShipVariantAPI presetVariant = preset.variantsMap.get(i);
                if (presetVariant == null) {
                    allShipsMatched = false;
                    break;
                }

                boolean variantMatched = false;
                if (areSameVariant(presetVariant, variant)) {
                    if (!isOfficerNought(captain) && preset.officersMap.containsKey(i)) {
                        OfficerVariantPair pair = preset.officersMap.get(i);
                        boolean officerMatched = false;

                        if (areSameVariant(pair.variant, variant) && (captain != null && pair.officer.getId().equals(captain.getId()))) officerMatched = true;

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
        if (playerFleetMembers.size() != preset.shipIds.size()) {
            return false;
        }

        boolean allShipsMatched = true;
        for (int i = 0; i < playerFleetMembers.size(); i++) {
            FleetMemberAPI member = playerFleetMembers.get(i);
            String hullId = member.getHullId();
            ShipVariantAPI variant = member.getVariant();
            PersonAPI captain = member.getCaptain();

            if (!preset.shipIds.contains(hullId)) {
                allShipsMatched = false;
                break;
            }

            ShipVariantAPI presetVariant = preset.variantsMap.get(i);
            if (presetVariant == null) {
                allShipsMatched = false;
                break;
            }
            boolean variantMatched = false;

            if (areSameVariant(presetVariant, variant)) {
                if (!isOfficerNought(captain) && preset.officersMap.containsKey(i)) {
                    OfficerVariantPair pair = preset.officersMap.get(i);
                    boolean officerMatched = false;

                    if (areSameVariant(pair.variant, variant) && (captain != null && pair.officer.getId().equals(captain.getId()))) {
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
        if (playerFleetMembers.size() != preset.shipIds.size()) {
            return false;
        }

        boolean allShipsMatched = true;
        for (int i = 0; i < playerFleetMembers.size(); i++) {
            FleetMemberAPI member = playerFleetMembers.get(i);
            String hullId = member.getHullId();
            ShipVariantAPI variant = member.getVariant();

            if (!preset.shipIds.contains(hullId)) {
                allShipsMatched = false;
                break;
            }

            ShipVariantAPI presetVariant = preset.variantsMap.get(i);
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

        for (String hullId : preset.shipIds) {
            requiredShips.put(hullId, requiredShips.getOrDefault(hullId, 0) + 1);
        }

        if (playerCurrentFleet != null) {
            for (FleetMemberAPI member : playerCurrentFleet) {
                String hullId = member.getHullId();
                if (!requiredShips.containsKey(hullId)) continue;

                for (ShipVariantAPI presetVariant : preset.variantsMap.values()) {
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

            Collection<ShipVariantAPI> presetVariants = preset.variantsMap.values();
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
        for (FleetMemberWrapper wrappedMember : preset.fleetMembers) {
            if (wrappedMember.id == member.getId()) {
                return true;
            }
        }
        return false;
    }

    public static Map<String, List<FleetMemberWrapper>> findNeededShipsWrappedNonIdMatching(FleetPreset preset, List<FleetMemberAPI> playerCurrentFleet) {
        Map<String, Integer> requiredShips = new HashMap<>();
        Map<String, List<FleetMemberWrapper>> foundShips = new HashMap<>();

        for (String hullId : preset.shipIds) {
            requiredShips.put(hullId, requiredShips.getOrDefault(hullId, 0) + 1);
        }

        if (playerCurrentFleet != null) {
            for (FleetMemberAPI member : playerCurrentFleet) {
                String hullId = member.getHullId();
                if (!requiredShips.containsKey(hullId)) continue;

                for (int i=0; i<preset.variantsMap.size(); i++) {
                    ShipVariantAPI presetVariant = preset.variantsMap.get(i);
                    if (areSameVariant(presetVariant, member.getVariant()) && preset.fleetMembers.get(i).id != member.getId()) {
                        if (foundShips.get(hullId) == null) {
                            foundShips.put(hullId, new ArrayList<>());
                        }
                        foundShips.get(hullId).add(preset.fleetMembers.get(i));
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
        FleetPreset preset = getFleetPresets().get(presetName);
        if (preset == null) return null;
        Map<String, List<FleetMemberWrapper>> neededShips = findNeededShipsWrappedNonIdMatching(preset, Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder());
        if (neededShips.isEmpty()) return null;

        CargoAPI storageCargo = market.getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo();
        initMothballedShips(storageCargo);

        Map<FleetMemberWrapper, FleetMemberAPI> neededMembers = new HashMap<>();

        for (FleetMemberAPI storedMember : storageCargo.getMothballedShips().getMembersInPriorityOrder()) {
            if (neededShips.containsKey(storedMember.getHullId())) {
                if (!isMemberFromPreset(storedMember, preset)) {
                    for (FleetMemberWrapper member : neededShips.get(storedMember.getHullId())) {
                        if (areSameVariant(member.member.getVariant(), storedMember.getVariant())) {
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
            if (wrappedMember.id.equals(memberToCheck.getId())) return true;
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
                if (i == wrappedMember.index) {
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
        for (String hullId: preset.shipIds) {
            for (int i = 0; i < playerFleetMembers.size(); i++) {
                FleetMemberAPI fleetMember = playerFleetMembers.get(i);
                if (hullId.equals(fleetMember.getHullId())) {
                    PersonAPI captain = fleetMember.getCaptain();

                    if (captain != null) {
                        preset.officersMap.put(i, new OfficerVariantPair(captain, fleetMember.getVariant(), i));
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
        for (int i = 0; i < preset.shipIds.size(); i++) {
            OfficerVariantPair pair = preset.officersMap.get(i);
            if (pair != null) {
                PersonAPI officer = pair.officer;
                if (officer.getId() == officerId) return true;
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

        if (areOfficersInPlayerFleet(playerFleetMembers) && preset.officersMap.isEmpty()) {
            assignofficersToPreset(preset, playerFleetMembers);
        } else {
            for (int i = 0; i < playerFleetMembers.size(); i++) {
                FleetMemberAPI fleetMember = playerFleetMembers.get(i);
                OfficerVariantPair pair = preset.officersMap.get(i);

                if (pair != null) {
                    if (areSameVariant(pair.variant, fleetMember.getVariant())) {
                        if (!isOfficerInFleet(pair.officer.getId(), playerFleetMembers)) {
                            preset.officersMap.remove(i);
                            preset.officersMap.put(i, new OfficerVariantPair(fleetMember.getCaptain(), pair.variant, i));
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
        SubmarketAPI storage = market.getSubmarket(Submarkets.SUBMARKET_STORAGE);
        if (storage == null) return null;
        SubmarketPlugin storagePlugin = storage.getPlugin();
        if (!isPlayerPaidForStorage(storagePlugin)) return null;

        CargoAPI storageCargo = storage.getCargo();
        initMothballedShips(storageCargo);

        return storageCargo.getMothballedShips().getMembersInPriorityOrder();
    }

    public static void takeAllShipsFromStorage() {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        MarketAPI market = getPlayerCurrentMarket();
        if (market == null) return;

        SubmarketAPI storage = market.getSubmarket(Submarkets.SUBMARKET_STORAGE);
        SubmarketPlugin storagePlugin = storage.getPlugin();
        if (!isPlayerPaidForStorage(storagePlugin)) return;

        CargoAPI storageCargo = storage.getCargo();
        initMothballedShips(storageCargo);

        FleetDataAPI mothballedShipsFleetData = storageCargo.getMothballedShips();

        for (FleetMemberAPI member : mothballedShipsFleetData.getMembersInPriorityOrder()) {
            mothballedShipsFleetData.removeFleetMember(member);
            playerFleet.getFleetData().addFleetMember(member);
        }

        for (OfficerDataAPI officer : Global.getSector().getPlayerFleet().getFleetData().getOfficersCopy()) {
            if (officer.getPerson().isPlayer()) continue;
            for (FleetMemberAPI member : playerFleet.getFleetData().getMembersInPriorityOrder()) {
                if (member.getCaptain().isPlayer() || !member.getCaptain().getName().getFullName().equals("")) continue;

                member.setCaptain(officer.getPerson());
                break;
            }
        }
        refreshFleetUI();
    }

    public static void storeFleetInStorage() {
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

    // this is because variant1.equals(variant2) doesnt always work, idk why and i dont really care
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
        // print("fittedweaponslots match:", variant1.getFittedWeaponSlots().equals(variant2.getFittedWeaponSlots()));
        // print("smoddedbuiltins match:", variant1.getSModdedBuiltIns().equals(variant2.getSModdedBuiltIns()));
        // print("fluxcapacitors match:", variant1.getNumFluxCapacitors() == variant2.getNumFluxCapacitors());
        // print("fluxvents match:", variant1.getNumFluxVents() == variant2.getNumFluxVents());
        // print("-------------------------------------------------------");

        return (variant1.getHullSpec().getHullId().equals(variant2.getHullSpec().getHullId())
            && variant1.getSMods().equals(variant2.getSMods())
            && variant1.getHullMods().equals(variant2.getHullMods())
            && variant1.getWings().equals(variant2.getWings())
            && variant1.getFittedWeaponSlots().equals(variant2.getFittedWeaponSlots())
            && variant1.getSModdedBuiltIns().equals(variant2.getSModdedBuiltIns())
            // && variant1.getWeaponGroups().equals(variant2.getWeaponGroups()) // fuck you xstream
            && variant1.getNumFluxCapacitors() == variant2.getNumFluxCapacitors()
            && variant1.getNumFluxVents() == variant2.getNumFluxVents());
    }

    public static boolean areSameHullMods(ShipVariantAPI variant1, ShipVariantAPI variant2) {
        return (variant1.getHullMods().equals(variant2.getHullMods()));
    }

    // TODO make D/SMOD AGNOSTIC SETTINGS, NEW HULLS? 
    // WHAT IF PLAYER WANTS VERY SPECIFIC DMOD/OFFICER VARIANTS?
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

        for (int i = 0; i < preset.shipIds.size(); i++) {
            String hullId = preset.shipIds.get(i);
            ShipVariantAPI variant = preset.variantsMap.get(i);

            boolean found = false;
            for (FleetMemberAPI storedMember : storageCargo.getMothballedShips().getMembersInPriorityOrder()) {
                if (storedMember.getHullId().equals(hullId)) {
                    if (preset.officersMap.containsKey(i)) {
                        OfficerVariantPair pair = preset.officersMap.get(i);

                        if (areSameVariant(pair.variant, variant)) {
                            storedMember.setCaptain(pair.officer);

                            storageCargo.getMothballedShips().removeFleetMember(storedMember);
                            membersDone.add(storedMember);
                            // CargoPresetUtils.refit(storedMember, variant, playerCargo, storageCargo);
                            found = true;
                            break;
                        }
                    }
                    
                    if (areSameVariant(variant, storedMember.getVariant())) {
                        storageCargo.getMothballedShips().removeFleetMember(storedMember);
                        membersDone.add(storedMember);
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
        refreshFleetUI();
        // this needs more work and conditional logic with options
        // CargoPresetUtils.equalizeCargo(playerFleetData.getMembersInPriorityOrder(), playerCargo, storageCargo, cargoRatios);

        if (allFound) {
            CampaignUIMessage msg = new CampaignUIMessage(RESTOREMESSAGE_SUCCESS_PREFIX + name, Misc.getPositiveHighlightColor());
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

    public static void addMessagesToCampaignUI() {
        List<CampaignUIMessage> messageQueue = (List<CampaignUIMessage>) Global.getSector().getMemoryWithoutUpdate().get(MESSAGEQUEUE_KEY);
        for (CampaignUIMessage message : messageQueue) {
            Global.getSector().getCampaignUI().addMessage(message.message, message.color);
        }
        messageQueue.clear();
    }

    public static void refreshFleetUI() {
        Object fleetInfoPanel = Global.getSector().getMemoryWithoutUpdate().get(FLEETINFOPANEL_KEY);
        if (fleetInfoPanel == null) return;

        Object infoPanelParent = ReflectionUtilis.invokeMethod("getParent", fleetInfoPanel);
        ReflectionUtilis.getMethodAndInvokeDirectly("recreateUI", ReflectionUtilis.invokeMethod("getFleetPanel", infoPanelParent), 1, true);
    }

    public static void deleteFleetPreset(String name) {
        FleetPreset preset = getFleetPresets().get(name);
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        if (mem.get(UNDOCKED_PRESET_KEY) != null && ((FleetPreset)mem.get(UNDOCKED_PRESET_KEY)).name.equals(name)) mem.unset(UNDOCKED_PRESET_KEY);

        preset.campaignFleet.despawn();
        preset.campaignFleet = null;

        Map<String, List<FleetMemberWrapper>> presetsMembers = getFleetPresetsMembers();
        for (FleetMemberWrapper member : preset.fleetMembers) {
            presetsMembers.get(member.id).remove(member);
        }

        getFleetPresets().remove(name);
    }

    public static void removeOfficerFromPresets(String officerId) {
        Map<String, FleetPreset> presets = getFleetPresets();

        for (FleetPreset fleetPreset : presets.values()) {
            for (int i = 0; i < fleetPreset.shipIds.size(); i++) {        
                fleetPreset.officersMap.remove(i);
            }

            for (FleetMemberWrapper member : fleetPreset.fleetMembers) {
                if (member.captainId != null && member.captainId.equals(officerId)) {
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

            for (Map.Entry<Integer, ShipVariantAPI> variantsEntry : fleetPreset.variantsMap.entrySet()) {
                ShipVariantAPI variant = variantsEntry.getValue();

                String name = variant.getHullSpec().getHullName();
                shipCountMap.put(name, shipCountMap.getOrDefault(name, 0) + 1);
                shipHullSizes.put(name, variant.getHullSize());
                shipHullSpecs.put(name, variant.getHullSpec());
            }

            String ships = createShipCountString(shipCountMap, shipHullSizes, shipHullSpecs, shipOrder);
            map.put(fleetPresetName, ships);
        }
        return PresetMiscUtils.sortByKeyAlphanumerically(map, ascendingNames);
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