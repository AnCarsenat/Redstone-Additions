# /ra_interactive:blocks/breeder/consume_item
# Consume one item from the dispenser. Macro function.
# Input: $(breed_item) = item ID to consume (without minecraft: prefix)

$function ra_lib:inventory/remove {id:"minecraft:$(breed_item)",count:1}
