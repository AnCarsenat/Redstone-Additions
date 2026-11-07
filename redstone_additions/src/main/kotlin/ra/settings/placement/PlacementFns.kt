package ra.settings.placement

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnValue
import ra.api.Settings
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Tag
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.settings.S
import ra.settings.returnUnlessS
import ra.settings.sCopy
import ra.settings.sRemove
import ra.settings.setTmp
import ra.settings.tmp

/** `ra_settings:placement/` functions: the disabled-block check and default seeding at placement. */
internal object PlacementFns {
	val checkStep = ref(Ns.SETTINGS, "placement/check_step")
	val checkOne = ref(Ns.SETTINGS, "placement/check_one")
	val seedAt = ref(Ns.SETTINGS, "placement/seed_at")

	fun Ra.registerPlacement() {
		// May the block this bat is carrying be placed? As the placement bat. Sets #blocked
		// ra.set.tmp to 1 when the type is switched off.
		//
		// Only DISABLED types are walked, and the list is empty on a pack nobody has configured --
		// so the common case is a single "is the list empty" test and no iteration at all. A flag
		// per block would instead cost one check per block type on every placement for ever.
		pack.define(Settings.Placement.check) {
			setTmp("#blocked", 0)
			returnUnlessS("disabled[0]")
			sCopy("dscan", "disabled")
			function(checkStep)
			sRemove("dscan")
		}

		// Walk the disabled list against this bat's placement tag.
		pack.define(checkStep) {
			returnUnlessS("dscan[0]")
			execute {
				ifCondition { score(tmp("#blocked"), Obj.SET_TMP) equalTo 1 }
				run { returnValue(0) }
			}
			function(checkOne, S, "dscan[0]")
			sRemove("dscan[0]")
			function(checkStep)
		}

		// `check_one {b}`: does this bat carry the routing tag of a disabled block?
		pack.define(checkOne) {
			raw("\$execute if entity @s[tag=${Tag.place("\$(b)")}] run scoreboard players set #blocked ${Obj.SET_TMP} 1")
		}

		// Tell the placer their block is switched off. As the placement bat, at the placement position.
		//
		// THE ITEM IS NOT RETURNED, THOUGH IT SHOULD BE. A spawn egg is consumed by vanilla before
		// the bat it spawns ever reaches this pack, and nothing here puts one back. A disabled
		// block is an admin's policy rather than a mistake by the player holding it, so it costing
		// them the item is wrong -- but doing it properly needs the item rebuilt from its block id,
		// and the bat carries only the ra.place.<id> tag, not the components.
		pack.define(Settings.Placement.refuse) {
			raw("""tellraw @a[tag=${Tag.PLACER},limit=1] [{text:"[Settings] ",color:"gold"},{text:"That block is disabled on this world.",color:"red"}]""")
			raw("playsound minecraft:block.note_block.bass block @a[tag=${Tag.PLACER},limit=1] ~ ~ ~ 0.6 0.7")
			// RUN, not suggest. This is aimed at whoever just tried to place the block, who is
			// usually not an operator -- a suggested /function is useless to somebody who cannot
			// run one. A trigger works for everybody and needs no confirmation dialog.
			raw("""tellraw @a[tag=${Tag.PLACER},limit=1] [{text:"  "},{text:"[ What else is disabled? ]",color:"yellow",hover_event:{action:"show_text",value:"List every block that cannot be placed here"},click_event:{action:"run_command",command:"/trigger ra.settings.open set 3"}}]""")
		}

		// Copy the configured defaults onto a freshly placed block. As the new marker.
		//
		// WHY HERE AND NOT IN ra_lib:util/property. property() is read by every consumer, every
		// bridge, every drain, on every tick, and it is four commands with no storage and no
		// sub-calls on purpose. Teaching it to consult a settings table would put that lookup in
		// the hottest path in the pack to answer a question that only changes when a block is
		// placed. Applying it at placement also gives "default" the meaning an admin expects: new
		// blocks come out with the configured value, and machines already standing in the world
		// are not silently re-tuned underneath a build balanced around them.
		pack.define(Settings.Placement.seed) {
			execute {
				unlessCondition { data(self(), "data.type") }
				run { returnValue(0) }
			}
			data(S) { modify("q.t") { set(self(), "data.type") } }
			function(seedAt, S, "q")
		}

		// `seed_at {t}`: merge, not set. The block's placement spec has already written whatever
		// properties it needs, and only the ones an admin has actually configured should be
		// replaced. A `set` would delete every property the settings page does not mention.
		pack.define(seedAt) {
			raw("\$execute if data storage ra:settings global.props.\"\$(t)\" run data modify entity @s data.properties merge from storage ra:settings global.props.\"\$(t)\"")
		}
	}
}
