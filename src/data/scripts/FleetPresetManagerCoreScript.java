// Code taken and modified from Officer Extension mod
package data.scripts;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.ui.ButtonAPI;

import data.scripts.ui.*;
import data.scripts.util.*;
import data.scripts.listeners.*;
import data.scripts.FleetPresetsFleetPanelInjector;

import java.util.*;

public class FleetPresetManagerCoreScript implements EveryFrameScript {
    private static void print(Object... args) {
        PresetMiscUtils.print(args);
    }
    private static PresetUtils.FleetPreset currentFleetPreset;

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
                    new Class<?>[] {
                        float.class,
                        float.class,
                    },
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