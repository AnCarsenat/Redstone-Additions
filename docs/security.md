# Security

Blocks that open only for the right code. The **Keypad Controller** gives a redstone pulse when it is shown
the [Pattern](automation.md#pattern) it was locked with.

## Getting them

| Item | Looks like | Recipe |
| --- | --- | --- |
| Keypad Controller | target block | ![Keypad Controller recipe](images/recipes/ra_security/keypad.png){ width="220" } |

Operators: `/function ra_security:items/give_all` (a Keypad Controller). The code is a Pattern from the
[Automation](automation.md) module: `/function ra_automation:pattern/give` gives a blank one.

## Keypad Controller

The code of a Keypad Controller is a Pattern: a 3×3 grid of items, recorded by sneaking and right-clicking
a crafter with a Pattern (see [Pattern](automation.md#pattern)). Two Patterns are the same code
when every one of the nine slots holds the same item; counts and item details do not matter.

1. Place the Keypad. You are its **owner**.
2. Record a grid on a Pattern, then **sneak + right-click the Keypad Controller** with it. That grid is the
   code. The Pattern stays in your hand: keep it, or copy the grid onto other Patterns for the
   people you trust.
3. Wire the Keypad Controller to a door, a piston or anything else: it is a target block, and it powers dust
   and blocks next to it like one.
4. To open, **right-click the Keypad Controller** with a Pattern holding the code. The Keypad Controller chimes and
   gives a redstone pulse of `pulse` ticks (20, one second, by default).

| Holding a Pattern | What happens |
| --- | --- |
| **Right-click** | Enters the Pattern. Right: a pulse and a chime. Wrong: a buzz. |
| **Sneak + right-click**, recorded Pattern | Sets the code. Allowed while the Keypad Controller has none, and always for its owner and for admins. |
| **Sneak + right-click**, blank Pattern | Clears the code (owner and admins). |

**Lockout.** After `max_attempts` wrong tries in a row (3 by default) the Keypad Controller refuses every
try for `lockout` seconds (30 by default), and says how long is left. A right code resets the
count. Only admins can change these two, so nobody can turn the lockout off to guess the code.

**Secrecy.** The code is never shown to anyone. The Goggles say only whether a code is set, who
owns the Keypad Controller and its state (ready, open, locked); the Data Handler lists its settings, not the
code; the Clipboard does not copy it. Anyone who knows the grid can still record it on a Pattern
of their own, so choose one that is hard to guess.

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
| "This Keypad Controller has no code yet" | Sneak + right-click it with a recorded Pattern to set one |
| "Only this Keypad Controller's owner can change its code" | Someone else placed it; ask them or an admin |
| "Wrong code" with the right Pattern | The code was changed, or the Pattern was re-recorded since; compare the grids in the lore |
| "This Keypad Controller is locked" | Too many wrong tries; wait the time it says |
| Owner shows `?` | The owner's name could not be read when it was placed; the owner is still known by their UUID |

---

## Advanced

### Keypad Controller data

| Where | What |
| --- | --- |
| `data.properties.pulse` | pulse length in ticks |
| `data.properties.max_attempts`, `data.properties.lockout` | wrong tries before a lockout, its length in seconds (admin-only) |
| `data.properties.owner` | the owner's name (read-only) |
| `data.data.code` | the code: a Pattern's `slots` list, nine strings |
| `data.data.owner` | the owner's UUID |
| `data.data.fails` | wrong tries since the last right one or lockout |
| `data.data.until`, `data.data.locked_until` | game times: the end of the pulse, the end of the lockout |
| `data.status.code`, `data.status.owner`, `data.status.state` | what the Goggles show |

The code is kept in `data.data`, never in `data.properties`, which is all the Data Handler, the
Clipboard and the Goggles read. Entering compares whole lists: the stored `slots` are copied
into storage and overwritten with the entered ones (`execute store success … data modify … set
from`); no change means the codes are equal. Nothing from the Pattern is pasted into a command.

Nothing runs while nobody uses a Keypad. A right-click with a Pattern runs it once (the
Pattern's `using_item` advancement, see [Pattern data](automation.md#pattern-data)). A pulsing
Keypad Controller carries `ra.sec.kp_pulse`, a locked one `ra.sec.kp_locked`, and either one
`ra.sec.kp_timed`; a schedule (`ra_security:blocks/keypad/clock/run`) runs every tick while one
pulses and once a second while only locked ones are left, and stops with neither. The owner's
name is read at placement from a player head given the placer's profile
(`fill_player_head`).
