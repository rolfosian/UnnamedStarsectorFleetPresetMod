package data.scripts.listeners;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OrbitalStationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;

import data.scripts.util.CargoPresetUtils;
import data.scripts.util.PresetMiscUtils;
import data.scripts.util.PresetUtils;
import data.scripts.util.PresetUtils.FleetPreset;

import assortment_of_things.frontiers.data.FrontiersData;
import assortment_of_things.frontiers.SettlementData;
import assortment_of_things.frontiers.interactions.SettlementInteraction;

import java.util.*;

public class DockingListener extends BaseCampaignEventListener {
    final DockingListener self = this;

    private boolean isBattle = false;
    private boolean isHostileTimeout = false;

    public DockingListener() {
        super(false);
    }
    public static final String PLAYERCURRENTMARKET_KEY = PresetUtils.PLAYERCURRENTMARKET_KEY;
    public static final String ISPLAYERPAIDFORSTORAGE_KEY = PresetUtils.ISPLAYERPAIDFORSTORAGE_KEY;

    public boolean canPlayerAccessStorage(MarketAPI market) {
        return (market != null && CargoPresetUtils.getStorageSubmarket(market) != null && PresetUtils.isPlayerPaidForStorage(CargoPresetUtils.getStorageSubmarket(market).getPlugin()) && !isHostileTimeout);
    }

    public MarketAPI getPlayerCurrentMarket() {
        return (MarketAPI) Global.getSector().getMemoryWithoutUpdate().get(PLAYERCURRENTMARKET_KEY);
    }

    @Override
    public void reportPlayerOpenedMarket(MarketAPI market) {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        mem.set(PLAYERCURRENTMARKET_KEY, market);
        mem.unset(PresetUtils.UNDOCKED_PRESET_KEY);
    }

    private void setUndockedPreset() {
        FleetPreset preset = PresetUtils.getPresetOfMembers(Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy());
        if (preset != null) {
            Global.getSector().getMemoryWithoutUpdate().set(PresetUtils.UNDOCKED_PRESET_KEY, preset);
        } else {
            Global.getSector().getMemoryWithoutUpdate().unset(PresetUtils.UNDOCKED_PRESET_KEY);
        }
    }

    public void setUndockedPreset(String presetName) {
        if (presetName == null) {
            Global.getSector().getMemoryWithoutUpdate().unset(PresetUtils.UNDOCKED_PRESET_KEY);
            return;
        }

        FleetPreset preset = PresetUtils.getFleetPresets().get(presetName);
        if (preset != null) {
            Global.getSector().getMemoryWithoutUpdate().set(PresetUtils.UNDOCKED_PRESET_KEY, preset);
        } else {
            Global.getSector().getMemoryWithoutUpdate().unset(PresetUtils.UNDOCKED_PRESET_KEY);
        }
    }

    @Override
    public void reportPlayerClosedMarket(MarketAPI market) {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        mem.unset(PLAYERCURRENTMARKET_KEY);
        mem.unset(ISPLAYERPAIDFORSTORAGE_KEY);
        setUndockedPreset();
    }

    private boolean isHostileTimeout(InteractionDialogAPI dialog) {
        if (dialog == null) return false;
    
        InteractionDialogPlugin plugin = dialog.getPlugin();
        if (plugin == null) return false;
    
        Map<String, MemoryAPI> memoryMap = plugin.getMemoryMap();
        if (memoryMap == null) return false;
    
        MemoryAPI marketMemory = memoryMap.get("market");
        if (marketMemory == null) return false;
    
        return marketMemory.get("$playerHostileTimeout") != null;
    }

    @Override
    public void reportShownInteractionDialog(InteractionDialogAPI dialog) {
        if (dialog.getInteractionTarget() == null || dialog.getInteractionTarget().getMarket() == null) return;
        MarketAPI originalMarket = dialog.getInteractionTarget().getMarket();
        boolean isSettlement = false;

        isHostileTimeout = isHostileTimeout(dialog);

        if (originalMarket.getName().endsWith(" Settlement") && !(dialog.getPlugin() instanceof RuleBasedInteractionDialogPluginImpl)) {
            reportPlayerClosedMarket(((FrontiersData)Global.getSector().getMemoryWithoutUpdate().get("$rat_frontiers_data")).getActiveSettlement().getPrimaryPlanet().getMarket());
            reportPlayerOpenedMarket(originalMarket);
            isSettlement = true;
        }

        new OptionPanelListener(dialog) {
            // private RunningMembers runningMembers = new RunningMembers(Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()); // TODO?
            private SettlementData settlement = null;

            @Override
            public void afterOptionSelected(Object optionData) {
                // print(optionData);

                switch (String.valueOf(optionData)) {
                    case "nex_outpostBuildStart":
                        reportPlayerClosedMarket(originalMarket);
                        reportPlayerOpenedMarket(dialog.getInteractionTarget().getMarket());
                        return;

                    case "nex_outpostDismantleConfirm":
                        return;

                    case "ratVisitSettlement":
                        if (this.settlement == null) this.settlement = ((FrontiersData)Global.getSector().getMemoryWithoutUpdate().get("$rat_frontiers_data")).getActiveSettlement();
                        reportPlayerClosedMarket(originalMarket);
                        reportPlayerOpenedMarket(this.settlement.getSettlementEntity().getMarket()); // ((SettlementData)settlement)
                        return;
                    
                    case "Descend towards the settlement":
                        if (this.settlement == null) this.settlement = ((FrontiersData)Global.getSector().getMemoryWithoutUpdate().get("$rat_frontiers_data")).getActiveSettlement();
                        reportPlayerClosedMarket(originalMarket);
                        reportPlayerOpenedMarket(this.settlement.getSettlementEntity().getMarket());
                        return;
                    
                    case "Back":
                        if (this.settlement != null) {
                            reportPlayerClosedMarket(this.settlement.getSettlementEntity().getMarket());

                            isHostileTimeout = isHostileTimeout(dialog);

                            if (!this.settlement.getAutoDescend()) {
                                reportPlayerOpenedMarket(this.settlement.getPrimaryPlanet().getMarket());
                            }
                        }

                    case "Manage Settlement":
                        return;

                    case "mktRaidConfirm":
                        reportPlayerClosedMarket(originalMarket);
                        return;

                    case "mktBombardConfirm":
                        reportPlayerClosedMarket(originalMarket);
                        return;
                        
                    case "nex_mktInvadeConfirm":
                        reportPlayerClosedMarket(originalMarket);
                        return;

                    case "CONTINUE_INTO_BATTLE":
                        isBattle = true;
                        return;

                    case "mktEngage":
                        // this.runningMembers = new RunningMembers(Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()); // TODO
                        reportPlayerClosedMarket(originalMarket);
                        return;

                    case"RECOVERY_CONTINUE":
                        if (isBattle) {
                            isBattle = false;
                            // PresetUtils.checkFleetAgainstPreset(runningMembers);
                        }
                        return;
                    
                    case "CONTINUE_LOOT":
                        if (isBattle) {
                            isBattle = false;
                            // PresetUtils.checkFleetAgainstPreset(runningMembers);
                        }
                        return;
                        
                    case "LEAVE":
                        if (isBattle) {
                            isBattle = false;
                            // PresetUtils.checkFleetAgainstPreset(runningMembers);
                        }
                        return;
                    default:
                        return;
                }
            }

            public void init(boolean isSettlement) {
                if (isSettlement) {
                    this.settlement = ((SettlementInteraction)dialog.getPlugin()).getData();
                }
            }
        }.init(isSettlement);
    }
}