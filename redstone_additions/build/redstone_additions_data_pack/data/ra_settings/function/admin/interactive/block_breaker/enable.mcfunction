# Allow Block Breaker to be placed again.
data remove storage ra:settings disabled[{b:"block_breaker"}]
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Block Breaker",color:"white"},{text:" enabled.",color:"green"}]
