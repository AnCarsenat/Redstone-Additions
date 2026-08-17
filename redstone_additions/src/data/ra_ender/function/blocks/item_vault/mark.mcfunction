# /ra_ender:blocks/item_vault/mark
# Record how full the barrel is, so the next cycle can tell an outside change from
# one this module made itself.
# Context: as the vault marker, at the barrel.

scoreboard players set #ender.mark ra.temp 0
execute if data block ~ ~ ~ Items store result score #ender.mark ra.temp run data get block ~ ~ ~ Items
execute store result entity @s data.data.last_used int 1 run scoreboard players get #ender.mark ra.temp
