package data.scripts.rat_frontiers_interactions;

import com.fs.starfarer.api.campaign.InteractionDialogPlugin;

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

    private SettlementData data;
    private InteractionDialogPlugin previousPlugin;
    private boolean dontReAddLargePlanet = false;

    public WrappedSettlementInteraction(SettlementInteraction wrapped) {
        this.wrapped = wrapped;
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
        
        if (arg0.equals("Manage Settlement")) {
            Global.getSector().addTransientScript(new EveryFrameScript() {
                private boolean isDone = false;
        
                private boolean isAbandoned() {
                    return !Global.getSector().getIntelManager().getIntel(wrapped.getData().getIntel().getClass()).contains(wrapped.getData().getIntel());
                }
        
                @Override
                public void advance(float arg0) {
                    if (isAbandoned()) {
                        Global.getSector().getListenerManager().getListeners(ColonyAbandonListener.class).get(0).reportPlayerAbandonedColony(wrapped.getData().getSettlementEntity().getMarket());
                        Global.getSector().removeScript(this);
                        isDone = true;
                        return;
                    }
        
                    if (Global.getSector().getCampaignUI().getCurrentInteractionDialog() == null) {
                        Global.getSector().removeScript(this);
                        isDone = true;
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
    }

    public void populateOptions() {
        wrapped.populateOptions();
    }
}
