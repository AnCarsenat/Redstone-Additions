# /ra_ender:load
# RA Ender — remote vaults and teleport anchors.
#
# A vault is half of a link. Two vaults sharing a channel string move contents
# between them, one sending and one receiving, so nothing is ever copied: every
# transfer removes from the source before, or in the same command as, it adds to
# the destination.

# Per-block work timers.
scoreboard objectives add ra.ender.cd dummy

# Teleport cooldowns: on the anchor, and on the player who just arrived.
scoreboard objectives add ra.ender.tp_cd dummy
scoreboard objectives add ra.ender.grace dummy

function ra_ender:blocks/item_vault/register_block
function ra_ender:blocks/fluid_vault/register_block
function ra_ender:blocks/power_vault/register_block
function ra_ender:blocks/teleport_anchor/register_block

tellraw @a [{text:"[",color:"dark_gray"},{text:"RA",color:"gold",bold:true},{text:"] ",color:"dark_gray"},{text:"Ender links loaded!",color:"light_purple"}]
