// credit for most of this goes to the author of the code in the officer extension mod

package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;

import data.scripts.listeners.FleetPresetManagementListener;
import data.scripts.ui.Button;
import data.scripts.ui.Position;
import data.scripts.ui.Label;
import data.scripts.ui.UIPanel;
import data.scripts.util.PresetUtils;
import data.scripts.util.UtilReflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.lwjgl.input.Keyboard;

public class FleetPanelInjector {

    private static Field fleetInfoPanelField;
    private static Field autoAssignButtonField;

    /** Keep track of the last known fleet info panel to track when it changes */
    private UIPanelAPI fleetInfoPanelRef;

    private boolean injected = false;
    private Button autoAssignButton;
    private Button presetFleetsButton;

    public void advance() {
        UIPanelAPI fleetInfoPanel = findFleetInfoPanel();

        if (fleetInfoPanel == null
                || fleetInfoPanel != fleetInfoPanelRef) {
            injected = false;
            fleetInfoPanelRef = fleetInfoPanel;
            autoAssignButton = null;
            return;
        }

        if (!injected) {
            injected = true;
            Global.getSector().getMemoryWithoutUpdate().set(PresetUtils.FLEETINFOPANEL_KEY, fleetInfoPanel);

            Button officerAutoAssignButton = new Button(getAutoAssignButton(fleetInfoPanel));
            PositionAPI officerAutoAssignButtonPosition = officerAutoAssignButton.getPosition();
            float officerAutoAssignButtonHeight = officerAutoAssignButtonPosition.getHeight();

            presetFleetsButton = UtilReflection.makeButton(
                    "   Fleet Presets Management",
                    new FleetPresetManagementListener(),
                    Misc.getBasePlayerColor(),
                    Misc.getDarkPlayerColor(),
                    Alignment.LMID,
                    CutStyle.BL_TR,
                    officerAutoAssignButtonPosition.getWidth(),
                    officerAutoAssignButtonPosition.getHeight(),
                    Keyboard.KEY_A);
            UIPanel presetFleetsButtonPanel = new UIPanel(fleetInfoPanel);
            new UIPanel(fleetInfoPanel).add(presetFleetsButton).set(officerAutoAssignButtonPosition);
            PositionAPI presetFleetsButtonPosition = presetFleetsButton.getPosition();
            // presetFleetsButtonPosition.setXAlignOffset(-0.25f);
            presetFleetsButtonPosition.setYAlignOffset(-officerAutoAssignButtonHeight * 1.55f);
        }
    }

    public UIPanelAPI findFleetInfoPanel() {
        if (!CoreUITabId.FLEET.equals(Global.getSector().getCampaignUI().getCurrentCoreTab())) {
            return null;
        }
        UIPanelAPI core = UtilReflection.getCoreUI();
        if (core == null) {
            return null;
        }
        Global.getSector().getMemoryWithoutUpdate().set(PresetUtils.COREUI_KEY, core);

        UIPanelAPI currentTab = (UIPanelAPI) UtilReflection.invokeGetter(core, "getCurrentTab");
        // Since the current tab ID is fleet, this *should* give us the fleet tab.
        // We need to find the field corresponding to the info panel. There's no good way to do this,
        // other than to go through every declared field, check that it's a UIPanelAPI, then look for
        // a LabelAPI and a CampaignFleetAPI field in that
        if (fleetInfoPanelField == null) {
            outer:
            for (Field field : currentTab.getClass().getDeclaredFields()) {
                if (!UIPanelAPI.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                boolean hasLabelField = false;
                boolean hasFleetField = false;
                for (Field innerField : field.getType().getDeclaredFields()) {
                    if (CampaignFleetAPI.class.isAssignableFrom(innerField.getType())) {
                        hasFleetField = true;
                    }
                    if (LabelAPI.class.isAssignableFrom(innerField.getType())) {
                        hasLabelField = true;
                    }
                    // The outer field is the fleet info panel
                    if (hasFleetField && hasLabelField) {
                        fleetInfoPanelField = field;
                        break outer;
                    }
                }
            }
        }

        if (fleetInfoPanelField == null) {
            throw new RuntimeException("Could not find the fleet info panel for the fleet tab");
        }

        fleetInfoPanelField.setAccessible(true);
        try {
            return (UIPanelAPI) fleetInfoPanelField.get(currentTab);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public ButtonAPI getAutoAssignButton(UIPanelAPI fleetInfoPanel) {
        if (autoAssignButtonField == null) {
            // Find the button that starts with "Auto-assign"
            for (Field field : fleetInfoPanel.getClass().getDeclaredFields()) {
                if (ButtonAPI.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    ButtonAPI button;
                    try {
                        button = (ButtonAPI) field.get(fleetInfoPanel);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        button = null;
                    }
                    if (button != null && new Button(button).getText().trim().startsWith("Auto-assign")) {
                        autoAssignButtonField = field;
                        break;
                    }
                }
            }
        }

        if (autoAssignButtonField == null) {
            throw new RuntimeException("Could not find the auto-assign button");
        }

        autoAssignButtonField.setAccessible(true);
        try {
            return (ButtonAPI) autoAssignButtonField.get(fleetInfoPanel);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}