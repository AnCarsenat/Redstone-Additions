# Allow Boiler to be placed again.
data remove storage ra:settings disabled[{b:"boiler"}]
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Boiler",color:"white"},{text:" enabled.",color:"green"}]
