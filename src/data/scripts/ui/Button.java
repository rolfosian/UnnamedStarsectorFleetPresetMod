// Code taken and modified from officer extension mod

package data.scripts.ui;

import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import data.scripts.ClassRefs;
import data.scripts.util.UtilReflection;
import data.scripts.util.ReflectionUtilis;
import data.scripts.util.ReflectionUtilis.ListenerFactory.ActionListener;

@SuppressWarnings("unused")
public class Button extends UIComponent implements Renderable {

    private TooltipMakerAPI tooltip;
    private CustomPanelAPI panel;

    /** [o] should be an instance of the underlying Button object */
    public Button(ButtonAPI o, TooltipMakerAPI tooltip, CustomPanelAPI panel) {
        super(o);
        this.tooltip = tooltip;
        this.panel = panel;
    }

    public TooltipMakerAPI getTooltip() {
        return tooltip;
    }

    public CustomPanelAPI getPanel() {
        return panel;
    }

    @Override
    public ButtonAPI getInstance() {
        return (ButtonAPI) inner;
    }

    public void setEnabled(boolean enabled) {
        ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonSetEnabledMethod, inner, enabled);
    }

    public boolean isEnabled() {
        return getInstance().isEnabled();
    }

    public void setListener(ActionListener listener) {
        ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonSetListenerMethod, inner, listener.getProxy());
    }

    public void setShortcut(int key, boolean idkWhatThisDoes) {
        ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonSetShortcutMethod, inner, key, idkWhatThisDoes);
    }

    public Object getListener() {
        return ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonGetListenerMethod, inner);
    }

    public void setButtonPressedSound(String soundId) {
        ReflectionUtilis.invokeMethodDirectly(ClassRefs.buttonSetButtonPressedSoundMethod, inner, soundId);
    }

    private static String graphicsObjectGetterName;
    private static String textGetterName;

    public String getText() {
        return getInstance().getText();
    }
}