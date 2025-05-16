// Thanks to Banana for this class
package data.scripts.ui;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import java.util.List;

public class BaseSelfRefreshingPanel implements CustomUIPanelPlugin {
    @Override
    public void positionChanged(PositionAPI position) {
    }
    public void setRoot(CustomPanelAPI panel) {
        this.root = panel;
        rebuild();
    }
    public CustomPanelAPI root;
    public CustomPanelAPI panel;
    private boolean rebuild = false;

    public void rebuild() {
        rebuild = true;
    }

    @Override
    public void renderBelow(float alphaMult) {

    }

    @Override
    public void render(float alphaMult) {
    }

    public void buildTooltip(CustomPanelAPI panel) {

    }

    @Override
    public final void advance(float amount) {

        if (root == null) return;
        if (rebuild) {
            try {
                root.removeComponent(panel);
                panel = null;
            } catch (Exception ignore) {
            }
            rebuild = false;
            panel = root.createCustomPanel(1, 1, new BaseCustomUIPanelPlugin() {
                @Override
                public void advance(float amount) {
                    advancePostCreation(amount);
                }
                @Override
                public void processInput(List<InputEventAPI> events) {
                    root.getPlugin().processInput(events);
                }
                @Override
                public void buttonPressed(Object buttonId) {
                    root.getPlugin().buttonPressed(buttonId);
                    rebuild();
                }
            });
            root.addComponent(panel);
            buildTooltip(panel);
        }

    }

    public void advancePostCreation(float amount) {
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
    }

    @Override
    public void buttonPressed(Object buttonId) {
    }
}