# /ra_lib_multiblock:io/is_block_here
# Internal: positioned at the IO block by io/at. Hands the comparison to a macro
# fed from its own small compound — passing the whole ra:multiblock storage as
# macro arguments would stringify every unrelated key on every call.

function ra_lib_multiblock:io/is_block_test with storage ra:multiblock io_test
