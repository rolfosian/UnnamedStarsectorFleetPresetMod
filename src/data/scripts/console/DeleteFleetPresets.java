package data.scripts.console;

import data.scripts.util.PresetUtils;
import data.scripts.util.PresetUtils.FleetPreset;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

import com.fs.starfarer.api.Global;

import java.util.*;

public class DeleteFleetPresets implements BaseCommand {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {

        List<String> deletedNames = new ArrayList<>();
        for (FleetPreset preset : PresetUtils.getFleetPresets().values()) {
            deletedNames.add(preset.getName());
        }

        Map<String, Object> persistentData = Global.getSector().getPersistentData();
        persistentData.put(PresetUtils.PRESETS_MEMORY_KEY, new HashMap<>());
        Global.getSector().getMemoryWithoutUpdate().unset(PresetUtils.UNDOCKED_PRESET_KEY);

        for (String name : deletedNames) Console.showMessage("Deleted Fleet Preset: " + name);
        return CommandResult.SUCCESS;
    }
}
