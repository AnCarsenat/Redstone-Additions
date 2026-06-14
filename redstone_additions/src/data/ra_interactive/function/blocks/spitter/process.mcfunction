# /ra_interactive:blocks/spitter/process
# Process single spitter. As armor stand, at dropper position.
# Spits one item every 4 ticks if has items.

# Initialize cooldown if not set
execute unless score @s ra.cooldown matches -2147483648.. run scoreboard players set @s ra.cooldown 0

# Cooldown
scoreboard players add @s ra.cooldown 1
execute if score @s ra.cooldown matches ..3 run return 0

# Check if dropper has any items
execute unless data block ~ ~ ~ Items[0] run return 0

# Has item - reset cooldown and spit
scoreboard players set @s ra.cooldown 0

# Store item data
data modify storage ra:temp spit_item set from block ~ ~ ~ Items[0]

# Spawn item entity in front (^ ^ ^0.8)
summon item ^ ^ ^0.8 {Item:{id:"minecraft:stone",count:1},Tags:["ra.spit"],PickupDelay:10s}
data modify entity @e[type=item,tag=ra.spit,limit=1,sort=nearest] Item set from storage ra:temp spit_item

# Remove tag
tag @e[type=item,tag=ra.spit] remove ra.spit

# Remove item from dropper
data remove block ~ ~ ~ Items[0]

# Play sound
playsound minecraft:block.dispenser.dispense block @a[distance=..16] ~ ~ ~ 0.5 1.2
