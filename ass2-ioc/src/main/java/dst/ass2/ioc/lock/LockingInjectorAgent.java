package dst.ass2.ioc.lock;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;

public class LockingInjectorAgent {
    // Defined a java agent in the pom.xml.
    // The premain method is called before the main method is called for setting uo the JVM.
    public static void premain(String args, Instrumentation inst) {
        // Add a transformer to the JVM.
        ClassFileTransformer transformer = new LockingInjector();
        // Register the transformer.
        inst.addTransformer(transformer);
    }

}