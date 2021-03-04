package me.earth.handlewrapper.wrappers;

import me.earth.handlewrapper.WrapperFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An interface that offers all public Methods of
 * {@link MethodHandle}, except the SignaturePolymorphic
 * ones ({@link MethodHandle#invoke(Object...)}
 * and {@link MethodHandle#invokeExact(Object...)}).
 */
@SuppressWarnings("unused")
public interface IMethodHandle {
    /** {@link MethodHandle#type()} */
    MethodType type();

    /** {@link MethodHandle#toString()} */
    String toString();

    /** {@link MethodHandle#asCollector(Class, int)} */
    MethodHandle asCollector(Class<?> clazz, int arrayLength);

    /** {@link MethodHandle#asFixedArity()} */
    MethodHandle asFixedArity();

    /** {@link MethodHandle#asSpreader(Class, int)} */
    MethodHandle asSpreader(Class<?> arrayType, int arrayLength);

    /** {@link MethodHandle#asType(MethodType)} */
    MethodHandle asType(MethodType type);

    /** {@link MethodHandle#asVarargsCollector(Class)} */
    MethodHandle asVarargsCollector(Class<?> arrayType);

    /** {@link MethodHandle#bindTo(Object)} */
    MethodHandle bindTo(Object obj);

    /** {@link MethodHandle#invokeWithArguments(Object...)} */
    Object invokeWithArguments(Object[] objects);

    /** {@link MethodHandle#invokeWithArguments(List)} */
    Object invokeWithArguments(List<?> args);

    /** {@link MethodHandle#isVarargsCollector()} */
    boolean isVarargsCollector();

    /**
     * @return a Map to use for
     *         {@link WrapperFactory#wrap(Class, Map, MethodHandle)}.
     * @throws NoSuchMethodException if a method can't be found (Critical).
     */
    static Map<Method, Method> getLinks() throws NoSuchMethodException {
        Map<Method, Method> links = new HashMap<>();
        for (Method method : IMethodHandle.class.getDeclaredMethods()) {
            if (!method.getName().equals("getLinks")) {
                Method corresponding = MethodHandle.class.getDeclaredMethod(method.getName(), method.getParameterTypes());
                links.put(method, corresponding);
            }
        }

        return links;
    }

}
