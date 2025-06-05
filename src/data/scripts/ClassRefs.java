// Code taken and modified from Officer Extension mod
package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.VisualPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.AbilityPlugin;
import com.fs.starfarer.api.characters.PersonAPI;
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
    /** Interface that contains a single method: actionPerformed */
    public static Class<?> actionListenerInterface;
    /** Interface that contains a single method: dialogDismissed */
    public static Class<?> dialogDismissedInterface;
    /** Interface for renderable UI elements */
    public static Class<?> renderableUIElementInterface;
    /** Obfuscated UI panel class */
    public static Class<?> uiPanelClass;

    /** Obfuscated fleet info panel class from the VisualPanelAPI */
    public static Class<?> visualPanelFleetInfoClass; 
    public static Class<?>[] visualPanelFleetInfoClassParams = new Class<?>[] {
        String.class, // fleet 1 name
        CampaignFleet.class, // fleet 1
        String.class, // fleet 2 name
        CampaignFleet.class, // fleet 2
        FleetEncounterContextPlugin.class,
        boolean.class // is before or after engagement? idk
    };
    /** Obfuscated ButtonAPI class */
    public static Class<?> buttonClass;

    /** Obfuscated InputEvent class */
    public static Class<?> inputEventClass;
    public static Class<?>[] inputEventClassParamTypes = new Class<?>[] {
        InputEventClass.class, // mouse or keyboard
        InputEventType.class, // type of input
        int.class, // x
        int.class, // y
        int.class, // key/mouse button
        char.class // unused for mouse afaik
    };

    private static boolean foundAllClasses = false;

    public static void findFleetInfoClass() {
        if (visualPanelFleetInfoClass != null) return;

        Global.getSector().addTransientListener(new CampaignEventListener() {
            public void reportShownInteractionDialog(InteractionDialogAPI dialog) {
                if (visualPanelFleetInfoClass != null) {
                    Global.getSector().removeListener(this);
                    dialog.dismiss();
                    return;
                }

                VisualPanelAPI visualPanel = dialog.getVisualPanel();
                visualPanel.showFleetInfo("", Global.getSector().getPlayerFleet(), null, null);
        
                for (Object child : (List<Object>) ReflectionUtilis.getMethodAndInvokeDirectly("getChildrenNonCopy", visualPanel, 0)) {
                    if (UIPanelAPI.class.isAssignableFrom(child.getClass()) && ReflectionUtilis.doInstantiationParamsMatch(child.getClass().getCanonicalName(), visualPanelFleetInfoClassParams)) {
                        visualPanelFleetInfoClass = child.getClass(); // found it
                        dialog.dismiss();
                        Global.getSector().removeListener(this);
                        return;
                    }
                }
                dialog.dismiss();
            }
            public void reportBattleFinished(CampaignFleetAPI primaryWinner, BattleAPI battle) {}
            public void reportBattleOccurred(CampaignFleetAPI primaryWinner, BattleAPI battle) {}
            public void reportEconomyMonthEnd() {}
            public void reportEconomyTick(int iterIndex) {}
            public void reportEncounterLootGenerated(FleetEncounterContextPlugin plugin, CargoAPI loot) {}
            public void reportFleetDespawned(CampaignFleetAPI fleet, CampaignEventListener.FleetDespawnReason reason, Object param) {}
            public void reportFleetJumped(CampaignFleetAPI fleet, SectorEntityToken from, JumpPointAPI.JumpDestination to) {}
            public void reportFleetReachedEntity(CampaignFleetAPI fleet, SectorEntityToken entity) {}
            public void reportFleetSpawned(CampaignFleetAPI fleet) {}
            public void reportPlayerActivatedAbility(AbilityPlugin ability, Object param) {}
            public void reportPlayerDeactivatedAbility(AbilityPlugin ability, Object param) {}
            public void reportPlayerDidNotTakeCargo(CargoAPI cargo) {}
            public void reportPlayerDumpedCargo(CargoAPI cargo) {}
            public void reportPlayerEngagement(EngagementResultAPI result) {}
            public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {}
            public void reportPlayerOpenedMarketAndCargoUpdated(MarketAPI market) {}
            public void reportPlayerReputationChange(PersonAPI person, float delta) {}
            public void reportPlayerReputationChange(String faction, float delta) {}
            public void reportPlayerClosedMarket(MarketAPI arg0) {}
            public void reportPlayerOpenedMarket(MarketAPI arg0) {}
        }
        );

        Global.getSector().getCampaignUI().showInteractionDialogFromCargo(new InteractionDialogPlugin() {
            public void advance(float arg0) { return; }
            public void backFromEngagement(EngagementResultAPI arg0) { return; }
            public Object getContext() { return ""; }
            public Map<String, MemoryAPI> getMemoryMap() { return new HashMap<>(); }
            public void init(InteractionDialogAPI arg0) { return; }
            public void optionMousedOver(String arg0, Object arg1) { return; }
            public void optionSelected(String arg0, Object arg1) { return; }
        }, null, new CampaignUIAPI.DismissDialogDelegate() {
            public void dialogDismissed() {}
        });
    }

    public static void findInputEventClass() {
        UIPanelAPI coreUI = UtilReflection.getCoreUI();
        if (coreUI == null) return;

;
        for (Object child : (List<Object>) ReflectionUtilis.getMethodAndInvokeDirectly("getChildrenNonCopy", coreUI, 0)) {
            if (ButtonAPI.class.isAssignableFrom(child.getClass()) && !child.getClass().getSimpleName().equals("ButtonAPI")) {

                for (Object method : child.getClass().getDeclaredMethods()) {
                    if (ReflectionUtilis.getMethodName(method).equals("buttonPressed")) {
                        for (Class<?> paramType : ReflectionUtilis.getMethodParamTypes(method)) {
                            if (ReflectionUtilis.doInstantiationParamsMatch(paramType.getCanonicalName(), inputEventClassParamTypes)) {
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
                Object screenPanel = ReflectionUtilis.getPrivateVariable("screenPanel", campaignUI);
                List<Object> children = (List<Object>) ReflectionUtilis.getMethodAndInvokeDirectly("getChildrenNonCopy", screenPanel, 0);
                // the confirm dialog will be the last child
                Object panel = children.get(children.size() - 1);
                confirmDialogClass = panel.getClass();
                // we have the class, dismiss the dialog

                ReflectionUtilis.getMethodAndInvokeDirectly("dismiss", panel, 1, 0);
                Global.getSector().setPaused(isPaused);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void findUIPanelClass() {
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        try {
            Object field = campaignUI.getClass().getDeclaredField("screenPanel");
            uiPanelClass = ReflectionUtilis.getFieldType(field);
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
            for (Object method : cls.getDeclaredMethods()) {
                if (ReflectionUtilis.getMethodName(method).equals("render")) {
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

        if (visualPanelFleetInfoClass == null) {
            findFleetInfoClass();
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
                && visualPanelFleetInfoClass != null
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
}