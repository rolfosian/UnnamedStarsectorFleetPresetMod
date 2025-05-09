// credit for a lot of this goes to the author of the code in the officer extension mod

package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.CampaignEventListener;

import data.scripts.util.PresetUtils;
import data.scripts.FleetPresetManagerCoreScript;
import data.scripts.listeners.DockingListener;
import data.scripts.listeners.OfficerDismissalTracker;

import java.util.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import org.apache.log4j.Logger;

public class FleetPresetManagerPlugin extends BaseModPlugin {
    private static final Logger logger = Logger.getLogger(FleetPresetManagerPlugin.class);
    private static final String MEMORY_KEY = PresetUtils.MEMORY_KEY;

    private static final String[] reflectionWhitelist = new String[] {
            "data.scripts.FleetPresetManagerCoreScript",
            "data.scripts.ClassRefs",
            "data.scripts.util.UtilReflection",
            "data.scripts.ui",
            "data.scripts.FleetPanelInjector",
            "data.scripts.listeners",
            "data.scripts.util.UtilReflection",
            "data.scripts.util.ReflectionBetterUtilis"
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

        Global.getSector().getMemoryWithoutUpdate().set("$playerDocked", false);

        @SuppressWarnings("resource")
        ClassLoader cl = new ReflectionEnabledClassLoader(url, getClass().getClassLoader());
        try {
            Global.getSector().getPersistentData().put(MEMORY_KEY, new HashMap<String, PresetUtils.FleetPreset>());
            Global.getSector().addTransientScript((EveryFrameScript) PresetUtils.instantiateClassNoParams(cl.loadClass("data.scripts.FleetPresetManagerCoreScript")));

            Global.getSector().addTransientScript(new OfficerDismissalTracker());
            Global.getSector().addListener(new DockingListener());
        } catch (Exception e) {
            logger.error("Failure to load core script class; exiting", e);
            return;
        }
    }

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