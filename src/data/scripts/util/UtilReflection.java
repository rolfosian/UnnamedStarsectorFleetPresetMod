// Code taken and modified from Officer Extension mod

package data.scripts.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.input.InputEventClass;
import com.fs.starfarer.api.input.InputEventType;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.campaign.fleet.CampaignFleet;

import data.scripts.ClassRefs;
import data.scripts.ui.Button;
import data.scripts.util.ReflectionUtilis.ListenerFactory.ActionListener;
import data.scripts.util.ReflectionUtilis.ListenerFactory.DialogDismissedListener;

import java.util.*;
import java.awt.Color;

import org.apache.log4j.Logger;

@SuppressWarnings("unchecked")
public class UtilReflection {
    public static final Logger logger = Logger.getLogger(UtilReflection.class);
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
        new Object[] {
            InputEventClass.MOUSE_EVENT,
            InputEventType.MOUSE_DOWN,
            (int)buttonPosition.getCenterX(),
            (int)buttonPosition.getCenterY(),
            0, // LMB
            '\0' // unused?
        }); 
    }

    public static Object createInputEventInstance(InputEventClass eventClass, InputEventType eventType, int x, int y, int val, char char_) {
        return ReflectionUtilis.instantiateClass(ClassRefs.inputEventClass,
        ClassRefs.inputEventClassParamTypes,
        new Object[] {
            eventClass,
            eventType,
            x,
            y,
            0, // LMB
            '\0' // unused?
        }); 
    }

    public static void clickButton(Object button) {
        if (button == null) return;

        Object listener = ReflectionUtilis.getMethodAndInvokeDirectly("getListener", button, 0);
        ReflectionUtilis.getMethodAndInvokeDirectly("actionPerformed", listener, 2, UtilReflection.createButtonClickEventInstance(((ButtonAPI)button).getPosition()), button);
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
        try {
            Object confirmDialog = ReflectionUtilis.instantiateClass(
                ClassRefs.confirmDialogClass,
                ClassRefs.confirmDialogClassParamTypes,
                new Object[] {
                    width,
                    height,
                    getField(Global.getSector().getCampaignUI(), "screenPanel"),
                    dialogListener.getProxy(),
                    text,
                    new String[]{confirmText, cancelText}
                }
            );
            
            ReflectionUtilis.getMethodAndInvokeDirectly("show", confirmDialog, 2, 0.25f, 0.25f);

            LabelAPI label = (LabelAPI) ReflectionUtilis.getMethodAndInvokeDirectly("getLabel", confirmDialog, 0);
            Button yes = new Button((ButtonAPI) ReflectionUtilis.getMethodAndInvokeDirectly("getButton", confirmDialog, 1, 0), null, null);
            Button no = new Button((ButtonAPI) ReflectionUtilis.getMethodAndInvokeDirectly("getButton", confirmDialog, 1, 1), null, null);
            return new ConfirmDialogData(
                    label,
                    yes,
                    no,
                    (UIPanelAPI) ReflectionUtilis.getMethodAndInvokeDirectly("getInnerPanel", confirmDialog, 0),
                    (UIPanelAPI) confirmDialog);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static UIPanelAPI getCoreUI() {
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        InteractionDialogAPI dialog = campaignUI.getCurrentInteractionDialog();

        CoreUIAPI core;
        if (dialog == null) {
            core = (CoreUIAPI) ReflectionUtilis.getPrivateVariable("core", campaignUI);
        }
        else {
            core = (CoreUIAPI) ReflectionUtilis.getMethodAndInvokeDirectly("getCoreUI", dialog, 0);
        }
        return core == null ? null : (UIPanelAPI) core;
    }

    public static Object getField(Object o, String fieldName) {
        if (o == null) {
            return null;
        }
        return getFieldExplicitClass(o.getClass(), o, fieldName);
    }

    public static Object getFieldExplicitClass(Class<?> cls, Object o, String fieldName) {
        if (o == null) return null;
        try {
            return ReflectionUtilis.getPrivateVariable(fieldName, o);
        } catch (Exception e) {
            logger.info("Exception for getFieldExplicitClass", e);
            e.printStackTrace();
            return null;
        }
    }

    public static void setField(Object o, String fieldName, Object to) {
        setFieldExplicitClass(o.getClass(), o, fieldName, to);
    }

    public static void setFieldExplicitClass(Class<?> cls, Object o, String fieldName, Object to) {
        if (o == null) return;
        try {
            Object field = cls.getDeclaredField(fieldName);
            ReflectionUtilis.setPrivateVariable(field, o, to);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Object invokeGetter(Object o, String methodName, Object... args) {
        if (o == null) return null;
        try {
            Class<?>[] argClasses = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                argClasses[i] = args[i].getClass();
                // unbox
                if (argClasses[i] == Integer.class) {
                    argClasses[i] = int.class;
                }
                else if (argClasses[i] == Boolean.class) {
                    argClasses[i] = boolean.class;
                }
                else if (argClasses[i] == Float.class) {
                    argClasses[i] = float.class;
                }
            }
            return ReflectionUtilis.getMethodAndInvokeDirectly(methodName, o, args.length, args);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
}