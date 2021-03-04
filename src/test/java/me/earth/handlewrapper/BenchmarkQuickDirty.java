package me.earth.handlewrapper;

import me.earth.handlewrapper.wrappers.HandleWrapper;
import me.earth.handlewrapper.util.CustomBenchmarkInterface;
import me.earth.handlewrapper.util.Dry;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

// Change these values to fit your benchmark, I didn't want to wait 10 minutes everytime
@Fork(value = 1, warmups = 1)
@Measurement(iterations = 10, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Warmup(iterations = 5)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@SuppressWarnings({"unused", "FieldMayBeFinal", "FieldCanBeLocal", "CommentedOutCode"})
public class BenchmarkQuickDirty {
    private static final BenchmarkQuickDirty INSTANCE = new BenchmarkQuickDirty();
    private static final MethodHandle STATIC;
    private static MethodHandle nonFinal;
    private HandleWrapper wrapper;
    private CustomBenchmarkInterface custom;
    private Dry dry;

    static {
        try {
            Method method = BenchmarkQuickDirty.class.getDeclaredMethod("getX");
            method.setAccessible(true);
            STATIC = MethodHandles.lookup().unreflect(method);
            nonFinal = STATIC;
        } catch (NoSuchMethodException | IllegalAccessException t) {
            throw new IllegalStateException(t);
        }
    }

    public BenchmarkQuickDirty() {
        try {
            Method method = BenchmarkQuickDirty.class.getDeclaredMethod("getX");
            method.setAccessible(true);
            MethodHandle handle = MethodHandles.lookup().unreflect(method);
            wrapper = WrapperFactory.wrap(handle, method);
            custom = WrapperFactory.wrap(CustomBenchmarkInterface.class, CustomBenchmarkInterface.getLinks(), handle);
            dry = new Dry();
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    private int getX() {
        return 5;
    }

    @Benchmark
    public int benchmarkDirect() {
        return INSTANCE.getX();
    }

    @Benchmark
    public int benchmarkStaticFinalMethodHandle() throws Throwable {
        return (int) STATIC.invokeExact(INSTANCE);
    }

    @Benchmark
    public int benchmarkWrapper() throws Throwable {
        return (int) wrapper.invoke(INSTANCE);
    }

    @Benchmark
    public int benchmarkNonFinalHandle() throws Throwable {
        return (int) nonFinal.invoke(INSTANCE);
    }

    /*

    @Benchmark
    public int dry() throws Throwable {
        return (int) dry.invoke(INSTANCE);
    }

    @Benchmark
    public int benchmarkCustomHandleWrapper() {
        return custom.invoke(INSTANCE);
    }

    @Benchmark
    public int benchmarkWrapperExact() throws Throwable {
        return (int) wrapper.invokeExact(INSTANCE);
    }

    */

}
