package data.scripts.ui.grid;

import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import data.scripts.ui.UIComponent;

public class Canvas extends UIComponent  {
    private UIPanelAPI panel;
    private TooltipMakerAPI tooltipMaker;

    public Canvas(UIPanelAPI panel, TooltipMakerAPI tooltipMaker) {
        super(panel);
        this.panel = panel;
        this.tooltipMaker = tooltipMaker;
    }

    public void addComponent(UIComponentAPI component) {
        panel.addComponent(component);
    }

    public void removeComponent(UIComponentAPI component) {
        panel.removeComponent(component);
    }
    
}