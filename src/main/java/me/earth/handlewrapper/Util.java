package me.earth.handlewrapper;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.DRETURN;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FRETURN;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.LRETURN;
import static org.objectweb.asm.Opcodes.RETURN;

final class Util {
    private Util() {
        throw new AssertionError();
    }

    public static boolean isSignaturePolymorphic(Method method) {
        for (Annotation annotation : method.getDeclaredAnnotations()) {
            // meh, theres definitely a better way to do this
            if (annotation.toString().contains("PolymorphicSignature")) {
                return true;
            }
        }

        return false;
    }

    public static void box(Type type, MethodVisitor mv) {
        String o; // owner
        String s; // signature

        switch (type.getSort()) {
            case Type.BOOLEAN:
                o = "java/lang/Boolean"; s = "(Z)Ljava/lang/Boolean;"; break;
            case Type.CHAR:
                o = "java/lang/Char";    s = "(C)Ljava/lang/Char;";    break;
            case Type.BYTE:
                o = "java/lang/Byte";    s = "(B)Ljava/lang/Byte;";    break;
            case Type.SHORT:
                o = "java/lang/Short";   s = "(S)Ljava/lang/Short;";   break;
            case Type.INT:
                o = "java/lang/Integer"; s = "(I)Ljava/lang/Integer;"; break;
            case Type.FLOAT:
                o = "java/lang/Float";   s = "(F)Ljava/lang/Float;";   break;
            case Type.LONG:
                o = "java/lang/Long";    s = "(J)Ljava/lang/Long;";    break;
            case Type.DOUBLE:
                o = "java/lang/Double";  s = "(D)Ljava/lang/Double;";  break;
            case Type.VOID:
                mv.visitInsn(ACONST_NULL);
            default:
                return;
        }

        mv.visitMethodInsn(INVOKESTATIC, o, "valueOf", s, false);
    }

    public static void unbox(Type type, MethodVisitor mv) {
        String o; // owner
        String n; // name
        String s; // signature

        switch (type.getSort()) {
            case Type.BOOLEAN:
                o = "java/lang/Boolean"; n = "booleanValue"; s = "()Z"; break;
            case Type.CHAR:
                o = "java/lang/Char";    n = "charValue";    s = "()C"; break;
            case Type.BYTE:
                o = "java/lang/Byte";    n = "byteValue";    s = "()B"; break;
            case Type.SHORT:
                o = "java/lang/Short";   n = "shortValue";   s = "()S"; break;
            case Type.INT:
                o = "java/lang/Integer"; n = "intValue";     s = "()I"; break;
            case Type.FLOAT:
                o = "java/lang/Float";   n = "floatValue";   s = "()F"; break;
            case Type.LONG:
                o = "java/lang/Long";    n = "longValue";    s = "()J"; break;
            case Type.DOUBLE:
                o = "java/lang/Double";  n = "doubleValue";  s = "()D"; break;
            default:
                mv.visitTypeInsn(CHECKCAST, type.getInternalName());
                return;
        }

        mv.visitTypeInsn(CHECKCAST, o);
        mv.visitMethodInsn(INVOKEVIRTUAL, o, n, s, false);
    }

    public static String buildHandleSignature(boolean isStatic, Class<?> owner, Class<?> rType, Class<?>...pTypes) {
        StringBuilder builder = new StringBuilder("(");
        if (!isStatic) {
            builder.append(Type.getDescriptor(owner));
        }

        for (Class<?> pType : pTypes) {
            builder.append(Type.getDescriptor(pType));
        }

        return builder.append(")").append(Type.getDescriptor(rType)).toString();
    }

    public static boolean exists(String clazz) {
        try {
            Class.forName(clazz, false, Util.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static void loadParam(MethodVisitor mv, Type type, int var) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                mv.visitVarInsn(ILOAD, var);
                return;
            case Type.FLOAT:
                mv.visitVarInsn(FLOAD, var);
                return;
            case Type.LONG:
                mv.visitVarInsn(LLOAD, var);
                return;
            case Type.DOUBLE:
                mv.visitVarInsn(DLOAD, var);
            case Type.VOID:
                return;
            default:
                mv.visitVarInsn(ALOAD, var);
        }
    }

    public static void makeReturn(MethodVisitor mv, Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                mv.visitInsn(IRETURN);
                return;
            case Type.FLOAT:
                mv.visitInsn(FRETURN);
                return;
            case Type.LONG:
                mv.visitInsn(LRETURN);
                return;
            case Type.DOUBLE:
                mv.visitInsn(DRETURN);
                return;
            case Type.VOID:
                mv.visitInsn(RETURN);
                return;
            default:
                mv.visitInsn(ARETURN);
        }
    }

    public static String[] internalTypeArray(Class<?>...types) {
        String[] result = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            result[i] = Type.getInternalName(types[i]);
        }

        return result;
    }

}
