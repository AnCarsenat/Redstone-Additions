# /ra_gates:load
# Initialize gates module

# Register gate blocks
function ra_gates:blocks/uni_gate/register_block
function ra_gates:blocks/clock/register_block
function ra_gates:blocks/delayer/register_block
function ra_gates:blocks/extender/register_block
function ra_gates:blocks/rand/register_block
function ra_gates:blocks/shortener/register_block

tellraw @a [{text:"[",color:"dark_gray"},{text:"RA",color:"gold",bold:true},{text:"] ",color:"dark_gray"},{text:"Logic Gates loaded!",color:"aqua"}]
