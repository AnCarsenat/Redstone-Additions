# /ra:tools/clipboard/clear
# Empty this player's clipboard. Context: as the player.

execute store result storage ra:temp clip.slot int 1 run scoreboard players get @s ra.clip.id
function ra:tools/clipboard/clear_slot with storage ra:temp clip
data remove storage ra:temp clip

tellraw @s [{text:"[Clipboard] ",color:"gold"},{text:"Cleared. The next block you click becomes the new origin.",color:"gray"}]
playsound minecraft:item.book.put block @s ~ ~ ~ 0.7 0.9
