package data.scripts.console;

import java.util.Map;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

import data.scripts.util.PresetUtils;

import com.fs.starfarer.api.Global;

public class UninstFleetPresets implements BaseCommand {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        Map<String, Object> persistentData = Global.getSector().getPersistentData();
        persistentData.remove(PresetUtils.PRESETS_MEMORY_KEY);
        persistentData.remove(PresetUtils.PRESET_MEMBERS_KEY);
        persistentData.remove(PresetUtils.STORED_PRESET_MEMBERIDS_KEY);
        Global.getSector().getMemoryWithoutUpdate().unset(PresetUtils.UNDOCKED_PRESET_KEY);

        Console.showMessage("Uninstalled Fleet Presets. Remember to save the game!");
        return CommandResult.SUCCESS;
    }
}
