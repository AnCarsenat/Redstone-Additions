# Current values on the Sensors page.
tellraw @s [{text:"─── ",color:"dark_gray"},{text:"Sensors",color:"gold",bold:true},{text:" ───",color:"dark_gray"}]
execute unless data storage ra:settings disabled[{b:"entity_detector"}] run tellraw @s [{text:"  Entity Detector: ",color:"white"},{text:"enabled",color:"green"}]
execute if data storage ra:settings disabled[{b:"entity_detector"}] run tellraw @s [{text:"  Entity Detector: ",color:"white"},{text:"disabled",color:"red"}]
execute unless data storage ra:settings disabled[{b:"tag_adder"}] run tellraw @s [{text:"  Tag Adder: ",color:"white"},{text:"enabled",color:"green"}]
execute if data storage ra:settings disabled[{b:"tag_adder"}] run tellraw @s [{text:"  Tag Adder: ",color:"white"},{text:"disabled",color:"red"}]
execute unless data storage ra:settings disabled[{b:"tag_remover"}] run tellraw @s [{text:"  Tag Remover: ",color:"white"},{text:"enabled",color:"green"}]
execute if data storage ra:settings disabled[{b:"tag_remover"}] run tellraw @s [{text:"  Tag Remover: ",color:"white"},{text:"disabled",color:"red"}]
