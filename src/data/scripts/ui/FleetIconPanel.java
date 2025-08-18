package data.scripts.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.ui.impl.StandardTooltipV2;

import data.scripts.ClassRefs;
import data.scripts.ui.TreeTraverser.TreeNode;
import data.scripts.util.ListenerFactory.ActionListener;
import data.scripts.util.PresetMiscUtils;
import data.scripts.util.PresetUtils;
import data.scripts.util.ReflectionUtilis;
import data.scripts.util.UtilReflection;

import java.awt.Color;
import java.util.*;

public class FleetIconPanel {
    public void print(Object... args) {
        PresetMiscUtils.print(args);
    }

    private CustomPanelAPI basePanel;
    private Map<ButtonAPI, Object> buttonToRenderControllerMap;
    private float width;
    private float height;

    public CustomPanelAPI getPanel() {
        return this.basePanel;
    }

    public FleetIconPanel(String name, CampaignFleetAPI fleet, Map<Integer, FleetMemberAPI> whichFleetMembersAvailable) {
        UIPanelAPI obfFleetPanel = UtilReflection.createObfFleetIconPanel(name, fleet);
        PositionAPI pos = obfFleetPanel.getPosition();

        width = pos.getWidth();
        height = pos.getHeight();
        
        TreeTraverser trav = new TreeTraverser(obfFleetPanel, 0, 0, 0, 1, 0, 0, 0, 0);
        List<ButtonAPI> buttons = new ArrayList<>();
        for (UIComponentAPI btn : trav.getTargetNode().getChildren()) buttons.add((ButtonAPI)btn);
        trav.clearPanel();
        
        LabelAPI label = Global.getSettings().createLabel(name, "graphics/fonts/orbitron20aabold.fnt");
        label.setColor(Misc.getBasePlayerColor());
        label.setHighlightOnMouseover(true);
        label.setHighlightColor(Color.YELLOW);

        buttonToRenderControllerMap = UtilReflection.getButtonToRenderControllerMap(ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonGetListenerMethod, buttons.get(0)));

        basePanel = Global.getSettings().createCustom(width, 0f, null);
        basePanel.addComponent((UIComponentAPI)label).inMid();

        CustomPanelAPI pane = Global.getSettings().createCustom(width, height-label.getPosition().getHeight()-10f, null);
        basePanel.addComponent(pane).belowMid((UIComponentAPI)label, 10f);

        TooltipMakerAPI ttA = pane.createUIElement(width, height-label.getPosition().getHeight()-10f, true);

        float yOffset;
        if (fleet.getFleetData().getMembersListCopy().size() < 7) {
            yOffset = whichFleetMembersAvailable == null ? arrayButtonsCentered(ttA, buttons, fleet, 0f) : arrayButtonsCentered(ttA, buttons, fleet, whichFleetMembersAvailable, 0f);
        } else {
            yOffset = whichFleetMembersAvailable == null ? arrayButtons(ttA, buttons, fleet, 0f) : arrayButtons(ttA, buttons, fleet, whichFleetMembersAvailable, 0f);
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
    }

    // array as a verb
    private float arrayButtons(TooltipMakerAPI ttA, List<ButtonAPI> buttons, CampaignFleetAPI fleet, Map<Integer, FleetMemberAPI> whichFleetMembersAvailable, float yOffset) {
        float xOffset = 0;
        int i = 0;
        PositionAPI bPos = null;

        for (ButtonAPI button : buttons) {
            if (whichFleetMembersAvailable.get(i) == null) {
                button.setEnabled(false);
                button.setOpacity(0.66f);
                UtilReflection.setButtonTooltipWithPostProcessing(button, fleet.getFleetData().getMembersListCopy().get(i));
                UtilReflection.setShipButtonHighlightColor(buttonToRenderControllerMap.get(button), UtilReflection.DARK_RED);
            } else {
                UtilReflection.setButtonTooltip(button, fleet.getFleetData().getMembersListCopy().get(i));
            }

            if (xOffset > width - 25f) {
                xOffset = 0f;
                yOffset += bPos.getHeight();
            }
            bPos = ttA.addComponent((UIComponentAPI)button).inTL(xOffset, yOffset);
            
            xOffset += bPos.getWidth();
            i++;
        }
        return yOffset;
    }

    private float arrayButtons(TooltipMakerAPI ttA, List<ButtonAPI> buttons, CampaignFleetAPI fleet, float yOffset) {
        float xOffset = 0;
        int i = 0;

        PositionAPI bPos = null;
        for (ButtonAPI button : buttons) {
            UtilReflection.setButtonTooltip(button, fleet.getFleetData().getMembersListCopy().get(i));

            if (xOffset > width - 25f) {
                xOffset = 0f;
                yOffset += bPos.getHeight();
            }
            bPos = ttA.addComponent((UIComponentAPI)button).inTL(xOffset, yOffset);
            
            xOffset += bPos.getWidth();
            i++;
        }
        return yOffset;
    }

    private float arrayButtonsCentered(TooltipMakerAPI ttA, List<ButtonAPI> buttons, CampaignFleetAPI fleet, Map<Integer, FleetMemberAPI> whichFleetMembersAvailable, float yOffset) {
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
                UtilReflection.setButtonTooltipWithPostProcessing(button, fleet.getFleetData().getMembersListCopy().get(i));
                UtilReflection.setShipButtonHighlightColor(buttonToRenderControllerMap.get(button), UtilReflection.DARK_RED);
            } else {
                UtilReflection.setButtonTooltip(button, fleet.getFleetData().getMembersListCopy().get(i));
            }

            if (xOffset > width - 25f) {
                xOffset = 0f;
                yOffset += bPos.getHeight();
            }
            bPos = ttA.addComponent((UIComponentAPI)button).inTL(xOffset, yOffset);
            
            xOffset += bPos.getWidth();
            i++;
        }
        return yOffset;
    }

    private float arrayButtonsCentered(TooltipMakerAPI ttA, List<ButtonAPI> buttons, CampaignFleetAPI fleet, float yOffset) {
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

            UtilReflection.setButtonTooltip(button, fleet.getFleetData().getMembersListCopy().get(i));

            if (xOffset > width - 25f) {
                xOffset = 0f;
                yOffset += bPos.getHeight();
            }
            bPos = ttA.addComponent((UIComponentAPI)button).inTL(xOffset, yOffset);
            
            xOffset += bPos.getWidth();
            i++;
        }
        return yOffset;
    }
}
