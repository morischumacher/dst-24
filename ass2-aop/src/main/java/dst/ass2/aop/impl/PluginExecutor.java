package dst.ass2.aop.impl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import dst.ass2.aop.IPluginExecutable;
import dst.ass2.aop.IPluginExecutor;

public class PluginExecutor implements IPluginExecutor {

    private volatile boolean running = true;
    private final Set<File> monitoredDirs = new HashSet<>();

    @Override
    public void monitor(File dir) {
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("File is not a directory: " + dir.getAbsolutePath());
        }
        monitoredDirs.add(dir);
    }

    @Override
    public void stopMonitoring(File dir) {
        monitoredDirs.remove(dir);
    }

    @Override
    public void start() {

        System.out.println("monitoredDirs: " + monitoredDirs.toString());

        Thread thread = new Thread(() -> {
            while (running) {
                try {
                    System.out.println("checking ...");
                    Thread.sleep(1000);

                    monitoredDirs.forEach(dir -> {
                        for (File file : dir.listFiles()) {
                            if (!file.isFile() || !file.getName().endsWith(".jar")) {
                                continue;
                            }
                            System.out.println(file.getAbsolutePath().toString());

                            executePluginInThread(file);
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    private void executePluginInThread(File file) {
        Thread pluginThread = new Thread(() -> {
            try (URLClassLoader classLoader = new URLClassLoader(new URL[] { file.toURI().toURL() });
                 JarFile jarFile = new JarFile(file)) {

                Enumeration<JarEntry> entries = jarFile.entries();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();

                    if (entry.getName().endsWith(".class")) {

                        String className = entry.getName().replace(".class", "").replace("/", ".");

                        System.out.println("className: " + className);

                        Class<?> clazz = classLoader.loadClass(className);

                        if (IPluginExecutable.class.isAssignableFrom(clazz)) {
                            try {
                                IPluginExecutable pluginExecutable = ((IPluginExecutable) clazz.getDeclaredConstructor().newInstance());

                                pluginExecutable.execute();

                            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        });

        pluginThread.start();
        try {
            pluginThread.join(); // Wait for plugin execution to finish before proceeding
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        running = false;
    }
}
