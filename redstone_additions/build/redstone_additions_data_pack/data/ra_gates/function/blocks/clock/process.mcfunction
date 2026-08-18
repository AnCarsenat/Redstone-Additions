# /ra_gates:blocks/clock/process
# Pulse: swap the ring to redstone for exactly one tick, every `delay` ticks.
# Context: as the clock marker, at its position.
#
# THE SHAPE THAT MATTERS
# The output is put out at the TOP of the tick, but only if it was lit — never
# unconditionally. That single word is what broke this block before: an
# unconditional off-fill followed by a conditional on-fill means both run on the
# same tick whenever the period is short, and the ring reads as permanently
# powered rather than pulsing.
#
# It also means an idle clock writes no blocks at all. The old version ran a fill
# over 27 blocks every tick of every clock in the world, whether anything changed
# or not, which is a block update per neighbour per tick for nothing.
#
# `cooldown` is in ticks and is the only property this block has: the number of
# ticks to wait before the next pulse. Every timed block in the pack uses that one
# name for that one meaning.

# Put out last tick's pulse. Conditional on the latch: nothing here fires unless
# there is something to switch off.
scoreboard players set #clk.was ra.temp 0
execute if entity @s[tag=ra.clock_on] run scoreboard players set #clk.was ra.temp 1
execute if score #clk.was ra.temp matches 1 run tag @s remove ra.clock_on
execute if score #clk.was ra.temp matches 1 at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block

# The period, in ticks. Guarded, because `store result` writes ZERO when its
# command fails, and zero means "fire every tick".
#
# Floored at 2, not 1: a pulse needs a tick to be on and a tick to be off. At a
# period of 1 the two fills above and below meet on the same tick and the ring
# never goes dark — which is exactly the "always redstone" this block was
# reported with. One tick on, one tick off is as fast as a pulse can honestly go.
scoreboard players set #clk.max ra.temp 20
execute if data entity @s data.properties.cooldown store result score #clk.max ra.temp run data get entity @s data.properties.cooldown 1
execute if score #clk.max ra.temp matches ..2 run scoreboard players set #clk.max ra.temp 2

# A score that was never set reads as absent, and a comparison against an absent
# score is false — which would fire every tick. This is what was actually wrong.
execute unless score @s ra.cooldown matches -2147483648.. run scoreboard players set @s ra.cooldown 0

scoreboard players add @s ra.cooldown 1
execute if score @s ra.cooldown < #clk.max ra.temp run return 0
scoreboard players set @s ra.cooldown 0

# Fire. The latch is what tells the top of the next tick to put it out again.
tag @s add ra.clock_on
execute at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block
