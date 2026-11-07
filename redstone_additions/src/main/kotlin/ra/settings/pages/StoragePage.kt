package ra.settings.pages

import ra.core.Ns

val StoragePage = Page(
	"storage", "Storage", Ns.STORAGE,
	blocks("boxer" to "Boxer", "unboxer" to "Unboxer"),
)
