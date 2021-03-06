package me.earth.handlewrapper.wrappers;

import me.earth.handlewrapper.WrapperFactory;

import java.lang.invoke.MethodHandle;

/**
 * A wrapper for a {@link MethodHandle}.
 * These Wrappers are generated by the {@link WrapperFactory}.
 */
public interface HandleWrapper {
    /** {@link MethodHandle#invoke(Object...)}. */
    Object invoke(Object...args) throws Throwable;

    /** {@link MethodHandle#invokeExact(Object...)}. */
    Object invokeExact(Object...args) throws Throwable;

}
