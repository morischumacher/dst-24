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

// Managing the execution of plugins within an application or system.
// Monitors specified directories for new plugin files, when new plugins are detected,
// it loads them dynamically into the application's runtime environment.
public class PluginExecutor implements IPluginExecutor {

    // Indicates whether the plugin executor is active.
    private volatile boolean isActive = true;

    // The directories to monitor for new plugins.
    private final Set<File> watchedDirectories = new HashSet<>();

    // The executor service to manage a pool of threads for executing plugins.
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    // The logger for this class.
    private static final Logger logger = Logger.getLogger(PluginExecutor.class.getName());

    // Sets the directory to monitor for new plugins.
    @Override
    public void monitor(File dir) {
        validateDirectory(dir);
        watchedDirectories.add(dir);
    }

    // Validates that the specified file is a directory.
    private void validateDirectory(File dir) {
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("File is not a directory: " + dir.getAbsolutePath());
        }
    }

    // Removes the specified directory from monitoring.
    @Override
    public void stopMonitoring(File dir) {
        watchedDirectories.remove(dir);
    }

    // Starts the plugin executor.
    @Override
    public void start() {
        logger.info("Watched directories: " + watchedDirectories.toString());
        startMonitoringThread();
    }

    // Starts a thread that monitors the watched directories for new plugins.
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


    // Checks the watched directories for new plugins.
    private void checkForNewPlugins() throws InterruptedException {
        logger.info("Checking for new plugins...");

        // Delay for each checking
        Thread.sleep(1000);

        // An ExecutorCompletionService (java concurrency) is being created with an ExecutorService. The
        // ExecutorCompletionService is a implementation of CompletionService that uses the provided Executor to execute
        // tasks.
        // When submitting to ab ExecutorService you get a Future object for each task submitted.
        // CompletionService are storing the Future objects in a queue in the order they are completed.
        CompletionService<Void> completionService = new ExecutorCompletionService<>(executorService);

        // For each file in the watched directories, check if there are any new plugins.
        watchedDirectories.forEach(dir -> {
            for (File file : dir.listFiles()) {
                if (isPlugin(file)) {
                    logger.info("Found plugin: " + file.getAbsolutePath());
                    // if so submit the plugin for execution
                    submitPluginForExecution(completionService, file);
                }
            }
        });

        waitForAllTasksToComplete(completionService);
    }

    // Checks if the specified file is a plugin (is a file end ends with .jar).
    private boolean isPlugin(File file) {
        return file.isFile() && file.getName().endsWith(".jar");
    }

    // Submits the specified plugin file for execution.
    private void submitPluginForExecution(CompletionService<Void> completionService, File file) {
        // Check if the executor service is not shutdown.
        if (!executorService.isShutdown()) {
            // Submit the plugin for to the completion service which uses the executor service to execute the plugin.
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


    // Loads the plugin file as a jar file and executes each class.
    private void executePlugin(File file) {
        // Try to load the plugin file as a jar file.
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{file.toURI().toURL()});
             JarFile jarFile = new JarFile(file)) {

            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                // If the entry is a class file, execute the class.
                if (entry.getName().endsWith(".class")) {
                    // Provide the entry and the classloader to the executeClass method.
                    executeClass(classLoader, entry);
                }
            }
        } catch (IOException e) {
            logger.severe("Error loading plugin: " + e.getMessage());
        }
    }


    // Executes the specified class file, gets the har file and the entry (class).
    private void executeClass(URLClassLoader classLoader, JarEntry entry) {

        // Transfer to fully qualified class name since they are stored like that in the jar file.
        String className = entry.getName().replace(".class", "").replace("/", ".");

        logger.info("Loading class: " + className);

        try {
            // Load the class from the classloader directly from the jar file using the qualified class name.
            Class<?> clazz = classLoader.loadClass(className);

            // If it's a @PluginExecutable class, instantiate and execute the plugin.
            if (IPluginExecutable.class.isAssignableFrom(clazz)) {
                instantiateAndExecutePlugin(clazz);
            }
        } catch (ClassNotFoundException e) {
            logger.severe("Error loading class: " + e.getMessage());
        }
    }

    private void instantiateAndExecutePlugin(Class<?> clazz) {
        try {
            // Instantiate the class and execute the plugin.
            IPluginExecutable pluginExecutable = ((IPluginExecutable) clazz.getDeclaredConstructor().newInstance());
            pluginExecutable.execute();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            logger.severe("Error instantiating plugin: " + e.getMessage());
        }
    }

    // Stops the plugin executor.
    @Override
    public void stop() {
        isActive = false;
        executorService.shutdown();
    }
}