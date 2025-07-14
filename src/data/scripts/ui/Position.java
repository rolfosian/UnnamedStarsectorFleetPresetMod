// Code taken and modified from officer extension mod
package data.scripts.ui;

import com.fs.starfarer.api.ui.PositionAPI;

import data.scripts.ClassRefs;
import data.scripts.util.ReflectionUtilis;

public class Position {
    private final PositionAPI inner;

    public Position(PositionAPI o) {
        inner = o;
    }

    public void set(PositionAPI copy) {
        ReflectionUtilis.invokeMethodDirectly(ClassRefs.positionSetMethod, inner, copy);
    }

    public PositionAPI getInstance() {
        return inner;
    }
}