// credit for this goes to the author of the code in the officer extension mod

package data.scripts.ui;

import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import data.scripts.ClassRefs;
import data.scripts.ui.UIComponent;
import data.scripts.ui.Renderable;
import data.scripts.ui.Position;

import java.lang.reflect.Method;
import java.util.List;

@SuppressWarnings("unused")
public class UIPanel extends UIComponent implements Renderable {

    private static Method addMethod;
    private static Method getChildrenNonCopyMethod;
    private static Method removeMethod;

    public UIPanel(Object o) {
        super(o);
        if (addMethod == null) {
            try {
                addMethod = inner.getClass().getMethod("add", ClassRefs.renderableUIElementInterface);
            }
            catch (Exception e) {
                throw new RuntimeException("UIPanel's add method not found");
            }
        }
        if (getChildrenNonCopyMethod == null) {
            try {
                getChildrenNonCopyMethod = inner.getClass().getMethod("getChildrenNonCopy");
            }
            catch (Exception e) {
                throw new RuntimeException("UIPanel's getChildrenNonCopy method not found");
            }
        }
        if (removeMethod == null) {
            try {
                removeMethod = inner.getClass().getMethod("remove", ClassRefs.renderableUIElementInterface);
            }
            catch (Exception e) {
                throw new RuntimeException("UIPanel's remove method not found");
            }
        }
    }

    public Position add(Renderable elem) {
        try {
            return new Position((PositionAPI) addMethod.invoke(inner, elem.getInstance()));
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void remove(Renderable elem) {
        try {
            removeMethod.invoke(inner, elem.getInstance());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<?> getChildrenNonCopy() {
        try {
            return (List<?>) getChildrenNonCopyMethod.invoke(inner);
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