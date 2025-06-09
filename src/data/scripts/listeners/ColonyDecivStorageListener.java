package data.scripts.listeners;

import java.util.Set;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.listeners.ColonyDecivListener;

import data.scripts.util.PresetUtils;

public class ColonyDecivStorageListener implements ColonyDecivListener {
    @Override
    public void reportColonyDecivilized(MarketAPI market, boolean fullyDestroyed) {
        if (fullyDestroyed) {
            Set<String> storedMembers = PresetUtils.getStoredFleetPresetsMemberIds().get(market.getName());

            if (storedMembers != null) {
                PresetUtils.getStoredFleetPresetsMemberIds().remove(market.getName());
                PresetUtils.cleanUpPerishedPresetMembers();
            }
        }
    }
    @Override public void reportColonyAboutToBeDecivilized(MarketAPI arg0, boolean arg1) {}
}