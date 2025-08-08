// Code taken and modified beyond recognition from Officer Extension mod
package data.scripts;

import com.fs.starfarer.campaign.fleet.CampaignFleet;
// import com.fs.graphics.Sprite;
// import com.fs.starfarer.campaign.fleet.FleetMember;

import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;

import com.fs.starfarer.api.input.InputEventClass;
import com.fs.starfarer.api.input.InputEventType;

import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import data.scripts.listeners.FleetPresetManagementListener;

import data.scripts.util.PresetMiscUtils;
import data.scripts.util.ReflectionUtilis;
import data.scripts.util.ReflectionUtilis.ObfuscatedClasses;

import java.awt.Color;
import java.util.*;

/** Stores references to class objects in the obfuscated game files */
public class ClassRefs {
    public static void print(Object... args) {
        PresetMiscUtils.print(args);
    }

    /** The class that CampaignUIAPI.showConfirmDialog instantiates. We need this because showConfirmDialog doesn't work
     *  if any core UI is open. */
    public static Class<?> confirmDialogClass;
    public static Class<?>[] confirmDialogClassParamTypes;
    public static Object confirmDialogGetHoloMethod;
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
    public static Class<?>[] uiPanelClassConstructorParamTypes = new Class<?>[] {
        float.class,
        float.class,
    };
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

    public static float FMRDialogWidth = FleetPresetManagementListener.CONFIRM_DIALOG_WIDTH * 0.87f;
    public static float FMRDialogHeight = FleetPresetManagementListener.CONFIRM_DIALOG_HEIGHT * 0.87f;
    public static float FMRDialogPanelWidth = FMRDialogWidth * 0.97f;
    public static float FMRDialogPanelHeight = FMRDialogHeight * 0.97f;

    /** Obfuscated ButtonAPI class */
    public static Class<?> buttonClass;
    public static Object buttonListenerActionPerformedMethod;
    public static Object buttonGetListenerMethod;
    public static Object buttonSetListenerMethod;
    public static Object buttonSetEnabledMethod;
    public static Object buttonSetShortcutMethod;
    public static Object buttonSetButtonPressedSoundMethod;
    public static Object buttonSetActiveMethod;

    // public static Class<?> buttonFactoryClass;
    // public static Object memberButtonFactoryMethod;
    // public static Object spriteButtonFactoryMethod;
    // public static class memberButtonEnums {
    //     public static Object FRIEND;
    //     public static Object NEUTRAL;
    //     public static Object ENEMY;
    // }

    public static Object tablePanelsetItemsSelectableMethod;
    public static Object tablePanelSelectMethod;
    public static Object tableRowGetButtonMethod;

    public static Object campaignUIScreenPanelField;
    public static Object campaignUIGetCoreMethod;
    public static Object coreUIgetCurrentTabMethod;

    public static Object fleetTabGetMarketPickerMethod;
    public static Object fleetTabGetFleetPanelMethod;
    public static Object fleetTabFleetInfoPanelField;

    public static Object fleetPanelGetListMethod;
    public static Object fleetPanelListGetItemsMethod;
    public static Object fleetPanelRecreateUIMethod;
    public static Object fleetPanelgetClickAndDropHandlerMethod;
    public static Object fleetPanelClickAndDropHandlerGetPickedUpMemberMethod;

    public static Class<?> uiPanelSuperClass;

    /** Obfuscated InputEvent class */
    public static Class<?> inputEventClass;
    public static Class<?>[] inputEventClassParamTypes = new Class<?>[] {
        InputEventClass.class, // mouse or keyboard
        InputEventType.class, // type of input
        int.class, // x
        int.class, // y
        int.class, // key/mouse button, is -1 for mouse move
        char.class // unused for mouse afaik, give '\0' for mouse prob
    };

    /** method to get optionData from optionItem class that is the type of the values of the InteractionDialogAPI OptionPanel's buttonsToItemsMap */
    public static Object getOptionDataMethod;

    public static Class<?>[] standardTooltipV2ConstructorParamTypes = ReflectionUtilis.getConstructorParamTypesSingleConstructor(com.fs.starfarer.ui.impl.StandardTooltipV2.class);
    public static Class<?> CRBarClass;
    public static Object CRBarClassSetProgressMethod;
    public static Object CRBarClassForceSyncMethod;

    static {
        Class<?>[] interfaces = ObfuscatedClasses.getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            Class<?> interfc = interfaces[i];

            Object[] methods = interfc.getDeclaredMethods();
            if (methods.length == 1 && ReflectionUtilis.getMethodName(methods[0]).equals("dialogDismissed")) {
                dialogDismissedInterface = interfc;
                break;
            }
        }

        Class<?>[] obfClasses = ObfuscatedClasses.getClasses();
        for (int i = 0; i < obfClasses.length; i++) {
            Class<?> cls = obfClasses[i];

            if (optionPanelGetButtonToItemMapMethod == null && OptionPanelAPI.class.isAssignableFrom(cls)) {
                optionPanelGetButtonToItemMapMethod = ReflectionUtilis.getMethod("getButtonToItemMap", cls, 0);
                continue;
            }
            if (interactionDialogGetCoreUIMethod == null && InteractionDialogAPI.class.isAssignableFrom(cls) && !cls.isAnonymousClass()) {
                interactionDialogGetCoreUIMethod = ReflectionUtilis.getMethod("getCoreUI", cls, 0);
                continue;
            }

            if (buttonClass == null && ButtonAPI.class.isAssignableFrom(cls)) {
                buttonClass = cls;
                buttonGetListenerMethod = ReflectionUtilis.getMethod("getListener", buttonClass, 0);
                buttonSetListenerMethod = ReflectionUtilis.getMethod("setListener", buttonClass, 1);
                buttonSetEnabledMethod = ReflectionUtilis.getMethod("setEnabled", buttonClass, 1);
                buttonSetShortcutMethod = ReflectionUtilis.getMethodExplicit("setShortcut", buttonClass, new Class<?>[]{int.class, boolean.class});
                buttonSetButtonPressedSoundMethod = ReflectionUtilis.getMethod("setButtonPressedSound", buttonClass, 1);
                buttonSetActiveMethod = ReflectionUtilis.getMethod("setActive", buttonClass, 1);

                actionListenerInterface = ReflectionUtilis.getReturnType(buttonGetListenerMethod);
                buttonListenerActionPerformedMethod = actionListenerInterface.getMethods()[0];

                Object buttonPressedMethod = ReflectionUtilis.getMethod("buttonPressed", buttonClass, 2);
                inputEventClass = ReflectionUtilis.getMethodParamTypes(buttonPressedMethod)[0];
                continue;
            }

            if (campaignUIGetCoreMethod == null && cls.getSimpleName().equals("CampaignState")) {
                campaignUIGetCoreMethod = ReflectionUtilis.getMethod("getCore", cls, 0);

                Class<?> coreUIClass = ReflectionUtilis.getReturnType(campaignUIGetCoreMethod);
                coreUIgetCurrentTabMethod = ReflectionUtilis.getMethod("getCurrentTab", coreUIClass, 0);

                campaignUIScreenPanelField = ReflectionUtilis.getFieldByName("screenPanel", cls);
                uiPanelClass = ReflectionUtilis.getFieldType(campaignUIScreenPanelField);
                uiPanelSuperClass = uiPanelClass.getSuperclass();
                
                outer:
                for (Class<?> interfc : uiPanelClass.getInterfaces()) {
                    for (Object method : interfc.getDeclaredMethods()) {
                        if (ReflectionUtilis.getMethodName(method).equals("render")) {
                            renderableUIElementInterface = interfc;
                            renderableSetOpacityMethod = ReflectionUtilis.getMethod("setOpacity", renderableUIElementInterface, 1);
                            break outer;
                        }
                    }
                }

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

                confirmDialogClassParamTypes = new Class<?>[] {
                    float.class,
                    float.class,
                    ClassRefs.uiPanelClass,
                    ClassRefs.dialogDismissedInterface,
                    String.class,
                    String[].class
                };
                continue;
            }

            Object[] methods = cls.getDeclaredMethods();
            switch(methods.length) {
                case 2:
                    if (getOptionDataMethod == null) {
                        boolean objReturnType = false;
                        boolean stringReturnType = false;
                        Object objReturnMethod = null;
        
                        for (int j = 0; j < 2; j++) {
                            Object method = methods[j];
                            Class<?> returnType = ReflectionUtilis.getReturnType(method);
        
                            if (returnType.equals(Object.class)) {
                                objReturnType = true;
                                objReturnMethod = method;
        
                            } else if (returnType.equals(String.class)) {
                                stringReturnType = true;
                            }
                        }
                        if (objReturnType && stringReturnType) {
                            getOptionDataMethod = objReturnMethod;
                        }
                    }
                    if (visualPanelFleetInfoClass == null && ReflectionUtilis.doInstantiationParamsMatch(cls, ClassRefs.visualPanelFleetInfoClassParamTypes)) {
                        visualPanelFleetInfoClass = cls;
                    }
                    continue;

                case 15:
                    if (confirmDialogClass == null) {
                        for (int j = 0; j < methods.length; j++) {
                            Object method = methods[j];
        
                            if ((ReflectionUtilis.getMethodName(method)).equals("setNoiseOnConfirmDismiss")) {
                                confirmDialogClass = cls;
        
                                confirmDialogGetHoloMethod = ReflectionUtilis.getMethod("getHolo", confirmDialogClass, 0);
                                confirmDialogGetButtonMethod = ReflectionUtilis.getMethod("getButton", confirmDialogClass, 1);
                                confirmDialogGetInnerPanelMethod = ReflectionUtilis.getMethod("getInnerPanel", confirmDialogClass, 0);
                                confirmDialogShowMethod = ReflectionUtilis.getMethod("show", confirmDialogClass, 2);
                                confirmDialogGetLabelMethod = ReflectionUtilis.getMethod("getLabel", confirmDialogClass, 0);
                                confirmDialogSetBackgroundDimAmountMethod = ReflectionUtilis.getMethod("setBackgroundDimAmount", confirmDialogClass, 1);
                                confirmDialogOutsideClickAbsorbedMethod = ReflectionUtilis.getMethodDeclared("outsideClickAbsorbed", confirmDialogClass, 1);
                                break;
                            }
                        }
                    }
                    continue;

                case 17:
                    if (fleetTabGetFleetPanelMethod == null) {
                        for (int j = 0; j < methods.length; j++) {
                            Object method = methods[j];
        
                            if (ReflectionUtilis.getMethodName(method).equals("getMousedOverFleetMember")) {
                                fleetTabGetFleetPanelMethod = ReflectionUtilis.getMethod("getFleetPanel", cls, 0);
                                fleetTabGetMarketPickerMethod = ReflectionUtilis.getMethod("getMarketPicker", cls, 0);
                        
                                Class<?> fleetPanelCls = ReflectionUtilis.getReturnType(fleetTabGetFleetPanelMethod);
                                fleetPanelgetClickAndDropHandlerMethod = ReflectionUtilis.getMethod("getClickAndDropHandler", fleetPanelCls, 0);
                                fleetPanelRecreateUIMethod = ReflectionUtilis.getMethod("recreateUI", fleetPanelCls, 1);
                                fleetPanelGetListMethod = ReflectionUtilis.getMethod("getList", fleetPanelCls, 0);
                        
                                Class<?> clickAndDropHandlerCls = ReflectionUtilis.getReturnType(fleetPanelgetClickAndDropHandlerMethod);
                                fleetPanelClickAndDropHandlerGetPickedUpMemberMethod = ReflectionUtilis.getMethod("getPickedUpMember", clickAndDropHandlerCls, 0);
                                
                                Class<?> fleetPanelListCls = ReflectionUtilis.getReturnType(fleetPanelGetListMethod);
                                fleetPanelListGetItemsMethod = ReflectionUtilis.getMethod("getItems", fleetPanelListCls, 0);
                                
                                Object[] fields = cls.getDeclaredFields();
                                outer:
                                for (int k = 0; k < fields.length; k++) {
                                    Object field = fields[k];
                                    Class<?> fieldType = ReflectionUtilis.getFieldType(field);
                                    if (!UIPanelAPI.class.isAssignableFrom(fieldType)) continue;
                                    
                                    boolean hasLabelField = false;
                                    boolean hasFleetField = false;

                                    Object[] innerFields = fieldType.getDeclaredFields();
                                    for (int l = 0; l < innerFields.length; l++) {
                                        Object innerField = innerFields[l];

                                        Class<?> innerFieldType = ReflectionUtilis.getFieldType(innerField);
                                        if (CampaignFleetAPI.class.isAssignableFrom(innerFieldType)) {
                                            hasFleetField = true;
                                        }
                                        if (LabelAPI.class.isAssignableFrom(innerFieldType)) {
                                            hasLabelField = true;
                                        }
                                        if (hasFleetField && hasLabelField) {
                                            fleetTabFleetInfoPanelField = field;
                                            break outer;
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                    continue;

                case 56:
                    if (CRBarClass == null) {
                        outer:
                        for (int j = 0; j < methods.length; j++) {
                            Object method = methods[j];

                            if (ReflectionUtilis.getMethodName(method).equals("setProgress")) {
                                CRBarClass = cls;
                                CRBarClassSetProgressMethod = method;

                                for (int k = 0; k < methods.length; k++) {
                                    Object methode = methods[k];

                                    if (ReflectionUtilis.getMethodName(methode).equals("forceSync")) {
                                        CRBarClassForceSyncMethod = methode;
                                        break outer;
                                    }
                                }
                            }
                        }
                    }
                    continue;

                default:
                    continue;
            }
        }

        CustomPanelAPI panel = Global.getSettings().createCustom(0f, 0f, null);
        TooltipMakerAPI tt = panel.createUIElement(0f, 0f, false);
        Object tablePanel = tt.beginTable(Global.getSettings().getBasePlayerColor(), Global.getSettings().getBasePlayerColor(), Global.getSettings().getBasePlayerColor(), 1f, false, false, new Object[]{"", 1f});
        tablePanelsetItemsSelectableMethod = ReflectionUtilis.getMethod("setItemsSelectable", tablePanel, 1);
        tablePanelSelectMethod = ReflectionUtilis.getMethod("select", tablePanel, 2);

        Object row = tt.addRowWithGlow(new Color(0, 0, 0), "");
        tableRowGetButtonMethod = ReflectionUtilis.getMethod("getButton", row, 0);
    }

    /**Dummy function to call to load the class and run the static block in onApplicationLoad */
    public static void findAllClasses() {}
}