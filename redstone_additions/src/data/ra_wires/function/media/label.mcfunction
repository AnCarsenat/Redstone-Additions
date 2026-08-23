# /ra_wires:media/label
# Write a readable medium name into this node's status, from its network.
# Context: as a fluid node marker.
#
# A MIXED RUN IS "MULTIMEDIUM"
# `medium` on the network is media[0] -- the medium that got there FIRST -- so a
# run that started with water went on reporting "Water" after lava was pumped
# into it, and the Amount line next to it was the total of both. That reads as
# "the lava I just put in arrived as water".
#
# It does not list them either. A billboard is one short line read from across
# the room, and "Water 5000 mL, Lava 5000 mL, Steam 2000 mL" is neither short nor
# readable at that distance -- and it repeats the Amount line underneath it in
# pieces. So the goggles say the run is mixed and stop there; the Multimeter
# prints the breakdown, in chat, where there is room for a line per medium.

function ra_lib:transport/net/read

data modify entity @s data.status.medium set value "Empty"
execute unless data storage ra:transport cur.medium run return 0

# One medium keeps the plain name, which is what the Data Handler and every
# single-medium block expects to read back.
execute unless data storage ra:transport cur.media[1] run return run function ra_wires:media/label_run with storage ra:transport cur

data modify entity @s data.status.medium set value "Multimedium"
