// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.backend.model;

import java.util.UUID;

public class CommonIdentityModel {

    private final UUID uuid;
    private final String citizenId;
    private final String playerName;
    private String pin;

    public CommonIdentityModel(UUID uuid, String citizenId, String playerName, String pin) {
        this.uuid = uuid;
        this.citizenId = citizenId;
        this.playerName = playerName;
        this.pin = pin;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getCitizenId() {
        return citizenId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }
}
