// Code taken and modified from Officer Extension mod

package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import data.scripts.util.PresetMiscUtils;
import data.scripts.util.PresetUtils;
import data.scripts.util.PresetUtils.FleetPreset;
import data.scripts.util.PresetUtils.FleetMemberWrapper;
import data.scripts.util.UtilReflection;
import data.scripts.FleetPresetManagerCoreScript;
import data.scripts.listeners.DockingListener;
import data.scripts.listeners.FleetMonitor;
import data.scripts.listeners.OfficerTracker;

import java.util.*;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;

import org.apache.log4j.Logger;

public class FleetPresetManagerPlugin extends BaseModPlugin {
    public static void print(Object... args) {
        PresetMiscUtils.print(args);
    }

    private static final String ver = "0.0.4";

    private static final String[] reflectionWhitelist = new String[] {
        "data.scripts.FleetPresetManagerCoreScript",
        "data.scripts.FleetPresetsFleetPanelInjector",

        "data.scripts.ClassRefs",
        "data.scripts.util.UtilReflection",
        "data.scripts.util.ReflectionUtilis",
        "data.scripts.util.ReflectionBetterUtilis",

        "data.scripts.ui",
        "data.scripts.listeners",
    };

    @Override
    public void onGameLoad(boolean newGame) {
        URL url;
        try {
            url = getClass().getProtectionDomain().getCodeSource().getLocation();
        }
        catch (SecurityException e) {
            throw new RuntimeException("Failed to get URL of this class", e);
        }

        String modVer = (String) Global.getSector().getPersistentData().get("$fleetPresetsManagerVer");
        if (modVer == null || !modVer.equals(ver)) {
            Global.getSector().getPersistentData().put(PresetUtils.PRESETS_MEMORY_KEY, new HashMap<String, FleetPreset>());
            Global.getSector().getPersistentData().put(PresetUtils.PRESET_MEMBERS_KEY, new HashMap<String, List<FleetMemberWrapper>>());
            Global.getSector().getPersistentData().put(PresetUtils.IS_AUTO_UPDATE_KEY, false);
            Global.getSector().getPersistentData().put("$fleetPresetsManagerVer", ver);
        }

        Global.getSector().getMemoryWithoutUpdate().set(PresetUtils.MESSAGEQUEUE_KEY, new ArrayList<>());

        // for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder()) {
        //     print(member.getId());
        // }
        // print("-----");
        // Map<String, PresetUtils.FleetPreset> presets = (Map<String, FleetPreset>) Global.getSector().getPersistentData().get(PresetUtils.PRESETS_MEMORY_KEY);
        // for (String key : presets.keySet()) {
        //     PresetUtils.FleetPreset preset = presets.get(key);
        //     for (FleetMemberWrapper wrappedMember : preset.fleetMembers) {
        //         print(wrappedMember.member.getId());
        //     }

        // }

        FleetPreset activePreset = PresetUtils.getPresetOfMembers(Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder());
        if (activePreset != null &&(boolean)Global.getSector().getPersistentData().get(PresetUtils.IS_AUTO_UPDATE_KEY)) {
            Global.getSector().getMemoryWithoutUpdate().set(PresetUtils.UNDOCKED_PRESET_KEY, activePreset);
        }

        @SuppressWarnings("resource")
        ClassLoader cl = new ReflectionEnabledClassLoader(url, getClass().getClassLoader());
        try {
            Global.getSector().addTransientScript((EveryFrameScript) UtilReflection.instantiateClassNoParams(cl.loadClass("data.scripts.FleetPresetManagerCoreScript")));

            Global.getSector().addTransientScript(new OfficerTracker());
            Global.getSector().addTransientScript(new FleetMonitor());
            Global.getSector().addListener(new DockingListener());
        } catch (Exception e) {
            print("Failure to load core script class; exiting", e);
            return;
        }
    }

    @Override
    public void onNewGame() {
        Global.getSector().getPersistentData().put(PresetUtils.PRESETS_MEMORY_KEY, new HashMap<String, PresetUtils.FleetPreset>());
        Global.getSector().getPersistentData().put(PresetUtils.PRESET_MEMBERS_KEY, new HashMap<String, List<FleetMemberWrapper>>());
        Global.getSector().getPersistentData().put(PresetUtils.IS_AUTO_UPDATE_KEY, false);
    }

    @Override
    public void beforeGameSave() {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        // required or else game catches exception about reflection during load and kicks this stuff from the save
        // its just smoother this way (its not actually a big deal i just dont like the look of the double loading info) 
        mem.unset(PresetUtils.FLEETINFOPANEL_KEY);
        mem.unset(PresetUtils.COREUI_KEY);
    }

    // credit for this goes to the author of the code in the officer extension mod
    public static class ReflectionEnabledClassLoader extends URLClassLoader {

        public ReflectionEnabledClassLoader(URL url, ClassLoader parent) {
            super(new URL[] {url}, parent);
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (name.startsWith("java.lang.reflect")) {
                return ClassLoader.getSystemClassLoader().loadClass(name);
            }
            return super.loadClass(name);
        }

        @Override
        public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            Class<?> c = findLoadedClass(name);
            if (c != null) {
                return c;
            }
            // Be the defining classloader for all classes in the reflection whitelist
            // For classes defined by this loader, classes in java.lang.reflect will be loaded directly
            // by the system classloader, without the intermediate delegations.
            for (String str : reflectionWhitelist) {
                if (name.startsWith(str)) {
                    return findClass(name);
                }
            }
            return super.loadClass(name, resolve);
        }
    }
}