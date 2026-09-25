package st.evening.mc.cbtweaker.util.config

import it.unimi.dsi.fastutil.chars.Char2ObjectMap
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap
import net.minecraft.util.math.Vec3i
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.structure.block.StructureBlockMatcher
import st.evening.mc.cbtweaker.structure.block.impl.AirStructureBlockMatcher
import st.evening.mc.cbtweaker.structure.block.joinMatchers
import st.evening.mc.prelude.api.data.ser.SerializationException
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.forEachArrayIndexed
import st.evening.mc.prelude.api.data.tjson.forEachStringIndexed
import st.evening.mc.prelude.api.data.tjson.forEachValue

object BlockArrayHelper {
    context(_: JsonPath)
    fun loadPalette(paletteDto: TJson.Object): Char2ObjectMap<StructureBlockMatcher> {
        val palette = Char2ObjectOpenHashMap<StructureBlockMatcher>()
        palette.put('.', AirStructureBlockMatcher)
        paletteDto.forEachValue { symbol, matchersDto ->
            if (symbol.length != 1) {
                throw SerializationException.withPath("Palette entry must be a single character: [$symbol]")
            }
            val symbolChar = symbol[0]
            when (symbolChar) {
                ' ', '.', '@' ->
                    throw SerializationException.withPath("Cannot use reserved character in palette: [$symbolChar]")
            }
            val matchers = mutableListOf<StructureBlockMatcher>()
            if (matchersDto is TJson.Array) {
                matchersDto.forEachValue {
                    matchers.addAll(CbTweaker.defns.templates.structureBlockMatcherTemplates.resolve(it))
                }
            } else {
                matchers.addAll(CbTweaker.defns.templates.structureBlockMatcherTemplates.resolve(matchersDto))
            }
            palette.put(symbolChar, matchers.joinMatchers())
        }
        return palette
    }

    context(_: JsonPath)
    fun loadBlockArray(blockArrayDto: TJson.Array): Array<Array<CharArray>> {
        // earlier y slices correspond to higher y values, and north is facing downwards "towards the viewer", so we
        // need to reverse at all three levels to transform back into the world coordinate system
        val blockArray = arrayOfNulls<Array<CharArray?>>(blockArrayDto.size)
        blockArrayDto.forEachArrayIndexed { coY, ySliceDto ->
            val ySlice = arrayOfNulls<CharArray>(ySliceDto.size)
            ySliceDto.forEachStringIndexed { coZ, zSliceDto ->
                ySlice[ySlice.size - 1 - coZ] = zSliceDto.toCharArray().also { it.reverse() }
            }
            blockArray[blockArray.size - 1 - coY] = ySlice
        }
        @Suppress("UNCHECKED_CAST")
        return blockArray as Array<Array<CharArray>>
    }

    fun searchBlockArray(blockArray: Array<Array<CharArray>>, query: Char): MutableList<Vec3i> {
        val results = mutableListOf<Vec3i>()
        blockArray.forEachIndexed { y, ySlice ->
            ySlice.forEachIndexed { z, zSlice ->
                zSlice.forEachIndexed { x, ch ->
                    if (ch == query) {
                        results += Vec3i(x, y, z)
                    }
                }
            }
        }
        return results
    }

    context(_: JsonPath)
    fun findControllerPosition(blockArray: Array<Array<CharArray>>): Vec3i {
        val candidates = searchBlockArray(blockArray, '@')
        when (candidates.size) {
            0 -> throw SerializationException.withPath("Structure has no controller!")
            1 -> return candidates[0]
            else -> throw SerializationException.withPath("Structure has more than one controller!")
        }
    }

    fun computeSize(blockArray: Array<Array<CharArray>>): Vec3i {
        var sizeX = 0
        var sizeZ = 0
        blockArray.forEach { ySlice ->
            ySlice.forEach { zSlice ->
                if (zSlice.size > sizeX) {
                    sizeX = zSlice.size
                }
            }
            if (ySlice.size > sizeZ) {
                sizeZ = ySlice.size
            }
        }
        return Vec3i(sizeX, blockArray.size, sizeZ)
    }
}
