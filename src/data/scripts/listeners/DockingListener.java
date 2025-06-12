package data.scripts.listeners;

import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.*;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;

import data.scripts.ClassRefs;
import data.scripts.util.CargoPresetUtils;
import data.scripts.util.PresetMiscUtils;
import data.scripts.util.PresetUtils;
import data.scripts.util.PresetUtils.FleetPreset;
import data.scripts.util.ReflectionUtilis;

import assortment_of_things.frontiers.data.FrontiersData;

import data.scripts.listeners.ColonyDecivStorageListener;

import java.util.*;

public class DockingListener extends BaseCampaignEventListener {
    private void print(Object... args) {
        PresetMiscUtils.print(args);
    }

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
        // print(market.getName());
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

        if ((PresetUtils.haveNexerelin || PresetUtils.haveRAT) && dialog.getPlugin() instanceof RuleBasedInteractionDialogPluginImpl) {
            RuleBasedInteractionDialogPluginImpl newPlugin = new RuleBasedInteractionDialogPluginImpl() {
                @Override
                public void optionSelected(String arg0, Object arg1) {
                    super.optionSelected(arg0, arg1);

                    switch(String.valueOf(arg1)) {
                        case "nex_outpostBuildStart":
                            reportPlayerOpenedMarket(dialog.getInteractionTarget().getMarket());
                            return;
                        
                        case "nex_outpostDismantleConfirm":
                            Global.getSector().getListenerManager().getListeners(ColonyDecivStorageListener.class).get(0).reportColonyDecivilized(dialog.getInteractionTarget().getMarket(), true);
                            return;

                        case "ratCreateSettlement":
                            return;

                        case "ratVisitSettlement":
                            reportPlayerOpenedMarket(((FrontiersData)Global.getSector().getMemoryWithoutUpdate().get("$rat_frontiers_data")).getActiveSettlement().getSettlementEntity().getMarket());
                            return;
                        default:
                            return;
                    }
                }
            };
            dialog.setPlugin(newPlugin);
            newPlugin.init(dialog);
        }
    }
}