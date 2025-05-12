// THIS IS TOTAL SPAGHETTI READ AT YOUR OWN RISK
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
import data.scripts.util.HighlightRectangle;
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

    private final Color c1 = Global.getSettings().getBasePlayerColor();
    private final Color c2 = Global.getSettings().getDarkPlayerColor();

    // private final CaptainPickerDialog dialog;
    // private final FleetPresetManagerCoreScript injector;

    private TextFieldAPI saveNameField;
    private String selectedPresetName;
    private HashMap<String, ButtonAPI> theButtons;
    private ButtonAPI cancelButton;

    private final HashMap<String, String> buttonToolTipParas = new HashMap<>();

    public FleetPresetManagementListener() {
        super();
        buttonToolTipParas.put("saveDialogButton", "Save current fleet as preset.");
        buttonToolTipParas.put("restoreButton", "Stores entire fleet in storage and then loads the selected preset.");
    }

    @Override
    public void trigger(Object... args) {

        List<String> buttonIds = new ArrayList<>();
        buttonIds.add("saveDialogButton");
        buttonIds.add("restoreButton");
        ButtonPlugin buttonPlugin = new ButtonPlugin(buttonIds);

        DialogDismissedListener dummyListener = new DummyDialogListener();
        UtilReflection.ConfirmDialogData data = UtilReflection.showConfirmationDialog(
            "",
            "",
            "",
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
        float cancelButtonWidth = cancelButtonPosition.getWidth();

        CustomPanelAPI customPanel = Global.getSettings().createCustom(cancelButtonWidth, PANEL_HEIGHT, buttonPlugin);
        TooltipMakerAPI tooltipMaker = customPanel.createUIElement(cancelButtonWidth, PANEL_HEIGHT, true);
        buttonPlugin.init(customPanel, tooltipMaker);

        theButtons = addTheButtons(tooltipMaker, confirmButtonPosition, cancelButtonPosition);

        data.panel.removeComponent(confirmButton);
        data.panel.removeComponent(cancelButton);
        this.cancelButton = cancelButton;

        CustomPanelAPI tablePanel = Global.getSettings().createCustom(PANEL_WIDTH - cancelButtonWidth, PANEL_HEIGHT, null);
        TooltipMakerAPI tableTipMaker = tablePanel.createUIElement(PANEL_WIDTH - cancelButtonWidth, PANEL_HEIGHT, true);

        UIPanelAPI table = tableTipMaker.beginTable(c1, c2,
        Misc.getHighlightedOptionColor(), 30f, true, true, 
        new Object[]{"Preset Name", NAME_COLUMN_WIDTH, "Ships", SHIP_COLUMN_WIDTH}
         );

        Map<String, String> presets = PresetUtils.getFleetPresetsMapForTable();
        List<TableRowListener> tableRowListeners = new ArrayList<>();

        int id = 0;
        for (Map.Entry<String, String> entry: presets.entrySet()) {
            String rowName = entry.getKey();
            Object row = tableTipMaker.addRowWithGlow(
                c1, 
                rowName,
                c1,
                entry.getValue()
            );

            PositionAPI rowPos = (PositionAPI) ReflectionUtilis.invokeMethod("getPosition", row);

            // logger.info(String.valueOf(rowPos.getX()) + " " + String.valueOf(rowPos.getY()));

            TableRowListener rowListener = new TableRowListener(rowPos, rowName, tableRowListeners, id);
            CustomPanelAPI rowOverlayPanel = Global.getSettings().createCustom(NAME_COLUMN_WIDTH + SHIP_COLUMN_WIDTH - 10f, 29f, rowListener);
            TooltipMakerAPI rowOverlayTooltipMaker = rowOverlayPanel.createUIElement(NAME_COLUMN_WIDTH + SHIP_COLUMN_WIDTH - 10f, 29f, false);

            tableTipMaker.addComponent(rowOverlayPanel).inTL(rowPos.getX(), rowPos.getY());
            rowListener.init(rowOverlayPanel, rowOverlayTooltipMaker, rowPos);
            
            tableRowListeners.add(rowListener);
            id++;
        }

        tableTipMaker.addTable("Fleet Presets Go Here", 0, 0f);

        tablePanel.addUIElement(tableTipMaker);
        customPanel.addUIElement(tooltipMaker);

        data.panel.addComponent(customPanel).inTL(0f, 0f);
        data.panel.addComponent(tablePanel).rightOfTop(customPanel, 10f);
        data.panel.sendToBottom(tablePanel);
    }

    private HashMap<String, ButtonAPI> addTheButtons(TooltipMakerAPI tooltipMaker, PositionAPI confirmPosition, PositionAPI cancelPosition) {
        HashMap<String, ButtonAPI> buttons = new HashMap<>();
        
        ButtonAPI saveDialogButton = tooltipMaker.addButton("SAVE FLEET", "saveDialogButton", c1, c2,
        Alignment.BR, CutStyle.ALL, confirmPosition.getWidth(), cancelPosition.getHeight(), 5f);
        saveDialogButton.setShortcut(Keyboard.KEY_1, false);

        ButtonAPI loadPresetButton = tooltipMaker.addButton("RESTORE", "restoreButton", c1, c2,
        Alignment.BR, CutStyle.ALL, confirmPosition.getWidth(), cancelPosition.getHeight(), 5f);
        // loadPresetButton.setShortcut(Keyboard.KEY_2, false);

        ButtonAPI storeAllButton = tooltipMaker.addButton("STORE FLEET", "storeButton", c1, c2,
        Alignment.BR, CutStyle.ALL, confirmPosition.getWidth(), confirmPosition.getHeight(), 5f);
        storeAllButton.setShortcut(Keyboard.KEY_3, false);

        ButtonAPI deleteButton = tooltipMaker.addButton("DELETE", "deleteButton", c1, c2,
        Alignment.BR, CutStyle.ALL, confirmPosition.getWidth(), confirmPosition.getHeight(), 5f);
        // deleteButton.setShortcut(Keyboard.KEY_4, false);

        
        if (!DockingListener.isPlayerDocked()) {
            loadPresetButton.setEnabled(false);
            storeAllButton.setEnabled(false);
            deleteButton.setEnabled(false);
        }
        
        buttons.put("saveDialogButton", saveDialogButton);
        buttons.put("restoreButton", loadPresetButton);
        buttons.put("storeButton", storeAllButton);
        buttons.put("deleteButton", deleteButton);

        return buttons;
    }

    private UtilReflection.ConfirmDialogData openSaveDialog() {

        SaveFleetPreset saveListener = new SaveFleetPreset();
        CustomPanelAPI textFieldPanel = Global.getSettings().createCustom(CONFIRM_DIALOG_WIDTH / 2 / 6, CONFIRM_DIALOG_HEIGHT / 2 / 12, null);
        TooltipMakerAPI textFieldTooltipMaker = textFieldPanel.createUIElement(CONFIRM_DIALOG_WIDTH / 2 / 5, CONFIRM_DIALOG_HEIGHT / 2 / 10, false);
        saveNameField = textFieldTooltipMaker.addTextField(CONFIRM_DIALOG_WIDTH/3, CONFIRM_DIALOG_HEIGHT/2/3, "graphics/fonts/orbitron24aabold.fnt", 10f);
        textFieldPanel.addUIElement(textFieldTooltipMaker).inTL(0f, 0f);

        UtilReflection.ConfirmDialogData subData = UtilReflection.showConfirmationDialog(
            "Enter Preset Name:",
            "Save preset",
            "Cancel",
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
            "Are you sure you want to delete preset " + selectedPresetName + "?",
            "Confirm",
            "Cancel",
            CONFIRM_DIALOG_WIDTH / 2,
            CONFIRM_DIALOG_HEIGHT / 2,
            deleteListener);

        subData.panel.addComponent(textPanel).inTL(0f, 0f);
        return subData;
    }

    private class SaveFleetPreset extends DialogDismissedListener {
        public static final Logger logger = Logger.getLogger(SaveFleetPreset.class);
    
        @Override
        public void trigger(Object... args) {
            int option = (int) args[1];

            if (option == 0) {
                // confirm
                String text = saveNameField.getText();
                if (!text.equals("")) PresetUtils.saveFleetPreset(text);

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
                    case "saveDialogButton":
                        openSaveDialog();
                        return;
                    case "restoreButton":
                        logger.info(selectedPresetName);
                        PresetUtils.restoreFleetFromPreset(selectedPresetName);
                        return;
                    case "storeButton":
                        logger.info("STORAGE PRESSED");
                        PresetUtils.storeFleetInStorage(selectedPresetName);
                        return;
                    case "deleteButton":
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

        public HighlightRectangle highLightRect;
    
        public TableRowListener(PositionAPI rowPos, String rowPresetName, List<TableRowListener> tableRowListeners, int id) {
            this.tableRowListeners = tableRowListeners;
            
            this.id = id;
            this.rowName = rowPresetName;
            // this.rowPos = rowPos;
            // logger.info(String.valueOf(rowPos.getX()) + " " + String.valueOf(rowPos.getY()));
            // logger.info("------------");
        }
    
        public void init(CustomPanelAPI panel, TooltipMakerAPI tooltipMaker, PositionAPI rowPos) {
            this.panel = panel;
            this.rowPos = rowPos;
            // this.highLightRect = new HighlightRectangle(0f, 0f, 0f, 0f, this.id, this.id, 1);
            // logger.info(String.valueOf(rowPos.getX()) + " " + String.valueOf(rowPos.getY()));
            // logger.info("------------");
            // this.highLightRect = new HighlightRectangle(row, this.id, this.id, 1);
        }

        public void clearHighlight() {
            // if (highLightRect != null) 
            // highLightRect.setHighlightFalse();
        }
    
        @Override
        public void buttonPressed(Object arg0) {
        }
    
        @Override
        public void positionChanged(PositionAPI arg0) {
        }

        @Override
        public void advance(float arg0) {
            // highLightRect.highlight(arg0);
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

                        ButtonAPI deleteButton = theButtons.get("deleteButton");
                        ButtonAPI restoreButton = theButtons.get("restoreButton");

                        if (DockingListener.isPlayerDocked()) {
                            restoreButton.setText(trimName(rowName, "RESTORE "));
                            restoreButton.setEnabled(true);
                        }

                        deleteButton.setText(trimName(rowName, "DELETE "));
                        deleteButton.setEnabled(true);

                    } // This doesnt work because of extra mouse events interfering? idk
                    // else {
                    //     selectedPresetName = "";

                    //     ButtonAPI deleteButton = theButtons.get("deleteButton");
                    //     ButtonAPI restoreButton = theButtons.get("restoreButton");
                    //     restoreButton.setText("RESTORE");
                    //     restoreButton.setEnabled(false);

                    //     deleteButton.setEnabled(false);
                    // }

                        

                        // logger.info(String.valueOf(rX) + " " + String.valueOf(rY));
                        // this.highLightRect.setXAndY(rowPos.getX(), (rowPos.getY() * 2));
                        // this.highLightRect.setCoordsAndSize(rX, rY, rW, rH);
                        // this.highLightRect.setCoordsAndSize(-0.5f, 0f, rW/300, rH/300);
                        // this.highLightRect.setHighlightTrue();

                        // logger.info(String.valueOf(eventX) + " " + String.valueOf(eventY));
                        // logger.info("_______________" + rowName + " FIN");

                        // for (TableRowListener rowListener : tableRowListeners) {
                        //     if (!(rowListener.id == this.id)) {
                        //         rowListener.clearHighlight();
                        //     }
                        // }
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