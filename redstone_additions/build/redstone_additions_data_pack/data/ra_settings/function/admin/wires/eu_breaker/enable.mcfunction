# Allow EU Breaker to be placed again.
data remove storage ra:settings disabled[{b:"electric_breaker"}]
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"EU Breaker",color:"white"},{text:" enabled.",color:"green"}]
