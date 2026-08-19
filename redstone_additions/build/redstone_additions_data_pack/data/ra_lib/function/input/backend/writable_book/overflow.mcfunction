# /ra_lib:input/backend/writable_book/overflow {req:<int>}
# The Input Form could not be held and fell on the floor. Context: as the player.
#
# WHY THIS EXISTS
# A book on the ground is how a player says "never mind" -- tick_scan_req watches
# for exactly that. But it is ALSO what the game does when /give has nowhere to
# put an item, and those two look identical from the outside. So an overflowing
# give was read as a deliberate cancel: the form appeared, hit the floor, and the
# request tore itself down in the same breath, which is precisely what "it gives
# me a book and instantly cancels" is.
#
# Caught here, at the moment of the give, rather than left for the drop watcher to
# misread a tick later. The form is taken back so nothing is left lying around,
# and the request ends with a message that says what to actually do about it.

$execute at @s as @e[type=item,distance=..8] if data entity @s {Item:{id:"minecraft:writable_book",components:{"minecraft:custom_data":{ra:{input_book:1b,input_req:$(req)}}}}} run kill @s

tellraw @s [{text:"[RA Input] ",color:"gold"},{text:"Your inventory is full — the Input Form had nowhere to go.",color:"red"}]
tellraw @s [{text:"  Free a slot and try again.",color:"gray"}]

function ra_lib:input/cancel
