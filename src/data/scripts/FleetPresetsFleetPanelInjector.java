// Code taken and modified from Officer Extension mod

package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.SubmarketPlugin.OnClickAction;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.input.InputEventType;
import com.fs.starfarer.api.input.InputEventClass;
import com.fs.starfarer.api.input.InputEventMouseButton;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.fleet.FleetMember;

import data.scripts.listeners.FleetPresetManagementListener;

import data.scripts.ui.Button;
import data.scripts.ui.Position;
import data.scripts.ui.Label;
import data.scripts.ui.UIPanel;
import data.scripts.util.CargoPresetUtils;
import data.scripts.util.PresetMiscUtils;
import data.scripts.util.PresetUtils;
import data.scripts.util.PresetUtils.FleetMemberWrapper;
import data.scripts.util.PresetUtils.RunningMembers;
import data.scripts.util.ReflectionUtilis;
import data.scripts.util.ReflectionUtilis.ListenerFactory.ActionListener;
import data.scripts.util.UtilReflection;
import data.scripts.util.UtilReflection.ConfirmDialogData;

import java.util.*;
import java.awt.Color;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

@SuppressWarnings("unchecked")
public class FleetPresetsFleetPanelInjector {
    public static void print(Object... args) {
        PresetMiscUtils.print(args);
    }

    private static Object fleetInfoPanelField;
    private static Object autoAssignButtonField;

    /** Keep track of the last known fleet info panel to track when it changes */
    private UIPanelAPI fleetInfoPanelRef;

    private boolean injected = false;
    private ButtonAPI autoAssignButton;
    private Button presetFleetsButton;
    private Button storeFleetButton;
    private Button pullAllShipsButton;

    private List<FleetMemberAPI> runningMembers; //we dont need the RunningMembers class for this because we don't care about the officer assignments here
    Map<String, Set<String>> storedMemberIds;
    private Map<String, List<FleetMemberWrapper>> presetMembers;

    public FleetPresetsFleetPanelInjector() {
        this.runningMembers = Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder();
        this.storedMemberIds = PresetUtils.getStoredFleetPresetsMemberIds();
        this.presetMembers = (Map<String, List<FleetMemberWrapper>>) Global.getSector().getPersistentData().get(PresetUtils.PRESET_MEMBERS_KEY);
    }

    private ButtonAPI getStorageButton(UIPanelAPI core) { // This will probably crash the game if you call it without the player docked at a market
        Object infoPanelParent = ReflectionUtilis.invokeMethod("getParent", fleetInfoPanelRef);
        // ReflectionUtilis.logMethods(infoPanelParent);
        Object marketPicker = ReflectionUtilis.getMethodAndInvokeDirectly("getMarketPicker", infoPanelParent, 0);
        if (marketPicker == null) return null;

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
            Global.getSector().getMemoryWithoutUpdate().unset(PresetUtils.FLEETINFOPANEL_KEY);
            return;
        }

        List<FleetMemberAPI> playerFleetMembers = Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder();
        MarketAPI market = PresetUtils.getPlayerCurrentMarket();
        List<FleetMemberAPI> mothballedShips = PresetUtils.getMothBalledShips(market);

        if (injected) {
            if (market != null && CargoPresetUtils.getStorageSubmarket(market) != null) {
                if (PresetUtils.isPlayerPaidForStorage(CargoPresetUtils.getStorageSubmarket(market).getPlugin())) {
                    if (mothballedShips != null && mothballedShips.size() > 0) {
                        pullAllShipsButton.setEnabled(true);
                    } else {
                        pullAllShipsButton.setEnabled(false);
                    }
                    storeFleetButton.setEnabled(true);
                }
                // checking if members are sold so there's no memory leak for wrappedMembers
                if (runningMembers.size() > playerFleetMembers.size()) {
                    if ((boolean)Global.getSector().getPersistentData().get(PresetUtils.IS_AUTO_UPDATE_KEY)) {
                        for (FleetMemberAPI runningMember : runningMembers) {
                            if (!playerFleetMembers.contains(runningMember)) {
                                
                                if (mothballedShips != null && !mothballedShips.contains(runningMember)) {
                                    // member was sold
                                    PresetUtils.cleanUpPerishedPresetMembers();
                                } else if (mothballedShips != null && mothballedShips.contains(runningMember) && PresetUtils.getFleetPresetsMembers().get(runningMember.getId()) != null) {
                                    // member was stored
                                    if (storedMemberIds.get(market.getName()) == null) {
                                        storedMemberIds.put(market.getName(), new HashSet<>());
                                    }
                                    storedMemberIds.get(market.getName()).add(runningMember.getId());
                                }
                            }
                        }
                    }
                } else if (playerFleetMembers.size() > runningMembers.size()) {
                    for (FleetMemberAPI member : playerFleetMembers) {
                        if (storedMemberIds.get(market.getName()) != null && storedMemberIds.get(market.getName()).contains(member.getId())) {
                            // member was taken from storage
                            storedMemberIds.get(market.getName()).remove(member.getId());
                            if (storedMemberIds.get(market.getName()).isEmpty()) storedMemberIds.remove(market.getName());
                        }
                    }
                } else {
                    // add logic for if sizes are the same but composition is different (order does not matter)
                }
            }
            if (playerFleetMembers.size() == 1 && storeFleetButton != null) {
                storeFleetButton.setEnabled(false);
            }
        }
        runningMembers = playerFleetMembers;

        if (!injected) {
            injected = true;
            Global.getSector().getMemoryWithoutUpdate().set(PresetUtils.FLEETINFOPANEL_KEY, fleetInfoPanel);

            autoAssignButton = (ButtonAPI) getAutoAssignButton(fleetInfoPanel);
            Global.getSector().getMemoryWithoutUpdate().set(PresetUtils.OFFICER_AUTOASSIGN_BUTTON_KEY, getAutoAssignButton(fleetInfoPanel));

            PositionAPI officerAutoAssignButtonPosition = autoAssignButton.getPosition();
            float officerAutoAssignButtonHeight = officerAutoAssignButtonPosition.getHeight();

            getStorageButton((UIPanelAPI) Global.getSector().getMemoryWithoutUpdate().get(PresetUtils.COREUI_KEY));

            if (market != null && CargoPresetUtils.getStorageSubmarket(market) != null) {
                PositionAPI storageButtonPosition = null;
                UIPanelAPI core = (UIPanelAPI) Global.getSector().getMemoryWithoutUpdate().get(PresetUtils.COREUI_KEY);
                Object storageButtonObf = getStorageButton(core);
                ButtonAPI storageButton = (ButtonAPI) storageButtonObf;

                if (storageButton != null) {
                    storageButtonPosition = storageButton.getPosition();

                    storeFleetButton = UtilReflection.makeButton(
                        " Store Entire Fleet",
                        new ActionListener() {
                            @Override
                            public void trigger(Object arg0, Object arg1) {
                                Global.getSector().getMemoryWithoutUpdate().unset(PresetUtils.UNDOCKED_PRESET_KEY);
                                PresetUtils.storeFleetInStorage();
                            }
                        },
                        Misc.getBasePlayerColor(),
                        Misc.getDarkPlayerColor(),
                        Alignment.MID,
                        CutStyle.TOP,
                        storageButtonPosition.getWidth(),
                        officerAutoAssignButtonPosition.getHeight(),
                        null);

                    Position pos = new UIPanel(fleetInfoPanel).add(storeFleetButton);
                    pos.set(officerAutoAssignButtonPosition);
                    pos.getInstance().setSize(storageButtonPosition.getWidth()-(storageButtonPosition.getWidth() / 4.8f), officerAutoAssignButtonPosition.getHeight()-6f);
                    // pos.getInstance().setYAlignOffset(29f).setXAlignOffset(-storageButtonPosition.getWidth()-10f);
                    pos.getInstance().setYAlignOffset(157f).setXAlignOffset(1352f);
                    
                    pullAllShipsButton = UtilReflection.makeButton(
                        "  Take all ships from storage",
                        new ActionListener() {
                            @Override
                            public void trigger(Object arg0, Object arg1) {
                                Global.getSector().getMemoryWithoutUpdate().unset(PresetUtils.UNDOCKED_PRESET_KEY);
                                PresetUtils.takeAllShipsFromStorage();
                            }
                        },
                        Misc.getBasePlayerColor(),
                        Misc.getDarkPlayerColor(),
                        Alignment.LMID,
                        CutStyle.TOP,
                        storageButtonPosition.getWidth(),
                        officerAutoAssignButtonPosition.getHeight(),
                        null);

                    pos = new UIPanel(fleetInfoPanel).add(pullAllShipsButton);
                    pos.set(officerAutoAssignButtonPosition);
                    pos.getInstance().setSize(storageButtonPosition.getWidth()+30f, officerAutoAssignButtonPosition.getHeight()-6f);
                    // pos.getInstance().setYAlignOffset(-7f).setXAlignOffset(-storageButtonPosition.getWidth()-10f);
                    pos.getInstance().setYAlignOffset(157f).setXAlignOffset(1127f);

                    if (!PresetUtils.isPlayerPaidForStorage(CargoPresetUtils.getStorageSubmarket(market).getPlugin())) {
                        storeFleetButton.setEnabled(false);
                        pullAllShipsButton.setEnabled(false);

                    } else {
                        if (playerFleetMembers.size() == 1) {
                            storeFleetButton.setEnabled(false);
                        } else if (mothballedShips == null || mothballedShips.size() == 0) {
                            pullAllShipsButton.setEnabled(false);
                        }
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
            PresetUtils.updateFleetPresetStats(playerFleetMembers);
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
            for (Object field : currentTab.getClass().getDeclaredFields()) {
                Class<?> fieldType = ReflectionUtilis.getFieldType(field);

                if (!UIPanelAPI.class.isAssignableFrom(fieldType)) {
                    continue;
                }
                boolean hasLabelField = false;
                boolean hasFleetField = false;
                for (Object innerField : fieldType.getDeclaredFields()) {
                    Class<?> innerFieldType = ReflectionUtilis.getFieldType(innerField);
                    if (CampaignFleetAPI.class.isAssignableFrom(innerFieldType)) {
                        hasFleetField = true;
                    }
                    if (LabelAPI.class.isAssignableFrom(innerFieldType)) {
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

        // fleetInfoPanelField.setAccessible(true);
        try {
            return (UIPanelAPI) ReflectionUtilis.getPrivateVariable(ReflectionUtilis.getFieldName(fleetInfoPanelField), currentTab);
            // return (UIPanelAPI) fleetInfoPanelField.get(currentTab);
        }
        catch (Exception e) {
            print(e);
            return null;
        }
    }

    public ButtonAPI getAutoAssignButton(UIPanelAPI fleetInfoPanel) {
        if (autoAssignButtonField == null) {
            // Find the button that starts with "Auto-assign"
            for (Object field : fleetInfoPanel.getClass().getDeclaredFields()) {
                if (ButtonAPI.class.isAssignableFrom(ReflectionUtilis.getFieldType(field))) {
                    ButtonAPI button = (ButtonAPI) ReflectionUtilis.getPrivateVariable(ReflectionUtilis.getFieldName(field), fleetInfoPanel);
                    

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

        return (ButtonAPI) ReflectionUtilis.getPrivateVariable(ReflectionUtilis.getFieldName(autoAssignButtonField), fleetInfoPanel);

    }
}