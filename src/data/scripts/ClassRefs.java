// Code taken and modified from Officer Extension mod
package data.scripts;


import com.fs.starfarer.api.Global;

import com.fs.starfarer.campaign.fleet.CampaignFleet;
import com.fs.starfarer.ui.newui.FleetMemberRecoveryDialog;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.api.campaign.FleetMemberPickerListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.VisualPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.input.InputEventClass;
import com.fs.starfarer.api.input.InputEventType;

import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import data.scripts.ui.TreeTraverser;
import data.scripts.ui.TreeTraverser.TreeNode;
import data.scripts.ui.UIComponent;
import data.scripts.ui.UIPanel;
import data.scripts.util.PresetMiscUtils;
import data.scripts.util.PresetUtils;
import data.scripts.util.ReflectionUtilis;
import data.scripts.util.UtilReflection;

import java.util.*;

import org.apache.log4j.Logger;

/** Stores references to class objects in the obfuscated game files */
@SuppressWarnings("unchecked")
public class ClassRefs {
    private static final Logger logger = Logger.getLogger(ClassRefs.class);
    public static void print(Object... args) {
        PresetMiscUtils.print(args);
    }

    /** The class that CampaignUIAPI.showConfirmDialog instantiates. We need this because showConfirmDialog doesn't work
     *  if any core UI is open. */
    public static Class<?> confirmDialogClass;
    public static Class<?>[] confirmDialogClassParamTypes;
    public static Object confirmDialogGetButtonMethod;
    public static Object confirmDialogGetInnerPanelMethod;
    public static Object confirmDialogShowMethod;
    public static Object confirmDialogGetLabelMethod;
    public static Object confirmDialogSetBackgroundDimAmountMethod;
    public static Object confirmDialogOutsideClickAbsorbedMethod;

    /** Interface that contains a single method: actionPerformed */
    public static Class<?> actionListenerInterface;
    /** Interface that contains a single method: dialogDismissed */
    public static Class<?> dialogDismissedInterface;

    /** Interface for renderable UI elements */
    public static Class<?> renderableUIElementInterface;
    public static Object renderableSetOpacityMethod;

    /** Obfuscated UI panel class */
    public static Class<?> uiPanelClass;
    public static Object uiPanelsetParentMethod;
    public static Object uiPanelsetOpacityMethod;
    public static Object uiPanelgetChildrenNonCopyMethod;
    public static Object uiPanelgetChildrenCopyMethod;
    public static Object uiPanelShowTooltipMethod;
    public static Object uiPanelHideTooltipMethod;
    public static Object uiPanelSetTooltipMethod;
    public static Object uiPanelGetTooltipMethod;
    public static Object uiPanelAddMethod;
    public static Object uiPanelRemoveMethod;
    public static Object positionSetMethod;

    /** Obfuscated fleet info panel class from the VisualPanelAPI */
    public static Class<?> visualPanelFleetInfoClass; 
    public static Class<?>[] visualPanelFleetInfoClassParamTypes = new Class<?>[] {
        String.class, // fleet 1 name
        CampaignFleet.class, // fleet 1
        String.class, // fleet 2 name
        CampaignFleet.class, // fleet 2
        FleetEncounterContextPlugin.class,
        boolean.class // is before or after engagement? idk
    };
    public static Object visualPanelGetChildrenNonCopyMethod;
    public static Object optionPanelGetButtonToItemMapMethod;
    public static Object interactionDialogGetCoreUIMethod;
    public static float FMRDialogWidth;
    public static float FMRDialogHeight;
    public static float FMRDialogPanelWidth;
    public static float FMRDialogPanelHeight;

    /** Obfuscated ButtonAPI class */
    public static Class<?> buttonClass;
    public static Object buttonListenerActionPerformedMethod;
    public static Object buttonGetListenerMethod;
    public static Object buttonSetListenerMethod;
    public static Object buttonSetEnabledMethod;
    public static Object buttonSetShortcutMethod;
    public static Object buttonSetButtonPressedSoundMethod;
    public static Object buttonSetActiveMethod;

    public static Object tablePanelsetItemsSelectableMethod;
    public static Object tablePanelSelectMethod;

    public static Object campaignUIGetCoreMethod;

    /** Obfuscated InputEvent class */
    public static Class<?> inputEventClass;
    public static Class<?>[] inputEventClassParamTypes = new Class<?>[] {
        InputEventClass.class, // mouse or keyboard
        InputEventType.class, // type of input
        int.class, // x
        int.class, // y
        int.class, // key/mouse button, is -1 for mouse move
        char.class // unused for mouse afaik
    };

    private static boolean foundAllClasses = false;

    public static void findFleetInfoClass() {
        if (visualPanelFleetInfoClass != null) return;

        Global.getSector().addTransientListener(new BaseCampaignEventListener(false) {
            public void reportShownInteractionDialog(InteractionDialogAPI dialog) {
                if (visualPanelFleetInfoClass != null) {
                    Global.getSector().removeListener(this);
                    dialog.dismiss();
                    return;
                }
                if (!(dialog.getPlugin() instanceof DummyInteractionDialogPlugin)) return;
                interactionDialogGetCoreUIMethod = ReflectionUtilis.getMethod("getCoreUI", dialog, 0);

                VisualPanelAPI visualPanel = dialog.getVisualPanel();
                visualPanel.showFleetInfo("", Global.getSector().getPlayerFleet(), null, null);
                visualPanelGetChildrenNonCopyMethod = ReflectionUtilis.getMethod("getChildrenNonCopy", visualPanel, 0);
                optionPanelGetButtonToItemMapMethod = ReflectionUtilis.getMethod("getButtonToItemMap", dialog.getOptionPanel(), 0);
        
                for (Object child : (List<Object>) ReflectionUtilis.invokeMethodDirectly(visualPanelGetChildrenNonCopyMethod, visualPanel)) {
                    if (UIPanelAPI.class.isAssignableFrom(child.getClass()) && ReflectionUtilis.doInstantiationParamsMatch(child.getClass(), visualPanelFleetInfoClassParamTypes)) {
                        visualPanelFleetInfoClass = child.getClass(); // found it
                        break;
                    }
                }

                dialog.showFleetMemberRecoveryDialog("", getDummyMembers(), new FleetMemberPickerListener() {
                    @Override public void cancelledFleetMemberPicking() {}
                    @Override public void pickedFleetMembers(List<FleetMemberAPI> arg0) {}});

                for (Object child : (List<Object>) ReflectionUtilis.invokeMethodDirectly(ClassRefs.visualPanelGetChildrenNonCopyMethod, dialog.getVisualPanel())) {
                    if (child instanceof FleetMemberRecoveryDialog) {
                        FleetMemberRecoveryDialog FMRDialog = (FleetMemberRecoveryDialog) child;

                        FMRDialogPanelWidth = FMRDialog.getInnerPanel().getPosition().getWidth();
                        FMRDialogPanelHeight = FMRDialog.getInnerPanel().getPosition().getHeight();
                        FMRDialogWidth = FMRDialog.getWidth();
                        FMRDialogHeight = FMRDialog.getHeight();

                        // TreeTraverser traverser = new TreeTraverser(FMRDialog.getInnerPanel());
                        // for (TreeNode node : traverser.getNodes()) {
                        //     List<LabelAPI> labels = node.getLabels();
                        //     List<ButtonAPI> buttons = node.getButtons();


                        //     if (labels != null) {
                        //         for (LabelAPI label : labels) {
                        //             if (label.getText().equals("")) {
                        //
                        //             }
                        //         }
                        //         break;
                        //     }

                        // }

                        break;
                    }
                }
                Global.getSector().removeListener(this);
                dialog.dismiss();
            }
        });

        Global.getSector().getCampaignUI().showInteractionDialogFromCargo(new DummyInteractionDialogPlugin(), null, new CampaignUIAPI.DismissDialogDelegate() {
            public void dialogDismissed() {}
        });
    }

    public static void findTablePanelMethods() {
        CustomPanelAPI panel = Global.getSettings().createCustom(0f, 0f, null);
        TooltipMakerAPI tt = panel.createUIElement(0f, 0f, false);
        Object tablePanel = tt.beginTable(Global.getSettings().getBasePlayerColor(), Global.getSettings().getBasePlayerColor(), Global.getSettings().getBasePlayerColor(), 1f, false, false, new Object[]{"", 1f});
        tablePanelsetItemsSelectableMethod = ReflectionUtilis.getMethod("setItemsSelectable", tablePanel, 1);
        tablePanelSelectMethod = ReflectionUtilis.getMethod("select", tablePanel, 2);
    }

    public static void findInputEventClass() {
        UIPanelAPI coreUI = UtilReflection.getCoreUI();
        if (coreUI == null) return;

        for (Object child : (List<Object>) ReflectionUtilis.getMethodAndInvokeDirectly("getChildrenNonCopy", coreUI, 0)) {
            if (ButtonAPI.class.isAssignableFrom(child.getClass()) && !child.getClass().getSimpleName().equals("ButtonAPI")) {

                for (Object method : child.getClass().getDeclaredMethods()) {
                    if (ReflectionUtilis.getMethodName(method).equals("buttonPressed")) {
                        for (Class<?> paramType : ReflectionUtilis.getMethodParamTypes(method)) {
                            if (ReflectionUtilis.doInstantiationParamsMatch(paramType, inputEventClassParamTypes)) {
                                inputEventClass = paramType;
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    public static void findButtonClass() {
        UIPanelAPI coreUI = UtilReflection.getCoreUI();
        if (coreUI == null) return;
        for (Object child : (List<Object>) ReflectionUtilis.getMethodAndInvokeDirectly("getChildrenNonCopy", coreUI, 0)) {
            if (ButtonAPI.class.isAssignableFrom(child.getClass()) && !child.getClass().getSimpleName().equals("ButtonAPI")) {
                buttonClass = child.getClass();
                buttonGetListenerMethod = ReflectionUtilis.getMethod("getListener", buttonClass, 0);
                buttonSetListenerMethod = ReflectionUtilis.getMethod("setListener", buttonClass, 1);
                buttonSetEnabledMethod = ReflectionUtilis.getMethod("setEnabled", buttonClass, 1);
                buttonSetShortcutMethod = ReflectionUtilis.getMethodExplicit("setShortcut", buttonClass, new Class<?>[]{int.class, boolean.class});
                buttonSetButtonPressedSoundMethod = ReflectionUtilis.getMethod("setButtonPressedSound", buttonClass, 1);
                buttonSetActiveMethod = ReflectionUtilis.getMethod("setActive", buttonClass, 1);
                return;
            }
        }
    }

    public static void findConfirmDialogClass() {
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();

        // If we don't know the confirmation dialog class, try to create a confirmation dialog in order to access it
        boolean isPaused = Global.getSector().isPaused();
        if (confirmDialogClass == null && campaignUI.showConfirmDialog("", "", "", null, null)) {
            Object screenPanel = ReflectionUtilis.getPrivateVariable("screenPanel", campaignUI);
            List<Object> children = (List<Object>) ReflectionUtilis.getMethodAndInvokeDirectly("getChildrenNonCopy", screenPanel, 0);
            // the confirm dialog will be the last child
            Object panel = children.get(children.size() - 1);

            confirmDialogClass = panel.getClass();
            confirmDialogGetButtonMethod = ReflectionUtilis.getMethod("getButton", panel, 1);
            confirmDialogGetInnerPanelMethod = ReflectionUtilis.getMethod("getInnerPanel", panel, 0);
            confirmDialogShowMethod = ReflectionUtilis.getMethod("show", panel, 2);
            confirmDialogGetLabelMethod = ReflectionUtilis.getMethod("getLabel", panel, 0);
            confirmDialogSetBackgroundDimAmountMethod = ReflectionUtilis.getMethod("setBackgroundDimAmount", confirmDialogClass, 1);
            confirmDialogOutsideClickAbsorbedMethod = ReflectionUtilis.getMethodDeclared("outsideClickAbsorbed", confirmDialogClass, 1);

            // we have the class, dismiss the dialog

            ReflectionUtilis.getMethodAndInvokeDirectly("dismiss", panel, 1, 0);
            Global.getSector().setPaused(isPaused);
        }
    }

    public static void findUIPanelClass() {
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        try {
            Object field = campaignUI.getClass().getDeclaredField("screenPanel");
            uiPanelClass = ReflectionUtilis.getFieldType(field);

            uiPanelsetParentMethod = ReflectionUtilis.getMethod("setParent", uiPanelClass, 1);
            uiPanelsetOpacityMethod = ReflectionUtilis.getMethod("setOpacity", uiPanelClass, 1);
            uiPanelgetChildrenNonCopyMethod = ReflectionUtilis.getMethod("getChildrenNonCopy", uiPanelClass, 0);
            uiPanelgetChildrenCopyMethod = ReflectionUtilis.getMethod("getChildrenCopy", uiPanelClass, 0);
            uiPanelShowTooltipMethod = ReflectionUtilis.getMethod("showTooltip", uiPanelClass, 1);
            uiPanelHideTooltipMethod = ReflectionUtilis.getMethod("hideTooltip", uiPanelClass, 1);
            uiPanelSetTooltipMethod = ReflectionUtilis.getMethod("setTooltip", uiPanelClass, 2);
            uiPanelGetTooltipMethod = ReflectionUtilis.getMethod("getTooltip", uiPanelClass, 0);
            uiPanelAddMethod = ReflectionUtilis.getMethodExplicit("add", uiPanelClass, new Class<?>[]{ClassRefs.renderableUIElementInterface});
            uiPanelRemoveMethod = ReflectionUtilis.getMethodExplicit("remove", uiPanelClass, new Class<?>[]{ClassRefs.renderableUIElementInterface});

            positionSetMethod = ReflectionUtilis.getMethod("set", ReflectionUtilis.getReturnType(uiPanelAddMethod), 1);
        } catch (Exception e) {
            print(e);
        }
    }

    /** [witness] needs to implement the renderable UI element interface */
    public static void findRenderableUIElementInterface(Object witness) {
        if (witness == null) {
            return;
        }
        for (Class<?> cls : witness.getClass().getInterfaces()) {
            // Look for an interface that has the "render" method
            for (Object method : cls.getDeclaredMethods()) {
                if (ReflectionUtilis.getMethodName(method).equals("render")) {
                    renderableUIElementInterface = cls;
                    renderableSetOpacityMethod = ReflectionUtilis.getMethod("setOpacity", renderableUIElementInterface, 1);
                    return;
                }
            }
        }
        if (renderableUIElementInterface == null) {
            throw new RuntimeException("``Renderable'' interface not found; perhaps invalid witness used?");
        }
    }

    /** [witness] needs to implement the action listener interface */
    public static void findActionListenerInterface(Object witness) {
        actionListenerInterface = findInterfaceByMethod(witness.getClass().getInterfaces(), "actionPerformed");
        buttonListenerActionPerformedMethod = actionListenerInterface.getDeclaredMethods()[0];
    }

    /** [witness] needs to implement the dialog dismissed interface */
    public static void findDialogDismissedInterface(Object witness) {
        dialogDismissedInterface = findInterfaceByMethod(witness.getClass().getInterfaces(), "dialogDismissed");
    }

    public static void findAllClasses() {
        if (foundAllClasses) return;
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        
        if (campaignUIGetCoreMethod == null) {
            campaignUIGetCoreMethod = ReflectionUtilis.getMethod("getCore", campaignUI, 0);
        }
        if (tablePanelSelectMethod == null) {
            findTablePanelMethods();
        }
        if (confirmDialogClass == null) {
            findConfirmDialogClass();
        }
        if (dialogDismissedInterface == null) {
            findDialogDismissedInterface(campaignUI);
        }
        if (actionListenerInterface == null) {
            findActionListenerInterface(campaignUI);
        }
        if (renderableUIElementInterface == null) {
            findRenderableUIElementInterface(ReflectionUtilis.getPrivateVariable("screenPanel", campaignUI));
        }
        if (uiPanelClass == null) {
            findUIPanelClass();
        }
        if (visualPanelFleetInfoClass == null) {
            findFleetInfoClass();
        }
        if (buttonClass == null) {
            findButtonClass();
        }
        if (inputEventClass == null) {
            findInputEventClass();
        }

        if (campaignUIGetCoreMethod != null
                &&tablePanelSelectMethod != null
                && confirmDialogClass != null
                && dialogDismissedInterface != null
                && actionListenerInterface != null
                && uiPanelClass != null
                && renderableUIElementInterface != null
                && visualPanelFleetInfoClass != null
                && buttonClass != null
                && inputEventClass != null) {
            confirmDialogClassParamTypes = new Class<?>[] {
                float.class,
                float.class,
                ClassRefs.uiPanelClass,
                ClassRefs.dialogDismissedInterface,
                String.class,
                String[].class
            };
            foundAllClasses = true;
        }
    }

    public static boolean foundAllClasses(){
        return foundAllClasses;
    }

    /** Tries to find an interface among [interfaces] that has [methodName] as its only method. */
    private static Class<?> findInterfaceByMethod(Class<?>[] interfaces, String methodName) {
        for (Class<?> cls : interfaces) {
            Object[] methods = cls.getDeclaredMethods();
            if (methods.length != 1) {
                continue;
            }
            Object method = methods[0];
            if (ReflectionUtilis.getMethodName(method).equals(methodName)) {
                return cls;
            }
        }

        throw new RuntimeException("Interface with only method " + methodName + " not found; perhaps invalid witness used?");
    }

    public static class DummyInteractionDialogPlugin implements InteractionDialogPlugin {
        public void advance(float arg0) { return; }
        public void backFromEngagement(EngagementResultAPI arg0) { return; }
        public Object getContext() { return ""; }
        public Map<String, MemoryAPI> getMemoryMap() { return new HashMap<>(); }
        public void init(InteractionDialogAPI arg0) { return; }
        public void optionMousedOver(String arg0, Object arg1) { return; }
        public void optionSelected(String arg0, Object arg1) { return; }
    }

    public static List<FleetMemberAPI> getDummyMembers() {
        List<FleetMemberAPI> members = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            members.add(Global.getFactory().createFleetMember(FleetMemberType.SHIP, Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder().get(0).getVariant()));
        }
        return members;
    }
}