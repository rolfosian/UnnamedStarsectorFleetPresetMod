// credit for most of this goes to the author of the code in the officer extension mod

package data.scripts.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.ui.*;

import data.scripts.ClassRefs;
import data.scripts.listeners.ActionListener;
import data.scripts.listeners.DialogDismissedListener;
import data.scripts.ui.Button;

import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandle;

import org.apache.log4j.Logger;

public class UtilReflection {
    public static final Logger logger = Logger.getLogger(UtilReflection.class);

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

    public static ConfirmDialogData showConfirmationDialog(
            String text,
            String confirmText,
            String cancelText,
            float width,
            float height,
            DialogDismissedListener dialogListener) {
        try {
            Constructor<?> cons = ClassRefs.confirmDialogClass
                    .getConstructor(
                            float.class,
                            float.class,
                            ClassRefs.uiPanelClass,
                            ClassRefs.dialogDismissedInterface,
                            String.class,
                            String[].class);
            Object confirmDialog = cons.newInstance(
                    width,
                    height,
                    getField(Global.getSector().getCampaignUI(), "screenPanel"),
                    dialogListener.getProxy(),
                    text,
                    new String[]{confirmText, cancelText}
            );
            Method show = confirmDialog.getClass().getMethod("show", float.class, float.class);
            show.invoke(confirmDialog, 0.25f, 0.25f);
            LabelAPI label = (LabelAPI) invokeGetter(confirmDialog, "getLabel");
            Button yes = new Button((ButtonAPI) invokeGetter(confirmDialog, "getButton", 0), null, null);
            Button no = new Button((ButtonAPI) invokeGetter(confirmDialog, "getButton", 1), null, null);
            return new ConfirmDialogData(
                    label,
                    yes,
                    no,
                    (UIPanelAPI) invokeGetter(confirmDialog, "getInnerPanel"),
                    (UIPanelAPI) confirmDialog);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static UIPanelAPI getCoreUI() {
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        InteractionDialogAPI dialog = campaignUI.getCurrentInteractionDialog();

        CoreUIAPI core;
        if (dialog == null) {
            core = (CoreUIAPI) UtilReflection.getField(campaignUI, "core");
        }
        else {
            core = (CoreUIAPI) UtilReflection.invokeGetter(dialog, "getCoreUI");
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
            Field field = cls.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(o);
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
            Field field = cls.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(o, to);
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
            Method method = o.getClass().getMethod(methodName, argClasses);
            return method.invoke(o, args);
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

    public static <T> T instantiateClassNoParams(Class<T> cls) throws NoSuchMethodException, IllegalAccessException {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle mh = lookup.findConstructor(cls, MethodType.methodType(void.class));
        try {
            //noinspection unchecked
            return (T) mh.invoke();
        }
        catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}