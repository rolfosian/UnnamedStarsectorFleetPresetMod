package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.VisualPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.ShowDefaultVisual;
import com.fs.starfarer.api.input.InputEventClass;
import com.fs.starfarer.api.input.InputEventType;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.campaign.fleet.CampaignFleet;
import com.fs.starfarer.api.impl.campaign.JumpPointInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;

import data.scripts.util.PresetMiscUtils;
import data.scripts.util.PresetUtils;
import data.scripts.util.ReflectionUtilis;
import data.scripts.util.UtilReflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import org.apache.log4j.Logger;

/** Stores references to class objects in the obfuscated game files */
public class ClassRefs {
    private static final Logger logger = Logger.getLogger(ClassRefs.class);
    public static void print(Object... args) {
        PresetMiscUtils.print(args);
    }

    /** The class that CampaignUIAPI.showConfirmDialog instantiates. We need this because showConfirmDialog doesn't work
     *  if any core UI is open. */
    public static Class<?> confirmDialogClass;
    /** Interface that contains a single method: actionPerformed */
    public static Class<?> actionListenerInterface;
    /** Interface that contains a single method: dialogDismissed */
    public static Class<?> dialogDismissedInterface;
    /** Interface for renderable UI elements */
    public static Class<?> renderableUIElementInterface;
    /** Obfuscated UI panel class */
    public static Class<?> uiPanelClass;
    /** Obfuscated fleet info panel class from the VisualPanelAPI */
    public static Class<?> visualPanelfleetInfoClass; 
    /** Obfuscated ButtonAPI class */
    public static Class<?> buttonClass;
    /** Obfuscated InputEvent class */
    public static Class<?> inputEventClass;

    private static boolean foundAllClasses = false;

    public static void beginFindFleetInfoClass() {
        if (visualPanelfleetInfoClass != null) return;

        Global.getSector().getCampaignUI().showInteractionDialogFromCargo(new InteractionDialogPlugin() {
            @Override public void advance(float arg0) { return; }
            @Override public void backFromEngagement(EngagementResultAPI arg0) { return; }
            @Override public Object getContext() { return ""; }
            @Override public Map<String, MemoryAPI> getMemoryMap() { return new HashMap<>(); }
            @Override public void init(InteractionDialogAPI arg0) { return; }
            @Override public void optionMousedOver(String arg0, Object arg1) { return; }
            @Override public void optionSelected(String arg0, Object arg1) { return; }
        }, null, new CampaignUIAPI.DismissDialogDelegate() {
            @Override public void dialogDismissed() {} 
        });
    }

    public static void setFleetInfoClass(Class<?> fleetInfoClazz) {
        visualPanelfleetInfoClass = fleetInfoClazz;
    }

    @SuppressWarnings("unchecked")
    public static void findInputEventClass() {
        Class<?>[] inputEventInstantiationParamTypes = new Class<?>[] {
            InputEventClass.class, 
            InputEventType.class, 
            int.class, 
            int.class, 
            int.class, 
            char.class
        };

        UIPanelAPI coreUI = UtilReflection.getCoreUI();
        if (coreUI == null) return;
        for (Object child : (List<Object>) ReflectionUtilis.getMethodAndInvokeDirectly("getChildrenNonCopy", coreUI, 0)) {
            if (ButtonAPI.class.isAssignableFrom(child.getClass()) && !child.getClass().getSimpleName().equals("ButtonAPI")) {

                for (Method method : child.getClass().getDeclaredMethods()) {
                    if (method.getName().equals("buttonPressed")) {
                        for (Class<?> paramType : ReflectionUtilis.getMethodParamTypes(method)) {
                            if (ReflectionUtilis.doInstantiationParamsMatch(paramType.getCanonicalName(), inputEventInstantiationParamTypes)) {
                                inputEventClass = paramType;
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void findButtonClass() {
        UIPanelAPI coreUI = UtilReflection.getCoreUI();
        if (coreUI == null) return;
        for (Object child : (List<Object>) ReflectionUtilis.getMethodAndInvokeDirectly("getChildrenNonCopy", coreUI, 0)) {
            if (ButtonAPI.class.isAssignableFrom(child.getClass()) && !child.getClass().getSimpleName().equals("ButtonAPI")) {
                buttonClass = child.getClass();
                return;
            }
        }
    }

    public static void findConfirmDialogClass() {
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        // If we don't know the confirmation dialog class, try to create a confirmation dialog in order to access it
        try {
            boolean isPaused = Global.getSector().isPaused();
            if (confirmDialogClass == null && campaignUI.showConfirmDialog("", "", "", null, null)) {
                Object screenPanel = UtilReflection.getField(campaignUI, "screenPanel");
                List<?> children = (List<?>) UtilReflection.invokeGetter(screenPanel, "getChildrenNonCopy");
                // the confirm dialog will be the last child
                Object panel = children.get(children.size() - 1);
                confirmDialogClass = panel.getClass();
                // we have the class, dismiss the dialog
                Method dismiss = confirmDialogClass.getMethod("dismiss", int.class);
                dismiss.invoke(panel, 0);
                Global.getSector().setPaused(isPaused);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void findUIPanelClass() {
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        try {
            Field field = campaignUI.getClass().getDeclaredField("screenPanel");
            uiPanelClass = field.getType();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** [witness] needs to implement the renderable UI element interface */
    public static void findRenderableUIElementInterface(Object witness) {
        if (witness == null) {
            return;
        }
        for (Class<?> cls : witness.getClass().getInterfaces()) {
            // Look for an interface that has the "render" method
            for (Method method : cls.getDeclaredMethods()) {
                if (method.getName().equals("render")) {
                    renderableUIElementInterface = cls;
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
    }

    /** [witness] needs to implement the dialog dismissed interface */
    public static void findDialogDismissedInterface(Object witness) {
        dialogDismissedInterface = findInterfaceByMethod(witness.getClass().getInterfaces(), "dialogDismissed");
    }

    public static void findAllClasses() {
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        if (confirmDialogClass == null) {
            findConfirmDialogClass();
        }
        if (dialogDismissedInterface == null) {
            findDialogDismissedInterface(campaignUI);
        }
        if (actionListenerInterface == null) {
            findActionListenerInterface(campaignUI);
        }
        if (uiPanelClass == null) {
            findUIPanelClass();
        }
        if (renderableUIElementInterface == null) {
            findRenderableUIElementInterface(UtilReflection.getField(campaignUI, "screenPanel"));
        }

        if (visualPanelfleetInfoClass == null) {
            beginFindFleetInfoClass();
            setFleetInfoClass((Class<?>) Global.getSector().getMemoryWithoutUpdate().get(PresetUtils.VISUALFLEETINFOPANEL_KEY));
        }
        if (buttonClass == null) {
            findButtonClass();
        }
        if (inputEventClass == null) {
            findInputEventClass();
        }


        if (confirmDialogClass != null
                && dialogDismissedInterface != null
                && actionListenerInterface != null
                && uiPanelClass != null
                && renderableUIElementInterface != null
                && visualPanelfleetInfoClass != null
                && buttonClass != null
                && inputEventClass != null) {
            foundAllClasses = true;
        }
    }

    public static boolean foundAllClasses(){
        return foundAllClasses;
    }

    /** Tries to find an interface among [interfaces] that has [methodName] as its only method. */
    private static Class<?> findInterfaceByMethod(Class<?>[] interfaces, String methodName) {
        for (Class<?> cls : interfaces) {
            Method[] methods = cls.getDeclaredMethods();
            if (methods.length != 1) {
                continue;
            }
            Method method = methods[0];
            if (method.getName().equals(methodName)) {
                return cls;
            }
        }

        throw new RuntimeException("Interface with only method " + methodName + " not found; perhaps invalid witness used?");
    }
}