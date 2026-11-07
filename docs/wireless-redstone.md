# Wireless Redstone

Send a redstone signal anywhere without wires. An **Emitter** broadcasts on a named channel, every
**Receiver** on the same channel turns on, and the **Redstone Remote** pulses receivers from your
hand.

## Getting them

| Item | Recipe |
| --- | --- |
| Wireless Emitter | ![Wireless Emitter recipe](images/recipes/ra_wireless/emitter.png){ width="240" } |
| Wireless Receiver | ![Wireless Receiver recipe](images/recipes/ra_wireless/receiver.png){ width="240" } |
| Redstone Remote | ![Redstone Remote recipe](images/recipes/ra_wireless/remote.png){ width="240" } |

Operators: `/function ra_wireless:items/give_all`, or `/function ra_wireless:tools/remote/give`
for the Remote alone.

## Using them

1. Place an Emitter and a Receiver. Both start on the channel `default`.
2. To use another channel, set the same name on both with the [Data Handler](tools.md) (for
   example `main`). Channels are text and must match exactly.
3. Put **iron blocks** next to the Receiver (anywhere in the 3×3×3 cube around it). They become
   **redstone blocks** while it is on.
4. Power the Emitter. Every Receiver on its channel turns on, and stays on while any Emitter on
   that channel is powered.

### Switching a block off

Right-click an Emitter or Receiver with the [Wrench](tools.md) to disable or enable it. A disabled
Emitter does not transmit; a disabled Receiver keeps its output off.

### The Redstone Remote

| Action | Does |
| --- | --- |
| Right-click | Pulses every Receiver on the Remote's channel for 4 ticks |
| Sneak + right-click | Opens an **Input Form** book: write the channel on page 1 and close it. Drop the book to cancel |

The Remote tells you its channel in chat each time you use it. Setting the channel needs no
permissions, so it works in survival.

## Troubleshooting

| Problem | Check |
| --- | --- |
| Receiver never turns on | The channel names are identical, the Emitter is powered, neither block is disabled, and there are iron blocks next to the Receiver |
| Receiver stays on | Another Emitter on the same channel is still powered |
| Remote does nothing | Its channel matches the Receiver's exactly |

---

## Advanced

- Properties on the markers: `data.properties.channel` (string, default `"default"`),
  `data.properties.enabled` (`1b` / `0b`, toggled by the wrench).
- The Remote's channel is stored on the item: `custom_data.ra.channel`.
- An Emitter reads its power with `ra_lib:redstone/detect`; while powered and enabled it carries
  the tag `ra.transmitting`. A Receiver scans transmitters for an exact channel match and tags
  itself `ra.receiving`.
- Remote pulses use `ra.pulsing` and the `ra.pulse_timer` score on the Receiver.
