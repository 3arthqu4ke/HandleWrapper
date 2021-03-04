package me.earth.handlewrapper;

import me.earth.handlewrapper.util.TestClass;
import me.earth.handlewrapper.wrappers.HandleWrapper;
import me.earth.handlewrapper.util.HandleMaker;
import me.earth.handlewrapper.util.CustomMethodHandle;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TestCases {
    @Test
    public void testFieldHandle() throws Throwable {
        Field field = TestClass.class.getDeclaredField("i");
        field.setAccessible(true);
        MethodHandle getter = MethodHandles.lookup().unreflectGetter(field);
        MethodHandle setter = MethodHandles.lookup().unreflectSetter(field);

        HandleWrapper wrapGetter  = WrapperFactory.wrapGetter(getter, field);
        HandleWrapper wrapSetter  = WrapperFactory.wrapSetter(setter, field);

        TestClass testClass = new TestClass();
        Assertions.assertEquals(wrapGetter.invoke(testClass), 5);
        wrapSetter.invoke(testClass, 10);
        Assertions.assertEquals(wrapGetter.invokeExact(testClass), 10);
    }

    @Test
    public void testSetStaticMethod() throws Throwable {
        Method method = TestClass.class.getDeclaredMethod("setStaticState", int.class);
        method.setAccessible(true);
        Assertions.assertEquals(TestClass.getStaticState(), 5);
        HandleWrapper wrapper = WrapperFactory.wrap(MethodHandles.lookup().unreflect(method), method);
        wrapper.invoke(10);

        Assertions.assertEquals(TestClass.getStaticState(), 10);
    }

    @Test
    public void testConstructorHandle() throws Throwable {
        Constructor<?> ctr = TestClass.class.getDeclaredConstructor(int.class);
        ctr.setAccessible(true);
        HandleWrapper wrapper = WrapperFactory.wrapConstructor(MethodHandles.lookup().unreflectConstructor(ctr), ctr);
        TestClass testClass = (TestClass) wrapper.invoke(600);

        Assertions.assertEquals(testClass.getI(), 600);
    }

    @Test
    public void testCustomHandle() throws Throwable {
        Random rnd = new Random();
        int random = rnd.nextInt();
        HandleMaker maker = new HandleMaker(random);
        CustomMethodHandle methodHandle = maker.make();
        List<Object> args = new ArrayList<>();
        args.add(maker);
        args.add(maker);

        Assertions.assertEquals(random, methodHandle.invoke(maker, maker));
        Assertions.assertEquals(random, methodHandle.invokeExact(maker, maker));
        Assertions.assertEquals(random, methodHandle.invokeWithArguments(args));
        Assertions.assertEquals(random, methodHandle.invokeWithArguments(args.toArray()));
    }

}
