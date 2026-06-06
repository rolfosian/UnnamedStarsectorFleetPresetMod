package data.scripts.ui;

import data.scripts.FleetPresetManagerPlugin;
import data.scripts.autopilotwithgates.util.Refl;
import data.scripts.listeners.FleetPresetManagementListener;
import data.scripts.util.BaseEveryFrameScript;
import data.scripts.util.PresetMiscUtils;
import data.scripts.util.PresetUtils;
import data.scripts.util.PresetUtils.FleetPreset;
import data.scripts.util.UtilUi.HoloNoise;
import data.scripts.util.UtilUi;

import data.scripts.util.UiUtil;
import data.scripts.util.UiUtil.DialogDismissedListener;
import data.scripts.util.UiUtil.ActionListener;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.FleetDataAPI;
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
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.ScrollPanelAPI;

import static data.scripts.util.UiUtil.utils;
// import static data.scripts.util.PresetUtils.FleetPreset.memberHandles;
// import static data.scripts.util.PresetUtils.FleetPreset.repairTrackerHandle;
// import static data.scripts.util.PresetUtils.FleetPreset.statusHandle;
import static data.scripts.FleetPresetManagerPlugin.fleetPanelInjector;

import java.awt.Color;
// import java.lang.invoke.VarHandle;
import java.util.*;

import org.lwjgl.input.Keyboard;

public class PartialRestorationDialog {
    private static final float FMRDialogWidth = FleetPresetManagementListener.CONFIRM_DIALOG_WIDTH * 0.87f;
    private static final float FMRDialogHeight = FleetPresetManagementListener.CONFIRM_DIALOG_HEIGHT * 0.87f;
    private static final float FMRDialogPanelWidth = FMRDialogWidth * 0.97f;
    private static final float FMRDialogPanelHeight = FMRDialogHeight * 0.97f;

    private FleetPreset preset;

    private Map<Integer, FleetMemberAPI> whichFleetMembersAvailable;
    
    private final List<FleetMemberAPI> playerFleetMembers = new ArrayList<>();
    private final List<FleetMemberAPI> pickedFleetMembers = new ArrayList<>();
    private final List<FleetMemberAPI> originalOrder;
    
    private HoloNoise holoNoise;
    private UtilUi.ConfirmDialogData FMRDialog;
    private UIPanelAPI innerPanel;

    private final Map<ButtonAPI, FleetMemberButton> shipButtons;
    private final Map<ButtonAPI, Object> buttonToRenderControllerMap;

    private final UIPanelAPI fleetPanel;
    private final UIPanelAPI fleetPanelList;
    private final ScrollPanelAPI fleetPanelListScroller;
    private final List<UIPanelAPI> originalMemberItems;
    
    private final FleetPresetManagementListener master;

    private boolean all = false;
    private int selected = 0;
    private int available = 0;

    private boolean isDone = false;

    public PartialRestorationDialog(Map<Integer, FleetMemberAPI> whichFleetMembersAvailable, FleetPreset preset, List<FleetMemberAPI> members, FleetPresetManagementListener master) {
        this.master = master;

        this.fleetPanel = utils.fleetTabGetFleetPanel(fleetPanelInjector.getFleetTab());
        this.fleetPanelList = utils.fleetPanelGetList(fleetPanel);
        this.fleetPanelListScroller = utils.listPanelGetScroller(fleetPanelList);

        List<UIPanelAPI> items = new ArrayList<>();
        for (UIComponentAPI item : new ArrayList<>(utils.listPanelGetItems(this.fleetPanelList))) {
            UIPanelAPI panel = (UIPanelAPI) item;
            items.add(panel);

            float scrollX = this.fleetPanelListScroller.getXOffset();
            float scrollY = this.fleetPanelListScroller.getYOffset();

            utils.listPanelRemoveItem(this.fleetPanelList, panel);
            utils.listPanelCollapseEmptySlots(this.fleetPanelList, true);

            utils.scrollerSetOffset(this.fleetPanelListScroller, scrollX, scrollY);
            utils.scrollerClampOffset(this.fleetPanelListScroller);
        }
        this.originalMemberItems = Collections.unmodifiableList(items);

        this.master.setPartialSelecting(true);
        this.preset = preset;
        this.playerFleetMembers.addAll(Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder());

        this.whichFleetMembersAvailable = whichFleetMembersAvailable;
        this.originalOrder = new ArrayList<>(members);
        
        this.FMRDialog = UtilUi.showConfirmationDialog(
            "graphics/illustrations/gate_hauler1.jpg",
            "",
            "Restore",
            "Cancel",
            FMRDialogWidth,
            FMRDialogHeight,
            new DialogDismissedListener() {
                @Override
                public void dialogDismissed(Object arg0, int arg1) {
                    if (arg1 == 0) {
                        pickedFleetMembers(pickedFleetMembers);
                    } else {
                        cancelledFleetMemberPicking();
                    }

                    holoNoise.resetColor();
                    master.getTablePlugin().rebuild();
                }
            });

        utils.confirmDialogSetBackgroundDimAmount(this.FMRDialog.dialog, 0f);
        
        this.holoNoise = new HoloNoise(this.FMRDialog.dialog);
        CustomPanelAPI holoNoiseOverrideOverlay = Global.getSettings().createCustom(UIConfig.DISPLAY_WIDTH, UIConfig.DISPLAY_HEIGHT, new HoloNoiseOverrideOverlayPlugin());
        this.FMRDialog.dialog.addComponent(holoNoiseOverrideOverlay);

        this.FMRDialog.confirmButton.setShortcut(Keyboard.KEY_G, false);
        this.FMRDialog.confirmButton.setEnabled(false);
        this.innerPanel = this.FMRDialog.panel;
        this.addAllButton();

        UIPanelAPI fleetPanel = UtilUi.createShipIconListPanel(UtilUi.DARK_GREEN, members);
        this.buttonToRenderControllerMap = UtilUi.getButtonToRenderControllerMap(fleetPanel);
        this.shipButtons = new LinkedHashMap<>();
        
        List<UIComponentAPI> buttons = utils.listPanelGetItems((UIPanelAPI)UiUtil.shipIconListListPanelHandle.get(fleetPanel));

        // remove last to first and wrap/cache
        for (int i = buttons.size() - 1; i >= 0; i--) {
            UIComponentAPI btn = buttons.get(i);
            utils.getParent(btn).removeComponent(btn);
        }

        for (int i = 0; i < buttons.size(); i++) {
            ButtonAPI btn = (ButtonAPI) buttons.get(i);
            this.shipButtons.put(btn, new FleetMemberButton(btn, getMemberFromButton(btn)));
        }

        this.reAddShipButtons();

        addVerticalSeedString(this.innerPanel, Global.getSector().getSeedString(), "left");
    }

    private void reAddShipButtons() {
        float width = FMRDialogPanelWidth - 5f;
        float height = FMRDialogPanelHeight - this.FMRDialog.confirmButton.getPosition().getHeight() - 20f;

        CustomPanelAPI pane = Global.getSettings().createCustom(0f, 0f, null);
        innerPanel.addComponent(pane).inTL(0f, 5f);

        TooltipMakerAPI ttHolder = pane.createUIElement(0f, 0f, false);
        pane.addUIElement(ttHolder);
        
        CustomPanelAPI shipPanel = Global.getSettings().createCustom(width, height, null);
        ttHolder.addCustom(shipPanel, 0f).getPosition().inTL(0f, 0f);

        TooltipMakerAPI tt = shipPanel.createUIElement(width, height, true);
        PositionAPI pos = null;
        float xOffset = 35f;
        float yOffset = 5f;

        int i = 0;
        Set<ButtonAPI> shipBtns = shipButtons.keySet();
        for (ButtonAPI btn : shipBtns) {
            FleetMemberAPI member = whichFleetMembersAvailable.get(i);
            FleetMemberButton buttonWrapper = shipButtons.get(btn);
            btn.setEnabled(true);

            if (member == null) {
                btn.setOpacity(0.66f);
                btn.setButtonPressedSound("ui_button_disabled_pressed");
                UtilUi.setButtonTooltipWithPostProcessing(btn, originalOrder.get(i));
                UtilUi.setShipButtonHighlightColor(buttonToRenderControllerMap.get(btn), UtilUi.DARK_RED);
                setListenerForDisabled(buttonWrapper);
            } else {
                available++;
                UtilUi.setShipButtonHighlightColor(buttonToRenderControllerMap.get(btn), UtilUi.DARK_GREEN);
                UtilUi.setButtonTooltip(btn, originalOrder.get(i));
                buttonWrapper.setListener(setListener(buttonWrapper, i));
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

    Map<FleetMemberAPI, List<Object>> origFieldMap = new HashMap<>();

    private ActionListener setListener(FleetMemberButton buttonWrapper, int idx) {
        ButtonAPI btn = buttonWrapper.getButton();
        FleetMemberAPI member = buttonWrapper.getMember();
        FleetMemberAPI realMember = whichFleetMembersAvailable.get(idx);

        UIPanelAPI memberItemPanel = UiUtil.instantiator.instantiateFleetMemberItem(member, fleetPanel);

        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(Object arg0, Object arg1) {
                if (btn.isChecked()) {
                    buttonWrapper.addLabel();
                    pickedFleetMembers.add(member);
                    addMemberToFleetPanelGrid(memberItemPanel);
                    selected++;

                    if (!FMRDialog.confirmButton.isEnabled()) FMRDialog.confirmButton.setEnabled(true);

                } else {
                    buttonWrapper.removeLabel();
                    pickedFleetMembers.remove(member);
                    removeMemberItemFromFleetPanelGrid(memberItemPanel);
                    selected--;
                    
                    if (all) all = false;
                    if (pickedFleetMembers.isEmpty()) FMRDialog.confirmButton.setEnabled(false);
                }

                if (selected == available) all = true;
            }
        };
        
        utils.buttonSetListener(btn, listener.getProxy());
        return listener;
    }

    private void setListenerForDisabled(FleetMemberButton buttonWrapper) {
        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(Object arg0, Object arg1) {
                if (String.valueOf(arg0).equals("All")) return;

                holoNoise.setColor(UtilUi.DARK_RED);
                holoNoise.setOverride(false);
                UtilUi.outsideClickAbsorbed(FMRDialog.dialog);

                Global.getSector().addTransientScript(new BaseEveryFrameScript(true) {
                    @Override
                    public void advance(float arg0) {
                        if (!holoNoise.isRendering()) {
                            holoNoise.resetColor();
                            Global.getSector().removeTransientScript(this);
                            isDone = true;
                        }
                    }
                });
            }
        };
        ButtonAPI btn = buttonWrapper.getButton();
        utils.buttonSetListener(btn, listener.getProxy());
        buttonWrapper.setListener(listener);
        return;
    }

    private void addMemberToFleetPanelGrid(UIPanelAPI memberPanel) {
        float scrollX = this.fleetPanelListScroller.getXOffset();
        float scrollY = this.fleetPanelListScroller.getYOffset();

        this.fleetPanelList.getPosition().setSuspendRecompute(true);
        this.fleetPanelListScroller.getPosition().setSuspendRecompute(true);
        utils.scrollerGetContentContainer(this.fleetPanelListScroller).getPosition().setSuspendRecompute(true);

        memberPanel.getPosition().setSuspendRecompute(true);
        utils.listPanelAddItem(this.fleetPanelList, memberPanel);
        memberPanel.getPosition().setSuspendRecompute(false);

        this.fleetPanelList.getPosition().setSuspendRecompute(false);
        this.fleetPanelListScroller.getPosition().setSuspendRecompute(false);
        utils.scrollerGetContentContainer(this.fleetPanelListScroller).getPosition().setSuspendRecompute(false);

        utils.scrollerSetOffset(this.fleetPanelListScroller, scrollX, scrollY);
    }

    private void removeMemberItemFromFleetPanelGrid(UIPanelAPI memberPanel) {
        float scrollX = this.fleetPanelListScroller.getXOffset();
        float scrollY = this.fleetPanelListScroller.getYOffset();

        utils.listPanelRemoveItem(this.fleetPanelList, memberPanel);
        utils.listPanelCollapseEmptySlots(this.fleetPanelList, false);

        utils.scrollerSetOffset(this.fleetPanelListScroller, scrollX, scrollY);
        utils.scrollerClampOffset(this.fleetPanelListScroller);
    }

    // private void removeMembersFromPlayerFleet() {
    //     for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersInPriorityOrder()) {
    //         Global.getSector().getPlayerFleet().getFleetData().removeFleetMember(member);
    //     }
    // }

    // private void removeAllPresetMembersFromPlayerFleet() {
    //     for (FleetMemberAPI member : presetFleet) {
    //         Global.getSector().getPlayerFleet().getFleetData().removeFleetMember(member);
    //     }
    // }

    // private void readdFleetMembersToPlayerFleet() {
    //     FleetDataAPI mothballedShips = PresetUtils.getMothBalledShipsData(this.master.getDockingListener().getPlayerCurrentMarket());

    //     for (FleetMemberAPI member : this.playerFleetMembers) {
    //         mothballedShips.removeFleetMember(member);
    //         Global.getSector().getPlayerFleet().getFleetData().addFleetMember(member);
    //     }
    //     this.playerFleetMembers.clear();
    // }

    private FleetMemberAPI getMemberFromButton(ButtonAPI button) {
        return (FleetMemberAPI) UiUtil.shipIconRendererFleetMemberHandle.get(buttonToRenderControllerMap.get(button));
    }

    private void cancelledFleetMemberPicking() {
        this.pickedFleetMembers.clear();

        for (UIComponentAPI c : new ArrayList<>(utils.listPanelGetItems(this.fleetPanelList))) {
            this.removeMemberItemFromFleetPanelGrid((UIPanelAPI)c);
        }

        for (UIPanelAPI panel : this.originalMemberItems) {
            this.addMemberToFleetPanelGrid(panel);
        }

        this.master.setPartialSelecting(false);
        this.master.enableButtonsRequiringSelection();
        isDone = true;
    }

    private void pickedFleetMembers(List<FleetMemberAPI> membersToRestore) {
        if (pickedFleetMembers.size() == 0) {
            this.cancelledFleetMemberPicking();
            return;
        }
        
        PresetUtils.partRestorePreset(membersToRestore, this.whichFleetMembersAvailable, this.preset);

        this.master.getTablePlugin().addShipList(this.preset.getMembers(), this.whichFleetMembersAvailable);
        this.master.setParas();
        this.master.setPartialSelecting(false);
        this.master.enableButtonsRequiringSelection();

        this.pickedFleetMembers.clear();
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

            this.panel = Global.getSettings().createCustom(button.getPosition().getWidth(), button.getPosition().getHeight(), null);
            this.panel.addComponent((UIComponentAPI)button).inTL(0f, 0f);

            this.label = Global.getSettings().createLabel("RESTORE", "graphics/fonts/victor14.fnt");
            this.label.setColor(new Color(173, 255, 47));
            this.label.setAlignment(Alignment.MID);
        }

        public void addLabel() {
            this.panel.addComponent((UIComponentAPI)this.label).inMid().setYAlignOffset(-5f);
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

        // public LabelAPI getLabel() {
        //     return this.label;
        // }

        public FleetMemberAPI getMember() {
            return this.member;
        }
    }

    private void addAllButton() {
        float width = this.FMRDialog.confirmButton.getPosition().getWidth() / 2;
        float height = this.FMRDialog.confirmButton.getPosition().getHeight();

        CustomPanelAPI panel = Global.getSettings().createCustom(width, height, new BaseCustomUIPanelPlugin() {
            @Override
            public void buttonPressed(Object buttonId) {
                if (!all) {
                    for (FleetMemberButton btn : shipButtons.values()) {
                        if (!btn.getButton().isChecked() && btn.getButton().isEnabled()) {
                            btn.getButton().setChecked(true);
                            btn.getListener().actionPerformed("All", null);
                        }
                    }
                    all = true;
                } else {
                    for (FleetMemberButton btn : shipButtons.values()) {
                        btn.getButton().setChecked(false);
                        btn.getListener().actionPerformed("All", null);
                    }
                }
            }
        });

        TooltipMakerAPI tt = panel.createUIElement(width, height, false);
        tt.setButtonFontOrbitron20();
        tt.addButton(
            "All",
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

        innerPanel.addComponent(panel).leftOfMid((UIComponentAPI)FMRDialog.cancelButton, 10f);

        this.FMRDialog.confirmButton.getPosition().leftOfMid(panel, 0f);
    }

    private class HoloNoiseOverrideOverlayPlugin extends BaseCustomUIPanelPlugin {
        private final float dialogLeftBound;
        private final float dialogRightBound;
        private final float dialogTopBound;
        private final float dialogBottomBound;
        
        public HoloNoiseOverrideOverlayPlugin() {
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
                    holoNoise.setOverride(true);
                    break;
                }
            }
        }
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
}
