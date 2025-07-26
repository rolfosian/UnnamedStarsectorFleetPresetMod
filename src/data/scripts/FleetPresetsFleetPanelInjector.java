// Code taken and modified from Officer Extension mod

package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.SubmarketPlugin.OnClickAction;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.fleet.FleetMember;

import data.scripts.listeners.DockingListener;
import data.scripts.listeners.FleetPresetManagementListener;

import data.scripts.ui.Button;
import data.scripts.ui.Position;
import data.scripts.ui.Label;
import data.scripts.ui.UIPanel;
import data.scripts.ui.UIConfig;

import data.scripts.util.CargoPresetUtils;
import data.scripts.util.PresetMiscUtils;
import data.scripts.util.PresetUtils;
import data.scripts.util.PresetUtils.FleetMemberWrapper;
import data.scripts.util.PresetUtils.FleetPreset;
import data.scripts.util.PresetUtils.RunningMembers;
import data.scripts.util.ReflectionUtilis;
import data.scripts.util.ListenerFactory.ActionListener;
import data.scripts.util.UtilReflection;
import data.scripts.util.UtilReflection.ConfirmDialogData;

import java.util.*;
import java.awt.Color;

import org.lwjgl.input.Keyboard;

@SuppressWarnings("unchecked")
public class FleetPresetsFleetPanelInjector {
    public static void print(Object... args) {
        PresetMiscUtils.print(args);
    }

    private static Object fleetInfoPanelField;
    private static Object autoAssignButtonField;
    private static Object marketPickerMethod;
    private static Object getFleetPanelMethod;

    private boolean injected = false;

    /** Keep track of the last known fleet info panel to track when it changes */
    private UIPanelAPI fleetInfoPanelRef;
    private UIPanel fleetTabLeftPane;
    private UIPanel fleetTabWrapped;
    private Object fleetTab;
    private Object fleetPanelClickHandler;
    private UIComponentAPI fleetDisengageableNotice;

    private ButtonAPI autoAssignButton;
    private PositionAPI officerAutoAssignButtonPosition;

    private Button presetFleetsButton;
    private Button storeFleetButton;
    private Button pullAllShipsButton;

    private Label currentPresetLabelHeader;
    private Label currentPresetLabel;

    private RunningMembers runningMembers; // we dont need the RunningMembers class for this because we don't care about the officer assignments here
    Map<String, Set<String>> storedMemberIds;
    private Map<String, List<FleetMemberWrapper>> presetMembers;

    private DockingListener dockingListener;
    private FleetPresetManagementListener masterPresetsDialog;

    public FleetPresetsFleetPanelInjector() {
        this.runningMembers = new RunningMembers(Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy());
        this.storedMemberIds = PresetUtils.getStoredFleetPresetsMemberIds();
        this.presetMembers = (Map<String, List<FleetMemberWrapper>>) Global.getSector().getPersistentData().get(PresetUtils.PRESET_MEMBERS_KEY);
    }

    public void init() {
        dockingListener = PresetUtils.getDockingListener();
    }

    private ButtonAPI getStorageButton(UIPanelAPI core) { // This will probably crash the game if you call it without the player docked at a market
        Object marketPicker = ReflectionUtilis.invokeMethodDirectly(ClassRefs.fleetTabGetMarketPickerMethod, fleetTab);
        if (marketPicker == null) return null;

        List<ButtonAPI> marketButtons = ((List<ButtonAPI>) ReflectionUtilis.invokeMethodDirectly(ClassRefs.visualPanelGetChildrenNonCopyMethod, marketPicker));
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
            currentPresetLabel = null;
            currentPresetLabelHeader = null;
            fleetTabLeftPane = null;
            fleetTab = null;
            fleetTabWrapped = null;
            fleetPanelClickHandler = null;
            masterPresetsDialog = null;
            Global.getSector().getMemoryWithoutUpdate().unset(PresetUtils.FLEET_TAB_KEY);
            return;
        }

        List<FleetMemberAPI> playerFleetMembers = Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy();
        MarketAPI market = PresetUtils.getPlayerCurrentMarket();
        List<FleetMemberAPI> mothballedShips = PresetUtils.getMothBalledShips(market);

        if (injected) {
            setCurrentPresetLabel(playerFleetMembers);

            if (market != null && CargoPresetUtils.getStorageSubmarket(market) != null) {
                if (dockingListener.canPlayerAccessStorage(market)) {
                    if (pullAllShipsButton == null || storeFleetButton == null) addAuxStorageButtons(playerFleetMembers, officerAutoAssignButtonPosition, mothballedShips, market);
                        if (mothballedShips != null && mothballedShips.size() > 0) {
                            if (!pullAllShipsButton.isEnabled()) pullAllShipsButton.setEnabled(true);
                        } else {
                            if (pullAllShipsButton.isEnabled()) pullAllShipsButton.setEnabled(false);
                        }

                        if (!storeFleetButton.isEnabled() && playerFleetMembers.size() > 1) storeFleetButton.setEnabled(true);
                }
                // checking if members are sold so there's no memory leak for wrappedMembers
                if (runningMembers.size() > playerFleetMembers.size()) {
                    if ((boolean)Global.getSector().getPersistentData().get(PresetUtils.IS_AUTO_UPDATE_KEY)) {
                        for (FleetMemberAPI runningMember : runningMembers.keySet()) {
                            if (!playerFleetMembers.contains(runningMember)) {
                                
                                if (mothballedShips != null && mothballedShips.contains(runningMember) && PresetUtils.getFleetPresetsMembers().get(runningMember.getId()) != null) {
                                    // member was stored
                                    if (storedMemberIds.get(market.getName()) == null) {
                                        storedMemberIds.put(market.getName(), new HashSet<>());
                                    }
                                    storedMemberIds.get(market.getName()).add(runningMember.getId());

                                } else if (PresetUtils.getFleetPresetsMembers().get(runningMember.getId()) != null && !masterPresetsDialog.isPartialSelecting() && getPickedUpMember() == null) {
                                    // member was sold or scuttled
                                    PresetUtils.cleanUpPerishedPresetMembers();
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
                // } else if (!runningMembers.keySet().equals(new HashSet<>(playerFleetMembers))) {

                }
            }
            if (storeFleetButton != null && playerFleetMembers.size() < 2) {
                if (storeFleetButton.isEnabled()) storeFleetButton.setEnabled(false);
            }
        }

        if (!injected) {
            injected = true;
            fleetTabLeftPane = new UIPanel(fleetInfoPanel);
            fleetDisengageableNotice = getDisengageableNotice(fleetInfoPanel);

            Global.getSector().getMemoryWithoutUpdate().set(PresetUtils.FLEET_TAB_KEY, fleetTab);

            autoAssignButton = (ButtonAPI) getAutoAssignButton(fleetInfoPanel);
            Global.getSector().getMemoryWithoutUpdate().set(PresetUtils.OFFICER_AUTOASSIGN_BUTTON_KEY, getAutoAssignButton(fleetInfoPanel));

            officerAutoAssignButtonPosition = autoAssignButton.getPosition();

            if (market != null && CargoPresetUtils.getStorageSubmarket(market) != null) {
                addAuxStorageButtons(playerFleetMembers, officerAutoAssignButtonPosition, mothballedShips, market);
            }

            masterPresetsDialog = new FleetPresetManagementListener();
            presetFleetsButton = UtilReflection.makeButton(
                    "   Fleet Presets Management",
                    masterPresetsDialog,
                    Misc.getBasePlayerColor(),
                    Misc.getDarkPlayerColor(),
                    Alignment.LMID,
                    CutStyle.BL_TR,
                    officerAutoAssignButtonPosition.getWidth(),
                    officerAutoAssignButtonPosition.getHeight(),
                    Keyboard.KEY_A);

            setCurrentPresetLabel(playerFleetMembers);

            if (UIConfig.DISPLAY_HEIGHT > 800) {
                Position pos = fleetTabLeftPane.add(presetFleetsButton);
                pos.getInstance().belowMid(fleetDisengageableNotice, 10f);

            } else {
                Position pos = fleetTabLeftPane.add(presetFleetsButton);
                pos.getInstance().inTL(UIConfig.MANAGEMENT_BTN_X_OFFSET, UIConfig.MANAGEMENT_BTN_Y_OFFSET);
                // print(pos.getInstance().getX(), pos.getInstance().getY());
            }
        }
        if (!masterPresetsDialog.isPartialSelecting()) runningMembers = new RunningMembers(playerFleetMembers);
    }

    private void setCurrentPresetLabel(List<FleetMemberAPI> playerFleetMembers) {
        FleetPreset preset = PresetUtils.getPresetOfMembers(playerFleetMembers);
        if (preset != null && UIConfig.IS_SET_CURRENT_PRESET_LABEL) {
            if (currentPresetLabel == null) {
                LabelAPI labbel1 = Global.getSettings().createLabel("Current Fleet Is Preset", Fonts.ORBITRON_12);
                labbel1.setColor(Global.getSettings().getBasePlayerColor());
                labbel1.setAlignment(Alignment.MID);
                currentPresetLabelHeader = new Label(labbel1);

                LabelAPI labbel2 = Global.getSettings().createLabel(preset.getName(), Fonts.ORBITRON_16);
                labbel2.setColor(Global.getSettings().getBasePlayerColor());
                labbel2.setHighlightColor(Global.getSettings().getBrightPlayerColor());
                labbel2.setHighlightOnMouseover(true);
                labbel2.setAlignment(Alignment.MID);
                currentPresetLabel = new Label(labbel2);
                
                Position labelPos1;
                Position labelPos2;
                if (UIConfig.DISPLAY_HEIGHT > 800) {
                    labelPos1 = fleetTabLeftPane.add(currentPresetLabelHeader);
                    labelPos1.getInstance().belowMid(fleetDisengageableNotice, presetFleetsButton.getInstance().getPosition().getHeight() + 20f);
                    labelPos2 = fleetTabLeftPane.add(currentPresetLabel);
                    labelPos2.getInstance().belowMid((UIComponentAPI)labbel1, 5f);
                } else {
                    labelPos1 = fleetTabLeftPane.add(currentPresetLabelHeader);
                    labelPos1.getInstance().inTL(UIConfig.CURRENT_PRESET_LABEL_X_OFFSET, UIConfig.CURRENT_PRESET_LABEL_Y_OFFSET);
                    labelPos2 = fleetTabLeftPane.add(currentPresetLabel);
                    labelPos2.getInstance().belowMid((UIComponentAPI)labbel1, 0f);
                }

            } else {
                if (!currentPresetLabel.getInstance().getText().equals(preset.getName())) {
                    float newWidth = currentPresetLabel.getInstance().computeTextWidth(preset.getName());
                    currentPresetLabel.getInstance().setText(preset.getName());
                    currentPresetLabel.getInstance().autoSizeToWidth(newWidth);
                }
            }

        } else if (currentPresetLabel != null) {
            if (UIConfig.DISPLAY_HEIGHT > 800) {
                fleetTabLeftPane.remove(currentPresetLabelHeader);
                fleetTabLeftPane.remove(currentPresetLabel);
            } else {
                fleetTabLeftPane.remove(currentPresetLabelHeader);
                fleetTabLeftPane.remove(currentPresetLabel);
            }
            currentPresetLabelHeader = null;
            currentPresetLabel = null;
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
        UIPanelAPI currentTab = (UIPanelAPI) ReflectionUtilis.invokeMethodDirectly(ClassRefs.coreUIgetCurrentTabMethod, core);

        Object fleetPanel = ReflectionUtilis.invokeMethodDirectly(ClassRefs.fleetTabGetFleetPanelMethod, currentTab);
        fleetPanelClickHandler = ReflectionUtilis.invokeMethodDirectly(ClassRefs.fleetPanelgetClickAndDropHandlerMethod, fleetPanel);

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

        fleetTab = currentTab;
        fleetTabWrapped = new UIPanel(fleetTab);

        return (UIPanelAPI) ReflectionUtilis.getPrivateVariable(fleetInfoPanelField, currentTab);

    }

    private UIComponentAPI getDisengageableNotice(UIPanelAPI fleetInfoPanel) {
        for (Object child : (List<Object>) ReflectionUtilis.getMethodAndInvokeDirectly("getChildrenNonCopy", fleetInfoPanel, 0)) {
            if (LabelAPI.class.isAssignableFrom(child.getClass())) {
                LabelAPI labbel = (LabelAPI) child;
                if (labbel.getText().startsWith("Your fleet is")) return (UIComponentAPI) labbel;
            }
        }
        return null;
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

    private void addAuxStorageButtons(List<FleetMemberAPI> playerFleetMembers, PositionAPI officerAutoAssignButtonPosition, List<FleetMemberAPI> mothballedShips, MarketAPI market) {
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
                    public void trigger(Object... args) {
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
            
            Position pos = fleetTabLeftPane.add(storeFleetButton);
            pos.set(officerAutoAssignButtonPosition);
            pos.getInstance().setSize(storageButtonPosition.getWidth()-(storageButtonPosition.getWidth() / 4.8f), officerAutoAssignButtonPosition.getHeight()-6f);
            pos.getInstance().setYAlignOffset(157f).setXAlignOffset(UIConfig.STORE_SHIPS_BTN_X_OFFSET);
            
            pullAllShipsButton = UtilReflection.makeButton(
                "  Take all ships from storage",
                new ActionListener() {
                    @Override
                    public void trigger(Object... args) {
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

            pos = fleetTabLeftPane.add(pullAllShipsButton);
            pos.set(officerAutoAssignButtonPosition);
            pos.getInstance().setSize(storageButtonPosition.getWidth()+30f, officerAutoAssignButtonPosition.getHeight()-6f);
            pos.getInstance().setYAlignOffset(157f).setXAlignOffset(UIConfig.TAKE_SHIPS_BTN_X_OFFSET);

            if (!PresetUtils.isPlayerPaidForStorage(CargoPresetUtils.getStorageSubmarket(market).getPlugin())) {
                if (storeFleetButton.isEnabled()) storeFleetButton.setEnabled(false);
                if (pullAllShipsButton.isEnabled()) pullAllShipsButton.setEnabled(false);

            } else {
                if (playerFleetMembers.size() == 1) {
                    if (storeFleetButton.isEnabled()) storeFleetButton.setEnabled(false);
                } else if (mothballedShips == null || mothballedShips.size() == 0) {
                    if (pullAllShipsButton.isEnabled()) pullAllShipsButton.setEnabled(false);
                }
            }

        } else {
            storeFleetButton = null;
            pullAllShipsButton = null;
        }
    }

    private Object getPickedUpMember() {
        return ReflectionUtilis.invokeMethodDirectly(ClassRefs.fleetPanelClickAndDropHandlerGetPickedUpMemberMethod, fleetPanelClickHandler);
    }

    private Object getFleetPanel() {
        return ReflectionUtilis.invokeMethodDirectly(ClassRefs.fleetTabGetFleetPanelMethod, fleetTab);
    }
}