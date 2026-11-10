# Security

Blocks that open only for the right code. The **Keypad Controller** is a crafter whose slots are
its keys: switch the right slots off and it gives a redstone pulse.

## Getting them

| Item | Looks like | Recipe |
| --- | --- | --- |
| Keypad Controller | crafter | ![Keypad Controller recipe](images/recipes/ra_security/keypad.png){ width="220" } |

Operators: `/function ra_security:items/give_all` (a Keypad Controller).

## Keypad Controller

The Keypad Controller is a crafter. Open it and click its empty slots to switch them off and on,
as on any crafter: the **code** is the set of slots switched off. It never crafts.

1. Place the Keypad Controller. You are its **owner**.
2. Open it, switch off the slots of your code (at least one), close it and wait 4 seconds. The
   first combination entered becomes the code: players nearby are told "code set", and every slot
   is switched back on.
3. Wire the Keypad Controller to a door, a piston or anything else: dust, repeaters and
   components next to it pick up its pulse, as they would from a redstone block.
4. To open, switch off the same slots and wait 4 seconds. The Keypad Controller chimes and gives a
   redstone pulse of `pulse` ticks (20, one second, by default).

**Entering.** A combination counts as entered once the grid has not changed for **4 seconds**;
closing the screen is not needed, and changing your mind within those 4 seconds simply restarts
the wait. Whatever the result, every slot is switched back on afterwards. Switching every slot
back on before the 4 seconds are up enters nothing.

| Combination entered | What happens |
| --- | --- |
| No code set yet | It becomes the code ("code set") |
| The code | A redstone pulse and a chime |
| Anything else | "Wrong code" and a buzz; counts as a failed try |

**The pulse.** A crafter cannot give a redstone signal, so for the length of the pulse the
Keypad Controller becomes a powered target block, drawn as a lit crafter. Its grid cannot be
opened during the pulse. Afterwards it is a crafter again, facing the same way.

**Lockout.** A combination entered during a lockout is refused, even when the code was cleared
meanwhile, so the lockout cannot be skipped by setting a new code. After `max_attempts` wrong tries in a row (3 by default) the Keypad Controller refuses every
try for `lockout` seconds (30 by default), and says how long is left. A right code resets the
count. Only admins can change these two, so nobody can turn the lockout off to guess the code.

**No items.** Anything put into its grid, by hand or by a hopper, is thrown back out of its front
face at once (at most one chat line a minute says so), so a redstone signal on it never crafts
anything.

**Secrecy.** The code is never shown to anyone. The Goggles say only whether a code is set, who
owns the Keypad Controller and its state (ready, open, locked); the Data Handler lists its
settings, not the code; the Clipboard does not copy it. Someone else with the grid open while you
type it sees the slots change, and a comparator reads how many slots are off, so type it unwatched.

### Changing the code

The owner or an admin clears the code with a plain **Wrench** click on the Keypad Controller; the
next combination entered then becomes the new code. Breaking the Keypad Controller also loses it.

### Wrench and tools

| Tool | Does |
| --- | --- |
| Wrench, RMB | The owner or an admin clears the code |
| Wrench, Shift+RMB | Steps the pulse through 2, 10, 20 and 40 ticks |
| Goggles | Code (set or not set), owner, state, pulse |
| Data Handler | `pulse` (1–200 ticks); `max_attempts` (1–20) and `lockout` (1–3600 s), admins only; `owner` (read-only) |

Server admins set the defaults a new Keypad Controller starts with on the **Security** settings page, and
can switch the Keypad Controller off for the world there.

Breaking the Keypad Controller drops it; the code is lost with it.

## Troubleshooting

| What happens | Check |
| --- | --- |
| Nothing happens after switching slots | Wait 4 seconds without touching the grid; switching every slot back on enters nothing |
| "Keypad Controller code set" when you meant to open it | It had no code (new, or cleared): your combination is now the code |
| "Wrong code" | The code was changed, or the slots differ; it is the set of slots that counts |
| "This Keypad Controller is locked" | Too many wrong tries; wait the time it says |
| "Only its owner or an admin can clear the code" | Someone else placed it; ask them or an admin |
| Items pop out of it | It takes no items; use empty slots only |
| Owner shows `?` | The owner's name could not be read when it was placed; the owner is still known by their UUID |

---

## Advanced

### Keypad Controller data

| Where | What |
| --- | --- |
| `data.properties.pulse` | pulse length in ticks |
| `data.properties.max_attempts`, `data.properties.lockout` | wrong tries before a lockout, its length in seconds (admin-only) |
| `data.properties.owner` | the owner's name (read-only) |
| `data.data.code` | the code: a 9-bit mask of the switched-off slots (sum of 2^slot) |
| `data.data.seen` | the grid's mask at the last look |
| `data.data.said` | game time it last said it takes no items |
| `data.data.due` | game time the current entry counts as entered |
| `data.data.owner` | the owner's UUID |
| `data.data.fails` | wrong tries since the last right one or lockout |
| `data.data.until`, `data.data.locked_until` | game times: the end of the pulse, the end of the lockout |
| `data.data.orient` | during a pulse: the crafter's orientation, to put it back |
| `data.status.code`, `data.status.owner`, `data.status.state` | what the Goggles show |

The code is kept in `data.data`, never in `data.properties`, which is all the Data Handler, the
Clipboard and the Goggles read. The crafter's `disabled_slots` is read as a mask
(`ra_security:blocks/keypad/mask`): the array is copied into a list one entry at a time and each
slot is tested with an NBT pattern, so the order of the array does not matter and a missing or
empty array is 0. Entering compares the entry's mask with the code's. Nothing the player does is
pasted into a command.

**What runs.** Every tick, the block's tick gives each Keypad Controller two block tests and an
`if items` test (the container's slots, no NBT read). Items are thrown out the tick they are
seen; the crafter's own craft is a scheduled block tick 4 ticks after its redstone edge, which
runs after the data pack's tick, so it never sees them. The grid is read only every 4 ticks, only
with a player within 10 blocks (nobody else can click it), and only when a slot is off or an
entry is under way: a mask other than `data.data.seen` (every slot back on included) tags it
`ra.sec.kp_entry` and sets `data.data.due` 80 ticks ahead.

A pulsing Keypad Controller carries `ra.sec.kp_pulse`, a locked one `ra.sec.kp_locked`, one
waiting for its grid to settle `ra.sec.kp_entry`, and any of them `ra.sec.kp_timed`; a schedule
(`ra_security:blocks/keypad/clock/run`) runs every tick while one pulses and once a second while
only locked or waiting ones are left, and stops with none. For the pulse the crafter is replaced
by `target[power=15]` (a power source, like the redstone block) and a block display tagged
`ra.sec.kp_skin` draws a lit crafter over it; at the end the crafter is set back with the
orientation it had. A marker removed by hand mid-pulse (`/kill`) leaves the powered target
block behind. The owner's name is read at placement from a player head given the placer's
profile (`fill_player_head`).
