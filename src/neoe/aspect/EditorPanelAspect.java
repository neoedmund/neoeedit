package neoe.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class EditorPanelAspect {
    private boolean pageInitialized = false;
    private boolean windowOpened = false;

    // Track when a page is set
    @After("execution(* neoe.ne.EditorPanel.setPage(..))")
    public void afterSetPage(JoinPoint joinPoint) {
        pageInitialized = true;
        System.out.println("Page has been initialized: " + joinPoint.getSignature());
    }

    @After("execution(* neoe.ne.EditorPanel.openWindow(..))")
    public void afterOpenWindow(JoinPoint joinPoint) {
        windowOpened = true; // set windowOpened to check for window listener later
        if(pageInitialized) {
            System.out.println("openWindow called: " + joinPoint.getSignature());
        } else {
            System.out.println("openWindow called but no page has been initialized");
        }
    }

    // MouseDragged must be called after page is initialized (setPage)
    @After("execution(* neoe.ne.EditorPanel.mouseDragged(..))")
    public void afterMouseMoved(JoinPoint joinPoint) {
        if(pageInitialized) {
            System.out.println("mouseDragged called: " + joinPoint.getSignature());
        } else {
            System.out.println("mouseDragged called but no page has been initialized");
        }
    }

    @After("execution(* neoe.ne.EditorPanel.installWindowListener(..))")
    public void afterInstallWindowListener(JoinPoint joinPoint) {
        if(windowOpened) {
            System.out.println("installWindowListener called: " + joinPoint.getSignature());
        } else {
            System.out.println("installWindowListener called but no window has been initialized");
        }
    }


}

