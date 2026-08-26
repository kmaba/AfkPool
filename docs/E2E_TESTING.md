# End-to-End Testing AfkPool on a real server

This documents how v2.2.0's action-bar countdown feature was verified against a
real Paper server, and how to re-run the same tests. A ready-to-use copy of the
test environment lives at **`/root/afkpool-testenv/`** on this machine
(see [Pre-built local environment](#pre-built-local-environment)).

## What gets verified

| Test | How |
|---|---|
| Plugin enables cleanly | Server log: `[AfkPool] AfkPool Version X enabled.` |
| Countdown shows inside region | mineflayer bot captures `ACTIONBAR \|Next reward in m:ss\|` packets every second |
| Countdown synced with rewards | Bar resets to full interval on the exact tick the reward fires (`Gave N ...` + title packets) |
| Countdown stops outside region | 0 action-bar packets while bot is teleported away |
| Countdown resumes on re-entry | Packets resume immediately after teleporting back |
| `timer.enabled: false` works | Flip config, `/afkpool reload`, expect 0 packets |
| `/afkpool reload` is safe (v2.2.1+) | Reload with a player online; expect no `IllegalStateException: zip file closed`, task count stable (`/ap values` shows `scheduled-tasks`), rewards continue |
| Unknown region-name warns (v2.2.1+) | Set region-name to a non-existent region + reload; expect a clear warning in the log and no crash |

## Pre-built local environment

```
/root/afkpool-testenv/
├── mcserver/          # Paper 1.21.4 server, fully configured & pre-booted
│   ├── paper.jar      # Paper 1.21.4 build 232
│   ├── eula.txt       # already accepted
│   ├── server.properties  # offline-mode, RCON :25575 pw=testpass123, low view distance
│   ├── ops.json       # TestBot pre-opped (level 4)
│   ├── rcon.py        # stdlib RCON client: python3 rcon.py "<command>"
│   └── plugins/
│       ├── afkpool-*.jar    # copy target/afkpool-x.y.z.jar here after building
│       ├── worldedit.jar    # WorldEdit 7.3.14 (MUST be Java 21 build, see gotchas)
│       ├── worldguard.jar   # WorldGuard 7.0.13-dist
│       ├── AfkPool/config.yml     # short intervals (200/400 ticks = 10s/20s) for fast cycles
│       └── WorldGuard/worlds/world/regions.yml  # afkpool-region1 (-20..20), afkpool-region2
└── minecraft-bot/           # Node.js mineflayer client
    ├── bot.js               # join + record all events (rcon drives teleports)
    ├── bot2.js              # self-driven enter/exit/re-enter timeline
    └── bot3.js              # spectator-mode enter/exit/re-enter (recommended)
```

### Run the whole thing

```sh
cd /root/AfkPool && mvn -B clean package          # build the jar
cp target/afkpool-*.jar /root/afkpool-testenv/mcserver/plugins/afkpool.jar

cd /root/afkpool-testenv/mcserver
setsid nohup java -Xms512M -Xmx1200M -jar paper.jar nogui </dev/null > server.log 2>&1 &
# wait ~60-90s until "Done (... )!" appears in server.log

python3 rcon.py "list"                            # sanity check

cd ../minecraft-bot && npm install                # only if node_modules missing
timeout 40 node bot3.js                           # run scenario, prints ACTIONBAR lines live
```

Then assert on `events3.log`:

```python
import re
lines = open('events3.log').read().splitlines()
phase, counts = 'pre', {'pre':0, 'outside':0, 'reentered':0}
for l in lines:
    if 'PHASE tp-out' in l: phase = 'outside'
    elif 'PHASE tp-back-in' in l: phase = 'reentered'
    if re.match(r'^[\d.]+ ACTIONBAR \|Next reward in \d+:\d+\|', l):
        counts[phase] += 1
assert counts['pre'] >= 2 and counts['outside'] == 0 and counts['reentered'] >= 5
```

Shut down with: `pkill -f paper.jar`

## Building the environment from scratch

### 1. Downloads (URLs valid as of Aug 2026)

PaperMC's old `api.papermc.io/v2` is dead (HTTP 410). Use the new API:

```sh
curl -s https://fill.papermc.io/v3/projects/paper/versions/1.21.4/builds/latest \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['downloads']['server:default']['url'])"
```

WorldEdit/WorldGuard come from Modrinth:
`https://api.modrinth.com/v2/project/<worldedit|worldguard>/version?loaders=paper&game_versions=%5B%221.21.4%22%5D`

**Gotcha — Java version:** the *latest* WE/WG builds are compiled for Java 25+
(class file major 69) and fail to load on a Java 21 runtime with
`UnsupportedClassVersionError`. Verify before use:

```sh
unzip -p plugin.jar com/sk89q/worldguard/bukkit/WorldGuardPlugin.class \
  | od -An -j6 -N2 -d --endian=big     # must print <= 65 (= Java 21)
```

Known-good combo: **WorldEdit 7.3.14 + WorldGuard 7.0.13-dist** (all classes
major ≤ 65).

### 2. Config files

`eula.txt`: `eula=true`

Key `server.properties` lines:

```properties
online-mode=false          # lets the offline mineflayer bot join
enable-rcon=true
rcon.port=25575
rcon.password=testpass123
view-distance=3            # keeps memory/CPU low
spawn-protection=0
```

`ops.json` — pre-op the bot using the offline-mode UUID
(`UUID.nameUUIDFromBytes("OfflinePlayer:<name>")` = MD5-based v3 UUID):

```python
import hashlib, uuid
name = "TestBot"
d = bytearray(hashlib.md5(("OfflinePlayer:"+name).encode()).digest())
d[6] = (d[6] & 0x0F) | 0x30; d[8] = (d[8] & 0x3F) | 0x80
print(uuid.UUID(bytes=bytes(d)))
```

`plugins/AfkPool/config.yml` — shorten intervals so reward cycles happen in
seconds during the test (`interval: 200` = 10s, `interval: 400` = 20s).

Region definitions can be hand-written into
`plugins/WorldGuard/worlds/world/regions.yml` before first boot — WorldGuard
loads them fine:

```yaml
regions:
    afkpool-region1:
        min: {x: -20, y: -64, z: -20}
        max: {x: 20, y: 319, z: 20}
        members: {}
        owners: {}
        flags: {}
        priority: 0
        type: cuboid
```

Note: world spawn lands inside region1 (~5.5, ?, 10.5) — convenient, because
the bot is inside the pool immediately on join.

### 3. The mineflayer bot

```sh
npm init -y && npm install mineflayer
```

Core pattern (full versions in `bot.js`–`bot3.js`):

```js
const bot = mineflayer.createBot({
  host: '127.0.0.1', port: 25565,
  username: 'TestBot', auth: 'offline',
  version: '1.21.4'
})
bot.on('actionBar', msg => log('ACTIONBAR |' + strip(msg) + '|'))  // the countdown
bot.on('title', (text, type) => log(`TITLE ${type}`))               // entering/exiting titles
bot.chat('/tp 5 100 5')   // bot is opped, can teleport itself (more reliable than rcon timing)
```

RCON is used for server-side commands (`list`, `op`, reload checks):
`python3 rcon.py "afkpool values"`.

## Gotchas learned the hard way

1. **Detaching long-running processes**: this sandbox's bash tool kills the
   process group when a command times out — a plain `nohup java ... &` gets
   killed too. Use `setsid nohup java -jar paper.jar nogui </dev/null >log 2>&1 & disown`.
2. **Don't teleport the test bot high up**: falling from y=100 kills it
   ("TestBot fell from a high place") and it respawns at world spawn — which is
   *inside* the AFK pool, making exit-tests meaningless. Use
   `/gamemode spectator` before teleporting out.
3. **Coordinate phases inside one script**: driving bot + RCON from separate
   shell commands drifts badly; put the timeline in the bot script itself
   (`setTimeout` chain) as in `bot3.js`.
4. **`TITLE` events print `[object Object]`** with naive `String(text)` —
   cosmetic mineflayer quirk, the title/subtitle packets do arrive correctly.
5. **WorldGuard console output via RCON is empty** for `rg list`/`rg info`;
   verify regions by behavior instead (or read `regions.yml`).
6. **Pre-existing UpdateChecker quirk**: any version difference logs
   `"There is a new AfkPool update available (X --> Y)"` even when installed >
   remote, and the direction shown is reversed. Harmless, unrelated to the timer.

## Results from the v2.2.0 verification run (Aug 2026)

All assertions passed:

```
action-bar counts per phase: {'pre': 6, 'outside': 0, 'reentered': 12}
ALL PHASE ASSERTIONS PASSED
```

Countdown observed ticking `0:09 → 0:01`, resetting to `0:10` exactly when
`Gave 2 [Diamond]` fired; zero packets outside the region; disabled cleanly via
`timer.enabled: false` + `/afkpool reload`; no exceptions in the server log for
the entire session.
