package com.github.zhongl.jsmx.agent;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;

/**
 * {@link Probe}
 * 
 * @author <a href=mailto:zhong.lunfu@gmail.com>zhongl</a>
 * @created 2011-8-14
 * 
 */
public class Probe {

  public static void onMethodBegin(String className,
                                   String methodName,
                                   String descriptor,
                                   Object thisObject,
                                   Object[] arguments) {
    System.out.println("before -> " + new Context(className, methodName, descriptor, thisObject, arguments));
  }

  public static void onMethodEnd(Object result,
                                 String className,
                                 String methodName,
                                 String descriptor,
                                 Object thisObject) {
    System.out.println("end -> " + new Context(className, methodName, descriptor, thisObject, result));
  }

  private static Method method(String name, Class<?>... argumentTypes) {
    try {
      return new Method(name, Type.getMethodDescriptor(Probe.class.getMethod(name, argumentTypes)));
    } catch (Exception e) {
      throw new Error(e);
    }
  }

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

  public static class Context {

    private Context(String className, String methodName, String descriptor, Object thisObject, Object resultOrArguments) {
      this.className = className.replace('/', '.');
      this.methodName = methodName;
      this.descriptor = descriptor;
      this.thisObject = thisObject;
      this.resultOrArguments = resultOrArguments;
    }

    public StackTraceElement[] stack() {
      return Thread.currentThread().getStackTrace();
    }

    @Override
    public String toString() {
      return String.format("Context [className=%s, methodName=%s, descriptor=%s, thisObject=%s, resultOrArguments=%s]",
                           className,
                           methodName,
                           descriptor,
                           thisObject,
                           resultOrArguments);
    }

    private final String className;
    private final String methodName;
    private final String descriptor;
    private final Object thisObject;
    private final Object resultOrArguments;
  }
}
