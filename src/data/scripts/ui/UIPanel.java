// Code taken and modified from officer extension mod

package data.scripts.ui;

import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import data.scripts.ClassRefs;
import data.scripts.util.ReflectionUtilis;

import java.util.List;

@SuppressWarnings("unused")
public class UIPanel extends UIComponent implements Renderable {
    public UIPanel(Object o) {
        super(o);
    }

    public Position add(Renderable elem) {
        return new Position((PositionAPI) ReflectionUtilis.invokeMethodDirectly(ClassRefs.uiPanelAddMethod, inner, elem.getInstance()));
    }

    public void remove(Renderable elem) {
        try {
            ReflectionUtilis.invokeMethodDirectly(ClassRefs.uiPanelRemoveMethod, inner, elem.getInstance());
        } catch (Exception ignored) {
            // it doesnt like it when this is used on scrollers
            return;
        }
    }

    public List<?> getChildrenNonCopy() {
        return (List<?>) ReflectionUtilis.invokeMethodDirectly(ClassRefs.uiPanelgetChildrenNonCopyMethod, inner);
    }

    @Override
    public UIPanelAPI getInstance() {
        return (UIPanelAPI) inner;
    }
}