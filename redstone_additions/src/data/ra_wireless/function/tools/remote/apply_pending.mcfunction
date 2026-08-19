# /ra_wireless:tools/remote/apply_pending
# Write a finished book input onto the player's remote.
# Context: as a player with ra.remote.pending set.
#
# This must NOT require the remote to still be the selected item. Writing in the
# Input Form means selecting the book, so a guard on SelectedItem fires the
# moment the player picks the book up — and cancelling the session runs
# clear_book_req, which deletes the book out of their hand. That was exactly the
# "book disappears when I switch to it" behaviour.
#
# The remote's hotbar slot is recorded when the prompt opens instead, since the
# player is necessarily holding it at that point. The session TTL still collects
# a request the player abandons.

# A cancelled session (the player dropped the form) never becomes ready, so stop
# waiting for it. `submit` clears ra.input.active but leaves state at 2, hence
# the second condition — without it a finished input would be discarded here on
# the very tick it became available.
execute unless entity @s[tag=ra.input.active] unless score @s ra.input.state matches 2 run scoreboard players set @s ra.remote.pending 0
execute unless entity @s[tag=ra.input.active] unless score @s ra.input.state matches 2 run return 0

execute store result score #remote_state ra.temp run function ra_lib:input/poll
execute unless score #remote_state ra.temp matches 2 run return 0

execute store result score #remote_state ra.temp run function ra_lib:input/consume
execute unless score #remote_state ra.temp matches 1 run return 0

scoreboard players set @s ra.remote.pending 0

# An empty book leaves the channel alone rather than blanking it.
execute if data storage ra:input consume{text:""} run tellraw @s [{text:"[Remote] ",color:"red"},{text:"No channel entered; left unchanged.",color:"gray"}]
execute if data storage ra:input consume{text:""} run return 0

# The player's text goes into the item via an item modifier reading from storage.
# Substituting it into an `item replace ... with blaze_rod[...]` literal instead
# would let a book containing a quote character append arbitrary components.
data modify storage ra_wireless:remote channel set from storage ra:input consume.text

execute store result storage ra_wireless:remote slot int 1 run scoreboard players get @s ra.remote.slot
execute store result score #remote_state ra.temp run function ra_wireless:tools/remote/apply_to_slot with storage ra_wireless:remote

execute if score #remote_state ra.temp matches 0 run tellraw @s [{text:"[Remote] ",color:"red"},{text:"Remote moved out of its slot; channel not applied.",color:"gray"}]
execute if score #remote_state ra.temp matches 0 run return 0

data modify storage ra:temp current_channel set from storage ra_wireless:remote channel
tellraw @s [{text:"[Remote] ",color:"red"},{text:"Channel set to ",color:"gray"},{nbt:"current_channel",storage:"ra:temp",color:"yellow"}]
playsound minecraft:block.note_block.pling block @s[scores={ra.u.snd=1..}] ~ ~ ~ 0.5 1.5
