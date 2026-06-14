# /ra:tools/input_data_handler/on_use
# Handle input data handler click. As player (from advancement).

advancement revoke @s only ra:tools/input_data_handler_use

tag @s add ra.input_handler_clicked
execute if entity @s[tag=ra.input_handler_active] run return fail

tag @s add ra.input_handler_active

# Match Creative Data Handler behavior: only open menu while sneaking.
execute unless predicate ra:is_sneaking run return 0
function ra:tools/input_data_handler/show_menu