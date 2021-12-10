package opencontacts.open.com.opencontacts.utils;

import java.util.List;

public class PrimitiveDataTypeUtils {
    public static boolean[] toPrimitiveBools(List<Boolean> collection) {
        boolean[] primitives = new boolean[collection.size()];
        for (int i = 0; i < primitives.length; i++) {
            primitives[i] = collection.get(i);
        }
        return primitives;
    }

    public static long[] toPrimitiveLongs(List<Long> list) {
        long[] primitives = new long[list.size()];
        for (int i = 0; i < primitives.length; i++) {
            primitives[i] = list.get(i);
        }
        return primitives;
    }
}
