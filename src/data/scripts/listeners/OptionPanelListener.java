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
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.campaign.CommDirectoryEntry;

import assortment_of_things.frontiers.interactions.CreateSettlementInteraction;
import assortment_of_things.frontiers.interactions.SettlementInteraction;
import assortment_of_things.misc.RATInteractionPlugin;
import data.scripts.ClassRefs;
import data.scripts.util.PresetMiscUtils;
import data.scripts.util.ReflectionUtilis;

import java.awt.Button;
import java.util.*;

import javax.print.attribute.HashAttributeSet;

@SuppressWarnings("unchecked")
public abstract class OptionPanelListener {
    public void print(Object... args) {
        PresetMiscUtils.print(args);
    }

    private static boolean haveRAT = Global.getSettings().getModManager().getModSpec("assortment_of_things") != null;

    private InteractionDialogAPI dialog;
    private OptionPanelAPI optionPanel;
    private VisualPanelAPI visualPanel;
    private OptionPanelListener self;
    private InteractionDialogPlugin plugin;
    private Map<Object, Object> buttonsToItemsMap;
    private String currentOption;

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
            currentButtons.clear();
            currentConfirmButtons.clear();

            Global.getSector().addTransientScript(new EveryFrameScript() {
                boolean isDone = false;

                @Override
                public void advance(float arg0) {
                    if (optionPanel.hasOptions()) {
                        isDone = true;
                        Global.getSector().removeScript(this);
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
                
            });
            return;
        }
        Set<Object> newButtons = new HashSet<>();

        for (Map.Entry<Object, Object> entry : buttonsToItemsMap.entrySet()) {
            if (currentButtons.contains(entry.getKey())) continue;
            currentButtons.add(entry.getKey());
            newButtons.add(entry.getKey());

            for (Object field : entry.getValue().getClass().getDeclaredFields()) {
                Object val = ReflectionUtilis.getPrivateVariable(field, entry.getValue());

                if (val != null && ReflectionUtilis.getFieldType(field).equals(Object.class)) {
                    // val is Option 'data'
                    currentOptions.add(val);

                    Object oldListener = ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonGetListenerMethod, entry.getKey());
                    ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonSetListenerMethod, entry.getKey(), new ReflectionUtilis.ListenerFactory.ActionListener() {
                        @Override
                        public void trigger(Object arg0, Object arg1) {
                            if (arg1 == entry.getKey()) {
                                ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonListenerActionPerformedMethod, oldListener, arg0, arg1);

                                if (optionPanel.optionHasConfirmDelegate(val)) {
                                    // option (usually) opens a confirm dialog, but not in the case of CONTINUE_INTO_BATTLE for example

                                    if (haveRAT && (dialog.getPlugin() instanceof CreateSettlementInteraction || dialog.getPlugin() instanceof SettlementInteraction)) {
                                        // need to find reference to the CustomVisualDialog as it's different to the traditional
                                    } 

                                    List<Object> children = (List<Object>) ReflectionUtilis.invokeMethodDirectly(ClassRefs.visualPanelGetChildrenNonCopyMethod, visualPanel);
                                    Object child = children.get(children.size()-1); // the confirm dialog
                                    // (ButtonAPI) ReflectionUtilis.getMethodAndInvokeDirectly("getButton", child, 1, 0), // Yes
                                    // (ButtonAPI) ReflectionUtilis.getMethodAndInvokeDirectly("getButton", child, 1, 1) // No
                                    // ReflectionUtilis.logMethods(child);

                                    // Yes button
                                    Object yesButton = ReflectionUtilis.getMethodAndInvokeDirectly("getButton", child, 1, 0);
                                    if (yesButton != null) {
                                        setConfirmListener(yesButton, val, newButtons);

                                    } else {
                                        Object innerPanel = ReflectionUtilis.getMethodAndInvokeDirectly("getInnerPanel", child, 0);
                                        
                                        if (innerPanel == null) {
                                            execute(val);
                                            if (String.valueOf(val).equals("CONTINUE_INTO_BATTLE")) {
                                                onPlayerEnterBattle();
                                                updateButtons(newButtons);
                                                return;
                                            }

                                            updateButtons(newButtons);
                                            populateOptions();
                                            return;
                                        }

                                        List<Object> innerChildren = (List<Object>) ReflectionUtilis.getMethodAndInvokeDirectly("getChildrenNonCopy", innerPanel, 0);
                                        Set<Object> nonButtons = new HashSet<>();

                                        if (innerChildren != null) {
                                            for (Object child_ : innerChildren) {
                                                if (ButtonAPI.class.isAssignableFrom(child_.getClass()) && !currentConfirmButtons.contains(child)) {
                                                    String buttonText = ((ButtonAPI) child_).getText().toLowerCase();

                                                    if (!buttonText.contains("cancel") || !buttonText.contains("no") || !buttonText.contains("dismiss") || !buttonText.contains("leave") || !buttonText.contains("back")) {
                                                        setConfirmListener(child_, val, newButtons);
                                                        currentConfirmButtons.add(child_);
                                                        return;
                                                    }

                                                } else {
                                                    nonButtons.add(child_);
                                                }
                                            }

                                            if (String.valueOf(val).equals("marketCommDir")) handleCommDirectory(nonButtons, newButtons);
                                            // for confirmation with only dismiss button such as marketCommDir
                                            execute(val);
                                            if (!newButtons.equals(currentButtons)) {
                                                updateButtons(newButtons);
                                                populateOptions();
                                            }
                                            return;

                                            // fallback, no buttons found?
                                        } else {
                                            execute(val);
                                            updateButtons(newButtons);
                                            populateOptions();
                                            return;
                                        }
                                    }
                                    // For the no button - we dont need this because natively the game does not call optionSelected when this is pressed
                                } else {
                                    execute(val);
                                    updateButtons(newButtons);
                                    populateOptions();
                                    return;
                                }
                            }
                        }
                    }.getProxy());
                    break;
                }
            }
        }
        updateButtons(newButtons);
    }

    private void execute(Object optionData) {
        currentOption = String.valueOf(optionData);
        onOptionSelected(optionData);
    }

    public abstract void onOptionSelected(Object optionData);

    public String getCurrentOption() {
        return currentOption;
    }

    private void updateButtons(Set<Object> newButtons) {
        if (!currentButtons.equals(newButtons)) {
            currentButtons = newButtons;
            currentConfirmButtons.clear();
        }
    }
    
    private void onPlayerEnterBattle() {
        Global.getSector().addTransientListener(new BaseCampaignEventListener(false) {
            @Override
            public void reportPlayerEngagement(EngagementResultAPI e) {;
                currentButtons.clear();
                currentConfirmButtons.clear();
                Global.getSector().removeListener(this);

                // i dont know why we have to do this you would think you could just put the advance logic up here and it would be fine but no it gives the pre combat map for some reason if we dont do this
                Global.getSector().addTransientScript(new EveryFrameScript() {
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
                        isDone = true;
                    }
                });
                
            }
        });
    }

    private void setConfirmListener(Object button, Object optionData, Set<Object> newButtons) {
        Object oldListener = ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonGetListenerMethod, button);
        ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonSetListenerMethod, button, new ReflectionUtilis.ListenerFactory.ActionListener() {
            @Override
            public void trigger(Object arg0, Object arg1) {
                if (arg1 == button) {
                    if (String.valueOf(optionData).equals("AUTORESOLVE_PURSUE") || String.valueOf(optionData).equals("CONTINUE_INTO_BATTLE")) onPlayerEnterBattle();

                    ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonListenerActionPerformedMethod, oldListener, arg0, arg1);
                    execute(optionData);
                    updateButtons(newButtons);
                    populateOptions();
                }
            }
        }.getProxy());
    }

    private void handleCommDirectory(Set<Object> innerPanelNonButtons, Set<Object> newButtons) {
        for (Object nonButton : innerPanelNonButtons) {
            for (Object child : (List<Object>) ReflectionUtilis.getMethodAndInvokeDirectly("getChildrenNonCopy", nonButton, 0)) {
                List<Object> lst = (List<Object>) ReflectionUtilis.getMethodAndInvokeDirectly("getItems", child, 0);
                
                if (lst != null) {
                    // these are the buttons for the comm directory entries, there is a field for this particular child that maps to CommDirectoryEntry with its keys being these buttons
                    for (Object o : lst) {
                        if (currentConfirmButtons.contains(o)) continue;

                        currentConfirmButtons.add(o);
                        setConmmDirectoryButtonListener(o, newButtons);
                    }
                }
            }
        }
    }

    private void setConmmDirectoryButtonListener(Object button, Set<Object> newButtons) {
        Object oldListener = ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonGetListenerMethod, button);
        ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonSetListenerMethod, button, new ReflectionUtilis.ListenerFactory.ActionListener() {
            @Override
            public void trigger(Object arg0, Object arg1) {
                if (arg1 == button) {
                    ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonListenerActionPerformedMethod, oldListener, arg0, arg1);
                    updateButtons(newButtons);
                    populateOptions();
                }
            }
        }.getProxy());
    }
}