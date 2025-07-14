// Code taken and modified from officer extension mod

package data.scripts.ui;

import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import data.scripts.ClassRefs;
import data.scripts.util.ReflectionUtilis;

public class UIComponent implements Renderable {
    protected final Object inner;

    /** [o] should be a renderable UI element, in particular it should implement UIComponentAPI
     *  and have the setOpacity method. */
    public UIComponent(Object o) {
        inner = o;
    }

    public void setOpacity(float amount) {
        ReflectionUtilis.invokeMethodDirectly(ClassRefs.renderableSetOpacityMethod, inner, amount);
    }

    public PositionAPI getPosition() {
        return ((UIComponentAPI) inner).getPosition();
    }

    @Override
    public Object getInstance() {
        return inner;
    }
}