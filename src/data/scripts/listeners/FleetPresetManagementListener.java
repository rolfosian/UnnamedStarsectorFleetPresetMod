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

public class FleetPresetManagementListener extends ActionListener {
    private static final Logger logger = Logger.getLogger(FleetPresetManagementListener.class);
    private static final String MEMORY_KEY = PresetUtils.MEMORY_KEY;

    private static final float DISPLAY_WIDTH = (float)Global.getSettings().getScreenWidth();
    private static final float DISPLAY_HEIGHT = (float)Global.getSettings().getScreenHeight();
    private static final float CONFIRM_DIALOG_WIDTH = DISPLAY_WIDTH / 2;
    private static final float CONFIRM_DIALOG_HEIGHT = DISPLAY_HEIGHT / 2;  
    private static final float PANEL_WIDTH = DISPLAY_WIDTH / 2;
    private static final float PANEL_HEIGHT = DISPLAY_HEIGHT / 3;
    private static final float NAME_COLUMN_WIDTH = PANEL_WIDTH / 2;
    private static final float SHIP_COLUMN_WIDTH = PANEL_WIDTH / 2;
    private static final float ROW_HEIGHT = 50f;

    // private final CaptainPickerDialog dialog;
    // private final FleetPresetManagerCoreScript injector;

    private TextFieldAPI saveNameField;
    private String selectedPresetName;
    private List<ButtonAPI> theButtons;

    private final HashMap<String, String> buttonToolTipParas = new HashMap<>();

    public FleetPresetManagementListener() {
        super();
        buttonToolTipParas.put("saveDialogButton", "Save current fleet as preset.");
        buttonToolTipParas.put("loadButton", "Stores entire fleet in storage and then loads the selected preset.");
    }

    @Override
    public void trigger(Object... args) {

        List<String> buttonIds = new ArrayList<>();
        buttonIds.add("saveDialogButton");
        buttonIds.add("loadButton");
        ButtonPlugin buttonPlugin = new ButtonPlugin(buttonIds);
        
        CustomPanelAPI customPanel = Global.getSettings().createCustom(PANEL_WIDTH, PANEL_HEIGHT, buttonPlugin);
        TooltipMakerAPI tooltipMaker = customPanel.createUIElement(PANEL_WIDTH, PANEL_HEIGHT, true);
        buttonPlugin.init(customPanel, tooltipMaker);

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

        theButtons = addTheButtons(tooltipMaker, confirmButtonPosition, cancelButtonPosition);

        data.panel.removeComponent(confirmButton);
        data.panel.removeComponent(cancelButton);

        customPanel.addUIElement(tooltipMaker);
        data.panel.addComponent(customPanel).inTL(0f, 0f);
    }

    private List<ButtonAPI> addTheButtons(TooltipMakerAPI tooltipMaker, PositionAPI confirmPosition, PositionAPI cancelPosition) {
        List<ButtonAPI> buttons = new ArrayList<>();
        
        Color c1 = Global.getSettings().getBasePlayerColor();
        Color c2 = Global.getSettings().getDarkPlayerColor();
        ButtonAPI saveDialogButton = tooltipMaker.addButton("Save current fleet", "saveDialogButton", c1, c2,
        Alignment.BR, CutStyle.BL_TR, confirmPosition.getWidth(), cancelPosition.getHeight(), 5f);

        ButtonAPI loadPresetButton = tooltipMaker.addButton("Load selected", "loadButton", c1, c2,
        Alignment.BR, CutStyle.BL_TR, confirmPosition.getWidth(), cancelPosition.getHeight(), 5f);

        if (!DockingListener.isPlayerDocked() || DockingListener.getPlayerCurrentMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE) == null ) {
            loadPresetButton.setEnabled(false);
        }

        buttons.add(saveDialogButton);
        buttons.add(loadPresetButton);

        return buttons;
    }

    private UtilReflection.ConfirmDialogData openSaveDialog() {
        SaveFleetPreset saveListener = new SaveFleetPreset();
        CustomPanelAPI textFieldPanel = Global.getSettings().createCustom(CONFIRM_DIALOG_WIDTH / 2 / 5, CONFIRM_DIALOG_HEIGHT / 2 /5, null);
        TooltipMakerAPI textFieldTooltipMaker = textFieldPanel.createUIElement(CONFIRM_DIALOG_WIDTH / 2 / 5, CONFIRM_DIALOG_HEIGHT / 2 / 5, false);
        saveNameField = textFieldTooltipMaker.addTextField(CONFIRM_DIALOG_WIDTH/2, CONFIRM_DIALOG_HEIGHT/2/2, "graphics/fonts/orbitron24aabold.fnt", 10f);
        textFieldPanel.addUIElement(textFieldTooltipMaker);

        UtilReflection.ConfirmDialogData subData = UtilReflection.showConfirmationDialog(
            "",
            "Save preset",
            "Cancel",
            CONFIRM_DIALOG_WIDTH / 2,
            CONFIRM_DIALOG_HEIGHT / 2,
            saveListener);

        subData.panel.addComponent(textFieldPanel).inTL(0f, 0f);
        saveNameField.showCursor();

        return subData;
    }

    private class SaveFleetPreset extends DialogDismissedListener {
        private static final Logger logger = Logger.getLogger(SaveFleetPreset.class);
    
        @Override
        public void trigger(Object... args) {
            int option = (int) args[1];

            if (option == 0) {
                // confirm
                PresetUtils.saveFleetPreset(saveNameField.getText());
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

            for (String buttonId : buttonIds) {
                CustomPanelAPI tooltipPanel = Global.getSettings().createCustom(250f, 60f, null);
                TooltipMakerAPI tooltip = tooltipPanel.createUIElement(250f, 60f, false);
                tooltip.addPara(buttonToolTipParas.get(buttonId), 0f);
                tooltipPanel.wrapTooltipWithBox(tooltip, Misc.getBasePlayerColor());
            
                tooltipPanel.addUIElement(tooltip).inTL(0f, 0f);
                tooltipMap.put(buttonId, tooltipPanel);
            }


        }
        
        public void init(CustomPanelAPI panel, TooltipMakerAPI tooltip) {
            this.masterPanel = panel;
            this.masterTooltip = tooltip;
        }

        private void showButtonToolTipAtLocation(String buttonId) {
            CustomPanelAPI toolTipPanel = tooltipMap.get(buttonId);
            toolTipPanel.setOpacity(100f);
            float width = 0f;
            float height = 0f;

            for (ButtonAPI button : theButtons) {
                if (button.getCustomData().equals(buttonId)) {
                    PositionAPI pos = button.getPosition();
                    width = pos.getWidth();
                    height = pos.getHeight();
                    break;
                }
            }

            this.masterTooltip.addComponent(toolTipPanel).inTL(width + 5f, height - 10f);
        }

        private void destroyButtonToolTip(String buttonId) {
            CustomPanelAPI toolTipPanel = tooltipMap.get(buttonId);
            toolTipPanel.setOpacity(0f);
            this.masterTooltip.removeComponent(toolTipPanel);
        }

        @Override
        public void advance(float amount) {
    
        }
    
        @Override
        public void buttonPressed(Object arg0) {
            if (arg0.equals("saveDialogButton")) {
                openSaveDialog();
                return;
            } else if (arg0.equals("loadButton")) {
                PresetUtils.restoreFleetFromPreset(selectedPresetName);
                }
        }
    
        @Override
        public void positionChanged(PositionAPI arg0) {
    
        }

        //this is so dirty i dont like it
        @Override
        public void processInput(List<InputEventAPI> arg0) {
            for (InputEventAPI event : arg0) {
                if (event.isMouseMoveEvent()) {
                    int mouseX = event.getX();
                    int mouseY = event.getY();

                    ButtonAPI button = getButton(theButtons, mouseX, mouseY);
                    if (button != null) {
                        String buttonId = (String) button.getCustomData();
                        this.isTooltip = true;
                        this.currentTooltipId = new String(buttonId);
                        showButtonToolTipAtLocation(buttonId);
                        return;
                    }
                    if (this.isTooltip && this.currentTooltipId != null) {
                        destroyButtonToolTip(this.currentTooltipId);
                        this.isTooltip = false;
                        this.currentTooltipId = null;
                        return;
                        // i dont know why we have to do this, probably because concurrency ticks take too long
                    } else {
                        for (String buttonId : this.buttonIds) {
                            tooltipMap.get(buttonId).setOpacity(0f);
                        }
                    }
                }
            }
        }

        private ButtonAPI getButton (List<ButtonAPI> buttons, int mouseX, int mouseY) {
            for (ButtonAPI button : theButtons) {
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



}