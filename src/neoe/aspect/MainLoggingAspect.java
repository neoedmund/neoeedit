package neoe.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

@Aspect
public class MainLoggingAspect {
    @Before("execution(* neoe.ne.Main.doinit(..))")
    public void beforeDoinit(JoinPoint joinPoint) {
        System.out.println("Initializing application: " + joinPoint.getSignature());
    }

    @Before("execution(* neoe.ne.Main.main(..))")
    public void beforeMain(JoinPoint joinPoint) {
        System.out.println("Starting application: " + joinPoint.getSignature());
    }

    @Before("execution(* neoe.ne.Main.openDoc(..))")
    public void beforeOpenDoc(JoinPoint joinPoint) {
        System.out.println("Opening document: " + joinPoint.getSignature());
    }
}
