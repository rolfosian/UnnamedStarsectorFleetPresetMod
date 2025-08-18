// Code taken and modified from Officer Extension mod
package data.scripts.util;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.VisualPanelAPI;

import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.input.InputEventClass;
import com.fs.starfarer.api.input.InputEventType;

import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

import com.fs.starfarer.campaign.CharacterStats;
import com.fs.starfarer.campaign.fleet.FleetMember;
import com.fs.starfarer.ui.impl.StandardTooltipV2;

import data.scripts.ClassRefs;
import data.scripts.ui.Button;
import data.scripts.ui.Label;
import data.scripts.ui.UIComponent;
import data.scripts.ui.UIPanel;
import data.scripts.ui.TreeTraverser.TreeNode;
import data.scripts.ui.TreeTraverser;
import data.scripts.util.ListenerFactory.ActionListener;
import data.scripts.util.ListenerFactory.DialogDismissedListener;

import java.util.*;
import java.awt.Color;

import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

@SuppressWarnings("unchecked")
public class UtilReflection {
    public static final void print(Object... args) {
        PresetMiscUtils.print(args);
    }
    
    public static final Color DARK_RED = new Color(139, 0, 0);

    public static Button makeButton(String text, ActionListener handler, Color base, Color bg, float width, float height, Object shortcutKey) {
        return makeButton(text, handler, base, bg, Alignment.MID, CutStyle.ALL, width, height, shortcutKey);
    }

    public static Button makeButton(String text, ActionListener handler, Color base, Color bg, Alignment align, CutStyle style, float width, float height, Object shortcutKey) {
        CustomPanelAPI dummyPanel = Global.getSettings().createCustom(0f, 0f, null);
        TooltipMakerAPI dummyTooltipMaker = dummyPanel.createUIElement(0f, 0f, false);
        Button button = new Button(dummyTooltipMaker.addButton(text, null, base, bg, align, style, width, height, 0f), dummyTooltipMaker, dummyPanel);
        button.setListener(handler);

        if (shortcutKey instanceof Integer) {
            button.setShortcut((int)shortcutKey, true);
        }

        return button;
    }

    public static Object createButtonClickEventInstance(PositionAPI buttonPosition) {
        return createInputEventInstance(
        InputEventClass.MOUSE_EVENT,
        InputEventType.MOUSE_DOWN,
        (int)buttonPosition.getCenterX(),
        (int)buttonPosition.getCenterY(),
        0, // LMB
        '\0' // unused?
        );
    }

    public static Object createInputEventInstance(InputEventClass eventClass, InputEventType eventType, int x, int y, int val, char char_) {
        return ReflectionUtilis.instantiateClass(ClassRefs.inputEventClass,
        ClassRefs.inputEventClassParamTypes,
        eventClass,
        eventType,
        x,
        y,
        val, // keyboard key or mouse button, is -1 for mouse move
        char_ // char is only appicable for keyboard keys afaik, give '\0' for mouse prob
        );
    }

    public static void setButtonHook(ButtonAPI button, Runnable runBefore, Runnable runAfter) {
        Object oldListener = ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonGetListenerMethod, button);

        ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonSetListenerMethod, button, new ActionListener() {
            @Override
            public void trigger(Object... args) {
                runBefore.run();
                ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonListenerActionPerformedMethod, oldListener, args);
                runAfter.run();
            }
        }.getProxy());
    }

    public static void clickButton(Object button) {
        if (button == null) return;

        Object listener = ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonGetListenerMethod, button);
        ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonListenerActionPerformedMethod, listener, createButtonClickEventInstance(((ButtonAPI)button).getPosition()), button);
    }

    public static List<Object> getChildren(Object parent) {
        return (List<Object>) ReflectionUtilis.invokeMethodDirectly(ClassRefs.uiPanelgetChildrenNonCopyMethod, parent);
    }

    public static List<Object> getChildrenRecursive(Object parentPanel) {
        List<Object> list = new ArrayList<>();
        collectChildren(parentPanel, list);
        return list;
    }
    
    private static void collectChildren(Object parent, List<Object> list) {
        List<Object> children;

        if (ClassRefs.uiPanelClass.isInstance(parent)) {
            children = (List<Object>) ReflectionUtilis.invokeMethodDirectly(ClassRefs.uiPanelgetChildrenNonCopyMethod, parent);
        } else {
            children = null;
        }

        if (children != null) {
            for (Object child : children) {
                list.add(child);
                collectChildren(child, list);
            }
        }
    }

    public static void setConfirmDialogButtonInterceptor(Button btn, UIPanelAPI dialog, CustomPanelAPI bgImagePanel) {
        Object oldListener = ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonGetListenerMethod, btn.getInstance());

        ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonSetListenerMethod, btn.getInstance(), new ActionListener() {
            public void trigger(Object... args) {
                bgImagePanel.setOpacity(0f);
                (((BackGroundImagePanelPlugin)bgImagePanel.getPlugin())).tt.setOpacity(0f);

                bgImagePanel.removeComponent(((BackGroundImagePanelPlugin)bgImagePanel.getPlugin()).tt);
                dialog.removeComponent(bgImagePanel);

                ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonListenerActionPerformedMethod, oldListener, args);
            }
        }.getProxy());
    }

    public static void clickOutsideAbsorb(UIPanelAPI confirmDialog) {
        ReflectionUtilis.invokePrivateMethodDirectly(ClassRefs.confirmDialogOutsideClickAbsorbedMethod, confirmDialog, createInputEventInstance(
            InputEventClass.MOUSE_EVENT,
            InputEventType.MOUSE_DOWN,
            9999,
            9999,
            0,
            '\0'
        ));
    }

    public static ConfirmDialogData showConfirmationDialog(
            String text,
            String confirmText,
            String cancelText,
            float width,
            float height,
            DialogDismissedListener dialogListener) {

        Object confirmDialog = createConfirmDialog(text, confirmText, cancelText, width, height, dialogListener);
        ReflectionUtilis.invokeMethodDirectly(ClassRefs.confirmDialogShowMethod, confirmDialog, 0.25f, 0.25f);

        LabelAPI label = (LabelAPI) ReflectionUtilis.invokeMethodDirectly(ClassRefs.confirmDialogGetLabelMethod, confirmDialog);
        Button yes = new Button((ButtonAPI) ReflectionUtilis.invokeMethodDirectly(ClassRefs.confirmDialogGetButtonMethod, confirmDialog, 0), null, null);
        Button no = new Button((ButtonAPI) ReflectionUtilis.invokeMethodDirectly(ClassRefs.confirmDialogGetButtonMethod, confirmDialog, 1), null, null);
        return new ConfirmDialogData(
                label,
                yes,
                no,
                (UIPanelAPI) ReflectionUtilis.invokeMethodDirectly(ClassRefs.confirmDialogGetInnerPanelMethod, confirmDialog),
                (UIPanelAPI) confirmDialog);
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
        ReflectionUtilis.invokeMethodDirectly(ClassRefs.confirmDialogShowMethod, confirmDialog, 0.25f, 0.25f);

        LabelAPI label = (LabelAPI) ReflectionUtilis.invokeMethodDirectly(ClassRefs.confirmDialogGetLabelMethod, confirmDialog);
        Button yes = new Button((ButtonAPI) ReflectionUtilis.invokeMethodDirectly(ClassRefs.confirmDialogGetButtonMethod, confirmDialog, 0), null, null);
        Button no = new Button((ButtonAPI) ReflectionUtilis.invokeMethodDirectly(ClassRefs.confirmDialogGetButtonMethod, confirmDialog, 1), null, null);

        UIPanelAPI innerPanel = (UIPanelAPI) ReflectionUtilis.invokeMethodDirectly(ClassRefs.confirmDialogGetInnerPanelMethod, confirmDialog);
        
        CustomPanelAPI bgImagePanel = addBackGroundImage((UIPanelAPI)confirmDialog, no.getInstance(), backgroundImagePath);
        innerPanel.bringComponentToTop((UIComponentAPI)label);
        innerPanel.bringComponentToTop((UIComponentAPI)yes.getInstance());
        innerPanel.bringComponentToTop((UIComponentAPI)no.getInstance());

        setConfirmDialogButtonInterceptor(no, confirmDialog, bgImagePanel);
        setConfirmDialogButtonInterceptor(yes, confirmDialog, bgImagePanel);
    
        return new ConfirmDialogData(
                label,
                yes,
                no,
                innerPanel,
                confirmDialog);
    }

    public static UIPanelAPI getCoreUI() {
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        InteractionDialogAPI dialog = campaignUI.getCurrentInteractionDialog();

        return dialog == null ? (UIPanelAPI) ReflectionUtilis.invokeMethodDirectly(ClassRefs.campaignUIGetCoreMethod, campaignUI) : (UIPanelAPI) ReflectionUtilis.invokeMethodDirectly(ClassRefs.interactionDialogGetCoreUIMethod, dialog);
    }

    public static class ConfirmDialogData {
        public LabelAPI textLabel;
        public Button confirmButton;
        public Button cancelButton;
        public UIPanelAPI panel;
        public UIPanelAPI dialog;

        public ConfirmDialogData(LabelAPI label, Button yes, Button no, UIPanelAPI panel, UIPanelAPI dialog) {
            this.textLabel = label;
            this.confirmButton = yes;
            this.cancelButton = no;
            this.panel = panel;
            this.dialog = dialog;
        }

        public void addGridLines(float delay, boolean withOverlay, boolean keepConfirmButton, boolean keepCancelButton, Color color) {
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

            }.init(null, cancelButton.getInstance()));

            Global.getSector().addTransientScript(new EveryFrameScript() {
                private boolean isDone = false;
                private IntervalUtil interval = new IntervalUtil(delay, delay);

                @Override
                public void advance(float arg0) {
                    interval.advance(arg0);
                    if (!interval.intervalElapsed()) return;
                    dialog.addComponent(gridPanel).inTL(0f, 0f);
                    dialog.sendToBottom(gridPanel);
        
                    setButtonInterceptorForGrid(cancelButton, gridPanel);
                    setButtonInterceptorForGrid(confirmButton, gridPanel);
        
                    if (keepConfirmButton) panel.bringComponentToTop(confirmButton.getInstance());
                    if (keepCancelButton) panel.bringComponentToTop(cancelButton.getInstance());
                    panel.bringComponentToTop((UIComponentAPI)textLabel);

                    isDone = true;
                    Global.getSector().removeTransientScript(this);
                }

                @Override
                public boolean isDone() {
                    return isDone;
                }

                @Override
                public boolean runWhilePaused() {
                    return true;
                }
                
            });
        }

        private void setButtonInterceptorForGrid(Button btn, CustomPanelAPI gridPanel) {
            Object oldListener = ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonGetListenerMethod, btn.getInstance());
    
            ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonSetListenerMethod, btn.getInstance(), new ActionListener() {
                public void trigger(Object... args) {
                    gridPanel.setOpacity(0f);
                    dialog.removeComponent(gridPanel);
    
                    ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonListenerActionPerformedMethod, oldListener, args);
                }
            }.getProxy());
        }
    }

    // This needs to have post processing initialization that a parent class needs to do to add officer portraits and CR/HP bars - grep "addIconFor\(FleetMember" in decompiled obf codebase to find the relevant class
    // These buttons CAN be grafted to a panel and be used just fine but they won't have officer portraits, CR/HP bars, or tooltips
    // Tooltips aren't hard to add but I couldn't easily find out how to do postprocessing for officer portraits in a vacuum
    // What these buttons DO do that the ones processed by the obf fleet info panel class don't are account for their members being mothballed in real time
    // I didnt bother trying to find out why, I decided it wasn't worth the trouble. May revisit this at some stage
    // public static ButtonAPI createFleetMemberButton(FleetMemberAPI member, ActionListener listener) {
    //     return (ButtonAPI) ReflectionUtilis.invokeMethodDirectly(ClassRefs.memberButtonFactoryMethod,
    //     null,
    //     member,
    //     listener.getProxy());
    // }

    public static UIPanelAPI createObfFleetIconPanel(String name, CampaignFleetAPI fleet) {
        return (UIPanelAPI) ReflectionUtilis.instantiateClass(ClassRefs.visualPanelFleetInfoClass,
        ClassRefs.visualPanelFleetInfoClassParamTypes,
        name,
        fleet,
        null,
        null,
        null,
        true
        );
    }

    public static void setButtonTooltips(String name, UIPanelAPI obfFleetInfoPanel, List<FleetMemberAPI> members) {
        TreeTraverser traverser = new TreeTraverser(obfFleetInfoPanel);

        outer:
        for (TreeNode node : traverser.getNodes()) {
            List<LabelAPI> labels = node.getLabels();

            if (labels != null) {
                for (LabelAPI label : labels) {
                    if (label.getText().equals(name)) {
                        label.setHighlightColor(Color.YELLOW);
                        label.setHighlightOnMouseover(true);
                        break outer;
                    }
                }
            }
        }

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
                    buttonToRenderControllerMap = getButtonToRenderControllerMap(ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonGetListenerMethod, btn));
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
        List<Object> vars = ReflectionUtilis.getAllVariables(btn);
        for (int i = 0; i < vars.size(); i++) {
            Object var = vars.get(i);

            if (String.valueOf(var).startsWith("com.fs.starfarer.ui.impl.StandardTooltipV2")) {
                ReflectionUtilis.setFieldAtIndex(btn, i, null);
                break;
            }
        }
    }

    // tooltip parameter needs to be set on button before this is called. natively the expanded tooltip doesnt show the actual CR, but the maximum, so we do this to 'fix' it
    public static StandardTooltipV2 fixCRBar(ButtonAPI btn, FleetMemberAPI member, StandardTooltipV2 tt) {
        ReflectionUtilis.invokeMethodDirectly(ClassRefs.uiPanelShowTooltipMethod, btn, tt);
        Object CRBar = new TreeTraverser(tt, 0, 5, 1).getTargetChild();

        ReflectionUtilis.invokeMethodDirectly(ClassRefs.CRBarClassSetProgressMethod, CRBar, member.getRepairTracker().getCR() * 100f);
        ReflectionUtilis.invokeMethodDirectly(ClassRefs.CRBarClassForceSyncMethod, CRBar);
        UIComponentAPI contents = (UIComponentAPI) tt.getContents();
        PositionAPI contentsPos = contents.getPosition();

        Object panel = ReflectionUtilis.instantiateClass(ClassRefs.uiPanelClass,
            ClassRefs.uiPanelClassConstructorParamTypes,
            contentsPos.getWidth(),
            contentsPos.getHeight()
        );
        ReflectionUtilis.invokeMethodDirectly(ClassRefs.uiPanelHideTooltipMethod, btn, tt);

        StandardTooltipV2 newTt = (StandardTooltipV2) ReflectionUtilis.instantiateClass(StandardTooltipV2.class,
            ClassRefs.standardTooltipV2ConstructorParamTypes,
            contents,
            panel
            );
        newTt.setCodexEntryFleetMember(member);

        return newTt;
    }

    public static void setButtonTooltip(ButtonAPI btn, FleetMemberAPI member) {
        StandardTooltipV2 tt = createShipButtonTooltip(member);
        ReflectionUtilis.invokeMethodDirectly(ClassRefs.uiPanelSetTooltipMethod, btn, 5f, tt);

        if (member.getRepairTracker().getMaxCR() != member.getRepairTracker().getCR()) {
            ReflectionUtilis.invokeMethodDirectly(ClassRefs.uiPanelSetTooltipMethod, btn, 5f, fixCRBar(btn, member, tt));
        }
    }

    public static void setButtonTooltipWithPostProcessing(ButtonAPI btn, FleetMemberAPI member) {
        StandardTooltipV2 tt = createShipButtonTooltip(member);
        ReflectionUtilis.invokeMethodDirectly(ClassRefs.uiPanelSetTooltipMethod, btn, 5f, tt);

        if (member.getRepairTracker().getMaxCR() != member.getRepairTracker().getCR()) {
            tt = fixCRBar(btn, member, tt);
            ReflectionUtilis.invokeMethodDirectly(ClassRefs.uiPanelSetTooltipMethod, btn, 5f, tt);
        }
        ReflectionUtilis.invokeMethodDirectly(ClassRefs.uiPanelsetOpacityMethod, tt, 0.5f);

        UIPanelAPI ttPa = (UIPanelAPI) tt;

        ReflectionUtilis.invokeMethodDirectly(ClassRefs.uiPanelShowTooltipMethod, btn, tt);
        tt.makeNonExpandable();
        float width = ttPa.getPosition().getWidth() - 7f;
        float height = ttPa.getPosition().getHeight() - 1f;
        ReflectionUtilis.invokeMethodDirectly(ClassRefs.uiPanelHideTooltipMethod, btn, tt);

        CustomPanelAPI pane = Global.getSettings().createCustom(width, height, null);
        TooltipMakerAPI ttB = pane.createUIElement(width, height, false);
        ButtonAPI overlay = ttB.addButton("", "", DARK_RED, DARK_RED, Alignment.MID, CutStyle.NONE, width, height, 0f);
        overlay.setOpacity(0.5f);
        overlay.setMouseOverSound(null);
        overlay.setButtonPressedSound(null);

        pane.addUIElement(ttB);
        ttPa.addComponent(pane).inTL(-1f, 1f);
    }

    public static StandardTooltipV2 getButtonTooltip(ButtonAPI btn) {
        return (StandardTooltipV2) ReflectionUtilis.invokeMethodDirectly(ClassRefs.uiPanelGetTooltipMethod, btn);
    }

    public static StandardTooltipV2 createShipButtonTooltip(FleetMemberAPI param) {
        FleetMember member = (FleetMember) param;
        CharacterStats stats = member.getCaptain().getStats();

        return StandardTooltipV2.createFleetMemberExpandedTooltip(member, stats);
    }

    public static Map<ButtonAPI, Object> getButtonToRenderControllerMap(Object listener) {
        for (Object field : listener.getClass().getDeclaredFields()) {
            Class<?> fieldType = ReflectionUtilis.getFieldType(field);

            for (Object nestedField : fieldType.getDeclaredFields()) {
                if (Map.class.equals(ReflectionUtilis.getFieldType(nestedField))) {
                    return (Map<ButtonAPI, Object>) ReflectionUtilis.getPrivateVariable(nestedField, ReflectionUtilis.getPrivateVariable(field, listener));
                }
            }
        }
        return null;
    }

    public static void setShipButtonHighlightColor(Object renderController, Color colorToSet) {
        for (Object field : renderController.getClass().getDeclaredFields()) {
            if (Color.class.isAssignableFrom(ReflectionUtilis.getFieldType(field))) {
                Color color = (Color) ReflectionUtilis.getPrivateVariable(field, renderController);
                if (color == null) continue;

                if (color.getGreen() == 255 && color.getRed() == 84) {
                    ReflectionUtilis.setPrivateVariable(field, renderController, colorToSet);
                    break;
                }
            }
        }
    }

    public static Object createConfirmDialog(String text, String confirmText, String cancelText, float width, float height, DialogDismissedListener dialogListener) {
        return ReflectionUtilis.instantiateClass(
            ClassRefs.confirmDialogClass,
            ClassRefs.confirmDialogClassParamTypes,
            width,
            height,
            ReflectionUtilis.getPrivateVariable("screenPanel", Global.getSector().getCampaignUI()),
            dialogListener.getProxy(),
            text,
            new String[]{confirmText, cancelText}
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

    public static class HoloVar {
        private final Object masterVar;
        private final List<Object> vars;
        private final List<Object> floatGetters;

        private Color originalColor;
        private int colorIndex;
        private boolean override = false;

        public HoloVar(UIPanelAPI dialog) {
            this.masterVar = getHoloVar(dialog);
            this.vars = ReflectionUtilis.getAllVariables(masterVar);
            this.floatGetters = getFloatMethods();

            for (int i = 0; i < vars.size(); i++) {
                Object var = vars.get(i);
                if (Color.class.equals(var.getClass())) {
                    Color varColor = (Color) var;
                    if (varColor.getRed() == 40) {
                        this.colorIndex = i;
                        this.originalColor = varColor;
                        break;
                    }
                }
            }
        }

        public void setColor(Color color) {
            ReflectionUtilis.setFieldAtIndex(masterVar, colorIndex, color);
        }
    
        public void resetColor() {
            ReflectionUtilis.setFieldAtIndex(masterVar, colorIndex, originalColor);
        }

        public boolean isRendering() {
            for (Object method : floatGetters) {
                float val = (float)ReflectionUtilis.invokeMethodDirectly(method, masterVar);
                if (val == 0f) {
                    return false;
                }
            }
            if (!override) {
                return true;
            }
            return false;
        }

        public void setOverride(boolean override) {
            this.override = override;
        }

        private Object getHoloVar(UIPanelAPI dialog) {
            Object holo = ReflectionUtilis.invokeMethodDirectly(ClassRefs.confirmDialogGetHoloMethod, dialog);
            for (Object variable : ReflectionUtilis.getAllVariables(holo)) {
                if (variable != null && !ReflectionUtilis.isNativeJavaClass(variable.getClass())) {
    
                    List<Class<?>> clses = new ArrayList<>();
                    for (Object nestedVariable : ReflectionUtilis.getAllVariables(variable)) {
                        if (nestedVariable != null) {
                            clses.add(nestedVariable.getClass());
                        }
                    }
                    int floatCount = 0;
                    int colorCount = 0;
                    int longCount = 0;
    
                    for (Class<?> cls : clses) {
                        if (cls == Float.class) floatCount++;
                        else if (cls == java.awt.Color.class) colorCount++;
                        else if (cls == Long.class) longCount++;
                    }
    
                    if (longCount == 1 && floatCount == 2 && colorCount == 2) {
                        return variable;
                    }
                }
            }
            return null;
        }

        private List<Object> getFloatMethods() {
            return ReflectionUtilis.getMethodsByReturnType(masterVar.getClass(), float.class, 0);
        }
    }
}