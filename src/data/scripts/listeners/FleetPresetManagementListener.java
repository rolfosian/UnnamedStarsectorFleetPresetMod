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
import data.scripts.ui.UIComponent;
import data.scripts.ui.UIPanel;
import data.scripts.listeners.DockingListener;

import data.scripts.util.RandomStringList;
import data.scripts.util.ReflectionUtilis;
import data.scripts.util.UtilReflection;
import data.scripts.util.PresetUtils;
import data.scripts.FleetPresetManagerCoreScript;

import java.awt.Color;
import java.lang.reflect.Method;
import java.util.*;

import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class FleetPresetManagementListener extends ActionListener {
    public static final Logger logger = Logger.getLogger(FleetPresetManagementListener.class);

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

    private static final String[] TABLE_HEADERS = {
        "Preset",
        "Ships",
        "Fleet Presets Go Here"
    };

    private static final Color c1 = Global.getSettings().getBasePlayerColor();
    private static final Color c2 = Global.getSettings().getDarkPlayerColor();
    private static final Color TEXT_HIGHLIGHT_COLOR = Misc.getHighlightColor();

    private TextFieldAPI saveNameField;
    private String selectedPresetName;
    private HashMap<String, ButtonAPI> theButtons;
    private ButtonAPI cancelButton;

    private final HashMap<String, String> buttonToolTipParas = new HashMap<>();
    private TablePlugin tablePlugin;

    public FleetPresetManagementListener() {
        super();

        this.selectedPresetName = "";
        this.tablePlugin = new TablePlugin();

        buttonToolTipParas.put(SAVE_DIALOG_BUTTON_ID, SAVE_DIALOG_BUTTON_TOOLTIP_PARA_TEXT);
        buttonToolTipParas.put(RESTORE_BUTTON_ID, RESTORE_BUTTON_TOOLTIP_PARA_TEXT);
    }

    @Override
    public void trigger(Object... args) {

        List<String> buttonIds = new ArrayList<>();
        buttonIds.add(SAVE_DIALOG_BUTTON_ID);
        buttonIds.add(RESTORE_BUTTON_ID);

        DialogDismissedListener dummyListener = new DummyDialogListener();
        UtilReflection.ConfirmDialogData data = UtilReflection.showConfirmationDialog(
            EMPTY_STRING,
            EMPTY_STRING,
            CLOSE_TEXT,
            CONFIRM_DIALOG_WIDTH,
            CONFIRM_DIALOG_HEIGHT,
            dummyListener);
        if (data == null) {
            return;
        }

        ButtonAPI confirmButton = data.confirmButton.getInstance();
        PositionAPI confirmButtonPosition = confirmButton.getPosition();
        ButtonAPI cancelButton = data.cancelButton.getInstance();
        PositionAPI cancelButtonPosition = cancelButton.getPosition();
        CANCEL_CONFIRM_BUTTON_WIDTH = cancelButtonPosition.getWidth();

        ButtonPlugin buttonPlugin = new ButtonPlugin(buttonIds);
        CustomPanelAPI customPanel = Global.getSettings().createCustom(CANCEL_CONFIRM_BUTTON_WIDTH, PANEL_HEIGHT, buttonPlugin);
        TooltipMakerAPI tooltipMaker = customPanel.createUIElement(CANCEL_CONFIRM_BUTTON_WIDTH, PANEL_HEIGHT, true);
        buttonPlugin.init(customPanel, tooltipMaker);

        theButtons = addTheButtons(tooltipMaker, confirmButtonPosition, cancelButtonPosition);

        data.panel.removeComponent(confirmButton);
        // data.panel.removeComponent(cancelButton);
        this.cancelButton = cancelButton;

        tablePlugin = new TablePlugin();
        CustomPanelAPI canvasPanel = Global.getSettings().createCustom(PANEL_WIDTH - CANCEL_CONFIRM_BUTTON_WIDTH, PANEL_HEIGHT, tablePlugin);

        CustomPanelAPI tableMasterPanel = Global.getSettings().createCustom(PANEL_WIDTH, PANEL_HEIGHT, null);
        canvasPanel.addComponent(tableMasterPanel).inTL(FLOAT_ZERO, FLOAT_ZERO);

        customPanel.addUIElement(tooltipMaker);
        data.panel.addComponent(customPanel).inTL(FLOAT_ZERO, FLOAT_ZERO);
        data.panel.addComponent(canvasPanel).rightOfTop(customPanel, 10f);
        tablePlugin.setRoot(data.panel, tableMasterPanel, customPanel, canvasPanel);
    }

    private HashMap<String, ButtonAPI> addTheButtons(TooltipMakerAPI tooltipMaker, PositionAPI confirmPosition, PositionAPI cancelPosition) {
        HashMap<String, ButtonAPI> buttons = new HashMap<>();
        
        ButtonAPI saveDialogButton = tooltipMaker.addButton(SAVE_DIALOG_BUTTON_TEXT, SAVE_DIALOG_BUTTON_ID, c1, c2,
        Alignment.BR, CutStyle.ALL, confirmPosition.getWidth(), cancelPosition.getHeight(), 5f);
        saveDialogButton.setShortcut(Keyboard.KEY_1, false);

        ButtonAPI restorePresetButton = tooltipMaker.addButton(RESTORE_BUTTON_TEXT, RESTORE_BUTTON_ID, c1, c2,
        Alignment.BR, CutStyle.ALL, confirmPosition.getWidth(), cancelPosition.getHeight(), 5f);
        restorePresetButton.setShortcut(Keyboard.KEY_2, false);

        ButtonAPI storeAllButton = tooltipMaker.addButton(STORE_BUTTON_TEXT, STORE_BUTTON_ID, c1, c2,
        Alignment.BR, CutStyle.ALL, confirmPosition.getWidth(), confirmPosition.getHeight(), 5f);
        storeAllButton.setShortcut(Keyboard.KEY_3, false);

        ButtonAPI deleteButton = tooltipMaker.addButton(DELETE_BUTTON_TEXT, DELETE_BUTTON_ID, c1, c2,
        Alignment.BR, CutStyle.ALL, confirmPosition.getWidth(), confirmPosition.getHeight(), 5f);
        deleteButton.setShortcut(Keyboard.KEY_4, false);

        
        if (!DockingListener.canPlayerAccessStorage()) {
            storeAllButton.setEnabled(false);
        }

        restorePresetButton.setEnabled(false);
        
        buttons.put(SAVE_DIALOG_BUTTON_ID, saveDialogButton);
        buttons.put(RESTORE_BUTTON_ID, restorePresetButton);
        buttons.put(STORE_BUTTON_ID, storeAllButton);
        buttons.put(DELETE_BUTTON_ID, deleteButton);

        return buttons;
    }

    private UtilReflection.ConfirmDialogData openSaveDialog() {

        SaveFleetPreset saveListener = new SaveFleetPreset();
        CustomPanelAPI textFieldPanel = Global.getSettings().createCustom(CONFIRM_DIALOG_WIDTH / 2 / 6, CONFIRM_DIALOG_HEIGHT / 2 / 12, null);
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

        PositionAPI subPos = subData.panel.getPosition(); 
        subData.panel.addComponent(textFieldPanel).inTL(0f, 0f);
        saveNameField.showCursor();

        return subData;
    }

    private UtilReflection.ConfirmDialogData openDeleteDialog() {

        ConfirmFleetPresetsDeletion deleteListener = new ConfirmFleetPresetsDeletion();
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

    private class SaveFleetPreset extends DialogDismissedListener {
        public static final Logger logger = Logger.getLogger(SaveFleetPreset.class);
    
        @Override
        public void trigger(Object... args) {
            int option = (int) args[1];

            if (option == 0) {
                // confirm
                String text = saveNameField.getText();
                if (!isEmptyOrWhitespace(text)) {
                    PresetUtils.saveFleetPreset(text);
                    tablePlugin.rebuild();
                }

                return;
            } else if (option == 1) {
                // cancel
                return;
            }
        }
    }

    public class ConfirmFleetPresetsDeletion extends DialogDismissedListener {
        
            @Override
            public void trigger(Object... args) {
                int option = (int) args[1];
        
                if (option == 0) {
                    // confirm
                    PresetUtils.deleteFleetPreset(selectedPresetName);
                    selectedPresetName = EMPTY_STRING;
                    tablePlugin.rebuild();
                    theButtons.get(DELETE_BUTTON_ID).setEnabled(false);
                    return;
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
                    default:
                        break;
                }
            }
        }
    
        @Override
        public void positionChanged(PositionAPI arg0) {
    
        }

        //this is so dirty i dont like it
        @Override
        public void processInput(List<InputEventAPI> arg0) {
            // for (InputEventAPI event : arg0) {
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
            // }
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

    public class TablePlugin implements CustomUIPanelPlugin {

        public boolean rebuild = false;
        public List<TableRowListener> tableRowListeners;

        public LabelAPI label;

        public UIPanelAPI root;
        public CustomPanelAPI panel;
        public CustomPanelAPI sibling;
        public CustomPanelAPI canvasPanel;
        private Color[] colors = { c1, TEXT_HIGHLIGHT_COLOR };

        private TooltipMakerAPI tableTipMaker;
    
        public TablePlugin() {
            this.tableRowListeners = new ArrayList<>();
        }
    
        public void setRoot(UIPanelAPI root, CustomPanelAPI panel, CustomPanelAPI sibling, CustomPanelAPI canvasPanel) {
            this.root = root;
            this.sibling = sibling;
            this.panel = panel;

            this.canvasPanel = canvasPanel;
            rebuild();
        }

        @Override
        public void positionChanged(PositionAPI position) {
        }
    
        public void rebuild() {
            rebuild = true;
        }
    
        @Override
        public void renderBelow(float alphaMult) {
    
        }
    
        @Override
        public void render(float alphaMult) {
        }

        private void processRow(Object row, String rowName, int id) {
            PositionAPI rowPos = (PositionAPI) ReflectionUtilis.invokeMethod("getPosition", row);
            TableRowListener rowListener = new TableRowListener(rowPos, rowName, tableRowListeners, id);
            CustomPanelAPI rowOverlayPanel = Global.getSettings().createCustom(NAME_COLUMN_WIDTH + SHIP_COLUMN_WIDTH - 10f, 29f, rowListener);
            TooltipMakerAPI rowOverlayTooltipMaker = rowOverlayPanel.createUIElement(NAME_COLUMN_WIDTH + SHIP_COLUMN_WIDTH - 10f, 29f, false);

            tableTipMaker.addComponent(rowOverlayPanel).inTL(rowPos.getX(), rowPos.getY());
            rowListener.init(rowOverlayPanel, rowOverlayTooltipMaker, rowPos);
            
            tableRowListeners.add(rowListener);
        }

        public void buildTooltip(CustomPanelAPI panel) {
            Map<String, String> presets = PresetUtils.getFleetPresetsMapForTable();
            tableTipMaker = panel.createUIElement(PANEL_WIDTH - CANCEL_CONFIRM_BUTTON_WIDTH, PANEL_HEIGHT, true);
            
            tableTipMaker.beginTable(c1, c2, Misc.getHighlightedOptionColor(), 30f, true, true, 
            new Object[]{TABLE_HEADERS[0], NAME_COLUMN_WIDTH, TABLE_HEADERS[1], SHIP_COLUMN_WIDTH}
             );
            
            tableRowListeners.clear();
            int id = 0;

            for (Map.Entry<String, String> entry: presets.entrySet()) {
                String rowName = entry.getKey();
                Object row;

                if (selectedPresetName.toLowerCase().equals(rowName.toLowerCase())) {
                    row = tableTipMaker.addRowWithGlow(
                        colors[1], 
                        rowName,
                        colors[1],
                        entry.getValue()
                    );
                    
                } else {
                    row = tableTipMaker.addRowWithGlow(
                        colors[0], 
                        rowName,
                        colors[0],
                        entry.getValue()
                    );
                }
                
                processRow(row, rowName, id);
                id++;
            }

            tableTipMaker.addTable(TABLE_HEADERS[2], 0, FLOAT_ZERO);

            panel.addUIElement(tableTipMaker);
        }
    
        @Override
        final public void advance(float amount) {
            if (root == null) return;

            if (rebuild) {
                try {
                    canvasPanel.removeComponent(panel);
                    root.removeComponent(canvasPanel);
                    panel = null;
                } catch (Exception ignore) {
                }
                rebuild = false;

                panel = canvasPanel.createCustomPanel(PANEL_WIDTH, PANEL_HEIGHT, new BaseCustomUIPanelPlugin());
                canvasPanel.addComponent(panel).inTL(FLOAT_ZERO, FLOAT_ZERO);

                buildTooltip(panel);
                root.addComponent(canvasPanel).rightOfTop(sibling, 5f);
            }
    
        }
    
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
            // for (InputEventAPI event : events) {
            // }
        }
    
        @Override
        public void buttonPressed(Object buttonId) {
        }
    }

    public class TableRowListener implements CustomUIPanelPlugin  {
        public static final Logger logger = Logger.getLogger(FleetPresetManagementListener.class);

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
                    if (eventX >= rX &&
                    eventX <= rX + rW &&
                    eventY >= rY &&
                    eventY <= rY + rH) {
                        selectedPresetName = rowName;

                        ButtonAPI deleteButton = theButtons.get(DELETE_BUTTON_ID);
                        ButtonAPI restoreButton = theButtons.get(RESTORE_BUTTON_ID);

                        if (DockingListener.canPlayerAccessStorage()) {
                            restoreButton.setEnabled(true);
                        }

                        deleteButton.setEnabled(true);
                        tablePlugin.rebuild();
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
    
}