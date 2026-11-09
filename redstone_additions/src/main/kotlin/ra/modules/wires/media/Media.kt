package ra.modules.wires.media

import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnValue
import ra.api.RaLib
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.define
import ra.core.executeReturnRun
import ra.core.raw
import ra.core.ref

object Media : PackPart {
	val init = ref(Ns.WIRES, "media/init")
	val label = ref(Ns.WIRES, "media/label")
	val labelRun = ref(Ns.WIRES, "media/label_run")
	val potions = ref(Ns.WIRES, "media/potions")
	val stateIs = ref(Ns.WIRES, "media/state_is")

	override fun Ra.register() {
		pack.define(Media.init) {
			// The medium registry. Media are named with strings.
			//
			// This replaces the old numeric medium_id scheme, where water was 1, lava 2,
			// powder snow 5, and gases were the same numbers with 10 added — so a gas pump
			// storing 11 meant "steam" only because 11 - 10 = 1 = water. Nothing in the data
			// said so, the mapping lived in a chain of `if score ... matches N` in
			// update_medium_label, and adding a medium meant editing every one of those
			// chains. Now a medium is its own name and everything about it is in one place.
			//
			// Fields:
			// name      display name for goggles and chat
			// state     "liquid" or "gas" — tanks of the wrong state refuse it
			// color     text colour for status lines
			// particle  particle shown by drains and placers handling it
			// drip      optional falling particle a Liquid Drain shows while it takes the medium in
			//           (falls back to `particle`)
			// block     world block this medium places and drains as, when it has one
			// pour      particle (with its options) a powered Liquid Drain pours a medium with no
			//           block, orb or effect out as: milk as white dust, steam as cloud, and so on
			// bucket    bucket item, when it has one

			raw("""data modify storage ra:wires media set value {water:{name:"Water",state:"liquid",color:"aqua",particle:"minecraft:splash",drip:"minecraft:falling_water",block:"minecraft:water",bucket:"minecraft:water_bucket"},lava:{name:"Lava",state:"liquid",color:"gold",particle:"minecraft:lava",drip:"minecraft:falling_lava",block:"minecraft:lava",bucket:"minecraft:lava_bucket"},powder_snow:{name:"Powder Snow",state:"liquid",color:"white",particle:"minecraft:snowflake",block:"minecraft:powder_snow",bucket:"minecraft:powder_snow_bucket"},milk:{name:"Milk",state:"liquid",color:"white",particle:"minecraft:item_slime",pour:"minecraft:dust{color:16777215,scale:1.5f}",bucket:"minecraft:milk_bucket"},experience:{name:"Experience",state:"liquid",color:"green",particle:"minecraft:glow",orb:1b},potion:{name:"Potion",state:"liquid",color:"light_purple",particle:"minecraft:effect",effect:1b},steam:{name:"Steam",state:"gas",color:"gray",particle:"minecraft:cloud",pour:"minecraft:cloud"},smoke:{name:"Smoke",state:"gas",color:"dark_gray",particle:"minecraft:smoke",pour:"minecraft:large_smoke"},oxygen:{name:"Oxygen",state:"gas",color:"blue",particle:"minecraft:bubble",pour:"minecraft:dust{color:11068671,scale:1.5f}"}}""")

			// Which world blocks a pump or drain can pick a medium up from. Kept separate
			// from the registry above because several block states map to one medium, and
			// the order matters: the first entry that matches wins.
			// What a held container can be emptied into a vertical drain, and what is left
			// in the hand afterwards. Same shape as source_blocks, so adding a medium here is
			// a data edit rather than a code one -- which is how potions and experience will
			// arrive once a network can hold more than one medium at a time.
			raw("""data modify storage ra:wires item_sources set value [{item:"minecraft:water_bucket",medium:"water",volume:5000,empty:"minecraft:bucket"},{item:"minecraft:lava_bucket",medium:"lava",volume:5000,empty:"minecraft:bucket"},{item:"minecraft:powder_snow_bucket",medium:"powder_snow",volume:5000,empty:"minecraft:bucket"},{item:"minecraft:milk_bucket",medium:"milk",volume:5000,empty:"minecraft:bucket"},{item:"minecraft:experience_bottle",medium:"experience",volume:700,empty:"minecraft:glass_bottle"},{item:"minecraft:potion",medium:"potion",volume:1000,empty:"minecraft:glass_bottle"}]""")

			// What the EU Generator will burn, and for how many ticks. Same shape as the
			// other registries here, so a new fuel is a data edit.
			//
			// Solid fuels only. A lava bucket would leave an empty bucket to put somewhere,
			// and a generator that eats your bucket is a bug report waiting to happen.
			// `name` is what the goggles show. Without it the readout said "minecraft:coal",
			// which is the only place in the pack a player would have seen a registry ID.
			raw("""data modify storage ra:wires fuels set value [{item:"minecraft:coal_block",ticks:16000,name:"Coal Block"},{item:"minecraft:dried_kelp_block",ticks:4000,name:"Dried Kelp Block"},{item:"minecraft:blaze_rod",ticks:2400,name:"Blaze Rod"},{item:"minecraft:coal",ticks:1600,name:"Coal"},{item:"minecraft:charcoal",ticks:1600,name:"Charcoal"},{item:"minecraft:bamboo_mosaic",ticks:300,name:"Bamboo Mosaic"},{item:"minecraft:oak_planks",ticks:300,name:"Oak Planks"},{item:"minecraft:spruce_planks",ticks:300,name:"Spruce Planks"},{item:"minecraft:birch_planks",ticks:300,name:"Birch Planks"},{item:"minecraft:jungle_planks",ticks:300,name:"Jungle Planks"},{item:"minecraft:acacia_planks",ticks:300,name:"Acacia Planks"},{item:"minecraft:dark_oak_planks",ticks:300,name:"Dark Oak Planks"},{item:"minecraft:oak_log",ticks:300,name:"Oak Log"},{item:"minecraft:spruce_log",ticks:300,name:"Spruce Log"},{item:"minecraft:birch_log",ticks:300,name:"Birch Log"},{item:"minecraft:stick",ticks:100,name:"Stick"}]""")

			// The same fuels again, keyed by item id instead of listed.
			//
			// WHY BOTH
			// The list is walked when you want to ask "which of these is in there?", which is
			// what the generator used to do: sixteen questions, one per fuel, every time it
			// looked for something to burn. The map answers the opposite and much cheaper
			// question -- "is THIS thing a fuel?" -- in one lookup, which is what you want
			// when you are walking the two or three stacks that are actually in the barrel.
			//
			// A data pack cannot iterate a compound's keys, so neither form can replace the
			// other; they are the same data indexed two ways.
			raw("""data modify storage ra:wires fuel_map set value {"minecraft:coal_block":{ticks:16000,name:"Coal Block"},"minecraft:dried_kelp_block":{ticks:4000,name:"Dried Kelp Block"},"minecraft:blaze_rod":{ticks:2400,name:"Blaze Rod"},"minecraft:coal":{ticks:1600,name:"Coal"},"minecraft:charcoal":{ticks:1600,name:"Charcoal"},"minecraft:bamboo_mosaic":{ticks:300,name:"Bamboo Mosaic"},"minecraft:oak_planks":{ticks:300,name:"Oak Planks"},"minecraft:spruce_planks":{ticks:300,name:"Spruce Planks"},"minecraft:birch_planks":{ticks:300,name:"Birch Planks"},"minecraft:jungle_planks":{ticks:300,name:"Jungle Planks"},"minecraft:acacia_planks":{ticks:300,name:"Acacia Planks"},"minecraft:dark_oak_planks":{ticks:300,name:"Dark Oak Planks"},"minecraft:oak_log":{ticks:300,name:"Oak Log"},"minecraft:spruce_log":{ticks:300,name:"Spruce Log"},"minecraft:birch_log":{ticks:300,name:"Birch Log"},"minecraft:stick":{ticks:100,name:"Stick"}}""")

			// `state` is the medium's state, copied here so a scan can skip sources its pump cannot
			// lift without a lookup per entry. `keep:1b` marks a source that is never used up: a lit
			// campfire gives smoke for as long as it burns, and is left as it is (`drained` names it
			// anyway, since every entry must carry every field the probe's macro reads).
			raw("""data modify storage ra:wires source_blocks set value [{match:"minecraft:water[level=0]",medium:"water",state:"liquid",volume:5000,drained:"minecraft:air"},{match:"minecraft:lava[level=0]",medium:"lava",state:"liquid",volume:5000,drained:"minecraft:air"},{match:"minecraft:powder_snow",medium:"powder_snow",state:"liquid",volume:5000,drained:"minecraft:air"},{match:"minecraft:water_cauldron[level=3]",medium:"water",state:"liquid",volume:1667,drained:"minecraft:water_cauldron[level=2]"},{match:"minecraft:water_cauldron[level=2]",medium:"water",state:"liquid",volume:1667,drained:"minecraft:water_cauldron[level=1]"},{match:"minecraft:water_cauldron[level=1]",medium:"water",state:"liquid",volume:1666,drained:"minecraft:cauldron"},{match:"minecraft:lava_cauldron",medium:"lava",state:"liquid",volume:5000,drained:"minecraft:cauldron"},{match:"minecraft:campfire[lit=true]",medium:"smoke",state:"gas",volume:500,drained:"minecraft:campfire[lit=true]",keep:1b},{match:"minecraft:soul_campfire[lit=true]",medium:"smoke",state:"gas",volume:500,drained:"minecraft:soul_campfire[lit=true]",keep:1b}]""")
		}

		pack.define(Media.label) {
			// Write a readable medium name into this node's status, from its network.
			// Context: as a fluid node marker.
			//
			// A MIXED RUN IS "MULTIMEDIUM"
			// `medium` on the network is media[0] -- the medium that got there FIRST -- so a
			// run that started with water went on reporting "Water" after lava was pumped
			// into it, and the Amount line next to it was the total of both. That reads as
			// "the lava I just put in arrived as water".
			//
			// It does not list them either. A billboard is one short line read from across
			// the room, and "Water 5000 mL, Lava 5000 mL, Steam 2000 mL" is neither short nor
			// readable at that distance -- and it repeats the Amount line underneath it in
			// pieces. So the goggles say the run is mixed and stop there; the Multimeter
			// prints the breakdown, in chat, where there is room for a line per medium.

			function(RaLib.Transport.Net.read)

			raw("data modify entity @s data.status.medium set value \"Empty\"")
			raw("execute unless data storage ra:transport cur.medium run return 0")

			// One medium keeps the plain name, which is what the Data Handler and every
			// single-medium block expects to read back.
			executeReturnRun("unless data storage ra:transport cur.media[1]", Media.labelRun, "with storage ra:transport cur")

			raw("data modify entity @s data.status.medium set value \"Multimedium\"")
		}

		pack.define(Media.labelRun) {
			// Internal: look the display name up, falling back to the raw key so an
			// unregistered medium shows as itself instead of silently reading "Empty".

			raw("\$data modify entity @s data.status.medium set value \"\$(medium)\"")
			raw("\$execute if data storage ra:wires media.\$(medium).name run data modify entity @s data.status.medium set from storage ra:wires media.\$(medium).name")
		}

		pack.define(Media.potions) {
			// What each vanilla potion does, so a drained potion can be applied rather than
			// just poured away.
			//
			// WHY A TABLE AT ALL
			// A potion item carries `potion_contents`, but a PRESET potion carries only its
			// id -- {potion:"minecraft:strength"} -- and nothing that says strength lasts
			// three minutes at level I. Only a brewed-with-commands potion fills in
			// `custom_effects`. So the effects of the forty vanilla potions have to be
			// written down somewhere, and this is that somewhere. A potion that does carry
			// `custom_effects` is read straight off the item and never comes here.
			//
			// Durations are in TICKS, at the values the wiki lists for the drinkable potion.
			// Instant health and harm are given one tick, because an instant effect ignores
			// duration and a zero would be read as "no effect at all".
			//
			// The four potions with no effect -- water, mundane, thick, awkward -- are
			// deliberately absent. A drain pouring one out applies nothing, which is correct.

			raw("""data modify storage ra:wires potion_effects set value {"minecraft:night_vision":[{e:"minecraft:night_vision",d:3600,a:0}],"minecraft:long_night_vision":[{e:"minecraft:night_vision",d:9600,a:0}],"minecraft:invisibility":[{e:"minecraft:invisibility",d:3600,a:0}],"minecraft:long_invisibility":[{e:"minecraft:invisibility",d:9600,a:0}],"minecraft:leaping":[{e:"minecraft:jump_boost",d:3600,a:0}],"minecraft:long_leaping":[{e:"minecraft:jump_boost",d:9600,a:0}],"minecraft:strong_leaping":[{e:"minecraft:jump_boost",d:1800,a:1}],"minecraft:fire_resistance":[{e:"minecraft:fire_resistance",d:3600,a:0}],"minecraft:long_fire_resistance":[{e:"minecraft:fire_resistance",d:9600,a:0}],"minecraft:swiftness":[{e:"minecraft:speed",d:3600,a:0}],"minecraft:long_swiftness":[{e:"minecraft:speed",d:9600,a:0}],"minecraft:strong_swiftness":[{e:"minecraft:speed",d:1800,a:1}],"minecraft:slowness":[{e:"minecraft:slowness",d:1800,a:0}],"minecraft:long_slowness":[{e:"minecraft:slowness",d:4800,a:0}],"minecraft:strong_slowness":[{e:"minecraft:slowness",d:400,a:3}],"minecraft:water_breathing":[{e:"minecraft:water_breathing",d:3600,a:0}],"minecraft:long_water_breathing":[{e:"minecraft:water_breathing",d:9600,a:0}],"minecraft:healing":[{e:"minecraft:instant_health",d:1,a:0}],"minecraft:strong_healing":[{e:"minecraft:instant_health",d:1,a:1}],"minecraft:harming":[{e:"minecraft:instant_damage",d:1,a:0}],"minecraft:strong_harming":[{e:"minecraft:instant_damage",d:1,a:1}],"minecraft:poison":[{e:"minecraft:poison",d:900,a:0}],"minecraft:long_poison":[{e:"minecraft:poison",d:1800,a:0}],"minecraft:strong_poison":[{e:"minecraft:poison",d:420,a:1}],"minecraft:regeneration":[{e:"minecraft:regeneration",d:900,a:0}],"minecraft:long_regeneration":[{e:"minecraft:regeneration",d:1800,a:0}],"minecraft:strong_regeneration":[{e:"minecraft:regeneration",d:440,a:1}],"minecraft:strength":[{e:"minecraft:strength",d:3600,a:0}],"minecraft:long_strength":[{e:"minecraft:strength",d:9600,a:0}],"minecraft:strong_strength":[{e:"minecraft:strength",d:1800,a:1}],"minecraft:weakness":[{e:"minecraft:weakness",d:1800,a:0}],"minecraft:long_weakness":[{e:"minecraft:weakness",d:4800,a:0}],"minecraft:luck":[{e:"minecraft:luck",d:6000,a:0}],"minecraft:slow_falling":[{e:"minecraft:slow_falling",d:1800,a:0}],"minecraft:long_slow_falling":[{e:"minecraft:slow_falling",d:4800,a:0}],"minecraft:turtle_master":[{e:"minecraft:slowness",d:400,a:3},{e:"minecraft:resistance",d:400,a:2}],"minecraft:long_turtle_master":[{e:"minecraft:slowness",d:800,a:3},{e:"minecraft:resistance",d:800,a:2}],"minecraft:strong_turtle_master":[{e:"minecraft:slowness",d:400,a:5},{e:"minecraft:resistance",d:400,a:3}],"minecraft:wind_charged":[{e:"minecraft:wind_charged",d:3600,a:0}],"minecraft:weaving":[{e:"minecraft:weaving",d:3600,a:0}],"minecraft:oozing":[{e:"minecraft:oozing",d:3600,a:0}],"minecraft:infested":[{e:"minecraft:infested",d:3600,a:0}]}""")

			// Each effect's colour (vanilla's, as an RGB int), so a powered Liquid Drain pours a
			// potion out in the potion's own colour. See ra_wires:fluid/drain_potion_color.
			raw("""data modify storage ra:wires effect_colors set value {"minecraft:speed":3402751,"minecraft:slowness":9154528,"minecraft:strength":16762624,"minecraft:instant_health":16262179,"minecraft:instant_damage":11101546,"minecraft:jump_boost":16646020,"minecraft:regeneration":13458603,"minecraft:fire_resistance":16750848,"minecraft:water_breathing":10017472,"minecraft:invisibility":16185078,"minecraft:night_vision":12779366,"minecraft:weakness":4738376,"minecraft:poison":8889187,"minecraft:resistance":9520880,"minecraft:luck":5882118,"minecraft:slow_falling":15978425,"minecraft:wind_charged":12438015,"minecraft:weaving":7891290,"minecraft:oozing":10092451,"minecraft:infested":9214860}""")
		}

		pack.define(Media.stateIs) {
			// kept: beet contract (nothing in the pack calls it).
			// Returns 1 when the medium is registered with that state.
			// Used to keep gas out of liquid tanks and the other way round.

			raw("\$execute if data storage ra:wires media.\$(medium){state:\"\$(state)\"} run return 1")
			returnValue(0)
		}
	}
}
