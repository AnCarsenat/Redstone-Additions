# /ra_ender:blocks/item_vault/process
# Move stacks between this vault and its partner.
# Context: as a vault marker, at the barrel.
#
# `link` mode is two-way, and it works by watching what the *outside world* does
# to this barrel rather than by comparing the two ends. Every cycle the number of
# stacks present is compared with the number this vault last left behind:
#
#   more than before  -> something was put in here, so push a stack across
#   fewer than before -> something was taken out here, so pull one back
#   the same          -> nobody touched either end; do nothing at all
#
# Comparing contents instead would shuffle forever: A hands its only stack to B,
# which then has more than A and hands it straight back. And a rule that only
# moved on a difference of two or more could never move a single stack at all,
# which is what made the first version one-way.
#
# Deliveries update the receiver's mark immediately, so an arriving stack is not
# mistaken for an insert and bounced back.

scoreboard players set @s ra.ender.cd 0

# Held for the length of this call: a two-way vault wears the send and the
# receive tag, so without this every partner search could find itself.
tag @s add ra.ender.self


# How full the barrel is now, and how full it was when we last looked.
scoreboard players set #ender.used ra.temp 0
execute if data block ~ ~ ~ Items store result score #ender.used ra.temp run data get block ~ ~ ~ Items
execute unless data entity @s data.data.last_used run data modify entity @s data.data.last_used set value 0
execute store result score #ender.was ra.temp run data get entity @s data.data.last_used

# Shared: the contents follow the player, and there is nothing else to do.

# One-way modes keep their old, simpler behaviour: a sender drains, a receiver waits.
execute if data entity @s data.properties{mode:"send"} run function ra_ender:blocks/item_vault/push
execute if data entity @s data.properties{mode:"receive"} run function ra_ender:blocks/item_vault/mark
execute unless data entity @s data.properties{mode:"link"} run return run function ra_ender:link/done

# Two-way: follow the outside world.
execute if score #ender.used ra.temp > #ender.was ra.temp run function ra_ender:blocks/item_vault/push
execute if score #ender.used ra.temp < #ender.was ra.temp run function ra_ender:blocks/item_vault/pull

function ra_ender:blocks/item_vault/mark

function ra_ender:link/done
