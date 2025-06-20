package data.scripts.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OrbitalStationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;

import data.scripts.ClassRefs;
import data.scripts.util.CargoPresetUtils;
import data.scripts.util.PresetMiscUtils;
import data.scripts.util.PresetUtils;
import data.scripts.util.PresetUtils.FleetPreset;
import data.scripts.util.PresetUtils.RunningMembers;
import data.scripts.util.ReflectionUtilis;

import data.scripts.interactions.WrappedCreateSettlementInteraction;
import data.scripts.interactions.WrappedSettlementInteraction;

import assortment_of_things.frontiers.data.FrontiersData;
import assortment_of_things.frontiers.SettlementData;
import assortment_of_things.frontiers.interactions.CreateSettlementInteraction;
import assortment_of_things.frontiers.interactions.SettlementInteraction;

import exerelin.campaign.battle.NexFleetInteractionDialogPluginImpl;

import java.util.*;

public class DockingListener extends BaseCampaignEventListener {
    private void print(Object... args) {
        PresetMiscUtils.print(args);
    }

    final DockingListener self = this;

    private boolean isBattle = false;

    public DockingListener(boolean permaRegister) {
        super(permaRegister);
    }
    public static final String PLAYERCURRENTMARKET_KEY = PresetUtils.PLAYERCURRENTMARKET_KEY;
    public static final String ISPLAYERPAIDFORSTORAGE_KEY = PresetUtils.ISPLAYERPAIDFORSTORAGE_KEY;

    public static boolean canPlayerAccessStorage(MarketAPI market) {
        return (market != null && CargoPresetUtils.getStorageSubmarket(market) != null && PresetUtils.isPlayerPaidForStorage(CargoPresetUtils.getStorageSubmarket(market).getPlugin()));
    }

    public static MarketAPI getPlayerCurrentMarket() {
        return (MarketAPI) Global.getSector().getMemoryWithoutUpdate().get(PresetUtils.PLAYERCURRENTMARKET_KEY);
    }

    @Override
    public void reportPlayerOpenedMarket(MarketAPI market) {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        mem.set(PLAYERCURRENTMARKET_KEY, market);
        mem.unset(PresetUtils.UNDOCKED_PRESET_KEY);
    }

    private void setUndockedPreset() {
        FleetPreset preset = PresetUtils.getPresetOfMembers(Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder());
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
        mem.unset(PresetUtils.ISPLAYERPAIDFORSTORAGE_KEY);
        
        PresetUtils.addMessagesToCampaignUI();
        setUndockedPreset();
    }

    // @Override
    // public void reportPlayerEngagement(EngagementResultAPI result) {
    //     if (getPlayerCurrentMarket() != null) {
    //         MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
    //         mem.unset(PLAYERCURRENTMARKET_KEY);
    //         mem.unset(PresetUtils.ISPLAYERPAIDFORSTORAGE_KEY);
    //     }
    // }

    @Override
    public void reportShownInteractionDialog(InteractionDialogAPI dialog) {
        // if (dialog.getInteractionTarget() == null || dialog.getInteractionTarget().getMarket() == null || CargoPresetUtils.getStorageSubmarket(dialog.getInteractionTarget().getMarket()) == null) return;
        if (dialog.getInteractionTarget() == null) return;
        new OptionPanelListener(dialog) {
            @Override
            public void onOptionSelected(Object optionData) {
                    print(optionData);
            }
        };

        // if (((PresetUtils.nexerelinVersion != null && !PresetMiscUtils.isVersionAfter(PresetUtils.nexerelinVersion, "0.12.0b")) // Backwards compatibility for these mods (up to what point before i do not know and cannot be bothered finding out)
        //     || (PresetUtils.RATVersion != null && !PresetMiscUtils.isVersionAfter(PresetUtils.RATVersion, "3.0.9"))) 
            // && dialog.getPlugin() instanceof RuleBasedInteractionDialogPluginImpl) {
        
        if (dialog.getPlugin() instanceof RuleBasedInteractionDialogPluginImpl && (false)) {
            MarketAPI originalMarket = dialog.getInteractionTarget().getMarket();
            RuleBasedInteractionDialogPluginImpl oldPlugin = (RuleBasedInteractionDialogPluginImpl) dialog.getPlugin();

            RuleBasedInteractionDialogPluginImpl newPlugin = new RuleBasedInteractionDialogPluginImpl() {
                @Override
                public void optionSelected(String arg0, Object arg1) {
                    super.optionSelected(arg0, arg1);

                    switch(String.valueOf(arg1)) {
                        case "nex_outpostBuildStart":
                            reportPlayerClosedMarket(originalMarket);
                            reportPlayerOpenedMarket(dialog.getInteractionTarget().getMarket());
                            return;
                        
                        case "nex_outpostDismantleConfirm":
                            Global.getSector().getListenerManager().getListeners(ColonyAbandonListener.class).get(0).reportPlayerAbandonedColony(dialog.getInteractionTarget().getMarket());
                            return;

                        case "ratCreateSettlement":
                            WrappedCreateSettlementInteraction newNewPlugin = new WrappedCreateSettlementInteraction((CreateSettlementInteraction)Global.getSector().getCampaignUI().getCurrentInteractionDialog().getPlugin(), self);

                            ReflectionUtilis.transplant(Global.getSector().getCampaignUI().getCurrentInteractionDialog().getPlugin(), newNewPlugin);
                            Global.getSector().getCampaignUI().getCurrentInteractionDialog().setPlugin(newNewPlugin);
                            return;

                        case "ratVisitSettlement":
                            SettlementData settlementData = ((FrontiersData)Global.getSector().getMemoryWithoutUpdate().get("$rat_frontiers_data")).getActiveSettlement();
                            reportPlayerClosedMarket(originalMarket);
                            reportPlayerOpenedMarket(settlementData.getSettlementEntity().getMarket());

                            WrappedSettlementInteraction newNewPlugin_ = new WrappedSettlementInteraction((SettlementInteraction)Global.getSector().getCampaignUI().getCurrentInteractionDialog().getPlugin(), self);
                            ReflectionUtilis.transplant(Global.getSector().getCampaignUI().getCurrentInteractionDialog().getPlugin(), newNewPlugin_);
                            newNewPlugin_.setData(settlementData); // just in case

                            Global.getSector().getCampaignUI().getCurrentInteractionDialog().setPlugin(newNewPlugin_);
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

                        // engage replaces the plugin with a fleet interaction plugin
                        case "mktEngage":
                            InteractionDialogPlugin plugin = Global.getSector().getCampaignUI().getCurrentInteractionDialog().getPlugin();
                            RunningMembers runningMembers = new RunningMembers(Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder());

                            if (plugin.getClass().equals(FleetInteractionDialogPluginImpl.class)) {
                                
                                FleetInteractionDialogPluginImpl newNewPlugin__ = new FleetInteractionDialogPluginImpl() {
                                    @Override
                                    public void optionSelected(String arg0, Object arg1) {
                                        if (String.valueOf(arg1).equals("CONTINUE_INTO_BATTLE")) {
                                            isBattle = true;
                                            reportPlayerClosedMarket(originalMarket);
                                        }

                                        super.optionSelected(arg0, arg1);

                                        if (isBattle && (String.valueOf(arg1).equals("RECOVERY_CONTINUE") || String.valueOf(arg1).equals("LEAVE"))) {
                                            isBattle = false;
                                            reportShownInteractionDialog(Global.getSector().getCampaignUI().getCurrentInteractionDialog());
                                            PresetUtils.checkFleetAgainstPreset(runningMembers);
                                        }
                                    }
                                };

                                ReflectionUtilis.transplant(Global.getSector().getCampaignUI().getCurrentInteractionDialog().getPlugin(), newNewPlugin__);
                                Global.getSector().getCampaignUI().getCurrentInteractionDialog().setPlugin(newNewPlugin__);

                            } else if (PresetUtils.nexerelinVersion != null && plugin.getClass().equals(NexFleetInteractionDialogPluginImpl.class)) {

                                FleetInteractionDialogPluginImpl newNewPlugin__ = new NexFleetInteractionDialogPluginImpl() {
                                    @Override
                                    public void optionSelected(String arg0, Object arg1) {
                                        if (String.valueOf(arg1).equals("CONTINUE_INTO_BATTLE")) {
                                            isBattle = true;
                                            reportPlayerClosedMarket(originalMarket);
                                        }

                                        super.optionSelected(arg0, arg1);

                                        if (isBattle && (String.valueOf(arg1).equals("RECOVERY_CONTINUE") || String.valueOf(arg1).equals("LEAVE"))) {
                                            isBattle = false;
                                            reportShownInteractionDialog(Global.getSector().getCampaignUI().getCurrentInteractionDialog());
                                            PresetUtils.checkFleetAgainstPreset(runningMembers);
                                        }
                                    }
                                };

                                ReflectionUtilis.transplant(Global.getSector().getCampaignUI().getCurrentInteractionDialog().getPlugin(), newNewPlugin__);
                                Global.getSector().getCampaignUI().getCurrentInteractionDialog().setPlugin(newNewPlugin__);
                            }
                        default:
                            return;
                    }
                }
            };
            ReflectionUtilis.transplant(oldPlugin, newPlugin);
            dialog.setPlugin(newPlugin);
        }
    }
}