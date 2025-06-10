// Code taken and modified from Officer Extension mod

package data.scripts;


import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import data.scripts.util.PresetMiscUtils;
import data.scripts.util.PresetUtils;
import data.scripts.util.ReflectionUtilis;
import data.scripts.util.PresetUtils.FleetPreset;
import data.scripts.util.PresetUtils.FleetMemberWrapper;
import data.scripts.util.UtilReflection;
import data.scripts.CustomConsole.CustomConsoleAppender;
import data.scripts.FleetPresetManagerCoreScript;
import data.scripts.listeners.ColonyDecivStorageListener;
import data.scripts.listeners.DockingListener;
import data.scripts.listeners.FleetMonitor;
import data.scripts.listeners.OfficerTracker;

import java.util.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

public class FleetPresetManagerPlugin extends BaseModPlugin {
    public static void print(Object... args) {
        PresetMiscUtils.print(args);
    }
    private static final String ver = "0.0.5";

    @Override
    public void onApplicationLoad() {
        CustomConsoleAppender consoleAppender = new CustomConsoleAppender();
        consoleAppender.setLayout(new PatternLayout("%d{HH:mm:ss} %-5p - %m%n"));
        Logger.getRootLogger().addAppender(consoleAppender);
    }
    
    @Override
    public void onGameLoad(boolean newGame) {
        SectorAPI sector = Global.getSector();
        MemoryAPI mem = sector.getMemoryWithoutUpdate();
        Map<String, Object> persistentData = sector.getPersistentData();

        String modVer = (String) persistentData.get("$fleetPresetsManagerVer");
        if (modVer == null || !modVer.equals(ver)) {
            persistentData.put(PresetUtils.PRESETS_MEMORY_KEY, new HashMap<String, FleetPreset>());
            persistentData.put(PresetUtils.PRESET_MEMBERS_KEY, new HashMap<String, List<FleetMemberWrapper>>());
            persistentData.put(PresetUtils.IS_AUTO_UPDATE_KEY, true);
            persistentData.put(PresetUtils.STORED_PRESET_MEMBERIDS_KEY, new HashMap<>());
            persistentData.put(PresetUtils.KEEPCARGORATIOS_KEY, false);
            persistentData.put("$fleetPresetsManagerVer", ver);
        }
        mem.set(PresetUtils.MESSAGEQUEUE_KEY, new ArrayList<>());
        mem.unset(PresetUtils.UNDOCKED_PRESET_KEY);

        FleetPreset activePreset = PresetUtils.getPresetOfMembers(sector.getPlayerFleet().getFleetData().getMembersInPriorityOrder());
        if (activePreset != null &&(boolean)persistentData.get(PresetUtils.IS_AUTO_UPDATE_KEY)) {
            sector.getMemoryWithoutUpdate().set(PresetUtils.UNDOCKED_PRESET_KEY, activePreset);
        }

        for (EveryFrameScript script : sector.getScripts()) {
            print(script.getClass().getName());
        }
        sector.addTransientScript(new FleetPresetManagerCoreScript());
        sector.addTransientScript(new OfficerTracker());
        sector.addTransientScript(new FleetMonitor());
        sector.addTransientListener(new DockingListener(false));
        sector.getListenerManager().addListener(new ColonyDecivStorageListener(), true);
    }

    @Override
    public void onNewGame() {
        Map<String, Object> persistentData = Global.getSector().getPersistentData();

        persistentData.put("$fleetPresetsManagerVer", ver);
        persistentData.put(PresetUtils.PRESETS_MEMORY_KEY, new HashMap<String, PresetUtils.FleetPreset>());
        persistentData.put(PresetUtils.PRESET_MEMBERS_KEY, new HashMap<String, List<FleetMemberWrapper>>());
        persistentData.put(PresetUtils.STORED_PRESET_MEMBERIDS_KEY, new HashMap<>());
        persistentData.put(PresetUtils.IS_AUTO_UPDATE_KEY, true);
        persistentData.put(PresetUtils.KEEPCARGORATIOS_KEY, false);
    }

    @Override
    public void beforeGameSave() {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        mem.unset(PresetUtils.FLEETINFOPANEL_KEY); // required or else game catches exception about reflection during load and kicks this stuff from memory
        mem.unset(PresetUtils.COREUI_KEY); // its just smoother this way (its not actually a big deal i just dont like the look of the double loading info)
        mem.unset(PresetUtils.OFFICER_AUTOASSIGN_BUTTON_KEY);
    }
}