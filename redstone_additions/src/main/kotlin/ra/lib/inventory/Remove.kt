package ra.lib.inventory

import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.scoreboard.Operation
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import net.benwoodworth.knbt.buildNbtList
import ra.api.RaLib
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.define
import ra.core.raw
import ra.lib.fake
import ra.lib.inventory.Inventory.here
import ra.lib.lib

/** `ra_lib:inventory/remove` and its helpers. */
internal object Remove {
	val step = lib("inventory/remove/step")
	val keep = lib("inventory/remove/keep")
	val consume = lib("inventory/remove/consume")

	private val INV = Storage.INVENTORY
	private val need = fake("#inv_need")
	private val entry = fake("#inv_entry")
	private val match = fake("#inv_match")

	fun Ra.registerRemove() {
		// `remove {id, count}`: remove N items with the given id from the container at the current
		// position. Returns 1 if the full amount was removed, 0 otherwise.
		//
		// Works with any container size and with the requested amount spread over several stacks.
		// All-or-nothing: when the container holds less than the amount asked for, nothing is
		// taken and the function returns 0.
		//
		// Example: function ra_lib:inventory/remove {id:"minecraft:wheat",count:1}
		pack.define(RaLib.Inventory.remove) {
			raw("\$data modify storage ra:inventory req.id set value \"\$(id)\"")
			raw("\$scoreboard players set #inv_need ${Obj.TEMP} \$(count)")

			execute {
				ifCondition { score(need, Obj.TEMP) lessThanOrEqualTo 0 }
				run { returnValue(0) }
			}
			execute {
				unlessCondition { data(here, "Items") }
				run { returnValue(0) }
			}

			// Rebuild the item list into `out` one entry at a time, taking from matching stacks on
			// the way. The container itself is untouched until the whole request has been
			// satisfied, so a failed removal cannot consume a partial amount.
			data(INV) { modify("scan") { set(here, "Items") } }
			data(INV) { modify("out") { set(buildNbtList<Nothing> { }) } }
			function(step, INV, "req")
			data(INV) { remove("scan") }

			// Anything still outstanding means the container did not hold enough.
			execute {
				ifCondition { score(need, Obj.TEMP) greaterThanOrEqualTo 1 }
				run { data(INV) { remove("out") } }
			}
			execute {
				ifCondition { score(need, Obj.TEMP) greaterThanOrEqualTo 1 }
				run { returnValue(0) }
			}

			data(here) { modify("Items") { set(INV, "out") } }
			data(INV) { remove("out") }
			returnValue(1)
		}

		// `step {id}`: moves storage ra:inventory scan[0] over to `out`, taking from it first when
		// it matches the requested id, then recurses on the rest of the list.
		//
		// Every read targets scan[0] specifically, never a filtered path, so a container holding
		// several stacks of the same item can never resolve ambiguously.
		pack.define(step) {
			execute {
				unlessCondition { data(INV, "scan[0]") }
				run { returnValue(0) }
			}

			scoreboard.players.set(match, Obj.TEMP, 0)
			raw("\$execute if data storage ra:inventory scan[0]{id:\"\$(id)\"} if score #inv_need ${Obj.TEMP} matches 1.. run scoreboard players set #inv_match ${Obj.TEMP} 1")

			execute {
				ifCondition { score(match, Obj.TEMP) equalTo 0 }
				run { function(keep) }
			}
			execute {
				ifCondition { score(match, Obj.TEMP) equalTo 1 }
				run { function(consume) }
			}

			data(INV) { remove("scan[0]") }
			function(step, INV, "req")
		}

		// Carry the current entry over to the output list untouched.
		pack.define(keep) { data(INV) { modify("out") { append(INV, "scan[0]") } } }

		// Take as much as is still outstanding from the current entry. The entry is only carried
		// over to the output list when something is left of it, so emptied stacks free their slot
		// instead of lingering as count:0.
		pack.define(consume) {
			raw("execute store result score #inv_entry ${Obj.TEMP} run data get storage ra:inventory scan[0].count")

			// Stack is no larger than the outstanding amount: it is consumed whole.
			execute {
				ifCondition { score(entry, Obj.TEMP) lessThanOrEqualTo score(need, Obj.TEMP) }
				run { scoreboard.players.operation(need, Obj.TEMP, Operation.REMOVE, entry, Obj.TEMP) }
			}
			execute {
				ifCondition { score(entry, Obj.TEMP) lessThanOrEqualTo score(need, Obj.TEMP) }
				run { returnValue(0) }
			}

			// Stack covers the remainder: shrink it and keep what is left.
			scoreboard.players.operation(entry, Obj.TEMP, Operation.REMOVE, need, Obj.TEMP)
			scoreboard.players.set(need, Obj.TEMP, 0)
			raw("execute store result storage ra:inventory scan[0].count int 1 run scoreboard players get #inv_entry ${Obj.TEMP}")
			data(INV) { modify("out") { append(INV, "scan[0]") } }
		}
	}
}
