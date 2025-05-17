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

import java.lang.reflect.Method;
import java.util.*;
import org.apache.log4j.Logger;

public class CargoPresetUtils {
    private static Logger logger = Logger.getLogger(CargoPresetUtils.class);

    public static void print(Object... args) {
        MiscUtils.print(args);
    }

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

    public static void stripAllToStorage(List<FleetMemberAPI> fleetMembers, CargoAPI storageCargo) {
        for (FleetMemberAPI member : fleetMembers) {
            stripToStorage(member.getVariant(), storageCargo);
        }
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

    public static class CargoResourceRatios {
        public float rawCrewRatio;
        public float crewToMarinesRatio;
        public float fuelRatio;
        public float supplyRatio;
        public float totalNeededCrewToMaxPersonnelRatio;

        public CargoResourceRatios(float rawCrewRatio, float crewToMarinesRatio, float fuelRatio, float supplyRatio, float totalNeededCrewToMaxPersonnelRatio) {
            this.rawCrewRatio = rawCrewRatio;
            this.crewToMarinesRatio = crewToMarinesRatio;
            this.fuelRatio = fuelRatio;
            this.supplyRatio = supplyRatio;
            this.totalNeededCrewToMaxPersonnelRatio = totalNeededCrewToMaxPersonnelRatio;
        }
    }

    public static CargoResourceRatios getCargoResourceRatios(List<FleetMemberAPI> fleetMembers, CargoAPI playerCargo) {
        float totalCrew = playerCargo.getCrew();
        float totalMarines = playerCargo.getMarines();
        float maxPersonnel = playerCargo.getMaxPersonnel();
        float supplies = playerCargo.getSupplies();
        float cargoCapacity = playerCargo.getMaxCapacity();
        float fuel = playerCargo.getFuel();
        float maxFuel = playerCargo.getMaxFuel();

        float totalNeededCrew = 0f;

        for (FleetMemberAPI member : fleetMembers) {
            totalNeededCrew += member.getNeededCrew();
        }

        float totalNeededCrewToMaxPersonnelRatio = totalNeededCrew / maxPersonnel;

        float crewToMarinesRatio = totalMarines / totalCrew;
        float rawCrewRatio = totalCrew / maxPersonnel;
        float fuelRatio = fuel / maxFuel;
        float supplyRatio = supplies / cargoCapacity;

        return new CargoResourceRatios(rawCrewRatio, crewToMarinesRatio, fuelRatio, supplyRatio, totalNeededCrewToMaxPersonnelRatio);
    }

    public static void equalizeCargo(List<FleetMemberAPI> fleetMembers, CargoAPI storageCargo, CargoAPI playerCargo, CargoResourceRatios previousCargoRatios) {
        float maxPersonnel = playerCargo.getMaxPersonnel();
        float playerCargoCapacity = playerCargo.getMaxCapacity();
        float maxFuel = playerCargo.getMaxFuel();
        
        storageCargo.addAll(playerCargo);
        playerCargo.clear();

        float storageFuel = storageCargo.getFuel();
        float storageSupplies = storageCargo.getSupplies();
        float storageCrew = storageCargo.getCrew();

        // minimum fuel is 20% of max fuel
        float desiredFuel = Math.max(maxFuel * previousCargoRatios.fuelRatio, maxFuel * 0.2f);
        float actualFuel = Math.min(desiredFuel, storageFuel);
        Global.getSector().getPlayerFleet().getCargo().addFuel(actualFuel);
        storageCargo.removeFuel(actualFuel);

        // minimum supplies is 20% of cargo capacity
        float desiredSupplies = Math.max(playerCargoCapacity * previousCargoRatios.supplyRatio, playerCargoCapacity * 0.2f);
        float actualSupplies = Math.min(desiredSupplies, storageSupplies);
        playerCargo.addSupplies(actualSupplies);
        storageCargo.removeSupplies(actualSupplies);
        
        float newNeededCrew = 0f;
        for (FleetMemberAPI member : fleetMembers) {
            newNeededCrew += member.getNeededCrew();
        }

        float baseCrew = Math.max(maxPersonnel * previousCargoRatios.rawCrewRatio, newNeededCrew * 1.1f);
        float totalCrew = Math.max(baseCrew, newNeededCrew);
        float totalMarines = 0f;
        
        if (previousCargoRatios.crewToMarinesRatio > 0) {
            totalMarines = totalCrew * previousCargoRatios.crewToMarinesRatio;
            totalCrew -= totalMarines;
        }

        float actualCrew = Math.min(totalCrew, storageCrew);
        playerCargo.addCrew((int)actualCrew);
        storageCargo.removeCrew((int)actualCrew);
        
        if (totalMarines > 0) {
            float storageMarines = storageCargo.getMarines();
            float actualMarines = Math.min(totalMarines, storageMarines);

            playerCargo.addMarines((int)actualMarines);
            storageCargo.removeMarines((int)actualMarines);
        }
    }

    public static void refitAllHullsOfIdInFleetWithVariant(String hullId, ShipVariantAPI variant, List<FleetMemberAPI> fleetMembers, CargoAPI playerCargo, CargoAPI storageCargo) {
        for (FleetMemberAPI member : fleetMembers) {
            if (member.getHullId().equals(hullId)) {
                refit(member, variant, playerCargo, storageCargo);
            }
        }
    }
}
