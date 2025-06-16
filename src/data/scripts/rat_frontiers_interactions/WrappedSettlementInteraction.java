package data.scripts.rat_frontiers_interactions;

import com.fs.starfarer.api.campaign.InteractionDialogPlugin;

import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;

import data.scripts.listeners.ColonyAbandonListener;
import data.scripts.listeners.DockingListener;

import data.scripts.util.PresetMiscUtils;
import data.scripts.util.ReflectionUtilis;

import assortment_of_things.frontiers.data.FrontiersData;
import assortment_of_things.frontiers.SettlementData;
import assortment_of_things.frontiers.interactions.SettlementInteraction;
import assortment_of_things.misc.RATInteractionPlugin;

// SettlementInteraction is final, we cant inherit so have to do this and transplant
public class WrappedSettlementInteraction extends RATInteractionPlugin {
    public void print(Object... args) {
        PresetMiscUtils.print(args);
    }

    private final SettlementInteraction wrapped;
    private final DockingListener dockingListener;

    private SettlementData data;
    private InteractionDialogPlugin previousPlugin;
    private boolean dontReAddLargePlanet = false;
    private boolean checkingAbandon = false;

    final WrappedSettlementInteraction self = this;

    public WrappedSettlementInteraction(SettlementInteraction wrapped, DockingListener dockingListener) {
        this.wrapped = wrapped;
        this.dockingListener = dockingListener;
    }

    @Override
    public void init() {
        wrapped.init();
    }

    public void setData(SettlementData data) {
        this.data = data;
    }

    @Override
    public void optionSelected(String arg0, Object arg1) {
        
        if (arg0.equals("Manage Settlement") && !checkingAbandon) {
            checkingAbandon = true;

            Global.getSector().addTransientScript(new EveryFrameScript() {
                private boolean isDone = false;
        
                private boolean isAbandoned() {
                    return !Global.getSector().getIntelManager().getIntel(wrapped.getData().getIntel().getClass()).contains(wrapped.getData().getIntel());
                }
        
                @Override
                public void advance(float arg0) {
                    
                    if (isAbandoned()) {
                        Global.getSector().getListenerManager().getListeners(ColonyAbandonListener.class).get(0).reportPlayerAbandonedColony(wrapped.getData().getSettlementEntity().getMarket());
                        isDone = true;
                        checkingAbandon = false;
                        return;
                    }
                    
                    if (Global.getSector().getCampaignUI().getCurrentInteractionDialog() == null) {
                        isDone = true;
                        checkingAbandon = false;
                        return;
                    }

                    if (dialog.getPlugin() != self) {
                        isDone = true;
                        checkingAbandon = false;
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
            });
        }
        wrapped.optionSelected(arg0, arg1);

        if (arg0.equals("Back")) {
            dockingListener.reportPlayerClosedMarket(wrapped.getData().getSettlementEntity().getMarket());
            dockingListener.reportPlayerOpenedMarket(wrapped.getData().getPrimaryPlanet().getMarket());
        }
    }

    public void populateOptions() {
        wrapped.populateOptions();
    }
}
