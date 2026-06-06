package data.scripts.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.loading.WeaponGroupSpec;
import com.fs.starfarer.api.loading.WeaponSpecAPI;

import java.util.*;
import org.apache.log4j.Logger;

import static data.scripts.util.PresetUtils.*;

public class CargoPresetUtils {
    public static SubmarketAPI getStorageSubmarket(MarketAPI market) {
        SubmarketAPI storage = market.getSubmarket(Submarkets.SUBMARKET_STORAGE);
        if (storage != null) return storage;
        
        return market.getSubmarket("rat_settlement_storage");
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

        Set<String> sMods = new HashSet<>(variant.getSMods());
        sMods.addAll(variant.getSModdedBuiltIns());

        variant.clear();

        for(String mod : variant.getHullSpec().getBuiltInMods()) {
            variant.addMod(mod);
        }

        for(String mod : variant.getPermaMods()) {
            variant.addMod(mod);
        }

        for (String mod : sMods) {
            variant.addMod(mod);
        }
    }

    public static void stripAllToStorage(List<FleetMemberAPI> fleetMembers, CargoAPI storageCargo) {
        for (FleetMemberAPI member : fleetMembers) {
            stripToStorage(member.getVariant(), storageCargo);
        }
    }

    public static Map<String, Integer> getNeededWeapons(ShipVariantAPI variant) {
        Map<String, Integer> neededWeapons = new HashMap<>();

        for (String slotId : variant.getFittedWeaponSlots()) {
            String wepId = variant.getWeaponSpec(slotId).getWeaponId();
            neededWeapons.put(wepId, neededWeapons.getOrDefault(wepId, 0) + 1);
        }
        return neededWeapons;
    }

    public static Map<String, Integer> getNeededFighters(ShipVariantAPI variant) {
        Map<String, Integer> neededFighters = new HashMap<>();

        for (String wingId : variant.getFittedWings()) {
            neededFighters.put(wingId, neededFighters.getOrDefault(wingId, 0) + 1);
        }
        return neededFighters;
    }

    public static boolean isVariantAvailableInStorage(
        ShipVariantAPI targetVariant,
        List<FleetMemberAPI> mothballedShipsInStorage,
        CargoAPI storageCargo
    ) {
        boolean weaponsMatch = false;
        boolean fightersMatch = false;
        boolean hullMatch = false;

        String hullId = targetVariant.getHullSpec().getBaseHullId();
        for (FleetMemberAPI member : mothballedShipsInStorage) {
            if (member.getHullSpec().getBaseHullId().equals(hullId)) {
                hullMatch = true;
                break;
            }
        }
        if (!hullMatch) return false;

        Map<String, Integer> neededWeapons = getNeededFighters(targetVariant);
        Map<String, Integer> neededFighters = getNeededFighters(targetVariant);

        if (neededWeapons.size() == 0 && neededFighters.size() == 0) return true;

        for (String weaponId : neededWeapons.keySet()) {
            if (storageCargo.getNumWeapons(weaponId) < neededWeapons.get(weaponId)) {
                weaponsMatch = false;
                break;
            }
        }

        for (String wing : neededFighters.keySet()) {
            if (storageCargo.getNumFighters(wing) < neededFighters.get(wing)) {
                fightersMatch = false;
                break;
            }
        }
        
        return weaponsMatch && fightersMatch;
    }

    // manual implementation for refit because FleetMemberAPI.setvariant materializes weapons and fighters out of thin air and simply overwrites and i couldnt find any other avenue, someone please let me know if there's a better way
    // PROBLEM: STAT COSTS FOR ORDNANCE POINTS??? - Only a problem if we implement imports/exports of FleetPresets to and from different saves
    // TODO: make D/Smod agnostic settings and implement conditional logic for options
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
        public float marinesRatio;
        public float fuelRatio;
        public float supplyRatio;

        public CargoResourceRatios(List<FleetMemberAPI> fleetMembers, CargoAPI playerCargo) {
            float totalCrew = playerCargo.getCrew();
            float totalMarines = playerCargo.getMarines();
            float maxPersonnel = playerCargo.getMaxPersonnel();
            float supplies = playerCargo.getSupplies();
            float cargoCapacity = playerCargo.getMaxCapacity();
            float fuel = playerCargo.getFuel();
            float maxFuel = playerCargo.getMaxFuel();

            this.rawCrewRatio = totalCrew / maxPersonnel;
            this.marinesRatio = totalMarines / maxPersonnel;
            this.fuelRatio = fuel / maxFuel;
            this.supplyRatio = supplies / cargoCapacity;
        }
    }

    // TODO needs debugging, more work and conditional logic with params for options
    public static void equalizeCargo(List<FleetMemberAPI> fleetMembers, CargoAPI storageCargo, CargoAPI playerCargo, CargoResourceRatios previousCargoRatios) {
        float maxPersonnel = playerCargo.getMaxPersonnel();
        float playerCargoCapacity = playerCargo.getMaxCapacity();
        float maxFuel = playerCargo.getMaxFuel();
        
        storageCargo.addFuel(playerCargo.getFuel());
        storageCargo.addSupplies(playerCargo.getSupplies());
        storageCargo.addCrew(playerCargo.getCrew());
        storageCargo.addMarines(playerCargo.getMarines());

        playerCargo.removeFuel(playerCargo.getFuel());
        playerCargo.removeSupplies(playerCargo.getSupplies());
        playerCargo.removeCrew(playerCargo.getCrew());
        playerCargo.removeMarines(playerCargo.getMarines());

        float storageFuel = storageCargo.getFuel();
        float storageSupplies = storageCargo.getSupplies();
        float storageCrew = storageCargo.getCrew();
        float storageMarines = storageCargo.getMarines();

        float totalNeededCrew = 0f;
        for (FleetMemberAPI member : fleetMembers) {
            totalNeededCrew += member.getNeededCrew();
        }

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

        // minimum is needed crew + 10%
        float desiredCrew = Math.max(maxPersonnel * previousCargoRatios.rawCrewRatio, totalNeededCrew * 1.1f);
        float actualCrew = Math.min(desiredCrew, storageCrew);

        playerCargo.addCrew((int)actualCrew);
        storageCargo.removeCrew((int)actualCrew);

        float desiredMarines = maxPersonnel * previousCargoRatios.marinesRatio;
        float actualMarines = Math.min(desiredMarines, storageMarines);
        playerCargo.addMarines((int)actualMarines);
        storageCargo.removeMarines((int)actualMarines);
    }

    // needs more work and conditional logic with params for options
    public static void MaxFuelSuppliesAndCrew(CargoAPI playerCargo, CargoAPI storageCargo) {
        storageCargo.addFuel(playerCargo.getFuel());
        storageCargo.addSupplies(playerCargo.getSupplies());
        storageCargo.addCrew(playerCargo.getCrew());
        playerCargo.removeFuel(playerCargo.getFuel());
        playerCargo.removeSupplies(playerCargo.getSupplies());
        playerCargo.removeCrew(playerCargo.getCrew());

        float maxFuel = playerCargo.getMaxFuel();
        float maxSupplies = playerCargo.getMaxCapacity();
        float maxCrew = playerCargo.getMaxPersonnel();

        float storageFuel = storageCargo.getFuel();
        float storageSupplies = storageCargo.getSupplies();
        float storageCrew = storageCargo.getCrew();

        float actualFuel = Math.min(maxFuel, storageFuel);
        float actualSupplies = Math.min(maxSupplies, storageSupplies);
        float actualCrew = Math.min(maxCrew, storageCrew);

        playerCargo.addFuel(actualFuel);
        playerCargo.addSupplies(actualSupplies);
        playerCargo.addCrew((int)actualCrew);

        storageCargo.removeFuel(actualFuel);
        storageCargo.removeSupplies(actualSupplies);
        storageCargo.removeCrew((int)actualCrew);
    }
    
    // this wont work for any valid usecase at the moment because the refit method will currently overwrite all the built-in/d/s hullmods and that would be stupid
    // needs conditional logic for hullmod agnosticism
    public static void refitAllHullsOfIdInFleetWithVariant(String hullId, ShipVariantAPI variant, List<FleetMemberAPI> fleetMembers, CargoAPI playerCargo, CargoAPI storageCargo) {
        for (FleetMemberAPI member : fleetMembers) {
            if (member.getHullId().equals(hullId)) {
                refit(member, variant, playerCargo, storageCargo);
            }
        }
    }
}
