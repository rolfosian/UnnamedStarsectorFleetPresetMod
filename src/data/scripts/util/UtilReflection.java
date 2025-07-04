// Code taken and modified from Officer Extension mod

package data.scripts.util;

import com.fs.starfarer.D.I.Oo;
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
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.input.InputEventClass;
import com.fs.starfarer.api.input.InputEventType;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.CharacterStats;
import com.fs.starfarer.campaign.fleet.CampaignFleet;
import com.fs.starfarer.campaign.fleet.FleetMember;
import com.fs.starfarer.ui.impl.StandardTooltipV2;

import data.scripts.ClassRefs;
import data.scripts.ui.Button;
import data.scripts.ui.Label;
import data.scripts.ui.UIComponent;
import data.scripts.ui.UIPanel;
import data.scripts.ui.TreeTraverser.TreeNode;
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

    public static void setConfirmDialogButtonInterceptor(Button btn, UIPanelAPI dialog, CustomPanelAPI bgImagePanel) {
        Object oldListener = ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonGetListenerMethod, btn.getInstance());
        ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonSetListenerMethod, btn.getInstance(), new ActionListener() {
            public void trigger(Object arg0, Object arg1) {
                bgImagePanel.setOpacity(0f);
                (((BackGroundImagePanelPlugin)bgImagePanel.getPlugin())).tt.setOpacity(0f);
                bgImagePanel.removeComponent(((BackGroundImagePanelPlugin)bgImagePanel.getPlugin()).tt);
                dialog.removeComponent(bgImagePanel);
                ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonListenerActionPerformedMethod, oldListener, arg0, arg1);
            }
        }.getProxy());
    }

    public static void clickOutsideAbsorb(UIPanelAPI confirmDialog) {
        Object event = createInputEventInstance(
            InputEventClass.MOUSE_EVENT,
            InputEventType.MOUSE_DOWN,
            9999,
            9999,
            0,
            '\0'
        );
        ReflectionUtilis.invokePrivateMethodDirectly(ClassRefs.confirmDialogOutsideClickAbsorbedMethod, confirmDialog, event);
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

    public static CustomPanelAPI addBackGroundImage(UIPanelAPI confirmDialog, String backgroundImagePath) {
        PositionAPI innerPanelPos = confirmDialog.getPosition();

        BackGroundImagePanelPlugin plugin = new BackGroundImagePanelPlugin();
        CustomPanelAPI bgImagePanel = Global.getSettings().createCustom(innerPanelPos.getWidth(), innerPanelPos.getHeight(), plugin);
        TooltipMakerAPI imagePanelTooltip = bgImagePanel.createUIElement(innerPanelPos.getWidth(), innerPanelPos.getHeight(), false);
        plugin.init(imagePanelTooltip);
    
        imagePanelTooltip.addImage(backgroundImagePath, innerPanelPos.getWidth()-5f, innerPanelPos.getHeight()-5f, 0f);
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
        DialogDismissedListener dialogListener) {

    UIPanelAPI confirmDialog = (UIPanelAPI) createConfirmDialog(text, confirmText, cancelText, width, height, dialogListener);
    ReflectionUtilis.invokeMethodDirectly(ClassRefs.confirmDialogShowMethod, confirmDialog, 0.25f, 0.25f);

    LabelAPI label = (LabelAPI) ReflectionUtilis.invokeMethodDirectly(ClassRefs.confirmDialogGetLabelMethod, confirmDialog);
    Button yes = new Button((ButtonAPI) ReflectionUtilis.invokeMethodDirectly(ClassRefs.confirmDialogGetButtonMethod, confirmDialog, 0), null, null);
    Button no = new Button((ButtonAPI) ReflectionUtilis.invokeMethodDirectly(ClassRefs.confirmDialogGetButtonMethod, confirmDialog, 1), null, null);

    UIPanelAPI innerPanel = (UIPanelAPI) ReflectionUtilis.invokeMethodDirectly(ClassRefs.confirmDialogGetInnerPanelMethod, confirmDialog);
    
    CustomPanelAPI bgImagePanel = addBackGroundImage((UIPanelAPI)confirmDialog, backgroundImagePath);
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

    public static void setButtonTooltips(UIPanelAPI obfFleetInfoPanel, List<FleetMemberAPI> members) {
        TreeTraverser traverser = new TreeTraverser(obfFleetInfoPanel);
        int i = 0;
        for (TreeTraverser.TreeNode node : traverser.getNodesAtDepth(7)) {
            for (Object child : node.getChildren()) {
                setButtonTooltip((ButtonAPI)child, members.get(i));
                i++;
            }
        }
    }

    public static void setButtonTooltips(UIPanelAPI obfFleetInfoPanel, Map<Integer, FleetMemberAPI> whichFleetMembersAvailable, List<FleetMemberAPI> allFleetMembers) {
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

    public static void setButtonTooltip(ButtonAPI btn, FleetMemberAPI member) {
        ReflectionUtilis.invokeMethodDirectly(ClassRefs.uiPanelSetTooltipMethod, btn, 5f, createShipButtonTooltip(member));
    }

    public static void setButtonTooltipWithPostProcessing(ButtonAPI btn, FleetMemberAPI member) {
        StandardTooltipV2 tt = createShipButtonTooltip(member);
        ReflectionUtilis.invokeMethodDirectly(ClassRefs.uiPanelsetOpacityMethod, tt, 0.5f);
        ReflectionUtilis.invokeMethodDirectly(ClassRefs.uiPanelSetTooltipMethod, btn, 5f, tt);

        UIPanel ttPa = new UIPanel(tt);

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
        ttPa.getInstance().addComponent(pane).inTL(-1f, 1f);
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

    private static class BackGroundImagePanelPlugin extends BaseCustomUIPanelPlugin {
        public TooltipMakerAPI tt;

        public BackGroundImagePanelPlugin() {
            super();
        }

        public void init(TooltipMakerAPI tt) {
            this.tt = tt;
        }

        
    }
}