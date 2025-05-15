package data.scripts.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI.SkillLevelAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.WeaponGroupSpec;
import com.fs.starfarer.api.loading.WeaponSlotAPI;

import java.util.*;
import org.apache.log4j.Logger;

public class CargoPresetUtils {
    private static Logger logger = Logger.getLogger(CargoPresetUtils.class);

    public static boolean playerKnowsHullmod(String modId) {
        return Global.getSector().getCharacterData().knowsHullMod(modId);
    }

    public static boolean isWeaponInCargo(CargoAPI cargo, String weaponId) {
        return (cargo.getNumWeapons(weaponId) > 0);
    }

    public static boolean isFighterInCargo(CargoAPI cargo, String wingId) {
        return (cargo.getNumFighters(wingId) > 0);
    }

    // manual implementation for refit because FleetMemberAPI.setvariant materializes weapons and fighters out of thin air and simply overwrites
    public static void stripToStorage(ShipVariantAPI variant, CargoAPI storageCargo) {
        List<String> nonBuiltInWings = variant.getNonBuiltInWings();
        List<String> nonBuiltInWeaponSlots = variant.getNonBuiltInWeaponSlots();

        for (String weaponSlot : nonBuiltInWeaponSlots) {
            String weaponId = variant.getWeaponId(weaponSlot);
            storageCargo.addWeapons(weaponId, 1);
        }
        for (String wing : nonBuiltInWings) {
            storageCargo.addFighters(wing, 1);
        }
        variant.clear();
    }

    // manual implementation for refit because FleetMemberAPI.setvariant materializes weapons and fighters out of thin air and simply overwrites and i couldnt find any other avenue, someone please let me know if there's a better way
    // PROBLEM: STAT COSTS FOR ORDNANCE POINTS??? I DONT KNOW HOW IT IS IMPLEMENTED. MAYBE ITS NOT A PROBLEM IDK
    // TODO: make D/SMOD AGNOSTIC SETTINGS
    public static void refit(FleetMemberAPI targetMember, ShipVariantAPI sourceVariant, CargoAPI playerFleetCargo, CargoAPI storageCargo) {
        ShipVariantAPI targetVariantOriginal = targetMember.getVariant();
        ShipVariantAPI targetVariant = targetVariantOriginal.clone();

        List<String> hullSpecBuiltInMods = sourceVariant.getHullSpec().getBuiltInMods();
        Collection<String> nonBuiltInHullMods = sourceVariant.getNonBuiltInHullmods();
        Set<String> permaMods = sourceVariant.getPermaMods();
        LinkedHashSet<String> sMods = sourceVariant.getSMods();
        LinkedHashSet<String> sModdedBuiltIns = sourceVariant.getSModdedBuiltIns();
        Set<String> suppressedMods = sourceVariant.getSuppressedMods();

        List<String> nonBuiltInWings = sourceVariant.getNonBuiltInWings();
        List<String> nonBuiltInWeaponSlots = sourceVariant.getNonBuiltInWeaponSlots();
        List<WeaponGroupSpec> weaponGroups = sourceVariant.getWeaponGroups();

        int numCapacitors = sourceVariant.getNumFluxCapacitors();
        int numVents = sourceVariant.getNumFluxVents();

        // unfit everything
        stripToStorage(targetVariant, storageCargo);
        targetVariant.getHullMods().clear();

        // refit everything
        targetVariant.setNumFluxCapacitors(numCapacitors);
        targetVariant.setNumFluxVents(numVents);

        for (String mod : hullSpecBuiltInMods) {
            targetVariant.addPermaMod(mod);
        }
        for (String mod : nonBuiltInHullMods) {
            targetVariant.addMod(mod);
        }
        for (String mod : permaMods) {
            targetVariant.addPermaMod(mod, false);
        }
        for (String mod : sMods) {
            targetVariant.addPermaMod(mod, true);
        }
        for (String mod : sModdedBuiltIns) {
            targetVariant.addPermaMod(mod, true);
        }
        for (String mod : suppressedMods) {
            targetVariant.addSuppressedMod(mod);
        }

        int count = 0;
        for (String wing : nonBuiltInWings) {
            if (isFighterInCargo(playerFleetCargo, wing)) {
                playerFleetCargo.removeFighters(wing, 1);
                targetVariant.setWingId(count, wing);
                count++;
            } else if (isFighterInCargo(storageCargo, wing)) {
                storageCargo.removeFighters(wing, 1);
                targetVariant.setWingId(count, wing);
                count++;
            }
        }
        
        for (String weaponSlot : nonBuiltInWeaponSlots) {
            String weaponId = sourceVariant.getWeaponId(weaponSlot);

            if (isWeaponInCargo(playerFleetCargo, weaponId)) {
                playerFleetCargo.removeWeapons(weaponId, 1);
                targetVariant.addWeapon(weaponSlot, weaponId);

            } else if (isWeaponInCargo(storageCargo, weaponId)) {
                storageCargo.removeWeapons(weaponId, 1);
                targetVariant.addWeapon(weaponSlot, weaponId);
            }
        }
        for (WeaponGroupSpec weaponGroup : weaponGroups) {
            targetVariant.addWeaponGroup(weaponGroup);
        }
        targetMember.setVariant(targetVariant, false, false);
    }
    // TODO implementation to keep supply/fuel/crew ratio relative to fleet preset consumption
}
