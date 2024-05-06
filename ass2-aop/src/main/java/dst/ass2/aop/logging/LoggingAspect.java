package dst.ass2.aop.logging;

import java.lang.reflect.Field;
import java.util.logging.Logger;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class LoggingAspect {

    @Around("execution(* dst.ass2.aop.IPluginExecutable.execute()) && !@annotation(Invisible)")
    public void pluginExecution(ProceedingJoinPoint joinPoint)
            throws Throwable {

        String name = joinPoint.getTarget().getClass().getName();

        logMessage(joinPoint, name + " started to execute");
        joinPoint.proceed();
        logMessage(joinPoint, name + " is finished");
    }

    private void logMessage(JoinPoint joinPoint, String message) {

        Logger log = null;

        try {
            log = findLogger(joinPoint);
        } catch (IllegalAccessException e) {
        }
        if (log != null) {
            log.info(message);
        } else {
            System.out.println(message);
        }
    }

    private Logger findLogger(JoinPoint joinPoint) throws IllegalAccessException {
        Logger logger;
        Field[] fields = joinPoint.getTarget().getClass().getDeclaredFields();
        for (Field field : fields) {
            if (Logger.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                logger = (Logger) field.get(joinPoint.getTarget());
                return logger;
            }
        }
        return null;
    }
}