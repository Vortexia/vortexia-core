// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.backend.command;

import me.alikuxac.vortexia.core.backend.model.CommonIdentityModel;

import java.util.concurrent.ThreadLocalRandom;

public class CccdLogic {

    public enum CCIDResult {
        SUCCESS,
        IDENTITY_NOT_FOUND,
        NO_CITIZEN_ID,
        INVENTORY_FULL
    }

    /**
     * Create CCID with format Vx-XXXX-XXXX
     */
    public static String generateCitizenId() {
        int part1 = ThreadLocalRandom.current().nextInt(1000, 10000);
        int part2 = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "VX-" + part1 + "-" + part2;
    }

    public CCIDResult validateCardWithdrawal(CommonIdentityModel identity, boolean isInventoryFull) {
        if (identity == null) {
            return CCIDResult.IDENTITY_NOT_FOUND;
        }

        if (identity.getCitizenId() == null || identity.getCitizenId().isEmpty()) {
            return CCIDResult.NO_CITIZEN_ID;
        }

        if (isInventoryFull) {
            return CCIDResult.INVENTORY_FULL;
        }

        return CCIDResult.SUCCESS;
    }
}
