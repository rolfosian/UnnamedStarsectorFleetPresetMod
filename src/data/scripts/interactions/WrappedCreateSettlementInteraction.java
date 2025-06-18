package data.scripts.interactions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;

import java.util.*;

import data.scripts.listeners.DockingListener;
import data.scripts.util.PresetMiscUtils;
import data.scripts.util.ReflectionUtilis;
import assortment_of_things.frontiers.data.SiteData;
import assortment_of_things.frontiers.interactions.CreateSettlementInteraction;
import assortment_of_things.frontiers.interactions.SettlementInteraction;
import assortment_of_things.misc.RATInteractionPlugin;
import assortment_of_things.misc.RATSettings;
import lunalib.lunaSettings.LunaSettings;

// CreateSettlementInteraction is final, we cant inherit so have to do this and transplant
public class WrappedCreateSettlementInteraction extends RATInteractionPlugin {
    public static void print(Object... args) {
        PresetMiscUtils.print(args);
    }

    private final DockingListener dockingListener;
    private final CreateSettlementInteraction wrapped;

    // just in case
    private InteractionDialogPlugin previousPlugin; 
    private SiteData selectedSite = null;
    private float cost = 350000f * LunaSettings.getFloat("assortment_of_things", "rat_frontiersCostMult");

    public WrappedCreateSettlementInteraction(CreateSettlementInteraction wrapped, DockingListener dockingListener) {
        this.wrapped = wrapped;
        this.dockingListener = dockingListener;
    }

    @Override
    public void init() {
        wrapped.init();
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        wrapped.optionSelected(optionText, optionData);
        if (optionText.equals("Descend towards the settlement")) {
            dockingListener.reportPlayerClosedMarket(((SettlementInteraction)Global.getSector().getCampaignUI().getCurrentInteractionDialog().getPlugin()).getData().getPrimaryPlanet().getMarket());
            dockingListener.reportPlayerOpenedMarket(Global.getSector().getCampaignUI().getCurrentInteractionDialog().getInteractionTarget().getMarket());

            WrappedSettlementInteraction newNewPlugin_ = new WrappedSettlementInteraction((SettlementInteraction)Global.getSector().getCampaignUI().getCurrentInteractionDialog().getPlugin(), dockingListener);
            ReflectionUtilis.transplant(Global.getSector().getCampaignUI().getCurrentInteractionDialog().getPlugin(), newNewPlugin_);

            Global.getSector().getCampaignUI().getCurrentInteractionDialog().setPlugin(newNewPlugin_);
        }
    }
    
    public void populateOptions() {
        wrapped.populateOptions();
    }
}
