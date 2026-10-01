# ATT2_Project — unofficial rebuild

An unofficial, rebuildable copy of **ATT2_Project**, Poubone's client-side companion mod for the
[Across The Time II](https://adventquest.com/across-the-time-ii-time-for-regrets) adventure map
(custom HUD, quest book, shop and stat menus, teleporter transitions).

The original source isn't public, so this repository is a decompile of the official release jars,
set up so it builds with Gradle and can be patched. On top of the original it adds a small set of
fixes, listed below. Everything else behaves exactly like the official release.

This is not an official release and is not affiliated with or endorsed by Poubone or the map's authors.

## Changes from the official release

**[Flashback](https://modrinth.com/mod/flashback) replay fixes.** In a replay, ATT2 reacted to
the recorded chat, sounds and scoreboard as if they were happening live. While a Flashback replay
is playing, this rebuild:

- doesn't open the NPC shop, miner shop, gambling or rune codex menus. Replayed chat used to
  reopen them, and nothing in the recording ever closed them again.
- doesn't send `/trigger` or scoreboard commands to Flashback's replay server, which rejected them
  with chat errors like `Unknown scoreboard objective 'ScoreTrigger'`.
- doesn't start the teleporter transition, which took over Flashback's camera and input.
- pauses Party Sync, and clears Discord Rich Presence until the replay ends.

Without Flashback installed, none of this has any effect.

**Online features are opt-in.** Two features talk to services outside the game. Both are now off
by default:

| Feature | What it contacts | How to enable |
|---|---|---|
| Party Sync (pings and item sharing) | `https://sync.guide-att2.com` over HTTPS. While enabled it sends, about once a second, your player UUID and name and the UUIDs of the players in the server's tab list. Pings and shared items send their position or item data | ATT2 HUD settings → General → **Party Sync**, or `"partySyncEnabled": true` in `config/att2_hud.json` |
| Discord Rich Presence | The Discord desktop app on your computer, which shows your current region and quest on your Discord profile | ATT2 HUD settings → General → **Discord Rich Presence** |

If you're upgrading from the official mod, your existing config is switched to these defaults once.
Anything you turn back on afterwards stays on.

## Installing

Requires Minecraft **1.21.11**, Fabric Loader **0.18.4** or newer and Fabric API.

Remove the official `ATT2_Project-*.jar` from your `mods` folder and add the rebuilt jar. It uses
the same mod id and config files, so it's a drop-in replacement. You can build the jar yourself
(below), or download it from [Releases](../../releases) when one is published.

## Building

Needs JDK 21 or newer.

```bash
./gradlew build
```

The jar is written to `build/libs/`.

## Updating to a new official release

`original/` holds the official jars this repository was decompiled from. The `upstream` branch
contains only the unmodified decompiles, one commit per release, and `master` puts the fixes on
top. To bring in a new release:

```bash
git checkout upstream
scripts/update-upstream.sh path/to/ATT2_Project-X.Y.Z.jar
git add -A && git commit -m "Update decompiled source to ATT2_Project X.Y.Z"
git checkout master && git rebase upstream
```

The script remaps the jar to Mojang names with Loom, decompiles it with
[Vineflower](https://github.com/Vineflower/vineflower), replaces `src/`, and runs
`scripts/fix_mixin_casts.py`. That restores the `(Object)` casts that compiling strips out of mixin
code, which the decompiler can't recover. Rebuilt without changes, the result matches the official
jar's classes and member signatures. Needs bash (Git Bash works on Windows), curl, Python and Java 21+.

## Credits and license

ATT2_Project is by **Poubone** and released under the [MIT License](LICENSE). This rebuild and its
changes are published under the same license.

Bundled with the original mod:

- Loot Beams code by EnsoX, VanderCat, Grend and IMS (MIT, see `LICENSE_lootbeams`)
- Ping rendering adapted from Ping Wheel by LukenSkyne (MIT, see `LICENSE_pingwheel`)
- RPG UI assets by Kenney (CC0, see `LICENSE_kenney_rpg_ui`)
- Grand Teleport transition code by Codex / hookuru_

The license files ship inside the jar, in `src/main/resources/`.
