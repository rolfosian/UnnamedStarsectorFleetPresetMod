package data.scripts.interactions;

import java.util.Map;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.events.CampaignEventPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;

public class WrappedRuleBasedInteractionDialogPluginImpl extends RuleBasedInteractionDialogPluginImpl {
    private RuleBasedInteractionDialogPluginImpl oldPlugin;

    public WrappedRuleBasedInteractionDialogPluginImpl(RuleBasedInteractionDialogPluginImpl oldPlugin) {
        super();
        this.oldPlugin = oldPlugin;
    }

    @Override
    public void advance(float arg0) {
        oldPlugin.advance(arg0);
    }

    @Override
    public void backFromEngagement(EngagementResultAPI arg0) {
        oldPlugin.backFromEngagement(arg0);
    }

    @Override
    public Object getContext() {
        return oldPlugin.getContext();
    }

    @Override
    public Map<String, MemoryAPI> getMemoryMap() {
        return oldPlugin.getMemoryMap();
    }

    @Override
    public void init(InteractionDialogAPI arg0) {
        oldPlugin.init(arg0);
    }

    @Override
    public void optionMousedOver(String arg0, Object arg1) {
        oldPlugin.optionMousedOver(arg0, arg1);
    }

    @Override
    public void optionSelected(String arg0, Object arg1) {
        oldPlugin.optionSelected(arg0, arg1);
    }

    @Override
    public boolean fireAll(String trigger) {
        return oldPlugin.fireAll(trigger);
    }

    @Override
    public boolean fireBest(String trigger) {
        return oldPlugin.fireBest(trigger);
    }

    @Override
    public void notifyActivePersonChanged() {
        oldPlugin.notifyActivePersonChanged();
    }

    @Override
    public Object getCustom1() {
        return oldPlugin.getCustom1();
    }

    @Override
    public Object getCustom2() {
        return oldPlugin.getCustom2();
    }

    @Override
    public Object getCustom3() {
        return oldPlugin.getCustom3();
    }

    @Override
    public void setCustom1(Object custom1) {
        oldPlugin.setCustom1(custom1);
    }

    @Override
    public void setCustom2(Object custom2) {
        oldPlugin.setCustom2(custom2);
    }

    @Override
    public void setCustom3(Object custom3) {
        oldPlugin.setCustom3(custom3);
    }

    @Override
    public void reinit(boolean withContinueOnRuleFound) {
        oldPlugin.reinit(withContinueOnRuleFound);
    }

    @Override
    public void setActiveMission(CampaignEventPlugin mission) {
        oldPlugin.setActiveMission(mission);
    }

    @Override
    public void setEmbeddedMode(boolean embeddedMode) {
        oldPlugin.setEmbeddedMode(embeddedMode);
    }

    @Override
    public void updateMemory() {
        oldPlugin.updateMemory();
    }
}
