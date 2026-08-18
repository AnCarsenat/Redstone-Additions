# /ra_interactive:blocks/breeder/try_breed
# Try to breed animals at current position (in front of breeder)
# Positioned at target area (^ ^ ^1 from breeder)

# Reset success flag
scoreboard players set @s ra.temp 0

# ONE CHECK BEFORE THIRTY-FIVE
# What follows is a line per (animal, food) pair, and every one of them runs an
# `if items ... container.*` against the same container before the first can
# match. That is thirty-five commands per powered breeder per tick, paid in full
# whenever nothing matches -- which is nearly always, because animals wander off,
# and the ones that are there spend most of their time either already in love or
# still on their breeding cooldown.
#
# The pairs below all end up asking the same question first: is there an animal
# in front of me that could breed right now? Asking it once, with an entity type
# tag, costs one command and skips the other thirty-five outright. It repeats the
# Age/InLove test each breed/* function does for itself, which is the point --
# those tests were the reason the chain ran to the end and found nothing.
#
# Measured from the breeder itself, with the same radius and the same NBT test
# the breed/* functions use. Note that they are NOT measured from the `^ ^ ^-1`
# the lines below use: each breed/* opens with `positioned ^ ^ ^1`, which undoes
# it exactly, so the animal search happens back at this block. Putting the gate a
# block behind would test empty air and stop every breeder working.
execute unless entity @e[type=#ra_interactive:breedable,distance=..2,limit=1,nbt={Age:0,InLove:0}] run return 0

# Try each animal type - functions check for food and breed if possible
# Cows/Mooshrooms - Wheat
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 if items block ~ ~ ~ container.* wheat run function ra_interactive:blocks/breeder/breed/cow
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 if items block ~ ~ ~ container.* wheat run function ra_interactive:blocks/breeder/breed/mooshroom

# Sheep - Wheat
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 if items block ~ ~ ~ container.* wheat run function ra_interactive:blocks/breeder/breed/sheep

# Pigs - Carrots, Potatoes, or Beetroot
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 if items block ~ ~ ~ container.* carrot run function ra_interactive:blocks/breeder/breed/pig_carrot
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 if items block ~ ~ ~ container.* potato run function ra_interactive:blocks/breeder/breed/pig_potato
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 if items block ~ ~ ~ container.* beetroot run function ra_interactive:blocks/breeder/breed/pig_beetroot

# Chickens - Seeds (wheat, melon, pumpkin, beetroot, torchflower, pitcher pod)
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 if items block ~ ~ ~ container.* wheat_seeds run function ra_interactive:blocks/breeder/breed/chicken_wheat_seeds
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 if items block ~ ~ ~ container.* melon_seeds run function ra_interactive:blocks/breeder/breed/chicken_melon_seeds
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 if items block ~ ~ ~ container.* pumpkin_seeds run function ra_interactive:blocks/breeder/breed/chicken_pumpkin_seeds
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 if items block ~ ~ ~ container.* beetroot_seeds run function ra_interactive:blocks/breeder/breed/chicken_beetroot_seeds
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 if items block ~ ~ ~ container.* torchflower_seeds run function ra_interactive:blocks/breeder/breed/chicken_torchflower_seeds
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 if items block ~ ~ ~ container.* pitcher_pod run function ra_interactive:blocks/breeder/breed/chicken_pitcher_pod

# Goats - Wheat
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 if items block ~ ~ ~ container.* wheat run function ra_interactive:blocks/breeder/breed/goat

# Rabbits - Carrots, Golden Carrots, or Dandelions
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 if items block ~ ~ ~ container.* carrot run function ra_interactive:blocks/breeder/breed/rabbit_carrot
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 if items block ~ ~ ~ container.* golden_carrot run function ra_interactive:blocks/breeder/breed/rabbit_golden_carrot
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 if items block ~ ~ ~ container.* dandelion run function ra_interactive:blocks/breeder/breed/rabbit_dandelion

# Horses/Donkeys - Golden Apples or Golden Carrots
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 if items block ~ ~ ~ container.* golden_apple run function ra_interactive:blocks/breeder/breed/horse_golden_apple
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 if items block ~ ~ ~ container.* golden_carrot run function ra_interactive:blocks/breeder/breed/horse_golden_carrot
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 if items block ~ ~ ~ container.* enchanted_golden_apple run function ra_interactive:blocks/breeder/breed/horse_enchanted_golden_apple

# Llamas - Hay Bale
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 if items block ~ ~ ~ container.* hay_block run function ra_interactive:blocks/breeder/breed/llama

# Turtles - Seagrass
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 if items block ~ ~ ~ container.* seagrass run function ra_interactive:blocks/breeder/breed/turtle

# Pandas - Bamboo
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 if items block ~ ~ ~ container.* bamboo run function ra_interactive:blocks/breeder/breed/panda

# Foxes - Sweet Berries or Glow Berries
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 if items block ~ ~ ~ container.* sweet_berries run function ra_interactive:blocks/breeder/breed/fox_sweet_berries
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 if items block ~ ~ ~ container.* glow_berries run function ra_interactive:blocks/breeder/breed/fox_glow_berries

# Bees - Flowers (check common ones)
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 run function ra_interactive:blocks/breeder/breed/bee

# Wolves - Any meat (raw/cooked) - check is done inside the function
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 run function ra_interactive:blocks/breeder/breed/wolf

# Cats - Raw Cod or Raw Salmon
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 if items block ~ ~ ~ container.* cod run function ra_interactive:blocks/breeder/breed/cat_cod
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 if items block ~ ~ ~ container.* salmon run function ra_interactive:blocks/breeder/breed/cat_salmon

# Axolotls - Bucket of Tropical Fish
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 if items block ~ ~ ~ container.* tropical_fish_bucket run function ra_interactive:blocks/breeder/breed/axolotl

# Striders - Warped Fungus
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 if items block ~ ~ ~ container.* warped_fungus run function ra_interactive:blocks/breeder/breed/strider

# Hoglins - Crimson Fungus
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 if items block ~ ~ ~ container.* crimson_fungus run function ra_interactive:blocks/breeder/breed/hoglin

# Camels - Cactus
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 if items block ~ ~ ~ container.* cactus run function ra_interactive:blocks/breeder/breed/camel

# Sniffers - Torchflower Seeds
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 if items block ~ ~ ~ container.* torchflower_seeds run function ra_interactive:blocks/breeder/breed/sniffer

# Frogs - Slimeballs
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 if items block ~ ~ ~ container.* slime_ball run function ra_interactive:blocks/breeder/breed/frog

# Armadillos - Spider Eye
execute if score @s ra.temp matches 0 positioned ^ ^ ^-1 if items block ~ ~ ~ container.* spider_eye run function ra_interactive:blocks/breeder/breed/armadillo
