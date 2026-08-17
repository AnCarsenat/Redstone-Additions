# /ra:tools/data_handler/props/toggle {name}
# Internal: flip a byte property on the selected block.
#
# The old value is latched into a tag before the first write. Testing the property
# again on the next line would read what that write just stored, so the pair undid
# itself and [Toggle] appeared to do nothing.

tag @s remove ra.dh.was_set
$execute if data entity @e[type=marker,tag=ra.dh_target,limit=1] {data:{properties:{$(name):1b}}} run tag @s add ra.dh.was_set
$execute if entity @s[tag=ra.dh.was_set] run data modify entity @e[type=marker,tag=ra.dh_target,limit=1] data.properties.$(name) set value 0b
$execute unless entity @s[tag=ra.dh.was_set] run data modify entity @e[type=marker,tag=ra.dh_target,limit=1] data.properties.$(name) set value 1b
tag @s remove ra.dh.was_set
