package data.scripts.ui;

import data.scripts.ClassRefs;
import data.scripts.listeners.FleetPresetManagementListener;
import data.scripts.util.PresetMiscUtils;
import data.scripts.util.PresetUtils;
import data.scripts.util.PresetUtils.FleetPreset;
import data.scripts.util.ReflectionUtilis;
import data.scripts.util.ListenerFactory.ActionListener;
import data.scripts.util.ListenerFactory.DialogDismissedListener;
import data.scripts.util.UtilReflection.HoloVar;
import data.scripts.util.UtilReflection;
import data.scripts.ui.TreeTraverser;
import data.scripts.ui.TreeTraverser.TreeNode;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.FleetMemberPickerListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken.VisibilityLevel;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.ScrollPanelAPI;

import java.awt.Color;
import java.util.*;

import org.lwjgl.input.Keyboard;

public class PartialRestorationDialog {
    public static void print(Object... args) {
        PresetMiscUtils.print(args);
    }

    private FleetPreset preset;
    private CampaignFleetAPI presetFleet;
    private CampaignFleetAPI tempFleet;

    private final Map<FleetMemberAPI, FleetMemberAPI> tempToPresetMembersMap = new HashMap<>();
    private Map<Integer, FleetMemberAPI> whichFleetMembersAvailable;
    
    private final List<FleetMemberAPI> playerFleetMembers = new ArrayList<>();
    private final  List<FleetMemberAPI> pickedFleetMembers = new ArrayList<>();
    private final List<FleetMemberAPI> originalOrder;
    
    private HoloVar holoVar;
    private UtilReflection.ConfirmDialogData FMRDialog;
    private UIPanelAPI innerPanel;
    private UIPanelAPI fleetPanel;
    private Map<ButtonAPI, FleetMemberButton> shipButtons;
    private Map<ButtonAPI, Object> buttonToRenderControllerMap;

    private FleetPresetManagementListener master;
    private PartialRestorationDialog self = this;

    private boolean all = false;
    private int selected = 0;
    private int available = 0;

    public PartialRestorationDialog(Map<Integer, FleetMemberAPI> whichFleetMembersAvailable, FleetPreset preset, CampaignFleetAPI fleet, FleetPresetManagementListener master) {
        master.setPartialSelecting(true);
        this.master = master;
        this.preset = preset;
        removeFleetMembersFromPlayerFleet();
        
        this.whichFleetMembersAvailable = whichFleetMembersAvailable;
        this.presetFleet = fleet;
        this.originalOrder = new ArrayList<>(fleet.getFleetData().getMembersListCopy());

        this.tempFleet = PresetUtils.createTempFleetCopy(fleet.getFleetData().getMembersListCopy());
        this.fleetPanel = UtilReflection.getObfFleetInfoPanel("", tempFleet);

        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            for (FleetMemberAPI tempMember : tempFleet.getFleetData().getMembersListCopy()) {
                if (member.getId().equals(PresetUtils.reverseMemberId(tempMember))) {
                    tempToPresetMembersMap.put(tempMember, member);
                    break;
                }
            }
        }
        
        FMRDialog = UtilReflection.showConfirmationDialog(
            "graphics/illustrations/gate_hauler1.jpg",
            "",
            "Restore",
            "Cancel",
            ClassRefs.FMRDialogWidth,
            ClassRefs.FMRDialogHeight,
            new DialogDismissedListener() {
                @Override
                public void trigger(Object... args) {

                    switch ((int) args[1]) {
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
                    holoVar.resetColor();
                    master.getTablePlugin().rebuild();
                }
            });
        if (FMRDialog == null) {
            return;
        }
        ReflectionUtilis.invokeMethodDirectly(ClassRefs.confirmDialogSetBackgroundDimAmountMethod, FMRDialog.dialog, 0f);
        
        holoVar = new HoloVar(FMRDialog.dialog);
        CustomPanelAPI holoVarOverrideOverlay = Global.getSettings().createCustom(UIConfig.DISPLAY_WIDTH, UIConfig.DISPLAY_HEIGHT, new HoloVarOverrideOverlayPlugin());
        FMRDialog.dialog.addComponent(holoVarOverrideOverlay);

        FMRDialog.confirmButton.setShortcut(Keyboard.KEY_G, false);
        FMRDialog.confirmButton.setEnabled(false);
        innerPanel = FMRDialog.panel;
        addAllButton();

        PresetUtils.refreshFleetUI();

        TreeTraverser traverser = new TreeTraverser(fleetPanel);
        refShipButtons(traverser);
        clearFleetPanel(traverser);
        reAddShipButtons();

        addVerticalSeedString(innerPanel, Global.getSector().getSeedString(), "left");
    }

    private void refShipButtons(TreeTraverser traverser) {
        shipButtons = new LinkedHashMap<>();
        for (TreeNode node : traverser.getNodesAtDepth(7)) {
            for (Object child : node.getChildren()) {
                ButtonAPI btn = (ButtonAPI) child;
                shipButtons.put(btn, new FleetMemberButton(btn, getMemberFromButton(btn)));
            }
        }
    }

    private void reAddShipButtons() {
        float width = ClassRefs.FMRDialogPanelWidth - 5f;
        float height = ClassRefs.FMRDialogPanelHeight - FMRDialog.confirmButton.getPosition().getHeight() - 20f;

        CustomPanelAPI pane = Global.getSettings().createCustom(0f, 0f, new BaseCustomUIPanelPlugin());
        innerPanel.addComponent(pane).inTL(0f, 5f);

        TooltipMakerAPI ttHolder = pane.createUIElement(0f, 0f, false);
        pane.addUIElement(ttHolder);
        
        CustomPanelAPI shipPanel = Global.getSettings().createCustom(width, height, new BaseCustomUIPanelPlugin());
        ttHolder.addCustom(shipPanel, 0f).getPosition().inTL(0f, 0f);

        TooltipMakerAPI tt = shipPanel.createUIElement(width, height, true);
        PositionAPI pos = null;
        float xOffset = 35f;
        float yOffset = 5f;

        int i = 0;
        Set<ButtonAPI> shipBtns = shipButtons.keySet();
        for (ButtonAPI btn : shipBtns) {
            if (i == 0) {
                buttonToRenderControllerMap = UtilReflection.getButtonToRenderControllerMap(ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonGetListenerMethod, btn));
            }

            FleetMemberAPI member = whichFleetMembersAvailable.get(i);
            FleetMemberButton buttonWrapper = shipButtons.get(btn);
            if (member == null) {
                btn.setOpacity(0.66f);
                UtilReflection.setButtonTooltipWithPostProcessing(btn, originalOrder.get(i));
                UtilReflection.setShipButtonHighlightColor(buttonToRenderControllerMap.get(btn), UtilReflection.DARK_RED);
                setListenerForDisabled(buttonWrapper);
            } else {
                available++;
                UtilReflection.setButtonTooltip(btn, originalOrder.get(i));
                buttonWrapper.setListener(setListener(buttonWrapper));
            }
           
            i++;

            if (xOffset > width - 35f) {
                xOffset = 35f;
                yOffset += pos.getHeight() + 5f;
            }

            pos = tt.addComponent(buttonWrapper.getPanel()).inTL(xOffset, yOffset);
            
            xOffset += pos.getWidth() + 5f;
        }
        tt.setHeightSoFar(yOffset);
        shipPanel.addUIElement(tt);
    }

    private void setListenerForDisabled(FleetMemberButton buttonWrapper) {
        ActionListener listener = new ActionListener() {
            public void trigger(Object... args) {
                if (String.valueOf(args[0]).equals("All")) return;

                holoVar.setColor(UtilReflection.DARK_RED);
                holoVar.setOverride(false);
                UtilReflection.clickOutsideAbsorb(FMRDialog.dialog);

                Global.getSector().addTransientScript(new EveryFrameScript() {
                    private boolean isDone = false;

                    @Override
                    public void advance(float arg0) {
                        if (!holoVar.isRendering()) {
                            holoVar.resetColor();
                            Global.getSector().removeTransientScript(this);
                            isDone = true;
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
            }
        };
        ButtonAPI btn = buttonWrapper.getButton();
        ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonSetListenerMethod, btn, listener.getProxy());
        buttonWrapper.setListener(listener);
        return;
    }

    private ActionListener setListener(FleetMemberButton buttonWrapper) {
        ButtonAPI btn = buttonWrapper.getButton();
        FleetMemberAPI member = buttonWrapper.getMember();

        ActionListener listener = new ActionListener() {
            public void trigger(Object... args) {

                if (btn.isChecked()) {
                    buttonWrapper.addLabel();
                    pickedFleetMembers.add(member);
                    Global.getSector().getPlayerFleet().getFleetData().addFleetMember(tempToPresetMembersMap.get(member));
                    selected++;

                    if (!FMRDialog.confirmButton.isEnabled()) FMRDialog.confirmButton.setEnabled(true);

                } else {
                    buttonWrapper.removeLabel();
                    pickedFleetMembers.remove(member);
                    Global.getSector().getPlayerFleet().getFleetData().removeFleetMember(tempToPresetMembersMap.get(member));
                    presetFleet.getFleetData().addFleetMember(tempToPresetMembersMap.get(member));
                    presetFleet.getFleetData().sortToMatchOrder(originalOrder);
                    selected--;
                    
                    if (all) all = false;
                    if (pickedFleetMembers.isEmpty()) FMRDialog.confirmButton.setEnabled(false);
                }

                // Regular FleetData sortToMatchOrder method uses member Ids to sort so we have to use our own here to maintain member agnosticism (and preset fleet dummy members do not share the same Ids)
                PresetUtils.sortToMatchOrder(Global.getSector().getPlayerFleet().getFleetData(), presetFleet.getFleetData().getMembersListCopy());
                if (!String.valueOf(args[0]).equals("All")) {
                    PresetUtils.refreshFleetUI();
                }
                if (selected == available) all = true;
            }
        };
        
        ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonSetListenerMethod, btn, listener.getProxy());
        return listener;
    }

    private static void addVerticalSeedString(UIPanelAPI panel, String sectorSeedString, String side) {
        String[] sectorSeedStringArr = sectorSeedString.split("");
        
        float xOffset = side.equals("left") ? 8f : panel.getPosition().getWidth() - 12f;
        float yOffset = 5f;

        for (int i = 0; i < sectorSeedStringArr.length; i++) {
            LabelAPI sectorSeedLabel = Global.getSettings().createLabel(sectorSeedStringArr[i], "graphics/fonts/victor14.fnt");
            sectorSeedLabel.setColor(Misc.getGrayColor());
            panel.addComponent((UIComponentAPI)sectorSeedLabel).inTL(xOffset, yOffset);

            yOffset += sectorSeedLabel.computeTextHeight(sectorSeedStringArr[i] + 1f);
        }
    }

    private void removeAllPresetMembersFromPlayerFleet() {
        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder()) {
            Global.getSector().getPlayerFleet().getFleetData().removeFleetMember(member);
            presetFleet.getFleetData().addFleetMember(member);

            // TODO: This comparer function needs more rigorous testing
            if (PresetUtils.areSameOfficerMinusId(member.getCaptain(), Global.getSector().getPlayerPerson())) {// (member.getCaptain().getId().equals(Global.getSector().getPlayerPerson().getId())) {
                presetFleet.setCommander(member.getCaptain());
                presetFleet.getFleetData().setFlagship(member);
            }
        }
        presetFleet.getFleetData().sortToMatchOrder(originalOrder);
    }

    // so warning message doesnt show for over max ships THIS IS IRRELEVANT NOW AS WE ARE NO LONGER USING THE ACTUAL FLEET MEMEBR RECOVERY DIALOG. IDK WHY I KEPT THIS
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
        for (Object var : ReflectionUtilis.getAllVariables(button)) {
            for (Object nestedVar : ReflectionUtilis.getAllVariables(var)) {
                if (FleetMemberAPI.class.isAssignableFrom(nestedVar.getClass())) {
                    return (FleetMemberAPI) nestedVar;
                }
            } 
        }
        return null;
    }

    private void clearFleetPanel(TreeTraverser traverser) {
        while (traverser.goDownOneLevel()) {}
        for (int i = 0; i < traverser.getNodes().size(); i++) {
            UIPanelAPI parent = traverser.getCurrentNode().getParent();

            if (parent instanceof ScrollPanelAPI) continue;
            for (UIComponentAPI child : traverser.getCurrentNode().getChildren()) {
                if (ButtonAPI.class.isAssignableFrom(child.getClass())) {
                    continue;
                }
                parent.removeComponent(child);
            }
            traverser.goUpOneLevel();
        }
    }

    private void cancelledFleetMemberPicking() {
        pickedFleetMembers.clear();
        removeAllPresetMembersFromPlayerFleet();
        readdFleetMembersToPlayerFleet();
        PresetUtils.refreshFleetUI();

        master.setPartialSelecting(false);
        master.enableButtonsRequiringSelection();
    }

    private void pickedFleetMembers(List<FleetMemberAPI> membersToRestore) {
        removeAllPresetMembersFromPlayerFleet();
        readdFleetMembersToPlayerFleet();
        if (pickedFleetMembers.size() == 0) {
            master.setPartialSelecting(false);
            master.enableButtonsRequiringSelection();
            return;
        }
        
        PresetUtils.partRestorePreset(membersToRestore, whichFleetMembersAvailable, preset);

        if (master.getMangledFleet() != null) {
            master.getTablePlugin().addShipList(master.getMangledFleet(), whichFleetMembersAvailable);
        } else {
            master.getTablePlugin().addShipList(preset.getCampaignFleet(), whichFleetMembersAvailable);
        }
        master.setParas();
        
        master.setPartialSelecting(false);
        master.enableButtonsRequiringSelection();
        pickedFleetMembers.clear();
    }

    private class FleetMemberButton  {
        private final ButtonAPI button;
        private final FleetMemberAPI member;
        private final CustomPanelAPI panel;
        private final LabelAPI label;
        private ActionListener listener;

        public FleetMemberButton(ButtonAPI button, FleetMemberAPI member) {
            this.button = button;
            this.member = member;

            this.panel = Global.getSettings().createCustom(button.getPosition().getWidth(), button.getPosition().getHeight(), new BaseCustomUIPanelPlugin());
            this.panel.addComponent((UIComponentAPI)button).inTL(0f, 0f);

            this.label = Global.getSettings().createLabel("RESTORE", "graphics/fonts/victor14.fnt");
            this.label.setColor(new Color(173, 255, 47));
            this.label.setAlignment(Alignment.MID);
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

        public ActionListener getListener() {
            return this.listener;
        }

        public void setListener(ActionListener listener) {
            this.listener = listener;
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

    private void addAllButton() {
        float width = FMRDialog.confirmButton.getPosition().getWidth() / 2;
        float height = FMRDialog.confirmButton.getPosition().getHeight();
        CustomPanelAPI panel = Global.getSettings().createCustom(width, height, new BaseCustomUIPanelPlugin() {
            @Override
            public void buttonPressed(Object buttonId) {
                if (!all) {
                    for (FleetMemberButton btn : shipButtons.values()) {
                        if (!btn.getButton().isChecked() && btn.getButton().isEnabled()) {
                            btn.getButton().setChecked(true);
                            btn.getListener().trigger("All", null);
                        }
                    }
                    all = true;
                } else {
                    for (FleetMemberButton btn : shipButtons.values()) {
                        btn.getButton().setChecked(false);
                        btn.getListener().trigger("All", null);
                    }
                }
                PresetUtils.refreshFleetUI();
            }
        });
        TooltipMakerAPI tt = panel.createUIElement(width, height, false);
        tt.setButtonFontOrbitron20();
        tt.addButton("All",
        "",
        Misc.getBasePlayerColor(),
        Misc.getDarkPlayerColor(),
        Alignment.MID,
        CutStyle.TL_BR,
        width,
        height,
        5f
        );
        panel.addUIElement(tt);

        
        // pos.set(FMRDialog.confirmButton.getPosition());
        innerPanel.addComponent(panel).leftOfMid((UIComponentAPI)FMRDialog.cancelButton.getInstance(), 10f);

        FMRDialog.confirmButton.getPosition().leftOfMid(panel, 0f);
    }

    private class HoloVarOverrideOverlayPlugin extends BaseCustomUIPanelPlugin {
        private final float dialogLeftBound;
        private final float dialogRightBound;
        private final float dialogTopBound;
        private final float dialogBottomBound;
        
        public HoloVarOverrideOverlayPlugin() {
            super();
            PositionAPI dialogPos = FMRDialog.dialog.getPosition();
            this.dialogLeftBound = dialogPos.getCenterX() - dialogPos.getWidth() / 2;
            this.dialogRightBound = dialogPos.getCenterX() + dialogPos.getWidth() / 2;
            this.dialogTopBound = dialogPos.getCenterY() + dialogPos.getHeight() / 2;
            this.dialogBottomBound = dialogPos.getCenterY() - dialogPos.getHeight() / 2;
        }

        private boolean isOutsideDialogBounds(float mouseX, float mouseY) {
            return (mouseX < dialogLeftBound || mouseX > dialogRightBound || 
            mouseY < dialogBottomBound || mouseY > dialogTopBound);
        }

        @Override
        public void processInput(List<InputEventAPI> events) {
            for (InputEventAPI event : events) {
                if (event.isLMBDownEvent() && isOutsideDialogBounds(event.getX(), event.getY())) {
                    holoVar.setOverride(true);
                    break;
                }
            }
        }
    }
}
