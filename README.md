# D0cCtor's Claims

D0cCtor's Claims is a Minecraft NeoForge mod for server-side land protection.

## Features

- Claim core system.
- Fuel-based protection using Void Energy.
- Upgrade system using Core Fragments.
- Custom claim core entity and GUI.
- Owner and trusted member management.
- Block placing and breaking protection inside active claims.
- Chests and containers remain accessible.
- PvP remains enabled.
- TNT block damage protection inside active claims.
- Claim core health and regeneration system.
- Player and admin commands.

## Minecraft Version

```txt
Minecraft: 1.21.1
NeoForge: 21.1.121
```

## Mod Info

```txt
Mod ID: d0cctors_claims
Display Name: D0cCtor's Claims
Version: 0.3.5
```

## Main Items

```txt
d0cctors_claims:claim_core
d0cctors_claims:combustible_del_ciclo
d0cctors_claims:fragmento_expansion
```

## Player Commands

```txt
/claim info
/claim list
/claim here
/claim preview
/claim trust <player>
/claim untrust <player>
/claim remove
```

## Admin Commands

```txt
/claim setfuel <hours>
/claim testmode
```

## Build

```powershell
gradle clean build
```

The compiled `.jar` will be generated in:

```txt
build/libs/
```

## Notes

This mod is designed for a custom private server environment.
