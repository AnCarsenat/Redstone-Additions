# /ra:tools/clipboard/on_use
# Handle Clipboard use. As player (from advancement).
#
# EVERYTHING IS SHIFT+RMB, ON PURPOSE
# A plain right-click cannot be used here. Most of the blocks worth configuring
# are backed by a container -- barrels, dispensers, droppers, furnaces -- and a
# plain click opens their GUI. Binding anything to it would mean a tool that
# sometimes configures a block and sometimes opens a chest, depending on what the
# block happens to be made of. Sneaking suppresses block interaction, so shift is
# the only click that reliably belongs to the tool.
#
# THE MODEL
# The first block you shift-click becomes the origin. Every block you shift-click
# after that is given the origin's settings, as long as it is the same kind. So
# configuring twenty drains is: set one up, shift-click it, then shift-click the
# other nineteen. Shift-click at nothing to clear and start again.

advancement revoke @s only ra:tools/clipboard_use

tag @s add ra.clip_clicked
execute if entity @s[tag=ra.clip_active] run return fail
tag @s add ra.clip_active

# Not sneaking: the player is interacting with a block, not using the tool.
execute unless predicate ra:is_sneaking run return 0

function ra:tools/clipboard/ensure_id

# Is the clipboard loaded? That, not the click, decides copy versus apply.
execute store result storage ra:temp clip.slot int 1 run scoreboard players get @s ra.clip.id
data remove storage ra:temp clip.kind
function ra:tools/clipboard/load with storage ra:temp clip

data remove storage ra:temp clip_found
execute if data storage ra:temp clip.kind run function ra:tools/clipboard/raycast_quiet {action:"paste"}
execute unless data storage ra:temp clip.kind run function ra:tools/clipboard/raycast_quiet {action:"copy"}

# Nothing in front of you is the deliberate way to empty it. Clearing on a miss
# rather than on a hit means a slightly-off click can never wipe a working origin.
execute unless data storage ra:temp clip_found run function ra:tools/clipboard/clear
data remove storage ra:temp clip_found
