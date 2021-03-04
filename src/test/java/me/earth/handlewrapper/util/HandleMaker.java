package me.earth.handlewrapper.util;

import me.earth.handlewrapper.WrapperFactory;
import me.earth.handlewrapper.wrappers.IMethodHandle;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Map;

public class HandleMaker {
    private final Map<Method, Method> links;
    private final int test;

    public HandleMaker(int test) throws Throwable {
        Map<Method, Method> links = IMethodHandle.getLinks();
        links.put(CustomMethodHandle.class.getDeclaredMethod("invoke", HandleMaker.class, HandleMaker.class), MethodHandle.class.getDeclaredMethod("invoke", Object[].class));
        links.put(CustomMethodHandle.class.getDeclaredMethod("invokeExact", HandleMaker.class, HandleMaker.class), MethodHandle.class.getDeclaredMethod("invokeExact", Object[].class));
        this.links = links;
        this.test = test;
    }

    public CustomMethodHandle make() throws Throwable {
        Method method = HandleMaker.class.getDeclaredMethod("testMethod", HandleMaker.class);
        method.setAccessible(true);
        return WrapperFactory.wrap(CustomMethodHandle.class, links, MethodHandles.lookup().unreflect(method));
    }

    private int testMethod(HandleMaker maker) {
        return maker.test;
    }

}
