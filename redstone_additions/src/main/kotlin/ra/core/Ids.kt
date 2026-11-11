package ra.core

import io.github.ayfri.kore.arguments.types.resources.storage

/**
 * Identifiers shared by more than one module: scoreboard objectives, entity tags and data
 * storages. Anything used by a single module lives next to that module instead (for example
 * `ra.jp.*` in the jetpacks package), so this file only grows when something becomes shared.
 *
 * These strings are written into saved worlds (scores on markers, tags on entities, storage
 * compounds), so they are a compatibility contract: renaming one orphans existing blocks.
 */
object Obj {
	/** Scratch objective for fake players (`#name`). Never read across ticks. */
	const val TEMP = "ra.temp"
	const val COOLDOWN = "ra.cooldown"

	// Orientation, written by ra_lib:placement/place and orientation/*.
	const val FACING = "ra.facing"
	const val YAW = "ra.yaw"
	const val PITCH = "ra.pitch"

	// Redstone reader output. 0 = off, 1..15 = level, 16 = superpower.
	const val POWER = "ra.power"
	const val POWER_UP = "ra.power.up"
	const val POWER_DOWN = "ra.power.down"
	const val POWER_NORTH = "ra.power.north"
	const val POWER_SOUTH = "ra.power.south"
	const val POWER_EAST = "ra.power.east"
	const val POWER_WEST = "ra.power.west"
	const val POWER_FRONT = "ra.power.front"
	const val POWER_BACK = "ra.power.back"
	const val POWER_LEFT = "ra.power.left"
	const val POWER_RIGHT = "ra.power.right"
	const val POWER_LOCAL_UP = "ra.power.local_up"
	const val POWER_LOCAL_DOWN = "ra.power.local_down"
	const val RS_INPUTS = "ra.rs_inputs"

	// Transport networks (ra_lib:transport).
	const val TR_NET = "ra.tr.net"
	const val TR_CAP = "ra.tr.cap"
	const val TR_CLASS = "ra.tr.class"
	const val TR_CARRY = "ra.tr.carry"
	const val TR_OLD = "ra.tr.old"
	const val TR_TMP = "ra.tr.tmp"

	const val INV_COUNT = "ra.inv.count"

	// Multiblocks (ra_lib_multiblock).
	const val MULTIBLOCK = "ra.multiblock"
	const val MB_TIMER = "ra.mb_timer"
	const val HEAT = "ra.heat"

	// Settings scratch + per-player ("user") settings read by every module.
	const val SET_TMP = "ra.set.tmp"
	/** Player wants block sounds. Filter: `scores={ra.u.snd=1..}`. */
	const val USER_SOUND = "ra.u.snd"
	/** Player wants block particles. */
	const val USER_PARTICLES = "ra.u.par"
	const val USER_DEBUG = "ra.u.dbg"
	/**
	 * Player wants the goggles to draw filter previews (the Item Pipe's floating filter item).
	 * Billboards are shared entities, so this decides whether THIS wearer asks for them: a pipe's
	 * preview is drawn when any goggles wearer in range has it on. See ra.tools.goggles.GogglesTool.
	 */
	const val USER_FILTER_PREVIEW = "ra.u.fprev"
	/** Player wants the jetpack fuel gauge on the actionbar while flying. See ra.modules.jetpacks.flight.Gauge. */
	const val USER_JETPACK_GAUGE = "ra.u.jpfuel"
	/** Player wants the Air Tank's air gauge on the actionbar under water. See ra.modules.wires.diving.AirTank. */
	const val USER_AIR_GAUGE = "ra.u.airg"
	/**
	 * Player wants chat notices: the automatic lines the pack sends without being asked (a
	 * sacrifice's result, a Rail Builder's progress, "fitted" confirmations, other players' wrench
	 * readouts). Off by default. Filter: `scores={ra.u.msg=1..}`; see ra.core.block.noticeReceiversSel.
	 */
	const val USER_NOTICES = "ra.u.msg"
}

/** Entity tags shared across modules. Per-block tags are built by [ra.core.block.CustomBlock]. */
object Tag {
	/** On every entity the pack summons, so `/kill @e[tag=ra]` removes the pack's entities. */
	const val RA = "ra"
	const val CUSTOM_BLOCK = "ra.custom_block"
	const val NEW = "ra.new"
	const val BROKEN = "ra.broken"
	const val SPAWNED = "ra.spawned"
	const val PROCESSED = "ra.processed"
	const val PROCESSING = "ra.processing"
	const val PLACER = "ra.placer"
	const val DROP_TEMP = "ra.drop_temp"
	const val DISPLAY = "ra.display"
	const val BILLBOARD = "ra.billboard"
	const val MULTIBLOCK = "ra.multiblock"
	const val POWERED = "ra.powered"
	const val DEBUG = "ra.debug"

	fun customBlock(id: String) = "$CUSTOM_BLOCK.$id"
	fun place(id: String) = "ra.place.$id"
	fun multiblock(id: String) = "$MULTIBLOCK.$id"
}

/** Data storages. All live in the `ra` namespace (`storage ra:<name>`). */
object Storage {
	val TEMP = storage("temp", "ra")
	val SETTINGS = storage("settings", "ra")
	val TRANSPORT = storage("transport", "ra")
	val MULTIBLOCK = storage("multiblock", "ra")
	val INPUT = storage("input", "ra")
	val INVENTORY = storage("inventory", "ra")
	val DH = storage("dh", "ra")
	val CDH = storage("cdh", "ra")
	val WRENCH = storage("wrench", "ra")
	val DISPLAY = storage("display", "ra")
	val CLIPBOARD = storage("clipboard", "ra")
	val DEBUG = storage("debug", "ra")
	val WIRES = storage("wires", "ra")
	val ENDER = storage("ender", "ra")
	val ENCHANT = storage("enchant", "ra")
	val JETPACKS = storage("jetpacks", "ra")
	val INTERACTIVE = storage("interactive", "ra")
	val BLOCK = storage("block", "ra")
}
