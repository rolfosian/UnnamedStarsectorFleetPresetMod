package data.scripts.console;

import data.scripts.util.PresetUtils;
import data.scripts.util.PresetUtils.FleetPreset;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

import java.util.ArrayList;
import java.util.List;

public class DeleteFleetPresets implements BaseCommand {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        List<FleetPreset> presetsToDelete = new ArrayList<>(PresetUtils.getFleetPresets().values());
        
        for (FleetPreset preset : presetsToDelete) {
            String name = preset.getName();
            PresetUtils.deleteFleetPreset(name);
            Console.showMessage("Deleted fleet preset: " + name);
        }
        return CommandResult.SUCCESS;
    }
}
