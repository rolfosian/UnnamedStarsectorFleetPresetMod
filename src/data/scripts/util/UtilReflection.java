// Code taken and modified from Officer Extension mod

package data.scripts.util;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
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
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.input.InputEventClass;
import com.fs.starfarer.api.input.InputEventType;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.campaign.fleet.CampaignFleet;

import data.scripts.ClassRefs;
import data.scripts.ui.Button;
import data.scripts.ui.Label;
import data.scripts.ui.UIComponent;
import data.scripts.ui.UIPanel;
import data.scripts.ui.TreeTraverser;
import data.scripts.util.ReflectionUtilis.ListenerFactory.ActionListener;
import data.scripts.util.ReflectionUtilis.ListenerFactory.DialogDismissedListener;

import java.util.*;
import java.awt.Color;

import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;

@SuppressWarnings("unchecked")
public class UtilReflection {
    public static final void print(Object... args) {
        PresetMiscUtils.print(args);
    }

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
        return ReflectionUtilis.instantiateClass(ClassRefs.inputEventClass,
        ClassRefs.inputEventClassParamTypes,
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
        val, // keyboard key or mouse button
        char_ // char is only appicable for keyboard keys afaik
        ); 
    }

    public static void clickButton(Object button) {
        if (button == null) return;

        Object listener = ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonGetListenerMethod, button);
        ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonListenerActionPerformedMethod, listener, UtilReflection.createButtonClickEventInstance(((ButtonAPI)button).getPosition()), button);
    }

    public static List<Object> getChildren(Object parent) {
        return (List<Object>) ReflectionUtilis.getMethodAndInvokeDirectly("getChildrenNonCopy", parent, 0);
    }

    public static List<Object> getChildrenRecursive(Object parentPanel) {
        List<Object> list = new ArrayList<>();
        collectChildren(parentPanel, list);
        return list;
    }
    
    private static void collectChildren(Object parent, List<Object> list) {
        List<Object> children = (List<Object>) ReflectionUtilis.getMethodAndInvokeDirectly("getChildrenNonCopy", parent, 0);

        if (children != null) {
            for (Object child : children) {
                list.add(child);
                collectChildren(child, list);
            }
        }
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

    public static void addBackGroundImage(UIPanelAPI innerPanel, String backgroundImagePath) {
        PositionAPI innerPanelPos = innerPanel.getPosition();
        CustomPanelAPI bgImagePanel = Global.getSettings().createCustom(innerPanelPos.getWidth(), innerPanelPos.getHeight(), null);
        TooltipMakerAPI imagePaneltt = bgImagePanel.createUIElement(innerPanelPos.getWidth(), innerPanelPos.getHeight(), false);
    
        imagePaneltt.addImage(backgroundImagePath, innerPanelPos.getWidth()-5f, innerPanelPos.getHeight()-5f, 0f);
        bgImagePanel.addUIElement(imagePaneltt);
    
        innerPanel.addComponent((UIComponentAPI)bgImagePanel).inMid();
        innerPanel.sendToBottom(bgImagePanel);
        innerPanel.sendToBottom(imagePaneltt);
    }

    public static ConfirmDialogData showConfirmationDialog(
        String backgroundImagePath,
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

    UIPanelAPI innerPanel = (UIPanelAPI) ReflectionUtilis.invokeMethodDirectly(ClassRefs.confirmDialogGetInnerPanelMethod, confirmDialog);
    
    addBackGroundImage((UIPanelAPI)confirmDialog, backgroundImagePath);
    innerPanel.bringComponentToTop((UIComponentAPI)label);
    innerPanel.bringComponentToTop((UIComponentAPI)yes.getInstance());
    innerPanel.bringComponentToTop((UIComponentAPI)no.getInstance());
 
    return new ConfirmDialogData(
            label,
            yes,
            no,
            innerPanel,
            (UIPanelAPI) confirmDialog);
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
            textLabel = label;
            confirmButton = yes;
            cancelButton = no;
            this.panel = panel;
            this.dialog = dialog;
        }
    }

    public static UIPanelAPI getObfFleetInfoPanel(String name, CampaignFleetAPI fleet) {
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

    public static void disableUnavailableMemberButtons(UIPanelAPI obfFleetInfoPanel, Map<Integer, FleetMemberAPI> whichFleetMembersAvailable) {
        TreeTraverser traverser = new TreeTraverser(obfFleetInfoPanel);
        int i = 0;
        for (TreeTraverser.TreeNode node : traverser.getNodesAtDepth(7)) {
            for (Object child : node.getChildren()) {
                if (whichFleetMembersAvailable.get(i) == null) {
                    ButtonAPI btn = (ButtonAPI) child;
                    btn.setGlowBrightness(0f);
                    btn.setEnabled(false);
                    btn.setFlashBrightness(0f);
                    btn.setHighlightBrightness(0f);
                    btn.setOpacity(0.5f);
                    btn.setShowTooltipWhileInactive(false);
                    ReflectionUtilis.getMethodAndInvokeDirectly("setActive", btn, 1, false);
                }
                i++;
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
}