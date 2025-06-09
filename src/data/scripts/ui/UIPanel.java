// Code taken and modified from officer extension mod

package data.scripts.ui;

import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import data.scripts.ClassRefs;
import data.scripts.ui.UIComponent;
import data.scripts.util.ReflectionUtilis;
import data.scripts.ui.Renderable;
import data.scripts.ui.Position;

import java.util.List;

@SuppressWarnings("unused")
public class UIPanel extends UIComponent implements Renderable {

    private static Object addMethod;
    private static Object getChildrenNonCopyMethod;
    private static Object removeMethod;

    public UIPanel(Object o) {
        super(o);
        if (addMethod == null) {
            try {
                addMethod = ReflectionUtilis.getMethodExplicit("add", inner, new Class<?>[]{ClassRefs.renderableUIElementInterface});//inner.getClass().getMethod("add", ClassRefs.renderableUIElementInterface);
            }
            catch (Exception e) {
                throw new RuntimeException("UIPanel's add method not found");
            }
        }
        if (getChildrenNonCopyMethod == null) {
            try {
                getChildrenNonCopyMethod = ReflectionUtilis.getMethod("getChildrenNonCopy", inner, 0);
            }
            catch (Exception e) {
                throw new RuntimeException("UIPanel's getChildrenNonCopy method not found");
            }
        }
        if (removeMethod == null) {
            try {
                removeMethod = ReflectionUtilis.getMethodExplicit("remove", inner, new Class<?>[]{ClassRefs.renderableUIElementInterface});
            }
            catch (Exception e) {
                throw new RuntimeException("UIPanel's remove method not found");
            }
        }
    }

    public Position add(Renderable elem) {
        try {
            return new Position((PositionAPI) ReflectionUtilis.invokeMethodDirectly(addMethod, inner, elem.getInstance()));
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void remove(Renderable elem) {
        try {
            ReflectionUtilis.invokeMethodDirectly(removeMethod, inner, elem.getInstance());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<?> getChildrenNonCopy() {
        try {
            return (List<?>) ReflectionUtilis.invokeMethodDirectly(getChildrenNonCopyMethod, inner);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public UIPanelAPI getInstance() {
        return (UIPanelAPI) inner;
    }
}