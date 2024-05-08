package dst.ass2.aop.impl;

import dst.ass2.aop.IPluginExecutable;
import dst.ass2.aop.IPluginExecutor;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

public class PluginExecutor implements IPluginExecutor {

    private volatile boolean isActive = true;
    private final Set<File> watchedDirectories = new HashSet<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private static final Logger logger = Logger.getLogger(PluginExecutor.class.getName());

    @Override
    public void monitor(File dir) {
        validateDirectory(dir);
        watchedDirectories.add(dir);
    }

    private void validateDirectory(File dir) {
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("File is not a directory: " + dir.getAbsolutePath());
        }
    }

    @Override
    public void stopMonitoring(File dir) {
        watchedDirectories.remove(dir);
    }

    @Override
    public void start() {
        logger.info("Watched directories: " + watchedDirectories.toString());
        startMonitoringThread();
    }

    private void startMonitoringThread() {
        Thread thread = new Thread(() -> {
            while (isActive) {
                try {
                    checkForNewPlugins();
                } catch (InterruptedException e) {
                    logger.severe("Plugin execution interrupted: " + e.getMessage());
                }
            }
        });
        thread.start();
    }

    private void checkForNewPlugins() throws InterruptedException {
        logger.info("Checking for new plugins...");
        Thread.sleep(1000);

        CompletionService<Void> completionService = new ExecutorCompletionService<>(executorService);

        watchedDirectories.forEach(dir -> {
            for (File file : dir.listFiles()) {
                if (isPlugin(file)) {
                    logger.info("Found plugin: " + file.getAbsolutePath());
                    submitPluginForExecution(completionService, file);
                }
            }
        });

        waitForAllTasksToComplete(completionService);
    }

    private boolean isPlugin(File file) {
        return file.isFile() && file.getName().endsWith(".jar");
    }

    private void submitPluginForExecution(CompletionService<Void> completionService, File file) {
        if (!executorService.isShutdown()) {
            completionService.submit(() -> {
                executePlugin(file);
                return null;
            });
        }
    }

    private void waitForAllTasksToComplete(CompletionService<Void> completionService) {
        for (int i = 0; i < watchedDirectories.size(); i++) {
            try {
                completionService.take().get();
            } catch (InterruptedException | ExecutionException e) {
                logger.severe("Error executing plugin: " + e.getMessage());
            }
        }
    }

    private void executePlugin(File file) {
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{file.toURI().toURL()});
             JarFile jarFile = new JarFile(file)) {

            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                if (entry.getName().endsWith(".class")) {
                    executeClass(classLoader, entry);
                }
            }
        } catch (IOException e) {
            logger.severe("Error loading plugin: " + e.getMessage());
        }
    }

    private void executeClass(URLClassLoader classLoader, JarEntry entry) {
        String className = entry.getName().replace(".class", "").replace("/", ".");

        logger.info("Loading class: " + className);

        try {
            Class<?> clazz = classLoader.loadClass(className);

            if (IPluginExecutable.class.isAssignableFrom(clazz)) {
                instantiateAndExecutePlugin(clazz);
            }
        } catch (ClassNotFoundException e) {
            logger.severe("Error loading class: " + e.getMessage());
        }
    }

    private void instantiateAndExecutePlugin(Class<?> clazz) {
        try {
            IPluginExecutable pluginExecutable = ((IPluginExecutable) clazz.getDeclaredConstructor().newInstance());
            pluginExecutable.execute();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            logger.severe("Error instantiating plugin: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        isActive = false;
        executorService.shutdown();
    }
}