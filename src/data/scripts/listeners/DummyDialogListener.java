package data.scripts.listeners;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.ui.CustomPanelAPI;

import data.scripts.listeners.FleetPresetManagementListener.TablePlugin;

public class DummyDialogListener extends DialogDismissedListener {
    public static final Logger logger = Logger.getLogger(DummyDialogListener.class);

    public DummyDialogListener() {
    }

    @Override
    public void trigger(Object... args) {
    }
}