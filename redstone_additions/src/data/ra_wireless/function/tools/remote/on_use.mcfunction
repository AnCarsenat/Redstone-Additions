# /ra_wireless:tools/remote/on_use
# Handle Redstone Remote use. As player (from advancement).

# Revoke advancement immediately
advancement revoke @s only ra_wireless:tools/remote_use

# Mark that player clicked this tick
tag @s add ra.remote_clicked

# If player is already holding (has tag), skip execution
execute if entity @s[tag=ra.remote_active] run return fail

# Mark player as actively using this tool
tag @s add ra.remote_active

# Check if sneaking (change channel) or not (toggle)
execute if predicate ra:is_sneaking run function ra_wireless:tools/remote/change_channel
execute unless predicate ra:is_sneaking run function ra_wireless:tools/remote/toggle_receivers
