# /ra_lib:input/session/timeout
# Expire active sessions that exceeded their TTL.

tellraw @s [{text:"[RA Input] ",color:"gold"},{text:"Input request expired.",color:"red"}]
function ra_lib:input/session/cleanup
tellraw @s [{text:"[RA Input] ",color:"gold"},{text:"Input timed out (req ",color:"gray"},{score:{name:"@s",objective:"ra.input.req"},color:"gray"},{text:").",color:"gray"}]
