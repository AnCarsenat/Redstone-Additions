# /ra:tools/wrench/init_registry
# Which properties each block can cycle, and what cycles them.
#
# ONE LIST, NOT A BRANCH PER BLOCK
# The wrench used to be a chain of `if entity @s[tag=ra.custom_block.X] run
# return run function <that block's cycler>`, which meant one cycler per block
# and no way to offer two. Blocks that wanted a second setting had to borrow the
# goggles, which is how the Electric Furnace ended up with its output on one tool
# and its power mode on another.
#
# Now a block declares a LIST of cyclable properties and the wrench works out
# what to do: nothing declared and it says so, one declared and it cycles that
# one immediately, more than one and it opens a menu. Adding a setting is an edit
# here, and nothing else changes.
#
# Keyed by the block type, which markers carry in data.type as of 5.1.9 -- a
# data pack cannot ask an entity which of its tags is the interesting one, so the
# type has to be written down at placement. See ra_migrations for old worlds.
#
# Fields: label shown in the menu, prop read for the current value, fn to run.
#
# There is no `enabled` entry. Every RA Wires block used to carry one, which put
# an "Enabled" row on blocks that had nothing else to configure and turned a
# one-setting block into a menu. A second off switch next to redstone is not a
# setting anyone reaches for -- breaking the wire does the same thing and reads
# better -- so the property is gone from the module entirely, and the EU Switch,
# whose only control it was, runs on redstone now.

data modify storage ra:wrench cyclables set value {"creative_fluid":[{label:"Medium",prop:"medium",fn:"ra_wires:blocks/creative_fluid/cycle_medium"}],"electric_consumer":[{label:"Rate",prop:"cooldown",fn:"ra_wires:blocks/electric_consumer/cycle_rate"}],"electric_furnace":[{label:"Output",prop:"output",fn:"ra_wires:blocks/electric_furnace/cycle_output"},{label:"Power",prop:"mode",fn:"ra_wires:blocks/electric_furnace/cycle_mode"}],"ender_fluid_vault":[{label:"Mode",prop:"mode",fn:"ra_ender:blocks/fluid_vault/cycle_mode"}],"ender_item_vault":[{label:"Mode",prop:"mode",fn:"ra_ender:blocks/item_vault/cycle_mode"}],"ender_power_vault":[{label:"Mode",prop:"mode",fn:"ra_ender:blocks/power_vault/cycle_mode"}],"liquid_drain":[{label:"Mode",prop:"mode",fn:"ra_wires:blocks/liquid_drain/cycle_mode"},{label:"Rate",prop:"cooldown",fn:"ra_wires:blocks/liquid_drain/cycle_rate"}],"uni_gate":[{label:"Gate type",prop:"gate",fn:"ra:tools/wrench/cycle_uni_gate"}]}
