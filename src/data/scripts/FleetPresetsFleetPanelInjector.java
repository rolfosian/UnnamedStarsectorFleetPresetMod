// credit for most of this goes to the author of the code in the officer extension mod

package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.SubmarketPlugin.OnClickAction;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.input.InputEventType;
import com.fs.starfarer.api.input.InputEventClass;
import com.fs.starfarer.api.input.InputEventMouseButton;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.ui.marketinfo.f;

import data.scripts.listeners.ActionListener;
import data.scripts.listeners.FleetPresetManagementListener;
import data.scripts.ui.Button;
import data.scripts.ui.Position;
import data.scripts.ui.Label;
import data.scripts.ui.UIPanel;
import data.scripts.util.PresetMiscUtils;
import data.scripts.util.PresetUtils;
import data.scripts.util.ReflectionUtilis;
import data.scripts.util.UtilReflection;
import data.scripts.util.PresetUtils;

import java.util.*;
import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class FleetPresetsFleetPanelInjector {
    public static void print(Object... args) {
        PresetMiscUtils.print(args);
    }

    private static Field fleetInfoPanelField;
    private static Field autoAssignButtonField;

    /** Keep track of the last known fleet info panel to track when it changes */
    private UIPanelAPI fleetInfoPanelRef;

    private boolean injected = false;
    private Button autoAssignButton;
    private Button presetFleetsButton;
    private Button storeFleetButton;
    private Button pullAllShipsButton;

    private Object getStorageButtonInputEventInstance(ButtonAPI storageButton) {
        PositionAPI storageButtonPosition = storageButton.getPosition();
        try {
            Class<?> inputEventClass = Class.forName(ClassRefs.inputEventClass.getCanonicalName());
            Constructor<?> ctor = inputEventClass.getDeclaredConstructor(InputEventClass.class, InputEventType.class, int.class, int.class, int.class, char.class);
            ctor.setAccessible(true);

            return ctor.newInstance(InputEventClass.MOUSE_EVENT, InputEventType.MOUSE_DOWN, (int)storageButtonPosition.getCenterX(), (int)storageButtonPosition.getCenterY(), 0, '\0');
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private ButtonAPI getStorageButton(UIPanelAPI core) { // This will probably crash the game if you call it without the player docked at a market
        Object infoPanelParent = ReflectionUtilis.invokeMethod("getParent", fleetInfoPanelRef);
        Object marketPicker = ReflectionUtilis.getMethodAndInvokeDirectly("getMarketPicker", infoPanelParent, 0);
        List<ButtonAPI> marketButtons = ((List<ButtonAPI>) ReflectionUtilis.getMethodAndInvokeDirectly("getChildrenNonCopy", marketPicker, 0));

        return marketButtons.get(marketButtons.size() - 1);
    }

    public void advance() {
        UIPanelAPI fleetInfoPanel = findFleetInfoPanel();

        if (fleetInfoPanel == null
                || fleetInfoPanel != fleetInfoPanelRef) {
            injected = false;
            fleetInfoPanelRef = fleetInfoPanel;
            autoAssignButton = null;
            presetFleetsButton = null;
            pullAllShipsButton = null;
            storeFleetButton = null;
            return;
        }

        MarketAPI market = (MarketAPI) Global.getSector().getMemoryWithoutUpdate().get(PresetUtils.PLAYERCURRENTMARKET_KEY);
        if (injected) {
            if (market != null) {
                if (PresetUtils.isPlayerPaidForStorage(market.getSubmarket(Submarkets.SUBMARKET_STORAGE).getPlugin())) {
                    if (PresetUtils.getMothBalledShips(market) != null && PresetUtils.getMothBalledShips(market).size() > 0) {
                        pullAllShipsButton.setEnabled(true);
                    } else {
                        pullAllShipsButton.setEnabled(false);
                    }
                    storeFleetButton.setEnabled(true);
                }
            }
            if (Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder().size() == 1) {
                storeFleetButton.setEnabled(false);
            }
        }

        if (!injected) {
            injected = true;
            Global.getSector().getMemoryWithoutUpdate().set(PresetUtils.FLEETINFOPANEL_KEY, fleetInfoPanel);

            autoAssignButton = new Button(getAutoAssignButton(fleetInfoPanel), null, null);
            PositionAPI officerAutoAssignButtonPosition = autoAssignButton.getPosition();
            float officerAutoAssignButtonHeight = officerAutoAssignButtonPosition.getHeight();

            PositionAPI storageButtonPosition = null;
            if (market != null) {
                UIPanelAPI core = (UIPanelAPI) Global.getSector().getMemoryWithoutUpdate().get(PresetUtils.COREUI_KEY);
                Object storageButtonObf = getStorageButton(core);
                ButtonAPI storageButton = (ButtonAPI) storageButtonObf;

                if (storageButton != null) {
                    storageButtonPosition = storageButton.getPosition();

                    storeFleetButton = UtilReflection.makeButton(
                        "   Store Entire Fleet",
                        new ActionListener() {
                            @Override
                            public void trigger(Object... args) {
                                if (!PresetUtils.isPlayerPaidForStorage(market.getSubmarket(Submarkets.SUBMARKET_STORAGE).getPlugin())) {
                                    Object infoPanelParent = ReflectionUtilis.invokeMethod("getParent", fleetInfoPanelRef);
                                    Object marketPicker = ReflectionUtilis.getMethodAndInvokeDirectly("getMarketPicker", infoPanelParent, 0);

                                    ReflectionUtilis.getMethodAndInvokeDirectly("actionPerformed", marketPicker, 2, getStorageButtonInputEventInstance(storageButton), storageButtonObf);
                                } else {
                                    Global.getSector().getMemoryWithoutUpdate().unset(PresetUtils.UNDOCKED_PRESET_KEY);
                                    PresetUtils.storeFleetInStorage();
                                }
                            }

                        },
                        Misc.getBasePlayerColor(),
                        Misc.getDarkPlayerColor(),
                        Alignment.LMID,
                        CutStyle.ALL,
                        storageButtonPosition.getWidth(),
                        officerAutoAssignButtonPosition.getHeight(),
                        null);;

                    Position pos = new UIPanel(fleetInfoPanel).add(storeFleetButton);
                    pos.set(storageButtonPosition);
                    pos.getInstance().setSize(storageButtonPosition.getWidth(), officerAutoAssignButtonPosition.getHeight());
                    pos.getInstance().setYAlignOffset(29f).setXAlignOffset(-storageButtonPosition.getWidth()-10f);
                    
                    pullAllShipsButton = UtilReflection.makeButton(
                        "Take all ships from storage",
                        new ActionListener() {
                            @Override
                            public void trigger(Object... args) {
                                if (!PresetUtils.isPlayerPaidForStorage(market.getSubmarket(Submarkets.SUBMARKET_STORAGE).getPlugin())) {
                                    Object infoPanelParent = ReflectionUtilis.invokeMethod("getParent", fleetInfoPanelRef);
                                    Object marketPicker = ReflectionUtilis.getMethodAndInvokeDirectly("getMarketPicker", infoPanelParent, 0);

                                    ReflectionUtilis.getMethodAndInvokeDirectly("actionPerformed", marketPicker, 2, getStorageButtonInputEventInstance(storageButton), storageButtonObf);
                                } else {
                                    Global.getSector().getMemoryWithoutUpdate().unset(PresetUtils.UNDOCKED_PRESET_KEY);
                                    PresetUtils.takeAllShipsFromStorage();
                                }
                            }
                        },
                        Misc.getBasePlayerColor(),
                        Misc.getDarkPlayerColor(),
                        Alignment.LMID,
                        CutStyle.ALL,
                        storageButtonPosition.getWidth(),
                        officerAutoAssignButtonPosition.getHeight(),
                        null);

                    pos = new UIPanel(fleetInfoPanel).add(pullAllShipsButton);
                    pos.set(storageButtonPosition);
                    pos.getInstance().setSize(storageButtonPosition.getWidth(), officerAutoAssignButtonPosition.getHeight()+2f);
                    pos.getInstance().setYAlignOffset(-7f).setXAlignOffset(-storageButtonPosition.getWidth()-10f);

                    if (Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder().size() == 1) {
                        storeFleetButton.setEnabled(false);
                    } else if (PresetUtils.getMothBalledShips(market) != null && PresetUtils.getMothBalledShips(market).size() == 0) {
                        pullAllShipsButton.setEnabled(false);
                    }

            } else {
                storeFleetButton = null;
                pullAllShipsButton = null;
            }
        }

        presetFleetsButton = UtilReflection.makeButton(
                "   Fleet Presets Management",
                new FleetPresetManagementListener(),
                Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(),
                Alignment.LMID,
                CutStyle.ALL,
                officerAutoAssignButtonPosition.getWidth(),
                officerAutoAssignButtonPosition.getHeight(),
                Keyboard.KEY_A);
        // UIPanel presetFleetsButtonPanel = new UIPanel(fleetInfoPanel);
        Position pos = new UIPanel(fleetInfoPanel).add(presetFleetsButton);
        pos.set(officerAutoAssignButtonPosition);
        pos.getInstance().setYAlignOffset(-officerAutoAssignButtonHeight * 10f);
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
                    if (button != null && new Button(button, null, null).getText().trim().startsWith("Auto-assign")) {
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