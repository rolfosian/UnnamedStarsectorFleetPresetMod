package data.scripts.util;

import com.fs.graphics.util.Fader;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.input.InputEventClass;
import com.fs.starfarer.api.input.InputEventType;

import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.IntervalUtil;

import com.fs.starfarer.campaign.CharacterStats;
import com.fs.starfarer.campaign.fleet.FleetMember;
import com.fs.starfarer.ui.impl.StandardTooltipV2;

import java.util.*;

import static data.scripts.util.TreeTraverser.TreeNode;
import static data.scripts.util.UiUtil.ActionListener;
import static data.scripts.util.UiUtil.DialogDismissedListener;
import static data.scripts.util.UiUtil.instantiator;
import static data.scripts.util.UiUtil.utils;

import java.awt.Color;

// import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

// @SuppressWarnings("unchecked")
public class UtilUi {    
    public static final Color DARK_RED = new Color(139, 0, 0);
    public static final Color DARK_GREEN = new Color(0, 139, 0);

    public static InputEventAPI createButtonClickEventInstance(PositionAPI buttonPosition) {
        return instantiator.instantiateInputEvent(
        InputEventClass.MOUSE_EVENT,
        InputEventType.MOUSE_DOWN,
        (int)buttonPosition.getCenterX(),
        (int)buttonPosition.getCenterY(),
        0, // LMB
        '\0' // unused?
        );
    }

    public static InputEventAPI createInputEventInstance(InputEventClass eventClass, InputEventType eventType, int x, int y, int val, char char_) {
        return instantiator.instantiateInputEvent(
            eventClass,
            eventType,
            x,
            y,
            val, // keyboard key or mouse button, is -1 for mouse move
            char_ // char is only appicable for keyboard keys afaik, give '\0' for mouse prob
        );
    }

    public static void clickButton(ButtonAPI button) {
        if (button == null) return;

        Object listener = utils.buttonGetListener(button);
        utils.actionPerformed(listener, button, createButtonClickEventInstance(((ButtonAPI)button).getPosition()));
    }

    public static List<UIComponentAPI> getChildrenRecursive(UIComponentAPI parentPanel) {
        List<UIComponentAPI> list = new ArrayList<>();
        collectChildren(parentPanel, list);
        return list;
    }
    
    private static void collectChildren(UIComponentAPI parent, List<UIComponentAPI> list) {
        List<UIComponentAPI> children = (List<UIComponentAPI>) utils.getChildrenNonCopy(parent);

        if (children != null) {
            for (UIComponentAPI child : children) {
                list.add(child);
                collectChildren(child, list);
            }
        }
    }

    public static void setConfirmDialogButtonInterceptor(ButtonAPI btn, UIPanelAPI dialog, CustomPanelAPI bgImagePanel) {
        Object oldListener = utils.buttonGetListener(btn);

        utils.buttonSetListener(btn, new ActionListener() {
            public void actionPerformed(Object arg0, Object arg1) {
                bgImagePanel.setOpacity(0f);
                (((BackGroundImagePanelPlugin)bgImagePanel.getPlugin())).tt.setOpacity(0f);

                bgImagePanel.removeComponent(((BackGroundImagePanelPlugin)bgImagePanel.getPlugin()).tt);
                dialog.removeComponent(bgImagePanel);

                utils.actionPerformed(oldListener, arg0, arg1);
            }
        }.getProxy());
    }

    public static void outsideClickAbsorbed(UIPanelAPI confirmDialog) {
        try {
            UiUtil.outsideClickAbsorbHandle.invoke(
                confirmDialog, createInputEventInstance(
                    InputEventClass.MOUSE_EVENT,
                    InputEventType.MOUSE_DOWN,
                    9999,
                    9999,
                    0,
                    '\0'
                )
            );
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static ConfirmDialogData showConfirmationDialog(
        String text,
        String confirmText,
        String cancelText,
        float width,
        float height,
        DialogDismissedListener dialogListener
    ) {
        UIPanelAPI confirmDialog = createConfirmDialog(text, confirmText, cancelText, width, height, dialogListener);
        utils.confirmDialogShow(confirmDialog, 0.25f, 0.25f);

        LabelAPI label = utils.confirmDialogGetLabel(confirmDialog);
        ButtonAPI yes = utils.confirmDialogGetButton(confirmDialog, 0);
        ButtonAPI no = utils.confirmDialogGetButton(confirmDialog, 1);
        return new ConfirmDialogData(
            label,
            yes,
            no,
            utils.confirmDialogGetInnerPanel(confirmDialog),
            confirmDialog
        );
    }

    public static CustomPanelAPI addBackGroundImage(UIPanelAPI confirmDialog, ButtonAPI cancelButton, String backgroundImagePath) {
        PositionAPI dialogPos = confirmDialog.getPosition();

        BackGroundImagePanelPlugin plugin = new BackGroundImagePanelPlugin(dialogPos);
        CustomPanelAPI bgImagePanel = Global.getSettings().createCustom(dialogPos.getWidth(), dialogPos.getHeight(), plugin);
        TooltipMakerAPI imagePanelTooltip = bgImagePanel.createUIElement(dialogPos.getWidth(), dialogPos.getHeight(), false);
        plugin.init(imagePanelTooltip, cancelButton);
    
        imagePanelTooltip.addImage(backgroundImagePath, dialogPos.getWidth()-5f, dialogPos.getHeight()-5f, 0f);
        bgImagePanel.addUIElement(imagePanelTooltip);
    
        confirmDialog.addComponent((UIComponentAPI)bgImagePanel).inMid();
        confirmDialog.sendToBottom(bgImagePanel);
        confirmDialog.sendToBottom(imagePanelTooltip);

        return bgImagePanel;
    }

    public static ConfirmDialogData showConfirmationDialog(
        String backgroundImagePath,
        String text,
        String confirmText,
        String cancelText,
        float width,
        float height,
        DialogDismissedListener dialogListener
    ) {
        UIPanelAPI confirmDialog = (UIPanelAPI) createConfirmDialog(text, confirmText, cancelText, width, height, dialogListener);
        utils.confirmDialogShow(confirmDialog, 0.25f, 0.25f);

        LabelAPI label = utils.confirmDialogGetLabel(confirmDialog);
        ButtonAPI yes = utils.confirmDialogGetButton(confirmDialog, 0);
        ButtonAPI no = utils.confirmDialogGetButton(confirmDialog, 1);

        UIPanelAPI innerPanel = utils.confirmDialogGetInnerPanel(confirmDialog);
        
        CustomPanelAPI bgImagePanel = addBackGroundImage(confirmDialog, no, backgroundImagePath);
        innerPanel.bringComponentToTop((UIComponentAPI)label);
        innerPanel.bringComponentToTop((UIComponentAPI)yes);
        innerPanel.bringComponentToTop((UIComponentAPI)no);

        setConfirmDialogButtonInterceptor(no, confirmDialog, bgImagePanel);
        setConfirmDialogButtonInterceptor(yes, confirmDialog, bgImagePanel);
    
        return new ConfirmDialogData(
            label,
            yes,
            no,
            innerPanel,
            confirmDialog
        );
    }

    public static class ConfirmDialogData {
        public LabelAPI textLabel;
        public ButtonAPI confirmButton;
        public ButtonAPI cancelButton;
        public UIPanelAPI panel;
        public UIPanelAPI dialog;

        public ConfirmDialogData(LabelAPI label, ButtonAPI yes, ButtonAPI no, UIPanelAPI panel, UIPanelAPI dialog) {
            this.textLabel = label;
            this.confirmButton = yes;
            this.cancelButton = no;
            this.panel = panel;
            this.dialog = dialog;
        }

        public void addGridLines(float delay, boolean withOverlay, boolean keepConfirmButton, boolean keepCancelButton, Color color, Runnable runAfterDelegate) {
            PositionAPI panelPos = dialog.getPosition();
            float width = panelPos.getWidth();
            float height = panelPos.getHeight();
            int gridWidth = (int) width;
            int gridHeight = (int) height;

            float red = color.getRed() / 255.0f;
            float green = color.getGreen() / 255.0f;
            float blue = color.getBlue() / 255.0f;
        
            CustomPanelAPI gridPanel = Global.getSettings().createCustom(width, height, new BackGroundImagePanelPlugin(panelPos) {
                private float x = panelPos.getX();
                private float y = panelPos.getY();
                private int cellSize = 24;
                private float verticalAlpha = 0.35f;
                private float horizontalAlpha = 0.525f;
        
                @Override
                public void render(float alphaMult) {
                    renderGridWithStencil(gridWidth, gridHeight, x, y);
                }

                private void drawStencilArea(int width, int height, float offsetX, float offsetY) {
                    GL11.glEnable(GL11.GL_STENCIL_TEST);
                    GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
                
                    GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
                    GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
                
                    GL11.glColorMask(false, false, false, false);
                    GL11.glDepthMask(false);
                
                    GL11.glBegin(GL11.GL_QUADS);
                    GL11.glVertex2f(offsetX, offsetY);
                    GL11.glVertex2f(offsetX + width, offsetY);
                    GL11.glVertex2f(offsetX + width, offsetY + height);
                    GL11.glVertex2f(offsetX, offsetY + height);
                    GL11.glEnd();
                
                    GL11.glColorMask(true, true, true, true);
                    GL11.glDepthMask(true);
                
                    GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
                    GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
                }
                
                private void renderGridWithStencil(int width, int height, float offsetX, float offsetY) {
                    drawStencilArea(width, height, offsetX, offsetY);
                    renderGrid(width, height, offsetX, offsetY);
                    GL11.glDisable(GL11.GL_STENCIL_TEST);
                }
        
                private void renderGrid(int width, int height, float offsetX, float offsetY) {
                    GL11.glDisable(GL11.GL_TEXTURE_2D);
                
                    float startX = -cellSize / 2f;
                    float endX = width + cellSize / 2f;
                    float startY = -cellSize / 2f;
                    float endY = height + cellSize / 2f;
                    
                    if (withOverlay) {
                        GL11.glColor4f(0f, 0f, 0f, 0.5f); 
                        GL11.glBegin(GL11.GL_QUADS);
                        GL11.glVertex2f(startX + offsetX, startY + offsetY);
                        GL11.glVertex2f(endX + offsetX, startY + offsetY);
                        GL11.glVertex2f(endX + offsetX, endY + offsetY);
                        GL11.glVertex2f(startX + offsetX, endY + offsetY);
                        GL11.glEnd();
                    }
                
                    GL11.glLineWidth(1f);
                    GL11.glBegin(GL11.GL_LINES);
                
                    GL11.glColor4f(red, green, blue, verticalAlpha);
                    for (float x = startX; x <= endX; x += cellSize) {
                        GL11.glVertex2f(x + offsetX, startY + offsetY);
                        GL11.glVertex2f(x + offsetX, endY + offsetY);
                    }
                
                    GL11.glColor4f(red, green, blue, horizontalAlpha);
                    for (float y = startY; y <= endY; y += cellSize) {
                        GL11.glVertex2f(startX + offsetX, y + offsetY);
                        GL11.glVertex2f(endX + offsetX, y + offsetY);
                    }
                
                    GL11.glEnd();
                }

            }.init(null, cancelButton));

            Global.getSector().addTransientScript(new BaseEveryFrameScript(true) {
                private IntervalUtil interval = new IntervalUtil(delay, delay);

                @Override
                public void advance(float arg0) {
                    interval.advance(arg0);
                    if (!interval.intervalElapsed()) return;
                    dialog.addComponent(gridPanel).inTL(0f, 0f);
                    List<UIComponentAPI> children = utils.getChildrenNonCopy(dialog);
                    children.remove(children.size()-1);
                    children.add(1, gridPanel);
        
                    setButtonInterceptorForGrid(cancelButton, gridPanel);
                    setButtonInterceptorForGrid(confirmButton, gridPanel);
        
                    if (keepConfirmButton) panel.bringComponentToTop(confirmButton);
                    if (keepCancelButton) panel.bringComponentToTop(cancelButton);
                    panel.bringComponentToTop((UIComponentAPI)textLabel);

                    if (runAfterDelegate != null) runAfterDelegate.run();

                    isDone = true;
                    Global.getSector().removeTransientScript(this);
                }
            });
        }

        private void setButtonInterceptorForGrid(ButtonAPI btn, CustomPanelAPI gridPanel) {
            Object oldListener = utils.buttonGetListener(btn);
    
            utils.buttonSetListener(btn, new ActionListener() {
                @Override
                public void actionPerformed(Object arg0, Object arg1) {
                    gridPanel.setOpacity(0f);
                    dialog.removeComponent(gridPanel);
    
                    utils.actionPerformed(oldListener, arg0, arg1);
                }
            }.getProxy());
        }
    }

    public static UIPanelAPI createShipIconListPanel(Color baseColor, List<FleetMemberAPI> members) {
        return createShipIconListPanel(
            7,
            (int) Math.ceil((float) members.size() / 7f),
            56f,
            baseColor,
            members
        );
    }

    public static UIPanelAPI createShipIconListPanel(int cols, int rows, float iconSize, Color baseColor, List<FleetMemberAPI> members) {
        TooltipMakerAPI tt = Global.getSettings().createCustom(0f,0f,null).createUIElement(0f,0f,false);

        tt.addShipList(
            cols,
            rows,
            iconSize,
            baseColor,
            members,
            0f
        );

        List<UIComponentAPI> children = utils.getChildrenNonCopy(utils.getContents(tt));

        UIPanelAPI result = (UIPanelAPI) children.get(children.size()-1);
        utils.getParent(result).removeComponent(result);

        return result;
    }

    public static void setButtonTooltips(UIPanelAPI obfFleetInfoPanel, List<FleetMemberAPI> members) {
        TreeTraverser traverser = new TreeTraverser(obfFleetInfoPanel);

        int i = 0;
        for (TreeNode node : traverser.getNodesAtDepth(7)) {
            for (Object child : node.getChildren()) {
                setButtonTooltip((ButtonAPI)child, members.get(i));
                i++;
            }
        }
    }

    public static void setButtonTooltips(String name, UIPanelAPI obfFleetInfoPanel, Map<Integer, FleetMemberAPI> whichFleetMembersAvailable, List<FleetMemberAPI> allFleetMembers) {
        Map<ButtonAPI, Object> buttonToRenderControllerMap = null;
        TreeTraverser traverser = new TreeTraverser(obfFleetInfoPanel);
        
        int i = 0;
        for (TreeTraverser.TreeNode node : traverser.getNodesAtDepth(7)) {
            for (Object child : node.getChildren()) {
                ButtonAPI btn = (ButtonAPI) child;
                if (i == 0) {
                    buttonToRenderControllerMap = getButtonToRenderControllerMap((UIPanelAPI)utils.buttonGetListener(btn));
                }

                if (whichFleetMembersAvailable.get(i) == null) {
                    btn.setEnabled(false);
                    btn.setOpacity(0.66f);
                    setButtonTooltipWithPostProcessing(btn, allFleetMembers.get(i));
                    setShipButtonHighlightColor(buttonToRenderControllerMap.get(btn), DARK_RED);
                } else {
                    setButtonTooltip(btn, allFleetMembers.get(i));
                }
                
                i++;
            }
        }
    }

    public static void removeTooltipFromButton(ButtonAPI btn) {
        UiUtil.uiComponentTooltipHandle.set(btn, null);
    }

    public static void setButtonTooltip(ButtonAPI btn, FleetMemberAPI member) {
        StandardTooltipV2 tt = createShipButtonTooltip(member);
        utils.setTooltip(btn, 5f, tt);
    }

    public static void setButtonTooltipWithPostProcessing(ButtonAPI btn, FleetMemberAPI member) {
        StandardTooltipV2 tt = createShipButtonTooltip(member);
        utils.setTooltip(btn, 5f, tt);

        tt.setOpacity(0.5f);

        UIPanelAPI ttPa = (UIPanelAPI) tt;

        utils.showTooltip(btn, tt);
        tt.makeNonExpandable();
        float width = ttPa.getPosition().getWidth() - 7f;
        float height = ttPa.getPosition().getHeight() - 1f;
        utils.hideTooltip(btn, tt);

        CustomPanelAPI pane = Global.getSettings().createCustom(width, height, null);
        TooltipMakerAPI ttB = pane.createUIElement(width, height, false);
        ButtonAPI overlay = ttB.addButton("", "", DARK_RED, DARK_RED, Alignment.MID, CutStyle.NONE, width, height, 0f);
        overlay.setOpacity(0.5f);
        overlay.setMouseOverSound(null);
        overlay.setButtonPressedSound(null);

        pane.addUIElement(ttB);
        ttPa.addComponent(pane).inTL(-1f, 1f);
    }

    public static StandardTooltipV2 createShipButtonTooltip(FleetMemberAPI member) {
        return StandardTooltipV2.createFleetMemberExpandedTooltip((FleetMember)member, (CharacterStats)member.getCaptain().getStats());
    }

    public static Map<ButtonAPI, Object> getButtonToRenderControllerMap(UIPanelAPI shipIconListPanel) {
        return (Map<ButtonAPI, Object>) UiUtil.listPanelMapHandle.get(UiUtil.shipIconListListPanelHandle.get(shipIconListPanel));
    }

    public static void setShipButtonHighlightColor(Object renderController, Color colorToSet) {
        UiUtil.shipIconRendererHighlightColorHandle.set(renderController, colorToSet);
    }

    public static UIPanelAPI createConfirmDialog(String title, String confirmText, String cancelText, float width, float height, DialogDismissedListener dialogListener) {
        return instantiator.instantiateConfirmDialog(
            width,
            height,
            utils.campaignUIGetScreenPanel(Global.getSector().getCampaignUI()),
            dialogListener.getProxy(),
            title,
            confirmText, cancelText
        );
    }

    public static class BackGroundImagePanelPlugin extends BaseCustomUIPanelPlugin {
        public TooltipMakerAPI tt;
        public ButtonAPI cancelButton;

        private final float dialogLeftBound;
        private final float dialogRightBound;
        private final float dialogTopBound;
        private final float dialogBottomBound;

        public BackGroundImagePanelPlugin(PositionAPI dialogPos) {
            super();

            this.dialogLeftBound = dialogPos.getCenterX() - dialogPos.getWidth() / 2;
            this.dialogRightBound = dialogPos.getCenterX() + dialogPos.getWidth() / 2;
            this.dialogTopBound = dialogPos.getCenterY() + dialogPos.getHeight() / 2;
            this.dialogBottomBound = dialogPos.getCenterY() - dialogPos.getHeight() / 2;
        }

        public BackGroundImagePanelPlugin init(TooltipMakerAPI tt, ButtonAPI cancelButton) {
            this.tt = tt;
            this.cancelButton = cancelButton;
            return this;
        }

        private boolean isOutsideDialogBounds(float mouseX, float mouseY) {
            return (mouseX < dialogLeftBound || mouseX > dialogRightBound || 
            mouseY < dialogBottomBound || mouseY > dialogTopBound);
        }

        @Override
        public void processInput(List<InputEventAPI> events) {
            for (InputEventAPI event : events) {
                if ((event.isKeyDownEvent() && Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) || (event.isRMBDownEvent() && isOutsideDialogBounds(event.getX(), event.getY()))) {
                    clickButton(cancelButton);
                    event.consume();
                    break;
                }
            }
        }
    }

    public static class HoloNoise {
        private final Object holo;
        private final Object holoNoiseRenderer;

        private final Color originalColor;
        private boolean override = false;
        private Fader fader;

        public HoloNoise(UIPanelAPI dialog) {
            this.holo = utils.confirmDialogGetHolo(dialog);
            this.holoNoiseRenderer = UiUtil.confirmDialogHoloNoiseRendererHandle.get(holo);
            this.originalColor = (Color) UiUtil.confirmDialogHoloNoiseColorHandle.get(holoNoiseRenderer);
            this.fader = (Fader) UiUtil.confirmDialogHoloNoiseFaderHandle.get(holoNoiseRenderer);
        }

        public void setColor(Color color) {
            UiUtil.confirmDialogHoloNoiseColorHandle.set(holoNoiseRenderer, color);
        }
    
        public void resetColor() {
            UiUtil.confirmDialogHoloNoiseColorHandle.set(holoNoiseRenderer, originalColor);
        }

        public boolean isRendering() {
            if (fader.getBrightness() == 0f) return false;
            if (!override) return true;
            return false;
        }

        public void setOverride(boolean override) {
            this.override = override;
        }
    }
}