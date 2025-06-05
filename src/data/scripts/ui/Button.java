// Code taken and modified from officer extension mod

package data.scripts.ui;

import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import data.scripts.ClassRefs;
import data.scripts.util.UtilReflection;
import data.scripts.listeners.ActionListener;
import data.scripts.ui.Renderable;
import data.scripts.ui.UIComponent;
import data.scripts.util.ReflectionUtilis;
import data.scripts.util.PresetMiscUtils;

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
        ReflectionUtilis.getMethodAndInvokeDirectly("setEnabled", inner, 1, enabled);
    }

    public void setListener(ActionListener listener) {
        ReflectionUtilis.getMethodAndInvokeDirectly("setListener", inner, 1, listener.getProxy());
    }

    public void setShortcut(int key, boolean idkWhatThisDoes) {
        ReflectionUtilis.getMethodExplicitAndInvokeDirectly("setShortcut", inner, new Class<?>[]{int.class, boolean.class}, key, idkWhatThisDoes);
    }

    public Object getListener() {
        return ReflectionUtilis.getMethodAndInvokeDirectly("getListener", inner, 0);
    }

    public void setButtonPressedSound(String soundId) {
        ReflectionUtilis.getMethodAndInvokeDirectly("setButtonPressedSound", inner, 1, soundId);
    }

    private static String graphicsObjectGetterName;
    private static String textGetterName;

    public String getText() {
        try {
            Object renderer = ReflectionUtilis.getMethodAndInvokeDirectly("getRenderer", inner, 0);
            Object graphicsObject = null;
            if (graphicsObjectGetterName != null) {
                graphicsObject = ReflectionUtilis.getMethodAndInvokeDirectly(graphicsObjectGetterName, renderer, 0);
            }
            else {
                for (Object method : renderer.getClass().getDeclaredMethods()) {
                    Class<?> returnType = ReflectionUtilis.getReturnType(method);

                    if (returnType != null) {
                        Package pack = returnType.getPackage();
                        if (pack != null && pack.getName().startsWith("com.fs.graphics")) {
                            graphicsObjectGetterName = ReflectionUtilis.getMethodName(method);
                            graphicsObject = ReflectionUtilis.invokeMethodDirectly(method, renderer);
                        }
                    }
                }
            }
            if (graphicsObject == null) {
                throw new RuntimeException("Renderer's graphics object not found");
            }

            if (textGetterName != null) {
                return (String) ReflectionUtilis.getMethodAndInvokeDirectly(textGetterName, graphicsObject, 0);
            }
            for (Object method : graphicsObject.getClass().getDeclaredMethods()) {
                if (String.class.isAssignableFrom(ReflectionUtilis.getReturnType(method))) {
                    textGetterName = ReflectionUtilis.getMethodName(method);
                    return (String) ReflectionUtilis.invokeMethodDirectly(method, graphicsObject);
                }
            }
            throw new RuntimeException("Text label for button object not found");
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            PresetMiscUtils.print(e);
            return null;
        }
    }
}