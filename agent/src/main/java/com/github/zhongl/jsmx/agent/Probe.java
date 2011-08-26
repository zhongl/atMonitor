package com.github.zhongl.jsmx.agent;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.softee.management.annotation.*;
import org.softee.management.helper.*;

/**
 * {@link Probe}
 * 
 * @author <a href=mailto:zhong.lunfu@gmail.com>zhongl</a>
 * @created 2011-8-14
 * 
 */
public class Probe {

  public static String adviceName() {
    return advice().getClass().getSimpleName();
  }

  public static void onMethodBegin(String className,
                                   String methodName,
                                   String descriptor,
                                   Object thisObject,
                                   Object[] arguments) {
    boolean voidReturn = Type.getReturnType(descriptor).equals(Type.VOID_TYPE);
    Context context = new Context(className, methodName, voidReturn, thisObject, arguments);
    try {
      advice().enterWith(context);
    } catch (Throwable t) {
      handleEnterError(context, t);
    }
    if (context.isTimerOn()) context.startAt(System.nanoTime());
    if (context.isTraceOn()) context.setStackTrace(currentStrackTrace());
    CONTEXT_STACK.get().push(context);
  }

  public static void onMethodEnd(Object result,
                                 String className,
                                 String methodName,
                                 String descriptor,
                                 Object thisObject) {
    Context context = CONTEXT_STACK.get().pop();
    if (context.isTimerOn()) context.stopAt(System.nanoTime());
    context.exitWith(result);
    try {
      advice().exitWith(context);
    } catch (Throwable t) {
      handleExitError(context, t);
    }
  }

  public static void setAdvice(Advice instance) throws Exception {
    registerIfIsMBean(instance);
    unregisterIfIsMBean(advice.getAndSet(instance));
  }

  private static Advice advice() {
    return advice.get();
  }

  private static StackTraceElement[] currentStrackTrace() {
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    return Arrays.copyOfRange(stackTrace, 4, stackTrace.length); // trim useless stack trace elements.
  }

  private static void handleEnterError(Context context, Throwable t) {
    t.printStackTrace();
  }

  private static void handleExitError(Context context, Throwable t) {
    t.printStackTrace();
  }

  private static Method method(String name, Class<?>... argumentTypes) {
    try {
      return new Method(name, Type.getMethodDescriptor(Probe.class.getMethod(name, argumentTypes)));
    } catch (Exception e) {
      throw new Error(e);
    }
  }

  private static void registerIfIsMBean(Advice instance) throws Exception {
    if (instance.getClass().getAnnotation(MBean.class) != null) new MBeanRegistration(instance).register();
  }

  private static void unregisterIfIsMBean(Advice instance) throws Exception {
    if (instance.getClass().getAnnotation(MBean.class) != null) new MBeanRegistration(instance).unregister();
  }

  private static final ThreadLocal<Stack<Context>> CONTEXT_STACK = new ThreadLocal<Stack<Context>>() {
    @Override
    protected java.util.Stack<Context> initialValue() {
      return new Stack<Context>();
    }
  };

  public static final AtomicReference<Advice> advice = new AtomicReference<Advice>(new Trace());

  public static final Method ENTRY = method("onMethodBegin",
                                            String.class,
                                            String.class,
                                            String.class,
                                            Object.class,
                                            Object[].class);

  public static final Method EXIT = method("onMethodEnd",
                                           Object.class,
                                           String.class,
                                           String.class,
                                           String.class,
                                           Object.class);

  public static final Type TYPE = Type.getType(Probe.class);

  private Probe() {}
}
