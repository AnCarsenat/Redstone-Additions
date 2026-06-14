# /ra_sensors:blocks/entity_detector/detect
# Detect entities in range. As armor stand, at position.

# Get entity selector and store for macro
data modify storage ra:temp entity_selector set from entity @s data.properties.entity_selector

# Use macro to check for entity selector
function ra_sensors:blocks/entity_detector/detect_macro with storage ra:temp
