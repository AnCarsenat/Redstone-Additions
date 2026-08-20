# /ra_interactive:blocks/big_torch/skin
# Draw the oversized torch over the end rod, or repair it if it went missing.
# Context: as the torch marker, at the block position.
#
# WHY NOT ra_lib:skin/apply
# That draws one full block over another at a fixed 1.02, which is exactly right
# for a barrel wearing a dispenser and no use here: a torch is not a full block
# and the whole point is that it is bigger than the one vanilla draws. The scale
# below is worked out from the two models rather than borrowed.
#
# THE NUMBERS
#   torch model    x,z 7..9   y 0..10   -> half-width 0.0625, height 0.625
#   end rod model  x,z 6..10  y 0..16   -> half-width 0.125,  height 1.0
#
# The torch has to ENCLOSE the end rod, not sit inside it. Two surfaces that
# share a plane z-fight, and the end rod's shaft is exactly as wide as a torch --
# drawn at 1:1 they would be the same 2x2 pixels and fight the whole way up.
#
# Height: 1.0 plus the pack's usual 0.01 of clearance per face is 1.02, so the
# scale is 1.02 / 0.625 = 1.632. The torch is therefore one block tall to look at
# and a hundredth over it in fact, which is the same margin ra_lib:skin/spawn
# uses and for the same reason -- two thousandths reads as coplanar to the depth
# buffer a few chunks out and flickers.
#
# Width: the end rod is 0.125 out from the middle, so the torch needs 0.135, and
# 0.135 / 0.0625 = 2.16. Rounded to 2.2 for a little more room. The end rod ends
# up entirely inside the torch, which is why none of it pokes through.
#
# translation centres the scaled model: -0.5 * scale on x and z, and on y the
# bottom is dropped to the block floor less the same clearance.

# Anchored to the block centre, which is where skin_spawn stands the display. The
# marker is nominally there too, but "nominally" is what went wrong with the item
# pipe's filter display: it was summoned 0.9 above the marker and looked for
# again within 0.6, so it never found its own and stacked a new one on every
# pass. Both ends of an ownership test have to agree on the anchor.
execute align xyz positioned ~0.5 ~0.5 ~0.5 if entity @e[type=block_display,tag=ra.skin.big_torch,distance=..0.4] run return 0

# Summoned straight from here rather than through a macro function. There is
# nothing to substitute: a torch is a light source and is drawn at full
# brightness whatever the light around it, so there is no sampled value to pass.
# It WAS a macro, with a `$` line carrying no `$(...)` in it at all, and Minecraft
# rejects that outright -- "No variables in macro" -- so the function failed to
# load and no torch was ever drawn. tools/lint_macros.py now fails the build on
# that rather than leaving it to be found in game.
#
# Centre-anchored, the same place the ownership test above looks, so the two
# agree about which display belongs to this block.
#
# Lit as a torch rather than sampled from the world: this IS the light, and a
# display that dimmed at night would be the one thing in the build that looked
# switched off.
execute align xyz positioned ~0.5 ~0.5 ~0.5 run summon block_display ~ ~ ~ {Tags:["ra","ra.display","ra.skin","ra.skin.big_torch"],brightness:{sky:15,block:15},block_state:{Name:"minecraft:torch"},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-1.1f,-0.51f,-1.1f],scale:[2.2f,1.632f,2.2f]}}
