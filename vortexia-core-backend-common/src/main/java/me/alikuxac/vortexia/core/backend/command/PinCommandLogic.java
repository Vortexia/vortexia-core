// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.backend.command;

import me.alikuxac.vortexia.core.backend.model.CommonIdentityModel;
import me.alikuxac.vortexia.core.backend.service.BaseSecurityService;

import java.util.UUID;

public class PinCommandLogic {

    public enum PinResult {
        SUCCESS,
        INVALID_FORMAT,
        ALREADY_SET,
        WRONG_PIN,
        IDENTITY_NOT_FOUND
    }

    private final BaseSecurityService securityService;

    public PinCommandLogic(BaseSecurityService securityService) {
        this.securityService = securityService;
    }

    public PinResult processSetup(CommonIdentityModel identity, String newPin) {
        if (newPin == null || !newPin.matches("\\d{4}")) {
            return PinResult.INVALID_FORMAT;
        }

        if (identity.getPin() != null && !identity.getPin().isEmpty()) {
            return PinResult.ALREADY_SET;
        }

        identity.setPin(newPin);
        securityService.authenticate(identity.getUuid());
        return PinResult.SUCCESS;
    }

    public PinResult processVerify(CommonIdentityModel identity, String inputPin) {
        if (identity == null || identity.getPin() == null || identity.getPin().isEmpty()) {
            return PinResult.IDENTITY_NOT_FOUND;
        }

        if (identity.getPin().equals(inputPin)) {
            securityService.authenticate(identity.getUuid());
            return PinResult.SUCCESS;
        }

        return PinResult.WRONG_PIN;
    }
}
