# /ra_ender:link/return_fluid {medium:"..."}
# Context: as the sending vault marker. Puts the unaccepted remainder back.

execute store result storage ra:ender offer.amount int 1 run scoreboard players get #ender.carry ra.temp
data modify storage ra:ender offer.medium set from storage ra:ender fluid.medium
function ra_ender:link/offer_fluid with storage ra:ender offer
