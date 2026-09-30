package me.alikuxac.vortexia.core.core.permission;

public enum Permission {

    ADMIN("vortexia.admin", "Full administrative access to VortexiaCore", PermissionCategory.ADMIN),
    COMMAND_PIN("vortexia.command.pin", "Access to the /pin command", PermissionCategory.COMMAND),
    COMMAND_CCCD("vortexia.command.cccd", "Access to the /cccd command", PermissionCategory.COMMAND),
    COMMAND_CCCD_VIEW("vortexia.command.cccd.view", "View other players' profiles", PermissionCategory.COMMAND),
    COMMAND_CCCD_GET("vortexia.command.cccd.get", "Withdraw a Citizen ID card", PermissionCategory.COMMAND),
    CREATIVE_GIVE("vortexia.creative.give", "Take items from the Guide GUI", PermissionCategory.CREATIVE),
    COMMAND_RESTORE("vortexia.command.restore", "Restore player inventory snapshots", PermissionCategory.COMMAND);

    private final String node;
    private final String description;
    private final PermissionCategory category;

    Permission(String node, String description, PermissionCategory category) {
        this.node = node;
        this.description = description;
        this.category = category;
    }

    public String getNode() {
        return node;
    }

    public String getDescription() {
        return description;
    }

    public PermissionCategory getCategory() {
        return category;
    }

    @Override
    public String toString() {
        return node;
    }
}
