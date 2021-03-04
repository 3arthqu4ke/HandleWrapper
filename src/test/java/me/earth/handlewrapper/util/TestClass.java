package me.earth.handlewrapper.util;

@SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal", "unused", "SameParameterValue"})
public class TestClass {
    private static int staticState = 5;

    private int i = 5;

    public TestClass() {

    }

    private TestClass(int o) {
        i = o;
    }

    public int getI() {
        return i;
    }

    private static void setStaticState(int state) {
        staticState = state;
    }

    public static int getStaticState() {
        return staticState;
    }

}
