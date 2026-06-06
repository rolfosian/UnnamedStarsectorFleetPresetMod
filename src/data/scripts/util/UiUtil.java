package data.scripts.util;

import java.util.*;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

import org.apache.log4j.Logger;
import java.awt.Color;

import com.fs.graphics.util.Fader;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.input.InputEventClass;
import com.fs.starfarer.api.input.InputEventType;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.ScrollPanelAPI;
import com.fs.starfarer.api.ui.TextFieldAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.campaign.CampaignState;
import com.fs.starfarer.campaign.command.AdminPickerDialog;
import com.fs.starfarer.campaign.fleet.FleetMember;
import com.fs.starfarer.campaign.ui.UITable;
import com.fs.starfarer.campaign.ui.marketinfo.CommodityOverviewPanel;
import com.fs.starfarer.campaign.ui.marketinfo.PlanetSurveyPanel;
import com.fs.starfarer.combat.CombatViewport;
import com.fs.starfarer.ui.interfacenew;
import com.fs.starfarer.ui.impl.StandardTooltipV2;
import com.fs.starfarer.ui.impl.StandardTooltipV2Expandable;
import com.fs.starfarer.ui.newui.CampaignEntityPickerDialog;

import data.scripts.autopilotwithgates.org.objectweb.asm.*;
import data.scripts.util.TreeTraverser.TreeNode;
import data.scripts.util.UiUtil.UiInstantiator;

public class UiUtil implements Opcodes {
    private static final Logger logger = Logger.getLogger(UiUtil.class);
    public static void print(Object... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i] instanceof String ? (String) args[i] : String.valueOf(args[i]));
            if (i < args.length - 1) sb.append(' ');
        }
        logger.info(sb.toString());
    }

    public static final ClassLoader cl = new ClassLoader(UiUtil.class.getClassLoader());

    public static interface UtilInterface {
        public Object interactionDialogGetCore(Object interactionDialog);
        public Object campaignUIgetCore(Object campaignUI);
        public UIPanelAPI coreGetCurrentTab(Object core);

        public UIPanelAPI campaignUIGetScreenPanel(Object campaignUI);

        public UIPanelAPI fleetTabGetMarketPicker(Object fleetTab);
        public UIPanelAPI fleetTabGetFleetPanel(Object fleetTab);

        public void fleetPanelRecreateUI(UIPanelAPI fleetPane, boolean doFleetSync);
        public UIPanelAPI fleetPanelGetList(UIPanelAPI fleetPanel);
        public Object fleetPanelGetHandler(UIPanelAPI fleetPanel);

        public FleetMemberAPI handlerGetPickedUpMember(Object fleetPanelHandler);

        public void confirmDialogDismiss(Object confirmDialog, int button);
        public ButtonAPI confirmDialogGetButton(Object confirmDialog, int button);
        public LabelAPI confirmDialogGetLabel(Object confirmDialog);
        public boolean isNoiseOnConfirmDismiss(Object confirmDialog);
        public void confirmDialogShow(Object confirmDialog, float durationIn, float durationOut);
        public UIPanelAPI confirmDialogGetInnerPanel(Object confirmDialog); // custom method with instanceof check confirmDialog superclass else return null
        public UIComponentAPI confirmDialogGetHolo(Object confirmDialog);
        public void confirmDialogSetBackgroundDimAmount(Object confirmDialog, float dimAmount);

        public void actionPerformed(Object listener, Object inputEvent, Object uiElement);

        public ButtonAPI factoryCreateRegularButton(String text, String font, Color bg, Color base, Alignment alignment, CutStyle cutStyle, Object listener);
        public Object buttonGetRenderer(Object button);
        public void buttonSetListener(Object button, Object listener);
        public Object buttonGetListener(Object button);

        public List<UIComponentAPI> listPanelGetItems(UIPanelAPI listPanel);
        public void listPanelAddItem(UIPanelAPI listPanel, UIComponentAPI item);
        public void listPanelRemoveItem(UIPanelAPI listPanel, UIComponentAPI item);
        public void listPanelCollapseEmptySlots(UIPanelAPI listPanel, boolean updateScroller);
        public void listPanelClear(UIPanelAPI listPanel);
        public ScrollPanelAPI listPanelGetScroller(UIPanelAPI listPanel);

        public UIPanelAPI scrollerGetContentContainer(ScrollPanelAPI scroller);
        public void scrollerSetOffset(ScrollPanelAPI scroller, float x, float y);
        public void scrollerClampOffset(ScrollPanelAPI scroller);

        public PositionAPI uiPanelAdd(UIPanelAPI panel, UIComponentAPI componentToAdd);
        public void positionSet(PositionAPI pos, PositionAPI toSet);

        public float getOpacity(Object uiComponent);
        public void setOpacity(Object uiComponent, float opacity);

        public void setTooltip(Object uiComponent, float activationDelay, Object tooltip);
        public void setTooltipPositionRelativeToAnchor(Object uiComponent, float xPad, float yPad, Object anchor); // anchor should be instance of uicomponent

        public void showTooltip(Object uiComponent, Object tooltip);
        public void hideTooltip(Object uiComponent, Object tooltip);
        public Object getTooltip(Object uiComponent);
        public UIPanelAPI getContents(Object tooltip);
        public UIPanelAPI tooltipGetCustom(Object tooltip);

        public UIPanelAPI labelGetParent(Object label);
        public UIPanelAPI getParent(Object uiComponent);

        public List<UIComponentAPI> getChildrenNonCopy(UIPanelAPI uiPanel);
        public List<UIComponentAPI> getChildrenNonCopy(UIComponentAPI parent); // custom method with instanceof check uiPanelClass else return null

        public List<Object> uiTableGetRows(UITable table);
        public void uiTableAddRow(UITable table, Object row);
        public void uiTableRemoveRow(UITable table, Object row);
        public Object uiTableGetRowForData(UITable table, Object data);
        public UIPanelAPI uiTableRowGetCol(Object row, int col);
        public ButtonAPI uiTableRowGetButton(Object row);
        public void uiTableRowRender(Object row, float alphaMult);
        public void uiTableRowSetButton(Object row, Object button);
        public Object uiTableRowGetData(Object row);
        public void uiTableRowSetData(Object row, Object data); // data type is actually java.lang.Object, no cast required

        public Object uiTableGetSelected(UITable table);
        public void uiTableSelect(UITable table, Object row, Object inputEvent);
        public void uiTableSelect(UITable table, Object row, Object inputEvent, boolean notifyDelegate);
        public UIPanelAPI uiTableGetList(UITable table);

        public Map<ButtonAPI, Object> optionPanelGetButtonToItemMap(OptionPanelAPI optionPanel);
        public Object optionPanelItemGetOptionData(Object optionItem);
        public List<UIComponentAPI> listPanelGetItems(Object listPanel); // custom method with instanceof check listPanelClass else return null
    }

    public static interface UiInstantiator {
        public UIPanelAPI instantiateUiPanel(float width, float height);
        // public UIPanelAPI instantiateProgressBar(String text, float rangeMin, float rangeMax);
        public UIPanelAPI instantiateConfirmDialog(float width, float height, UIPanelAPI parent, Object dialogDismissedListener, String titleLabelText, String... buttonTexts);
        public InputEventAPI instantiateInputEvent(InputEventClass eventClass, InputEventType eventType, int x, int y, int value, char c);
        public List<InputEventAPI> instantiateInputEventList();
        public UIPanelAPI instantiateFleetMemberItem(FleetMemberAPI member, UIPanelAPI fleetPanel); // for fleet tab fleet panel list, width and height params notwithstanding - will do getstatic operations for these
    }
 
    // With this we can implement the above interface and generate a class at runtime to call obfuscated class methods platform agnostically without reflection overhead
    private static Class<?>[] implementUtilInterface(Class<?> coreClass, Class<?> buttonClass, Class<?> actionListenerInterface) throws Throwable {
        String listDesc = Type.getDescriptor(List.class);
        String mapDesc = Type.getDescriptor(Map.class);

        String coreClassInternalName = Type.getInternalName(coreClass);

        Class<?> interactionDialogClass = Refl.getFieldType(Refl.getFieldByName("encounterDialog", CampaignState.class));
        Class<?> uiPanelClass = coreClass.getSuperclass().getSuperclass();
        Class<?> uiComponentClass = uiPanelClass.getSuperclass();

        Class<?> uiTableRowSuperSuperClass = Refl.getReturnType((Refl.getMethod("getSelected", UITable.class)));
        String uiTableInternalName = Type.getInternalName(UITable.class);
        String uiTableRowInternalName = Type.getInternalName(uiTableRowSuperSuperClass);
        String uiTableDesc = Type.getDescriptor(UITable.class);
        String uiTableRowDesc = Type.getDescriptor(uiTableRowSuperSuperClass);

        TooltipMakerAPI tt = Global.getSettings().createCustom(0f,0f,null).createUIElement(0f,0f,false);
        tt.beginTable(Global.getSettings().getBasePlayerColor(), Global.getSettings().getBasePlayerColor(), Global.getSettings().getBasePlayerColor(), 1f, false, false, new Object[]{"", 1f});
        Class<?> uiTableRowSubClass = tt.addRowWithGlow("").getClass();
        // String uiTableRowSubClassDesc = Type.getDescriptor(uiTableRowSubClass);
        String uiTableRowSubClassInternalName = Type.getInternalName(uiTableRowSubClass);

        Class<?> listPanelClass = Refl.getReturnType(Refl.getMethod("getListAdmins", AdminPickerDialog.class));
        Class<?> uiComponentInterfaceA = Refl.getMethodParamTypes(Refl.getMethod("addItem", listPanelClass))[0];

        String listPanelDesc = Type.getDescriptor(listPanelClass);
        String listPanelInternalName = Type.getInternalName(listPanelClass);

        Class<?> scrollerClass = Refl.getReturnType(Refl.getMethod("getScroller", listPanelClass));
        Class<?> scrollerContentContainerClass = Refl.getReturnType(Refl.getMethod("getContentContainer", scrollerClass));

        Class<?> toolTipClass = Refl.getReturnType(Refl.getMethod("getTooltip", uiComponentClass));
        Class<?> positionClass = null;

        for (Object method : uiPanelClass.getDeclaredMethods()) {
            if (Refl.getMethodName(method).equals("add")) {
                Class<?> returnType = Refl.getReturnType(method);
                if (returnType != void.class) {
                    positionClass = returnType;
                    break;
                }
            }
        }
        String positionAPIDesc = Type.getDescriptor(PositionAPI.class);
        String positionInternalName = Type.getInternalName(positionClass);
        String positionDesc = Type.getDescriptor(positionClass);

        Class<?> fleetTabClass = getFleetTabClass(coreClass, actionListenerInterface);
        String fleetTabClassInternalName = Type.getInternalName(fleetTabClass);

        Class<?> fleetPanelClass = Refl.getReturnType(Refl.getMethod("getFleetPanel", fleetTabClass));
        String fleetPanelDesc = Type.getDescriptor(fleetPanelClass);
        String fleetPanelInternalName = Type.getDescriptor(fleetPanelClass);

        Class<?> fleetPanelItemClass = Refl.getReturnType(Refl.getMethod("getRowFor", fleetPanelClass));
        String fleetPanelItemDesc = Type.getDescriptor(fleetPanelItemClass);
        String fleetPanelItemInternalName = Type.getInternalName(fleetPanelItemClass);

        Class<?> fleetPanelHandlerClass = Refl.getReturnType(Refl.getMethod("getHandler", fleetPanelClass));
        String fleetPanelHandlerDesc = Type.getDescriptor(fleetPanelClass);
        String fleetPanelHandlerInternalName = Type.getInternalName(fleetPanelHandlerClass);
        String fleetMemberDesc = Type.getDescriptor(FleetMember.class);

        String uiPanelInternalName = Type.getInternalName(uiPanelClass);
        String uiComponentInternalName = Type.getInternalName(uiComponentClass);
        
        Class<?> confirmDialogClass = CampaignEntityPickerDialog.class.getSuperclass();
        String confirmDialogInternalName = Type.getInternalName(confirmDialogClass);

        Class<?> confirmDialogHoloClass = Refl.getReturnType(Refl.getMethod("getHolo", confirmDialogClass));
        String confirmDialogHoloDesc = Type.getDescriptor(confirmDialogHoloClass);

        Class<?> dialogDismissedInterface = Refl.getReturnType(Refl.getMethod("getDelegate", confirmDialogClass.getSuperclass()));
        String dialogDismissedInterfaceDesc = Type.getDescriptor(dialogDismissedInterface);

        Class<?> labelClass = Refl.getReturnType(Refl.getMethod("getLabel", confirmDialogClass));
        String labelInternalName = Type.getInternalName(labelClass);
        String labelDesc = Type.getDescriptor(labelClass);
        String labelAPIDesc = Type.getDescriptor(LabelAPI.class);

        Class<?> inputEventClass = Refl.getMethodParamTypes(Refl.getMethod("buttonPressed", buttonClass))[0];
        String inputEventInternalName = Type.getInternalName(inputEventClass);
        String inputEventClassDesc = Type.getDescriptor(InputEventClass.class);
        String inputEventTypeDesc = Type.getDescriptor(InputEventType.class);
        String inputEventAPIDesc = Type.getDescriptor(InputEventAPI.class);

        Class<?> inputEventListClass = Refl.getMethodParamTypes(Refl.getMethodDeclared("processInputImpl", uiPanelClass))[0];
        String inputEventListInternalName = Type.getInternalName(inputEventListClass);

        String[] tooltipMethodData = getTooltipMethodData();
        Class<?> shipIconListClass = Class.forName(tooltipMethodData[3].replace("/", "."), false, StandardTooltipV2Expandable.class.getClassLoader());
        // String shipIconListDesc = Type.getDescriptor(shipIconListClass);
        // String shipIconListInternalName = Type.getDescriptor(shipIconListClass);

        Class<?> shipIconRendererClass = getshipIconRendererClass(shipIconListClass);

        String buttonClassInternalName = Type.getInternalName(buttonClass);
        String actionListenerInterfaceDesc = Type.getDescriptor(actionListenerInterface);
        String actionListenerInterfaceInternalName = Type.getInternalName(actionListenerInterface);
        
        String superName = Type.getType(Object.class).getInternalName();
        String interfaceName = Type.getType(UtilInterface.class).getInternalName();

        String coreClassDesc = Type.getDescriptor(coreClass);
        String uiPanelClassDesc = Type.getDescriptor(uiPanelClass);
        String uiPanelAPIDesc = Type.getDescriptor(UIPanelAPI.class);
        String uiComponentAPIDesc = Type.getDescriptor(UIComponentAPI.class);
        String uiComponentDesc = Type.getDescriptor(uiComponentClass);

        Class<?> showTooltipInterface = null;
        Class<?> setTooltipInterface = null;
        for (Class<?> interfc : uiComponentClass.getInterfaces()) {
            for (Object method: interfc.getMethods()) {
                String name = Refl.getMethodName(method);

                if (name.equals("showTooltip")) {
                    showTooltipInterface = interfc;
                } else if (name.equals("setTooltip")) {
                    setTooltipInterface = interfc;
                }
            }
        }

        String showTooltipInterfaceInternalName = Type.getInternalName(showTooltipInterface);
        String setTooltipInterfaceInternalName = Type.getInternalName(setTooltipInterface);
        String toolTipInternalName = Type.getInternalName(toolTipClass);
        String tooltipDesc = Type.getDescriptor(toolTipClass);

        Class<?> optionPanelClass = null;
        for (Object field : interactionDialogClass.getDeclaredFields()) {
            Class<?> fieldType = Refl.getFieldType(field);
            if (OptionPanelAPI.class.isAssignableFrom(fieldType)) {
                optionPanelClass = fieldType;
                break;
            }
        }
        Class<?> optionPanelItemClass = null;
        for (Class<?> cls : optionPanelClass.getClasses()) {
            Class<?>[] paramTypes = Refl.getConstructorParamTypes(cls.getConstructors()[0]);
            if (paramTypes.length == 4
                && paramTypes[0] == String.class
                && paramTypes[1] == Object.class
                && paramTypes[2] == Color.class
                && paramTypes[3] == String.class
            ) {
                optionPanelItemClass = cls;
                break;
            }
        }
        String optionPanelInternalName = Type.getInternalName(optionPanelClass);
        String optionPanelItemInternalName = Type.getInternalName(optionPanelItemClass);
        String optionPanelApiDesc = Type.getDescriptor(OptionPanelAPI.class);

        String buttonAPIDesc = Type.getDescriptor(ButtonAPI.class);
        String buttonClassDesc = Type.getDescriptor(buttonClass);

        String uiComponentInterfaceAInternalName = Type.getInternalName(uiComponentInterfaceA);
        String uiComponentInterfaceADesc = Type.getDescriptor(uiComponentInterfaceA);

        String campaignStateInternalName = Type.getInternalName(CampaignState.class);

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        // public class UtilInterface extends Object implements this crap
        cw.visit(
            V17,
            ACC_PUBLIC,
            "data/scripts/util/UtilInterface",
            null,
            superName,
            new String[] {interfaceName}
        );

        MethodVisitor ctor = cw.visitMethod(
            ACC_PUBLIC,
            "<init>",
            "()V",
            null,
            null
        );
        ctor.visitCode();
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitMethodInsn(INVOKESPECIAL, superName, "<init>", "()V", false);
        ctor.visitInsn(RETURN);
        ctor.visitMaxs(0, 0);
        ctor.visitEnd();

        // public Object interactionDialogGetCore(Object interactionDialog) {
        //     return ((interactionDialogClass)interactionDialog).getCoreUI();
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "interactionDialogGetCore",
                "(Ljava/lang/Object;)Ljava/lang/Object;",
                null,
                null
            );
            mv.visitCode();
            String interactionDialogInternalName = Type.getInternalName(interactionDialogClass);

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, interactionDialogInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                interactionDialogInternalName,
                "getCoreUI",
                "()" + coreClassDesc,
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public Object campaignUIgetCore(Object campaignUI) {
        //     return ((CampaignState)campaignUI).getCore();
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "campaignUIgetCore",
                "(Ljava/lang/Object;)Ljava/lang/Object;",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, campaignStateInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                campaignStateInternalName,
                "getCore",
                "()" + coreClassDesc,
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public UIPanelAPI coreGetCurrentTab(Object core) {
        //     return ((coreClass)core).getCurrentTab();
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "coreGetCurrentTab",
                "(Ljava/lang/Object;)" + uiPanelAPIDesc,
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, coreClassInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                coreClassInternalName,
                "getCurrentTab",
                "()" + uiPanelClassDesc,
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public UIPanelAPI fleetTabGetMarketPicker(Object fleetTab) {
        //     return ((fleetTabClass)fleetTab).getMarketPicker();
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "fleetTabGetMarketPicker",
                "(Ljava/lang/Object;)" + uiPanelAPIDesc,
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, fleetTabClassInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                fleetTabClassInternalName,
                "getMarketPicker",
                "()" + Type.getDescriptor(Refl.getReturnType(Refl.getMethod("getMarketPicker", fleetTabClass))),
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public UIPanelAPI fleetTabGetFleetPanel(Object fleetTab) {
        //     return ((fleetTabClass)fleetTab).getFleetTab();
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "fleetTabGetFleetPanel",
                "(Ljava/lang/Object;)" + uiPanelAPIDesc,
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, fleetTabClassInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                fleetTabClassInternalName,
                "getFleetPanel",
                "()" + fleetPanelDesc,
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public void fleetPanelRecreateUI(UIPanelAPI fleetPanel, boolean doSync) {
        //     return ((fleetPanelClass)fleetPanel).recreateUI(doSync);
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "fleetPanelRecreateUI",
                "(" + uiPanelAPIDesc + "Z)V",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, fleetPanelInternalName);
            mv.visitVarInsn(ILOAD, 2);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                fleetPanelInternalName,
                "recreateUI",
                "(Z)V",
                false
            );

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public UIPanelAPI fleetPanelGetList(UIPanelAPI fleetPanel) {
        //     return ((fleetPanelClass)fleetPanel).getList();
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "fleetPanelGetList",
                "(" + uiPanelAPIDesc + ")" + uiPanelAPIDesc ,
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, fleetPanelInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                fleetPanelInternalName,
                "getList",
                "()" + listPanelDesc,
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public Object fleetPanelGetHandler(UIPanelAPI fleetPanel) {
        //     return ((fleetPanelClass)fleetPanel).getHandler();
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "fleetPanelGetHandler",
                "(Ljava/lang/Object;)Ljava/lang/Object;",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, fleetPanelInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                fleetPanelInternalName,
                "getHandler",
                "()" + fleetPanelHandlerDesc,
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public FleetMemberAPI handlerGetPickedUpMember(Object handler) {
        //     return ((handlerClass)handler).getPickedUpMember();
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "handlerGetPickedUpMember",
                "(Ljava/lang/Object;)" + Type.getDescriptor(FleetMemberAPI.class),
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, fleetPanelHandlerInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                fleetPanelHandlerInternalName,
                "getPickedUpMember",
                "()" + fleetMemberDesc,
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public void confirmDialogDismiss(Object confirmDialog, int confirmOrCancel) {
        //     ((confirmDialogClass)confirmDialog).dismiss(confirmOrCancel);
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "confirmDialogDismiss",
                "(Ljava/lang/Object;I)V",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, confirmDialogInternalName);

            mv.visitVarInsn(ILOAD, 2);
            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                confirmDialogInternalName,
                "dismiss",
                "(I)V",
                false
            );

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public UIComponentAPI confirmDialogGetHolo(Object confirmDialog) {
        //     return ((confirmDialogClass)confirmDialog).getHolo();
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "confirmDialogGetHolo",
                "(Ljava/lang/Object;)" + uiComponentAPIDesc,
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, confirmDialogInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                confirmDialogInternalName,
                "getHolo",
                "()" + confirmDialogHoloDesc,
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public void confirmDialogSetBackgroundDimAmount(Object confirmDialog, float amount) {
        //     return ((confirmDialogClass)confirmDialog).setBackgroundDimAmount(amount);
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "confirmDialogSetBackgroundDimAmount",
                "(Ljava/lang/Object;F)V",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, confirmDialogInternalName);
            mv.visitVarInsn(FLOAD, 2);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                confirmDialogInternalName,
                "setBackgroundDimAmount",
                "(F)V",
                false
            );

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }


        // public ButtonAPI confirmDialogGetButton(Object confirmDialog, int button) {
        //     return ((confirmDialogClass)confirmDialog).getButton(button);
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "confirmDialogGetButton",
                "(Ljava/lang/Object;I)" + buttonAPIDesc,
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, confirmDialogInternalName);
            mv.visitVarInsn(ILOAD, 2);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                confirmDialogInternalName,
                "getButton",
                "(I)" + buttonClassDesc,
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public LabelAPI confirmDialogGetLabel(Object confirmDialog) {
        //     return ((confirmDialogClass)confirmDialog).getLabel();
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "confirmDialogGetLabel",
                "(Ljava/lang/Object;)" + labelAPIDesc,
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, confirmDialogInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                confirmDialogInternalName,
                "getLabel",
                "()" + labelDesc,
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public boolean isNoiseOnConfirmDismiss(Object confirmDialog) {
        //     return ((confirmDialogClass)confirmDialog).isNoiseOnConfirmDismiss();
        // }
        {
            String isNoiseOnDismissDesc = Type.getMethodDescriptor(Refl.getMethod("isNoiseOnConfirmDismiss", confirmDialogClass));
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "isNoiseOnConfirmDismiss",
                "(Ljava/lang/Object;)Z",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, confirmDialogInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                confirmDialogInternalName,
                "isNoiseOnConfirmDismiss",
                isNoiseOnDismissDesc,
                false
            );

            mv.visitInsn(IRETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public void confirmDialogShow(Object confirmDialog, float durationIn, float durationOut) {
        //     ((confirmDialogClass)confirmDialog).show(durationIn, durationOut);
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "confirmDialogShow",
                "(Ljava/lang/Object;FF)V",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, confirmDialogInternalName);
            mv.visitVarInsn(FLOAD, 2);
            mv.visitVarInsn(FLOAD, 3);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                confirmDialogInternalName,
                "show",
                "(FF)V",
                false
            );

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public UIPanelAPI confirmDialogGetInnerPanel(Object confirmDialog) {
        //     if (instanceof confirmDialogSuperClass) {
        //         return ((confirmDialgSuperClass)confirmDialog).getInnerPanel();
        //     }
        //     return null;
        // }
        {   
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "confirmDialogGetInnerPanel",
                "(Ljava/lang/Object;)" + uiPanelAPIDesc,
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(INSTANCEOF, confirmDialogInternalName);
            
            Label ifInstanceOf = new Label();
            mv.visitJumpInsn(IFNE, ifInstanceOf);
            
            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ARETURN);
            
            mv.visitLabel(ifInstanceOf);

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, confirmDialogInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                confirmDialogInternalName,
                "getInnerPanel",
                "()" + uiPanelClassDesc,
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public UIPanelAPI campaignUIGetScreenPanel(Object campaignUI) {
        //     return ((CampaignState)campaignUI).getScreenPanel();
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "campaignUIGetScreenPanel",
                "(Ljava/lang/Object;)" + uiPanelAPIDesc,
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, campaignStateInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                campaignStateInternalName,
                "getScreenPanel",
                "()" + uiPanelClassDesc,
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public void actionPerformed(Object listener, Object inputEvent, Object uiElement) {
        //     ((actionListenerInterface)listener).actionPerformed(inputEvent, uiElement);
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "actionPerformed",
                "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, actionListenerInterfaceInternalName);

            mv.visitVarInsn(ALOAD, 2);
            mv.visitVarInsn(ALOAD, 3);

            mv.visitMethodInsn(
                INVOKEINTERFACE,
                actionListenerInterfaceInternalName,
                "actionPerformed",
                "(Ljava/lang/Object;Ljava/lang/Object;)V",
                true // interface method
            );

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public void listPanelAddItem(UIPanelAPI listPanel, UIComponentAPI item) {
        //     ((listPanelClass)listPanel).addItem((uiComponentClass)item);
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "listPanelAddItem",
                "(" + uiPanelAPIDesc + uiComponentAPIDesc + ")V",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, listPanelInternalName);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitTypeInsn(CHECKCAST, uiComponentInterfaceAInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                listPanelInternalName,
                "addItem",
                "(" + uiComponentInterfaceADesc + ")V",
                false
            );

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public void listPanelClear(UIPanelAPI listPanel) {
        //     ((listPanelClass)listPanel).clear();
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "listPanelClear",
                "(" + uiPanelAPIDesc + ")V",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, listPanelInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                listPanelInternalName,
                "clear",
                "()V",
                false
            );

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public void listPanelRemoveItem(UIPanelAPI listPanel, UIComponentAPI item) {
        //     ((listPanelClass)listPanel).removeItem((uiComponentClass)item);
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "listPanelRemoveItem",
                "(" + uiPanelAPIDesc + uiComponentAPIDesc + ")V",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, listPanelInternalName);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitTypeInsn(CHECKCAST, uiComponentInterfaceAInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                listPanelInternalName,
                "removeItem",
                "(" + uiComponentInterfaceADesc + ")V",
                false
            );

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public void listPanelCollapseEmptySlots(UIPanelAPI listPanel, boolean updateScroller) {
        //     ((listPanelClass)listPanel).removeItem(updateScroller);
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "listPanelCollapseEmptySlots",
                "(" + uiPanelAPIDesc + "Z)V",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, listPanelInternalName);
            mv.visitVarInsn(ILOAD, 2);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                listPanelInternalName,
                "collapseEmptySlots",
                "(Z)V",
                false
            );

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public void scrollerClampOffset(ScrollPanelAPI scroller) {
        //     return ((scrollerClass)scroller).clampOffset();
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "scrollerClampOffset",
                "(" + Type.getDescriptor(ScrollPanelAPI.class) + ")V",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(scrollerClass));
            
            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                Type.getInternalName(scrollerClass),
                "clampOffset",
                "()V",
                false
            );

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public void scrollerSetOffset(ScrollPanelAPI scroller, float x, float y) {
        //     return ((scrollerClass)scroller).setOffset(x, y);
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "scrollerSetOffset",
                "(" + Type.getDescriptor(ScrollPanelAPI.class) + "FF)V",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(scrollerClass));
            mv.visitVarInsn(FLOAD, 2);
            mv.visitVarInsn(FLOAD, 3);
            
            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                Type.getInternalName(scrollerClass),
                "setOffset",
                "(FF)V",
                false
            );

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public UIPanelAPI scrollerGetContentContainer(ScrollPanelAPI scroller) {
        //     return ((scrollerClass)scroller).getContentContainer();
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "scrollerGetContentContainer",
                "(" + Type.getDescriptor(ScrollPanelAPI.class) + ")" + uiPanelAPIDesc,
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(scrollerClass));
            
            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                Type.getInternalName(scrollerClass),
                "getContentContainer",
                "()" + Type.getDescriptor(scrollerContentContainerClass),
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public ScrollPanelAPI listPanelGetScroller(UIPanelAPI listPanel) {
        //     return ((listPanelClass)listPanel).getScroller();
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "listPanelGetScroller",
                "(" + uiPanelAPIDesc + ")" + Type.getDescriptor(ScrollPanelAPI.class),
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, listPanelInternalName);
            
            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                listPanelInternalName,
                "getScroller",
                "()" + Type.getDescriptor(scrollerClass),
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public List<UIComponentAPI> listPanelGetItems(UIPanelAPI listPanel) {
        //     return ((listPanelClass)listPanel).getItems();
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "listPanelGetItems",
                "(" + uiPanelAPIDesc + ")" + listDesc,
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, listPanelInternalName);
            
            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                listPanelInternalName,
                "getItems",
                "()" + listDesc,
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public List<UIComponent> listPanelGetItems(Object listPanel) {
        //     if (listPanel instanceof listPanelClass) {
        //         return ((listPanelClass)listPanel).getItems();
        //     }
        //     return null;
        // }
        {   
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "listPanelGetItems",
                "(Ljava/lang/Object;)" + listDesc,
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(INSTANCEOF, listPanelInternalName);
            
            Label ifInstanceOf = new Label();
            mv.visitJumpInsn(IFNE, ifInstanceOf);
            
            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ARETURN);
            
            mv.visitLabel(ifInstanceOf);

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, listPanelInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                listPanelInternalName,
                "getItems",
                "()" + listDesc,
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public ButtonAPI factoryCreateRegularButton(String text, String font, Color bg, Color base, Alignment alignment, CutStyle cutStyle, Object listener) {
        //     ((buttonClass)button).createRegularButton(text, font, bg, base, alignment, cutStyle, listener);
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "factoryCreateRegularButton",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/awt/Color;Ljava/awt/Color;"
                + Type.getDescriptor(Alignment.class) + Type.getDescriptor(CutStyle.class)
                + "Ljava/lang/Object;)" + buttonAPIDesc,
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitVarInsn(ALOAD, 4);
            mv.visitVarInsn(ALOAD, 5);
            mv.visitVarInsn(ALOAD, 6);
            mv.visitVarInsn(ALOAD, 7);
            mv.visitTypeInsn(CHECKCAST, actionListenerInterfaceInternalName);
            
            mv.visitMethodInsn(
                INVOKESTATIC,
                tooltipMethodData[0],
                tooltipMethodData[1],
                tooltipMethodData[2],
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public void buttonSetListener(Object button, Object listener) {
        //     ((buttonClass)button).setListener(listener);
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "buttonSetListener",
                "(Ljava/lang/Object;Ljava/lang/Object;)V",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, buttonClassInternalName);

            mv.visitVarInsn(ALOAD, 2);
            mv.visitTypeInsn(CHECKCAST, actionListenerInterfaceInternalName);
            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                buttonClassInternalName,
                "setListener",
                "(" + actionListenerInterfaceDesc + ")V",
                false
            );

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public Object buttonGetListener(Object button) {
        //     ((buttonClass)button).getListener();
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "buttonGetListener",
                "(Ljava/lang/Object;)Ljava/lang/Object;",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, buttonClassInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                buttonClassInternalName,
                "getListener",
                "()" + actionListenerInterfaceDesc,
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public Object buttonGetRenderer(Object button) {
        //     ((buttonClass)button).getRenderer();
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "buttonGetRenderer",
                "(Ljava/lang/Object;)Ljava/lang/Object;",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, buttonClassInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                buttonClassInternalName,
                "getRenderer",
                "()" + Type.getDescriptor(Refl.getReturnType(Refl.getMethod("getRenderer", buttonClass))),
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public PositionAPI uiPanelAdd(UIPanelAPI panel, UIComponentAPI component) {
        //     return ((uiPanelClass)panel).add((componentClass)component);
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "uiPanelAdd",
                "(" + uiPanelAPIDesc + uiComponentAPIDesc + ")" + positionAPIDesc,
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, uiPanelInternalName);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitTypeInsn(CHECKCAST, uiComponentInterfaceAInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                uiPanelInternalName,
                "add",
                "(" + uiComponentInterfaceADesc + ")" + positionDesc,
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public void positionSet(PositionAPI pos, PositionAPI toSet) {
        //     return ((positionClass)pos).set((positionClass)toSet);
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "positionSet",
                "(" + positionAPIDesc + positionAPIDesc + ")V",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, positionInternalName);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitTypeInsn(CHECKCAST, positionInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                positionInternalName,
                "set",
                "(" + positionDesc + ")V",
                false
            );

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public Object getTooltip(Object uiComponent) {
        //     ((uiComponentClass)uiComponent).getTooltip();
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "getTooltip",
                "(Ljava/lang/Object;)Ljava/lang/Object;",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, uiComponentInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                uiComponentInternalName,
                "getTooltip",
                "()" + tooltipDesc,
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public void showTooltip(Object uiComponent, Object tooltip) {
        //     ((showToolTipInterface)uiComponent).showTooltip(tooltip);
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "showTooltip",
                "(Ljava/lang/Object;Ljava/lang/Object;)V",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, showTooltipInterfaceInternalName);

            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(
                INVOKEINTERFACE,
                showTooltipInterfaceInternalName,
                "showTooltip",
                "(Ljava/lang/Object;)V",
                true // interface method
            );

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public void hideTooltip(Object uiComponent, Object tooltip) {
        //     ((showToolTipInterface)uiComponent).hideTooltip(tooltip);
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "hideTooltip",
                "(Ljava/lang/Object;Ljava/lang/Object;)V",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, showTooltipInterfaceInternalName);

            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(
                INVOKEINTERFACE,
                showTooltipInterfaceInternalName,
                "hideTooltip",
                "(Ljava/lang/Object;)V",
                true // interface method
            );

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public void uiComponentsetTooltipOffsetFromCenter(Object uiComponent, float xPad, float yPad) {
        //     ((uiComponentClass)uiComponent).setTooltipOffsetFromCenter(xPad, yPad);
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "uiComponentsetTooltipOffsetFromCenter",
                "(Ljava/lang/Object;FF)V",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, uiComponentInternalName);
            mv.visitVarInsn(FLOAD, 2);
            mv.visitVarInsn(FLOAD, 3);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                uiComponentInternalName,
                "setTooltipOffsetFromCenter",
                "(FF)V",
                false
            );

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public void setTooltip(Object uiComponent, float var1, Object tooltip) {
        //     ((setTooltipInterface)uiComponent).setTooltip(var1, tooltip);
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "setTooltip",
                "(Ljava/lang/Object;FLjava/lang/Object;)V",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, setTooltipInterfaceInternalName);
            mv.visitVarInsn(FLOAD, 2);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitTypeInsn(CHECKCAST, toolTipInternalName);

            mv.visitMethodInsn(
                INVOKEINTERFACE,
                setTooltipInterfaceInternalName,
                "setTooltip",
                "(F" + tooltipDesc + ")V",
                true // interface method
            );

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public void setTooltipPositionRelativeToAnchor(Object uiComponent, float xPad, float yPad, Object anchor) {
        //     ((setTooltipInterface)uiComponent).setTooltipPositionRelativeToAnchor(xPad, yPad, (uiComponentClass)anchor);
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "setTooltipPositionRelativeToAnchor",
                "(Ljava/lang/Object;FFLjava/lang/Object;)V",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, setTooltipInterfaceInternalName);
            mv.visitVarInsn(FLOAD, 2);
            mv.visitVarInsn(FLOAD, 3);
            mv.visitVarInsn(ALOAD, 4);
            mv.visitTypeInsn(CHECKCAST, uiComponentInterfaceAInternalName);

            mv.visitMethodInsn(
                INVOKEINTERFACE,
                setTooltipInterfaceInternalName,
                "setTooltipPositionRelativeToAnchor",
                "(FF" + uiComponentInterfaceADesc + ")V",
                true // interface method
            );

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public UIPanelAPI getContents(Object tooltip) {
        //     return tooltip.getContents();
        // }
        {   
            String returnDesc = Type.getDescriptor(Refl.getReturnType(Refl.getMethod("getContents", StandardTooltipV2.class)));
            String standardToolTipV2InternalName = Type.getInternalName(StandardTooltipV2.class);
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "getContents",
                "(Ljava/lang/Object;)" + uiPanelAPIDesc,
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, standardToolTipV2InternalName);
            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                standardToolTipV2InternalName,
                "getContents",
                "()" + returnDesc,
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public UIPanelAPI tooltipGetCustom(Object tooltip) {
        //     return tooltip.getCustom();
        // }
        {   
            String returnDesc = Type.getDescriptor(Refl.getReturnType(Refl.getMethod("getCustom", StandardTooltipV2.class)));
            String standardToolTipV2InternalName = Type.getInternalName(StandardTooltipV2.class);
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "tooltipGetCustom",
                "(Ljava/lang/Object;)" + uiPanelAPIDesc,
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, standardToolTipV2InternalName);
            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                standardToolTipV2InternalName,
                "getCustom",
                "()" + returnDesc,
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public UIPanelAPI labelGetParent(Object label) {
        //     return ((labelClass)label).getParent();
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "labelGetParent",
                "(Ljava/lang/Object;)" + uiPanelAPIDesc,
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, labelInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                labelInternalName,
                "getParent",
                "()" + uiPanelClassDesc,
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public UIPanelAPI getParent(Object uiComponent) {
        //     return ((uiComponentClass)uiComponent).getParent();
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "getParent",
                "(Ljava/lang/Object;)" + uiPanelAPIDesc,
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, uiComponentInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                uiComponentInternalName,
                "getParent",
                "()" + uiPanelClassDesc,
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public List<UIComponentAPI> getChildrenNonCopy(UIComponentAPI parent) {
        //     if (parent instanceof uiPanelClass) {
        //         return ((uiPanelClass)parent).getChildrenNonCopy();
        //     }
        //     return null;
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "getChildrenNonCopy",
                "(" + Type.getDescriptor(UIComponentAPI.class) + ")Ljava/util/List;",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            
            mv.visitTypeInsn(INSTANCEOF, uiPanelInternalName);
            
            Label ifInstanceOf = new Label();
            mv.visitJumpInsn(IFNE, ifInstanceOf);
            
            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ARETURN);
            
            mv.visitLabel(ifInstanceOf);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, uiPanelInternalName);
            
            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                uiPanelInternalName,
                "getChildrenNonCopy",
                "()Ljava/util/List;",
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public List<UIComponentAPI> getChildrenNonCopy(Object uiPanel) {
        //     return ((uiPanelClass)uiPanel).getChildrenNonCopy();
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "getChildrenNonCopy",
                "(" + uiPanelAPIDesc + ")Ljava/util/List;",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, uiPanelInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                uiPanelInternalName,
                "getChildrenNonCopy",
                "()Ljava/util/List;",
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public List<Object> uiTableGetRows(UITable table) {
        //     return table.getRows();
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "uiTableGetRows",
                "(" + uiTableDesc + ")" + listDesc,
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                uiTableInternalName,
                "getRows",
                "()" + listDesc,
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public void uiTableAddRow(UITable table, Object row) {
        //     table.addRow(row);
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "uiTableAddRow",
                "(" + uiTableDesc + "Ljava/lang/Object;)V",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitTypeInsn(CHECKCAST, uiTableRowInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                uiTableInternalName,
                "addRow",
                "(" + uiTableRowDesc + ")V",
                false
            );

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public void uiTableRemoveRow(UITable table, Object row) {
        //     table.removeRow(row);
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "uiTableRemoveRow",
                "(" + uiTableDesc + "Ljava/lang/Object;)V",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitTypeInsn(CHECKCAST, uiTableRowInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                uiTableInternalName,
                "removeRow",
                "(" + uiTableRowDesc + ")V",
                false
            );

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public Object uiTableGetRowForData(UITable table, Object data) {
        //     return table.getRowForData(data);
        // }
        {
            Object getRowForDataMethod = Refl.getMethod("getRowForData", UITable.class);
            String getRowForDataDesc = Type.getMethodDescriptor(getRowForDataMethod);
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "uiTableGetRowForData",
                "(" + uiTableDesc + "Ljava/lang/Object;)Ljava/lang/Object;",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 2);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                uiTableInternalName,
                "getRowForData",
                getRowForDataDesc,
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public UIPanelAPI uiTableRowGetCol(Object row, int col) {
        //     return ((UITableRow)row).getCol(col);
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "uiTableRowGetCol",
                "(Ljava/lang/Object;I)" + uiPanelAPIDesc,
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, uiTableRowInternalName);

            mv.visitVarInsn(ILOAD, 2);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                uiTableRowInternalName,
                "getCol",
                "(I)" + uiPanelClassDesc,
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public ButtonAPI uiTableRowGetButton(Object row) {
        //     return ((UITableRow)row).getButton();
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "uiTableRowGetButton",
                "(Ljava/lang/Object;)" + buttonAPIDesc,
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, uiTableRowInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                uiTableRowInternalName,
                "getButton",
                "()" + buttonClassDesc,
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public void uiTableRowSetButton(Object row, Object button) {
        //     ((UITableRow)row).setButton((Button)button);
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "uiTableRowSetButton",
                "(Ljava/lang/Object;Ljava/lang/Object;)V",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, uiTableRowInternalName);

            mv.visitVarInsn(ALOAD, 2);
            mv.visitTypeInsn(CHECKCAST, buttonClassInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                uiTableRowInternalName,
                "setButton",
                "(" + buttonClassDesc + ")V",
                false
            );

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public Object uiTableRowGetData(Object row) {
        //     return ((UITableRow)row).getData();
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "uiTableRowGetData",
                "(Ljava/lang/Object;)Ljava/lang/Object;",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, uiTableRowInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                uiTableRowInternalName,
                "getData",
                "()Ljava/lang/Object;",
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public void uiTableRowRender(Object row, float alphaMult) {
        //     ((uiTableRowSuperClass)row).render(alphaMult);
        // }
        {
            String superInternalName = Type.getInternalName(uiTableRowSubClass.getSuperclass());
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "uiTableRowRender",
                "(Ljava/lang/Object;F)V",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, superInternalName);
            mv.visitVarInsn(FLOAD, 2);
            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                superInternalName,
                "render",
                "(F)V",
                false
            );

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public void uiTableRowSetData(Object row, Object data) {
        //     ((UITableRow)row).setData(data);
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "uiTableRowSetData",
                "(Ljava/lang/Object;Ljava/lang/Object;)V",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, uiTableRowInternalName);

            mv.visitVarInsn(ALOAD, 2);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                uiTableRowInternalName,
                "setData",
                "(Ljava/lang/Object;)V",
                false
            );

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public Object uiTableGetSelected(UITable table) {
        //     return table.getSelected();
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "uiTableGetSelected",
                "(" + uiTableDesc + ")Ljava/lang/Object;" ,
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                uiTableInternalName,
                "getSelected",
                "()" + uiTableRowDesc,
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public void uiTableSelect(UITable table, Object row, Object inputEvent) {
        //     table.select(row, inputEvent);
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "uiTableSelect",
                "(" + uiTableDesc + "Ljava/lang/Object;Ljava/lang/Object;)V",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);

            mv.visitVarInsn(ALOAD, 2);
            mv.visitTypeInsn(CHECKCAST, uiTableRowInternalName);
            mv.visitVarInsn(ALOAD, 3);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                uiTableInternalName,
                "select",
                "(" + uiTableRowDesc + "Ljava/lang/Object;)V",
                false
            );

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public void uiTableSelect(UITable table, Object row, Object inputEvent, boolean notifyDelegate) {
        //     table.select(row, inputEvent, notifyDelegate);
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "uiTableSelect",
                "(" + uiTableDesc + "Ljava/lang/Object;Ljava/lang/Object;Z)V",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);

            mv.visitVarInsn(ALOAD, 2);
            mv.visitTypeInsn(CHECKCAST, uiTableRowInternalName);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitVarInsn(ILOAD, 4);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                uiTableInternalName,
                "select",
                "(" + uiTableRowDesc + "Ljava/lang/Object;Z)V",
                false
            );

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public UIPanelAPI uiTableGetList(UITable table) {
        //     return table.getList();
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "uiTableGetList",
                "(" + uiTableDesc + ")" + uiPanelAPIDesc,
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                uiTableInternalName,
                "getList",
                "()" + listPanelDesc,
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public Map<ButtonAPI, Object> optionPanelGetButtonToItemMap(OptionPanelAPI optionPanel) {
        //     return ((optionPanelClass)optionPanel).getButtonToItemMap();
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "optionPanelGetButtonToItemMap",
                "(" + optionPanelApiDesc + ")" + mapDesc,
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, optionPanelInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                optionPanelInternalName,
                "getButtonToItemMap",
                "()" + mapDesc,
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public Object optionPanelItemGetOptionData(Object optionPanelItem) {
        //     return ((optionPanelItemClass)optionPanelItem).getOptionData();
        // }
        {
            String methodName = Refl.getMethodName(Refl.getMethodsByReturnType(optionPanelItemClass, Object.class).get(0));
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "optionPanelItemGetOptionData",
                "(Ljava/lang/Object;)Ljava/lang/Object;",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, optionPanelItemInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                optionPanelItemInternalName,
                methodName,
                "()" + Type.getDescriptor(Object.class),
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        
        cw.visitEnd();
        Class<?> utilClass = cl.define(cw.toByteArray(), "data.scripts.util.UtilInterface");

        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        // public class UiInstantiator implements this crap
        cw.visit(
            V17,
            ACC_PUBLIC,
            "data/scripts/util/UiInstantiator",
            null,
            superName,
            new String[] {Type.getInternalName(UiInstantiator.class)}
        );

        ctor = cw.visitMethod(
            ACC_PUBLIC,
            "<init>",
            "()V",
            null,
            null
        );
        ctor.visitCode();
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitMethodInsn(INVOKESPECIAL, superName, "<init>", "()V", false);
        ctor.visitInsn(RETURN);
        ctor.visitMaxs(0, 0);
        ctor.visitEnd();

        // {
        //     // public UIPanelAPI instantiateProgressBar(String text, float rangeMin, float rangeMax)
        //     MethodVisitor mv = cw.visitMethod(
        //         ACC_PUBLIC,
        //         "instantiateProgressBar",
        //         "(Ljava/lang/String;FF)" + uiPanelAPIDesc,
        //         null,
        //         null
        //     );
            
        //     mv.visitCode();
            
        //     mv.visitTypeInsn(NEW, progressBarInternalName);
        //     mv.visitInsn(DUP);
            
        //     mv.visitVarInsn(ALOAD, 1);
        //     mv.visitVarInsn(FLOAD, 2);
        //     mv.visitVarInsn(FLOAD, 3);
            
        //     mv.visitMethodInsn(
        //         INVOKESPECIAL,
        //         progressBarInternalName,
        //         "<init>",
        //         "(Ljava/lang/String;FF)V",
        //         false
        //     );
            
        //     mv.visitInsn(ARETURN);
            
        //     mv.visitMaxs(0, 0);
        //     mv.visitEnd();
        // }

        {
            // public UIPanelAPI instantiateUiPanel(float width, float height)
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "instantiateProgressBar",
                "(FF)" + uiPanelAPIDesc,
                null,
                null
            );
            
            mv.visitCode();
            
            mv.visitTypeInsn(NEW, uiPanelInternalName);
            mv.visitInsn(DUP);
            
            mv.visitVarInsn(FLOAD, 1);
            mv.visitVarInsn(FLOAD, 2);
            mv.visitMethodInsn(
                INVOKESPECIAL,
                uiPanelInternalName,
                "<init>",
                "(Ljava/lang/String;FF)V",
                false
            );
            
            mv.visitInsn(ARETURN);
            
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        {
            // public UIPanelAPI instantiateConfirmDialog(float width, float height, UIPanelAPI parent, Object dialogDismissedListener, String titleLabelText, String... buttonTexts)
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "instantiateConfirmDialog",
                "(FF" + uiPanelAPIDesc + "Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/String;)" + uiPanelAPIDesc,
                null,
                null
            );
        
            mv.visitCode();
        
            mv.visitTypeInsn(NEW, confirmDialogInternalName);
            mv.visitInsn(DUP);
        
            mv.visitVarInsn(FLOAD, 1);
            mv.visitVarInsn(FLOAD, 2);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitVarInsn(ALOAD, 4);
            mv.visitVarInsn(ALOAD, 5);
            mv.visitVarInsn(ALOAD, 6);
        
            mv.visitMethodInsn(
                INVOKESPECIAL,
                confirmDialogInternalName,
                "<init>",
                "(FF" + uiPanelClassDesc + dialogDismissedInterfaceDesc + "Ljava/lang/String;[Ljava/lang/String;)V",
                false
            );
        
            mv.visitInsn(ARETURN);
        
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        
        {
            // public UIPanelAPI instantiateFleetMemberItem(FleetMemberAPI member, UIPanelAPI fleetPanel)
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "instantiateFleetMemberItem",
                "(" + Type.getDescriptor(FleetMemberAPI.class) + uiPanelAPIDesc + ")" + uiPanelAPIDesc,
                null,
                null
            );
        
            mv.visitCode();
        
            mv.visitTypeInsn(NEW, Type.getInternalName(fleetPanelItemClass));
            mv.visitInsn(DUP);
        
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(FleetMember.class));

            mv.visitFieldInsn(GETSTATIC, fleetPanelInternalName, "FM_ITEM_WIDTH", "F");
            mv.visitFieldInsn(GETSTATIC, fleetPanelInternalName, "FM_ITEM_HEIGHT", "F");

            mv.visitVarInsn(ALOAD, 2);
            mv.visitTypeInsn(CHECKCAST, fleetPanelInternalName);
        
            mv.visitMethodInsn(
                INVOKESPECIAL,
                Type.getInternalName(fleetPanelItemClass),
                "<init>",
                "(" + Type.getDescriptor(FleetMember.class) + "FF" + fleetPanelDesc + ")V",
                false
            );
        
            mv.visitInsn(ARETURN);
        
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        {
            // public InputEventAPI instantiateInputEvent(InputEventClass eventClass, InputEventType eventType, int x, int y, int value, char c)
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "instantiateInputEvent",
                "(" + inputEventClassDesc + inputEventTypeDesc + "IIIC)" + inputEventAPIDesc,
                null,
                null
            );
        
            mv.visitCode();
        
            mv.visitTypeInsn(NEW, inputEventInternalName);
            mv.visitInsn(DUP);
        
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitVarInsn(ILOAD, 3);
            mv.visitVarInsn(ILOAD, 4);
            mv.visitVarInsn(ILOAD, 5);
            mv.visitVarInsn(ILOAD, 6);
        
            mv.visitMethodInsn(
                INVOKESPECIAL,
                inputEventInternalName,
                "<init>",
                "(" + inputEventClassDesc + inputEventTypeDesc + "IIIC)V",
                false
            );
        
            mv.visitInsn(ARETURN);
        
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        {
            // public List<InputEventAPI> instantiateInputEventList()
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "instantiateInputEventList",
                "()" + listDesc,
                null,
                null
            );
        
            mv.visitCode();
        
            mv.visitTypeInsn(NEW, inputEventListInternalName);
            mv.visitInsn(DUP);
        
            mv.visitMethodInsn(
                INVOKESPECIAL,
                inputEventListInternalName,
                "<init>",
                "()V",
                false
            );
        
            mv.visitInsn(ARETURN);
        
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        cw.visitEnd();

        Class<?> uiInstantiatorClass = cl.define(cw.toByteArray(), "data.scripts.util.UiInstantiator");

        return new Class<?>[] {
            utilClass,
            uiInstantiatorClass,
            uiPanelClass,
            uiComponentClass,
            toolTipClass,
            fleetTabClass,
            confirmDialogHoloClass,
            shipIconListClass,
            listPanelClass,
            shipIconRendererClass,
            uiTableRowSubClass,
            dialogDismissedInterface,
            confirmDialogClass,
            inputEventClass
        };
    }

    public static final UtilInterface utils;
    public static final UiInstantiator instantiator;

    public static final Class<?> uiPanelClass;
    public static final Class<?> uiComponentClass;
    public static final Class<?> confirmDialogClass;

    private static final VarHandle fleetTabLeftPaneHandle;
    private static final VarHandle officerAutoAssignButtonHandle;

    public static final VarHandle confirmDialogHoloNoiseRendererHandle;
    public static final VarHandle confirmDialogHoloNoiseFaderHandle;
    public static final VarHandle confirmDialogHoloNoiseColorHandle;

    public static final VarHandle listPanelMapHandle;

    public static final VarHandle uiTableRowParamsHandle;
    public static final VarHandle uiTableRowCreatedHandle;

    public static final VarHandle shipIconListListPanelHandle;
    public static final VarHandle shipIconRendererHighlightColorHandle;
    public static final VarHandle shipIconRendererFleetMemberHandle;

    public static final VarHandle uiComponentTooltipHandle;

    public static final VarHandle playerPaidToUnlockStorageHandle;

    private static final CallSite actionListenerCallSite;
    private static final CallSite dialogDismissedCallSite;

    public static final MethodHandle outsideClickAbsorbHandle; // damn protected method

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        VarHandle handle = null;
        VarHandle handle2 = null;

        try {
            playerPaidToUnlockStorageHandle = MethodHandles.privateLookupIn(StoragePlugin.class, lookup).findVarHandle(
                StoragePlugin.class,
                "playerPaidToUnlock",
                boolean.class
            );

            Class<?> buttonClass = Global.getSettings().createCustom(0f,0f,null).createUIElement(0f,0f,false).addButton("",null,0f,0f,0f).getClass();
            Class<?> actionListenerInterface = Refl.getReturnType(Refl.getMethod("getListener", buttonClass));

            Class<?> coreClass = Refl.getReturnType(Refl.getMethod("getCore", CampaignState.class));

            int i = 0;
            Class<?>[] result = implementUtilInterface(coreClass, buttonClass, actionListenerInterface);

            utils = (UtilInterface) Refl.instantiateClass(result[i++].getConstructors()[0]);
            instantiator = (UiInstantiator) Refl.instantiateClass(result[i++].getConstructors()[0]);

            uiPanelClass = result[i++];
            uiComponentClass = result[i++];
            Class<?> tooltipClass = result[i++];

            for (Object field : uiComponentClass.getDeclaredFields()) {
                if (Refl.getFieldType(field) == tooltipClass) {
                    handle = MethodHandles.privateLookupIn(uiComponentClass, lookup).findVarHandle(
                        uiComponentClass,
                        Refl.getFieldName(field),
                        tooltipClass
                    );
                }
            }
            uiComponentTooltipHandle = handle;

            Class<?> fleetTabClass = result[i++];
            Class<?> fleetTabLeftPaneClass = null;

            for (Object field : fleetTabClass.getDeclaredFields()) {
                Class<?> fieldType = Refl.getFieldType(field);

                if (fieldType.getSuperclass() == uiPanelClass) {
                    Object ctor = fieldType.getConstructors()[0];

                    if (Refl.getConstructorParamTypes(ctor)[0] == coreClass) {
                        handle = MethodHandles.privateLookupIn(fleetTabClass, lookup).findVarHandle(
                            fleetTabClass,
                            Refl.getFieldName(field),
                            fieldType
                        );
                        fleetTabLeftPaneClass = fieldType;
                        break;
                    }
                }
            }
            fleetTabLeftPaneHandle = handle;

            String officerAutoAssignButtonFieldName = getOfficerAutoAssignButtonFieldName(fleetTabLeftPaneClass);
            officerAutoAssignButtonHandle = MethodHandles.privateLookupIn(fleetTabLeftPaneClass, lookup).findVarHandle(
                fleetTabLeftPaneClass,
                officerAutoAssignButtonFieldName,
                buttonClass
            );

            Class<?> holoClass = result[i++];
            Class<?> holoNoiseRendererClass = getHoloNoiseRendererClass(holoClass);
            String holoNoiseColorName = getHoloNoiseColorFieldName(holoNoiseRendererClass);

            handle = null;
            for (Object field : holoClass.getDeclaredFields()) {
                Class<?> fieldType = Refl.getFieldType(field);

                if (fieldType == holoNoiseRendererClass) {
                    handle = MethodHandles.privateLookupIn(holoClass, lookup).findVarHandle(
                        holoClass,
                        Refl.getFieldName(field),
                        holoNoiseRendererClass
                    );
                }
            }
            confirmDialogHoloNoiseRendererHandle = handle;

            MethodHandles.Lookup privateLookup = MethodHandles.privateLookupIn(holoNoiseRendererClass, lookup);
            VarHandle faderHandle = null;
            handle = null;
            for (Object field : holoNoiseRendererClass.getDeclaredFields()) {
                Class<?> fieldType = Refl.getFieldType(field);

                if (fieldType == Color.class) {
                    String fieldName = Refl.getFieldName(field);
                    if (fieldName.equals(holoNoiseColorName)) {
                        handle = privateLookup.findVarHandle(
                            holoNoiseRendererClass,
                            fieldName,
                            Color.class
                        );
                    }
                } else if (fieldType == Fader.class) {
                    faderHandle = privateLookup.findVarHandle(
                        holoNoiseRendererClass,
                        Refl.getFieldName(field),
                        Fader.class
                    );
                }
            }
            confirmDialogHoloNoiseColorHandle = handle;
            confirmDialogHoloNoiseFaderHandle = faderHandle;

            Class<?> shipIconListClass = result[i++];
            Class<?> listPanelClass = result[i++];
            handle = null;
            for (Object field : shipIconListClass.getDeclaredFields()) {
                if (Refl.getFieldType(field) == listPanelClass) {
                    handle = MethodHandles.privateLookupIn(shipIconListClass, lookup).findVarHandle(
                        shipIconListClass,
                        Refl.getFieldName(field),
                        listPanelClass
                    );
                    break;
                }
            }
            shipIconListListPanelHandle = handle;
            handle = null;
            for (Object field : listPanelClass.getDeclaredFields()) {
                if (Refl.getFieldType(field) == Map.class) {
                    handle = MethodHandles.privateLookupIn(listPanelClass, lookup).findVarHandle(
                        listPanelClass,
                        Refl.getFieldName(field),
                        Map.class
                    );
                    break;
                }
            }
            listPanelMapHandle = handle;

            Class<?> shipIconRendererClass = result[i++];
            String shipIconRendererHighlightColorName = getShipIconHighlightColorFieldName(shipIconRendererClass);
            handle = null;
            handle2 = null;
            privateLookup = MethodHandles.privateLookupIn(shipIconRendererClass, lookup);
            for (Object field : shipIconRendererClass.getDeclaredFields()) {
                String fieldName = Refl.getFieldName(field);
                Class<?> fieldType = Refl.getFieldType(field);

                if (fieldName.equals(shipIconRendererHighlightColorName)) {
                    handle = privateLookup.findVarHandle(
                        shipIconRendererClass,
                        fieldName,
                        Color.class
                    );
                } else if (fieldType == FleetMember.class) {
                    handle2 = privateLookup.findVarHandle(
                        shipIconRendererClass,
                        fieldName,
                        fieldType
                    );
                }
            }
            shipIconRendererHighlightColorHandle = handle;
            shipIconRendererFleetMemberHandle = handle2;
            handle = null;
            Class<?> uiTableRowClass = result[i++];
            for (Object field : uiTableRowClass.getDeclaredFields()) {;
                if (Refl.getFieldType(field) == Object[].class) {
                    handle = MethodHandles.privateLookupIn(uiTableRowClass, lookup).findVarHandle(
                        uiTableRowClass,
                        Refl.getFieldName(field),
                        Object[].class
                    );
                    break;
                }
            }
            uiTableRowParamsHandle = handle;
            uiTableRowCreatedHandle = MethodHandles.privateLookupIn(uiTableRowClass.getSuperclass(), lookup).findVarHandle(
                uiTableRowClass.getSuperclass(),
                "created",
                boolean.class
            );
            
            MethodType factoryType = MethodType.methodType(actionListenerInterface, ActionListenerProxy.class);
            MethodType actualSamMethodType = MethodType.methodType(void.class, Object.class, Object.class);
            MethodType implSignature = MethodType.methodType(void.class, Object.class, Object.class);
            MethodHandle implementationMethodHandle = lookup.findVirtual(ActionListenerProxy.class, "actionPerformed", implSignature);

            actionListenerCallSite = LambdaMetafactory.metafactory(
                lookup,
                "actionPerformed",
                factoryType,
                actualSamMethodType,
                implementationMethodHandle,
                actualSamMethodType
            );

            Class<?> dialogDismissedInterface = result[i++];
            Class<?> dialogDismissedParamClass = Refl.getMethodParamTypes(dialogDismissedInterface.getDeclaredMethods()[0])[0];
            confirmDialogClass = result[i++];

            actualSamMethodType = MethodType.methodType(void.class, dialogDismissedParamClass, int.class);
            implementationMethodHandle = lookup.findVirtual(DialogDismissedListenerProxy.class, "dialogDismissed", MethodType.methodType(void.class, Object.class, int.class));
            factoryType = MethodType.methodType(dialogDismissedInterface, DialogDismissedListenerProxy.class);

            dialogDismissedCallSite = LambdaMetafactory.metafactory(
                lookup,
                "dialogDismissed",
                factoryType,
                actualSamMethodType,
                implementationMethodHandle,
                actualSamMethodType
            );

            Class<?> inputEventClass = result[i++];
            outsideClickAbsorbHandle = MethodHandles.privateLookupIn(confirmDialogClass, lookup).findVirtual(
                confirmDialogClass,
                "outsideClickAbsorbed",
                MethodType.methodType(void.class, inputEventClass)
            );

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getCore(Object campaignUI, Object interactionDialog) {
        return interactionDialog == null ? utils.campaignUIgetCore(campaignUI) : utils.interactionDialogGetCore(interactionDialog);
    }

    public static UIPanelAPI getFleetTabLeftPane(UIPanelAPI fleetTab) {
        return (UIPanelAPI) fleetTabLeftPaneHandle.get(fleetTab);
    }

    public static ButtonAPI getOfficerAutoAssignButton(Object fleetTabLeftPane) {
        return (ButtonAPI) officerAutoAssignButtonHandle.get(fleetTabLeftPane);
    }

    public static void setButtonHook(ButtonAPI button, Runnable runBefore, Runnable runAfter) {
        Object oldListener = utils.buttonGetListener(button);
        utils.buttonSetListener(button, new ActionListener() {
            @Override
            public void actionPerformed(Object inputEvent, Object uiElement) {
                runBefore.run();
                utils.actionPerformed(oldListener, inputEvent, uiElement);
                runAfter.run();
            }
        }.getProxy());
    }

    public static void setButtonBeforeHook(ButtonAPI button, Runnable runBefore) {
        Object oldListener = utils.buttonGetListener(button);
        utils.buttonSetListener(button, new ActionListener() {
            @Override
            public void actionPerformed(Object inputEvent, Object uiElement) {
                runBefore.run();
                utils.actionPerformed(oldListener, inputEvent, uiElement);
            }
        }.getProxy());
    }

    public static void setButtonAfterHook(ButtonAPI button, Runnable runAfter) {
        Object oldListener = utils.buttonGetListener(button);
        utils.buttonSetListener(button, new ActionListener() {
            @Override
            public void actionPerformed(Object inputEvent, Object uiElement) {
                utils.actionPerformed(oldListener, inputEvent, uiElement);
                runAfter.run();
            }
        }.getProxy());
    }

    public static boolean isInBounds(PositionAPI pos, float mouseX, float mouseY) {
        float leftBound = pos.getCenterX() - pos.getWidth() / 2;
        float rightBound = pos.getCenterX() + pos.getWidth() / 2;
        float topBound = pos.getCenterY() - pos.getHeight() / 2;
        float bottomBound = pos.getCenterY() + pos.getHeight() / 2;

        return mouseX >= leftBound && mouseX <= rightBound &&
               mouseY >= topBound && mouseY <= bottomBound;
    }

    public static void test() {
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        Object core = getCore(campaignUI, campaignUI.getCurrentInteractionDialog());
        UIPanelAPI currentTab = utils.coreGetCurrentTab(core);

        FactionSpecAPI factionSpec = Global.getSector().getPlayerFaction().getFactionSpec();

        Map<Color, Color> colorMap = new HashMap<>();
        colorMap.put(factionSpec.getBaseUIColor(), Color.RED);
        colorMap.put(factionSpec.getBrightUIColor(), Color.GREEN);
        colorMap.put(factionSpec.getColor(), Color.PINK);
        colorMap.put(factionSpec.getSecondaryUIColor(), Color.BLUE);

        factionSpec.setBaseUIColor(Color.RED);
        factionSpec.setBrightUIColor(Color.GREEN);
        factionSpec.setColor(Color.PINK);
        factionSpec.setSecondaryUIColor(Color.BLUE);

        UIPanelAPI screenPanel = (UIPanelAPI) Refl.getMethodAndInvokeDirectly("getDialogParentForSubDialog", core);
        List<UIComponentAPI> children = utils.getChildrenNonCopy(screenPanel);

        com.fs.starfarer.campaign.ui.marketinfo.o0Oo dialog = (com.fs.starfarer.campaign.ui.marketinfo.o0Oo) children.get(children.size()-1);

        List<Boolean> btnsChecked = new ArrayList<>();
        String text = null;
        for (TreeNode node : new TreeTraverser(dialog).getNodes()) {
            for (UIComponentAPI c : node.getChildren()) {
                if (c instanceof TextFieldAPI textPanel) text = textPanel.getText();
                else if (c instanceof ButtonAPI btn) btnsChecked.add(btn.isChecked());
            }
        }

        dialog.createUI();
        dialog.show(0,0);

        int i = 0;
        for (TreeNode node : new TreeTraverser(dialog).getNodes()) {
            for (UIComponentAPI c : node.getChildren()) {
                if (c instanceof TextFieldAPI textPanel) textPanel.setText(text);
                else if (c instanceof ButtonAPI btn) btn.setChecked(btnsChecked.get(i++));
            }
        }
    }

    public static List<UIComponentAPI> getChildrenRecursive(UIComponentAPI parentPanel) {
        List<UIComponentAPI> list = new ArrayList<>();
        collectChildren(parentPanel, list);
        return list;
    }

    private static void collectChildren(UIComponentAPI parent, List<UIComponentAPI> list) {
        List<UIComponentAPI> children = utils.getChildrenNonCopy(parent);

        if (children != null) {
            for (UIComponentAPI child : children) {
                list.add(child);
                collectChildren(child, list);
            }
        }
    }

    public static abstract class ActionListener {
        private final Object listener;
    
        public ActionListener() {
            try {
                listener = actionListenerCallSite.getTarget().invoke(new ActionListenerProxy(this));
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    
        public abstract void actionPerformed(Object inputEvent, Object uiElement);
        
        public final Object getProxy() {
            return listener;
        }
    }

    public static abstract class DialogDismissedListener {
        protected final Object listener;

        public DialogDismissedListener() {
            try {
                listener = dialogDismissedCallSite.getTarget().invoke(new DialogDismissedListenerProxy(this));
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        public abstract void dialogDismissed(Object arg0, int arg1);

        public Object getProxy() {
            return this.listener;
        }
    }

    private static class ActionListenerProxy {
        private final ActionListener proxyTriggerClassInstance;

        public ActionListenerProxy(ActionListener proxyTriggerClassInstance) {
            this.proxyTriggerClassInstance = proxyTriggerClassInstance;
        }

        @SuppressWarnings("unused")
        // @Override 
        public void actionPerformed(Object arg0, Object arg1) {
            proxyTriggerClassInstance.actionPerformed(arg0, arg1);
        }
    }

    private static class DialogDismissedListenerProxy {
        private final DialogDismissedListener listener;

        public DialogDismissedListenerProxy(DialogDismissedListener listener) {
            this.listener = listener;
        }
        @SuppressWarnings("unused")
        // @Override
        public void dialogDismissed(Object arg0, int arg1) {
            this.listener.dialogDismissed(arg0, arg1);
        };
    }

    private static int computeBufferSize(Object inputStream, Object inputStreamAvailableMethod) {
        try {
            int expectedLength = (int) Refl.invokeMethodDirectly(inputStreamAvailableMethod, inputStream);

            if (expectedLength < 256) {
              return 4096;
            }
            return Math.min(expectedLength, 1024 * 1024);

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
      }

    private static byte[] readStream(Object inputStream) {
        try {
            Class<?> baosClass = Class.forName("java.io.ByteArrayOutputStream", false, Class.class.getClassLoader());
            Class<?> inputStreamClass = Class.forName("java.io.InputStream", false, Class.class.getClassLoader());

            Object baosCtor = Refl.getConstructor(baosClass, new Class<?>[0]);
            Object baosWriteMethod = Refl.getMethodExplicit("write", baosClass, new Class<?>[]{byte[].class, int.class, int.class});
            Object outputStreamFlushMethod = Refl.getMethod("flush", baosClass.getSuperclass());
            Object baosToByteArrayMethod = Refl.getMethod("toByteArray", baosClass);
    
            Object inputStreamAvailableMethod = Refl.getMethod("available", inputStreamClass);
            Object inputStreamReadMethod = Refl.getMethodExplicit("read", inputStreamClass, new Class<?>[] {byte[].class, int.class, int.class});

            int bufferSize = computeBufferSize(inputStream, inputStreamAvailableMethod);
            Object outputStream = Refl.instantiateClass(baosCtor);

            byte[] data = new byte[bufferSize];
            int bytesRead;
            int readCount = 0;

            while ((bytesRead = (int) Refl.invokeMethodDirectly(inputStreamReadMethod, inputStream, data, 0, bufferSize)) != -1) {
                Refl.invokeMethodDirectly(baosWriteMethod, outputStream, data, 0, bytesRead);
                readCount++;
            }
            
            Refl.invokeMethodDirectly(outputStreamFlushMethod, outputStream);
            if (readCount == 1) {
                return data;
            }
            return (byte[]) Refl.invokeMethodDirectly(baosToByteArrayMethod, outputStream);

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static Class<?> getFleetTabClass(Class<?> coreClass, Class<?> actionListenerInterface) throws ClassNotFoundException {
        Map<String, String> tabs = new HashMap<>();

        for (Class<?> cls : coreClass.getNestMembers()) {
            if (cls.isAnonymousClass()) {
                Class<?>[] interfcs = cls.getInterfaces();

                if (interfcs.length == 1 && actionListenerInterface == interfcs[0]) {
                    Object inputStream = Refl.getMethodAndInvokeDirectly(
                        "getResourceAsStream",
                        coreClass.getClassLoader(),
                        cls.getName().replace(".", "/") + ".class"
                    );

                    final String[] typeName = {null};
                    final String[] tabName = {null};

                    ClassReader cr = new ClassReader(readStream(inputStream));

                    cr.accept(new ClassVisitor(ASM9) {
                        @Override
                        public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                            if (!name.equals("actionPerformed")) return null;

                            return new MethodVisitor(ASM9) {
                                @Override
                                public void visitTypeInsn(int opcode, String type) {
                                    if (opcode == NEW) {
                                        typeName[0] = type.replace("/", ".");
                                    }
                                }

                                @Override
                                public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                                    if (opcode == GETSTATIC && owner.equals("com/fs/starfarer/api/campaign/CoreUITabId") ) {
                                        tabName[0] = name;
                                    }
                                }
                            };
                        }
                    }, 0);

                    if (tabName[0] != null && typeName[0] != null) {
                        tabs.put(tabName[0], typeName[0]);
                    }
                }
            }
        }
        return Class.forName(tabs.get("FLEET"), false, coreClass.getClassLoader());
    }

    private static String[] getTooltipMethodData() throws ClassNotFoundException {
        Object inputStream = Refl.getMethodAndInvokeDirectly(
            "getResourceAsStream",
            StandardTooltipV2Expandable.class.getClassLoader(),
            StandardTooltipV2Expandable.class.getCanonicalName().replace(".", "/") + ".class"
        );

        final String[] foundNames = {null, null, null, null};

        ClassReader cr = new ClassReader(readStream(inputStream));

        cr.accept(new ClassVisitor(ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                if (name.equals("addButton")) {
                    return new MethodVisitor(ASM9) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                            if (opcode == INVOKESTATIC) {
                                foundNames[0] = owner;
                                foundNames[1] = name;
                                foundNames[2] = descriptor;
                            }
                        }
                    };
                }

                else if (name.equals("addShipList"))
                return new MethodVisitor(ASM9) {
                    @Override
                    public void visitTypeInsn(int opcode, String type) {
                        if (opcode == NEW) {
                            foundNames[3] = type;
                        }
                    }
                };
                return null;
            }
        }, 0);
        return foundNames;
    }

    private static String getOfficerAutoAssignButtonFieldName(Class<?> fleetTabLeftPaneClass) {
        Object inputStream = Refl.getMethodAndInvokeDirectly(
            "getResourceAsStream",
            fleetTabLeftPaneClass.getClassLoader(),
            fleetTabLeftPaneClass.getCanonicalName().replace(".", "/") + ".class"
        );

        final String[] foundName = {null};

        ClassReader cr = new ClassReader(readStream(inputStream));

        cr.accept(new ClassVisitor(ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                if (!name.equals("sizeChanged")) return null;

                return new MethodVisitor(ASM9) {
                    int putFields = 0;
                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                        if (opcode == PUTFIELD && ++putFields == 4) foundName[0] = name;

                    }
                };
            }
        }, 0);

        return foundName[0];
    }

    private static Class<?> getshipIconRendererClass(Class<?> shipIconListClass) throws ClassNotFoundException {
        Object inputStream = Refl.getMethodAndInvokeDirectly(
            "getResourceAsStream",
            shipIconListClass.getClassLoader(),
            shipIconListClass.getCanonicalName().replace(".", "/") + ".class"
        );

        final String[] foundName = {null};

        ClassReader cr = new ClassReader(readStream(inputStream));

        cr.accept(new ClassVisitor(ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                if (!name.equals("actionPerformed")) return null;

                return new MethodVisitor(ASM9) {
                    String lastName;

                    @Override
                    public void visitTypeInsn(int opcode, String name) {
                        if (opcode == CHECKCAST) {
                            lastName = name;
                        }
                    }

                    @Override
                    public void visitVarInsn(int opcode, int idx) {
                        if (opcode == ASTORE && idx == 3) {
                            foundName[0] = lastName;
                        }
                    }
                };
            }
        }, 0);

        return Class.forName(foundName[0].replace("/", "."), false, shipIconListClass.getClassLoader());
    }

    private static String getShipIconHighlightColorFieldName(Class<?> shipIconRendererClass) {
        FleetMemberAPI member = new FleetMember(0, Global.getSettings().getAllVariantIds().get(0), FleetMemberType.SHIP);
        List<FleetMemberAPI> members = new ArrayList<>();
        members.add(member);

        TooltipMakerAPI tt = Global.getSettings().createCustom(0f,0f,null).createUIElement(0f, 0f, false);
        tt.addShipList(1, 1, 0f, UtilUi.DARK_RED, members, 1f);

        Object buttonRenderer = utils.buttonGetRenderer(utils.listPanelGetItems(shipIconListListPanelHandle.get(utils.getChildrenNonCopy(utils.getContents(tt)).get(0))).get(0));
        for (Object field : buttonRenderer.getClass().getDeclaredFields()) {
            if (Refl.getFieldType(field) == shipIconRendererClass) {
                Object value =  Refl.getPrivateVariable(field, buttonRenderer);

                for (Object fielde : shipIconRendererClass.getDeclaredFields()) {
                    if (Refl.getFieldType(fielde) == Color.class) {
                        Color color = (Color) Refl.getPrivateVariable(fielde, value);
                        if (color != null && String.valueOf(color.getGreen()).equals("84")) {
                            return Refl.getFieldName(fielde);
                        }
                        // if (UtilUi.DARK_RED.equals(Refl.getPrivateVariable(fielde, value))) {
                        //     return Refl.getFieldName(fielde);
                        // }
                    }
                }
            }
        }
        return null;
    }

    private static Class<?> getHoloNoiseRendererClass(Class<?> holoClass) throws ClassNotFoundException {
        Object inputStream = Refl.getMethodAndInvokeDirectly(
            "getResourceAsStream",
            holoClass.getClassLoader(),
            holoClass.getCanonicalName().replace(".", "/") + ".class"
        );

        final String[] foundName = {null};

        ClassReader cr = new ClassReader(readStream(inputStream));

        cr.accept(new ClassVisitor(ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                return new MethodVisitor(ASM9) {
                    boolean sawNoise = false;

                    @Override
                    public void visitLdcInsn(Object value) {
                        if ("noise".equals(value)) {
                            sawNoise = true;
                        }
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                        if (sawNoise && "<init>".equals(name) && opcode == INVOKESPECIAL && foundName[0] == null) {
                            foundName[0] = owner;
                        }
                    }
                };
            }
        }, 0);

        return Class.forName(foundName[0].replace("/", "."), false, holoClass.getClassLoader());
    }

    public static String getHoloNoiseColorFieldName(Class<?> holoNoiseClass) {
        Object inputStream = Refl.getMethodAndInvokeDirectly(
            "getResourceAsStream",
            holoNoiseClass.getClassLoader(),
            holoNoiseClass.getCanonicalName().replace(".", "/") + ".class"
        );

        final String[] foundName = {null};

        ClassReader cr = new ClassReader(readStream(inputStream));

        cr.accept(new ClassVisitor(ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                if (!name.equals("<init>")) return null;
                return new MethodVisitor(ASM9) {
                    boolean seen = false;
                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                        if (opcode == GETSTATIC && !owner.equals(Type.getInternalName(Color.class)) && descriptor.equals(Type.getDescriptor(Color.class))) {
                            seen = true;
                        }
                        if (seen && opcode == PUTFIELD && foundName[0] == null) {
                            foundName[0] = name;
                        }
                    }
                };
            }
        }, 0);

        return foundName[0];
    }

    public static class ClassLoader extends java.lang.ClassLoader {
        public ClassLoader(java.lang.ClassLoader parent) {
            super(parent);
        }

        public Class<?> define(byte[] classBytes, String name) {
            return defineClass(name, classBytes, 0, classBytes.length);
        }
    }

    public static void init() {}
}