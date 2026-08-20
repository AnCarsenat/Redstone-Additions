# Allow Teleport Anchor to be placed again.
data remove storage ra:settings disabled[{b:"teleport_anchor"}]
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Teleport Anchor",color:"white"},{text:" enabled.",color:"green"}]
