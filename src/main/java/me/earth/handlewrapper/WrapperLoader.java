package me.earth.handlewrapper;

final class WrapperLoader extends ClassLoader {
    public WrapperLoader(ClassLoader parent) {
        super(parent);
    }

    public Class<?> define(String name, byte[] data) {
        return defineClass(name, data, 0, data.length);
    }

}
