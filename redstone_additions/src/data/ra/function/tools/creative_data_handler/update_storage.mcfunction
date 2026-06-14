# /ra:tools/creative_data_handler/update_storage
# Update storage with current block data. As armor stand.

data modify storage ra:cdh block_type set from entity @s data.block_type
data modify storage ra:cdh properties set from entity @s data.properties
data modify storage ra:cdh internal_data set from entity @s data.data
