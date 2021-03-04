package me.earth.handlewrapper;

import me.earth.handlewrapper.wrappers.HandleWrapper;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.objectweb.asm.Opcodes.AALOAD;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACC_VARARGS;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_6;

/**
 * The WrapperFactory that produces {@link HandleWrapper}s or
 * custom HandleWrappers.
 *
 * MethodHandles are basically as fast as direct invocation,
 * as long as they are <b>static final</b>. This limits them,
 * because we can't create MethodHandles dynamically and have
 * them have the same performance as <b>static final</b> ones.
 *
 * So the idea is as follows: Use ASM to create a class at
 * runtime that has the given {@link MethodHandle} as a
 * <b>static final</b> field. What happens in the background
 * is roughly this:
 *
 * <blockquote><pre>{@code
 * public class ExampleClass {
 *     // Method to wrap:
 *     public ExampleClass exampleMethod(int i) { ... }
 * }
 *
 * public class SomeCreatedNameID+ implements HandleWrapper {
 *     private static final MethodHandle HANDLE;
 *
 *     static {
 *         HANDLE = Handles.getHandle(id); // ID comes from LDC instruction.
 *     }
 *
 *     public Object invoke(Object...args) throws Throwable {
 *         return HANDLE.invoke((ExampleClass) args[0], (int) args[1]);
 *     }
 *
 *     ...
 * }
 * </pre></blockquote>
 */
public class WrapperFactory {
    private static final AtomicInteger ID = new AtomicInteger();

    private WrapperFactory() {
        throw new AssertionError();
    }

    /**
     * Wraps a {@link MethodHandles.Lookup#unreflectGetter(Field)}
     * or similar. Will call the wrap method for following arguments:
     * <p>-for the handle,
     * <p>-the fields declaring class,
     * <p>-<tt>true</tt> if the field is static
     * <p>-the fields type as return type.
     * <p>
     * <p>The invoke method needs to be called with the Object the field
     * belongs to as first argument, unless the field is static and
     * no other arguments:
     *
     * <blockquote><pre>{@code
     *      public class SomeClass {
     *          private int foo = 5;
     *      }
     *
     *      SomeClass target = new SomeClass();
     *      HandleWrapper wrapper = ... // Wrapper for MethodHandle for "foo"
     *      int foo = wrapper.invoke(target); // gets the value for foo
     *
     * }</pre></blockquote>
     *
     * @param handle the handle to wrap.
     * @param field the field we want to access.
     * @return a {@link HandleWrapper} for the Handle.
     * @throws Throwable so much can go wrong when creating a class with ASM.
     */
    // Could return some sort of Getter interface here instead.
    public static HandleWrapper wrapGetter(MethodHandle handle, Field field) throws Throwable {
        boolean isStatic = Modifier.isStatic(field.getModifiers());
        return wrap(handle, field.getDeclaringClass(), isStatic, field.getType());
    }

    /**
     * Wraps a {@link MethodHandles.Lookup#unreflectSetter(Field)}
     * or similar. Will call the wrap method for following arguments:
     * <p>-the handle
     * <p>-the fields declaring class,
     * <p>-<tt>true</tt> if the field is static,
     * <p>-void.class as return type,
     * <p>-the fields type as the first parameter type.
     * <p>
     * <p>The invoke method needs to be called with the Object the field
     * belongs to as first argument, unless the field is static, and
     * the value the field should be set to as second (or first if the
     * field is static):
     *
     * <blockquote><pre>{@code
     *      public class SomeClass {
     *          private int foo = 5;
     *      }
     *
     *      SomeClass target = new SomeClass();
     *      HandleWrapper wrapper = ... // Wrapper for MethodHandle for "foo".
     *      wrapper.invoke(target, 10); // sets foo's value to 10.
     *
     * }</pre></blockquote>
     *
     * @param handle the handle to wrap.
     * @param field the field we want to access.
     * @return a {@link HandleWrapper} for the Handle.
     * @throws Throwable so much can go wrong when creating a class with ASM.
     */
    // Could return some sort of Setter interface here instead.
    public static HandleWrapper wrapSetter(MethodHandle handle, Field field) throws Throwable {
        boolean isStatic = Modifier.isStatic(field.getModifiers());
        return wrap(handle, field.getDeclaringClass(), isStatic, void.class, field.getType());
    }

    /**
     * Wraps a {@link MethodHandles.Lookup#unreflectConstructor(Constructor)}
     * or similar. Will call the wrap method for following arguments:
     * <p>-the handle,
     * <p>-the constructors declaring class,
     * <p>-always <tt>true</tt>, this is really important!
     * <p>-the constructors declaring class as return type,
     * <p>-and the constructors parameter types as parameter types.
     * <p>
     * <p>Calling a wrapped ConstructorHandle should be done like this:
     * <blockquote><pre>{@code
     *      public class SomeClass {
     *          private SomeClass(int i) { ... }
     *      }
     *
     *      HandleWrapper wrapper = ... // Wrapper for the Constructor.
     *      SomeClass someClass = wrapper.invoke(5);
     *
     * }</pre></blockquote>
     *
     * @param handle the handle to wrap.
     * @param constructor the constructor we want to access.
     * @return a {@link HandleWrapper} for the Handle.
     * @throws Throwable so much can go wrong when creating a class with ASM.
     */
    public static HandleWrapper wrapConstructor(MethodHandle handle, Constructor<?> constructor) throws Throwable {
        return wrap(handle, constructor.getDeclaringClass(), true, constructor.getDeclaringClass(), constructor.getParameterTypes());
    }

    /**
     * Wraps a {@link MethodHandle} for a {@link Method}.
     * Will call the wrap method for following arguments:
     * <p>-the handle,
     * <p>-the methods declaring class,
     * <p>-<tt>true</tt>, if the method is static
     * <p>-the methods return type,
     * <p>-and methods parameter types.
     * <p>
     * <p>Calling a wrapped Method should be done like this:
     * <blockquote><pre>{@code
     *      public class SomeClass {
     *          private int someMethod(String text, int i) { ... }
     *      }
     *
     *      SomeClass target = new SomeClass();
     *      HandleWrapper wrapper = ... // Wrapper for the Method.
     *      int someInt = (int) wrapper.invoke(target, "Text", 10);
     * }</pre></blockquote>
     * <p>For static methods no target is required:
     * <blockquote><pre>{@code
     *      public class SomeClass {
     *          private static int someStaticMethod(String text, int i) { ... }
     *      }
     *
     *      SomeClass target = new SomeClass();
     *      HandleWrapper wrapper = ... // Wrapper for the Method.
     *      int someInt = (int) wrapper.invoke("SomeText", 10);
     * }</pre></blockquote>
     *
     * @param handle the MethodHandle to wrap.
     * @param method the method
     * @return a {@link HandleWrapper} for the Handle.
     * @throws Throwable so much can go wrong when creating a class with ASM.
     */
    public static HandleWrapper wrap(MethodHandle handle, Method method) throws Throwable {
        return wrap(handle, method.getDeclaringClass(), Modifier.isStatic(method.getModifiers()), method.getReturnType(), method.getParameterTypes());
    }

    /**
     * Calls {@link WrapperFactory#wrap(MethodHandle, Class, boolean, Class, ClassLoader, Class[])}
     * for all arguments and the owners classloader.
     */
    public static HandleWrapper wrap(MethodHandle handle, Class<?> owner, boolean staticOrCtr, Class<?> rType, Class<?>...pTypes) throws Throwable {
        return wrap(handle, owner, staticOrCtr, rType, owner.getClassLoader(), pTypes);
    }

    /**
     * Wraps a {@link MethodHandle} into a {@link HandleWrapper}.
     *
     * @param handle the MethodHandle to wrap.
     * @param owner the owner of the target of the handle.
     * @param staticOrCtr if the target is static or a constructor.
     * @param rType the returnType of the handle.
     * @param classLoader the classLoader to load the HandleWrapper with.
     * @param pTypes the parameterTypes of the handle
     *               (shouldn't include the target class).
     * @return a HandleWrapper for the given handle.
     * @throws Throwable so much can go wrong when creating a class with ASM.
     */
    public static HandleWrapper wrap(MethodHandle handle, Class<?> owner, boolean staticOrCtr, Class<?> rType, ClassLoader classLoader, Class<?>...pTypes) throws Throwable {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        int id = ID.incrementAndGet();
        String[] nameAndDescr = begin(cw, id, owner, staticOrCtr, rType, pTypes, Type.getInternalName(HandleWrapper.class));
        String name = nameAndDescr[0];
        String description = nameAndDescr[1];

        initAndClinit(cw, id, description);

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_VARARGS, "invoke", "([Ljava/lang/Object;)Ljava/lang/Object;", null, new String[]{"java/lang/Throwable"});
        buildHandleMethod("invoke", description, mv, owner, rType, staticOrCtr, pTypes);

        mv = cw.visitMethod(ACC_PUBLIC | ACC_VARARGS, "invokeExact", "([Ljava/lang/Object;)Ljava/lang/Object;", null, new String[]{"java/lang/Throwable"});
        buildHandleMethod("invokeExact", description, mv, owner, rType, staticOrCtr, pTypes);

        cw.visitEnd();
        Handles.add(id, handle);

        try {
            Class<?> wrapperClass = new WrapperLoader(classLoader).define(name, cw.toByteArray());
            return (HandleWrapper) wrapperClass.newInstance();
        } finally {
            Handles.del(id);
        }
    }

    /**
     * Implements the given interface.
     * The given Maps keys specify the methods of the interface and
     * the given Maps values specify the corresponding method of
     * {@link MethodHandle}, that should be called. For all non
     * SignaturePolymorphic methods it's important that the Signature
     * of the interface method is exactly the same as the targeted one.
     * <p>
     * <p>The arguments need to follow these rules:
     * <p>-they can't be null and the class needs to be an interface,
     * <p>-interfaces with generic types aren't supported (TODO: yet?),
     * <p>-All methods in the links map need to be:
     * <p>  -notNull
     * <p>  -public
     * <p>  -key methods need to belong to the interface
     * <p>  -value methods need to belong to the MethodHandle class.
     * <p>  -key methods can't be static.
     *
     * @param around the interface to wrap the handle into.
     * @param links which methods of the interface should be mapped
     *              to which of the handle.
     * @param handle the handle to wrap.
     * @param <T> type of the interface.
     * @return an instance of the given class.
     * @throws Throwable so much can go wrong when creating a class with ASM.
     */
    @SuppressWarnings("unchecked")
    public static <T> T wrap(Class<T> around, Map<Method, Method> links, MethodHandle handle) throws Throwable {
        if (around == null || !around.isInterface()) {
            throw new IllegalArgumentException("Given class needs to be an interface.");
        } else if (around.getTypeParameters().length > 0) {
            throw new IllegalArgumentException("Generic Interfaces aren't supported.");
        }

        for (Map.Entry<Method, Method> entry : links.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                throw new IllegalArgumentException("A Method was null.");
            } else if (!entry.getKey().getDeclaringClass().isAssignableFrom(around)) {
                throw new IllegalArgumentException("Method: " + entry.getKey().getName() + " is not part of " + around.getName() + ".");
            } else if (!Modifier.isPublic(entry.getValue().getModifiers())) {
                throw new IllegalArgumentException("Method: " + entry.getValue().getName() + " is not public.");
            } else if (entry.getValue().getDeclaringClass() != MethodHandle.class) {
                throw new IllegalArgumentException("Method: " + entry.getValue().getName() + " doesn't belong to the MethodHandle class.");
            } else if (Modifier.isStatic(entry.getKey().getModifiers())) {
                throw new IllegalArgumentException("Method: " + entry.getKey().getName() + " is static, can't be overridden.");
            }
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        int id = ID.incrementAndGet();
        String[] nameAndDescr = begin(cw, id, around, false, null, new Class<?>[]{}, Type.getInternalName(around));
        String name = nameAndDescr[0];
        String description = nameAndDescr[1];

        initAndClinit(cw, id, description);
        for (Map.Entry<Method, Method> entry : links.entrySet()) {
            link(cw, entry.getKey(), entry.getValue(), description);
        }

        cw.visitEnd();
        Handles.add(id, handle);

        try {
            Class<?> wrapperClass = new WrapperLoader(around.getClassLoader()).define(name, cw.toByteArray());
            return (T) wrapperClass.newInstance();
        } finally {
            Handles.del(id);
        }
    }

    private static void link(ClassWriter cw, Method from, Method to, String description) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, from.getName(), Type.getType(from).getDescriptor(), null, Util.internalTypeArray(from.getExceptionTypes()));
        mv.visitCode();

        if (Util.isSignaturePolymorphic(to)) { // special case, signature will be customized
            mv.visitFieldInsn(GETSTATIC, description, "HANDLE", "Ljava/lang/invoke/MethodHandle;");
            for (int i = 0; i < from.getParameterTypes().length; i++) {
                Util.loadParam(mv, Type.getType(from.getParameterTypes()[i]), i + 1);
            }

            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", to.getName(), Type.getType(from).getDescriptor(), false);
            Util.makeReturn(mv, Type.getType(from.getReturnType()));
            mv.visitMaxs(0, 0);
            mv.visitEnd();
            return;
        }

        boolean isStatic = Modifier.isStatic(to.getModifiers());
        if (!isStatic) {
            mv.visitFieldInsn(GETSTATIC, description, "HANDLE", "Ljava/lang/invoke/MethodHandle;");
        }

        for (int i = 0; i < from.getParameterTypes().length; i++) {
            Util.loadParam(mv, Type.getType(from.getParameterTypes()[i]), isStatic ? i : i + 1);
        }

        mv.visitMethodInsn(isStatic ? INVOKESTATIC : INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", to.getName(), Type.getType(to).getDescriptor(), false);
        Util.makeReturn(mv, Type.getType(to.getReturnType()));
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static String[] begin(ClassWriter cw, int id, Class<?> owner, boolean staticOrCtr, Class<?> rType, Class<?>[] pTypes, String...interfaces) {
        String name = getName(id, owner, staticOrCtr, rType, pTypes);
        while (Util.exists(name)) {
            name = name + id;
        }

        String description = name.replace(".", "/");

        // Create Implementation of MethodWrapper.
        cw.visit(V1_6, ACC_PUBLIC | ACC_SUPER, description, null, "java/lang/Object", interfaces);
        // Create private static final MethodHandle field.
        cw.visitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, "HANDLE", "Ljava/lang/invoke/MethodHandle;", null, null).visitEnd();

        return new String[]{name, description};
    }

    private static void initAndClinit(ClassWriter cw, int id, String description) {
        // Static Initializer
        MethodVisitor mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        mv.visitLdcInsn(id);
        mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Handles.class), "getHandle", "(I)Ljava/lang/invoke/MethodHandle;", false);
        mv.visitFieldInsn(PUTSTATIC, description, "HANDLE", "Ljava/lang/invoke/MethodHandle;");
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // Default Ctr
        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    private static void buildHandleMethod(String name, String description, MethodVisitor mv, Class<?> owner, Class<?> rType, boolean staticOrCtr, Class<?>...pTypes) {
        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, description, "HANDLE", "Ljava/lang/invoke/MethodHandle;");

        if (!staticOrCtr) {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitInsn(ICONST_0);
            mv.visitInsn(AALOAD);
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(owner));
        }

        for (int i = 0; i < pTypes.length; i++) {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitLdcInsn(staticOrCtr ? i : i + 1);
            mv.visitInsn(AALOAD);
            Util.unbox(Type.getType(pTypes[i]), mv);
        }

        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", name, Util.buildHandleSignature(staticOrCtr, owner, rType, pTypes), false);
        Util.box(Type.getType(rType), mv);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static String getName(int id, Class<?> owner, boolean staticOrCtr, Class<?> rType, Class<?>...pTypes) {
        StringBuilder builder = new StringBuilder(owner.getName());
        if (staticOrCtr) {
            builder.append("_static");
        }

        for (Class<?> pType : pTypes) {
            builder.append("_").append(pType.getSimpleName());
        }

        return builder.append("_").append(rType == null ? "" : rType.getSimpleName()).append(id).toString();
    }

}
