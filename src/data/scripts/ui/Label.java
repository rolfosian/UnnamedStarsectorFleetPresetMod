// credit for this goes to the author of the code in the officer extension mod

package data.scripts.ui;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;

import data.scripts.util.ReflectionUtilis;

public class Label implements Renderable {

    private final LabelAPI inner;
    private static Object createMethod;
    private static Object createSmallInsigniaMethod;
    private static Object setHighlightOnMouseoverMethod;
    private static Object tooltipField;
    private static Object setTooltipMethod;
    private static Object autoSizeMethod;

    public Label(LabelAPI o) {
        inner = o;
        if (createMethod == null) {
            try {
                createMethod = ReflectionUtilis.getMethodExplicit("create", inner, new Class<?>[]{String.class});// inner.getClass().getMethod("create", String.class);
            }
            catch (Exception e) {
                throw new RuntimeException("LabelAPI's create Object not found");
            }
        }
        if (createSmallInsigniaMethod == null) {
            try {
                createSmallInsigniaMethod = ReflectionUtilis.getMethodExplicit("createSmallInsigniaLabel", inner, new Class<?>[]{String.class, Alignment.class});// inner.getClass().getMethod("createSmallInsigniaLabel", String.class, Alignment.class);
            }
            catch (Exception e) {
                throw new RuntimeException("LabelAPI's createSmallInsigniaLabel Object not found");
            }
        }
        if (setHighlightOnMouseoverMethod == null) {
            try {
                setHighlightOnMouseoverMethod = inner.getClass().getMethod("setHighlightOnMouseover", boolean.class);
            }
            catch (Exception e) {
                throw new RuntimeException("LabelAPI's setHighlightOnMouseover Object not found");
            }
        }
        if (tooltipField == null) {
            try {
                for (Object field : inner.getClass().getDeclaredFields()) {
                    // Look for an interface with the "notifyShown" method
                    Class<?> fieldType = ReflectionUtilis.getFieldType(field);
                    if (fieldType.isInterface()) {
                        try {
                            fieldType.getMethod("notifyShown");
                            // Past here, we know we found the Object we're looking for
                            tooltipField = field;
                        }
                        catch (NoSuchMethodException e) {
                            // continue
                        }
                    }
                }
                if (tooltipField == null) {
                    throw new RuntimeException("LabelAPI's tooltip Object not found");
                }
            }
            catch (Exception e) {
                throw new RuntimeException("LabelAPI's tooltip Object not found");
            }
        }
        if (setTooltipMethod == null) {
            try {
                setTooltipMethod = ReflectionUtilis.getMethodExplicit("setTooltip", inner, new Class<?>[] {float.class, ReflectionUtilis.getFieldType(tooltipField)});
            }
            catch (Exception e) {
                throw new RuntimeException("LabelAPI's setTooltip Object not found");
            }
        }
        if (autoSizeMethod == null) {
            try {
                autoSizeMethod = ReflectionUtilis.getMethod("autoSize", inner, 0);
            }
            catch (Exception e) {
                throw new RuntimeException("LabelAPI's autoSize Object not found");
            }
        }
    }

    public Label create(String text) {
        try {
            return new Label((LabelAPI) ReflectionUtilis.invokeMethodDirectly(createMethod, inner, new Class<?>[]{String.class}, text));
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Label createSmallInsigniaLabel(String text, Alignment alignment) {
        try {
            return new Label((LabelAPI) ReflectionUtilis.invokeMethodDirectly(createSmallInsigniaMethod, inner, text, alignment));
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void autoSize() {
        try {
            ReflectionUtilis.invokeMethodDirectly(autoSizeMethod, inner);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeTooltip() {
        try {
            ReflectionUtilis.invokeMethodDirectly(setTooltipMethod, inner, 0f, null);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setHighlightOnMouseover(boolean value) {
        try {
            ReflectionUtilis.invokeMethodDirectly(setHighlightOnMouseoverMethod, inner, value);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public LabelAPI getInstance() {
        return inner;
    }
}