# Allow Solar Panel to be placed again.
data remove storage ra:settings disabled[{b:"solar_panel"}]
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Solar Panel",color:"white"},{text:" enabled.",color:"green"}]
