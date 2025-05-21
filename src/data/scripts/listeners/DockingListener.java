package data.scripts.listeners;

import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;

import data.scripts.util.PresetUtils;
import data.scripts.util.PresetUtils.FleetPreset;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class DockingListener implements CampaignEventListener {
        public static final Logger logger = Logger.getLogger(DockingListener.class);

    public static final String PLAYERCURRENTMARKET_KEY = PresetUtils.PLAYERCURRENTMARKET_KEY;
    public static final String ISPLAYERPAIDFORSTORAGE_KEY = PresetUtils.ISPLAYERPAIDFORSTORAGE_KEY;

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

        FleetDataAPI playerFleetData = Global.getSector().getPlayerFleet().getFleetData();
        if (PresetUtils.isPlayerFleetAPreset(playerFleetData.getMembersInPriorityOrder())) {
            mem.set(PresetUtils.UNDOCKED_PRESET_KEY, playerFleetData);
        }
    }

    @Override
    public void reportBattleFinished(CampaignFleetAPI primaryWinner, BattleAPI battle) {
        if (battle.isPlayerInvolved()) {
            PresetUtils.afterBattleFinished();
        }
    }

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

    public void reportShownInteractionDialog(InteractionDialogAPI dialog) {}

}
