// THIS UI CODE IS A RAGING DUMPSTER FIRE SPAGHETTI MESS READ AT YOUR OWN RISK
package data.scripts.listeners;

import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;

import com.fs.starfarer.api.impl.campaign.ids.Submarkets;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TextFieldAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.input.InputEventType;

import com.fs.starfarer.api.util.Misc;

// import data.scripts.listeners.SaveFleetPreset;
import data.scripts.listeners.DialogDismissedListener;
import data.scripts.listeners.DummyDialogListener;
import data.scripts.ui.BaseSelfRefreshingPanel;
import data.scripts.ui.UIComponent;
import data.scripts.ui.UIPanel;
import data.scripts.listeners.DockingListener;

import data.scripts.util.RandomStringList;
import data.scripts.util.ReflectionUtilis;
import data.scripts.util.UtilReflection;
import data.scripts.util.PresetUtils;
import data.scripts.util.MiscUtils;
import data.scripts.FleetPresetManagerCoreScript;

import java.awt.Color;


import java.lang.reflect.Method;
import java.util.*;

import javax.swing.text.TableView.TableRow;

import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class FleetPresetManagementListener extends ActionListener {
    public static final Logger logger = Logger.getLogger(FleetPresetManagementListener.class);
    private void print(Object... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i] instanceof String ? (String) args[i] : String.valueOf(args[i]));
            if (i < args.length - 1) sb.append(' ');
        }
        logger.info(sb.toString());
    }

    private static final float DISPLAY_WIDTH = (float)Global.getSettings().getScreenWidth();
    private static final float DISPLAY_HEIGHT = (float)Global.getSettings().getScreenHeight();
    private static final float CONFIRM_DIALOG_WIDTH = DISPLAY_WIDTH / 2;
    private static final float CONFIRM_DIALOG_HEIGHT = DISPLAY_HEIGHT / 2;  
    private static final float PANEL_WIDTH = DISPLAY_WIDTH / 2;
    private static final float PANEL_HEIGHT = DISPLAY_HEIGHT / 3;
    private static final float NAME_COLUMN_WIDTH = PANEL_WIDTH / 4;
    private static final float SHIP_COLUMN_WIDTH = PANEL_WIDTH / 1.8f;
    private static final float ROW_HEIGHT = 50f;
    private static final float FLOAT_ZERO = 0f;
    private static float CANCEL_CONFIRM_BUTTON_WIDTH;

    private static final String EMPTY_STRING = "";
    private static final String QUESTON_MARK = "?";
    private static final String CLOSE_TEXT = "Close";
    private static final String CONFIRM_TEXT = "Confirm";
    private static final String CANCEL_TEXT = "Cancel";

    private static final String SAVE_DIALOG_HEADER = "Enter Preset Name:";
    private static final String SAVE_DIALOG_YES_TEXT = "Save preset";
    private static final String DELETE_DIALOG_HEADER_PREFIX = "Are you sure you want to delete ";
    private static final String OVERWRITE_DIALOG_HEADE_PREFIX = "Are you sure you want to overwrite ";
    
    private static final String SAVE_DIALOG_BUTTON_ID = "saveDialogButton";
    private static final String SAVE_DIALOG_BUTTON_TOOLTIP_PARA_TEXT = "Saves the current fleet as preset.";
    private static final String SAVE_DIALOG_BUTTON_TEXT = "SAVE FLEET";

    private static final String RESTORE_BUTTON_ID = "restoreButton"; 
    private static final String RESTORE_BUTTON_TOOLTIP_PARA_TEXT = "Restores the selected preset.";
    private static final String RESTORE_BUTTON_TEXT  = "RESTORE";

    private static final String STORE_BUTTON_ID = "storeButton";
    private static final String STORE_BUTTON_TOOLTIP_PARA_TEXT = "Store current fleet in storage.";
    private static final String STORE_BUTTON_TEXT  = "STORE FLEET";

    private static final String DELETE_BUTTON_ID = "deleteButton";
    private static final String DELETE_BUTTON_TOOLTIP_PARA_TEXT = "Deletes the selected preset.";
    private static final String DELETE_BUTTON_TEXT = "DELETE";

    private static final String OVERWRITE_PRESET_BUTTON_ID = "overwriteToPresetButton";
    private static final String OVERWRITE_PRESET_BUTTON_TOOLTIP_PARA_TEXT = "Overwrites the current fleet to the selected preset.";
    private static final String OVERWRITE_PRESET_BUTTON_TEXT = "OVERWRITE";
    
    private static final String BLANK_TABLE_TEXT = "Fleet Presets Go Here";
    private static final Color c1 = Global.getSettings().getBasePlayerColor();
    private static final Color c2 = Global.getSettings().getDarkPlayerColor();
    private static final Color TEXT_HIGHLIGHT_COLOR = Misc.getHighlightColor();

    private TextFieldAPI saveNameField;

    private String selectedPresetName = EMPTY_STRING;
    private LabelAPI selectedPresetNamePara;
    private String selectedPresetNameParaFormat = "Selected Preset: %s";
    private int selectedRowIndex = -1;
    private int currentPresetsNum = 0;

    private Map<String, ButtonAPI> theButtons = new HashMap<>();
    private ButtonAPI MasterCancelButton;
    private final HashMap<String, String> buttonToolTipParas = new HashMap<>();

    private List<TableRowListener> tableRowListeners = new ArrayList<>();
    private TablePlugin tablePlugin;
    private PositionAPI tableCanvasPos;
    private LinkedHashMap<String, String> currentTableMap;
    private boolean tableUp = true;
    private boolean tableRight = true;
    private boolean rebuild = false;
    private String tablePresetNamesColumnHeader = "Presets <Ascending>";
    private String tableShipsColumnHeader = "Ships >";

    public FleetPresetManagementListener() {
        super();

        this.selectedPresetName = "";
        this.tablePlugin = new TablePlugin();

        buttonToolTipParas.put(SAVE_DIALOG_BUTTON_ID, SAVE_DIALOG_BUTTON_TOOLTIP_PARA_TEXT);
        buttonToolTipParas.put(RESTORE_BUTTON_ID, RESTORE_BUTTON_TOOLTIP_PARA_TEXT);
        buttonToolTipParas.put(STORE_BUTTON_ID, STORE_BUTTON_TOOLTIP_PARA_TEXT);
        buttonToolTipParas.put(DELETE_BUTTON_ID, DELETE_BUTTON_TOOLTIP_PARA_TEXT);
        buttonToolTipParas.put(OVERWRITE_PRESET_BUTTON_ID, OVERWRITE_PRESET_BUTTON_TOOLTIP_PARA_TEXT);
    }

    @Override
    public void trigger(Object... args) {

        List<String> buttonIds = new ArrayList<>();
        buttonIds.add(SAVE_DIALOG_BUTTON_ID);
        buttonIds.add(RESTORE_BUTTON_ID);

        DialogDismissedListener dummyListener = new DummyDialogListener();
        UtilReflection.ConfirmDialogData master = UtilReflection.showConfirmationDialog(
            EMPTY_STRING,
            EMPTY_STRING,
            CLOSE_TEXT,
            CONFIRM_DIALOG_WIDTH,
            CONFIRM_DIALOG_HEIGHT,
            dummyListener);
        if (master == null) {
            return;
        }

        ButtonAPI confirmButton = master.confirmButton.getInstance();
        PositionAPI confirmButtonPosition = confirmButton.getPosition();
        ButtonAPI cancelButton = master.cancelButton.getInstance();
        PositionAPI cancelButtonPosition = cancelButton.getPosition();
        CANCEL_CONFIRM_BUTTON_WIDTH = cancelButtonPosition.getWidth();

        ButtonPlugin buttonPlugin = new ButtonPlugin(buttonIds);
        CustomPanelAPI buttonsPanel = Global.getSettings().createCustom(CANCEL_CONFIRM_BUTTON_WIDTH, PANEL_HEIGHT, buttonPlugin);
        TooltipMakerAPI tooltipMaker = buttonsPanel.createUIElement(CANCEL_CONFIRM_BUTTON_WIDTH, PANEL_HEIGHT, true);
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
        selectedPresetNamePara = tooltipMaker.addParaWithMarkup(String.format(selectedPresetNameParaFormat, selectedPresetName), c1, 5f);

        master.panel.removeComponent(confirmButton);
        // data.panel.removeComponent(cancelButton);
        this.MasterCancelButton = cancelButton;

        tablePlugin = new TablePlugin();
        CustomPanelAPI canvasPanel = Global.getSettings().createCustom(PANEL_WIDTH - CANCEL_CONFIRM_BUTTON_WIDTH, PANEL_HEIGHT, tablePlugin);
        CustomPanelAPI tableMasterPanel = Global.getSettings().createCustom(PANEL_WIDTH - CANCEL_CONFIRM_BUTTON_WIDTH, PANEL_HEIGHT, new BaseCustomUIPanelPlugin());
        canvasPanel.addComponent(tableMasterPanel).inTL(FLOAT_ZERO, FLOAT_ZERO);

        buttonsPanel.addUIElement(tooltipMaker);
        master.panel.addComponent(buttonsPanel).inTL(FLOAT_ZERO, FLOAT_ZERO);
        master.panel.addComponent(canvasPanel).rightOfTop(buttonsPanel, 10f);

        tablePlugin.setRoot(tableMasterPanel);
    }

    private void addTheButtons(TooltipMakerAPI tooltipMaker, PositionAPI confirmPosition, PositionAPI cancelPosition) {
        float buttonWidth = confirmPosition.getWidth();
        float buttonHeight = cancelPosition.getHeight();
        
        ButtonAPI saveDialogButton = tooltipMaker.addButton(SAVE_DIALOG_BUTTON_TEXT, SAVE_DIALOG_BUTTON_ID, c1, c2,
        Alignment.BR, CutStyle.ALL, buttonWidth, buttonHeight, 5f);
        saveDialogButton.setShortcut(Keyboard.KEY_1, false);

        ButtonAPI restorePresetButton = tooltipMaker.addButton(RESTORE_BUTTON_TEXT, RESTORE_BUTTON_ID, c1, c2,
        Alignment.BR, CutStyle.ALL, buttonWidth, buttonHeight, 5f);
        restorePresetButton.setShortcut(Keyboard.KEY_2, false);

        ButtonAPI storeAllButton = tooltipMaker.addButton(STORE_BUTTON_TEXT, STORE_BUTTON_ID, c1, c2,
        Alignment.BR, CutStyle.ALL, buttonWidth, buttonHeight, 5f);
        storeAllButton.setShortcut(Keyboard.KEY_3, false);

        ButtonAPI deleteButton = tooltipMaker.addButton(DELETE_BUTTON_TEXT, DELETE_BUTTON_ID, c1, c2,
        Alignment.BR, CutStyle.ALL, buttonWidth, buttonHeight, 5f);
        deleteButton.setShortcut(Keyboard.KEY_4, false);

        ButtonAPI overwriteToPresetButton = tooltipMaker.addButton(OVERWRITE_PRESET_BUTTON_TEXT, OVERWRITE_PRESET_BUTTON_ID, c1, c2,
        Alignment.BR, CutStyle.ALL, buttonWidth, buttonHeight, 5f);
        overwriteToPresetButton.setShortcut(Keyboard.KEY_5, false);

        theButtons.put(SAVE_DIALOG_BUTTON_ID, saveDialogButton);
        theButtons.put(RESTORE_BUTTON_ID, restorePresetButton);
        theButtons.put(STORE_BUTTON_ID, storeAllButton);
        theButtons.put(DELETE_BUTTON_ID, deleteButton);
        theButtons.put(OVERWRITE_PRESET_BUTTON_ID, overwriteToPresetButton);
        disableButtonsRequiringSelection();
        enableButtonsRequiringSelection();

        return;
    }

    private UtilReflection.ConfirmDialogData openOverwriteDialog() {

        SaveListener saveListener = new SaveListener(true);
        CustomPanelAPI textFieldPanel = Global.getSettings().createCustom(CONFIRM_DIALOG_WIDTH / 2 / 6, CONFIRM_DIALOG_HEIGHT / 2 / 18, null);

        UtilReflection.ConfirmDialogData subData = UtilReflection.showConfirmationDialog(
            OVERWRITE_DIALOG_HEADE_PREFIX + selectedPresetName + QUESTON_MARK,
            CONFIRM_TEXT,
            CANCEL_TEXT,
            CONFIRM_DIALOG_WIDTH / 2,
            CONFIRM_DIALOG_HEIGHT / 2,
            saveListener);

        // PositionAPI subPos = subData.panel.getPosition();
        subData.panel.addComponent(textFieldPanel).inTL(0f, 0f);

        return subData;
    }

    private UtilReflection.ConfirmDialogData openSaveDialog() {

        SaveListener saveListener = new SaveListener(false);
        BaseCustomUIPanelPlugin textPanelPlugin = new BaseCustomUIPanelPlugin() {
            @Override 
            public void processInput(List<InputEventAPI> events) {
                for (InputEventAPI event : events) {
                    if (event.isKeyDownEvent() && (Keyboard.isKeyDown(Keyboard.KEY_RETURN) || Keyboard.isKeyDown(Keyboard.KEY_NUMPADENTER)) && saveNameField.hasFocus()) {
                        MiscUtils.pressKey(Keyboard.KEY_RETURN);
                    }
                }
            }
        };

        CustomPanelAPI textFieldPanel = Global.getSettings().createCustom(CONFIRM_DIALOG_WIDTH / 2 / 6, CONFIRM_DIALOG_HEIGHT / 2 / 12, textPanelPlugin);
        TooltipMakerAPI textFieldTooltipMaker = textFieldPanel.createUIElement(CONFIRM_DIALOG_WIDTH / 2 / 5, CONFIRM_DIALOG_HEIGHT / 2 / 10, false);
        saveNameField = textFieldTooltipMaker.addTextField(CONFIRM_DIALOG_WIDTH/3, CONFIRM_DIALOG_HEIGHT/2/3, "graphics/fonts/orbitron24aabold.fnt", 10f);
        textFieldPanel.addUIElement(textFieldTooltipMaker).inTL(0f, 0f);

        UtilReflection.ConfirmDialogData subData = UtilReflection.showConfirmationDialog(
            SAVE_DIALOG_HEADER,
            SAVE_DIALOG_YES_TEXT,
            CANCEL_TEXT,
            CONFIRM_DIALOG_WIDTH / 2,
            CONFIRM_DIALOG_HEIGHT / 2,
            saveListener);

        // PositionAPI subPos = subData.panel.getPosition();
        subData.panel.addComponent(textFieldPanel).inTL(0f, 0f);
        saveNameField.grabFocus();

        return subData;
    }

    private UtilReflection.ConfirmDialogData openDeleteDialog() {

        DeleteListener deleteListener = new DeleteListener();
        CustomPanelAPI textPanel = Global.getSettings().createCustom(CONFIRM_DIALOG_WIDTH / 2 / 10, CONFIRM_DIALOG_HEIGHT / 2 / 20, null);

        UtilReflection.ConfirmDialogData subData = UtilReflection.showConfirmationDialog(
            DELETE_DIALOG_HEADER_PREFIX + selectedPresetName + QUESTON_MARK,
            CONFIRM_TEXT,
            CANCEL_TEXT,
            CONFIRM_DIALOG_WIDTH / 2,
            CONFIRM_DIALOG_HEIGHT / 2,
            deleteListener);

        subData.panel.addComponent(textPanel).inTL(0f, 0f);
        return subData;
    }

    public static boolean isEmptyOrWhitespace(String s) {
        return s != null && s.trim().isEmpty();
    }

    private class SaveListener extends DialogDismissedListener {
        private boolean overwrite;

        public SaveListener(boolean overwrite) {
            this.overwrite = overwrite;
        }
    
        @Override
        public void trigger(Object... args) {
            int option = (int) args[1];

            if (option == 0) {
                // confirm
                if (overwrite) {
                    PresetUtils.saveFleetPreset(selectedPresetName);
                    currentTableMap = PresetUtils.getFleetPresetsMapForTable(tableUp, tableRight);
                } else {
                    String text = saveNameField.getText();
                    if (!isEmptyOrWhitespace(text)) {
                        if (currentTableMap.containsKey(text)) {
                            selectedPresetName = text;
                            openOverwriteDialog();
                            selectedRowIndex = getTableMapIndex(text);
                        } else {
                            selectedPresetName = text;
                            PresetUtils.saveFleetPreset(text);
                            refreshTableMap();
                            selectedRowIndex = getTableMapIndex(text);
                            enableButtonsRequiringSelection();
                        }
                    }
                }
                tablePlugin.rebuild();
                return;
            } else if (option == 1) {
                // cancel
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
                        selectedRowIndex = -1;
                        selectedPresetName = EMPTY_STRING;
                    } else {
                        selectedRowIndex = Math.min(selectedRowIndex, tableRowListeners.size() - 1);
                        selectedPresetName = tableRowListeners.get(tableRowListeners.size() - selectedRowIndex - 1).rowName;
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

    private class ButtonPlugin implements CustomUIPanelPlugin {
        CustomPanelAPI masterPanel;
        TooltipMakerAPI masterTooltip;

        HashMap<String, CustomPanelAPI> tooltipMap;
        List<String> buttonIds;

        boolean isTooltip;
        String currentTooltipId;

        public ButtonPlugin(List<String> buttonIds) {
            this.tooltipMap = new HashMap<>();
            this.isTooltip = false;
            this.buttonIds = buttonIds;

            // for (String buttonId : buttonIds) {
            //     CustomPanelAPI tooltipPanel = Global.getSettings().createCustom(250f, 60f, null);
            //     TooltipMakerAPI tooltip = tooltipPanel.createUIElement(250f, 60f, false);
            //     tooltip.addPara(buttonToolTipParas.get(buttonId), 0f);
            //     tooltipPanel.wrapTooltipWithBox(tooltip, Misc.getBasePlayerColor());
            
            //     tooltipPanel.addUIElement(tooltip).inTL(0f, 0f);
            //     tooltipMap.put(buttonId, tooltipPanel);
            // }


        }
        
        public void init(CustomPanelAPI panel, TooltipMakerAPI tooltip) {
            this.masterPanel = panel;
            this.masterTooltip = tooltip;
        }

        // private void showButtonToolTipAtLocation(String buttonId) {
        //     CustomPanelAPI toolTipPanel = tooltipMap.get(buttonId);
        //     toolTipPanel.setOpacity(100f);
        //     float width = 0f;
        //     float height = 0f;

        //     for (Map.Entry<String, ButtonAPI> entry: theButtons.entrySet()) {
        //         ButtonAPI button = entry.getValue();
        //         if (button.getCustomData().equals(buttonId)) {
        //             PositionAPI pos = button.getPosition();
        //             width = pos.getWidth();
        //             height = pos.getHeight();
        //             break;
        //         }
        //     }

        //     this.masterTooltip.addComponent(toolTipPanel).inTL(width + 5f, height - 10f);
        // }

        // private void destroyButtonToolTip(String buttonId) {
        //     CustomPanelAPI toolTipPanel = tooltipMap.get(buttonId);
        //     toolTipPanel.setOpacity(0f);
        //     this.masterTooltip.removeComponent(toolTipPanel);
        // }

        @Override
        public void advance(float amount) {
    
        }
    
        @Override
        public void buttonPressed(Object arg0) {
            if (arg0 instanceof String) {
                String action = (String) arg0;
        
                switch (action) {
                    case SAVE_DIALOG_BUTTON_ID:
                        openSaveDialog();
                        return;
                    case RESTORE_BUTTON_ID:
                        PresetUtils.restoreFleetFromPreset(selectedPresetName);
                        return;
                    case STORE_BUTTON_ID:
                        PresetUtils.storeFleetInStorage(selectedPresetName);
                        return;
                    case DELETE_BUTTON_ID:
                        openDeleteDialog();
                        return;
                    case OVERWRITE_PRESET_BUTTON_ID:
                        openOverwriteDialog();
                        return;
                    default:
                        break;
                }
            }
        }
    
        @Override
        public void positionChanged(PositionAPI arg0) {
    
        }

        @Override
        public void processInput(List<InputEventAPI> arg0) {
            for (InputEventAPI event : arg0) {
            //     if (event.isMouseMoveEvent()) {
            //         int mouseX = event.getX();
            //         int mouseY = event.getY();

            //         ButtonAPI button = getButton(theButtons, mouseX, mouseY);
            //         if (button != null) {
            //             String buttonId = (String) button.getCustomData();
            //             this.isTooltip = true;
            //             this.currentTooltipId = new String(buttonId);
            //             showButtonToolTipAtLocation(buttonId);
            //             return;
            //         }
            //         if (this.isTooltip && this.currentTooltipId != null) {
            //             destroyButtonToolTip(this.currentTooltipId);
            //             this.isTooltip = false;
            //             this.currentTooltipId = null;
            //             return;
            //             // i dont know why we have to do this, probably because concurrency ticks take too long
            //         } else {
            //             for (String buttonId : this.buttonIds) {
            //                 tooltipMap.get(buttonId).setOpacity(0f);
            //             }
            //         }
            //     }
                
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

                            } else if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE) && rowNum > 0 && selectedPresetName != EMPTY_STRING) {
                                disableButtonsRequiringSelection();
                                selectedRowIndex = -1;
                                selectedPresetName = EMPTY_STRING;

                                tablePlugin.rebuild();
                                event.consume();
                            }
                        }
                    }
                }
            }
        }

        private ButtonAPI getButton (HashMap<String, ButtonAPI> buttons, int mouseX, int mouseY) {
            for (Map.Entry<String, ButtonAPI> entry: theButtons.entrySet()) {
                ButtonAPI button = entry.getValue();
                PositionAPI pos = button.getPosition();
                float x = pos.getX();
                float y = pos.getY();
                float width = pos.getWidth();
                float height = pos.getHeight();

                if (mouseX >= x && mouseX <= x + width - 5f &&
                mouseY >= y && mouseY <= y + height - 5f) {
                return button;
                }
            }
            return null;
        }   
    
        @Override
        public void render(float arg0) {
    
        }
    
        @Override
        public void renderBelow(float arg0) {
    
        }
        
    }

    private void enableButtonsRequiringSelection() {
        if (selectedPresetName != EMPTY_STRING) {
            if (DockingListener.getPlayerCurrentMarket() != null && DockingListener.canPlayerAccessStorage(DockingListener.getPlayerCurrentMarket())) {
                theButtons.get(RESTORE_BUTTON_ID).setEnabled(true);
                theButtons.get(STORE_BUTTON_ID).setEnabled(true);
                theButtons.get(OVERWRITE_PRESET_BUTTON_ID).setEnabled(true);
            } else {
                theButtons.get(OVERWRITE_PRESET_BUTTON_ID).setEnabled(true);
            }
            theButtons.get(DELETE_BUTTON_ID).setEnabled(true);
        }
    }

    private void disableButtonsRequiringSelection() {
        theButtons.get(DELETE_BUTTON_ID).setEnabled(false);
        theButtons.get(RESTORE_BUTTON_ID).setEnabled(false);
        theButtons.get(STORE_BUTTON_ID).setEnabled(false);
        theButtons.get(OVERWRITE_PRESET_BUTTON_ID).setEnabled(false);
    }

    public class TablePlugin extends BaseSelfRefreshingPanel {
        public LabelAPI label;
        public boolean rebuild;
        public UIPanelAPI root;
        public CustomPanelAPI panel;
        // public PositionAPI panelPos;
        // public CustomPanelAPI sibling;
        // public CustomPanelAPI canvasPanel;
        private UIPanelAPI tablePanel;

        public TablePlugin() {
        }
    
        public void setRoot(CustomPanelAPI root, CustomPanelAPI panel) {
            this.root = root;
            // this.sibling = sibling;
            this.panel = panel;

            // this.canvasPanel = canvasPanel;
            this.rebuild();
        }

        @Override
        public void positionChanged(PositionAPI position) {
        }
    
        @Override
        public void renderBelow(float alphaMult) {
    
        }
    
        @Override
        public void render(float alphaMult) {
        }

        private void processRow(Object row, String rowName, TooltipMakerAPI tableTipMaker, int id) {
            PositionAPI rowPos = (PositionAPI) ReflectionUtilis.invokeMethod("getPosition", row);
            TableRowListener rowListener = new TableRowListener(rowPos, rowName, tableRowListeners, id);
            CustomPanelAPI rowOverlayPanel = Global.getSettings().createCustom(NAME_COLUMN_WIDTH + SHIP_COLUMN_WIDTH - 10f, 29f, rowListener);
            TooltipMakerAPI rowOverlayTooltipMaker = rowOverlayPanel.createUIElement(NAME_COLUMN_WIDTH + SHIP_COLUMN_WIDTH - 10f, 29f, false);

            tableTipMaker.addComponent(rowOverlayPanel).inTL(rowPos.getX(), rowPos.getY());
            rowListener.init(rowOverlayPanel, rowOverlayTooltipMaker, rowPos);
            
            tableRowListeners.add(rowListener);
        }

        @Override
        public void buildTooltip(CustomPanelAPI panel) {
            refreshTableMap();
            TooltipMakerAPI tableTipMaker = panel.createUIElement(PANEL_WIDTH - CANCEL_CONFIRM_BUTTON_WIDTH, PANEL_HEIGHT, true);
            
            tablePanel = tableTipMaker.beginTable(c1, c2, Misc.getHighlightedOptionColor(), 30f, true, true, 
            new Object[]{tablePresetNamesColumnHeader, NAME_COLUMN_WIDTH, tableShipsColumnHeader, SHIP_COLUMN_WIDTH}
            );
            
            tableRowListeners.clear();
            int id;
            int size = currentTableMap.size();
            
            if (tableUp) {
                id = 0;
            } else {
                id = (size == 1) ? 0 : size - 1;
            }

            for (Map.Entry<String, String> entry: currentTableMap.entrySet()) {
                String rowName = entry.getKey();
                Object row;
                if (selectedRowIndex == id) {
                    row = tableTipMaker.addRowWithGlow(
                        TEXT_HIGHLIGHT_COLOR, 
                        rowName,
                        TEXT_HIGHLIGHT_COLOR,
                        entry.getValue()
                    );
                } else {
                    row = tableTipMaker.addRowWithGlow(
                        c1, 
                        rowName,
                        c1,
                        entry.getValue()
                    );
                }
                
                processRow(row, rowName, tableTipMaker, id);
                id += tableUp ? 1 : -1;
            }
            tableTipMaker.addTable(BLANK_TABLE_TEXT, 0, FLOAT_ZERO);
            panel.addUIElement(tableTipMaker);
        }
    
        @Override
        public void advancePostCreation(float amount) {
            if (selectedPresetName != EMPTY_STRING) {
                selectedPresetNamePara.setText(String.format(selectedPresetNameParaFormat, selectedPresetName));
            }
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
    

        private class TableHeadListener implements CustomUIPanelPlugin {
            private PositionAPI panelPos;

            public void init(PositionAPI panelPos) {
                this.panelPos = panelPos;
            }

            @Override
            public void advance(float arg0) {
            }

            @Override
            public void buttonPressed(Object arg0) {

            }

            @Override
            public void positionChanged(PositionAPI arg0) {
            }

            @Override
            public void processInput(List<InputEventAPI> arg0) {
                // for (InputEventAPI event : arg0) {
                // }
            }

            @Override
            public void render(float arg0) {

            }

            @Override
            public void renderBelow(float arg0) {

            }

        }
    }

    public class TableRowListener implements CustomUIPanelPlugin  {
        public Object row;
        public String rowName;
        public int id;
        public List<TableRowListener> tableRowListeners;
        public CustomPanelAPI panel;
        public PositionAPI rowPos;
        public TooltipMakerAPI tooltipMaker;
        public LabelAPI label;
    
        public TableRowListener(PositionAPI rowPos, String rowPresetName, List<TableRowListener> tableRowListeners, int id) {
            this.tableRowListeners = tableRowListeners;

            this.id = id;
            this.rowName = rowPresetName;
            this.rowPos = rowPos;
        }
    
        public void init(CustomPanelAPI panel, TooltipMakerAPI tooltipMaker, PositionAPI rowPos) {
            this.panel = panel;
            this.rowPos = rowPos;
        }

        public void clearHighlight() {

        }
    
        @Override
        public void buttonPressed(Object arg0) {

        }
    
        @Override
        public void positionChanged(PositionAPI arg0) {

        }

        @Override
        public void advance(float arg0) {

        }

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
                if (event.isLMBDownEvent()) {
                    int eventX = event.getX();
                    int eventY = event.getY();
                    float rX = rowPos.getX();
                    float rY = rowPos.getY();
                    float rW = rowPos.getWidth();
                    float rH = rowPos.getHeight();

                    
                    // For table headers. I couldnt get mouse events to register on the header itself, idk what is blocking them. Above and below worked fine lol
                    if (tableUp && this.id == 0 || (!tableUp && this.id == tableRowListeners.size() - 1)) {
                        float yOffsetBottom = rY / 100 * 4;
                        float yOffsetTop = rY / 100 * 2.6f;

                        // logger.info(String.valueOf(rX));
                        // logger.info(String.valueOf(rY));
                        if (eventX >= rX + 4f &&
                            eventX <= rX + NAME_COLUMN_WIDTH - 5f &&
                            eventY >= rY + yOffsetBottom &&
                            eventY <= rY + rH + yOffsetTop ) {
                            
                            if (tableUp) {
                                tablePresetNamesColumnHeader = "Presets <Descending>";
                                tableUp = false;
                            } else {
                                tablePresetNamesColumnHeader = "Presets <Ascending>";
                                tableUp = true;
                            }
                            tablePlugin.rebuild();
                            // event.consume();
                            break;

                        } else if (eventX >= rX + 5f + NAME_COLUMN_WIDTH
                            && eventX <= rX + rW + 5f
                            && eventY >= rY + yOffsetBottom
                            && eventY <= rY + rH + yOffsetTop ) {
                            if (tableRight) {
                                tableShipsColumnHeader = "Ships <";
                                tableRight = false;
                            } else {
                                tableShipsColumnHeader = "Ships >";
                                tableRight = true;
                            }
                            tablePlugin.rebuild();
                            // event.consume();
                            break;
                        }
                    }

                    if (eventX >= rX &&
                    eventX <= rX + rW &&
                    eventY >= rY &&
                    eventY <= rY + rH) {
                        selectedPresetName = rowName;
                        selectedRowIndex = id;
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
}