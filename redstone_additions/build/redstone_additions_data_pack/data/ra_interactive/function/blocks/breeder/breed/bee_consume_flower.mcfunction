# /ra_interactive:blocks/breeder/breed/bee_consume_flower
# Consume one flower from the breeder for bee breeding
# Context: positioned at breeder block

# Check each flower type, set bee into love mode, and consume the flower
execute if items block ~ ~ ~ container.* dandelion positioned ^ ^ ^1 as @e[type=bee,distance=..2,limit=1,nbt={Age:0,InLove:0}] run data modify entity @s InLove set value 600
execute if items block ~ ~ ~ container.* dandelion run data modify storage ra:temp breed_item set value "dandelion"
execute if items block ~ ~ ~ container.* dandelion run function ra_interactive:blocks/breeder/breed_success
execute if score @s ra.temp matches 1 run return 1

execute if items block ~ ~ ~ container.* poppy positioned ^ ^ ^1 as @e[type=bee,distance=..2,limit=1,nbt={Age:0,InLove:0}] run data modify entity @s InLove set value 600
execute if items block ~ ~ ~ container.* poppy run data modify storage ra:temp breed_item set value "poppy"
execute if items block ~ ~ ~ container.* poppy run function ra_interactive:blocks/breeder/breed_success
execute if score @s ra.temp matches 1 run return 1

execute if items block ~ ~ ~ container.* blue_orchid positioned ^ ^ ^1 as @e[type=bee,distance=..2,limit=1,nbt={Age:0,InLove:0}] run data modify entity @s InLove set value 600
execute if items block ~ ~ ~ container.* blue_orchid run data modify storage ra:temp breed_item set value "blue_orchid"
execute if items block ~ ~ ~ container.* blue_orchid run function ra_interactive:blocks/breeder/breed_success
execute if score @s ra.temp matches 1 run return 1

execute if items block ~ ~ ~ container.* allium positioned ^ ^ ^1 as @e[type=bee,distance=..2,limit=1,nbt={Age:0,InLove:0}] run data modify entity @s InLove set value 600
execute if items block ~ ~ ~ container.* allium run data modify storage ra:temp breed_item set value "allium"
execute if items block ~ ~ ~ container.* allium run function ra_interactive:blocks/breeder/breed_success
execute if score @s ra.temp matches 1 run return 1

execute if items block ~ ~ ~ container.* azure_bluet positioned ^ ^ ^1 as @e[type=bee,distance=..2,limit=1,nbt={Age:0,InLove:0}] run data modify entity @s InLove set value 600
execute if items block ~ ~ ~ container.* azure_bluet run data modify storage ra:temp breed_item set value "azure_bluet"
execute if items block ~ ~ ~ container.* azure_bluet run function ra_interactive:blocks/breeder/breed_success
execute if score @s ra.temp matches 1 run return 1

execute if items block ~ ~ ~ container.* red_tulip positioned ^ ^ ^1 as @e[type=bee,distance=..2,limit=1,nbt={Age:0,InLove:0}] run data modify entity @s InLove set value 600
execute if items block ~ ~ ~ container.* red_tulip run data modify storage ra:temp breed_item set value "red_tulip"
execute if items block ~ ~ ~ container.* red_tulip run function ra_interactive:blocks/breeder/breed_success
execute if score @s ra.temp matches 1 run return 1

execute if items block ~ ~ ~ container.* orange_tulip positioned ^ ^ ^1 as @e[type=bee,distance=..2,limit=1,nbt={Age:0,InLove:0}] run data modify entity @s InLove set value 600
execute if items block ~ ~ ~ container.* orange_tulip run data modify storage ra:temp breed_item set value "orange_tulip"
execute if items block ~ ~ ~ container.* orange_tulip run function ra_interactive:blocks/breeder/breed_success
execute if score @s ra.temp matches 1 run return 1

execute if items block ~ ~ ~ container.* white_tulip positioned ^ ^ ^1 as @e[type=bee,distance=..2,limit=1,nbt={Age:0,InLove:0}] run data modify entity @s InLove set value 600
execute if items block ~ ~ ~ container.* white_tulip run data modify storage ra:temp breed_item set value "white_tulip"
execute if items block ~ ~ ~ container.* white_tulip run function ra_interactive:blocks/breeder/breed_success
execute if score @s ra.temp matches 1 run return 1

execute if items block ~ ~ ~ container.* pink_tulip positioned ^ ^ ^1 as @e[type=bee,distance=..2,limit=1,nbt={Age:0,InLove:0}] run data modify entity @s InLove set value 600
execute if items block ~ ~ ~ container.* pink_tulip run data modify storage ra:temp breed_item set value "pink_tulip"
execute if items block ~ ~ ~ container.* pink_tulip run function ra_interactive:blocks/breeder/breed_success
execute if score @s ra.temp matches 1 run return 1

execute if items block ~ ~ ~ container.* oxeye_daisy positioned ^ ^ ^1 as @e[type=bee,distance=..2,limit=1,nbt={Age:0,InLove:0}] run data modify entity @s InLove set value 600
execute if items block ~ ~ ~ container.* oxeye_daisy run data modify storage ra:temp breed_item set value "oxeye_daisy"
execute if items block ~ ~ ~ container.* oxeye_daisy run function ra_interactive:blocks/breeder/breed_success
execute if score @s ra.temp matches 1 run return 1

execute if items block ~ ~ ~ container.* cornflower positioned ^ ^ ^1 as @e[type=bee,distance=..2,limit=1,nbt={Age:0,InLove:0}] run data modify entity @s InLove set value 600
execute if items block ~ ~ ~ container.* cornflower run data modify storage ra:temp breed_item set value "cornflower"
execute if items block ~ ~ ~ container.* cornflower run function ra_interactive:blocks/breeder/breed_success
execute if score @s ra.temp matches 1 run return 1

execute if items block ~ ~ ~ container.* lily_of_the_valley positioned ^ ^ ^1 as @e[type=bee,distance=..2,limit=1,nbt={Age:0,InLove:0}] run data modify entity @s InLove set value 600
execute if items block ~ ~ ~ container.* lily_of_the_valley run data modify storage ra:temp breed_item set value "lily_of_the_valley"
execute if items block ~ ~ ~ container.* lily_of_the_valley run function ra_interactive:blocks/breeder/breed_success
execute if score @s ra.temp matches 1 run return 1

execute if items block ~ ~ ~ container.* torchflower positioned ^ ^ ^1 as @e[type=bee,distance=..2,limit=1,nbt={Age:0,InLove:0}] run data modify entity @s InLove set value 600
execute if items block ~ ~ ~ container.* torchflower run data modify storage ra:temp breed_item set value "torchflower"
execute if items block ~ ~ ~ container.* torchflower run function ra_interactive:blocks/breeder/breed_success
