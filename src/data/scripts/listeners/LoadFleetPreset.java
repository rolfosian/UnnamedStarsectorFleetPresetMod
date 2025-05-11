// package data.scripts.listeners;

// import com.fs.starfarer.api.Global;

// import data.scripts.util.PresetUtils;

// import org.apache.log4j.Logger;

// public class LoadFleetPreset extends ActionListener {
//     public static final Logger logger = Logger.getLogger(LoadFleetPreset.class);

//     @Override
//     public void trigger(Object... args) {
//         logger.info("Load Fleet Preset Listener triggered");

//         if (args[0] != null) {
//             PresetUtils.restoreFleetFromPreset(args[0].toString());
//         }
//     }
// }