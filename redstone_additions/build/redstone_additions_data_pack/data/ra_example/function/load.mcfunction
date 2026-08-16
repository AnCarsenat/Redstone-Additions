# /ra_example:load
# Example multiblocks loaded
#
# TEMPLATE — intentionally not registered.
# Nothing calls ra_example:load or ra_example:tick, and none of the example
# multiblocks appear in the #ra_lib_multiblock:* tags, so this namespace never
# runs in game. It exists as the worked example behind docs/extension-examples.md.
# To make it live: call this from ra:load and ra_example:tick from ra:tick, and
# add the example's five hooks to the #ra_lib_multiblock tags.

function ra_multiblock:load

tellraw @a [{text:"[",color:"dark_gray"},{text:"RA",color:"gold",bold:true},{text:"] ",color:"dark_gray"},{text:"Example multiblocks loaded!",color:"aqua"}]
