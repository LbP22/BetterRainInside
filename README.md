# Better Rain Inside

NeoForge client-side mod for Minecraft 1.21.1 that makes rain much quieter indoors while keeping smooth transitions near doors, windows, overhangs, trees, and cave entrances.

## Features

- Strong rain dampening in enclosed spaces (houses, caves, underground rooms).
- Differentiation between real enclosure and light canopy cover.
- Smooth volume transitions to avoid abrupt pops while moving.
- Configurable detection and volume response through JSON config.

## Versions

- Minecraft: 1.21.1
- NeoForge: 21.1.230
- Java: 21

## Build

```powershell
.\gradlew.bat clean build
```

Output jar:

- build/libs/betterraininside-1.0.0.jar

## Install

1. Install NeoForge for Minecraft 1.21.1.
2. Place the built jar in your Minecraft mods folder.
3. Start the game.

## Config

The config file is created automatically in your Minecraft config directory:

- config/betterraininside.json

Main options:

- enabled: turn the mod on or off.
- minimumVolume: absolute floor for rain volume multiplier.
- maximumIndoorVolume: highest rain multiplier when clearly indoors.
- sampleRadius: roof sampling radius around player.
- wallProbeDistance: lateral wall probe distance.
- roofCoverageWeight: influence of roof coverage in enclosure score.
- wallCoverageWeight: influence of wall blockage in enclosure score.
- enclosureCurve: non-linear shaping for enclosure classification.
- detectionSensitivity: global multiplier for indoor detection aggressiveness.
- roofedSpaceBoost: extra boost for roofed partial enclosures.
- treeCanopyRelief: reduces indoor score for leaf-canopy-only shelter.
- indoorResponseCurve: controls how quickly rain gets quieter as spaces close.
- updateIntervalTicks: how often environment analysis is recalculated.
- transitionSpeed: smoothing speed for volume transitions.

## Suggested Aggressive Indoor Preset

Use this if indoor difference still feels too subtle:

- maximumIndoorVolume: 0.06
- detectionSensitivity: 2.0
- roofedSpaceBoost: 0.25
- indoorResponseCurve: 2.3
- enclosureCurve: 2.0

## Notes

- This is a client-side audio behavior mod.
- No server installation is required for single-player behavior.
