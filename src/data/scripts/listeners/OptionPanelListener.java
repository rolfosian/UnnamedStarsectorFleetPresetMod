package data.scripts.listeners;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.CustomDialogDelegate;
import com.fs.starfarer.api.campaign.VisualPanelAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.campaign.CommDirectoryEntry;

import assortment_of_things.frontiers.interactions.CreateSettlementInteraction;
import assortment_of_things.frontiers.interactions.SettlementInteraction;
import assortment_of_things.misc.RATInteractionPlugin;
import data.scripts.ClassRefs;
import data.scripts.util.PresetMiscUtils;
import data.scripts.util.ReflectionUtilis;
import data.scripts.util.ListenerFactory.ActionListener;
import data.scripts.util.UtilReflection;

import java.util.*;

@SuppressWarnings("unchecked")
public abstract class OptionPanelListener {
    public void print(Object... args) {
        PresetMiscUtils.print(args);
    }

    private static Class<?> proxyListenerClass = new ActionListener() {
        public void trigger(Object... args) {}
    }.getProxy().getClass();

    private InteractionDialogAPI dialog;
    private OptionPanelAPI optionPanel;
    private VisualPanelAPI visualPanel;
    private OptionPanelListener self;
    private InteractionDialogPlugin plugin;
    private Map<Object, Object> buttonsToItemsMap;
    private Object currentOption;

    private Set<Object> currentOptions = new HashSet<>();
    private Set<Object> currentButtons = new HashSet<>();
    private Set<Object> currentConfirmButtons = new HashSet<>();

    public OptionPanelListener(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        this.optionPanel = dialog.getOptionPanel();
        this.visualPanel = dialog.getVisualPanel();
        this.plugin = dialog.getPlugin();
        this.buttonsToItemsMap = (Map<Object, Object>) ReflectionUtilis.invokeMethodDirectly(ClassRefs.optionPanelGetButtonToItemMapMethod, optionPanel);
        this.self = this;

        populateOptions();
    }

    private void reinit(InteractionDialogAPI dialog_) {
        this.dialog = dialog_;
        this.optionPanel = dialog_.getOptionPanel();
        this.visualPanel = dialog_.getVisualPanel();
        this.plugin = dialog_.getPlugin();
        this.buttonsToItemsMap = (Map<Object, Object>) ReflectionUtilis.invokeMethodDirectly(ClassRefs.optionPanelGetButtonToItemMapMethod, optionPanel);

        currentOption = null;
        currentOptions.clear();
        currentButtons.clear();
        currentConfirmButtons.clear();
        populateOptions();
    }

    private void populateOptions() {
        InteractionDialogAPI dialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
        if (dialog == null) return;
        if (dialog != this.dialog || this.optionPanel != dialog.getOptionPanel() || this.visualPanel != dialog.getVisualPanel() || this.plugin != dialog.getPlugin()) {
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
        Set<Object> newButtons = new HashSet<>();
        Set<Object> newOptions = new HashSet<>();

        for (Map.Entry<Object, Object> entry : buttonsToItemsMap.entrySet()) {
            if (currentButtons.contains(entry.getKey())) continue;
            newButtons.add(entry.getKey());

            Object oldListener = ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonGetListenerMethod, entry.getKey());
            if (oldListener.getClass().equals(proxyListenerClass)) continue;

            Object optionData = ReflectionUtilis.invokeMethodDirectly(ClassRefs.getOptionDataMethod, entry.getValue());
            if (optionData != null) {
                newOptions.add(optionData);

                // add the interceptor listener for the option
                ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonSetListenerMethod, entry.getKey(), new ActionListener() {
                    @Override
                    public void trigger(Object... args) {
                        if (args[1] == entry.getKey()) {
                            if (optionPanel.optionHasConfirmDelegate(optionData)) {
                                // option (usually) opens a confirm dialog, but not in the case of CONTINUE_INTO_BATTLE and also Manage Storage/Refit Fleet options in rat settlements for example
                                if (String.valueOf(optionData).equals("CONTINUE_INTO_BATTLE")) {
                                    onPlayerEnterBattle();
                                }

                                ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonListenerActionPerformedMethod, oldListener, args);
                                
                                List<Object> children = (List<Object>) ReflectionUtilis.invokeMethodDirectly(ClassRefs.visualPanelGetChildrenNonCopyMethod, visualPanel);
                                Object confirmDialog = children.get(children.size()-1); // the standard confirm dialog
                                // (ButtonAPI) ReflectionUtilis.getMethodAndInvokeDirectly("getButton", child, 1, 0), // Yes
                                // (ButtonAPI) ReflectionUtilis.getMethodAndInvokeDirectly("getButton", child, 1, 1); // No

                                // Yes button
                                Object yesButton = ReflectionUtilis.getMethodAndInvokeDirectly("getButton", confirmDialog, 1, 0);
                                if (yesButton != null) {
                                    setConfirmListener(yesButton, optionData, newButtons, newOptions);

                                } else {
                                    // the confirm button is possibly nested, such as in the case of "transfer command for this engagement"
                                    Object innerPanel = ReflectionUtilis.getMethodAndInvokeDirectly("getInnerPanel", confirmDialog, 0);
                                    
                                    if (innerPanel == null) {
                                        executeAfter(optionData);
                                        updateOptions(newButtons, newOptions);
                                        populateOptions();
                                        return;
                                    }

                                    List<Object> innerChildren = UtilReflection.getChildrenRecursive(innerPanel);
                                    Set<Object> nonButtons = new HashSet<>();

                                    if (innerChildren != null) {
                                        for (Object child : innerChildren) {
                                            if (ButtonAPI.class.isAssignableFrom(child.getClass()) && !currentConfirmButtons.contains(child)) {
                                                if (((ButtonAPI) child).getText() == null) continue;
                                                String buttonText = ((ButtonAPI) child).getText().toLowerCase();

                                                if (currentConfirmButtons.contains(child)) continue;

                                                if (!buttonText.contains("all") && !buttonText.contains("cancel") && !buttonText.contains("no") && !buttonText.contains("dismiss") && !buttonText.contains("leave") && !buttonText.contains("back") && !buttonText.contains("never")) {
                                                    setConfirmListener(child, optionData, newButtons, newOptions);
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
                                            Global.getSector().addTransientScript(new ButtonChecker(newButtons, newOptions));
                                        }
                                        return;

                                        // fallback, no buttons found?
                                    } else {
                                        executeAfter(optionData);
                                        updateOptions(newButtons, newOptions);
                                        populateOptions();
                                        return;
                                    }
                                }
                                // Natively the game does not call optionSelected when the no button is pressed

                            } else {
                                ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonListenerActionPerformedMethod, oldListener, args);
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
        // print(currentOptions);
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

    private void updateOptions(Set<Object> newButtons, Set<Object> newOptions) {
        currentOptions = newOptions;
        currentButtons = newButtons;
        currentConfirmButtons.clear();
        // if (!currentOptions.equals(newOptions)) currentOptions = newOptions;
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

    private void setConfirmListener(Object button, Object optionData, Set<Object> newButtons, Set<Object> newOptions) {
        Object oldListener = ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonGetListenerMethod, button);
        ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonSetListenerMethod, button, new ActionListener() {
            @Override
            public void trigger(Object... args) {
                if (args[1] == button) {
                    if (String.valueOf(optionData).equals("AUTORESOLVE_PURSUE") || String.valueOf(optionData).equals("CONTINUE_INTO_BATTLE")) onPlayerEnterBattle();

                    ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonListenerActionPerformedMethod, oldListener, args);
                    executeAfter(optionData);

                    updateOptions(newButtons, newOptions);
                    Global.getSector().addTransientScript(new ButtonToItemsMapRefFixer());
                }
            }
        }.getProxy());
    }

    private void handleCommDirectory(Set<Object> innerPanelNonButtons, Set<Object> newButtons, Set<Object> newOptions) {
        for (Object nonButton : innerPanelNonButtons) {
            List<Object> children = (List<Object>) ReflectionUtilis.getMethodAndInvokeDirectly("getChildrenNonCopy", nonButton, 0);
            if (children != null) {
                for (Object child : children) {
                    List<Object> lst = (List<Object>) ReflectionUtilis.getMethodAndInvokeDirectly("getItems", child, 0);
                    
                    if (lst != null) {
                        // these are the buttons for the comm directory entries, there is a field for this particular child that maps to CommDirectoryEntry with its keys being these buttons
                        for (Object o : lst) {
                            if (currentConfirmButtons.contains(o)) continue;
    
                            currentConfirmButtons.add(o);
                            setCommmDirectoryButtonListener(o, newButtons, newOptions);
                        }
                    }
                }
            }
        }
    }

    private void setCommmDirectoryButtonListener(Object button, Set<Object> newButtons, Set<Object> newOptions) {
        Object oldListener = ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonGetListenerMethod, button);
        ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonSetListenerMethod, button, new ActionListener() {
            @Override
            public void trigger(Object... args) {
                if (args[1] == button) {
                    ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonListenerActionPerformedMethod, oldListener, args);
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
                self.buttonsToItemsMap = (Map<Object, Object>)ReflectionUtilis.invokeMethodDirectly(ClassRefs.optionPanelGetButtonToItemMapMethod, optionPanel);
                populateOptions();
                Global.getSector().removeTransientScript(this);
                isDone = true;
            }
    }

    private class ButtonChecker implements EveryFrameScript {
        private boolean isDone = false;
        private Set<Object> newButtons;
        private Set<Object> newOptions;

        public ButtonChecker(Set<Object> newButtons, Set<Object> newOptions) {
            this.newButtons = newButtons;
            this.newOptions = newOptions;
            
        }

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
}