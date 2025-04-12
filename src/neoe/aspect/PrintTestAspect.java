package neoe.aspect;

import org.aspectj.lang.annotation.*;
import org.aspectj.lang.JoinPoint;
import java.util.ArrayList;
import java.util.List;

@Aspect
public class PrintTestAspect {
    private boolean drawStringCall = false;
    private boolean drawReturnCall = false;
    private boolean getTotalPagesCall = false;
    private boolean printCall = false;
    private boolean printPagesCall = false;

    // Track execution of individual methods in Print class
    @Before("execution(* neoe.ne.Print.getTotalPage(..))")
    public void trackTotalPageExecution(JoinPoint joinPoint) {
        this.getTotalPagesCall = true;
        System.out.println("Tracked Method: " + joinPoint.getSignature().getName());
    }

    @After("execution(* neoe.ne.Print.drawStringLine(..))")
    public void trackDrawStringExecution(JoinPoint joinPoint) {
        this.drawStringCall = true;
        System.out.println("Tracked Method: " + joinPoint.getSignature().getName());
    }

    @Before("execution(* neoe.ne.Print.drawReturn(..))")
    public void trackDrawReturnExecution(JoinPoint joinPoint) {
        this.drawReturnCall = true;
        System.out.println("Tracked Method: " + joinPoint.getSignature().getName());
    }

    @Before("execution(* neoe.ne.Print.print(..))")
    public void trackPrintExecution(JoinPoint joinPoint) {
        this.printCall = true;
        System.out.println("Tracked Method: " + joinPoint.getSignature().getName());
    }

    @Before("execution(* neoe.ne.Print.printPages(..))")
    public void trackPrintPagesExecution(JoinPoint joinPoint) {
        this.printPagesCall = true;
        System.out.println("Tracked Method: " + joinPoint.getSignature().getName());
    }


    @After("execution(* neoe.ne.Print.getTotalPage(..))")
    public void verifyTotalPageOrder() {
        if (!this.printPagesCall) {
            System.err.println("\n❌ getTotalPages execution order is incorrect!");
        } else {
            System.out.println("\n✅ Sequential getTotalPages passed!");
        }
    }

    @After("execution(* neoe.ne.Print.print(..))")
    public void verifyPrintOrder() {
        if (!this.getTotalPagesCall) {
            System.err.println("\n❌ print execution order is incorrect!");
        } else {
            System.out.println("\n✅ Sequential print passed!");
        }
    }

    @After("execution(* neoe.ne.Print.drawStringLine(..))")
    public void verifyDrawStringOrder() {
        if (!this.printCall) {
            System.err.println("\n❌ drawStringLine execution order is incorrect!");
        } else {
            System.out.println("\n✅ Sequential drawStringLine passed!");
        }
    }

    @After("execution(* neoe.ne.Print.drawReturn(..))")
    public void verifyDrawReturnOrder() {
        if (!this.drawStringCall) {
            System.err.println("\n❌ drawReturn execution order is incorrect!");
        } else {
            System.out.println("\n✅ Sequential drawReturn passed!");
        }
    }
}