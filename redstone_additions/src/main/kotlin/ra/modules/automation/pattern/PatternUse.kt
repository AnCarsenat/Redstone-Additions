package ra.modules.automation.pattern

import io.github.ayfri.kore.arguments.components.matchers.customData
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.features.advancements.advancement
import io.github.ayfri.kore.features.advancements.criteria
import io.github.ayfri.kore.features.advancements.rewards
import io.github.ayfri.kore.features.advancements.triggers.usingItem
import io.github.ayfri.kore.features.predicates.sub.itemStackPredicate
import io.github.ayfri.kore.features.predicates.sub.predicates
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.TextFormat
import ra.core.define
import ra.core.item.SComp
import ra.core.RA_CHAT_PREFIX
import ra.core.raw
import ra.core.ref
import ra.lib.BlockTags
import ra.modules.automation.pattern.Pattern.HELD
import ra.modules.automation.pattern.Pattern.STORE
import ra.modules.automation.pattern.Pattern.USER

/**
 * What right-clicking a [Pattern] does. The Pattern is an endless `consumable`, so a right-click
 * fires the `using_item` advancement `ra_automation:pattern/use` (every tick it is held down; only
 * the first tick of a press counts, [USED]). Nothing runs while nobody uses one.
 *
 * The look is walked from the eyes like the tools' ray: the first block the crosshair would stop
 * on decides.
 * - a custom block in [Pattern.targets]: its function takes the Pattern;
 * - a vanilla crafter: its nine slots are recorded (only reachable sneaking: a plain right-click
 *   opens the crafter);
 * - anything else, or nothing in reach: sneaking clears the Pattern, otherwise a hint.
 *
 * THE LORE. The grid is shown as item names. Each name is a translated component built from the
 * id (`item.minecraft.<path>`, falling back to `block.minecraft.<path>` through `fallback:"%s"`),
 * written into `storage ra_automation:pattern lore.c<n>` and read into the lore by `set_lore`
 * resolving an `nbt` component. The id reaches the one macro line only after `ra_lib:safe/check`
 * (format ID) passed it; anything else is shown as plain text copied from storage, never pasted.
 */
object PatternUse {
	private fun fn(path: String) = ref(Ns.AUTOMATION, "pattern/$path")

	private val onUse = fn("on_use")
	private val step = fn("ray")
	private val record = fn("record")
	private val clear = fn("clear")
	private val lore = fn("lore")
	private val cell = fn("lore_cell")
	private val cellName = fn("lore_cell_name")

	/** The game time the player's Pattern was last seen in use. */
	const val USED = "ra.auto.used"

	private const val T = Obj.TEMP
	private const val REACH_STEPS = 50
	private const val MODIFIER = "ra_automation:pattern/write"

	/** The set_lore of a recorded Pattern: three rows of three names, then the hints. */
	private val gridLore: String = buildList {
		for (row in 0 until 3) add((0 until 3).joinToString(",", """[{text:"",color:"gray",italic:false},""", "]") { col ->
			val cellRef = """{nbt:"lore.c${row * 3 + col}",storage:"$STORE",interpret:true}"""
			if (col == 0) cellRef else """{text:" | ",color:"dark_gray"},$cellRef"""
		})
		addAll(Pattern.hintLore().map(SComp::text))
	}.joinToString(",", "[", "]")

	private val blankLore = Pattern.blankLore().joinToString(",", "[", "]") { it.text() }

	private fun setLore(lines: String) =
		"""item modify entity @s weapon.mainhand {function:"minecraft:set_lore",entity:"this",mode:"replace_all",lore:$lines}"""

	fun Ra.registerPatternUse() {
		val useAdvancement = pack.advancement("pattern/use") {
			namespace = Ns.AUTOMATION.id
			criteria {
				usingItem("requirement") {
					this.item = itemStackPredicate(Items.PAPER) {
						predicates { customData { this["ra"] = nbt { this["pattern"] = nbt {} } } }
					}
				}
			}
			rewards { function = onUse }
		}

		// As the player, from the advancement, which fires every tick the button is held: only the
		// first tick of a press acts.
		pack.define(onUse) {
			raw("advancement revoke @s only ${useAdvancement.asId()}")
			raw("execute store result score #pat.now $T run time query gametime")
			raw("scoreboard players operation #pat.prev $T = @s $USED")
			raw("scoreboard players operation @s $USED = #pat.now $T")
			raw("scoreboard players remove #pat.now $T 1")
			raw("execute if score #pat.prev $T >= #pat.now $T run return 0")
			raw("execute unless items entity @s weapon.mainhand $HELD run return 0")

			raw("tag @s add $USER")
			raw("scoreboard players set #pat.hit $T 0")
			raw("scoreboard players set #pat.ray $T $REACH_STEPS")
			// `anchored feet` once at the eyes, as the tools' ray: every `^` step would add the eye height again.
			raw("execute at @s anchored eyes positioned ^ ^ ^ anchored feet run function ${step.asId()}")
			raw("tag @s remove $USER")

			raw("execute if score #pat.hit $T matches 0 if predicate ra:is_sneaking run return run function ${clear.asId()}")
			raw("execute if score #pat.hit $T matches 0 run return run ${hint()}")
		}

		// One point of the look, as the player. Written once every part has registered its targets.
		afterAllParts {
			pack.define(step) {
				for (route in Pattern.targets) {
					raw("execute align xyz positioned ~0.5 ~0.5 ~0.5 as @e[type=minecraft:marker,tag=${route.tag},distance=..0.6,limit=1] run return run function ${route.fn.asId()}")
				}
				raw("execute if block ~ ~ ~ minecraft:crafter align xyz positioned ~0.5 ~0.5 ~0.5 run return run function ${record.asId()}")
				// A block the crosshair stops on ends the walk; not one the player's own body is in.
				raw("execute unless block ~ ~ ~ ${BlockTags.rayPassable.asId()} align xyz unless entity @s[dx=0,dy=0,dz=0] run return 0")
				raw("scoreboard players remove #pat.ray $T 1")
				raw("execute if score #pat.ray $T matches 1.. positioned ^ ^ ^0.1 run function ${step.asId()}")
			}
		}

		// As the player, at the crafter's centre.
		pack.define(record) {
			raw("scoreboard players set #pat.hit $T 1")
			raw("execute unless data block ~ ~ ~ Items[0] run return run ${say("This crafter is empty: fill its grid first.", "red")}")
			raw("data modify storage $STORE rec set value {slots:${Pattern.blankSlotsSnbt}}")
			for (n in 0 until 9) raw("data modify storage $STORE rec.slots[$n] set from block ~ ~ ~ Items[{Slot:${n}b}].id")
			raw("item modify entity @s weapon.mainhand $MODIFIER")
			function(lore)
			raw("data remove storage $STORE rec")
			raw(say("Pattern recorded.", "green"))
			raw("playsound minecraft:ui.cartography_table.take_result player @s[scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.7 1.2")
		}

		// As the player.
		pack.define(clear) {
			raw("execute if items entity @s weapon.mainhand *[minecraft:custom_data~{ra:{pattern:{slots:${Pattern.blankSlotsSnbt}}}}] run return run ${hint()}")
			raw("data modify storage $STORE rec set value {slots:${Pattern.blankSlotsSnbt}}")
			raw("item modify entity @s weapon.mainhand $MODIFIER")
			raw(setLore(blankLore))
			raw("data remove storage $STORE rec")
			raw(say("Pattern cleared.", "yellow"))
			raw("playsound minecraft:item.book.page_turn player @s[scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.7 1.0")
		}

		// storage rec.slots -> lore.c0..c8 -> the held Pattern's lore. As the player.
		pack.define(lore) {
			for (n in 0 until 9) {
				raw("data modify storage $STORE cur set from storage $STORE rec.slots[$n]")
				raw("data modify storage $STORE lore.cell set value {text:\"-\",color:\"dark_gray\"}")
				raw("execute unless data storage $STORE {cur:\"\"} run function ${cell.asId()}")
				raw("data modify storage $STORE lore.c$n set from storage $STORE lore.cell")
			}
			raw(setLore(gridLore))
			raw("data remove storage $STORE lore")
			raw("data remove storage $STORE cur")
		}
		pack.define(cell) { cell() }
		// {p}: the path of a `minecraft:` id that passed ra_lib:safe/check in format ID -- no quote,
		// backslash or space can reach this line.
		pack.define(cellName) {
			raw("\$data modify storage $STORE lore.cell set value {translate:\"item.minecraft.\$(p)\",fallback:\"%s\",with:[{translate:\"block.minecraft.\$(p)\"}],color:\"gray\"}")
		}
	}

	/** `storage cur` (a non-empty id) into `storage lore.cell`. */
	private fun Function.cell() {
		// Whatever it is, it can be shown as text: copied, never pasted.
		raw("data modify storage $STORE lore.cell set value {text:\"\",color:\"gray\"}")
		raw("data modify storage $STORE lore.cell.text set string storage $STORE cur")
		raw("data remove storage ra:safe s")
		raw("data modify storage ra:safe s set from storage $STORE cur")
		raw("data modify storage ra:safe f set value ${TextFormat.ID.code}")
		raw("execute store result score #pat.ok $T run function ${RaLib.Safe.check.asId()}")
		raw("execute unless score #pat.ok $T matches 1 run return 0")
		// A vanilla id: its name, translated by the client.
		raw("execute store result score #pat.len $T run data get storage $STORE cur")
		raw("execute if score #pat.len $T matches ..10 run return 0")
		raw("data modify storage $STORE ns set string storage $STORE cur 0 10")
		raw("execute unless data storage $STORE {ns:\"minecraft:\"} run return run data remove storage $STORE ns")
		raw("data remove storage $STORE ns")
		raw("data modify storage $STORE arg set value {}")
		raw("data modify storage $STORE arg.p set string storage $STORE cur 10")
		raw("function ${cellName.asId()} with storage $STORE arg")
		raw("data remove storage $STORE arg")
	}

	private fun say(text: String, color: String) =
		"""tellraw @s [$RA_CHAT_PREFIX,{text:"$text",color:"$color"}]"""

	private fun hint() = say("Sneak + right-click a crafter to record its grid; sneak + right-click air to clear.", "gray")
}
