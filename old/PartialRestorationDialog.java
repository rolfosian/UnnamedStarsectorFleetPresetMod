package data.scripts.ui;

import data.scripts.ClassRefs;
import data.scripts.listeners.FleetPresetManagementListener;
import data.scripts.util.PresetMiscUtils;
import data.scripts.util.PresetUtils;
import data.scripts.util.ReflectionUtilis;
import data.scripts.util.ReflectionUtilis.ListenerFactory.ActionListener;
import data.scripts.util.UtilReflection;
import data.scripts.util.UtilReflection.TreeTraverser;
import data.scripts.util.UtilReflection.TreeTraverser.TreeNode;

import com.fs.graphics.Sprite;
import com.fs.starfarer.ui.newui.FleetMemberRecoveryDialog;
import com.fs.util.container.Pair;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.FleetMemberPickerListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken.VisibilityLevel;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import java.awt.Color;
import java.util.*;


// keeping this for reference
@SuppressWarnings("unchecked")
public class PartialRestorationDialog {
    public static void print(Object... args) {
        PresetMiscUtils.print(args);
    }

    private CampaignFleetAPI fleet;
    private Map<Integer, FleetMemberAPI> whichFleetMembersAvailable;
    private List<FleetMemberAPI> playerFleetMembers = new ArrayList<>();
    
    private TreeTraverser traverser;
    private List<ButtonAPI> shipButtons;
    private List<UIComponentAPI> yesNoButtons;
    private FleetMemberRecoveryDialog FMRDialog;
    private UIPanel innerPanel;

    private FleetPresetManagementListener master;
    private PartialRestorationDialog self = this;

    public PartialRestorationDialog(Map<Integer, FleetMemberAPI> whichFleetMembersAvailable, UIPanelAPI overlordPanel, CampaignFleetAPI fleet, FleetPresetManagementListener master) {
        master.setPartialSelecting(true);
        this.whichFleetMembersAvailable = whichFleetMembersAvailable;
        this.fleet = fleet;
        this.master = master;

        removeFleetMembersFromPlayerFleet();
        PresetUtils.refreshFleetUI();

        Global.getSector().addTransientListener(new BaseCampaignEventListener(false) {
            @Override @SuppressWarnings("unchecked")
            public void reportShownInteractionDialog(InteractionDialogAPI dialog) {
                if (!(dialog.getPlugin() instanceof ClassRefs.DummyInteractionDialogPlugin)) return;
                FMRDialog = null;

                dialog.showFleetMemberRecoveryDialog("", fleet.getFleetData().getMembersListCopy(), new FleetMemberPickerListener() {
                    @Override
                    public void cancelledFleetMemberPicking() {
                        removeAllMembersFromPlayerFleet();
                        readdFleetMembersToPlayerFleet();
                        PresetUtils.refreshFleetUI();

                        master.setPartialSelecting(false);
                        master.enableButtonsRequiringSelection();
                        
                    }

                    @Override
                    public void pickedFleetMembers(List<FleetMemberAPI> arg0) {
                        removeAllMembersFromPlayerFleet();
                        readdFleetMembersToPlayerFleet();

                        PresetUtils.partRestorePreset(arg0, whichFleetMembersAvailable);

                        master.setPartialSelecting(false);
                        master.enableButtonsRequiringSelection();
                    }
                });
                
                for (Object child : (List<Object>) ReflectionUtilis.invokeMethodDirectly(ClassRefs.visualPanelGetChildrenNonCopyMethod, dialog.getVisualPanel())) {
                    if (child instanceof FleetMemberRecoveryDialog) {
                        FMRDialog = (FleetMemberRecoveryDialog) child;
                        ReflectionUtilis.invokeMethodDirectly(ClassRefs.uiPanelsetParentMethod, FMRDialog, overlordPanel);
                        ReflectionUtilis.invokeMethodDirectly(ClassRefs.uiPanelsetParentMethod, FMRDialog.getInnerPanel(), FMRDialog);
                        break;
                    }
                }
                Global.getSector().removeListener(this);

                dialog.dismiss();
                FMRDialog.show(0.25f, 0.25f);
                innerPanel = new UIPanel(FMRDialog.getInnerPanel());
                traverser = new TreeTraverser(FMRDialog.getInnerPanel());

                refShipButtons();
                clearDialog();
                resetDialogSize();
                UtilReflection.addBackGroundImage(FMRDialog, "graphics/illustrations/gate_hauler1.jpg", 0.66f);
                reAddShipButtons();
            }
        });

        Global.getSector().getCampaignUI().showInteractionDialogFromCargo(new ClassRefs.DummyInteractionDialogPlugin(), null, new CampaignUIAPI.DismissDialogDelegate() {
            public void dialogDismissed() {}
        });

        return;
    }

    private void resetDialogSize() {
        innerPanel.getPosition().setSize(ClassRefs.FMRDialogPanelWidth-5f, ClassRefs.FMRDialogPanelHeight-5f);
        FMRDialog.getPosition().setSize(ClassRefs.FMRDialogWidth-5f, ClassRefs.FMRDialogHeight-5f);
        FMRDialog.setSize(ClassRefs.FMRDialogWidth-5f, ClassRefs.FMRDialogHeight-5f);
    }

    private void clearDialog() {
        yesNoButtons = new ArrayList<>();

        while (traverser.goDownOneLevel()) {}
        for (int i = 0; i < traverser.getNodes().size(); i++) {
            UIPanel par = new UIPanel(traverser.getCurrentNode().getParent());

            for (Object child : traverser.getCurrentNode().getChildren()) {
                if (ButtonAPI.class.isAssignableFrom(child.getClass())) {
                    ButtonAPI btn = (ButtonAPI) child;
                    String text = btn.getText();

                    if (text != null) {
                        if (text.equals("All")) {
                            btn.getPosition().setSize(0f, 0f);
                            btn.setText(null);
                            btn.setEnabled(false);
                            btn.setMouseOverSound(null);
                            btn.setButtonPressedSound(null);
                            btn.setOpacity(0f);
                        } else {
                            yesNoButtons.add(btn);
                        }
                    }
                    continue;

                }
                par.remove(new UIComponent(child));
            }
            traverser.goUpOneLevel();
        }
    }

    private void refShipButtons() {
        shipButtons = new ArrayList<>();
        for (TreeNode node : traverser.getNodesAtDepth(5)) {
            for (Object child : node.getChildren()) {
                shipButtons.add((ButtonAPI) child);
            }
        }
    }

    private void reAddShipButtons() {
        float width = ClassRefs.FMRDialogPanelWidth - 5f;
        float height = ClassRefs.FMRDialogPanelHeight - 5f;

        CustomPanelAPI shipPanel = Global.getSettings().createCustom(width, height, null);
        TooltipMakerAPI tt = shipPanel.createUIElement(width, height, true);

        PositionAPI pos = null;
        float xOffset = 35f;
        float yOffset = 5f;

        int i = 0;
        for (ButtonAPI btn : shipButtons) {
            if (whichFleetMembersAvailable.get(i) == null) {
                btn.setGlowBrightness(0f);
                btn.setEnabled(false);
                btn.setFlashBrightness(0f);
                btn.setHighlightBrightness(0f);
                btn.setOpacity(0.5f);
                btn.setShowTooltipWhileInactive(false);
                ReflectionUtilis.getMethodAndInvokeDirectly("setActive", btn, 1, false);
            }
            i++;

            if (xOffset > width - 35f) {
                xOffset = 35f;
                yOffset += pos.getHeight() + 5f;
            }
            
            replaceListener(btn);
            pos = tt.addCustomDoNotSetPosition((UIComponentAPI)btn).getPosition().inTL(xOffset, yOffset);
            pos.setSize(pos.getWidth()*0.75f, pos.getWidth()*0.75f);
            
            xOffset += pos.getWidth() + 5f;
        }

        for (UIComponentAPI btn : yesNoButtons) {
            innerPanel.getInstance().bringComponentToTop(btn);
        }
        // tt.addCustom(shipHolder, 0f);
        shipPanel.addUIElement(tt);
        innerPanel.getInstance().addComponent(shipPanel).inTMid(5f);
    }

    private void replaceListener(ButtonAPI btn) {
        FleetMemberAPI member = getMemberFromButton(btn);
        Object oldListener = ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonGetListenerMethod, btn);

        Map<ButtonAPI, Object> map = getButtonToRenderControllerMap(oldListener);
        Object renderController = map.get(btn);

        Object[] labelRenderers = getButtonLabelRenderers(renderController);
        Object textSetter = ReflectionUtilis.getMethodByParamTypes(renderController, new Class<?>[]{String.class});
        ReflectionUtilis.invokeMethodDirectly(textSetter, renderController, "RESTORE");

        List<Object> colorSetters = ReflectionUtilis.getMethodsByParamTypes(labelRenderers[0], new Class<?>[]{Color.class});
        
        Color[] colors = new Color[] {new Color(173, 255, 47), new Color(0, 0, 0)};
        for (Object renderer : labelRenderers) {
            ReflectionUtilis.logFields(renderer);

            int i = 0;
            for (Object method : colorSetters) {
                ReflectionUtilis.invokeMethodDirectly(method, renderer, colors[i]);
                i++;
                if (i > 1) i = 0;
            }
        }
        
        ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonSetListenerMethod, btn, new ActionListener() {
            public void trigger(Object arg0, Object arg1) {
                ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonListenerActionPerformedMethod, oldListener, arg0, arg1);
                
                if (btn.isChecked()) {

                    ReflectionUtilis.setFieldAtIndex(renderController, 24, false);

                    Global.getSector().getPlayerFleet().getFleetData().addFleetMember(member);
                } else {
                    Global.getSector().getPlayerFleet().getFleetData().removeFleetMember(member);
                }

                Global.getSector().getPlayerFleet().getFleetData().sortToMatchOrder(fleet.getFleetData().getMembersListCopy());
                PresetUtils.refreshFleetUI();
            }
        }.getProxy());
    }

    private void removeAllMembersFromPlayerFleet() {
        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder()) {
            Global.getSector().getPlayerFleet().getFleetData().removeFleetMember(member);
        }
    }

    // so warning message doesnt show for over max ships
    private void removeFleetMembersFromPlayerFleet() {
        List<FleetMemberAPI> members = Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder();
        FleetDataAPI mothballedShips = PresetUtils.getMothBalledShipsData(master.getDockingListener().getPlayerCurrentMarket());

        for (FleetMemberAPI member : members) {
            Global.getSector().getPlayerFleet().getFleetData().removeFleetMember(member);
            mothballedShips.addFleetMember(member);
            playerFleetMembers.add(member);
        }
    }

    private void readdFleetMembersToPlayerFleet() {
        FleetDataAPI mothballedShips = PresetUtils.getMothBalledShipsData(master.getDockingListener().getPlayerCurrentMarket());

        for (FleetMemberAPI member : playerFleetMembers) {
            mothballedShips.removeFleetMember(member);
            Global.getSector().getPlayerFleet().getFleetData().addFleetMember(member);
        }
        playerFleetMembers.clear();
    }

    private FleetMemberAPI getMemberFromButton(ButtonAPI button) {
        for (Object var : ReflectionUtilis.getAllFields(button)) {
            for (Object nestedVar : ReflectionUtilis.getAllFields(var)) {
                if (FleetMemberAPI.class.isAssignableFrom(nestedVar.getClass())) {
                    return (FleetMemberAPI) nestedVar;
                }
            } 
        }
        return null;
    }

    private Map<ButtonAPI, Object> getButtonToRenderControllerMap(Object listener) {
        for (Object field : listener.getClass().getDeclaredFields()) {
            Class<?> fieldType = ReflectionUtilis.getFieldType(field);

            for (Object nestedField : fieldType.getDeclaredFields()) {
                if (Map.class.equals(ReflectionUtilis.getFieldType(nestedField))) {
                    // ReflectionUtilis.logFields(ReflectionUtilis.getPrivateVariable(field, listener));
                    return (Map<ButtonAPI, Object>) ReflectionUtilis.getPrivateVariable(nestedField, ReflectionUtilis.getPrivateVariable(field, listener));
                }
            }
        }
        return null;
    }

    private Object[] getButtonLabelRenderers(Object labelCreator) {
        Object[] renderers = new Object[3];
        int i = 0;
        for (Object v : ReflectionUtilis.getAllFields(labelCreator)) {
            if ( v != null && ReflectionUtilis.getMethodsByReturnType(v.getClass(), String.class, 0).size() == 2) {
                renderers[i] = v;
                i++;
                if (i == 3) break;
            }
        } 

        return renderers;
    }
}
