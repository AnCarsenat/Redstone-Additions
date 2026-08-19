# Goggles redraw (ticks): back to the shipped default of 20.
data modify storage ra:settings global."goggles_redraw" set value 20
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Goggles redraw (ticks)",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.\"goggles_redraw\"",storage:"ra:settings",color:"aqua"}]
