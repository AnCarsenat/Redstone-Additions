# /ra:tools/data_handler/update_storage
# Update storage with current block data. As armor stand.

data modify storage ra:dh block_type set from entity @s data.block_type
data modify storage ra:dh properties set from entity @s data.properties
data modify storage ra:dh internal_data set from entity @s data.data
