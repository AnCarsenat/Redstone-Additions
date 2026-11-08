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

Neither block has an off switch. An Emitter transmits while it is powered: to stop it, cut its
redstone input or give it a channel no Receiver listens on. A Receiver follows its channel: to keep
one off, give it a channel nothing transmits on, or remove the iron blocks around it.

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
| Receiver never turns on | The channel names are identical, the Emitter is powered, and there are iron blocks next to the Receiver |
| Receiver stays on | Another Emitter on the same channel is still powered |
| Remote does nothing | Its channel matches the Receiver's exactly |

---

## Advanced

- Properties on the markers: `data.properties.channel` (string, default `"default"`). Emitters
  and Receivers placed before v6.0.1 may still carry an `enabled` key; nothing reads it.
- The Remote's channel is stored on the item: `custom_data.ra.channel`.
- An Emitter reads its power with `ra_lib:redstone/detect`; while powered it carries
  the tag `ra.transmitting`. A Receiver scans transmitters for an exact channel match and tags
  itself `ra.receiving`.
- Remote pulses use `ra.pulsing` and the `ra.pulse_timer` score on the Receiver.
