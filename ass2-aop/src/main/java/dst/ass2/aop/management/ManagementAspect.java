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

@Aspect
public class ManagementAspect {

    private final Map<IPluginExecutable, Timer> pluginTimers = new ConcurrentHashMap<>();

    @Pointcut("execution(void dst.ass2.aop.IPluginExecutable.execute())")
    public void executionMethodPointcut() {
    }

    @Before("executionMethodPointcut()")
    public void executionStart(JoinPoint joinPoint) {
        IPluginExecutable plugin = (IPluginExecutable) joinPoint.getTarget();

        long timeoutMs = getTimeout(plugin);

        TimerTask interruptTask = new TimerTask() {
            @Override
            public void run() {
                plugin.interrupted();
            }
        };

        Timer timer = new Timer();
        timer.schedule(interruptTask, timeoutMs);

        pluginTimers.put(plugin, timer);
    }

    @After("executionMethodPointcut()")
    public void executionEnds(JoinPoint joinPoint) {
        IPluginExecutable plugin = (IPluginExecutable) joinPoint.getTarget();
        Timer timer = pluginTimers.remove(plugin);
        if (timer != null) {
            timer.cancel();
        }
    }

    private long getTimeout(IPluginExecutable plugin) {
        long timeoutMs = 0;
        Timeout annotation = plugin.getClass().getAnnotation(Timeout.class);
        if (annotation != null) {
            timeoutMs = annotation.value();
        }
        return timeoutMs;
    }
}
