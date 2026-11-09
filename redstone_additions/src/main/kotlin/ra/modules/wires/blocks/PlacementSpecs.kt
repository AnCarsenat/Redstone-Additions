package ra.modules.wires.blocks

/**
 * The placement spec of every Wires block, keyed by the place tag its bat carries
 * (`ra.place.<id>`). blocks/handle_placement copies the matching one into `storage ra:wires spec`;
 * the field meanings are documented there.
 *
 * Order is the order the specs are tested in, which is the order the beet file listed them.
 */
internal val SPECS: List<Pair<String, String>> = listOf(
	// One pipe. The four of them placed the same block with the same marker and the
	// same capacity -- gas and liquid pipes were never different, and the tiers stopped
	// meaning anything when capacity became "a pipe holds a litre". The three retired
	// place tags still map here so pipes already sitting in a chest still place a pipe.
	"liquid_pipe_copper" to """{block:"minecraft:conduit[waterlogged=false]",marker:"liquid_pipe",fluid:1b,net:"fluid",capacity:1000}""",
	"liquid_pipe_netherite" to """{block:"minecraft:conduit[waterlogged=false]",marker:"liquid_pipe",fluid:1b,net:"fluid",capacity:1000}""",
	"gas_pipe_copper" to """{block:"minecraft:conduit[waterlogged=false]",marker:"liquid_pipe",fluid:1b,net:"fluid",capacity:1000}""",
	"gas_pipe_netherite" to """{block:"minecraft:conduit[waterlogged=false]",marker:"liquid_pipe",fluid:1b,net:"fluid",capacity:1000}""",

	"liquid_tank" to """{block:"minecraft:waxed_copper_block",marker:"liquid_tank",fluid:1b,net:"fluid",capacity:100000}""",
	"gas_tank" to """{block:"minecraft:iron_block",marker:"gas_tank",fluid:1b,net:"fluid",capacity:100000}""",

	"liquid_pump" to """{block:"minecraft:dispenser",marker:"liquid_pump",dir:2,fluid:1b,net:"fluid",capacity:5000,props:{}}""",
	"gas_pump" to """{block:"minecraft:smoker",marker:"gas_pump",dir:1,fluid:1b,net:"fluid",capacity:5000,props:{}}""",

	"liquid_valve" to """{block:"minecraft:waxed_cut_copper",marker:"liquid_valve",bridge:1b,props:{rate:2000}}""",
	// A filter is a valve that only passes one medium. Same bridge machinery, one
	// extra property -- see ra_wires:bridge/read_src for where filter_medium is read.
	"liquid_filter" to """{block:"minecraft:waxed_exposed_cut_copper",marker:"liquid_filter",bridge:1b,props:{rate:2000,filter_medium:"water"}}""",
	"gas_valve" to """{block:"minecraft:smooth_basalt",marker:"gas_valve",bridge:1b,props:{rate:2000}}""",

	"liquid_drain" to """{block:"minecraft:dropper",marker:"liquid_drain",dir:2,fluid:1b,net:"fluid",capacity:5000,props:{rate:${ra.modules.wires.fluid.Drain.DEFAULT_RATE}}}""",

	// One wire. The two tiers differed only in a capacity they no longer have and a
	// transfer rate that stopped existing when charge moved onto the grid, so an L2
	// wire was a more expensive way to buy the same block. The netherite place tag is
	// kept so wires already in chests still place something.
	"electric_wire_copper" to """{block:"minecraft:conduit[waterlogged=false]",marker:"electric_wire",electric:1b,net:"electric",capacity:0}""",
	"electric_wire_netherite" to """{block:"minecraft:conduit[waterlogged=false]",marker:"electric_wire",electric:1b,net:"electric",capacity:0}""",
	"electric_generator" to """{block:"minecraft:barrel",marker:"electric_generator",electric:1b,net:"electric",dir:1,capacity:50,props:{}}""",
	"electric_consumer" to """{block:"minecraft:observer",marker:"electric_consumer",electric:1b,net:"electric",capacity:50,props:{}}""",
	"solar_panel" to """{block:"minecraft:daylight_detector",marker:"solar_panel",electric:1b,net:"electric",capacity:50,props:{}}""",
	"electric_switch" to """{block:"minecraft:waxed_cut_copper",marker:"electric_switch",electric:1b,net:"electric",capacity:0,props:{}}""",

	"battery" to """{block:"minecraft:waxed_copper_grate",marker:"battery",electric:1b,net:"electric",capacity:10000,props:{}}""",
	"creative_eu" to """{block:"minecraft:beacon",marker:"creative_eu",electric:1b,net:"electric",capacity:100000,props:{}}""",
	"creative_fluid" to """{block:"minecraft:beacon",marker:"creative_fluid",fluid:1b,net:"fluid",capacity:100000,props:{medium:"water",rate:1000}}""",
	"electric_furnace" to """{block:"minecraft:barrel",marker:"electric_furnace",electric:1b,net:"electric",dir:1,capacity:50,props:{mode:"low"}}""",
	"industrial_light" to """{block:"minecraft:sea_lantern",marker:"industrial_light",electric:1b,net:"electric",capacity:0,dir:3,props:{eu_use:10}}""",
	"electric_breaker" to """{block:"minecraft:waxed_copper_bulb",marker:"electric_breaker",bridge:1b,props:{rate:200}}""",

	"boiler" to """{block:"minecraft:furnace",marker:"boiler",props:{}}""",

	// Readouts: a target block facing the player (dir 3, so the marker carries the facing) that
	// reads the network behind it without joining it. `readout` tags the marker for the slow
	// clock in ra_wires:readout.
	"network_gauge" to """{block:"minecraft:target",marker:"network_gauge",dir:3,readout:1b}""",
	"usage_meter" to """{block:"minecraft:target",marker:"usage_meter",dir:3,readout:1b,props:{mode:"consumed",every:${ra.modules.wires.blocks.usage_meter.UsageMeter.DEFAULT_EVERY},total:0}}""",

	// Machines on the once-a-second clock (ra_wires:machine): `clocked` tags the marker for it.
	"tesla_coil" to """{block:"minecraft:waxed_lightning_rod",marker:"tesla_coil",electric:1b,net:"electric",capacity:0,clocked:1b,props:{radius:${ra.modules.wires.blocks.tesla_coil.TeslaCoil.DEFAULT_RADIUS},damage:${ra.modules.wires.blocks.tesla_coil.TeslaCoil.DEFAULT_DAMAGE},eu_per_zap:${ra.modules.wires.blocks.tesla_coil.TeslaCoil.DEFAULT_EU}}}""",
	"spout" to """{block:"minecraft:waxed_exposed_copper_grate",marker:"spout",fluid:1b,net:"fluid",capacity:1000,clocked:1b}""",
	"gas_vent" to """{block:"${ra.modules.wires.blocks.gas_vent.GasVent.BLOCK}",marker:"gas_vent",fluid:1b,net:"fluid",capacity:${ra.modules.wires.blocks.gas_vent.GasVent.CAPACITY},clocked:1b,props:{radius:${ra.modules.wires.blocks.gas_vent.GasVent.DEFAULT_RADIUS},rate:${ra.modules.wires.blocks.gas_vent.GasVent.DEFAULT_RATE}}}""",
)
