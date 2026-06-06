package data.scripts.listeners;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.VisualPanelAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.campaign.CommDirectoryEntry;

import data.scripts.util.UiUtil;
import data.scripts.util.UiUtil.ActionListener;

import static data.scripts.util.UiUtil.utils;

import java.util.*;

public abstract class OptionPanelListener {
    private static final Class<?> proxyListenerClass = new ActionListener() {
        public void actionPerformed(Object arg0, Object arg1) {}
    }.getProxy().getClass();

    private InteractionDialogAPI dialog;
    private OptionPanelAPI optionPanel;
    private VisualPanelAPI visualPanel;
    private OptionPanelListener self;
    private InteractionDialogPlugin plugin;
    private Map<ButtonAPI, Object> buttonsToItemsMap;

    private Object currentCommDirectoryEntryData = null; // this is usually a Person object
    private Object currentOption = null;

    private Set<Object> currentOptions = new HashSet<>();
    private Set<UIComponentAPI> currentButtons = new HashSet<>();
    private Set<UIComponentAPI> currentConfirmButtons = new HashSet<>();

    public OptionPanelListener(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        this.optionPanel = dialog.getOptionPanel();
        this.visualPanel = dialog.getVisualPanel();
        this.plugin = dialog.getPlugin();
        this.buttonsToItemsMap = utils.optionPanelGetButtonToItemMap(optionPanel);
        this.self = this;

        populateOptions();
    }

    private void reinit(InteractionDialogAPI dialog_) {
        this.dialog = dialog_;
        this.optionPanel = dialog_.getOptionPanel();
        this.visualPanel = dialog_.getVisualPanel();
        this.plugin = dialog_.getPlugin();
        this.buttonsToItemsMap = utils.optionPanelGetButtonToItemMap(optionPanel);

        currentOption = null;
        currentOptions.clear();
        currentButtons.clear();
        currentConfirmButtons.clear();
        populateOptions();
    }

    private void populateOptions() {
        InteractionDialogAPI dialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
        if (dialog == null) return;
        if (isNewDialog(dialog)) {
            reinit(dialog);
            return;
        }

        if (!this.optionPanel.hasOptions()) {
            currentOptions.clear();
            currentButtons.clear();
            currentConfirmButtons.clear();
            Global.getSector().addTransientScript(new BackGroundOptionChecker());
            return;
        }
        Set<UIComponentAPI> newButtons = new HashSet<>();
        Set<Object> newOptions = new HashSet<>();

        for (Map.Entry<ButtonAPI, Object> entry : buttonsToItemsMap.entrySet()) {
            ButtonAPI optionButton = entry.getKey();
            if (currentButtons.contains(optionButton)) continue;
            newButtons.add(optionButton);

            Object oldListener = utils.buttonGetListener(optionButton);
            if (oldListener.getClass().equals(proxyListenerClass)) continue;

            Object optionData = utils.optionPanelItemGetOptionData(entry.getValue());
            if (optionData != null) {
                newOptions.add(optionData);

                // add the interceptor listener for the option
                utils.buttonSetListener(optionButton, new ActionListener() {
                    @Override
                    public void actionPerformed(Object arg0, Object arg1) {
                        if (arg1 == optionButton) {
                            if (optionPanel.optionHasConfirmDelegate(optionData)) {
                                // option (usually) opens a confirm dialog, but not in the case of CONTINUE_INTO_BATTLE and also Manage Storage/Refit Fleet options in rat settlements for example
                                if (String.valueOf(optionData).equals("CONTINUE_INTO_BATTLE")) {
                                    onPlayerEnterBattle();
                                }

                                utils.actionPerformed(oldListener, arg0, arg1);
                                
                                List<UIComponentAPI> children = utils.getChildrenNonCopy((UIPanelAPI)visualPanel);
                                Object confirmDialog = children.get(children.size()-1); // the standard confirm dialog
                                // (ButtonAPI) RolfLectionUtil.getMethodAndInvokeDirectly("getButton", child, 1, 0), // Yes
                                // (ButtonAPI) RolfLectionUtil.getMethodAndInvokeDirectly("getButton", child, 1, 1); // No

                                // Yes button
                                ButtonAPI yesButton = null;
                                if (UiUtil.confirmDialogClass.isInstance(confirmDialog)) {
                                    yesButton = utils.confirmDialogGetButton(confirmDialog, 0);

                                    if (yesButton != null) {
                                        setConfirmListener(yesButton, optionData, newButtons, newOptions);
                                        return;
                                    }
                                }
                                // the confirm button is possibly nested, such as in the case of "transfer command for this engagement"
                                UIPanelAPI innerPanel = utils.confirmDialogGetInnerPanel(confirmDialog);

                                if (innerPanel == null) {
                                    executeAfter(optionData);
                                    updateOptions(newButtons, newOptions);
                                    populateOptions();
                                    return;
                                }

                                List<UIComponentAPI> innerChildren = UiUtil.getChildrenRecursive(innerPanel);
                                Set<UIComponentAPI> nonButtons = new HashSet<>();

                                if (innerChildren != null) {
                                    for (UIComponentAPI child : innerChildren) {
                                        if (child instanceof ButtonAPI button && !currentConfirmButtons.contains(button)) {
                                            if (button.getText() == null) continue;
                                            String buttonText = button.getText().toLowerCase();

                                            if (!isNoButton(buttonText)) {
                                                setConfirmListener(button, optionData, newButtons, newOptions);
                                                currentConfirmButtons.add(child);
                                                return;
                                            }
                                        } else {
                                            nonButtons.add(child);
                                        }
                                    }

                                    if (String.valueOf(optionData).equals("marketCommDir")) handleCommDirectory(nonButtons, newButtons, newOptions);

                                    // for confirmation with only dismiss button such as marketCommDir
                                    executeAfter(optionData);

                                    updateOptions(newButtons, newOptions);

                                    if (!Global.getSector().hasTransientScript(ButtonChecker.class) && !Global.getSector().hasTransientScript(BackGroundOptionChecker.class)) {
                                        Global.getSector().addTransientScript(new ButtonChecker());
                                    }
                                    return;

                                    // fallback, no buttons found?
                                } else {
                                    executeAfter(optionData);
                                    updateOptions(newButtons, newOptions);
                                    populateOptions();
                                    return;
                                }
                                // Natively the game does not call optionSelected when the no button is pressed

                            } else {
                                utils.actionPerformed(oldListener, arg0, arg1);
                                executeAfter(optionData);

                                updateOptions(newButtons, newOptions);
                                populateOptions();
                                return;
                            }
                        }
                    }
                }.getProxy());
            }
        }
        updateOptions(newButtons, newOptions);
    }

    private void executeAfter(Object optionData) {
        currentOption = optionData;
        afterOptionSelected(optionData);
    }

    public abstract void afterOptionSelected(Object optionData);

    public Object getCurrentOption() {
        return currentOption;
    }

    public Set<Object> getCurrentOptions() {
        return currentOptions;
    }

    private void updateOptions(Set<UIComponentAPI> newButtons, Set<Object> newOptions) {
        currentOptions = newOptions;
        currentButtons = newButtons;
        currentConfirmButtons.clear();
    }
    
    private void onPlayerEnterBattle() {
        Global.getSector().addTransientListener(new BaseCampaignEventListener(false) {
            @Override
            public void reportPlayerEngagement(EngagementResultAPI e) {;
                currentButtons.clear();
                currentConfirmButtons.clear();
                Global.getSector().removeListener(this);

                // i dont know why we have to do this you would think you could just put the advance logic up here and it would be fine but no it gives the pre combat map for some reason if we dont do this
                Global.getSector().addTransientScript(new ButtonToItemsMapRefFixer());
            }
        });
    }

    private void setConfirmListener(UIComponentAPI button, Object optionData, Set<UIComponentAPI> newButtons, Set<Object> newOptions) {
        Object oldListener = utils.buttonGetListener(button);
        utils.buttonSetListener(button, new ActionListener() {
            @Override
            public void actionPerformed(Object arg0, Object arg1) {
                if (arg1 == button) {
                    if (String.valueOf(optionData).equals("AUTORESOLVE_PURSUE") || String.valueOf(optionData).equals("CONTINUE_INTO_BATTLE")) onPlayerEnterBattle();

                    utils.actionPerformed(oldListener, arg0, arg1);
                    executeAfter(optionData);

                    updateOptions(newButtons, newOptions);
                    Global.getSector().addTransientScript(new ButtonToItemsMapRefFixer());
                }
            }
        }.getProxy());
    }

    private void handleCommDirectory(Set<UIComponentAPI> innerPanelNonButtons, Set<UIComponentAPI> newButtons, Set<Object> newOptions) {
        for (UIComponentAPI nonButton : innerPanelNonButtons) {
            List<UIComponentAPI> children = utils.getChildrenNonCopy(nonButton);
            if (children != null) {
                for (UIComponentAPI child : children) {
                    List<UIComponentAPI> lst = utils.listPanelGetItems(child);
                    if (lst != null) {
                        // these are the buttons for the comm directory entries, there is a field for this particular child that maps to CommDirectoryEntry with its keys being these buttons
                        Map<ButtonAPI, CommDirectoryEntry> buttonToCommDirectoryEntryMap = (Map<ButtonAPI, CommDirectoryEntry>) UiUtil.listPanelMapHandle.get(child);
                        if (buttonToCommDirectoryEntryMap.isEmpty()) continue;

                        for (UIComponentAPI o : lst) {
                            if (currentConfirmButtons.contains(o)) continue;
    
                            currentConfirmButtons.add(o);
                            setCommmDirectoryButtonListener(o, newButtons, newOptions, buttonToCommDirectoryEntryMap.get(o).getEntryData());
                        }
                    }
                }
            }
        }
    }

    public Object getCurrentCommDirectoryEntryData() {
        return this.currentCommDirectoryEntryData; // this is usually a Person Object
    }

    private void setCommmDirectoryButtonListener(UIComponentAPI button, Set<UIComponentAPI> newButtons, Set<Object> newOptions, Object commDirEntryData) {
        Object oldListener = utils.buttonGetListener(button);
        utils.buttonSetListener(button, new ActionListener() {
            @Override
            public void actionPerformed(Object arg0, Object arg1) {
                if (arg1 == button) {
                    currentCommDirectoryEntryData = commDirEntryData;
                    
                    utils.actionPerformed(oldListener, arg0, arg1);
                    updateOptions(newButtons, newOptions);
                    populateOptions();
                }
            }
        }.getProxy());
    }

    private class ButtonToItemsMapRefFixer implements EveryFrameScript {
        boolean isDone = false;
        @Override
        public boolean isDone() {
            return isDone;
        }

        @Override
        public boolean runWhilePaused() {
            return true;
        }

        @Override
        public void advance(float amount) {
            self.buttonsToItemsMap = utils.optionPanelGetButtonToItemMap(optionPanel);
            populateOptions();
            Global.getSector().removeTransientScript(this);
            isDone = true;
        }
    }

    private class ButtonChecker implements EveryFrameScript {
        private boolean isDone = false;

        @Override
        public void advance(float arg0) {
            if (Global.getSector().getCampaignUI().getCurrentInteractionDialog() == null) {
                Global.getSector().removeTransientScript(this);
                isDone = true;
                return;
            }
            if (!buttonsToItemsMap.keySet().equals(currentButtons)) {
                populateOptions();
                isDone = true;
                Global.getSector().removeTransientScript(this);
                return;
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
    }

    private class BackGroundOptionChecker implements EveryFrameScript {
        private boolean isDone = false;
        @Override
        public void advance(float arg0) {
            if (optionPanel.hasOptions()) {
                isDone = true;
                Global.getSector().removeTransientScript(this);
                populateOptions();
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
    }

    private boolean isNewDialog(InteractionDialogAPI dialog) {
        return dialog != this.dialog 
            || this.optionPanel != dialog.getOptionPanel()
            || this.visualPanel != dialog.getVisualPanel()
            || this.plugin != dialog.getPlugin();
    }

    private boolean isNoButton(String buttonText) {
        return !buttonText.contains("all")
        && !buttonText.contains("cancel")
        && !buttonText.contains("no")
        && !buttonText.contains("dismiss")
        && !buttonText.contains("leave")
        && !buttonText.contains("back")
        && !buttonText.contains("never");
    }

    public InteractionDialogAPI getDialog() {
        return this.dialog;
    }
}