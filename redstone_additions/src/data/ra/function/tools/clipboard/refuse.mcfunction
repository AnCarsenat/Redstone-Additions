# /ra:tools/clipboard/refuse
# Internal: say why the paste did not happen, naming both kinds.

tellraw @a[tag=ra.clip_active,limit=1] [{text:"[Clipboard] ",color:"gold"},{text:"Origin is a ",color:"gray"},{nbt:"clip.kind",storage:"ra:temp",color:"yellow"},{text:" — that is a ",color:"gray"},{nbt:"block_name",storage:"ra:temp",color:"yellow"},{text:".",color:"gray"}]
playsound minecraft:block.note_block.bass block @a[tag=ra.clip_active,limit=1,scores={ra.u.snd=1..}] ~ ~ ~ 0.7 0.7
data remove storage ra:temp clip
data remove storage ra:temp clip_ok
