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
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.input.InputEventClass;
import com.fs.starfarer.api.input.InputEventType;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.campaign.fleet.CampaignFleet;

import data.scripts.ClassRefs;
import data.scripts.ui.Button;
import data.scripts.ui.Label;
import data.scripts.ui.UIPanel;
import data.scripts.util.ReflectionUtilis.ListenerFactory.ActionListener;
import data.scripts.util.ReflectionUtilis.ListenerFactory.DialogDismissedListener;

import java.util.*;
import java.awt.Color;

import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;

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
            val, // keyboard key or mouse button
            char_ // char is only appicable for keyboard keys afaik
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

    public abstract static class OptionPanelListener {
        private InteractionDialogAPI dialog;
        private OptionPanelAPI optionPanel;
        private VisualPanelAPI visualPanel;
        private OptionPanelListener self;
        private InteractionDialogPlugin plugin;
        private Map<Object, Object> buttonsToItemsMap;
        private String currentOption;

        private Set<Object> currentOptions = new HashSet<>();
        private Set<Object> currentButtons = new HashSet<>();
        private Set<Object> currentConfirmButtons = new HashSet<>();

        public OptionPanelListener(InteractionDialogAPI dialog) {
            this.dialog = dialog;
            this.optionPanel = dialog.getOptionPanel();
            this.visualPanel = dialog.getVisualPanel();
            this.plugin = dialog.getPlugin();
            this.buttonsToItemsMap = (Map<Object, Object>) ReflectionUtilis.getMethodAndInvokeDirectly("getButtonToItemMap", optionPanel, 0);
            this.self = this;
            populateOptions();
        }

        private void reinit(InteractionDialogAPI dialog_) {
            this.dialog = dialog_;
            this.optionPanel = dialog_.getOptionPanel();
            this.visualPanel = dialog_.getVisualPanel();
            this.plugin = dialog_.getPlugin();
            this.buttonsToItemsMap = (Map<Object, Object>) ReflectionUtilis.getMethodAndInvokeDirectly("getButtonToItemMap", optionPanel, 0);
            currentOption = null;
            currentOptions.clear();
            currentButtons.clear();
            currentConfirmButtons.clear();
            populateOptions();
        }

        private void populateOptions() {
            InteractionDialogAPI dialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
            if (dialog == null) return;
            if (dialog != this.dialog || this.optionPanel != dialog.getOptionPanel() || this.visualPanel != dialog.getVisualPanel() || this.plugin != dialog.getPlugin()) {
                reinit(dialog);
                return;
            }

            if (!this.optionPanel.hasOptions()) {
                if (!currentOption.equals("marketOpenCoreUI")) currentOptions.clear();

                Global.getSector().addTransientScript(new EveryFrameScript() {
                    boolean isDone = false;

                    @Override
                    public void advance(float arg0) {
                        if (optionPanel.hasOptions()) {
                            isDone = true;
                            Global.getSector().removeScript(this);
                            populateOptions();
                        }
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
                return;
            }
            Set<Object> newOptions = new HashSet<>();
            Set<Object> newButtons = new HashSet<>();

            for (Map.Entry<Object, Object> entry : buttonsToItemsMap.entrySet()) {
                newButtons.add(entry.getKey());

                for (Object field : entry.getValue().getClass().getDeclaredFields()) {
                    Object val = ReflectionUtilis.getPrivateVariable(field, entry.getValue());

                    if (val != null && ReflectionUtilis.getFieldType(field).equals(Object.class)) {
                        // val is Option 'data'
                        newOptions.add(val);
                        if (currentOptions.contains(val)) {
                            break;
                        } 
                        currentOptions.add(val);
                        currentButtons.add(entry.getKey());

                        Object oldListener = ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonGetListenerMethod, entry.getKey());
                        ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonSetListenerMethod, entry.getKey(), new ReflectionUtilis.ListenerFactory.ActionListener() {
                            @Override
                            public void trigger(Object arg0, Object arg1) {
                                if (arg1 == entry.getKey()) {
                                    ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonListenerActionPerformedMethod, oldListener, arg0, arg1);

                                    if (optionPanel.optionHasConfirmDelegate(val)) {
                                        // option (usually) opens a confirm dialog, but not in the case of CONTINUE_INTO_BATTLE for example

                                        List<Object> children = (List<Object>) ReflectionUtilis.invokeMethodDirectly(ClassRefs.visualPanelGetChildrenNonCopyMethod, visualPanel);
                                        Object child = children.get(children.size()-1); // the confirm dialog afaik
                                        // (ButtonAPI) ReflectionUtilis.getMethodAndInvokeDirectly("getButton", child, 1, 0), // Yes
                                        // (ButtonAPI) ReflectionUtilis.getMethodAndInvokeDirectly("getButton", child, 1, 1) // No

                                        // Yes button
                                        Object yesButton = ReflectionUtilis.getMethodAndInvokeDirectly("getButton", child, 1, 0);
                                        if (yesButton != null) {
                                            setConfirmListener(yesButton, val);

                                        } else {
                                            Object innerPanel = ReflectionUtilis.getMethodAndInvokeDirectly("getInnerPanel", child, 0);
                                            if (innerPanel == null) {
                                                execute(val);
                                                if (String.valueOf(val).equals("CONTINUE_INTO_BATTLE")) {
                                                    onPlayerEnterBattle();
                                                    return;
                                                }

                                                populateOptions();
                                                return;
                                            }

                                            List<Object> innerChildren = (List<Object>) ReflectionUtilis.getMethodAndInvokeDirectly("getChildrenNonCopy", innerPanel, 0);
                                            if (innerChildren != null) {
                                                boolean yesButtonSet = false;

                                                for (Object child_ : innerChildren) {
                                                    if (ButtonAPI.class.isAssignableFrom(child_.getClass()) && !currentConfirmButtons.contains(child)) {
                                                        String buttonText = ((ButtonAPI) child_).getText().toLowerCase();
    
                                                        if (buttonText.contains("ok") || buttonText.contains("confirm") || buttonText.contains("yes") || buttonText.contains("proceed")) {
                                                            setConfirmListener(child_, val);
                                                            yesButtonSet = true;
                                                            currentConfirmButtons.add(child_);
                                                            break;
                                                        }
                                                    }
                                                }

                                                if (!yesButtonSet) { // for confirmation with only dismiss button such as marketCommDir
                                                    execute(val);
                                                    populateOptions();
                                                    return;
                                                }

                                                // fallback, but no buttons found?
                                            } else {
                                                execute(val);
                                                populateOptions();
                                                return;
                                            }
                                        }
                                        // For the no button - we dont need this because natively the game does not call optionSelected when this is pressed
                                    } else {
                                        execute(val);
                                        populateOptions();
                                        return;
                                    }
                                }
                            }
                        }.getProxy());
                        break;
                    }
                }
            }
            if (!currentOptions.equals(newOptions) || !currentButtons.equals(newButtons) || (currentOptions.equals(newOptions) && currentButtons.equals(newButtons))) {
                currentOptions.clear();
                currentButtons.clear();
                currentConfirmButtons.clear();

            } else {

            }
        }

        private void execute(Object optionData) {
            currentOption = String.valueOf(optionData);
            onOptionSelected(optionData);
        }

        public abstract void onOptionSelected(Object optionData);

        private void onPlayerEnterBattle() {
            Global.getSector().addTransientListener(new BaseCampaignEventListener(false) {
                @Override
                public void reportPlayerEngagement(EngagementResultAPI e) {
                    currentOptions.clear();
                    currentButtons.clear();
                    currentConfirmButtons.clear();
                    Global.getSector().removeListener(this);

                    // i dont know why we have to do this you would think you could just put the advance logic up here and it would be fine but no it gives the pre combat map for some reason if we dont do this
                    Global.getSector().addTransientScript(new EveryFrameScript() {
                        boolean isDone = false;
                        @Override
                        public boolean isDone() {
                            return isDone;
                        }

                        @Override
                        public boolean runWhilePaused() {
                            return true;
                        }

                        @Override
                        public void advance(float amount) {
                            self.buttonsToItemsMap = (Map<Object, Object>)ReflectionUtilis.getMethodAndInvokeDirectly("getButtonToItemMap", optionPanel, 0);
                            populateOptions();
                            isDone = true;
                        }
                    });
                    
                }
            });
        }

        private void setConfirmListener(Object button, Object optionData) {
            Object oldListener = ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonGetListenerMethod, button);
            ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonSetListenerMethod, button, new ReflectionUtilis.ListenerFactory.ActionListener() {
                @Override
                public void trigger(Object arg0, Object arg1) {
                    if (arg1 == button) {
                        if (String.valueOf(optionData) == "CONTINUE_INTO_BATTLE") onPlayerEnterBattle();
                        ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonListenerActionPerformedMethod, oldListener, 2, arg0, arg1);
                        execute(optionData);
                        populateOptions();
                    }
                }
            }.getProxy());
        }
    }
}