# Random Crafts

[![License](https://img.shields.io/github/license/Weyne1/random-crafts.svg)](https://github.com/Weyne1/random-crafts/blob/master/LICENSE)
[![MC Versions](https://cf.way2muchnoise.eu/versions/For%20MC_1523432_all.svg)](https://www.curseforge.com/minecraft/mc-mods/random-crafts)
[![CurseForge Downloads](https://cf.way2muchnoise.eu/full_1523432_downloads.svg)](https://www.curseforge.com/minecraft/mc-mods/random-crafts)
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/d4RW7cgE?label=Modrinth%20Downloads&color=00AF5C)](https://modrinth.com/mod/random-crafts)

### Versions
[1.21.1](https://github.com/Weyne1/random-crafts/tree/1.21.1)
[1.21.3](https://github.com/Weyne1/random-crafts/tree/1.21.3)
[1.21.4](https://github.com/Weyne1/random-crafts/tree/1.21.4)
[1.21.5](https://github.com/Weyne1/random-crafts/tree/1.21.5)
[1.21.6-1.21.8](https://github.com/Weyne1/random-crafts/tree/1.21.6)
[1.21.9-1.21.10](https://github.com/Weyne1/random-crafts/tree/1.21.9)
[1.21.11](https://github.com/Weyne1/random-crafts/tree/1.21.11)

## Updating / Downgrading Versions

> [!IMPORTANT]
> Before updating, check if the version you need is available at https://parchmentmc.org/docs/getting-started

Change the settings in `gradle.properties`
* `minecraft_version` in `gradle.properties`
* Select the correct mc_pack_format from the website https://minecraft.wiki/w/Pack_format
* Update the validation ranges if they are not listed (`build.gradle:8`)
* Synchronize dependencies in `gradle.properties`

## Gradle commands
Build: `./gradlew build`  
Build for a specific platform:
* Fabric: `./gradlew :fabric:build`
* Forge:  `./gradlew :neoforge:build`

Force a build config check without building:  
`./gradlew validateBuildConfig`
