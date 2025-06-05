// credit for this goes to the author of the code in the officer extension mod

package data.scripts.ui;

import com.fs.starfarer.api.ui.PositionAPI;

import data.scripts.util.ReflectionUtilis;

public class Position {

    private final PositionAPI inner;
    private static Object setMethod;

    public Position(PositionAPI o) {
        inner = o;
        if (setMethod == null) {
            try {
                setMethod =  ReflectionUtilis.getMethod("set", inner, 1);
            }
            catch (Exception e) {
                throw new RuntimeException("PositionAPI's set method not found");
            }
        }
    }

    public void set(PositionAPI copy) {
        try {
            ReflectionUtilis.invokeMethodDirectly(setMethod, inner, copy);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public PositionAPI getInstance() {
        return inner;
    }
}