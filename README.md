# Vortexia Core

[![Build Development](https://github.com/Vortexia/vortexia-core/actions/workflows/build-development.yml/badge.svg)](https://github.com/Vortexia/vortexia-core/actions/workflows/build-development.yml)
[![Build Beta](https://github.com/Vortexia/vortexia-core/actions/workflows/build-beta.yml/badge.svg)](https://github.com/Vortexia/vortexia-core/actions/workflows/build-beta.yml)
[![Build Release](https://github.com/Vortexia/vortexia-core/actions/workflows/build-master.yml/badge.svg)](https://github.com/Vortexia/vortexia-core/actions/workflows/build-master.yml)
[![Modrinth](https://img.shields.io/modrinth/v/VxHFxXAM?label=Modrinth&color=00AD5C)](https://modrinth.com/plugin/vortexia-core)
[![Hangar](https://img.shields.io/hangar/v/vortexia-core?label=Hangar&color=005A9C)](https://hangar.papermc.io/Vortexia/vortexia-core)
[![Java Version](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/technologies/downloads/)
[![License](https://img.shields.io/github/license/Vortexia/vortexia-core)](LICENSE)

> [!WARNING]
> Experimental build under active development. APIs and behaviors may break between builds.

Core module of the **Vortexia** Minecraft plugin ecosystem (1.21+). Handles storage, player identity, two-factor PIN authentication, auth hooks, wireless networks, multiblock grids, HUD/WAILA overlays, and PlaceholderAPI expansion.

## Features

- **Storage & Caching**: SQLite and MySQL backends with Caffeine caching.
- **Identity & PIN 2FA**: Tracks citizen cards and binds player data to Citizen IDs (CID). Blocks unauthorized access during name/UUID changes until verified by PIN.
- **AuthHook Integration**: Delays PIN prompts until authentication plugins (such as AuthMe) finish the base login sequence.
- **Multiblock Grid Engine**: Tick-driven power and machine networking.
- **WAILA & HUD**: Real-time look-at HUD with Paper and Folia support.
- **In-Game Guide & Recipe Viewer**: In-game interfaces for viewing items and recipes.
- **Item Owner Tagging**: Binds crafter UUID and username to crafted gear; preserves metadata through anvil repair, smithing upgrades, and grindstone disenchanting.
- **Inventory Sync**: CID-based inventory and Ender Chest snapshots for cross-server synchronization and backup recovery.
- **PlaceholderAPI**: Exposes citizen profile, PIN state, issue dates, and arbitrary database metadata to other plugins.
- **Platform-Aware Update Checker**: Background release checker filtered by server platform (Paper, Velocity, Sponge).

## Requirements

- **Java**: 21
- **Platforms**:
  - Paper, Purpur, Folia, Spigot (1.21+)
  - Velocity, BungeeCord, Waterfall
  - Sponge v10+

## Build

```bash
./gradlew clean shadowJar
```

Artifacts are written to `build/libs/`.

## PlaceholderAPI

Identifier: `%vortexia_<placeholder>%`

| Placeholder | Description | Example Output |
|:---|:---|:---|
| `%vortexia_cccd%` | Citizen identification code | `VX-2026-081` |
| `%vortexia_pin_status%` | PIN verification state | `VERIFIED` / `LOCKED` |
| `%vortexia_created_date%` | Citizen profile creation date | `12/10/2026` |
| `%vortexia_meta_<key>%` | Value from the `vortexia_metadata` table | `VIP_TIER_2` |

## Commands & Permissions

| Command | Permission | Description |
|:---|:---|:---|
| `/pin setup <pin>` | none (default: all) | Sets account PIN. |
| `/pin verify <pin>` | none (default: all) | Unlocks account when PIN verification is required. |
| `/cccd` | `vortexia.command.cccd` | Opens citizen profile GUI. Aliases: `/profile`, `/id`. |
| `/cccd view <player>` | `vortexia.command.cccd.view` | Opens another player's profile GUI. |
| `/cccd get` | `vortexia.command.cccd.get` | Withdraws a physical Citizen ID card item. Alias: `/cccd withdraw`. |
| `/vortexia guide` | none (`vortexia.creative.give` to take items) | Opens the guide book GUI. Aliases: `/vortexia creative`, `/vortexia guidebook`. |
| `/vortexia reload` | `vortexia.admin` | Reloads configuration files. |
| `/vortexia update` | `vortexia.admin` | Checks for updates immediately. |
| `/vortexia restore <cccd> <timestamp>` | `vortexia.admin` | Restores an inventory snapshot from database records. |

## AuthHook System

Implement `IAuthHook` to integrate external auth plugins:

```java
public class CustomAuthHook implements IAuthHook, Listener {
    @Override
    public boolean isInstalled() { return true; }

    @Override
    public boolean isAuthenticated(Player player) {
        return true;
    }
}
```

Register during plugin initialization:

```java
authHookManager.registerHook(new CustomAuthHook(this));
```

## License

GNU General Public License v3.0. See [LICENSE](LICENSE).
