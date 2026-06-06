package data.scripts.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;

import data.scripts.util.UiUtil;

import static data.scripts.util.UiUtil.utils;

import data.scripts.util.PresetMiscUtils;
// import data.scripts.util.PresetUtils;
import data.scripts.util.UtilUi;

import java.awt.Color;
import java.util.*;

@SuppressWarnings("unchecked")
public class FleetIconPanel {
    private static final float DEFAULT_ICON_SIZE = 56f;

    private CustomPanelAPI basePanel;
    private Map<ButtonAPI, Object> buttonToRenderControllerMap;
    private float width;
    private float height;

    public CustomPanelAPI getPanel() {
        return this.basePanel;
    }

    public FleetIconPanel(String name, List<FleetMemberAPI> fleetMembers, Map<Integer, FleetMemberAPI> whichFleetMembersAvailable) {
        UIPanelAPI shipIconList = UtilUi.createShipIconListPanel(UtilUi.DARK_GREEN, fleetMembers);
        PositionAPI pos = shipIconList.getPosition();
        pos.setSize(400f, 400f);

        width = 400f;
        height = 400f;
        
        buttonToRenderControllerMap = UtilUi.getButtonToRenderControllerMap(shipIconList);
        List<ButtonAPI> buttons = (List<ButtonAPI>) (List<?>) utils.listPanelGetItems((UIPanelAPI)UiUtil.shipIconListListPanelHandle.get(shipIconList));
        for (int i = buttons.size() - 1; i >= 0; i--) {
            ButtonAPI btn = buttons.get(i);
            utils.getParent(btn).removeComponent(btn);
        }
        
        LabelAPI label = Global.getSettings().createLabel(name, "graphics/fonts/orbitron20aabold.fnt");
        label.setColor(Misc.getBasePlayerColor());
        label.setHighlightOnMouseover(true);
        label.setHighlightColor(Color.YELLOW);

        basePanel = Global.getSettings().createCustom(width, 0f, null);
        basePanel.addComponent((UIComponentAPI)label).inMid();

        CustomPanelAPI pane = Global.getSettings().createCustom(width, height-label.getPosition().getHeight()-10f, null);
        TooltipMakerAPI ttA = pane.createUIElement(width, height-label.getPosition().getHeight()-10f, true);

        float yOffset;
        if (fleetMembers.size() < 7) {
            yOffset = whichFleetMembersAvailable == null ? arrayButtonsCentered(ttA, buttons, fleetMembers, 0f) : arrayButtonsCentered(ttA, buttons, fleetMembers, whichFleetMembersAvailable, 0f);
        } else {
            yOffset = whichFleetMembersAvailable == null ? arrayButtons(ttA, buttons, fleetMembers, 0f) : arrayButtons(ttA, buttons, fleetMembers, whichFleetMembersAvailable, 0f);
        }

        ttA.setHeightSoFar(yOffset);
        pane.addUIElement(ttA);

        Color color = Misc.getBrightPlayerColor();
        pane.addComponent(Global.getSettings().createCustom(0f, 0f, new BaseCustomUIPanelPlugin() {
            private float red = color.getRed() / 255f;
            private float green = color.getGreen() / 255f;
            private float blue = color.getBlue() / 255f;

            @Override
            public void render(float alphaMult) {
                PresetMiscUtils.drawTaperedLine(red, green, blue,
                    pane.getPosition().getCenterX(),
                    label.getPosition().getY()-5f,
                    0f,
                    width,
                    1f,
                    0.3f
                );
            }
        })).inTL(0f, 0f);

        basePanel.addComponent(pane).belowMid((UIComponentAPI)label, 10f);
    }

    // array as a verb
    private float arrayButtons(TooltipMakerAPI ttA, List<ButtonAPI> buttons, List<FleetMemberAPI> fleetMembers, Map<Integer, FleetMemberAPI> whichFleetMembersAvailable, float yOffset) {
        float xOffset = 0;
        int i = 0;
        PositionAPI bPos = null;

        for (ButtonAPI button : buttons) {
            if (whichFleetMembersAvailable.get(i) == null) {
                button.setEnabled(false);
                button.setOpacity(0.66f);
                UtilUi.setButtonTooltipWithPostProcessing(button, fleetMembers.get(i));
                UtilUi.setShipButtonHighlightColor(buttonToRenderControllerMap.get(button), UtilUi.DARK_RED);
            } else {
                UtilUi.setButtonTooltip(button, whichFleetMembersAvailable.get(i));
            }

            if (xOffset > width - 25f) {
                xOffset = 0f;
                yOffset += bPos.getHeight();
            }
            bPos = ttA.addComponent((UIComponentAPI)button).inTL(xOffset, yOffset);
            
            xOffset += bPos.getWidth();
            i++;
        }
        return yOffset + bPos.getHeight();
    }

    private float arrayButtons(TooltipMakerAPI ttA, List<ButtonAPI> buttons, List<FleetMemberAPI> fleetMembers, float yOffset) {
        float xOffset = 0;
        int i = 0;

        PositionAPI bPos = null;
        for (ButtonAPI button : buttons) {
            UtilUi.setButtonTooltip(button, fleetMembers.get(i));

            if (xOffset > width - 25f) {
                xOffset = 0f;
                yOffset += bPos.getHeight();
            }
            bPos = ttA.addComponent((UIComponentAPI)button).inTL(xOffset, yOffset);

            xOffset += bPos.getWidth();
            i++;
        }
        return yOffset + bPos.getHeight();
    }

    private float arrayButtonsCentered(TooltipMakerAPI ttA, List<ButtonAPI> buttons, List<FleetMemberAPI> fleetMembers, Map<Integer, FleetMemberAPI> whichFleetMembersAvailable, float yOffset) {
        PositionAPI bPos = buttons.get(0).getPosition();

        float scaleFactor;
        int count = buttons.size();
        float t = (7 - count) / 6f; 
        scaleFactor = 1.1f + t * (1.4f - 1.1f);

        float newWidth = bPos.getWidth() * scaleFactor;
        float newHeight = bPos.getHeight() * scaleFactor;

        float totalWidth = newWidth * count;
        float xOffset = (width - totalWidth) / 2f;
        int i = 0;
        
        for (ButtonAPI button : buttons) {
            bPos = button.getPosition();
            bPos.setSize(newWidth, newHeight);

            if (whichFleetMembersAvailable.get(i) == null) {
                button.setEnabled(false);
                button.setOpacity(0.66f);
                UtilUi.setButtonTooltipWithPostProcessing(button, fleetMembers.get(i));
                UtilUi.setShipButtonHighlightColor(buttonToRenderControllerMap.get(button), UtilUi.DARK_RED);
            } else {
                UtilUi.setButtonTooltip(button, whichFleetMembersAvailable.get(i));
            }

            if (xOffset > width - 25f) {
                xOffset = 0f;
                yOffset += bPos.getHeight();
            }
            bPos = ttA.addComponent((UIComponentAPI)button).inTL(xOffset, yOffset);
            
            xOffset += bPos.getWidth();
            i++;
        }
        return yOffset + bPos.getHeight();
    }

    private float arrayButtonsCentered(TooltipMakerAPI ttA, List<ButtonAPI> buttons, List<FleetMemberAPI> fleetMembers, float yOffset) {
        PositionAPI bPos = buttons.get(0).getPosition();

        float scaleFactor;
        int count = buttons.size();
        float t = (7 - count) / 6f; 
        scaleFactor = 1.1f + t * (1.4f - 1.1f);

        float newWidth = bPos.getWidth() * scaleFactor;
        float newHeight = bPos.getHeight() * scaleFactor;

        float totalWidth = newWidth * count;
        float xOffset = (width - totalWidth) / 2f;
        int i = 0;

        for (ButtonAPI button : buttons) {
            bPos = button.getPosition();
            bPos.setSize(newWidth, newHeight);

            UtilUi.setButtonTooltip(button, fleetMembers.get(i));

            if (xOffset > width - 25f) {
                xOffset = 0f;
                yOffset += bPos.getHeight();
            }
            bPos = ttA.addComponent((UIComponentAPI)button).inTL(xOffset, yOffset);
            
            xOffset += bPos.getWidth();
            i++;
        }
        return yOffset + bPos.getHeight();
    }
}
