package dst.ass2.aop.logging;

import java.lang.reflect.Field;
import java.util.logging.Logger;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

// Aspect-oriented programming (AOP) is a programming paradigm that aims to increase modularity by allowing the
// separation of cross-cutting concerns.
// Encapsulates behaviors that affect multiple classes into reusable modules.
@Aspect
public class LoggingAspect {

    // Around advice (pluginExecution()) runs before and after the method execution (expect @Invisible).
    @Around("execution(* dst.ass2.aop.IPluginExecutable.execute()) && !@annotation(Invisible)")
    public void pluginExecution(ProceedingJoinPoint joinPoint)
            throws Throwable {

        // JoinPoints represent points in the execution of a program, it allows to control the execution of the program.
        // Get the name of the class.
        String name = joinPoint.getTarget().getClass().getName();

        // Log the start and end of the method execution.
        logMessage(joinPoint, name + " started to execute");
        joinPoint.proceed();
        logMessage(joinPoint, name + " is finished");
    }

    private void logMessage(JoinPoint joinPoint, String message) {

        Logger log = null;

        try {
            // Try to find a logger in the target class.
            log = findLogger(joinPoint);
        } catch (IllegalAccessException e) {
        }
        // If found log a info message
        if (log != null) {
            log.info(message);
        } else {
            //Otherwise print the message to the console.
            System.out.println(message);
        }
    }

    // This method finds the logger in the target class.
    private Logger findLogger(JoinPoint joinPoint) throws IllegalAccessException {
        Logger logger;

        // Get the fields of the target class.
        Field[] fields = joinPoint.getTarget().getClass().getDeclaredFields();

        // Iterate over the fields to find the logger.
        for (Field field : fields) {
            if (Logger.class.isAssignableFrom(field.getType())) {
                // If private field, make it accessible.
                field.setAccessible(true);
                logger = (Logger) field.get(joinPoint.getTarget());
                return logger;
            }
        }
        return null;
    }
}