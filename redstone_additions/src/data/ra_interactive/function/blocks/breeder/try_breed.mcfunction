# /ra_interactive:blocks/breeder/try_breed
# Try to breed animals at current position (in front of breeder)
# Positioned at target area (^ ^ ^1 from breeder)

# Reset success flag
scoreboard players set @s ra.temp 0

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
