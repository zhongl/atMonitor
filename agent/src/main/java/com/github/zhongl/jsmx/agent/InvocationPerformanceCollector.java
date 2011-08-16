package com.github.zhongl.jsmx.agent;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.softee.management.annotation.*;

/**
 * {@link InvocationPerformanceCollector}
 * 
 * @author <a href=mailto:zhong.lunfu@gmail.com>zhongl</a>
 * @created 2011-8-16
 * 
 */
@MBean(objectName = "jsmx:type=PerformanceCollector")
public class InvocationPerformanceCollector extends Advice {

  @Override
  public void enterWith(Context context) {
    context.setTimeOn();
    context.setTraceOn();
  }

  @Override
  public void exitWith(Context context) {
    System.out.println(context.getClassName() + "." + context.getMethodName() + ": " + context.elapse() + "ns");
    StackTraceElement[] stackTrace = context.getStackTrace();
    for (StackTraceElement stackTraceElement : stackTrace) {
      System.out.println("\t" + stackTraceElement);
    }
  }

  static class Key {

    private final String className;
    private final String methodName;

    public Key(String className, String methodName) {
      this.className = className;
      this.methodName = methodName;
    }

    @Override
    public String toString() {
      return className + '.' + methodName;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((className == null) ? 0 : className.hashCode());
      result = prime * result + ((methodName == null) ? 0 : methodName.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      Key other = (Key) obj;
      if (className == null) {
        if (other.className != null) return false;
      } else if (!className.equals(other.className)) return false;
      if (methodName == null) {
        if (other.methodName != null) return false;
      } else if (!methodName.equals(other.methodName)) return false;
      return true;
    }

  }

  static class Value {

    private final AtomicLong count = new AtomicLong();
    private final AtomicLong totalElapse = new AtomicLong();

    public void increase(long elapse) {
      count.incrementAndGet();
      totalElapse.addAndGet(elapse);
    }

    public long average() {
      return totalElapse.get() / count.get();
    }

    public long count() {
      return count.get();
    }
  }
  
  public static void main(String[] args) {
    int[] is = {1,2,3};
    System.out.println(is.hashCode());
    System.out.println(is);
    System.out.println(Arrays.hashCode(is));
    
  }

  @Override
  public boolean matchs(String className, String methodName, Class<?>[] argumentTypes) {
    // TODO Auto-generated method stub
    return false;
  }

}
