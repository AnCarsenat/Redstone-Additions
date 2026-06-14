# /ra_interactive:blocks/breeder/breed_success
# Called when breeding is successful. Positioned at breeder, consumes item.
# Input: storage ra:temp breed_item = item id to consume

# Play breeding sound and particles
playsound minecraft:entity.experience_orb.pickup block @a[distance=..8] ^ ^ ^1 0.5 1.2
particle minecraft:heart ^ ^ ^1 0.5 0.5 0.5 0.02 5

# Find and consume one of the breeding item from the dispenser
function ra_interactive:blocks/breeder/consume_item with storage ra:temp

# Mark success
scoreboard players set @s ra.temp 1
