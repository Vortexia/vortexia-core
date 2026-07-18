# Vortexia Core

> [!WARNING]
> This is an **experimental** build of Vortexia Core under active development. Features are subject to change, and APIs or behaviors may break at any time. Use at your own risk in production environments!

The core module of the **Vortexia** Minecraft Plugin ecosystem (version 1.21+). This module is responsible for Storage, Identity handling, Security (including 2FA PIN protection & Auth Hook systems), Wireless Networks, Custom Grids, and on-screen HUD displays.

## Features

- **Storage & Caching**: Manages data operations across various platforms such as MySQL and SQLite, along with highly optimized caching powered by Caffeine.
- **Identity Security (2FA PIN)**: The `IdentityMigrationHelper` mechanism prevents Account Hijacking when a player changes their name (changing UUID) by requiring a PIN verification before syncing changes to the database.
- **AuthHook System (Delayed Execution)**: Resolves conflicts between Vortexia's PIN Security and external authentication plugins (e.g., AuthMe). Vortexia pauses its operations to let the Auth Plugin handle the baseline login before requiring the 2nd-layer PIN.
- **Dynamic Grid & Economy**: A ticking multi-block network manager (`CoreGridManager`) handling energy routing, machinery linkages, and modular connections.
- **WAILA HUD Display**: A real-time, highly-configurable look-at block display (`CoreWailaManager`) featuring support for Folia/Paper and customizable screen position parameters.
- **Interactive Guide Book & GUI**: A user-friendly interface (`GuideGUI`) allowing players to learn plugin systems, view recipes (`RecipeViewerGUI`), and retrieve custom items if authorized.
- **Custom Item & Recipe Registries**: Dynamically registers unique custom items (`CoreItemRegistry`) and advanced recipes (`CoreCustomRecipeManager`).
- **Item Owner Tagging**: Automatically tags the crafter's UUID and username onto tools, weapons, and armors upon crafting. This ownership is safely preserved and transferred when repairing (Anvil), upgrading (Smithing Table), or refining (Grindstone) until the item is completely broken/destroyed.
- **Shadow Inventory Sync**: Safe inventory and Ender Chest sync and recovery based on Citizen ID (CID) to prevent data loss or duplicate items during online-mode flips. Features history snapshot logs.
- **Loader-Aware Update Checker**: Scans for the latest GitHub releases in the background and dynamically filters them based on the current server loader (e.g. Paper, Velocity, Sponge) to ensure version compatibility.
- **CommandAPI & NBT**: Integrates deeply with specialized libraries for Advanced Command Parsing and System Item Handling.

## Quick Start

### Requirements
- **Java**: 21
- **Minecraft Loaders**:
  - **Plugins**: Paper, Purpur, Folia, Spigot, Bukkit (1.21+)
  - **Proxies**: BungeeCord, Velocity, Waterfall
  - **Sponge**: Sponge v10+

### Local Build

The system comes with `build_local.bat` for Windows users. Alternatively, you can run the Gradle Wrapper directly:

```bash
cd vortexia-core
./gradlew clean shadowJar
```

Compiled jars will be placed in the `build/libs` directory.

## Auth Hook System

### Logic Flow (Auth Hook)
Vortexia Core integrates the `IAuthHook` interface to automatically work seamlessly alongside other Auth plugins:
- Instead of locking player actions the exact moment they join, the system delays the operation and shifts control to the Auth Plugin first.
- If **AuthMe is present**: Requires the player to successfully log in before presenting the PIN Verification / PIN Setup prompt.
- If **NO Auth Plugin is present**: Vortexia immediately acts as the sole security layer, prompting the PIN requirement the moment a player joins.

### Adding a New API Hook
Create a class implementing the `IAuthHook` interface under the `hook.impl` package and register it within the `VortexiaCore` main class:

```java
public class MyCustomAuthHook implements IAuthHook, Listener {
    @Override
    public boolean isInstalled() { return true; }

    @Override
    public boolean isAuthenticated(Player player) {
        // Auth plugin logic here
        return true; 
    }
}
```

Then register the hook in the Core's `onEnable()` method:
```java
this.authHookManager.registerHook(new MyCustomAuthHook(this));
```

## Commands & Permissions

| Command | Permission | Description |
|:---|:---|:---|
| `/pin setup <digits>` | `none` (default: true) | Sets up a new security PIN. |
| `/pin verify <digits>`| `none` (default: true) | Unlocks the account when caught in Ghost Authentication (2FA). |
| `/cccd` (aliases: `/profile`, `/id`) | `vortexia.command.cccd` | Opens your Citizen Profile GUI. |
| `/cccd view [target]` | `vortexia.command.cccd.view` | Opens another player's Profile GUI interface. |
| `/cccd get` (aliases: `/cccd withdraw`)| `vortexia.command.cccd.get` | Withdraws your physical Citizen ID card item. |
| `/vortexia reload` | `vortexia.admin` | Reloads the plugin configuration. |
| `/vortexia update` | `vortexia.admin` | Triggers a manual check for updates. |
| `/vortexia restore <cccd> <timestamp>` | `vortexia.admin` | Restores player inventory from a specific snapshot timestamp. |
| `/vortexia guide` (aliases: `/vortexia creative`, `/vortexia guidebook`) | `none` (requires `vortexia.creative.give` to take items) | Opens the dynamic Vortexia Guide Book GUI. |

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.

---

> *Copyright belongs to Team Vortexia | Core developed by: @alikuxac.*
