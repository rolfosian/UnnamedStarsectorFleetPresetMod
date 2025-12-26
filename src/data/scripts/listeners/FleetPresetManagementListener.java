package data.scripts.listeners;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.FleetMemberPickerListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipLocation;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.ScrollPanelAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TextFieldAPI;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.input.InputEventClass;
import com.fs.starfarer.api.input.InputEventType;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.ui.UITable;

import data.scripts.ClassRefs;
import data.scripts.listeners.DockingListener;

import data.scripts.ui.BaseSelfRefreshingPanel;
import data.scripts.ui.FleetIconPanel;
import data.scripts.ui.PartialRestorationDialog;
import data.scripts.ui.TreeTraverser;
import data.scripts.ui.UIComponent;
import data.scripts.ui.UIPanel;
import data.scripts.ui.TreeTraverser.TreeNode;
import data.scripts.ui.UIConfig;

import data.scripts.util.ReflectionUtilis;
import data.scripts.util.ListenerFactory.DialogDismissedListener;
import data.scripts.util.ListenerFactory.ActionListener;
import data.scripts.util.UtilReflection;
import data.scripts.util.UtilReflection.*;
import data.scripts.util.PresetUtils;
import data.scripts.util.PresetUtils.FleetMemberWrapper;
import data.scripts.util.PresetUtils.FleetPreset;
import data.scripts.util.PresetMiscUtils;

import java.awt.Color;
import java.util.*;

import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;

public class FleetPresetManagementListener extends ActionListener {
    private static void print(Object... args) {
        PresetMiscUtils.print(args);
    }

    public static final float CONFIRM_DIALOG_WIDTH = UIConfig.DISPLAY_WIDTH / UIConfig.CONFIRM_DIALOG_WIDTH_DIVISOR;
    public static final float CONFIRM_DIALOG_HEIGHT = UIConfig.DISPLAY_HEIGHT / UIConfig.CONFIRM_DIALOG_HEIGHT_DIVISOR;

    private static final float PANEL_WIDTH = UIConfig.DISPLAY_WIDTH / UIConfig.CONFIRM_DIALOG_WIDTH_DIVISOR - UIConfig.PANEL_WIDTH_SUBTRACTOR;
    private static final float PANEL_HEIGHT = UIConfig.DISPLAY_HEIGHT / UIConfig.CONFIRM_DIALOG_HEIGHT_DIVISOR - UIConfig.PANEL_HEIGHT_SUBTRACTOR;
    
    private static final float NAME_COLUMN_WIDTH = PANEL_WIDTH / UIConfig.NAME_COLUMN_WIDTH_DIVISOR;
    private static final float SHIP_COLUMN_WIDTH = PANEL_WIDTH / UIConfig.SHIP_COLUMN_WIDTH_DIVISOR;

    private static final float FLOAT_ZERO = 0f;
    
    private static float CANCEL_CONFIRM_BUTTON_WIDTH;

    private static final String EMPTY_STRING = "";
    private static final String QUESTON_MARK = "?";
    private static final String CLOSE_TEXT = "Close";
    private static final String CONFIRM_TEXT = "Confirm";
    private static final String CANCEL_TEXT = "Cancel";

    private static final String SAVE_DIALOG_HEADER = "Enter Preset Name:";
    private static final String SAVE_DIALOG_YES_TEXT = "Save Preset";
    private static final String DELETE_DIALOG_HEADER_PREFIX = "Are you sure you want to delete ";
    private static final String OVERWRITE_DIALOG_HEADE_PREFIX = "Are you sure you want to overwrite ";
    private static final String OVERWRITE_DIALOG_HEADE_SUFFIX = " with the current fleet?";
    private static final String RENAME_DIALOG_HEADE_PREFIX = "Are you sure you want to rename ";
    
    private static final String SAVE_DIALOG_BUTTON_ID = "saveDialogButton";
    private static final String SAVE_DIALOG_BUTTON_TOOLTIP_PARA_TEXT = "Saves the current fleet as preset.";
    private static final String SAVE_DIALOG_BUTTON_TEXT = "SAVE FLEET";

    private static final String RESTORE_BUTTON_ID = "restoreButton"; 
    private static final String RESTORE_BUTTON_TOOLTIP_PARA_TEXT = "Restores the selected preset.";
    private static final String RESTORE_BUTTON_TEXT  = "RESTORE";

    private static final String PARTIAL_RESTORE_BUTTON_ID = "partialRestoreButton";
    private static final String PARTIAL_RESTORE_BUTTON_TOOLTIP_PARA_TEXT = "Opens a dialog for ship selection and partial preset restoration.";
    private static final String PARTIAL_RESTORE_BUTTON_TEXT = "PART. RESTORE";

    private static final String STORE_BUTTON_ID = "storeButton";
    private static final String STORE_BUTTON_TOOLTIP_PARA_TEXT = "Stores the current fleet in storage.";
    private static final String STORE_BUTTON_TEXT  = "STORE FLEET";

    private static final String DELETE_BUTTON_ID = "deleteButton";
    private static final String DELETE_BUTTON_TOOLTIP_PARA_TEXT = "Deletes the selected preset.";
    private static final String DELETE_BUTTON_TEXT = "DELETE";

    // private static final String OVERWRITE_PRESET_BUTTON_ID = "overwriteToPresetButton";
    // private static final String OVERWRITE_PRESET_BUTTON_TOOLTIP_PARA_TEXT = "Overwrites the selected preset with the current fleet.";
    // private static final String OVERWRITE_PRESET_BUTTON_TEXT = "UPDATE";

    private static final String AUTO_UPDATE_BUTTON_ID = "autoUpdateButton";
    private static final String AUTO_UPDATE_BUTTON_TOOLTIP_PARA_TEXT = "Toggle to automatically update the preset when the fleet changes, if undocked with a COMPLETE preset fleet.";
    private static final String AUTO_UPDATE_BUTTON_TEXT = "AUTO UPDATE";

    // the underlying function for this is broken and needs work and i cannot be bothered right now
    // private static final String KEEP_CARGO_RATIOS_BUTTON_ID = "cargoRatiosButton";
    // private static final String KEEP_CARGO_RATIOS_BUTTON_PARA_TEXT = "Toggle to keep the supplies/fuel/crew ratios when switching fleets.";
    // private static final String KEEP_CARGO_RATIOS_BUTTON_TEXT = "EQUALIZE CARGO";
    
    private static final String BLANK_TABLE_TEXT = "Presets Go Here";
    private static final Color c1 = Global.getSettings().getBasePlayerColor();
    private static final Color c2 = Global.getSettings().getDarkPlayerColor();
    private static final Color TEXT_HIGHLIGHT_COLOR = Misc.getHighlightColor();

    private void selectPreset(String presetName, int rowIndex) {
        this.selectedPresetName = presetName;
        this.selectedRowIndex = rowIndex;
        this.selectedPreset = currentTableMap.get(selectedPresetName);
    }

    private void resetTopLevelVars() {
        this.saveNameField = null;

        this.selectedPreset = null;
        this.selectedPresetName = EMPTY_STRING;
        this.selectedPresetNamePara = null;
        this.selectedPresetNameParaFormat = "Selected Preset: %s";

        this.isSelectedPresetAvailablePara = null;
        this.isSelectedPresetAvailableParaFormat = "Selected Preset is %s at this location";

        this.isSelectedPresetFleetPara = null;

        this.selectedRowIndex = -1;
        this.currentPresetsNum = 0;

        this.overlordPanel = null;
        this.overlordPanelPos = null;
        this.buttonsPanel = null;

        this.theButtons = new HashMap<>();
        this.masterCancelButton = null;
        
        this.fenaglePanele = null;
        this.tablePanel = null;
        this.tableRowListeners = new ArrayList<>();
        this.tablePlugin = null;
        this.tableCanvasPos = null;
        this.currentTableMap = null;
        this.tableUp = true;
        this.tableRight = false;
        this.tablePresetNamesColumnHeader = "Presets <Ascending>";

        if (this.mangledFleet != null) this.mangledFleet.despawn();
        this.mangledFleet = null;
        this.whichMembersAvailable = null;
        // this.tableShipsColumnHeader = "Ships <Descending>";
    }

    private TextFieldAPI saveNameField = null;

    private String selectedPresetName = EMPTY_STRING;
    private FleetPreset selectedPreset = null;
    
    private LabelAPI selectedPresetNamePara = null;
    private String selectedPresetNameParaFormat = "Selected Preset: %s";

    private LabelAPI isSelectedPresetAvailablePara = null;
    private String isSelectedPresetAvailableParaFormat = "Selected Preset is %s at this location";

    private LabelAPI isSelectedPresetFleetPara = null;

    private int selectedRowIndex = -1;
    private int currentPresetsNum = 0;

    private ConfirmDialogData overlord;
    private PositionAPI overlordPanelPos = null;
    private UIPanelAPI overlordPanel = null;
    private CustomPanelAPI buttonsPanel = null;
    private Map<String, ButtonAPI> theButtons = new HashMap<>();
    private ButtonAPI masterCancelButton = null;
    private FenaglePanele fenaglePanele = null;

    private UITable tablePanel = null;
    private List<TableRowListener> tableRowListeners = new ArrayList<>();
    private TablePlugin tablePlugin = null;
    private PositionAPI tableCanvasPos = null;
    private LinkedHashMap<String, PresetUtils.FleetPreset> currentTableMap = null;
    private boolean tableUp = true;
    private boolean tableRight = false;
    private String tablePresetNamesColumnHeader = "Presets <Ascending>";
        // private String tableShipsColumnHeader = "Ships <Descending>";

    Map<Integer, FleetMemberAPI> whichMembersAvailable = null;
    private CampaignFleetAPI mangledFleet = null;
    private boolean isPartialSelecting = false;

    private final DockingListener dockingListener;

    public FleetPresetManagementListener() {
        super();
        dockingListener = PresetUtils.getDockingListener();
    }

    @Override
    public void trigger(Object... args) {
        PresetUtils.cleanUpPerishedPresetMembers();
        if (PresetUtils.isAutoUpdatePresets()) PresetUtils.updateFleetPresetStats(Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy());

        CustomPanelAPI tableMasterPanel = Global.getSettings().createCustom(PANEL_WIDTH - CANCEL_CONFIRM_BUTTON_WIDTH - 5f, PANEL_HEIGHT, new BaseCustomUIPanelPlugin() );
        ConfirmDialogData master = UtilReflection.showConfirmationDialog(
            EMPTY_STRING,
            EMPTY_STRING,
            CLOSE_TEXT,
            CONFIRM_DIALOG_WIDTH,
            CONFIRM_DIALOG_HEIGHT,
            new DialogDismissedListener() {
                @Override
                public void trigger(Object... args) {
                    // resetTopLevelVars();
                }
        });

        if (master == null) return;
        master.panel.removeComponent(master.confirmButton.getInstance());
        master.addGridLines(0.13f, true, false, true, Misc.getDarkPlayerColor());

        overlord = master;
        overlordPanel = master.panel;
        overlordPanelPos = master.panel.getPosition();

        ButtonAPI cancelButton = master.cancelButton.getInstance();
        PositionAPI cancelButtonPosition = cancelButton.getPosition();
        CANCEL_CONFIRM_BUTTON_WIDTH = cancelButtonPosition.getWidth();
        cancelButton.setShortcut(Keyboard.KEY_G, false);
        UtilReflection.setButtonHook(cancelButton, () -> resetTopLevelVars(), () -> {});

        ButtonPlugin buttonPlugin = new ButtonPlugin();
        buttonsPanel = Global.getSettings().createCustom(CANCEL_CONFIRM_BUTTON_WIDTH, PANEL_HEIGHT, buttonPlugin);
        TooltipMakerAPI tooltipMaker = buttonsPanel.createUIElement(CANCEL_CONFIRM_BUTTON_WIDTH+2f, PANEL_HEIGHT, false);
        buttonPlugin.init(buttonsPanel, tooltipMaker);

        addTheButtons(tooltipMaker, cancelButtonPosition);

        String storageAvailableText;
        Color storageAvailableColor;
        if (dockingListener.canPlayerAccessStorage(dockingListener.getPlayerCurrentMarket())) {
            storageAvailableText = "Storage Available";
            storageAvailableColor = Misc.getPositiveHighlightColor();
        } else {
            storageAvailableText = "Storage Unavailable";
            storageAvailableColor = Misc.getNegativeHighlightColor();
        }
        tooltipMaker.addPara(storageAvailableText, storageAvailableColor, 5f);

        isSelectedPresetAvailablePara = tooltipMaker.addParaWithMarkup("", c1, 5f);
        isSelectedPresetFleetPara = tooltipMaker.addParaWithMarkup("", c1, 5f);

        // data.panel.removeComponent(cancelButton);
        this.masterCancelButton = cancelButton;
        tablePlugin = new TablePlugin();
        CustomPanelAPI canvasPanel = Global.getSettings().createCustom(PANEL_WIDTH - CANCEL_CONFIRM_BUTTON_WIDTH - SHIP_COLUMN_WIDTH - 10f, PANEL_HEIGHT, tablePlugin);
        canvasPanel.addComponent(tableMasterPanel).inTL(FLOAT_ZERO, FLOAT_ZERO);
        fenaglePanele = new FenaglePanele(master.panel, canvasPanel);

        buttonsPanel.addUIElement(tooltipMaker);
        
        master.panel.addComponent(buttonsPanel).inTL(FLOAT_ZERO, FLOAT_ZERO);
        master.panel.addComponent(canvasPanel).rightOfTop(buttonsPanel, 10f);

        tablePlugin.setRoot(tableMasterPanel);
    }
    
    private void addTheButtons(TooltipMakerAPI tooltipMaker, PositionAPI cancelPosition) {
        float buttonWidth = cancelPosition.getWidth();
        float buttonHeight = cancelPosition.getHeight();
        int i = 2;

        ButtonAPI saveDialogButton = tooltipMaker.addButton(SAVE_DIALOG_BUTTON_TEXT, SAVE_DIALOG_BUTTON_ID, c1, c2,
        Alignment.BR, CutStyle.ALL, buttonWidth, buttonHeight, 5f);
        saveDialogButton.setShortcut(i, false);
        tooltipMaker.addTooltipTo(tc(SAVE_DIALOG_BUTTON_TOOLTIP_PARA_TEXT), saveDialogButton, TooltipLocation.RIGHT, false);
        i++;

        ButtonAPI restorePresetButton = tooltipMaker.addButton(RESTORE_BUTTON_TEXT, RESTORE_BUTTON_ID, c1, c2,
        Alignment.BR, CutStyle.ALL, buttonWidth, buttonHeight, 5f);
        restorePresetButton.setShortcut(i, false);
        tooltipMaker.addTooltipTo(tc(RESTORE_BUTTON_TOOLTIP_PARA_TEXT), restorePresetButton, TooltipLocation.RIGHT, false);
        i++;

        ButtonAPI partialRestorePresetButton = tooltipMaker.addButton(PARTIAL_RESTORE_BUTTON_TEXT, PARTIAL_RESTORE_BUTTON_ID, c1, c2,
        Alignment.BR, CutStyle.ALL, buttonWidth, buttonHeight, 5f);
        partialRestorePresetButton.setShortcut(i, false);
        tooltipMaker.addTooltipTo(tc(PARTIAL_RESTORE_BUTTON_TOOLTIP_PARA_TEXT), partialRestorePresetButton, TooltipLocation.RIGHT, false);
        i++;

        ButtonAPI storeAllButton = tooltipMaker.addButton(STORE_BUTTON_TEXT, STORE_BUTTON_ID, c1, c2,
        Alignment.BR, CutStyle.ALL, buttonWidth, buttonHeight, 5f);
        storeAllButton.setShortcut(i, false);
        tooltipMaker.addTooltipTo(tc(STORE_BUTTON_TOOLTIP_PARA_TEXT), storeAllButton, TooltipLocation.RIGHT, false);
        i++;

        ButtonAPI deleteButton = tooltipMaker.addButton(DELETE_BUTTON_TEXT, DELETE_BUTTON_ID, c1, c2,
        Alignment.BR, CutStyle.ALL, buttonWidth, buttonHeight, 5f);
        deleteButton.setShortcut(i, false);
        tooltipMaker.addTooltipTo(tc(DELETE_BUTTON_TOOLTIP_PARA_TEXT), deleteButton, TooltipLocation.RIGHT, false);
        i++;

        // ButtonAPI overwriteToPresetButton = tooltipMaker.addButton(OVERWRITE_PRESET_BUTTON_TEXT, OVERWRITE_PRESET_BUTTON_ID, c1, c2,
        // Alignment.BR, CutStyle.ALL, buttonWidth, buttonHeight, 5f);
        // overwriteToPresetButton.setShortcut(i, false);
        // tooltipMaker.addTooltipTo(tc(OVERWRITE_PRESET_BUTTON_TOOLTIP_PARA_TEXT), overwriteToPresetButton, TooltipLocation.RIGHT, false);
        // i++;

        ButtonAPI autoUpdateButton = tooltipMaker.addCheckbox(buttonWidth, buttonHeight, AUTO_UPDATE_BUTTON_TEXT, AUTO_UPDATE_BUTTON_ID, Fonts.ORBITRON_12, c1,
        ButtonAPI.UICheckboxSize.SMALL, 5f);
        autoUpdateButton.setChecked(PresetUtils.isAutoUpdatePresets());
        tooltipMaker.addTooltipTo(tc(AUTO_UPDATE_BUTTON_TOOLTIP_PARA_TEXT), autoUpdateButton, TooltipLocation.RIGHT, false);

        // ButtonAPI cargoRatiosButton = tooltipMaker.addCheckbox(buttonWidth, buttonHeight, KEEP_CARGO_RATIOS_BUTTON_TEXT, KEEP_CARGO_RATIOS_BUTTON_ID, Fonts.ORBITRON_12, c1,
        // ButtonAPI.UICheckboxSize.SMALL, 5f);
        // cargoRatiosButton.setChecked((boolean)Global.getSector().getPersistentData().get(PresetUtils.KEEPCARGORATIOS_KEY));
        // tooltipMaker.addTooltipTo(tc(KEEP_CARGO_RATIOS_BUTTON_PARA_TEXT), cargoRatiosButton, TooltipLocation.RIGHT, false);

        theButtons.put(SAVE_DIALOG_BUTTON_ID, saveDialogButton);
        theButtons.put(RESTORE_BUTTON_ID, restorePresetButton);
        theButtons.put(STORE_BUTTON_ID, storeAllButton);
        theButtons.put(DELETE_BUTTON_ID, deleteButton);
        // theButtons.put(OVERWRITE_PRESET_BUTTON_ID, overwriteToPresetButton);
        theButtons.put(AUTO_UPDATE_BUTTON_ID, autoUpdateButton);
        theButtons.put(PARTIAL_RESTORE_BUTTON_ID, partialRestorePresetButton);
        // theButtons.put(KEEP_CARGO_RATIOS_BUTTON_ID, cargoRatiosButton);
        disableButtonsRequiringSelection();
        enableButtonsRequiringSelection();
        return;
    }

    private void openRenameDialog(String oldName, String newName, Object oldSaveButtonListener, ButtonAPI saveButton, Object cancelButtonListener, ButtonAPI cancelButton) {
        ConfirmDialogData subData = UtilReflection.showConfirmationDialog(
            RENAME_DIALOG_HEADE_PREFIX + oldName + " to " + newName + QUESTON_MARK,
            CONFIRM_TEXT,
            CANCEL_TEXT,
            CONFIRM_DIALOG_WIDTH / 1.5f,
            CONFIRM_DIALOG_HEIGHT / 4,
            new DialogDismissedListener() {
                @Override
                public void trigger(Object... args) {
                    if ((int)args[1] == 0) {
                        // confirm
                        if (currentTableMap.containsKey(newName)) {
                            selectPreset(newName, getTableMapIndex(newName));
                            openOverwriteDialog(oldSaveButtonListener, saveButton);
                            return;
                        }

                        PresetUtils.deleteFleetPreset(oldName);
                        PresetUtils.saveFleetPreset(newName);

                        refreshTableMap();
                        selectPreset(newName, getTableMapIndex(newName));
                        tablePlugin.rebuild();
                        enableButtonsRequiringSelection();

                        ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonListenerActionPerformedMethod,
                        cancelButtonListener,
                        UtilReflection.createButtonClickEventInstance(cancelButton.getPosition()),
                        cancelButton);
                        return;
                    }
                    if (saveNameField != null) saveNameField.grabFocus(false);
                }
            });
            subData.confirmButton.getInstance().setShortcut(Keyboard.KEY_G, false);
    }

    private void openOverwriteDialog(Object oldSaveButtonListener, ButtonAPI saveButton) {

        ConfirmDialogData subData = UtilReflection.showConfirmationDialog(
            OVERWRITE_DIALOG_HEADE_PREFIX + selectedPresetName + OVERWRITE_DIALOG_HEADE_SUFFIX,
            CONFIRM_TEXT,
            CANCEL_TEXT,
            CONFIRM_DIALOG_WIDTH / 1.5f,
            CONFIRM_DIALOG_HEIGHT / 4,
            new DialogDismissedListener() {
                @Override
                public void trigger(Object... args) {
                    if ((int)args[1] == 0) {
                        // confirm

                        ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonListenerActionPerformedMethod,
                        oldSaveButtonListener,
                        UtilReflection.createButtonClickEventInstance(saveButton.getPosition()),
                        saveButton);
                        return;
                    }

                    if (saveNameField != null) saveNameField.grabFocus(false);
                }
            });
        subData.confirmButton.getInstance().setShortcut(Keyboard.KEY_G, false);
    }

    @SuppressWarnings("unchecked")
    private void openSaveDialog() {
        CustomPanelAPI textFieldPanel = Global.getSettings().createCustom(CONFIRM_DIALOG_WIDTH / 2 / 6, CONFIRM_DIALOG_HEIGHT / 2 / 12, null);
        TooltipMakerAPI textFieldTooltipMaker = textFieldPanel.createUIElement(CONFIRM_DIALOG_WIDTH / 2 / 5, CONFIRM_DIALOG_HEIGHT / 2 / 10, false);
        saveNameField = textFieldTooltipMaker.addTextField(CONFIRM_DIALOG_WIDTH/3, CONFIRM_DIALOG_HEIGHT/2/3, "graphics/fonts/orbitron24aabold.fnt", 10f);
        textFieldPanel.addUIElement(textFieldTooltipMaker).inTL(0f, 0f);

        ConfirmDialogData subData = UtilReflection.showConfirmationDialog(
            "graphics/illustrations/entering_hyperspace.jpg",
            SAVE_DIALOG_HEADER,
            SAVE_DIALOG_YES_TEXT,
            CANCEL_TEXT,
            CONFIRM_DIALOG_WIDTH / 1.5f,
            CONFIRM_DIALOG_HEIGHT / 2,
            new SaveListener()
        );
        setSaveButtonListenerInterceptor(subData.confirmButton.getInstance(), subData.cancelButton.getInstance());

        SaveNameFieldInputInterceptor plugin = new SaveNameFieldInputInterceptor();
        ((UIPanelAPI)saveNameField).addComponent(Global.getSettings().createCustom(0f, 0f, plugin)); // we arent done with the interceptor yet. refer to plugin.init

        CustomPanelAPI imgButtonPanel = Global.getSettings().createCustom(172f, 172f, null);
        TooltipMakerAPI imageButtonTt = imgButtonPanel.createUIElement(172f, 172f, false);
        imgButtonPanel.addUIElement(imageButtonTt).setYAlignOffset(1f);
        imageButtonTt.beginTable(c2, c2, c2, 54f, true, false, new Object[]{"", 170f});
        imageButtonTt.addTable("", 0, 0f);
        subData.panel.addComponent((UIComponentAPI)imgButtonPanel).inTL(subData.panel.getPosition().getWidth() - 185f, 12f);

        String sectorSeedString = Global.getSector().getSeedString();
        addVerticalSeedString(subData.panel, sectorSeedString);

        float yOffset = 0f;
        float xOffset = 0f;
        int column = 0;
        int row = 0;
        for (String[] pair : PresetUtils.FLEET_TYPES) {
            String fleetType = pair[0];
            String icon = pair[1];

            CustomPanelAPI imgTooltipPanel = Global.getSettings().createCustom(50f, 50f, new BaseCustomUIPanelPlugin() {
                @Override
                public void buttonPressed(Object buttonId) {
                    if (saveNameField.getText().equals(fleetType)) {
                        saveNameField.setText("");
                        saveNameField.grabFocus();
                    } else {
                        saveNameField.setText(fleetType);
                        saveNameField.grabFocus();
                    }
                }
            });
    
            TooltipMakerAPI imgTooltip = imgTooltipPanel.createUIElement(50f, 50f, false);
            imgTooltip.addImage(icon, 50f, 50f, 0f);
            imgTooltipPanel.addUIElement(imgTooltip);

            ButtonAPI button = imgTooltip.addButton("", "", Global.getSettings().getBasePlayerColor(), Global.getSettings().getBasePlayerColor(), Alignment.MID, CutStyle.NONE, 50f, 50f, 0f);
            button.setOpacity(0.15f);
            button.getPosition().setYAlignOffset(50f);

            xOffset = 5f + column * 55f;
            yOffset = 12f + row > 0 ? row * 55f : 0f;
            
            imgButtonPanel.addComponent(imgTooltipPanel).inTL(xOffset, yOffset);

            column++;
            if (column >= 3) {
                column = 0;
                row++;
            }
        }

        Object[] deployPtsBreakdown = PresetUtils.getDeploymentPointsBreakdown();
        String deployPts = String.valueOf(deployPtsBreakdown[0]);

        LabelAPI dumDumLabel = Global.getSettings().createLabel(deployPts, Fonts.ORBITRON_24AA);
        float width = dumDumLabel.computeTextWidth(deployPts);
        float height = dumDumLabel.computeTextHeight(deployPts);

        CustomPanelAPI ptsLabbelPanel = Global.getSettings().createCustom(width + 20f, height, null);
        TooltipMakerAPI ptsLabbelTooltip = ptsLabbelPanel.createUIElement(width + 20f, height, false);
        ptsLabbelTooltip.setParaOrbitronVeryLarge();

        LabelAPI ptsLabbel = ptsLabbelTooltip.addPara(deployPts, new Color(0, 177, 211), 10f);
        ptsLabbel.setHighlightOnMouseover(true);
        ptsLabbel.setHighlightColor(new Color(51, 193, 220));

        ptsLabbelTooltip.addTooltipTo(getPtsLabelTt((Map<String, String>)deployPtsBreakdown[1], Fonts.ORBITRON_16), ptsLabbelTooltip, TooltipLocation.RIGHT);
        ptsLabbelPanel.addUIElement(ptsLabbelTooltip);
        Object ptsLabbelTt = ReflectionUtilis.invokeMethodDirectly(ClassRefs.uiPanelGetTooltipMethod, ptsLabbelTooltip);

        subData.panel.addComponent(ptsLabbelPanel).inTL(0f, subData.panel.getPosition().getHeight() - height - 8f);
        subData.panel.addComponent(textFieldPanel).inTL(0f, 0f).setXAlignOffset(CONFIRM_DIALOG_WIDTH / 2 / 2 / 2 / 2 - 20f).setYAlignOffset(-CONFIRM_DIALOG_HEIGHT / 2 / 2 / 2);

        plugin.init(ptsLabbelTt, ptsLabbelPanel, ptsLabbelTooltip, subData);

        saveNameField.grabFocus(false);
    }

    private void setSaveButtonListenerInterceptor(ButtonAPI saveButton, ButtonAPI cancelButton) {
        Object oldSaveButtonListener = ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonGetListenerMethod, saveButton);
        Object cancelButtonListener = ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonGetListenerMethod, cancelButton);

        ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonSetListenerMethod, saveButton, new ActionListener() {
            @Override
            public void trigger(Object... args) {
                String text = saveNameField.getText();
                if (!isEmptyOrWhitespace(text)) {
                    FleetPreset possibleDuplicate = PresetUtils.getPresetOfMembers(Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy());

                    if (currentTableMap.containsKey(text)) {
                        if (possibleDuplicate != null && !possibleDuplicate.getName().equals(text)) {
                            new MessageBox("Duplicates are not allowed!", null).dialogData.addGridLines(0.1f, false, false, true, UtilReflection.DARK_RED);
                            return;
                        }

                        selectPreset(text, getTableMapIndex(text));
                        openOverwriteDialog(oldSaveButtonListener, saveButton);
                        return;
                    }
                    
                    
                    if (possibleDuplicate != null) {
                        openRenameDialog(possibleDuplicate.getName(), text, oldSaveButtonListener, saveButton, cancelButtonListener, cancelButton);
                        return;
                    }
                }
                ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonListenerActionPerformedMethod, oldSaveButtonListener, saveButton, args[1]);
            }
        }.getProxy());
    }

    private class SaveNameFieldInputInterceptor extends BaseCustomUIPanelPlugin {
        private Object tt;
        private boolean isShowingTt = false;

        private ConfirmDialogData subData;
        private CustomPanelAPI ptsLabbelPanel;
        private TooltipMakerAPI ptsLabbelTt;
        private TtHideEnsurer hideEnsurer = new TtHideEnsurer();

        float leftBound;
        float rightBound;
        float topBound;
        float btmBound;

        private class TtHideEnsurer implements EveryFrameScript {
            private boolean isDone = false;
            private boolean isActive = false;

            private IntervalUtil interval = new IntervalUtil(0.33f, 0.33f);

            @Override
            public void advance(float arg0) {
                interval.advance(arg0);

                if (interval.intervalElapsed() && !isShowingTt) {
                    isShowingTt = true;
                    ReflectionUtilis.invokeMethodDirectly(ClassRefs.uiPanelShowTooltipMethod, ptsLabbelTt, tt);
                }

                if (!saveNameField.hasFocus()) {
                    ReflectionUtilis.invokeMethodDirectly(ClassRefs.uiPanelHideTooltipMethod, ptsLabbelTt, tt);
                    isShowingTt = false;
                    isDone = true;
                    Global.getSector().removeTransientScript(this);
                }
            }

            @Override
            public boolean isDone() {
                return this.isDone;
            }

            @Override
            public boolean runWhilePaused() {
                return true;
            }

            public void setIsDone(boolean isDone) {
                this.isDone = isDone;
            }

            public void resetInterval() {
                this.interval.setElapsed(0f);
            }

            public boolean isActive() {
                return this.isActive;
            }

            public void setIsActive(boolean isActive) {
                this.isActive = isActive;
            }
        }

        @Override
        public void processInput(List<InputEventAPI> events) {
            for (InputEventAPI event : events) {
                // as the tooltip for the label sibling does not show when the text field is focused for some reason, although the highlighting fader for mouseover still does its job???
                if (saveNameField.hasFocus() && event.isMouseMoveEvent()) {
                    float mouseX = event.getX();
                    float mouseY = event.getY();

                    if (!isShowingTt && isInsideBounds(mouseX, mouseY) && !hideEnsurer.isActive()) {
                        hideEnsurer.setIsActive(true);
                        hideEnsurer.setIsDone(false);
                        Global.getSector().addTransientScript(hideEnsurer);

                    } else if (isShowingTt && !isInsideBounds(mouseX, mouseY)) {
                        ReflectionUtilis.invokeMethodDirectly(ClassRefs.uiPanelHideTooltipMethod, ptsLabbelTt, tt);
                        isShowingTt = false;
                        hideEnsurer.setIsDone(true);
                        hideEnsurer.setIsActive(false);
                        Global.getSector().removeTransientScript(hideEnsurer);
                        hideEnsurer.resetInterval();
                    }
                }

                // natively, pressing enter/escape with the text field focused will hand over focus to its parent, and then consume the event
                // with this we intercept the enter/escape inputs and synthetically click the confirm or cancel buttons so the player does not have to press enter/esc twice
                if (event.isKeyDownEvent() && saveNameField.hasFocus()) {
                    if (event.getEventValue() == Keyboard.KEY_RETURN) {
                        UtilReflection.clickButton(subData.confirmButton.getInstance());
                        break;
                    }
                    else if (event.getEventValue() == Keyboard.KEY_ESCAPE) {
                        UtilReflection.clickButton(subData.cancelButton.getInstance());
                        break;
                    }
                }
            }
        }

        private boolean isInsideBounds(float mouseX, float mouseY) {
            return (mouseX >= leftBound && mouseX <= rightBound &&
                    mouseY >= btmBound && mouseY <= topBound);
        }

        public void init(Object tt, CustomPanelAPI ptsLabbelPanel, TooltipMakerAPI ptsLabbelTt, ConfirmDialogData subData) {
            this.tt = tt;
            this.ptsLabbelPanel = ptsLabbelPanel;
            this.ptsLabbelTt = ptsLabbelTt;
            this.subData = subData;

            PositionAPI pos = ptsLabbelPanel.getPosition();
            this.leftBound = pos.getCenterX() - pos.getWidth() / 2;
            this.rightBound = pos.getCenterX() + pos.getWidth() / 2;
            this.topBound = pos.getCenterY() + pos.getHeight() / 2;
            this.btmBound = pos.getCenterY() - pos.getHeight() / 2;

        }
    }

    private void openDeleteDialog() {
        ConfirmDialogData subData = UtilReflection.showConfirmationDialog(
            DELETE_DIALOG_HEADER_PREFIX + selectedPresetName + QUESTON_MARK,
            CONFIRM_TEXT,
            CANCEL_TEXT,
            CONFIRM_DIALOG_WIDTH / 1.5f,
            CONFIRM_DIALOG_HEIGHT / 4,
            new DeleteListener());
        subData.confirmButton.setShortcut(Keyboard.KEY_G, false);
    }

    public static boolean isEmptyOrWhitespace(String s) {
        return s != null && s.trim().isEmpty();
    }

    private class ButtonPlugin implements CustomUIPanelPlugin {
        CustomPanelAPI masterPanel;
        TooltipMakerAPI masterTooltip;

        public ButtonPlugin() {}
        
        public void init(CustomPanelAPI panel, TooltipMakerAPI tooltip) {
            this.masterPanel = panel;
            this.masterTooltip = tooltip;
        }
    
        @Override
        public void buttonPressed(Object arg0) {
            switch ((String) arg0) {
                case SAVE_DIALOG_BUTTON_ID:
                    openSaveDialog();
                    return;

                case RESTORE_BUTTON_ID:
                    PresetUtils.restoreFleetFromPreset(selectedPresetName);

                    if (mangledFleet != null) {
                        tablePlugin.addShipList(mangledFleet, whichMembersAvailable);
                    } else {
                        tablePlugin.addShipList(selectedPreset.getCampaignFleet(), whichMembersAvailable);
                    }
                    setParas();

                    enableButtonsRequiringSelection();
                    return;

                case PARTIAL_RESTORE_BUTTON_ID:
                    showFleetMemberRecoveryDialog();
                    return;

                case STORE_BUTTON_ID:
                    PresetUtils.storeFleetInStorage();
                    setParas();
                    enableButtonsRequiringSelection();
                    return;
                    
                case DELETE_BUTTON_ID:
                    openDeleteDialog();
                    return;
                    
                // case OVERWRITE_PRESET_BUTTON_ID:
                //     openOverwriteDialog(false);
                //     return;

                case AUTO_UPDATE_BUTTON_ID:
                    if (theButtons.get(AUTO_UPDATE_BUTTON_ID).isChecked()) {
                        Global.getSector().getPersistentData().put(PresetUtils.IS_AUTO_UPDATE_KEY, true);
                        if (dockingListener.getPlayerCurrentMarket() == null) {
                            FleetPreset preset = PresetUtils.getPresetOfMembers(Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy());

                            if (preset != null) {
                                Global.getSector().getMemoryWithoutUpdate().set(PresetUtils.UNDOCKED_PRESET_KEY, preset);
                            }
                        }
                    } else {
                        Global.getSector().getPersistentData().put(PresetUtils.IS_AUTO_UPDATE_KEY, false);
                        Global.getSector().getMemoryWithoutUpdate().unset(PresetUtils.UNDOCKED_PRESET_KEY);
                    }
                    return;

                // case KEEP_CARGO_RATIOS_BUTTON_ID:
                //     if (theButtons.get(KEEP_CARGO_RATIOS_BUTTON_ID).isChecked()) {
                //         Global.getSector().getPersistentData().put(PresetUtils.KEEPCARGORATIOS_KEY, true);
                //     } else {
                //         Global.getSector().getPersistentData().put(PresetUtils.KEEPCARGORATIOS_KEY, false);
                //     }
                //     return;

                default:
                    break;
            }
        }

        private void navigateTableRows(int key, int rowNum) {
            boolean unselected = selectedRowIndex == -1;
            if (rowNum == 1 && !unselected) return;

            int direction = (tableUp ^ key == Keyboard.KEY_UP) ? 1 : -1;
            int newIndex = (unselected && key == Keyboard.KEY_UP) ? rowNum - 1 : (selectedRowIndex + direction + rowNum) % rowNum;

            TableRowListener rowListener = tableRowListeners.get(newIndex);
            UtilReflection.clickButton(rowListener.button);
        }

        @Override
        public void processInput(List<InputEventAPI> arg0) {
            for (InputEventAPI event : arg0) {
                if (event.isKeyboardEvent()) {
                    boolean isKeyDownEvent = event.isKeyDownEvent();

                    if (isKeyDownEvent) {
                        if (Keyboard.isKeyDown(Keyboard.KEY_RETURN) || Keyboard.isKeyDown(Keyboard.KEY_NUMPADENTER) || Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
                            event.consume();
                            continue;
                        }
                    }

                    if (!tableRowListeners.isEmpty()) {
                        int rowNum = tableRowListeners.size();
                        boolean keyDown = Keyboard.isKeyDown(Keyboard.KEY_DOWN);
                        boolean keyUp = Keyboard.isKeyDown(Keyboard.KEY_UP);

                        if (keyDown || keyUp) {
                            navigateTableRows(event.getEventValue(), rowNum);

                        } else if (isKeyDownEvent) {
                            if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE) && rowNum > 0 && selectedPresetName != EMPTY_STRING) {
                                if (selectedRowIndex != -1) {
                                    TableRowListener selectedRowListener = tableRowListeners.get(selectedRowIndex);
                                    ReflectionUtilis.invokeMethodDirectly(ClassRefs.tablePanelSelectMethod, tablePanel, null, null);
                                    tablePlugin.setRowColorAndText(selectedRowListener.row, new Object[] {c1, selectedRowListener.rowName});
                                    tablePlugin.addShipList(null, null);
                                }
    
                                selectedRowIndex = -1;
                                selectPreset(EMPTY_STRING, selectedRowIndex);
                                disableButtonsRequiringSelection();
                                isSelectedPresetAvailablePara.setText("");
                                event.consume();
                            }
                        } 
                    }
                } // else if (event.isLMBEvent()) {
                //     if (event.getX() < overlordPanelPos.getX() || 
                //         event.getX() > overlordPanelPos.getX() + overlordPanelPos.getWidth() ||
                //         event.getY() < overlordPanelPos.getY() || 
                //         event.getY() > overlordPanelPos.getY() + overlordPanelPos.getHeight()) {
                //         ReflectionUtilis.getMethodAndInvokeDirectly("setEventValue", event, 1, 1);
                //     }
                // }
            }
        }
        
        @Override
        public void render(float arg0) {}
    
        @Override
        public void renderBelow(float arg0) {}

        @Override
        public void positionChanged(PositionAPI arg0) {}

        @Override
        public void advance(float amount) {}
        
    }

    public void enableButtonsRequiringSelection() {
        if (selectedPresetName != EMPTY_STRING) {
            if (dockingListener.canPlayerAccessStorage(dockingListener.getPlayerCurrentMarket())) {
                theButtons.get(PARTIAL_RESTORE_BUTTON_ID).setEnabled(true);

                if (Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy().size() > 1) {
                    theButtons.get(STORE_BUTTON_ID).setEnabled(true);
                } else {
                    theButtons.get(STORE_BUTTON_ID).setEnabled(false);
                }
                
                if (!PresetUtils.isPresetPlayerFleet(selectedPreset)) {
                    theButtons.get(RESTORE_BUTTON_ID).setEnabled(true);
                } else {
                    theButtons.get(RESTORE_BUTTON_ID).setEnabled(false);
                }
            }
            // theButtons.get(OVERWRITE_PRESET_BUTTON_ID).setEnabled(true);
            theButtons.get(DELETE_BUTTON_ID).setEnabled(true);

        } else {
            theButtons.get(RESTORE_BUTTON_ID).setEnabled(false);
            theButtons.get(PARTIAL_RESTORE_BUTTON_ID).setEnabled(false);
            // theButtons.get(OVERWRITE_PRESET_BUTTON_ID).setEnabled(false);
            theButtons.get(DELETE_BUTTON_ID).setEnabled(false);

            if (dockingListener.canPlayerAccessStorage(dockingListener.getPlayerCurrentMarket())
                && Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy().size() > 1) {
                theButtons.get(STORE_BUTTON_ID).setEnabled(true);
            } else {
                theButtons.get(STORE_BUTTON_ID).setEnabled(false);
            }
        }
    }

    private void disableButtonsRequiringSelection() {
        theButtons.get(DELETE_BUTTON_ID).setEnabled(false);
        theButtons.get(RESTORE_BUTTON_ID).setEnabled(false);
        theButtons.get(PARTIAL_RESTORE_BUTTON_ID).setEnabled(false);
        // theButtons.get(OVERWRITE_PRESET_BUTTON_ID).setEnabled(false);

        if (dockingListener.getPlayerCurrentMarket() != null && dockingListener.canPlayerAccessStorage(dockingListener.getPlayerCurrentMarket())
            && Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy().size() > 1) {
            theButtons.get(STORE_BUTTON_ID).setEnabled(true);
        } else {
            theButtons.get(STORE_BUTTON_ID).setEnabled(false);
        }
    }

    public class TablePlugin extends BaseSelfRefreshingPanel {
        public LabelAPI label;
        public boolean rebuild;
        public UIPanelAPI panel;
        
        private TooltipMakerAPI tableTipMaker;
        private CustomPanelAPI shipsPanel;
        private UIPanelAPI fleetInfoPanel;
        public float yScrollOffset;

        public TablePlugin() {}

        // @Override
        // public void setRoot(CustomPanelAPI panel) {
        //     super.setRoot(panel);
        //     this.advance(0.0f);

        //     float dividerLineX = tablePanel.getPosition().getX() + tablePanel.getPosition().getWidth();
        //     float dividerLineY = overlordPanel.getPosition().getY() + (overlordPanel.getPosition().getHeight() / 2);

        //     CustomPanelAPI linePanel = Global.getSettings().createCustom(0f, 0f, new BaseCustomUIPanelPlugin() {
        //         @Override
        //         public void render(float alphaMult) {
        //             PresetMiscUtils.drawTaperedLine(c2, 
        //             dividerLineX,
        //             dividerLineY,
        //             90f, 
        //             CONFIRM_DIALOG_HEIGHT, 
        //             2f);
        //         }
        //     });
        //     overlordPanel.addComponent(linePanel).inTL(0f, 0f);
        // }

        @Override
        public void rebuild() {
            if (tableTipMaker != null) {
                yScrollOffset = tableTipMaker.getExternalScroller().getYOffset();
            }
            super.rebuild();
        }

        @Override
        public void positionChanged(PositionAPI position) {}
    
        @Override
        public void renderBelow(float alphaMult) {}
    
        @Override
        public void render(float alphaMult) {}

        public void setRowColorAndText(Object row, Object[] colorAndText) {
            ReflectionUtilis.setPrivateVariable(ClassRefs.tableRowParamsField, row, colorAndText);
            ReflectionUtilis.setPrivateVariable(ClassRefs.tableRowCreatedField, row, false); // setting created to false makes the renderer reinitialize when it is called and use the new color/text
            ReflectionUtilis.invokeMethodDirectly(ClassRefs.tableRowRenderMethod, row, 0.01f);
        }

        private void setRowButtonHook(TableRowListener rowListener) {
            Object oldListener = ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonGetListenerMethod, rowListener.button);

            ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonSetListenerMethod, rowListener.button, new ActionListener() {
                @Override
                public void trigger(Object... args) {
                    if (selectedRowIndex == rowListener.id) {
                        return;
                    } else if (selectedRowIndex != -1) {
                        TableRowListener previousSelected = tableRowListeners.get(selectedRowIndex);
                        setRowColorAndText(previousSelected.row, new Object[] {c1, previousSelected.rowName});
                    }

                    selectPreset(rowListener.rowName, rowListener.id);
                    setRowColorAndText(rowListener.row, new Object[] {TEXT_HIGHLIGHT_COLOR, rowListener.rowName});
                    // ReflectionUtilis.invokeMethodDirectly(ClassRefs.tablePanelSelectMethod, tablePanel, rowListener.row, null);

                    if (selectedPresetName != EMPTY_STRING) {
                        selectedPreset = currentTableMap.get(selectedPresetName);
        
                        // in case there is a matching member in storage with the same variant but not the exact same member the preset was saved with and that member is stored somewhere else
                        Map<FleetMemberWrapper, FleetMemberAPI> neededMembers = PresetUtils.getIdAgnosticRequiredMembers(dockingListener.getPlayerCurrentMarket(), selectedPresetName);
        
                        setParas();
        
                        if (neededMembers != null) {
                            mangledFleet = PresetUtils.mangleFleet(neededMembers, selectedPreset.getCampaignFleet());
                            addShipList(mangledFleet, whichMembersAvailable);
                        } else {
                            addShipList(selectedPreset.getCampaignFleet(), whichMembersAvailable);
                        }
        
                    } else {
                        isSelectedPresetAvailablePara.setText(EMPTY_STRING);
                        addShipList(null, null);
                    };

                    ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonListenerActionPerformedMethod, oldListener, args);
                    enableButtonsRequiringSelection();
                }
            }.getProxy());
        }

        private void processRow(UIPanelAPI row, String rowName, int id) {
            PositionAPI rowPos = row.getPosition();

            TableRowListener rowListener = new TableRowListener(row, (ButtonAPI) ReflectionUtilis.invokeMethodDirectly(ClassRefs.tableRowGetButtonMethod, row), rowPos, rowName, id);
            // CustomPanelAPI rowOverlayPanel = Global.getSettings().createCustom(NAME_COLUMN_WIDTH, 29f, rowListener);
            // TooltipMakerAPI rowOverlayTooltipMaker = rowOverlayPanel.createUIElement(NAME_COLUMN_WIDTH, 29f, false);

            // tableTipMaker.addComponent(rowOverlayPanel).inTL(rowPos.getX(), rowPos.getY());
            // rowListener.init(rowOverlayPanel, rowOverlayTooltipMaker, rowPos);
            
            tableRowListeners.add(rowListener);
        }

        @Override
        public void buildTooltip(CustomPanelAPI panel) {
            refreshTableMap();

            whichMembersAvailable = null;
            if (mangledFleet != null) {
                mangledFleet.despawn();
                mangledFleet = null;
            }

            tableTipMaker = panel.createUIElement(NAME_COLUMN_WIDTH, PANEL_HEIGHT, true);
            
            tablePanel = (UITable) tableTipMaker.beginTable(c1, c2, Misc.getHighlightedOptionColor(), 30f, false, false, 
            new Object[]{tablePresetNamesColumnHeader, NAME_COLUMN_WIDTH - 10f });
            // tablePanel = tableTipMaker.beginTable2(Global.getSector().getPlayerFaction(), 30f, true, true, 
            // new Object[]{tablePresetNamesColumnHeader, NAME_COLUMN_WIDTH - 1f});
            
            tableRowListeners.clear();
            int id;
            // int size = currentTableMap.size();
            
            // this mechanism works if there's a table header but i took it out because it was a superfluous and unnecessary feature, see tablerowlistener processinput for more details
            // if (tableUp) {
                id = 0;
            // } else {
                // id = (size == 1) ? 0 : size - 1;
            // }

            UIPanelAPI selectedRow = null;
            for (Map.Entry<String, PresetUtils.FleetPreset> entry: currentTableMap.entrySet()) {
                String rowName = entry.getKey();

                UIPanelAPI row;
                if (selectedRowIndex == id) {
                    row = (UIPanelAPI) tableTipMaker.addRowWithGlow(
                        TEXT_HIGHLIGHT_COLOR, 
                        rowName
                    );
                    selectedRow = row;
                } else {
                    row = (UIPanelAPI) tableTipMaker.addRowWithGlow(
                        c1,
                        rowName
                    );
                } 
                
                processRow(row, rowName, id);
                // id += tableUp ? 1 : -1;
                id++;
            }
            tableTipMaker.addTable(BLANK_TABLE_TEXT, 0, 5f);

            tablePanel.setItemsSelectable(true);
            if (selectedRow != null) {
                ReflectionUtilis.invokeMethodDirectly(ClassRefs.tablePanelSelectMethod, tablePanel, selectedRow, null);
            }

            if (selectedPresetName != EMPTY_STRING) {
                selectedPreset = currentTableMap.get(selectedPresetName);

                // in case there is a matching member in storage with the same variant but not the exact same member the preset was saved with
                Map<FleetMemberWrapper, FleetMemberAPI> neededMembers = PresetUtils.getIdAgnosticRequiredMembers(dockingListener.getPlayerCurrentMarket(), selectedPresetName);

                setParas();

                if (neededMembers != null) {
                    mangledFleet = PresetUtils.mangleFleet(neededMembers, selectedPreset.getCampaignFleet());
                    addShipList(mangledFleet, whichMembersAvailable);
                } else {
                    addShipList(selectedPreset.getCampaignFleet(), whichMembersAvailable);
                }

            } else {
                isSelectedPresetAvailablePara.setText(EMPTY_STRING);
                addShipList(null, null);
            }

            panel.addUIElement(tableTipMaker);

            for (TableRowListener rowListener : tableRowListeners) setRowButtonHook(rowListener);

            tableTipMaker.getExternalScroller().setYOffset(yScrollOffset);
        }

        public void addShipList(CampaignFleetAPI fleet, Map<Integer, FleetMemberAPI> whichMembersAvailable) {
            if (fleetInfoPanel != null && shipsPanel != null) {
                shipsPanel.removeComponent(fleetInfoPanel);
                fleetInfoPanel = null;
            }
            if (shipsPanel != null) {
                fenaglePanele.parent.removeComponent(shipsPanel);
                shipsPanel = null;
            }
            if (fleet == null) return;

            shipsPanel = Global.getSettings().createCustom(1, 1, null);

            TooltipMakerAPI fleetInfoPanelHolder = shipsPanel.createUIElement(SHIP_COLUMN_WIDTH, PANEL_HEIGHT, false);
            fleetInfoPanel = new FleetIconPanel(selectedPresetName, fleet, whichMembersAvailable).getPanel();

            // fleetInfoPanel = UtilReflection.createObfFleetIconPanel(selectedPresetName, fleet); // Object casted to UIPanelAPI, fixed size 400x400 afaik
            
            // if (whichMembersAvailable != null) {
            //     UtilReflection.setButtonTooltips(selectedPresetName, fleetInfoPanel, whichMembersAvailable, fleet.getFleetData().getMembersListCopy());
            // } else {
            //     UtilReflection.setButtonTooltips(selectedPresetName, fleetInfoPanel, fleet.getFleetData().getMembersListCopy());
            // }

            fleetInfoPanelHolder.addComponent(fleetInfoPanel).inTL(0f, 0f);
            shipsPanel.addUIElement(fleetInfoPanelHolder).inTL(0f, 0f);
            
            // have to do this because if directly added to the refreshing panel then the game crashes when the confirm dialog window is closed
            fenaglePanele.parent.addComponent(shipsPanel).rightOfTop(fenaglePanele.panel, 0f)
            .setXAlignOffset(-10f)
            .setYAlignOffset(-1f * UIConfig.SHIPLIST_Y_OFFSET_MULTIPLIER);
        }

        @Override
        public void advancePostCreation(float amount) {
        }
    
        @Override
        public void processInput(List<InputEventAPI> events) {
        }
    
        @Override
        public void buttonPressed(Object buttonId) {
        }
    }

    public class TableRowListener { // implements CustomUIPanelPlugin  {
        public Object row;
        public String rowName;
        public int id;
        public CustomPanelAPI panel;
        public PositionAPI rowPos;
        public TooltipMakerAPI tooltipMaker;
        public ButtonAPI button;

        public TableRowListener(Object row, ButtonAPI rowButton, PositionAPI rowPos, String rowPresetName, int id) {
            this.row = row;
            this.id = id;
            this.rowName = rowPresetName;
            this.rowPos = rowPos;
            this.button = rowButton;
        }
    
        // public void init(CustomPanelAPI panel, TooltipMakerAPI tooltipMaker, PositionAPI rowPos) {
        //     this.panel = panel;
        //     this.rowPos = rowPos;
        // }
    
        // @Override
        // public void buttonPressed(Object arg0) {}
    
        // @Override
        // public void positionChanged(PositionAPI arg0) {}

        // @Override
        // public void advance(float arg0) {}

        // @Override
        // public void processInput(List<InputEventAPI> arg0) {
            // for (InputEventAPI event : arg0) {
                // if (!isActive) continue;
                // if (event.isMouseMoveEvent()) {
                //     int eventX = event.getX();
                //     int eventY = event.getY();
                //     float rX = rowPos.getX();
                //     float rY = rowPos.getY();
                //     float rW = rowPos.getWidth();
                //     float rH = rowPos.getHeight();

                //     if (eventX >= rX &&
                //     eventX <= rX + rW &&
                //     eventY >= rY &&
                //     eventY <= rY + rH) {
                        // break;
                //     }
                // }

                //  if (event.isLMBDownEvent()) {
                //     int eventX = event.getX();
                //     int eventY = event.getY();
                //     float rX = rowPos.getX();
                //     float rY = rowPos.getY();
                //     float rW = rowPos.getWidth();
                //     float rH = rowPos.getHeight();
                    
                    // For table headers to sort the table by ascending or descending.
                    // I couldnt get mouse events to register on the header itself, idk what is blocking them. Above and below worked fine lol. So used offsets instead
                    // if (tableUp && this.id == 0 || (!tableUp && this.id == tableRowListeners.size() - 1)) {
                    //     float yOffsetBottom = rY / 100 * 4;
                    //     float yOffsetTop = rY / 100 * 2.6f;

                        // logger.info(String.valueOf(rX));
                        // logger.info(String.valueOf(rY));
                        // if (eventX >= rX + 4f &&
                        //     eventX <= rX + NAME_COLUMN_WIDTH - 5f &&
                        //     eventY >= rY + yOffsetBottom &&
                        //     eventY <= rY + rH + yOffsetTop ) {
                            
                        //     if (tableUp) {
                        //         tablePresetNamesColumnHeader = "Presets <Descending>";
                        //         tableUp = false;
                        //     } else {
                        //         tablePresetNamesColumnHeader = "Presets <Ascending>";
                        //         tableUp = true;
                        //     }
                        //     tablePlugin.rebuild();
                        //     // event.consume();
                        //     break;
                        // }
                        // } else if (eventX >= rX + 5f + NAME_COLUMN_WIDTH
                        //     && eventX <= rX + rW + 5f
                        //     && eventY >= rY + yOffsetBottom
                        //     && eventY <= rY + rH + yOffsetTop ) {
                        //     if (tableRight) {
                        //         tableShipsColumnHeader = "Ships <";
                        //         tableRight = false;
                        //     } else {
                        //         tableShipsColumnHeader = "Ships >";
                        //         tableRight = true;
                        //     }
                        //     tablePlugin.rebuild();
                        //     // event.consume();
                        //     break;
                        // }
                    // }

            //         if (eventX >= rX &&
            //         eventX <= rX + rW &&
            //         eventY >= rY &&
            //         eventY <= rY + rH) {
            //             selectPreset(rowName, id);
            //             enableButtonsRequiringSelection();
            //             tablePlugin.rebuild();
            //             // event.consume();
            //             break;
            //         }
                    
            //     }
            // }
        // }
    
        // @Override
        // public void render(float arg0) {}
    
        // @Override
        // public void renderBelow(float arg0) {}
    }

    public void setParas() {
        if (PresetUtils.isPresetAvailableAtCurrentMarket(dockingListener.getPlayerCurrentMarket(), selectedPresetName, 
        Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy())) {

            isSelectedPresetAvailablePara.setText(String.format(isSelectedPresetAvailableParaFormat, "available"));
            isSelectedPresetAvailablePara.setColor(Misc.getPositiveHighlightColor());

            if (PresetUtils.isPresetPlayerFleet(selectedPreset)) {
                theButtons.get(RESTORE_BUTTON_ID).setEnabled(false);
                isSelectedPresetAvailablePara.setText(String.format("Selected Preset is the current fleet"));
                isSelectedPresetAvailablePara.setColor(Misc.getPositiveHighlightColor());
                Global.getSector().getMemoryWithoutUpdate().set(PresetUtils.UNDOCKED_PRESET_KEY, selectedPreset);
            } else {
                Global.getSector().getMemoryWithoutUpdate().unset(PresetUtils.UNDOCKED_PRESET_KEY);
            }

        } else {

            if (PresetUtils.isPresetPlayerFleet(selectedPreset)) {
                isSelectedPresetAvailablePara.setText(String.format("Selected Preset is the current fleet"));
                isSelectedPresetAvailablePara.setColor(Misc.getPositiveHighlightColor());

            } else if (PresetUtils.isPresetPlayerFleetOfficerAgnostic(selectedPreset)) {
                isSelectedPresetAvailablePara.setText(String.format("Selected Preset is the current fleet but the ship order or officer assignments are different."));
                isSelectedPresetAvailablePara.setColor(TEXT_HIGHLIGHT_COLOR);

            } else if (PresetUtils.isPresetContainedInPlayerFleet(selectedPreset)) {
                isSelectedPresetAvailablePara.setText(String.format("Selected Preset is a part of the current fleet"));
                isSelectedPresetAvailablePara.setColor(TEXT_HIGHLIGHT_COLOR);

            } else {
                isSelectedPresetAvailablePara.setText(String.format(isSelectedPresetAvailableParaFormat, "only partially available, or is unavailable"));
                isSelectedPresetAvailablePara.setColor(Misc.getNegativeHighlightColor());
                if (selectedPreset != null) whichMembersAvailable = PresetUtils.whichMembersAvailable(dockingListener.getPlayerCurrentMarket(), selectedPreset.getCampaignFleet().getFleetData().getMembersListCopy());
            }
        }
    }

    private void refreshTableMap() {
        currentTableMap = PresetUtils.getFleetPresetsMapForTable(tableUp, tableRight);
        currentPresetsNum = currentTableMap.size();
    }

    private int getTableMapIndex(String presetName) {
        List<String> keys = new ArrayList<>(currentTableMap.keySet());
        if (!tableUp) Collections.reverse(keys);
        for (int i = 0; i < keys.size(); i++) {
            if (keys.get(i).equals(presetName)) return i;
        }
        return -1;
    }

    private class SaveListener extends DialogDismissedListener {
        @Override
        public void trigger(Object... args) {
            int option = (int) args[1];

            if (option == 0) {
                // confirm
                String text = saveNameField.getText();
                if (!isEmptyOrWhitespace(text)) {
                    selectedPresetName = text;
                    PresetUtils.saveFleetPreset(selectedPresetName);
                    refreshTableMap();
                    selectedRowIndex = getTableMapIndex(selectedPresetName);

                    if (Global.getSector().getMemoryWithoutUpdate().get(PresetUtils.PLAYERCURRENTMARKET_KEY) == null) {
                        Global.getSector().getMemoryWithoutUpdate().set(PresetUtils.UNDOCKED_PRESET_KEY, PresetUtils.getFleetPresets().get(selectedPresetName));
                    }

                    enableButtonsRequiringSelection();
                    tablePlugin.rebuild();
                    return;
                }

            } else {
                // cancel
                if (!selectedPresetName.equals(EMPTY_STRING)) enableButtonsRequiringSelection();
                return;
            }
        }
    }

    public class DeleteListener extends DialogDismissedListener {
        @Override
        public void trigger(Object... args) {
            int option = (int) args[1];
    
            if (option == 0) {
                // confirm
                PresetUtils.deleteFleetPreset(selectedPresetName);
                
                if (currentPresetsNum == 1) {
                    disableButtonsRequiringSelection();
                    selectPreset(EMPTY_STRING, -1);
                    tablePlugin.rebuild();
                    return;
                }
                
                if (tableUp) {
                    tableRowListeners.remove(selectedRowIndex);
                    selectedRowIndex--;
                    if (selectedRowIndex >= 0) {
                        selectPreset(tableRowListeners.get(selectedRowIndex).rowName, selectedRowIndex);
                    } else if (!tableRowListeners.isEmpty()) {
                        selectPreset(tableRowListeners.get(0).rowName, 0);
                    }
                    tablePlugin.rebuild(); 
                    return;
                } else {
                    int actualIndex = tableRowListeners.size() - selectedRowIndex - 1;
                    tableRowListeners.remove(actualIndex);
                    
                    if (tableRowListeners.isEmpty()) {
                        selectPreset(EMPTY_STRING, -1);
                    } else {
                        selectPreset(tableRowListeners.get(tableRowListeners.size() - selectedRowIndex - 1).rowName,
                        tableRowListeners.size() - selectedRowIndex - 1);
                        enableButtonsRequiringSelection();
                    }
                    tablePlugin.rebuild();
                    return;
                }
            } else if (option == 1) {
                // cancel
                return;
            }
        }
    }

    public class FenaglePanele  {
        public UIPanelAPI parent;
        public CustomPanelAPI panel;
        public FenaglePanele(UIPanelAPI parent, CustomPanelAPI panel) {
            this.panel = panel;
            this.parent = parent;
        }
    }

    public static TooltipMakerAPI.TooltipCreator tc(final String text) {
        final LabelAPI l1 = Global.getSettings().createLabel(text, Fonts.ORBITRON_12);
        //create simple tooltip with text inside it
        return new TooltipMakerAPI.TooltipCreator() {
            @Override
            public boolean isTooltipExpandable(Object tooltipParam) {
                return false;
            }

            @Override
            public float getTooltipWidth(Object tooltipParam) {
                return l1.getPosition().getWidth();
            }

            @Override
            public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                tooltip.addCustom((UIComponentAPI) l1, 0);
            }
        };
    }

    public static TooltipMakerAPI.TooltipCreator getPtsLabelTt(final Map<String, String> breakdown, final String font) {
        final float width = 300f;
        final float height = 32f + 32f * breakdown.size();
        
        final CustomPanelAPI l1 = Global.getSettings().createCustom(width, height, null);
        TooltipMakerAPI tt = l1.createUIElement(width, height, false);

        LabelAPI header = tt.addPara("Total Deployment Pts (Non Civilian)", 0f);
        header.setAlignment(Alignment.MID);

        UIPanelAPI tablePanel = tt.beginTable(c1, c2, c1, 32f, true, false, new Object[]{"", width / 2 - 2.5f, "", width / 2 - 2.5f});

        for (Map.Entry<String, String> entry : breakdown.entrySet()) {
            LabelAPI shipLabbel = Global.getSettings().createLabel(entry.getKey(), Fonts.ORBITRON_12);
            shipLabbel.setColor(Misc.getBrightPlayerColor());

            LabelAPI ptsLabbel = Global.getSettings().createLabel(entry.getValue(), Fonts.ORBITRON_16);
            ptsLabbel.setColor(new Color(0, 177, 211));

            tt.addRow(Alignment.MID, Misc.getBrightPlayerColor(), shipLabbel, Alignment.MID, new Color(0, 177, 211), ptsLabbel);
        }

        tt.addTable("", 0, 0f);

        tablePanel.getPosition().belowMid((UIComponentAPI)header, 10f);

        l1.addUIElement(tt);
        return new TooltipMakerAPI.TooltipCreator() {
            @Override
            public boolean isTooltipExpandable(Object tooltipParam) {
                return false;
            }

            @Override
            public float getTooltipWidth(Object tooltipParam) {
                return l1.getPosition().getWidth();
            }

            @Override
            public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                tooltip.addCustom((UIComponentAPI) l1, 0);
            }
        };
    }

    private static void addVerticalSeedString(UIPanelAPI panel, String sectorSeedString) {
        String[] sectorSeedStringArr = sectorSeedString.split("");
        
        float xOffset = panel.getPosition().getWidth() - 8f;
        float yOffset = 0f;

        for (int i = 0; i < sectorSeedStringArr.length; i++) {
            LabelAPI sectorSeedLabel = Global.getSettings().createLabel(sectorSeedStringArr[i], Fonts.VICTOR_10);
            sectorSeedLabel.setColor(Misc.getGrayColor());
            panel.addComponent((UIComponentAPI)sectorSeedLabel).inTL(xOffset, yOffset);

            yOffset += sectorSeedLabel.computeTextHeight(sectorSeedStringArr[i] + 1f);
        }
    }

    private void showFleetMemberRecoveryDialog() {
        if (whichMembersAvailable == null) whichMembersAvailable = PresetUtils.whichMembersAvailable(dockingListener.getPlayerCurrentMarket(), selectedPreset.getCampaignFleet().getFleetData().getMembersListCopy());

        CampaignFleetAPI fleet = mangledFleet == null ? selectedPreset.getCampaignFleet() : mangledFleet;
        new PartialRestorationDialog(whichMembersAvailable, selectedPreset,  fleet, this);
    }

    public boolean isPartialSelecting() {
        return this.isPartialSelecting;
    }

    public void setPartialSelecting(boolean isPartialSelecting) {
        this.isPartialSelecting = isPartialSelecting;
    }

    public DockingListener getDockingListener() {
        return this.dockingListener;
    }

    public CampaignFleetAPI getMangledFleet() {
        return this.mangledFleet;
    }

    public TablePlugin getTablePlugin() {
        return this.tablePlugin;
    }

    private class MessageBox {
        private ConfirmDialogData dialogData;
    
        public MessageBox(String message, DialogDismissedListener listener) {
            if (listener == null) listener = new DialogDismissedListener() {public void trigger(Object... args){}};

            LabelAPI labbel = Global.getSettings().createLabel(message, Fonts.ORBITRON_16);
            labbel.setAlignment(Alignment.MID);
            labbel.setColor(Misc.getBasePlayerColor());
            labbel.setHighlightColor(Misc.getBrightPlayerColor());
            float width = labbel.computeTextWidth(message);
            float height = labbel.computeTextHeight(message);
    
            dialogData = UtilReflection.showConfirmationDialog("graphics/icons/industry/battlestation.png",
            "",
            "",
            "Ok",
            250f,
            100f,
            listener
            );
    
            dialogData.panel.removeComponent((UIComponentAPI)dialogData.confirmButton.getInstance());
            dialogData.panel.removeComponent((UIComponentAPI)dialogData.textLabel);
            PositionAPI buttonPos = dialogData.cancelButton.getInstance().getPosition();
    
            CustomPanelAPI labbelPanel = Global.getSettings().createCustom(width, height, null);
            TooltipMakerAPI tt = labbelPanel.createUIElement(width, height, false);
            tt.setParaFont(Fonts.ORBITRON_16);
            LabelAPI messageText = tt.addPara(message, 0f);
    
            messageText.setColor(Misc.getBasePlayerColor());
            messageText.setHighlightColor(Misc.getBrightPlayerColor());
            messageText.setHighlightOnMouseover(true);
            messageText.setAlignment(Alignment.MID);
            labbelPanel.addUIElement(tt);
    
            dialogData.panel.addComponent(labbelPanel).inMid().setYAlignOffset(0f);
    
            width = buttonPos.getWidth() / 2;
            height = buttonPos.getHeight();
            buttonPos.setSize(width, height);
        }
    }
}