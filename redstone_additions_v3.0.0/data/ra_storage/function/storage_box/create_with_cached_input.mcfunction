# /ra_storage:storage_box/create_with_cached_input
# Build a new storage box item stack in storage ra:temp storage_box.generated_item.
# Uses cached input at storage ra:temp storage_box.input_item and input_preview.

data modify storage ra:temp storage_box.target_box set value {id:"minecraft:player_head",count:1,components:{"minecraft:item_name":"Storage Box","minecraft:profile":"BoxMan01234","minecraft:max_stack_size":64,"minecraft:lore":[{text:"(empty)",color:"gray",italic:false}],"minecraft:custom_data":{ra:{item_box:1b,storage_box:{items:[],preview:[]}}}}}

data modify storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items append from storage ra:temp storage_box.input_item
data modify storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.preview append from storage ra:temp storage_box.input_preview
function ra_storage:storage_box/update_lore_storage_target

data modify storage ra:temp storage_box.generated_item set from storage ra:temp storage_box.target_box
return 1
