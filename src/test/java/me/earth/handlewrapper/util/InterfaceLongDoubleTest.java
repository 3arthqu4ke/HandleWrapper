package me.earth.handlewrapper.util;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Test if var offsets for long and double work.
 */
public interface InterfaceLongDoubleTest {
    double invoke(LongDoubleTest longDoubleTest, long l, String s, double d, int i);

    static Map<Method, Method> getLinks() {
        Map<Method, Method> map = new HashMap<>();

        try {
            map.put(InterfaceLongDoubleTest.class.getDeclaredMethod("invoke", LongDoubleTest.class, long.class, String.class, double.class, int.class),
                    MethodHandle.class.getDeclaredMethod("invoke", Object[].class));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }

        return map;
    }

    class LongDoubleTest {
        @SuppressWarnings("unused")
        public double testLongDouble(long l, String s, double d, int i) {
            return l + d;
        }
    }

}
