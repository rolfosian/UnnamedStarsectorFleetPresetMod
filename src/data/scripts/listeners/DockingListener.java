package data.scripts.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;

import data.scripts.ClassRefs;
import data.scripts.util.CargoPresetUtils;
import data.scripts.util.PresetMiscUtils;
import data.scripts.util.PresetUtils;
import data.scripts.util.PresetUtils.FleetPreset;
import data.scripts.util.ReflectionUtilis;

import data.scripts.rat_frontiers_interactions.WrappedCreateSettlementInteraction;
import data.scripts.rat_frontiers_interactions.WrappedSettlementInteraction;
import assortment_of_things.frontiers.data.FrontiersData;
import assortment_of_things.frontiers.SettlementData;
import assortment_of_things.frontiers.interactions.CreateSettlementInteraction;
import assortment_of_things.frontiers.interactions.SettlementInteraction;

import java.util.*;

public class DockingListener extends BaseCampaignEventListener {
    private void print(Object... args) {
        PresetMiscUtils.print(args);
    }

    final DockingListener self = this;

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

    @Override
    public void reportPlayerClosedMarket(MarketAPI market) {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        mem.unset(PLAYERCURRENTMARKET_KEY);
        mem.unset(PresetUtils.ISPLAYERPAIDFORSTORAGE_KEY);
        
        PresetUtils.addMessagesToCampaignUI();

        FleetPreset preset = PresetUtils.getPresetOfMembers(Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder());
        if (preset != null) {
            mem.set(PresetUtils.UNDOCKED_PRESET_KEY, preset);
        } else {
            mem.unset(PresetUtils.UNDOCKED_PRESET_KEY);
        }
    }

    @Override
    public void reportShownInteractionDialog(InteractionDialogAPI dialog) {
        if (!(dialog.getInteractionTarget() instanceof PlanetAPI)) return;

        if (((PresetUtils.nexerelinVersion != null && !PresetMiscUtils.isVersionAfter(PresetUtils.nexerelinVersion, "0.12.0b")) // Backwards compatibility for these mods (up to what point before i do not know and cannot be bothered finding out)
            || (PresetUtils.RATVersion != null && PresetMiscUtils.isVersionAfter(PresetUtils.RATVersion, "3.0.9"))) 
            && dialog.getPlugin() instanceof RuleBasedInteractionDialogPluginImpl) {
            
            MarketAPI originalMarket = dialog.getInteractionTarget().getMarket();
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

                        default:
                            return;
                    }
                }
            };
            ReflectionUtilis.transplant(dialog.getPlugin(), newPlugin);
            dialog.setPlugin(newPlugin);
        }
    }
}