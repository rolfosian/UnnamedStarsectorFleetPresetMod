package data.scripts.plugins;

import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;

public class DummyFleetEncounterContextPlugin implements FleetEncounterContextPlugin {
    @Override public boolean adjustPlayerReputation(InteractionDialogAPI arg0, String arg1) { return false; }
    @Override public float computePlayerContribFraction() { return 0.0f; }
    @Override public BattleAPI getBattle() { return null; }
    @Override public DataForEncounterSide getDataFor(CampaignFleetAPI arg0) { return null; }
    @Override public DisengageHarryAvailability getDisengageHarryAvailability(CampaignFleetAPI arg0, CampaignFleetAPI arg1) { return DisengageHarryAvailability.NO_READY_SHIPS; }
    @Override public EngagementOutcome getLastEngagementOutcome() { return EngagementOutcome.BATTLE_PLAYER_WIN; }
    @Override public CampaignFleetAPI getLoser() { return null; }
    @Override public DataForEncounterSide getLoserData() { return null; }
    @Override public PursueAvailability getPursuitAvailability(CampaignFleetAPI arg0, CampaignFleetAPI arg1) { return PursueAvailability.NO_READY_SHIPS; }
    @Override public CampaignFleetAPI getWinner() { return null; }
    @Override public DataForEncounterSide getWinnerData() { return null; }
    @Override public boolean isEngagedInHostilities() { return false; }
    @Override public boolean isOtherFleetHarriedPlayer() { return false; }
    @Override public float performPostVictoryRecovery(EngagementResultAPI arg0) { return 0.0f; }
    @Override public void setOtherFleetHarriedPlayer(boolean arg0) { }
}
