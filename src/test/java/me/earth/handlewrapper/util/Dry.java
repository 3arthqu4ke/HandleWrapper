package me.earth.handlewrapper.util;

import me.earth.handlewrapper.BenchmarkQuickDirty;
import me.earth.handlewrapper.wrappers.HandleWrapper;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public class Dry implements HandleWrapper {
    private static final MethodHandle HANDLE;

    static {
        try {
            Method method = BenchmarkQuickDirty.class.getDeclaredMethod("getX");
            method.setAccessible(true);
            HANDLE = MethodHandles.lookup().unreflect(method);
        } catch (NoSuchMethodException | IllegalAccessException t) {
            throw new IllegalStateException(t);
        }
    }

    @Override
    public Object invoke(Object...args) throws Throwable {
        return (int) HANDLE.invoke((BenchmarkQuickDirty) args[0]);
    }

    @Override
    public Object invokeExact(Object... args) throws Throwable {
        return (int) HANDLE.invokeExact((BenchmarkQuickDirty) args[0]);
    }

}
