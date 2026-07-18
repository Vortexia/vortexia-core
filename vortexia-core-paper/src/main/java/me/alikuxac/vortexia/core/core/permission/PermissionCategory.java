package me.alikuxac.vortexia.core.core.permission;

public enum PermissionCategory {

    ADMIN("Admin", "§c[Admin]"),
    COMMAND("Commands", "§b[Commands]"),
    CREATIVE("Creative", "§d[Creative]");

    private final String displayName;
    private final String colorPrefix;

    PermissionCategory(String displayName, String colorPrefix) {
        this.displayName = displayName;
        this.colorPrefix = colorPrefix;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorPrefix() {
        return colorPrefix;
    }
}
