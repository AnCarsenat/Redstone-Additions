# /ra_interactive:blocks/block_placer/fire
# Place one block in front. Context: as the marker, at the placer, already
# confirmed powered, with air in front and something in the inventory.
#
# The cooldown is reset before anything is placed, for the same reason the Block
# Breaker resets first: the old version placed the block and then re-tested
# `if block ^ ^ ^1 air` before resetting. Placing a block makes that space not
# air, so the reset never ran, the counter climbed for ever, and the placer
# emptied itself at one block per tick.

scoreboard players set @s ra.cooldown 0

data modify storage ra:temp place_item set from block ~ ~ ~ Items[0]
execute positioned ^ ^ ^1 run function ra_interactive:blocks/block_placer/place_block with storage ra:temp place_item
playsound minecraft:block.stone.place block @a[distance=..16,scores={ra.u.snd=1..}] ^ ^ ^1 1 0.8
particle minecraft:cloud ^ ^ ^1 0.2 0.2 0.2 0.02 5 normal @a[scores={ra.u.par=1..}]
