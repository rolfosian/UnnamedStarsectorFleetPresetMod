package data.scripts.ui;

import com.fs.starfarer.api.ui.LabelAPI;

import data.scripts.util.ReflectionUtilis;

public class Label implements Renderable {
    private final LabelAPI inner;

    public Label(LabelAPI o) {
        inner = o;
    }

    public LabelAPI getInstance() {
        return inner;
    }
}