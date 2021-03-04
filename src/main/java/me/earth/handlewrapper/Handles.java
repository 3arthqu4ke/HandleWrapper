package me.earth.handlewrapper;

import java.lang.invoke.MethodHandle;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * I didn't find a simple way to hide this properly,
 * so keep in mind that this shouldn't be accessible for you.
 * <p>
 * <p>This classes goal is to get the MethodHandle into
 * the static initializer like this:
 * <blockquote><pre>{@code
 * static {
 *      MethodHandle handle = Handles.getHandle(id);
 * }
 * }</pre></blockquote>
 *
 * The id comes from an LDC instruction.
 */
public class Handles {
    private static final Map<Integer, MethodHandle> HANDLES = new ConcurrentHashMap<>();

    private Handles() {
        throw new AssertionError();
    }

    static void add(int id, MethodHandle handle) {
        if (HANDLES.containsKey(id)) {
            throw new IllegalStateException("ID " + id + " already exists.");
        }

        HANDLES.put(id, handle);
    }

    /**
     * Deprecated because dangerous, not because not used:
     * This method should never be called by you
     * and will most likely throw an unchecked Exception.
     */
    @Deprecated
    public static MethodHandle getHandle(int id) {
        MethodHandle handle = HANDLES.get(id);
        if (handle == null) {
            throw new IllegalStateException("ID: " + id + " MethodHandle didn't exist");
        }

        return handle;
    }

    static void del(int id) {
        HANDLES.remove(id);
    }

}
