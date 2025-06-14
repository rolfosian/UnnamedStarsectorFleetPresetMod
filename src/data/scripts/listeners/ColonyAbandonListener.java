package data.scripts.listeners;

import java.util.Set;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.PlayerColonizationListener;

import data.scripts.util.PresetUtils;

public class ColonyAbandonListener implements PlayerColonizationListener {

    @Override
    public void reportPlayerAbandonedColony(MarketAPI market) {
        Set<String> storedMembers = PresetUtils.getStoredFleetPresetsMemberIds().get(market.getName());

        if (storedMembers != null) {
            PresetUtils.getStoredFleetPresetsMemberIds().remove(market.getName());
            PresetUtils.cleanUpPerishedPresetMembers();
        }
    }

    @Override
    public void reportPlayerColonizedPlanet(PlanetAPI arg0) {}
}
