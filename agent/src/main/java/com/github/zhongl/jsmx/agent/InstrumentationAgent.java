package com.github.zhongl.jsmx.agent;

import java.lang.instrument.*;

import org.softee.management.annotation.*;
import org.softee.management.helper.*;

/**
 * {@link InstrumentationAgent}
 * 
 * @author <a href=mailto:jushi@taobao.com>jushi</a>
 * @created 2011-8-5
 * 
 */
public class InstrumentationAgent {

  public static void premain(String agentArgs, Instrumentation instrumentation) throws Exception {
    System.out.println("loading...");
    new InstrumentationMBean(instrumentation);
    System.out.println("loaded instrumentation mbean.");
  }

  @MBean(objectName = "jsmx:type=Instrumentation")
  static class InstrumentationMBean {

    public InstrumentationMBean(Instrumentation instrumentation) throws Exception {
      this.instrumentation = instrumentation;
      instrumentation.addTransformer(Trace.transformer, true);
      new MBeanRegistration(this).register();
    }

    @ManagedOperation
    public String getAllLoadedClasses() {
      return toString(instrumentation.getAllLoadedClasses());
    }

    public String getInitiatedClasses(ClassLoader loader) {
      return toString(instrumentation.getInitiatedClasses(loader));
    }

    public long getObjectSize(Object objectToSize) {
      return instrumentation.getObjectSize(objectToSize);
    }

    @ManagedOperation
    public boolean isModifiableClass(String className) throws ClassNotFoundException {
      return instrumentation.isModifiableClass(Class.forName(className));
    }

    @ManagedOperation
    public boolean isRedefineClassesSupported() {
      return instrumentation.isRedefineClassesSupported();
    }

    @ManagedOperation
    public boolean isRetransformClassesSupported() {
      return instrumentation.isRetransformClassesSupported();
    }

    public void redefineClasses(ClassDefinition... definitions) throws ClassNotFoundException,
                                                               UnmodifiableClassException {
      instrumentation.redefineClasses(definitions);
    }

    @ManagedOperation
    public void retransformClasses(@Parameter("className") String className) throws Exception {

      try {
        Trace.transformer.addIncludeClassName(className);
        instrumentation.retransformClasses(toClasses(className));
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }

    private Class<?>[] toClasses(String... classNames) {
      final Class<?>[] classes = new Class<?>[classNames.length];
      for (int i = 0; i < classes.length; i++) {
        try {
          classes[i] = Class.forName(classNames[i]);
        } catch (final ClassNotFoundException e) {
          e.printStackTrace();
        }
      }
      return classes;
    }

    @SuppressWarnings("rawtypes")
    private String toString(Class[] classes) {
      final StringBuilder sb = new StringBuilder("" + classes.length);
      for (final Class clazz : classes) {
        sb.append("\n").append(clazz);
      }
      return sb.toString();
    }

    private final Instrumentation instrumentation;

  }

}
