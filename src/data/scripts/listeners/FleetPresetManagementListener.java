package data.scripts.listeners;

import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
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

import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.fleet.CampaignFleet;

import data.scripts.ClassRefs;
import data.scripts.listeners.DockingListener;

import data.scripts.ui.BaseSelfRefreshingPanel;
import data.scripts.ui.UIComponent;
import data.scripts.ui.UIPanel;
import data.scripts.ui.UiConfig;

import data.scripts.util.ReflectionUtilis;
import data.scripts.util.ReflectionUtilis.ListenerFactory.DialogDismissedListener;
import data.scripts.util.ReflectionUtilis.ListenerFactory.ActionListener;
import data.scripts.util.UtilReflection;
import data.scripts.util.PresetUtils;
import data.scripts.util.PresetUtils.FleetMemberWrapper;
import data.scripts.util.PresetUtils.FleetPreset;
import data.scripts.util.PresetMiscUtils;

import java.awt.Color;
import java.awt.Desktop.Action;
import java.util.*;

import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;

public class FleetPresetManagementListener extends ActionListener {
    private static void print(Object... args) {
        PresetMiscUtils.print(args);
    }

    private static final float CONFIRM_DIALOG_WIDTH = UiConfig.DISPLAY_WIDTH / UiConfig.CONFIRM_DIALOG_WIDTH_DIVISOR;
    private static final float CONFIRM_DIALOG_HEIGHT = UiConfig.DISPLAY_HEIGHT / UiConfig.CONFIRM_DIALOG_HEIGHT_DIVISOR;

    private static final float PANEL_WIDTH = UiConfig.DISPLAY_WIDTH / UiConfig.CONFIRM_DIALOG_WIDTH_DIVISOR - UiConfig.PANEL_WIDTH_SUBTRACTOR;
    private static final float PANEL_HEIGHT = UiConfig.DISPLAY_HEIGHT / UiConfig.CONFIRM_DIALOG_HEIGHT_DIVISOR - UiConfig.PANEL_HEIGHT_SUBTRACTOR;
    
    private static final float NAME_COLUMN_WIDTH = PANEL_WIDTH / UiConfig.NAME_COLUMN_WIDTH_DIVISOR;
    private static final float SHIP_COLUMN_WIDTH = PANEL_WIDTH / UiConfig.SHIP_COLUMN_WIDTH_DIVISOR;

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
    
    private static final String SAVE_DIALOG_BUTTON_ID = "saveDialogButton";
    private static final String SAVE_DIALOG_BUTTON_TOOLTIP_PARA_TEXT = "Saves the current fleet as preset.";
    private static final String SAVE_DIALOG_BUTTON_TEXT = "SAVE FLEET";

    private static final String RESTORE_BUTTON_ID = "restoreButton"; 
    private static final String RESTORE_BUTTON_TOOLTIP_PARA_TEXT = "Restores the selected preset.";
    private static final String RESTORE_BUTTON_TEXT  = "RESTORE";

    private static final String STORE_BUTTON_ID = "storeButton";
    private static final String STORE_BUTTON_TOOLTIP_PARA_TEXT = "Stores the current fleet in storage.";
    private static final String STORE_BUTTON_TEXT  = "STORE FLEET";

    private static final String DELETE_BUTTON_ID = "deleteButton";
    private static final String DELETE_BUTTON_TOOLTIP_PARA_TEXT = "Deletes the selected preset.";
    private static final String DELETE_BUTTON_TEXT = "DELETE";

    private static final String OVERWRITE_PRESET_BUTTON_ID = "overwriteToPresetButton";
    private static final String OVERWRITE_PRESET_BUTTON_TOOLTIP_PARA_TEXT = "Overwrites the selected preset with the current fleet.";
    private static final String OVERWRITE_PRESET_BUTTON_TEXT = "UPDATE";

    private static final String AUTO_UPDATE_BUTTON_ID = "autoUpdateButton";
    private static final String AUTO_UPDATE_BUTTON_TOOLTIP_PARA_TEXT = "Toggle to automatically update the preset when the fleet changes, if undocked with a preset fleet.";
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
    }

    private void resetTopLevelVars() {
        this.saveNameField = null;

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
        this.tablePlugin = new TablePlugin();
        this.tableCanvasPos = null;
        this.currentTableMap = null;
        this.tableUp = true;
        this.tableRight = false;
        this.tablePresetNamesColumnHeader = "Presets <Ascending>";

        if (this.mangledFleet != null) this.mangledFleet.despawn();
        this.mangledFleet = null;
        // this.tableShipsColumnHeader = "Ships <Descending>";
    }

    private TextFieldAPI saveNameField = null;

    private String selectedPresetName = EMPTY_STRING;
    
    private LabelAPI selectedPresetNamePara = null;
    private String selectedPresetNameParaFormat = "Selected Preset: %s";

    private LabelAPI isSelectedPresetAvailablePara = null;
    private String isSelectedPresetAvailableParaFormat = "Selected Preset is %s at this location";

    private LabelAPI isSelectedPresetFleetPara = null;

    private int selectedRowIndex = -1;
    private int currentPresetsNum = 0;

    private PositionAPI overlordPanelPos = null;
    private UIPanelAPI overlordPanel = null;
    private CustomPanelAPI buttonsPanel = null;
    private Map<String, ButtonAPI> theButtons = new HashMap<>();
    private ButtonAPI masterCancelButton = null;
    private FenaglePanele fenaglePanele = null;

    private Object tablePanel = null;
    private List<TableRowListener> tableRowListeners = new ArrayList<>();
    private TablePlugin tablePlugin = new TablePlugin();
    private PositionAPI tableCanvasPos = null;
    private LinkedHashMap<String, PresetUtils.FleetPreset> currentTableMap = null;
    private boolean tableUp = true;
    private boolean tableRight = false;
    private String tablePresetNamesColumnHeader = "Presets <Ascending>";

    private CampaignFleetAPI mangledFleet = null;
    // private String tableShipsColumnHeader = "Ships <Descending>";

    public FleetPresetManagementListener() {
        super();
    }

    @Override
    public void trigger(Object arg0, Object arg1) {
        CustomPanelAPI tableMasterPanel = Global.getSettings().createCustom(PANEL_WIDTH - CANCEL_CONFIRM_BUTTON_WIDTH - 5f, PANEL_HEIGHT, new BaseCustomUIPanelPlugin() );
        UtilReflection.ConfirmDialogData master = UtilReflection.showConfirmationDialog(
            EMPTY_STRING,
            EMPTY_STRING,
            CLOSE_TEXT,
            CONFIRM_DIALOG_WIDTH,
            CONFIRM_DIALOG_HEIGHT,
            new DialogDismissedListener() {
                @Override
                public void trigger(Object arg0, Object arg1) {
                    resetTopLevelVars();
                }
            });
        if (master == null) {
            return;
        }

        ButtonAPI confirmButton = master.confirmButton.getInstance();
        PositionAPI confirmButtonPosition = confirmButton.getPosition();
        ButtonAPI cancelButton = master.cancelButton.getInstance();
        PositionAPI cancelButtonPosition = cancelButton.getPosition();
        CANCEL_CONFIRM_BUTTON_WIDTH = cancelButtonPosition.getWidth();
        cancelButton.setShortcut(Keyboard.KEY_G, false);

        ButtonPlugin buttonPlugin = new ButtonPlugin();
        buttonsPanel = Global.getSettings().createCustom(CANCEL_CONFIRM_BUTTON_WIDTH, PANEL_HEIGHT, buttonPlugin);
        TooltipMakerAPI tooltipMaker = buttonsPanel.createUIElement(CANCEL_CONFIRM_BUTTON_WIDTH+2f, PANEL_HEIGHT, false);
        buttonPlugin.init(buttonsPanel, tooltipMaker);

        addTheButtons(tooltipMaker, confirmButtonPosition, cancelButtonPosition);

        String storageAvailableText;
        Color storageAvailableColor;
        if (DockingListener.getPlayerCurrentMarket() != null && DockingListener.canPlayerAccessStorage(DockingListener.getPlayerCurrentMarket())) {
            storageAvailableText = "Storage Available";
            storageAvailableColor = Misc.getPositiveHighlightColor();
        } else {
            storageAvailableText = "Storage Unavailable";
            storageAvailableColor = Misc.getNegativeHighlightColor();
        }
        tooltipMaker.addPara(storageAvailableText, storageAvailableColor, 5f);

        isSelectedPresetAvailablePara = tooltipMaker.addParaWithMarkup("", c1, 5f);
        isSelectedPresetFleetPara = tooltipMaker.addParaWithMarkup("", c1, 5f);

        master.panel.removeComponent(confirmButton);
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
        overlordPanel = master.panel;
        overlordPanelPos = master.panel.getPosition();
    }
    
    private void addTheButtons(TooltipMakerAPI tooltipMaker, PositionAPI confirmPosition, PositionAPI cancelPosition) {
        float buttonWidth = confirmPosition.getWidth();
        float buttonHeight = cancelPosition.getHeight();
        
        ButtonAPI saveDialogButton = tooltipMaker.addButton(SAVE_DIALOG_BUTTON_TEXT, SAVE_DIALOG_BUTTON_ID, c1, c2,
        Alignment.BR, CutStyle.ALL, buttonWidth, buttonHeight, 5f);
        saveDialogButton.setShortcut(Keyboard.KEY_1, false);
        tooltipMaker.addTooltipTo(tc(SAVE_DIALOG_BUTTON_TOOLTIP_PARA_TEXT), saveDialogButton, TooltipLocation.RIGHT, false);

        ButtonAPI restorePresetButton = tooltipMaker.addButton(RESTORE_BUTTON_TEXT, RESTORE_BUTTON_ID, c1, c2,
        Alignment.BR, CutStyle.ALL, buttonWidth, buttonHeight, 5f);
        restorePresetButton.setShortcut(Keyboard.KEY_2, false);
        tooltipMaker.addTooltipTo(tc(RESTORE_BUTTON_TOOLTIP_PARA_TEXT), restorePresetButton, TooltipLocation.RIGHT, false);

        ButtonAPI storeAllButton = tooltipMaker.addButton(STORE_BUTTON_TEXT, STORE_BUTTON_ID, c1, c2,
        Alignment.BR, CutStyle.ALL, buttonWidth, buttonHeight, 5f);
        storeAllButton.setShortcut(Keyboard.KEY_3, false);
        tooltipMaker.addTooltipTo(tc(STORE_BUTTON_TOOLTIP_PARA_TEXT), storeAllButton, TooltipLocation.RIGHT, false);

        ButtonAPI deleteButton = tooltipMaker.addButton(DELETE_BUTTON_TEXT, DELETE_BUTTON_ID, c1, c2,
        Alignment.BR, CutStyle.ALL, buttonWidth, buttonHeight, 5f);
        deleteButton.setShortcut(Keyboard.KEY_4, false);
        tooltipMaker.addTooltipTo(tc(DELETE_BUTTON_TOOLTIP_PARA_TEXT), deleteButton, TooltipLocation.RIGHT, false);

        ButtonAPI overwriteToPresetButton = tooltipMaker.addButton(OVERWRITE_PRESET_BUTTON_TEXT, OVERWRITE_PRESET_BUTTON_ID, c1, c2,
        Alignment.BR, CutStyle.ALL, buttonWidth, buttonHeight, 5f);
        overwriteToPresetButton.setShortcut(Keyboard.KEY_5, false);
        tooltipMaker.addTooltipTo(tc(OVERWRITE_PRESET_BUTTON_TOOLTIP_PARA_TEXT), overwriteToPresetButton, TooltipLocation.RIGHT, false);

        ButtonAPI autoUpdateButton = tooltipMaker.addCheckbox(buttonWidth, buttonHeight, AUTO_UPDATE_BUTTON_TEXT, AUTO_UPDATE_BUTTON_ID, Fonts.ORBITRON_12, c1,
        ButtonAPI.UICheckboxSize.SMALL, 5f);
        autoUpdateButton.setChecked((boolean)Global.getSector().getPersistentData().get(PresetUtils.IS_AUTO_UPDATE_KEY));
        tooltipMaker.addTooltipTo(tc(AUTO_UPDATE_BUTTON_TOOLTIP_PARA_TEXT), autoUpdateButton, TooltipLocation.RIGHT, false);

        // ButtonAPI cargoRatiosButton = tooltipMaker.addCheckbox(buttonWidth, buttonHeight, KEEP_CARGO_RATIOS_BUTTON_TEXT, KEEP_CARGO_RATIOS_BUTTON_ID, Fonts.ORBITRON_12, c1,
        // ButtonAPI.UICheckboxSize.SMALL, 5f);
        // cargoRatiosButton.setChecked((boolean)Global.getSector().getPersistentData().get(PresetUtils.KEEPCARGORATIOS_KEY));
        // tooltipMaker.addTooltipTo(tc(KEEP_CARGO_RATIOS_BUTTON_PARA_TEXT), cargoRatiosButton, TooltipLocation.RIGHT, false);

        theButtons.put(SAVE_DIALOG_BUTTON_ID, saveDialogButton);
        theButtons.put(RESTORE_BUTTON_ID, restorePresetButton);
        theButtons.put(STORE_BUTTON_ID, storeAllButton);
        theButtons.put(DELETE_BUTTON_ID, deleteButton);
        theButtons.put(OVERWRITE_PRESET_BUTTON_ID, overwriteToPresetButton);
        theButtons.put(AUTO_UPDATE_BUTTON_ID, autoUpdateButton);
        // theButtons.put(KEEP_CARGO_RATIOS_BUTTON_ID, cargoRatiosButton);
        disableButtonsRequiringSelection();
        enableButtonsRequiringSelection();

        return;
    }

    private void openOverwriteDialog(boolean overwrite) {

        SaveListener saveListener = new SaveListener(true, overwrite);
        CustomPanelAPI textFieldPanel = Global.getSettings().createCustom(CONFIRM_DIALOG_WIDTH / 2 / 6, CONFIRM_DIALOG_HEIGHT / 2 / 18, null);

        UtilReflection.ConfirmDialogData subData = UtilReflection.showConfirmationDialog(
            OVERWRITE_DIALOG_HEADE_PREFIX + selectedPresetName + OVERWRITE_DIALOG_HEADE_SUFFIX,
            CONFIRM_TEXT,
            CANCEL_TEXT,
            CONFIRM_DIALOG_WIDTH / 1.5f,
            CONFIRM_DIALOG_HEIGHT / 4,
            saveListener);

        // PositionAPI subPos = subData.panel.getPosition();
        subData.panel.addComponent(textFieldPanel).inTL(0f, 0f);
        subData.confirmButton.getInstance().setShortcut(Keyboard.KEY_G, false);
    }

    private void openSaveDialog() {

        SaveListener saveListener = new SaveListener(false, true);
        BaseCustomUIPanelPlugin textPanelPlugin = null; // new BaseCustomUIPanelPlugin() {
        //     @Override 
        //     public void processInput(List<InputEventAPI> events) {
        //         for (InputEventAPI event : events) {
        //             if (event.isKeyDownEvent() && (Keyboard.isKeyDown(Keyboard.KEY_RETURN) || Keyboard.isKeyDown(Keyboard.KEY_NUMPADENTER))) {
        //                 // PresetMiscUtils.pressKey(Keyboard.KEY_RETURN); // THIS DOESNT EVEN WORK - THE TEXTFIELD CONSUMES THE EVENT BEFORE IT GETS HERE...
        //             }
        //         }
        //     };
        // };

        CustomPanelAPI textFieldPanel = Global.getSettings().createCustom(CONFIRM_DIALOG_WIDTH / 2 / 6, CONFIRM_DIALOG_HEIGHT / 2 / 12, textPanelPlugin);
        TooltipMakerAPI textFieldTooltipMaker = textFieldPanel.createUIElement(CONFIRM_DIALOG_WIDTH / 2 / 5, CONFIRM_DIALOG_HEIGHT / 2 / 10, false);
        saveNameField = textFieldTooltipMaker.addTextField(CONFIRM_DIALOG_WIDTH/3, CONFIRM_DIALOG_HEIGHT/2/3, "graphics/fonts/orbitron24aabold.fnt", 10f);

        textFieldPanel.addUIElement(textFieldTooltipMaker).inTL(0f, 0f);

        UtilReflection.ConfirmDialogData subData = UtilReflection.showConfirmationDialog(
            SAVE_DIALOG_HEADER,
            SAVE_DIALOG_YES_TEXT,
            CANCEL_TEXT,
            CONFIRM_DIALOG_WIDTH / 1.5f,
            CONFIRM_DIALOG_HEIGHT / 2,
            saveListener);

        float yOffset = 0f;
        float xOffset = 0f;
        int column = 0;
        int row = 0;
        for (String[] pair : PresetUtils.FLEET_TYPES) {
            String fleetType = pair[0];
            String icon = pair[1];

            CustomPanelAPI imgTooltipPanel = Global.getSettings().createCustom(40f, 40f, new BaseCustomUIPanelPlugin() {
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
    
            TooltipMakerAPI imgTooltip = imgTooltipPanel.createUIElement(40f, 40f, false);
            imgTooltip.addImage(icon, 40f, 40f, 0f);
            imgTooltipPanel.addUIElement(imgTooltip);
            ButtonAPI button = imgTooltip.addButton("", "", Global.getSettings().getBasePlayerColor(), Global.getSettings().getBasePlayerColor(), Alignment.MID, CutStyle.NONE, 40f, 40f, 0f);
            button.setOpacity(0.15f);
            button.getPosition().setYAlignOffset(40f);

            xOffset = subData.panel.getPosition().getWidth() - 67f - (column * 45f);
            // yOffset = ((CONFIRM_DIALOG_HEIGHT / 2 / 2 / 2) + 5f) + (row > 0 ? row * 45f : 0f);
            yOffset = 35f + (row > 0 ? row * 45f : 0f);
            
            subData.panel.addComponent(imgTooltipPanel).inTL(xOffset, yOffset);

            column++;
            if (column >= 3) {
                column = 0;
                row++;
            }
        }
        // subData.textLabel.setColor(Global.getSettings().getBrightPlayerColor());

        LabelAPI labbel = Global.getSettings().createLabel("Deployment Pts (Non civilian)", Fonts.ORBITRON_12);
        labbel.setColor(Global.getSettings().getBrightPlayerColor());
        labbel.setAlignment(Alignment.MID);
        // subData.panel.addComponent((UIComponentAPI)labbel).inTL(CONFIRM_DIALOG_WIDTH / 2 / 1.35f, 5f);
        
        LabelAPI ptsLabbel = Global.getSettings().createLabel(String.valueOf(PresetUtils.getDeploymentPointsMinusCivilian()), Fonts.ORBITRON_16);
        ptsLabbel.setColor(Misc.getHighlightColor());
        ptsLabbel.setAlignment(Alignment.MID);

        subData.panel.addComponent((UIComponentAPI)labbel).inTL(10f, subData.panel.getPosition().getHeight() - (ptsLabbel.computeTextHeight(ptsLabbel.getText()) + labbel.computeTextHeight(labbel.getText()) + 30f));
        subData.panel.addComponent((UIComponentAPI)ptsLabbel).belowLeft((UIComponentAPI)labbel, 5f);

        subData.panel.addComponent(textFieldPanel).inTL(0f, 0f).setXAlignOffset(CONFIRM_DIALOG_WIDTH / 2 / 2 / 2 / 2 - 20f).setYAlignOffset(-CONFIRM_DIALOG_HEIGHT / 2 / 2 / 2);
        saveNameField.grabFocus();
    }

    private void openDeleteDialog() {

        DeleteListener deleteListener = new DeleteListener();
        CustomPanelAPI textPanel = Global.getSettings().createCustom(CONFIRM_DIALOG_WIDTH / 2 / 10, CONFIRM_DIALOG_HEIGHT / 2 / 20, null);

        UtilReflection.ConfirmDialogData subData = UtilReflection.showConfirmationDialog(
            DELETE_DIALOG_HEADER_PREFIX + selectedPresetName + QUESTON_MARK,
            CONFIRM_TEXT,
            CANCEL_TEXT,
            CONFIRM_DIALOG_WIDTH / 1.5f,
            CONFIRM_DIALOG_HEIGHT / 4,
            deleteListener);
        subData.confirmButton.setShortcut(Keyboard.KEY_G, false);

        subData.panel.addComponent(textPanel).inTL(0f, 0f);
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
                    enableButtonsRequiringSelection();
                    return;

                case STORE_BUTTON_ID:
                    PresetUtils.storeFleetInStorage();
                    enableButtonsRequiringSelection();
                    return;
                    
                case DELETE_BUTTON_ID:
                    openDeleteDialog();
                    return;
                    
                case OVERWRITE_PRESET_BUTTON_ID:
                    openOverwriteDialog(false);
                    return;

                case AUTO_UPDATE_BUTTON_ID:
                    if (theButtons.get(AUTO_UPDATE_BUTTON_ID).isChecked()) {
                        Global.getSector().getPersistentData().put(PresetUtils.IS_AUTO_UPDATE_KEY, true);
                        if (DockingListener.getPlayerCurrentMarket() == null) {
                            FleetPreset preset = PresetUtils.getPresetOfMembers(Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder());

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

                default:
                    break;
            }
        }

        @Override
        public void processInput(List<InputEventAPI> arg0) {
            for (InputEventAPI event : arg0) {
                if (event.isKeyDownEvent()) {
                    if (Keyboard.isKeyDown(Keyboard.KEY_RETURN) || Keyboard.isKeyDown(Keyboard.KEY_NUMPADENTER)) {
                        event.consume();
                        continue;
                    }
                    if (!tableRowListeners.isEmpty()) {
                        if (selectedPresetName != EMPTY_STRING) {
                            int rowNum = tableRowListeners.size();

                            if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
                                selectedRowIndex += tableUp ? 1 : -1;
                            
                                if (selectedRowIndex < 0) {
                                    selectedRowIndex = 0;
                                } else if (selectedRowIndex >= rowNum) {
                                    selectedRowIndex = rowNum - 1;
                                } else {
                                    selectedPresetName = tableRowListeners.get(selectedRowIndex).rowName;
                                    tablePlugin.rebuild();
                                }
                                enableButtonsRequiringSelection();
                            
                            } else if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
                                selectedRowIndex += tableUp ? -1 : 1;
                            
                                if (selectedRowIndex < 0) {
                                    selectedRowIndex = 0;
                                } else if (selectedRowIndex >= rowNum) {
                                    selectedRowIndex = rowNum - 1;
                                } else {
                                    selectedPresetName = tableRowListeners.get(selectedRowIndex).rowName;
                                    tablePlugin.rebuild();
                                }
                                enableButtonsRequiringSelection();

                            } else if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE) && rowNum > 0 && selectedPresetName != EMPTY_STRING) {
                                disableButtonsRequiringSelection();
                                selectedRowIndex = -1;
                                selectedPresetName = EMPTY_STRING;

                                tablePlugin.rebuild();
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

    private void enableButtonsRequiringSelection() {
        if (selectedPresetName != EMPTY_STRING) {
            if (DockingListener.getPlayerCurrentMarket() != null && DockingListener.canPlayerAccessStorage(DockingListener.getPlayerCurrentMarket())) {
                if (Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder().size() > 1) {
                    theButtons.get(STORE_BUTTON_ID).setEnabled(true);
                } else {
                    theButtons.get(STORE_BUTTON_ID).setEnabled(false);
                }
                
                if (!PresetUtils.isPresetPlayerFleet(selectedPresetName)) {
                    theButtons.get(RESTORE_BUTTON_ID).setEnabled(true);
                } else {
                    theButtons.get(RESTORE_BUTTON_ID).setEnabled(false);
                }
            }
            theButtons.get(OVERWRITE_PRESET_BUTTON_ID).setEnabled(true);
            theButtons.get(DELETE_BUTTON_ID).setEnabled(true);

        } else {
            theButtons.get(RESTORE_BUTTON_ID).setEnabled(false);
            theButtons.get(OVERWRITE_PRESET_BUTTON_ID).setEnabled(false);
            theButtons.get(DELETE_BUTTON_ID).setEnabled(false);

            if (DockingListener.getPlayerCurrentMarket() != null && DockingListener.canPlayerAccessStorage(DockingListener.getPlayerCurrentMarket())
                && Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder().size() > 1) {
                theButtons.get(STORE_BUTTON_ID).setEnabled(true);
            } else {
                theButtons.get(STORE_BUTTON_ID).setEnabled(false);
            }
        }
    }

    private void disableButtonsRequiringSelection() {
        theButtons.get(DELETE_BUTTON_ID).setEnabled(false);
        theButtons.get(RESTORE_BUTTON_ID).setEnabled(false);
        theButtons.get(OVERWRITE_PRESET_BUTTON_ID).setEnabled(false);

        if (DockingListener.getPlayerCurrentMarket() != null && DockingListener.canPlayerAccessStorage(DockingListener.getPlayerCurrentMarket())
            && Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder().size() > 1) {
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

        public TablePlugin() {
        }

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

        private void processRow(UIPanelAPI row, String rowName, int id, PresetUtils.FleetPreset fleetpreset) {
            PositionAPI rowPos = row.getPosition();
            
            TableRowListener rowListener = new TableRowListener(row, rowPos, rowName, id, fleetpreset.getFleetMembers());
            CustomPanelAPI rowOverlayPanel = Global.getSettings().createCustom(NAME_COLUMN_WIDTH, 29f, rowListener);
            TooltipMakerAPI rowOverlayTooltipMaker = rowOverlayPanel.createUIElement(NAME_COLUMN_WIDTH, 29f, false);

            tableTipMaker.addComponent(rowOverlayPanel).inTL(rowPos.getX(), rowPos.getY());
            rowListener.init(rowOverlayPanel, rowOverlayTooltipMaker, rowPos);
            
            tableRowListeners.add(rowListener);
        }

        @Override
        public void buildTooltip(CustomPanelAPI panel) {
            refreshTableMap();
            tableTipMaker = panel.createUIElement(NAME_COLUMN_WIDTH, PANEL_HEIGHT, true);
            
            tablePanel = tableTipMaker.beginTable(c1, c2, Misc.getHighlightedOptionColor(), 30f, false, false, 
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
                PresetUtils.FleetPreset fleetPreset = entry.getValue();

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
                
                processRow(row, rowName, id, fleetPreset);
                // id += tableUp ? 1 : -1;
                id++;
            }
            tableTipMaker.addTable(BLANK_TABLE_TEXT, 0, 5f);

            if (selectedRow != null) {
                ReflectionUtilis.invokeMethodDirectly(ClassRefs.tablePanelsetItemsSelectableMethod, tablePanel, true);
                ReflectionUtilis.invokeMethodDirectly(ClassRefs.tablePanelSelectMethod, tablePanel, selectedRow, null);
            }

            panel.addUIElement(tableTipMaker);

            if (selectedPresetName != EMPTY_STRING) {
                // in case there is a matching member in storage with the same variant but not the exact same member the preset was saved with
                Map<FleetMemberWrapper, FleetMemberAPI> neededMembers = PresetUtils.getIdAgnosticRequiredMembers(DockingListener.getPlayerCurrentMarket(), selectedPresetName);

                if (PresetUtils.isPresetAvailableAtCurrentMarket(DockingListener.getPlayerCurrentMarket(), selectedPresetName, 
                    Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder())) {

                    isSelectedPresetAvailablePara.setText(String.format(isSelectedPresetAvailableParaFormat, "available"));
                    isSelectedPresetAvailablePara.setColor(Misc.getPositiveHighlightColor());

                    if (PresetUtils.isPresetPlayerFleet(selectedPresetName)) {
                        theButtons.get(RESTORE_BUTTON_ID).setEnabled(false);
                        isSelectedPresetAvailablePara.setText(String.format("Selected Preset is the current fleet"));
                        isSelectedPresetAvailablePara.setColor(Misc.getPositiveHighlightColor());
                        Global.getSector().getMemoryWithoutUpdate().set(PresetUtils.UNDOCKED_PRESET_KEY, PresetUtils.getFleetPresets().get(selectedPresetName));
                    } else {
                        Global.getSector().getMemoryWithoutUpdate().unset(PresetUtils.UNDOCKED_PRESET_KEY);
                    }

                } else {
                    // theButtons.get(RESTORE_BUTTON_ID).setEnabled(false);

                    if (PresetUtils.isPresetPlayerFleet(selectedPresetName)) {
                        isSelectedPresetAvailablePara.setText(String.format("Selected Preset is the current fleet"));
                        isSelectedPresetAvailablePara.setColor(Misc.getPositiveHighlightColor());
                    } else if (PresetUtils.isPresetPlayerFleetOfficerAgnostic(selectedPresetName)) {
                        isSelectedPresetAvailablePara.setText(String.format("Selected Preset is the current fleet but the officer assignments are different."));
                        isSelectedPresetAvailablePara.setColor(TEXT_HIGHLIGHT_COLOR);
                    } else {
                        isSelectedPresetAvailablePara.setText(String.format(isSelectedPresetAvailableParaFormat, "only partially available, or unavailable"));
                        isSelectedPresetAvailablePara.setColor(Misc.getNegativeHighlightColor());
                    }
                }
                // selectedPresetNamePara.setText(String.format(selectedPresetNameParaFormat, selectedPresetName));
                // addShipList(currentTableMap.get(selectedPresetName).getFleetMembers());
                if (neededMembers != null) {
                    if (mangledFleet != null) {
                        mangledFleet.despawn();
                        mangledFleet = null;
                    }
                    mangledFleet = PresetUtils.mangleFleet(neededMembers, currentTableMap.get(selectedPresetName).getCampaignFleet());
                    addShipList(mangledFleet);
                } else {
                    addShipList(currentTableMap.get(selectedPresetName).getCampaignFleet());
                    // Global.getSector().getMemoryWithoutUpdate().unset(PresetUtils.EXTRANEOUS_MEMBERS_KEY);
                }

            } else {
                // selectedPresetNamePara.setText(EMPTY_STRING);
                isSelectedPresetAvailablePara.setText(EMPTY_STRING);
                addShipList(null);
            };

            tableTipMaker.getExternalScroller().setYOffset(yScrollOffset);
        }

        // public void addShipList(List<PresetUtils.FleetMemberWrapper> fleetMembers) {
        public void addShipList(CampaignFleetAPI fleet) {
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
            fleetInfoPanel = UtilReflection.getObfFleetInfoPanel(selectedPresetName, fleet); // Object casted to UIPanelAPI, fixed size 400x400 afaik
            fleetInfoPanelHolder.addComponent(fleetInfoPanel).inTL(0f, 0f);
            
            shipsPanel.addUIElement(fleetInfoPanelHolder).inTL(0f, 0f);
            
            // have to do this because if directly added to the refreshing panel then the game crashes when the confirm dialog window is closed
            fenaglePanele.parent.addComponent(shipsPanel).rightOfTop(fenaglePanele.panel, 0f)
            .setXAlignOffset(-8f)
            .setYAlignOffset(-1f * UiConfig.SHIPLIST_Y_OFFSET_MULTIPLIER);
        }

        @Override
        public void advancePostCreation(float amount) {
        }
    
        private String trimName(String name, String prefix) {
            String trimmed = prefix;
    
            if (name.length() > 6) {
                trimmed += name.toUpperCase().substring(0, 5) + "...";
            } else trimmed += name.toUpperCase();
    
            return trimmed;
        }
    
        @Override
        public void processInput(List<InputEventAPI> events) {
        }
    
        @Override
        public void buttonPressed(Object buttonId) {
        }
    }

    public class TableRowListener implements CustomUIPanelPlugin  {
        public Object row;
        public String rowName;
        public int id;
        public CustomPanelAPI panel;
        public PositionAPI rowPos;
        public TooltipMakerAPI tooltipMaker;
        public List<PresetUtils.FleetMemberWrapper> fleetMembers;

        public TableRowListener(Object row, PositionAPI rowPos, String rowPresetName, int id, List<PresetUtils.FleetMemberWrapper> fleetMembers) {
            this.row = row;
            this.id = id;
            this.rowName = rowPresetName;
            this.rowPos = rowPos;
            this.fleetMembers = fleetMembers;
        }
    
        public void init(CustomPanelAPI panel, TooltipMakerAPI tooltipMaker, PositionAPI rowPos) {
            this.panel = panel;
            this.rowPos = rowPos;
        }
    
        @Override
        public void buttonPressed(Object arg0) {}
    
        @Override
        public void positionChanged(PositionAPI arg0) {}

        @Override
        public void advance(float arg0) {}

        private String trimName(String name, String prefix) {
            String trimmed = prefix;

            if (name.length() > 6) {
                trimmed += name.toUpperCase().substring(0, 5) + "...";
            } else trimmed += name.toUpperCase();

            return trimmed;
        }

        @Override
        public void processInput(List<InputEventAPI> arg0) {
            for (InputEventAPI event : arg0) {
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

                 if (event.isLMBDownEvent()) {
                    int eventX = event.getX();
                    int eventY = event.getY();
                    float rX = rowPos.getX();
                    float rY = rowPos.getY();
                    float rW = rowPos.getWidth();
                    float rH = rowPos.getHeight();

                    
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

                    if (eventX >= rX &&
                    eventX <= rX + rW &&
                    eventY >= rY &&
                    eventY <= rY + rH) {
                        selectPreset(rowName, id);
                        enableButtonsRequiringSelection();
                        tablePlugin.rebuild();
                        // event.consume();
                        break;
                    }
                    
                }
            }
        }
    
        @Override
        public void render(float arg0) {
        }
    
        @Override
        public void renderBelow(float arg0) {
        }
    }

    private void refreshTableMap() {
        currentTableMap = PresetUtils.getFleetPresetsMapForTable(tableUp, tableRight);
        currentPresetsNum = currentTableMap.size();
    }

    private int getTableMapIndex(String text) {
        List<String> keys = new ArrayList<>(currentTableMap.keySet());
        if (!tableUp) Collections.reverse(keys);
        for (int i = 0; i < keys.size(); i++) {
            if (keys.get(i).equals(text)) return i;
        }
        return -1;
    }

    private class SaveListener extends DialogDismissedListener {
        private boolean overwrite;
        private boolean cancel;
        public SaveListener(boolean overwrite, boolean cancel) {
            this.overwrite = overwrite;
            this.cancel = cancel;
        }
    
        @Override
        public void trigger(Object arg0, Object arg1) {
            int option = (int) arg1;

            if (option == 0) {
                // confirm
                if (overwrite && !cancel) {
                    PresetUtils.saveFleetPreset(selectedPresetName);
                    currentTableMap = PresetUtils.getFleetPresetsMapForTable(tableUp, tableRight);
                    if (Global.getSector().getMemoryWithoutUpdate().get(PresetUtils.PLAYERCURRENTMARKET_KEY) == null) {
                        Global.getSector().getMemoryWithoutUpdate().set(PresetUtils.UNDOCKED_PRESET_KEY, PresetUtils.getFleetPresets().get(selectedPresetName));
                    }
                } else {
                    // FleetPreset possibleDuplicate = PresetUtils.getPresetOfMembers(Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder());
                    String text = saveNameField.getText();
                    if (!isEmptyOrWhitespace(text)) {
                        if (currentTableMap.containsKey(text)) {
                            selectPreset(text, getTableMapIndex(text));
                            openOverwriteDialog(false);
                        
                        // } else if (possibleDuplicate != null) { // someone can implement this if they want to i cant be bothered refactoring rn
                        //     openOverwriteDialo

                        } else {
                            PresetUtils.saveFleetPreset(text);
                            refreshTableMap();
                            selectPreset(text, getTableMapIndex(text));

                            if (Global.getSector().getMemoryWithoutUpdate().get(PresetUtils.PLAYERCURRENTMARKET_KEY) == null) {
                                Global.getSector().getMemoryWithoutUpdate().set(PresetUtils.UNDOCKED_PRESET_KEY, PresetUtils.getFleetPresets().get(selectedPresetName));
                            }
                            enableButtonsRequiringSelection();
                        }
                    }
                }
                tablePlugin.rebuild();
                return;
            } else if (option == 1) {
                if (overwrite && cancel) {
                    openSaveDialog();
                }
                // cancel
                return;
            }
        }
    }

    public class DeleteListener extends DialogDismissedListener {
        @Override
        public void trigger(Object arg0, Object arg1) {
            int option = (int) arg1;
    
            if (option == 0) {
                // confirm
                PresetUtils.deleteFleetPreset(selectedPresetName);
                
                if (currentPresetsNum == 1) {
                    disableButtonsRequiringSelection();
                    selectedRowIndex = -1;
                    selectedPresetName = EMPTY_STRING;
                    tablePlugin.rebuild();
                    return;
                }
                
                if (tableUp) {
                    tableRowListeners.remove(selectedRowIndex);
                    selectedRowIndex--;
                    if (selectedRowIndex >= 0) {
                        selectedPresetName = tableRowListeners.get(selectedRowIndex).rowName;
                    } else if (!tableRowListeners.isEmpty()) {
                        selectedRowIndex = 0;
                        selectedPresetName = tableRowListeners.get(0).rowName;
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

    public static TooltipMakerAPI.TooltipCreator tc(final String text){
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
}