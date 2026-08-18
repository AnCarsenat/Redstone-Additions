# /ra_wires:electric/adopt
# Enrol an electric node that is not on a transport network yet.
# Context: as the node marker.
#
# Two callers in practice. New blocks are joined at placement by
# ra_wires:blocks/place_join, so they never reach here. Everything placed before
# electric moved onto the network engine does — those markers carry
# ra.wires.electric_node but no ra.tr.node, and would otherwise sit inert on a
# grid that cannot see them.
#
# Capacity is the node's contribution to its network's total, and the numbers
# match the specs in ra_wires:blocks/handle_placement.
#
# Wires, switches and the vault contribute NOTHING. A grid stores what its
# batteries store and no more, so a run of wire is not secretly a tank: with no
# battery on it, whatever a generator makes has to be spent on the same tick or
# it is gone. Generators, consumers and panels keep 50 only so they can function
# standing alone.

execute if entity @s[tag=ra.custom_block.electric_wire] run function ra_lib:transport/net/join {class:"electric",capacity:0}
execute if entity @s[tag=ra.custom_block.electric_generator] run function ra_lib:transport/net/join {class:"electric",capacity:50}
execute if entity @s[tag=ra.custom_block.electric_consumer] run function ra_lib:transport/net/join {class:"electric",capacity:50}
execute if entity @s[tag=ra.custom_block.solar_panel] run function ra_lib:transport/net/join {class:"electric",capacity:50}
execute if entity @s[tag=ra.custom_block.electric_switch] run function ra_lib:transport/net/join {class:"electric",capacity:0}
execute if entity @s[tag=ra.custom_block.battery] run function ra_lib:transport/net/join {class:"electric",capacity:10000}
execute if entity @s[tag=ra.custom_block.ender_power_vault] run function ra_lib:transport/net/join {class:"electric",capacity:0}

# The per-node buffer is dead weight now: the charge lives on the network. Left
# behind it would show up in the Data Handler as an editable field that changes
# nothing.
# transfer_rate steered the old per-node handover and steers nothing now -- except
# on the Ender Power Vault, where it is still the rate its channel moves EU at.
execute unless entity @s[tag=ra.custom_block.ender_power_vault] run data remove entity @s data.properties.transfer_rate
data remove entity @s data.data.eu
data remove entity @s data.data.capacity
