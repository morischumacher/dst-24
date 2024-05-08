package dst.ass2.ioc.lock;

import dst.ass2.ioc.di.annotation.Component;
import javassist.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.InvocationTargetException;
import java.security.ProtectionDomain;

public class LockingInjector implements ClassFileTransformer {

    private final ClassPool pool;

    public LockingInjector() {
        this.pool = ClassPool.getDefault();
    }

    @Override
    public byte[] transform(ClassLoader loader, String className,
                            Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
        CtClass cl = getCtClass(classfileBuffer);

        if (!hasComponentAnnotation(cl)) {
            return classfileBuffer;
        }

        modifyMethods(cl);

        return getByteCode(cl);
    }

    private Object getLockAnnotation(CtMethod method) {
        try {
            return method.getAnnotation(Lock.class);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private CtClass getCtClass(byte[] classfileBuffer) {
        try {
            return pool.makeClass(new ByteArrayInputStream(classfileBuffer));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean hasComponentAnnotation(CtClass cl) {
        try {
            return cl.hasAnnotation(Component.class);
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    private void modifyMethods(CtClass cl) {
        for (CtMethod method : cl.getDeclaredMethods()) {
            Object annotation = getLockAnnotation(method);
            if (annotation != null) {
                String lockName = getLockName(annotation);
                insertLockCode(method, lockName);
            }
        }
    }

    private byte[] getByteCode(CtClass cl) {
        try {
            return cl.toBytecode();
        } catch (IOException | CannotCompileException e) {
            throw new RuntimeException(e);
        }
    }


    private String getLockName(Object annotation) {
        try {
            return (String) annotation.getClass().getMethod("value").invoke(annotation);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void insertLockCode(CtMethod method, String lockName) {
        try {
            String lockCode = "dst.ass2.ioc.lock.LockManager.getInstance().getLock(\"" + lockName + "\")";
            method.insertBefore(lockCode + ".lock();");
            method.insertAfter(lockCode + ".unlock(); return $_;", false);
            method.addCatch(lockCode + ".unlock(); throw $e;", pool.get("java.lang.Exception"));
        } catch (CannotCompileException | NotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
