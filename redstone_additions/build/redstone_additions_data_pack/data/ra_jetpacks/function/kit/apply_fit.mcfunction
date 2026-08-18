# /ra_jetpacks:kit/apply_fit {tier,n}
# Internal: stamp the chestplate with the state named by tier and n.

$item modify entity @s armor.chest ra_jetpacks:fit_$(tier)_$(n)
