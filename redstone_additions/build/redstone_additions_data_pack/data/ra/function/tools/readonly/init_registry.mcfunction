# /ra:tools/readonly/init_registry
# Which properties a block owns and the player may not edit.
#
# WHAT READ-ONLY MEANS HERE
# Some properties are the block's own working numbers rather than settings: a
# generator's generation_rate, a valve's transfer rate, a breaker's cooldown. The
# block writes them, reads them, and depends on them being sane. Letting a player
# type into them produces machines that are quietly broken in ways no error
# message explains.
#
# Marking one read-only does two things at once:
#   - the Data Handler still SHOWS it, with a struck-through [Modify] and a
#     reason on hover, because hiding a value the goggles display anyway is just
#     confusing
#   - the wrench never offers it in the cycle menu
#
# ONE DECLARATION, NOT ONE PER MODULE
# This used to be five separate `tools/hidden_fields.mcfunction` files, each a
# chain of `if entity @s[tag=ra.custom_block.X]`, reached through a function tag
# -- which is exactly the per-block-table shape the Data Handler registry was
# consolidated to get rid of. Now it is one map keyed by the block type markers
# carry in data.type, the same key the wrench registry uses.
#
# Stored as compounds rather than lists because both readers ask "is THIS name
# read-only?", and `if data storage ... ."$(name)"` answers that in one command
# where a list would need a walk.

data modify storage ra:dh readonly set value {"block_breaker":{cooldown:1b},"block_placer":{cooldown:1b},"electric_breaker":{rate:1b},"electric_consumer":{eu_use:1b},"electric_generator":{generation_rate:1b},"ender_fluid_vault":{transfer_rate:1b},"ender_power_vault":{transfer_rate:1b},"gas_valve":{rate:1b},"industrial_light":{eu_use:1b},"liquid_valve":{rate:1b},"mineral_generator":{cooldown:1b},"multiblock_base":{tier:1b,tier_level:1b},"nether_generator":{cooldown:1b},"poppy_generator":{cooldown:1b},"solar_panel":{generation_rate:1b}}
