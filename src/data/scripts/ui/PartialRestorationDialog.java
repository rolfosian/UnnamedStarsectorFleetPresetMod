package data.scripts.ui;

import data.scripts.ClassRefs;
import data.scripts.listeners.FleetPresetManagementListener;
import data.scripts.util.PresetMiscUtils;
import data.scripts.util.PresetUtils;
import data.scripts.util.ReflectionUtilis;
import data.scripts.util.ReflectionUtilis.ListenerFactory.ActionListener;
import data.scripts.util.ReflectionUtilis.ListenerFactory.DialogDismissedListener;
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
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import java.awt.Color;
import java.util.*;

public class PartialRestorationDialog {
    public static void print(Object... args) {
        PresetMiscUtils.print(args);
    }

    private CampaignFleetAPI fleet;
    private Map<Integer, FleetMemberAPI> whichFleetMembersAvailable;
    private List<FleetMemberAPI> playerFleetMembers = new ArrayList<>();
    private List<FleetMemberAPI> pickedFleetMembers = new ArrayList<>();
    private List<FleetMemberAPI> originalOrder = new ArrayList<>();
    
    private TreeTraverser traverser;
    private Map<ButtonAPI, FleetMemberButton> shipButtons;
    private UtilReflection.ConfirmDialogData FMRDialog;
    private UIPanelAPI fleetPanel;
    private UIPanel innerPanel;

    private FleetPresetManagementListener master;
    private PartialRestorationDialog self = this;

    private CampaignFleetAPI tempFleet;
    private Map<FleetMemberAPI, FleetMemberAPI> tempToPresetMembersMap = new HashMap<>();

    public PartialRestorationDialog(Map<Integer, FleetMemberAPI> whichFleetMembersAvailable, CampaignFleetAPI fleet, FleetPresetManagementListener master) {
        master.setPartialSelecting(true);
        this.master = master;
        removeFleetMembersFromPlayerFleet();
        
        this.whichFleetMembersAvailable = whichFleetMembersAvailable;
        this.fleet = fleet;
        this.originalOrder = new ArrayList<>(fleet.getFleetData().getMembersListCopy());

        this.tempFleet = PresetUtils.createTempFleetCopy(fleet.getFleetData().getMembersListCopy());
        this.fleetPanel = UtilReflection.getObfFleetInfoPanel("", tempFleet);

        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            for (FleetMemberAPI tempMember : tempFleet.getFleetData().getMembersListCopy()) {
                if (member.getId().equals(tempMember.getId())) {
                    tempToPresetMembersMap.put(tempMember, member);
                    break;
                }
            }
        }
        
        FMRDialog = UtilReflection.showConfirmationDialog(
            "graphics/illustrations/gate_hauler1.jpg",
            "",
            "Restore",
            "Close",
            ClassRefs.FMRDialogWidth,
            ClassRefs.FMRDialogHeight,
            new DialogDismissedListener() {
                @Override
                public void trigger(Object arg0, Object arg1) {

                    switch ((int) arg1) {
                        case 0:
                            pickedFleetMembers(pickedFleetMembers);
                            tempFleet.despawn();
                            tempFleet = null;
                            break;

                        case 1:
                            cancelledFleetMemberPicking();
                            tempFleet.despawn();
                            tempFleet = null;
                            break;

                        default:
                            break;
                    }
                    master.getTablePlugin().rebuild();
                }
            });
        if (FMRDialog == null) {
            return;
        }
        innerPanel = new UIPanel(FMRDialog.panel);

        PresetUtils.refreshFleetUI();

        traverser = new TreeTraverser(fleetPanel);
        refShipButtons();
        clearFleetPanel();

        FMRDialog.dialog.bringComponentToTop(FMRDialog.panel);
        FMRDialog.dialog.bringComponentToTop(FMRDialog.confirmButton.getInstance());
        FMRDialog.dialog.bringComponentToTop(FMRDialog.cancelButton.getInstance());
        reAddShipButtons();

        return;
    }

    private void refShipButtons() {
        shipButtons = new LinkedHashMap<>();
        for (TreeNode node : traverser.getNodesAtDepth(8)) {
            for (Object child : node.getChildren()) {
                ButtonAPI btn = (ButtonAPI) child;
                shipButtons.put(btn, new FleetMemberButton(btn));
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
        for (ButtonAPI btn : shipButtons.keySet()) {
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
            pos = tt.addCustomDoNotSetPosition(shipButtons.get(btn).getPanel()).getPosition().inTL(xOffset, yOffset);
            
            xOffset += pos.getWidth() + 5f;
        }

        shipPanel.addUIElement(tt);
        innerPanel.getInstance().addComponent(shipPanel).inTMid(5f);
    }

    private void replaceListener(ButtonAPI btn) {
        FleetMemberButton buttonWrapper = shipButtons.get(btn);
        FleetMemberAPI member = buttonWrapper.getMember();
        
        ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonSetListenerMethod, btn, new ActionListener() {
            public void trigger(Object arg0, Object arg1) {

                if (btn.isChecked()) {
                    buttonWrapper.addLabel();
                    pickedFleetMembers.add(member);
                    Global.getSector().getPlayerFleet().getFleetData().addFleetMember(tempToPresetMembersMap.get(member));

                } else {
                    buttonWrapper.removeLabel();
                    pickedFleetMembers.remove(member);
                    Global.getSector().getPlayerFleet().getFleetData().removeFleetMember(tempToPresetMembersMap.get(member));
                    self.fleet.getFleetData().addFleetMember(tempToPresetMembersMap.get(member));
                    self.fleet.getFleetData().sortToMatchOrder(originalOrder);
                }

                Global.getSector().getPlayerFleet().getFleetData().sortToMatchOrder(fleet.getFleetData().getMembersListCopy());
                PresetUtils.refreshFleetUI();
            }
        }.getProxy());
    }

    private void removeAllMembersFromPlayerFleet() {
        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder()) {
            Global.getSector().getPlayerFleet().getFleetData().removeFleetMember(member);
            self.fleet.getFleetData().addFleetMember(member);
            if (member.getCaptain().getId().equals(Global.getSector().getPlayerPerson().getId())) {
                self.fleet.setCommander(member.getCaptain());
                self.fleet.getFleetData().setFlagship(member);
            }
        }
        self.fleet.getFleetData().sortToMatchOrder(originalOrder);
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

    private void clearFleetPanel() {
        while (traverser.goDownOneLevel()) {}
        for (int i = 0; i < traverser.getNodes().size(); i++) {
            UIPanel par = new UIPanel(traverser.getCurrentNode().getParent());

            for (Object child : traverser.getCurrentNode().getChildren()) {
                if (ButtonAPI.class.isAssignableFrom(child.getClass())) {
                    continue;
                }
                par.remove(new UIComponent(child));
            }
            traverser.goUpOneLevel();
        }
    }

    private void cancelledFleetMemberPicking() {
        pickedFleetMembers.clear();
        removeAllMembersFromPlayerFleet();
        readdFleetMembersToPlayerFleet();
        PresetUtils.refreshFleetUI();

        master.setPartialSelecting(false);
        master.enableButtonsRequiringSelection();
        
    }

    private void pickedFleetMembers(List<FleetMemberAPI> membersToRestore) {
        removeAllMembersFromPlayerFleet();
        readdFleetMembersToPlayerFleet();

        PresetUtils.partRestorePreset(membersToRestore, whichFleetMembersAvailable, fleet.getFleetData());

        master.setPartialSelecting(false);
        master.enableButtonsRequiringSelection();
        pickedFleetMembers.clear();
    }

    private class FleetMemberButton  {
        private final ButtonAPI button;
        private final FleetMemberAPI member;
        private final CustomPanelAPI panel;
        private final LabelAPI label;
        private final PositionAPI panelPos;

        public FleetMemberButton(ButtonAPI button) {
            this.button = button;

            this.panel = Global.getSettings().createCustom(button.getPosition().getWidth(), button.getPosition().getHeight(), null);
            this.panel.addComponent((UIComponentAPI)button).inTL(0f, 0f);
            panelPos = panel.getPosition();

            this.label = Global.getSettings().createLabel("Restore", Fonts.VICTOR_10);
            this.label.setColor(new Color(173, 255, 47));
            this.label.setAlignment(Alignment.MID);
            this.label.setHighlightColor(Color.GREEN);
            this.label.setHighlightOnMouseover(true);

            this.member = getMemberFromButton(button);
        }

        public void addLabel() {
            this.panel.addComponent((UIComponentAPI)this.label).inMid();
        }

        public void removeLabel() {
            this.panel.removeComponent((UIComponentAPI)this.label);
        }

        public ButtonAPI getButton() {
            return this.button;
        }
        
        public CustomPanelAPI getPanel() {
            return this.panel;
        }

        public LabelAPI getLabel() {
            return this.label;
        }

        public FleetMemberAPI getMember() {
            return this.member;
        }
    }
}
