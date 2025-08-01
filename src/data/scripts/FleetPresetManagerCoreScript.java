// Code taken and modified from Officer Extension mod
package data.scripts;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;

import data.scripts.util.*;
import data.scripts.FleetPresetsFleetPanelInjector;

public class FleetPresetManagerCoreScript implements EveryFrameScript {
    private static void print(Object... args) {
        PresetMiscUtils.print(args);
    }

    private boolean isFirstFrame = true;
    private final FleetPresetsFleetPanelInjector fleetPanelInjector;

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    public FleetPresetManagerCoreScript() {
        fleetPanelInjector = new FleetPresetsFleetPanelInjector();
    }
    
    @Override
    public void advance(float amount) {
        if (isFirstFrame) {
            CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();

            if (ReflectionUtilis.getPrivateVariable(ClassRefs.campaignUIScreenPanelField, campaignUI) == null) {
                ReflectionUtilis.setPrivateVariable(ClassRefs.campaignUIScreenPanelField, campaignUI,
                    ReflectionUtilis.instantiateClass(ClassRefs.uiPanelClass,
                    ClassRefs.uiPanelClassConstructorParamTypes,
                    Global.getSettings().getScreenWidth(),
                    Global.getSettings().getScreenHeight()
                    )
                );
            }
            
            isFirstFrame = false;
            fleetPanelInjector.init();
        }
        fleetPanelInjector.advance();
    }
}