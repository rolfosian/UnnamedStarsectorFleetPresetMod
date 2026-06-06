// Code taken and modified from Officer Extension mod

package data.scripts;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import com.fs.starfarer.api.ui.*;

import com.fs.starfarer.api.util.Misc;


import data.scripts.ui.UIConfig;

import data.scripts.listeners.DockingListener;
import data.scripts.listeners.FleetPresetManagementListener;

import data.scripts.util.CargoPresetUtils;
import data.scripts.util.PresetMiscUtils;
import data.scripts.util.PresetUtils;
import data.scripts.util.PresetUtils.FleetPreset;
// import data.scripts.util.ReflectionUtilis;
import data.scripts.util.UiUtil;
import data.scripts.util.UiUtil.ActionListener;
import static data.scripts.util.UiUtil.utils;

import java.util.*;
// import java.awt.Color;

import org.lwjgl.input.Keyboard;

// @SuppressWarnings("unchecked")
public class FleetPanelInjector implements EveryFrameScript {
    private boolean injected = false;

    private UIPanelAPI fleetTab;
    private UIPanelAPI fleetTabLeftPaneRef; /** Keep track of the last known fleet info panel to track when it changes */
    private UIPanelAPI fleetTabLeftPane;
    private UIComponentAPI targetDialogButtonAnchor;
    private Object fleetPanelClickHandler;

    private ButtonAPI autoAssignButton;
    private PositionAPI officerAutoAssignButtonPosition;

    private ButtonAPI presetFleetsButton;
    private ButtonAPI storeFleetButton;
    private ButtonAPI pullAllShipsButton;

    private LabelAPI currentPresetLabelHeader;
    private LabelAPI currentPresetLabel;

    private DockingListener dockingListener;
    private FleetPresetManagementListener masterPresetsDialog;

    public void init(DockingListener listener) {
        dockingListener = listener;
    }

    private ButtonAPI getStorageButton(UIPanelAPI fleetTab) {
        UIPanelAPI marketPicker = utils.fleetTabGetMarketPicker(fleetTab);
        if (marketPicker == null) return null;
        List<UIComponentAPI> children = utils.getChildrenNonCopy(marketPicker);
        return children.size() > 0 ? (ButtonAPI) children.get(children.size()-1) : null;
    }

    public void advance(float arg0) {
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();

        if (campaignUI.getCurrentCoreTab() != CoreUITabId.FLEET) {
            this.injected = false;
            this.fleetTabLeftPaneRef = null;
            this.autoAssignButton = null;
            this.presetFleetsButton = null;
            this.pullAllShipsButton = null;
            this.storeFleetButton = null;
            this.currentPresetLabel = null;
            this.currentPresetLabelHeader = null;
            this.fleetTabLeftPane = null;
            this.fleetTab = null;
            this.masterPresetsDialog = null;
            return;
        }

        UIPanelAPI fleetTab = utils.coreGetCurrentTab(UiUtil.getCore(campaignUI, campaignUI.getCurrentInteractionDialog()));
        UIPanelAPI fleetTabLeftPane = UiUtil.getFleetTabLeftPane(fleetTab);

        if (this.fleetTabLeftPane != null && fleetTabLeftPane != this.fleetTabLeftPane) {
            this.reset();
            return;
        }

        List<FleetMemberAPI> playerFleetMembers = Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy();
        MarketAPI market = PresetUtils.getPlayerCurrentMarket();
        List<FleetMemberAPI> mothballedShips = PresetUtils.getMothBalledShips(market);

        if (fleetTabLeftPane == this.fleetTabLeftPane) {
            setCurrentPresetLabel(playerFleetMembers);
            if (market == null && this.currentPresetLabel != null && PresetUtils.isAutoUpdatePresets()) {
                dockingListener.setUndockedPreset(this.currentPresetLabel.getText());
            }

            if (market != null && CargoPresetUtils.getStorageSubmarket(market) != null) {
                if (dockingListener.canPlayerAccessStorage(market)) {
                    updateAuxStorageButtons(playerFleetMembers, mothballedShips, market);
                }
            }
            return;
        }

        if (!this.injected) {
            this.injected = true;
            this.fleetTab = fleetTab;
            this.fleetTabLeftPane = fleetTabLeftPane;
            this.autoAssignButton = UiUtil.getOfficerAutoAssignButton(this.fleetTabLeftPane);
            this.officerAutoAssignButtonPosition = this.autoAssignButton.getPosition();

            if (market != null && CargoPresetUtils.getStorageSubmarket(market) != null) {
                addAuxStorageButtons(playerFleetMembers, this.officerAutoAssignButtonPosition, mothballedShips, market);
            }

            this.masterPresetsDialog = new FleetPresetManagementListener();
            this.presetFleetsButton = utils.factoryCreateRegularButton(
                "   Fleet Presets Management",
                "graphics/fonts/orbitron12condensed.fnt",
                Misc.getDarkPlayerColor(),
                Misc.getBasePlayerColor(),
                Alignment.LMID,
                CutStyle.BL_TR,
                this.masterPresetsDialog.getProxy()
            );
            this.presetFleetsButton.setShortcut(Keyboard.KEY_A, true);
            this.presetFleetsButton.getPosition().setSize(this.officerAutoAssignButtonPosition.getWidth(), this.officerAutoAssignButtonPosition.getHeight());

            if (market == null && this.currentPresetLabel != null && PresetUtils.isAutoUpdatePresets()) {
                this.dockingListener.setUndockedPreset(this.currentPresetLabel.getText());
            }

            if (UIConfig.DISPLAY_HEIGHT > 800) {
                this.targetDialogButtonAnchor = getLowestCenterChild(this.fleetTabLeftPane);
                this.fleetTabLeftPane.addComponent(this.presetFleetsButton).belowMid(this.targetDialogButtonAnchor, 10f);

            } else {
                this.fleetTabLeftPane.addComponent(this.presetFleetsButton).inTL(UIConfig.MANAGEMENT_BTN_X_OFFSET, UIConfig.MANAGEMENT_BTN_Y_OFFSET);
            }
            setCurrentPresetLabel(playerFleetMembers);
        }
    }

    private void reset() {
        this.fleetTabLeftPaneRef = null;
        this.autoAssignButton = null;
        this.fleetTab = null;
        this.fleetTabLeftPane = null;
        this.masterPresetsDialog = null;

        if (this.presetFleetsButton != null) {
            UIPanelAPI parent = utils.getParent(this.presetFleetsButton);
            if (parent != null) parent.removeComponent(this.presetFleetsButton);
            this.presetFleetsButton = null;
        }

        if (this.pullAllShipsButton != null) {
            UIPanelAPI parent = utils.getParent(this.pullAllShipsButton);
            if (parent != null) parent.removeComponent(this.pullAllShipsButton);
            this.pullAllShipsButton = null;
        }

        if (this.storeFleetButton != null) {
            UIPanelAPI parent = utils.getParent(this.storeFleetButton);
            if (parent != null) parent.removeComponent(this.storeFleetButton);
            this.storeFleetButton = null;
        }

        if (this.currentPresetLabelHeader != null) {
            UIPanelAPI parent = utils.labelGetParent(this.currentPresetLabelHeader);
            if (parent != null) parent.removeComponent((UIComponentAPI) this.currentPresetLabelHeader);
            this.currentPresetLabelHeader = null;
        }

        if (this.currentPresetLabel != null) {
            UIPanelAPI parent = utils.labelGetParent(this.currentPresetLabel);
            if (parent != null) parent.removeComponent((UIComponentAPI) this.currentPresetLabel);
            this.currentPresetLabel = null;
        }

        this.injected = false;
    }

    public UIPanelAPI getFleetTab() {
        return this.fleetTab;
    }

    public ButtonAPI getOfficerAutoAssignButton() {
        return this.autoAssignButton;
    }

    public Object getFleetPanelClickHandler() {
        return this.fleetPanelClickHandler;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    private UIComponentAPI getLowestCenterChild(UIPanelAPI leftPane) {
        List<UIComponentAPI> children = utils.getChildrenNonCopy(leftPane);
        int size = children.size();
        float[] centerXArr = new float[size];
        float[] yArr = new float[size];

        for (int i = 0; i < size; i++) {
            UIComponentAPI child = children.get(i);
            PositionAPI childPos = child.getPosition();

            centerXArr[i] = childPos.getCenterX();
            yArr[i] = childPos.getY();
        }

        float targetCenterX = PresetMiscUtils.getMostCommon(centerXArr);
        float targetY = PresetMiscUtils.getSmallest(yArr);

        List<UIComponentAPI> centerChildren = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            UIComponentAPI child = children.get(i);
            PositionAPI childPos = child.getPosition();

            if (childPos.getCenterX() == targetCenterX && childPos.getY() == targetY) return child;
            else centerChildren.add(child);
        }

        yArr = new float[centerChildren.size()];
        for (int i = 0; i< centerChildren.size(); i++) {
            yArr[i] = centerChildren.get(i).getPosition().getY();
        }

        targetY = PresetMiscUtils.getSmallest(yArr);
        for (UIComponentAPI child : centerChildren) {
            if (child.getPosition().getY() == targetY) return child;
        }

        return children.get(children.size() - 1);
    }

    private void setCurrentPresetLabel(List<FleetMemberAPI> playerFleetMembers) {
        FleetPreset preset = PresetUtils.getPresetOfMembers(playerFleetMembers);
        
        if (preset != null && UIConfig.IS_SET_CURRENT_PRESET_LABEL) {
            if (this.currentPresetLabel == null) {
                this.currentPresetLabelHeader = Global.getSettings().createLabel("Current Fleet Is Preset", Fonts.ORBITRON_12);
                this.currentPresetLabelHeader.setColor(Global.getSettings().getBasePlayerColor());
                this.currentPresetLabelHeader.setAlignment(Alignment.MID);

                this.currentPresetLabel = Global.getSettings().createLabel(preset.getName(), Fonts.ORBITRON_16);
                this.currentPresetLabel.setColor(Global.getSettings().getBasePlayerColor());
                this.currentPresetLabel.setHighlightColor(Global.getSettings().getBrightPlayerColor());
                this.currentPresetLabel.setHighlightOnMouseover(true);
                this.currentPresetLabel.setAlignment(Alignment.MID);
                
                if (UIConfig.DISPLAY_HEIGHT > 800) {
                    utils.uiPanelAdd(this.fleetTabLeftPane, (UIComponentAPI)this.currentPresetLabelHeader).belowMid(targetDialogButtonAnchor, presetFleetsButton.getPosition().getHeight() + 20f);
                    utils.uiPanelAdd(this.fleetTabLeftPane, (UIComponentAPI)this.currentPresetLabel).belowMid((UIComponentAPI)this.currentPresetLabelHeader, 5f);
                } else {
                    utils.uiPanelAdd(this.fleetTabLeftPane, (UIComponentAPI)this.currentPresetLabelHeader).inTL(UIConfig.CURRENT_PRESET_LABEL_X_OFFSET, UIConfig.CURRENT_PRESET_LABEL_Y_OFFSET);
                    utils.uiPanelAdd(this.fleetTabLeftPane, (UIComponentAPI)this.currentPresetLabel).belowMid((UIComponentAPI)this.currentPresetLabelHeader, 0f);
                }

            } else {
                if (!this.currentPresetLabel.getText().equals(preset.getName())) {
                    float newWidth = this.currentPresetLabel.computeTextWidth(preset.getName());
                    this.currentPresetLabel.setText(preset.getName());
                    this.currentPresetLabel.autoSizeToWidth(newWidth);
                }
            }

        } else if (this.currentPresetLabel != null) {
            if (UIConfig.DISPLAY_HEIGHT > 800) {
                this.fleetTabLeftPane.removeComponent((UIComponentAPI)this.currentPresetLabelHeader);
                this.fleetTabLeftPane.removeComponent((UIComponentAPI)this.currentPresetLabel);
            } else {
                this.fleetTabLeftPane.removeComponent((UIComponentAPI)this.currentPresetLabelHeader);
                this.fleetTabLeftPane.removeComponent((UIComponentAPI)this.currentPresetLabel);
            }
            this.currentPresetLabelHeader = null;
            this.currentPresetLabel = null;
        }
    }

    private void addAuxStorageButtons(List<FleetMemberAPI> playerFleetMembers, PositionAPI officerAutoAssignButtonPosition, List<FleetMemberAPI> mothballedShips, MarketAPI market) {
        PositionAPI storageButtonPosition = null;
        ButtonAPI storageButton = getStorageButton(this.fleetTab);

        if (storageButton != null) {
            storageButtonPosition = storageButton.getPosition();

            this.storeFleetButton = utils.factoryCreateRegularButton(
                " Store Entire Fleet",
                "graphics/fonts/orbitron12condensed.fnt",
                Misc.getDarkPlayerColor(),
                Misc.getBasePlayerColor(),
                Alignment.MID,
                CutStyle.TOP,
                new ActionListener() {
                    @Override
                    public void actionPerformed(Object arg0, Object arg1) {
                        PresetUtils.storeFleetInStorage();
                    }
                }.getProxy()
            );
            
            PositionAPI pos = utils.uiPanelAdd(this.fleetTabLeftPane, this.storeFleetButton);
            utils.positionSet(pos, officerAutoAssignButtonPosition);
            pos.setSize(storageButtonPosition.getWidth()-(storageButtonPosition.getWidth() / 4.8f), officerAutoAssignButtonPosition.getHeight()-6f);
            pos.setYAlignOffset(157f).setXAlignOffset(UIConfig.STORE_SHIPS_BTN_X_OFFSET);
            
            this.pullAllShipsButton = utils.factoryCreateRegularButton(
                "  Take all ships from storage",
                "graphics/fonts/orbitron12condensed.fnt",
                Misc.getDarkPlayerColor(),
                Misc.getBasePlayerColor(),
                Alignment.LMID,
                CutStyle.TOP,
                new ActionListener() {
                    @Override
                    public void actionPerformed(Object arg0, Object arg1) {
                        PresetUtils.takeAllShipsFromStorage();
                    }
                }.getProxy()
            );

            pos = this.fleetTabLeftPane.addComponent(this.pullAllShipsButton);
            utils.positionSet(pos, officerAutoAssignButtonPosition);
            pos.setSize(storageButtonPosition.getWidth()+30f, officerAutoAssignButtonPosition.getHeight()-6f);
            pos.setYAlignOffset(157f).setXAlignOffset(UIConfig.TAKE_SHIPS_BTN_X_OFFSET);

            if (!PresetUtils.isPlayerPaidForStorage(CargoPresetUtils.getStorageSubmarket(market).getPlugin())) {
                if (this.storeFleetButton.isEnabled()) this.storeFleetButton.setEnabled(false);
                if (this.pullAllShipsButton.isEnabled()) this.pullAllShipsButton.setEnabled(false);

            } else {
                if (playerFleetMembers.size() == 1) {
                    if (this.storeFleetButton.isEnabled()) this.storeFleetButton.setEnabled(false);
                } else if (mothballedShips == null || mothballedShips.size() == 0) {
                    if (this.pullAllShipsButton.isEnabled()) this.pullAllShipsButton.setEnabled(false);
                }
            }

        } else {
            this.storeFleetButton = null;
            this.pullAllShipsButton = null;
        }
    }

    private void updateAuxStorageButtons(List<FleetMemberAPI> playerFleetMembers, List<FleetMemberAPI> mothballedShips, MarketAPI market) {
        if (this.pullAllShipsButton == null || this.storeFleetButton == null) addAuxStorageButtons(playerFleetMembers, officerAutoAssignButtonPosition, mothballedShips, market);
        if (mothballedShips != null && mothballedShips.size() > 0) {
            if (!this.pullAllShipsButton.isEnabled()) this.pullAllShipsButton.setEnabled(true);
        } else {
            if (this.pullAllShipsButton.isEnabled()) this.pullAllShipsButton.setEnabled(false);
        }

        if (!this.storeFleetButton.isEnabled() && playerFleetMembers.size() > 1) this.storeFleetButton.setEnabled(true);
        else if (this.storeFleetButton.isEnabled() && playerFleetMembers.size() < 2) this.storeFleetButton.setEnabled(false);
    }
}