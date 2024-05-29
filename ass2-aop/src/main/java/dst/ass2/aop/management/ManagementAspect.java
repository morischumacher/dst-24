package dst.ass2.aop.management;

import dst.ass2.aop.IPluginExecutable;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

// Aspect-oriented programming (AOP) is a programming paradigm that aims to increase modularity by allowing the
// separation of cross-cutting concerns.
// Encapsulates behaviors that affect multiple classes into reusable modules.
@Aspect
public class ManagementAspect {

    // ConcurrentHashMap is a thread-safe implementation of Map, by dividing the map into segments.
    // The stored plugins and their timers.
    private final Map<IPluginExecutable, Timer> pluginTimers = new ConcurrentHashMap<>();

    // Around advice (executionMethodPointcut()) runs before and after the method execution (expect @Invisible).
    @Pointcut("execution(void dst.ass2.aop.IPluginExecutable.execute())")
    public void executionMethodPointcut() {
    }

    // Before advice runs before the method executionMethodPointcut().
    @Before("executionMethodPointcut()")
    public void executionStart(JoinPoint joinPoint) {
        // Cast instance of plugin to IPluginExecutable (safe because of the pointcut).
        IPluginExecutable plugin = (IPluginExecutable) joinPoint.getTarget();

        // Gets the timeout value from the plugin.
        long timeoutMs = getTimeout(plugin);

        // Create a new TimerTask that interrupts the plugin.
        TimerTask interruptTask = new TimerTask() {
            @Override
            public void run() {
                plugin.interrupted();
            }
        };

        Timer timer = new Timer();

        // Schedule the interruptTask to run after the timeout.
        timer.schedule(interruptTask, timeoutMs);

        // Store the plugin and its timer.
        pluginTimers.put(plugin, timer);
    }

    // After advice runs after the method executionMethodPointcut().
    @After("executionMethodPointcut()")
    public void executionEnds(JoinPoint joinPoint) {

        // Create a new TimerTask that interrupts the plugin.
        IPluginExecutable plugin = (IPluginExecutable) joinPoint.getTarget();

        // Gets the timeout value from the plugin and remove if present.
        Timer timer = pluginTimers.remove(plugin);
        if (timer != null) {
            // Cancel the timer.
            timer.cancel();
        }
    }

    private long getTimeout(IPluginExecutable plugin) {
        long timeoutMs = 0;
        // Check if the plugin has a Timeout annotation.
        Timeout annotation = plugin.getClass().getAnnotation(Timeout.class);
        if (annotation != null) {
            // If it does, get the value from the annotation.
            timeoutMs = annotation.value();
        }
        return timeoutMs;
    }
}
