package xyz.phanta.cbtweaker.util.helper;

import io.github.phantamanta44.libnine.util.math.Vec2i;

public class CbtMathUtils {

    // automatically figure out how to lay out slots in a variable-size group!
    // first, we compute the smallest square number that is >= the slot count; this will be the square size
    // next, we want to minimize the number of rows used, then minimize the difference in count between rows
    public static Vec2i layOutSlotGroup(int slotWidth, int slotHeight, Vec2i[] slotPosList) {
        int rowLength = 1;
        while (rowLength * rowLength < slotPosList.length) {
            rowLength++;
        }
        int rowCount = (int)Math.ceil(slotPosList.length / (double)rowLength);
        int longRowCount;
        if (rowCount == 1) {
            longRowCount = 1; // edge case, should only happen for slotCount = 1 or 2
        } else {
            longRowCount = slotPosList.length % rowCount;
            if (longRowCount == 0) {
                longRowCount = rowLength;
            }
        }
        int i = 0;
        int y = 0;
        while (y < longRowCount) {
            for (int x = 0; x < rowLength; x++) {
                slotPosList[i++] = new Vec2i(x * slotWidth, y * slotHeight);
            }
            y++;
        }
        while (y < rowCount) {
            for (int x = 0; x < rowLength - 1; x++) {
                slotPosList[i++] = new Vec2i(slotWidth / 2 + x * slotWidth, y * slotHeight);
            }
            y++;
        }
        return new Vec2i(rowLength * slotWidth, rowCount * slotHeight);
    }

}
