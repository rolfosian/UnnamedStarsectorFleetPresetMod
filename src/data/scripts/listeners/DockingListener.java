package data.scripts.listeners;

import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.characters.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;

import org.apache.log4j.Logger;

public class DockingListener implements CampaignEventListener {
    private static final Logger logger = Logger.getLogger(DockingListener.class);

    public static boolean isPlayerDocked() {
        return Global.getSector().getMemoryWithoutUpdate().getBoolean("$playerDocked");
    }

    @Override
    public void reportPlayerOpenedMarket(MarketAPI market) {
        Global.getSector().getMemoryWithoutUpdate().set("$playerDocked", true);
    }

    @Override
    public void reportPlayerClosedMarket(MarketAPI market) {
        Global.getSector().getMemoryWithoutUpdate().unset("$playerDocked");
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

    public void reportShownInteractionDialog(InteractionDialogAPI dialog) {}

}
