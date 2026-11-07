package ra.multiblocklib.structure

import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnRun
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.multiblocklib.MB
import ra.multiblocklib.mb
import ra.multiblocklib.mbCopy
import ra.multiblocklib.mbRemove
import ra.multiblocklib.returnIfMbScore
import ra.multiblocklib.returnUnlessMb

/**
 * `ra_lib_multiblock:structure/` functions: checks every block a registered structure requires, in one
 * facing.
 *
 * This replaces the per-multiblock check_blocks macro: the pattern comes from the registered
 * spec, so there is nothing to keep in sync by hand.
 */
object Structure {
	/** Macro `{type, facing}`. At the base block. Returns 1 when the whole pattern matches. */
	val check = ref(Ns.LIB_MULTIBLOCK, "structure/check")
	val checkNext = ref(Ns.LIB_MULTIBLOCK, "structure/check_next")
	val checkOne = ref(Ns.LIB_MULTIBLOCK, "structure/check_one")

	fun Ra.registerStructure() {
		pack.define(check) {
			raw("\$data modify storage ra:multiblock chk_q set from storage ra:multiblock types.\$(type).facings.\$(facing).blocks")
			scoreboard.players.set(mb("#mb_ok"), Obj.MULTIBLOCK, 1)
			function(checkNext)
			mbRemove("chk_q", "chk_cur")
			returnRun { scoreboard.players.get(mb("#mb_ok"), Obj.MULTIBLOCK) }
		}

		// Stops at the first mismatch, so a structure missing its first block costs three commands.
		pack.define(checkNext) {
			returnIfMbScore("#mb_ok", 0, 0)
			returnUnlessMb("chk_q[0]")
			mbCopy("chk_cur", "chk_q[0]")
			mbRemove("chk_q[0]")
			function(checkOne, MB, "chk_cur")
			function(checkNext)
		}

		// `match` is substituted verbatim, so ids, #block_tags and block states all work.
		pack.define(checkOne) {
			raw("\$execute unless block \$(pos) \$(match) run scoreboard players set #mb_ok ${Obj.MULTIBLOCK} 0")
		}
	}
}
