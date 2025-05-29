package data.scripts.listeners;

import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.campaign.fleet.CampaignFleet;

import data.scripts.ClassRefs;
import data.scripts.plugins.DummyFleetEncounterContextPlugin;
import data.scripts.util.PresetUtils;
import data.scripts.util.PresetUtils.FleetPreset;
import data.scripts.util.ReflectionUtilis;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

public class DockingListener implements CampaignEventListener {
    public static final String PLAYERCURRENTMARKET_KEY = PresetUtils.PLAYERCURRENTMARKET_KEY;
    public static final String ISPLAYERPAIDFORSTORAGE_KEY = PresetUtils.ISPLAYERPAIDFORSTORAGE_KEY;

    public static void print(Object... args) {
        PresetUtils.print(args);
    }

    public static boolean canPlayerAccessStorage(MarketAPI market) {
        return (market != null && PresetUtils.isPlayerPaidForStorage(market.getSubmarket(Submarkets.SUBMARKET_STORAGE).getPlugin()));
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

        FleetPreset preset = PresetUtils.getPresetOfPlayerFleet();
        if (preset != null) {
            mem.set(PresetUtils.UNDOCKED_PRESET_KEY, preset);
        } else {
            mem.unset(PresetUtils.UNDOCKED_PRESET_KEY);
        }
    }

    @Override @SuppressWarnings("unchecked") // for VisualPanel fleet info panel class for ClassRefs
    public void reportShownInteractionDialog(InteractionDialogAPI dialog) {
        if (ClassRefs.foundAllClasses() || !String.valueOf(dialog.getPlugin().getContext()).equals("") || 
            Global.getSector().getMemoryWithoutUpdate().get(PresetUtils.VISUALFLEETINFOPANEL_KEY) != null) return;

        Class<?>[] targetConstructorParams = new Class<?>[] {
            String.class,
            CampaignFleet.class,
            String.class,
            CampaignFleet.class,
            FleetEncounterContextPlugin.class,
            boolean.class
        };

        VisualPanelAPI visualPanel = dialog.getVisualPanel();
        visualPanel.showFleetInfo("", Global.getSector().getPlayerFleet(), null, null);

        for (Object child : (List<Object>) ReflectionUtilis.getMethodAndInvokeDirectly("getChildrenNonCopy", visualPanel, 0)) {
            if (UIPanelAPI.class.isAssignableFrom(child.getClass())) {
                try {
                    if (ReflectionUtilis.doInstantiationParamsMatch(child.getClass().getCanonicalName(), targetConstructorParams)) {
                        Global.getSector().getMemoryWithoutUpdate().set(PresetUtils.VISUALFLEETINFOPANEL_KEY, child.getClass());
                        dialog.dismiss();
                        return;
                    }
                } catch (Exception ignore) {}
            }
        }
        dialog.dismiss();
    }

    public void reportBattleFinished(CampaignFleetAPI primaryWinner, BattleAPI battle) {}
    public void reportBattleOccurred(CampaignFleetAPI primaryWinner, BattleAPI battle) {}
    public void reportEconomyMonthEnd() {}
    public void reportEconomyTick(int iterIndex) {}
    public void reportEncounterLootGenerated(FleetEncounterContextPlugin plugin, CargoAPI loot) {}
    public void reportFleetDespawned(CampaignFleetAPI fleet, CampaignEventListener.FleetDespawnReason reason, Object param) {}
    public void reportFleetJumped(CampaignFleetAPI fleet, SectorEntityToken from, JumpPointAPI.JumpDestination to) {}
    public void reportFleetReachedEntity(CampaignFleetAPI fleet, SectorEntityToken entity) {}
    public void reportFleetSpawned(CampaignFleetAPI fleet) {}
    public void reportPlayerActivatedAbility(AbilityPlugin ability, Object param) {}
    public void reportPlayerDeactivatedAbility(AbilityPlugin ability, Object param) {}
    public void reportPlayerDidNotTakeCargo(CargoAPI cargo) {}
    public void reportPlayerDumpedCargo(CargoAPI cargo) {}
    public void reportPlayerEngagement(EngagementResultAPI result) {}
    public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {}
    public void reportPlayerOpenedMarketAndCargoUpdated(MarketAPI market) {}
    public void reportPlayerReputationChange(PersonAPI person, float delta) {}
    public void reportPlayerReputationChange(String faction, float delta) {}
}
