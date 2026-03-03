# Vortexia Core

The core module of the **Vortexia** Minecraft Plugin ecosystem (version 1.21+). This module is responsible for Storage, Identity handling, and Security (including 2FA PIN protection & Auth Hook systems).

## Features

- **Storage & Caching**: Manages data operations across various platforms such as MySQL and SQLite, along with highly optimized caching powered by Caffeine.
- **Identity Security (2FA PIN)**: The `IdentityMigrationHelper` mechanism prevents Account Hijacking when a player changes their name (changing UUID) by requiring a PIN verification before syncing changes to the database.
- **AuthHook System (Delayed Execution)**: Resolves conflicts between Vortexia's PIN Security and external authentication plugins (e.g., AuthMe). Vortexia pauses its operations to let the Auth Plugin handle the baseline login before requiring the 2nd-layer PIN.
- **CommandAPI & NBT**: Integrates deeply with specialized libraries for Advanced Command Parsing and System Item Handling.

## Quick Start

### Requirements
- **Java**: 21
- **Minecraft**: 1.21+ (Paper / Purpur)

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

## Commands
| Command | Permission | Description |
|----------|-------------|---------|
| `/pin setup <digits>` | `none` | Sets up a new security PIN (applicable when your Account doesn't have a PIN configured yet). |
| `/pin verify <digits>`| `none` | Unlocks the account when caught in Ghost Authentication (2FA). |
| `/vortexia reload` | `vortexia.admin` | Reloads the plugin configuration. |

---

> *Copyright belongs to Team Vortexia | Core developed by: @alikuxac.*
