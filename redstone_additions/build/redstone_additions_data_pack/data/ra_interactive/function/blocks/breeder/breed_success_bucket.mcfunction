# /ra_interactive:blocks/breeder/breed_success_bucket
# Called when breeding with bucket is successful. Replaces bucket with empty bucket.

# Play breeding sound and particles
playsound minecraft:entity.experience_orb.pickup block @a[distance=..8] ^ ^ ^1 0.5 1.2
particle minecraft:heart ^ ^ ^1 0.5 0.5 0.5 0.02 5

# Replace tropical fish bucket with water bucket
data modify block ~ ~ ~ Items[{id:"minecraft:tropical_fish_bucket"}].id set value "minecraft:water_bucket"

# Mark success
scoreboard players set @s ra.temp 1
