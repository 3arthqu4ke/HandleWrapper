package me.earth.handlewrapper.util;

import me.earth.handlewrapper.BenchmarkQuickDirty;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public interface CustomBenchmarkInterface {
    int invoke(BenchmarkQuickDirty benchmarkQuickDirty);

    static Map<Method, Method> getLinks() {
        Map<Method, Method> map = new HashMap<>();

        try {
            map.put(CustomBenchmarkInterface.class.getDeclaredMethod("invoke", BenchmarkQuickDirty.class),
                    MethodHandle.class.getDeclaredMethod("invoke", Object[].class));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }

        return map;
    }

}
