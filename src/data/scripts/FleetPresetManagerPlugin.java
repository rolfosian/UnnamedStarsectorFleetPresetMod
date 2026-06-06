package data.scripts;

import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import data.scripts.util.BaseEveryFrameScript;
import data.scripts.util.PresetMiscUtils;
import data.scripts.util.PresetUtils;
import data.scripts.util.UiUtil;

import data.scripts.util.PresetUtils.FleetPreset;

import data.scripts.listeners.DockingListener;
import data.scripts.listeners.FleetMonitor;
import data.scripts.listeners.OfficerTracker;

import java.util.*;

public class FleetPresetManagerPlugin extends BaseModPlugin {
    private static final String ver = "0.1.2";

    public static FleetPanelInjector fleetPanelInjector;

    @Override
    public void onApplicationLoad() {
        UiUtil.init();
        // ClassRefs.findAllClasses();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        SectorAPI sector = Global.getSector();
        MemoryAPI mem = sector.getMemoryWithoutUpdate();
        Map<String, Object> persistentData = sector.getPersistentData();

        String modVer = (String) persistentData.get("$fleetPresetsManagerVer");
        if (modVer == null || !modVer.equals(ver)) {
            persistentData.put(PresetUtils.PRESETS_MEMORY_KEY, new HashMap<String, FleetPreset>());
            // persistentData.put(PresetUtils.IS_AUTO_UPDATE_KEY, true);
            // persistentData.put(PresetUtils.KEEPCARGORATIOS_KEY, false);
            persistentData.put("$fleetPresetsManagerVer", ver);
        }
        mem.unset(PresetUtils.UNDOCKED_PRESET_KEY);

        FleetPreset activePreset = PresetUtils.getPresetOfMembers(sector.getPlayerFleet().getFleetData().getMembersListCopy());
        if (activePreset != null &&(boolean)persistentData.get(PresetUtils.IS_AUTO_UPDATE_KEY)) {
            sector.getMemoryWithoutUpdate().set(PresetUtils.UNDOCKED_PRESET_KEY, activePreset);
        }
        PresetUtils.updatePresetVariants(); // HAVE TO DO IT HERE TOO NOW FOR NO REASON??? SCOPE ABSOLUTELY MANGLED EVEN MORE DUE TO NEW CLASSES IN THE CODEBASE?? WTF???

        sector.addTransientScript(new BaseEveryFrameScript(true) { // wait 1 frame for core ui to exist
            @Override
            public void advance(float arg0) {
                sector.addTransientScript(fleetPanelInjector = new FleetPanelInjector());
                sector.addTransientScript(new OfficerTracker());
                sector.addTransientScript(new FleetMonitor());

                DockingListener dockingListener;
                sector.addTransientListener(dockingListener = new DockingListener());
                fleetPanelInjector.init(dockingListener);

                sector.removeTransientScript(this);
            }
        });
    }

    @Override
    public void onNewGame() {
        Map<String, Object> persistentData = Global.getSector().getPersistentData();

        persistentData.put(PresetUtils.PRESETS_MEMORY_KEY, new HashMap<String, FleetPreset>());
        // persistentData.put(PresetUtils.IS_AUTO_UPDATE_KEY, true);
        // persistentData.put(PresetUtils.KEEPCARGORATIOS_KEY, false);
        persistentData.put("$fleetPresetsManagerVer", ver);
    }

    @Override
    public void beforeGameSave() {}

    @Override // no idea what save does to variant reference scope but it fucks it up all in kinds of ways, still needs mitigations for Collections.equals even after doing this
    public void afterGameSave() {
        PresetUtils.updatePresetVariants();
    }
}