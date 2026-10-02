# DurabilityAlert

A Fabric mod for Minecraft 1.21.1. **Never break your diamond pickaxe by accident again.**

When the tool in your hand, the item in your offhand, or any armor piece gets low, you hear a ding and see a warning above your hotbar:

> **⚠ Diamond Pickaxe: 18 uses left!**

| When | What happens |
|---|---|
| **Low**: 10% left (or 20 uses, whichever is more) | Orange warning + a soft ding |
| **Critical**: 3% left (or 5 uses) | Red warning + a bell |
| Still swinging a critical tool | Reminder every 5 seconds |

It only warns when an item **gets** worse while you're using it, not every time you switch to an old tool. Creative mode is ignored.

## Commands & config

- `/durability`: turn the alerts on or off.
- `config/durabilityalert.properties`: change the thresholds (`lowPercent`, `lowUses`, `criticalPercent`, `criticalUses`) or turn the sound off.

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.1.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) in your `mods` folder.
3. Download `durabilityalert-x.x.x.jar` from [**Releases**](../../releases) and put it in `mods`.

Client-side only. Works on any server.

## Building

```
./gradlew build
```

The jar ends up in `build/libs/`.

To ship an update, bump `mod_version` in `gradle.properties` and push. GitHub Actions builds it and publishes a release automatically.
