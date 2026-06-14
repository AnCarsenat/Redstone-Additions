# /ra_storage:storage_box/make_preview_from_input
# MACRO INPUT: {id:"minecraft:...",count:N}

$data modify storage ra:temp storage_box.input_preview set value {text:"$(id) x$(count)",color:"gray",italic:false}
