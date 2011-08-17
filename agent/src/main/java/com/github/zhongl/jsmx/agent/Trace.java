package com.github.zhongl.jsmx.agent;

import java.text.*;

/**
 * {@link Trace}
 * 
 * @author <a href=mailto:zhong.lunfu@gmail.com>zhongl</a>
 * 
 */
public class Trace extends Advice {

  @Override
  public void enterWith(Context context) {
    println("{0}.{1} is calling with {2}.", context.getClassName(), context.getMethodName(), context.getArguments());
    context.setTraceOn();
  }

  @Override
  public void exitWith(Context context) {
    Object result = context.getResult();
    boolean breakByThrowing = result instanceof Throwable;
    String pattern = "{0}.{1} is called " + (breakByThrowing ? "but throw" : "and return") + " {2}.";
    println(pattern, context.getClassName(), context.getMethodName(), result);
    StackTraceElement[] stackTrace = (breakByThrowing) ? ((Throwable) result).getStackTrace() : context.getStackTrace();
    println(stackTrace);
  }

  private static void println(StackTraceElement[] stackTrace) {
    for (StackTraceElement stackTraceElement : stackTrace) {
      System.out.println("\t" + stackTraceElement);
    }
  }

  private static void println(String pattern, Object... arguments) {
    System.out.println(MessageFormat.format(pattern, arguments));
  }

}
