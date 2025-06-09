package data.scripts.listeners;

import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.*;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;

import data.scripts.ClassRefs;
import data.scripts.util.PresetUtils;
import data.scripts.util.PresetUtils.FleetPreset;
import data.scripts.util.ReflectionUtilis;

import java.util.*;

public class DockingListener extends BaseCampaignEventListener {
    public DockingListener(boolean permaRegister) {
        super(permaRegister);
    }
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

        FleetPreset preset = PresetUtils.getPresetOfMembers(Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder());
        if (preset != null) {
            mem.set(PresetUtils.UNDOCKED_PRESET_KEY, preset);
        } else {
            mem.unset(PresetUtils.UNDOCKED_PRESET_KEY);
        }
    }
}
