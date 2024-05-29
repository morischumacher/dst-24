package dst.ass2.ioc.lock;

import dst.ass2.ioc.di.annotation.Component;
import javassist.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.InvocationTargetException;
import java.security.ProtectionDomain;

/**
 * A class file transformer that injects locking code into methods annotated with {@link Lock}.
 * ClassFileTransformer allows it to transform the bytecode of loaded classes before they're defined by the JVM.
 *
 * Bytecode injection typically occurs during class loading or class definition.
 * When a class is loaded or redefined by the JVM, it passes through a class file transformer, which allows modification
 * of the class bytecode before it is defined in memory.
 */
public class LockingInjector implements ClassFileTransformer {

    // Set of CtClass objects (class in the Javassist library)
    private final ClassPool pool;

    public LockingInjector() {
        this.pool = ClassPool.getDefault();
    }

    // Transforms the byte code of a loaded class.
    @Override
    public byte[] transform(ClassLoader loader, String className,
                            Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {

        // Get the CtClass object for the class from the class byte code.
        CtClass cl = getCtClass(classfileBuffer);

        // Check if the class has the @Component annotation, if not return the class byte code.
        if (!hasComponentAnnotation(cl)) {
            return classfileBuffer;
        }
        // If yes then modify the methods of the class.
        modifyMethods(cl);

        return getByteCode(cl);
    }

    // Get the @Lock annotation of a method.
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

    // Checks if the class has the @Component annotation.
    private boolean hasComponentAnnotation(CtClass cl) {
        try {
            return cl.hasAnnotation(Component.class);
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    private void modifyMethods(CtClass cl) {
        // For each method in the class, check if it has the @Lock annotation.
        for (CtMethod method : cl.getDeclaredMethods()) {
            // Retrieve the @Lock annotation of the method, or RuntimeException if it does not exist.
            Object annotation = getLockAnnotation(method);
            if (annotation != null) {
                // Get the name of the lock from the @Lock annotation.
                String lockName = getLockName(annotation);
                // Insert the lock code into the method.
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


    // Get the name of the lock from the @Lock annotation.
    private String getLockName(Object annotation) {
        try {
            // Get the name of the lock from the @Lock annotation by calling the value method.
            return (String) annotation.getClass().getMethod("value").invoke(annotation);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void insertLockCode(CtMethod method, String lockName) {
        try {
            // Gets the lock code to be inserted into the method from the lock manager by the lock name.
            String lockCode = "dst.ass2.ioc.lock.LockManager.getInstance().getLock(\"" + lockName + "\")";
            // Insert lock code before the method.
            method.insertBefore(lockCode + ".lock();");
            // Insert unlock code after the method.
            method.insertAfter(lockCode + ".unlock(); return $_;", false);
            // Add a catch block to unlock the lock if an exception is thrown.
            method.addCatch(lockCode + ".unlock(); throw $e;", pool.get("java.lang.Exception"));
        } catch (CannotCompileException | NotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
