package me.earth.handlewrapper.util;

import me.earth.handlewrapper.wrappers.IMethodHandle;

import java.lang.invoke.MethodHandle;

/**
 * An interface Defining all public Methods for {@link MethodHandle}.
 */
public interface CustomMethodHandle extends IMethodHandle {
    int invoke(HandleMaker maker1, HandleMaker maker2) throws Throwable;

    int invokeExact(HandleMaker maker1, HandleMaker maker2) throws Throwable;

}
