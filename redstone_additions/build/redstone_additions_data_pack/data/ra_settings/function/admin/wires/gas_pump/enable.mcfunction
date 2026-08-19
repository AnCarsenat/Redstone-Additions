# Allow Gas Pump to be placed again.
data remove storage ra:settings disabled[{b:"gas_pump"}]
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Gas Pump",color:"white"},{text:" enabled.",color:"green"}]
