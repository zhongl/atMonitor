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

  private final Aggregation<Category, Statistics> aggregation = new Aggregation<Category, Statistics>() {

    @Override
    protected Statistics initialValue() {
      return new Statistics();
    }
  };

  @Override
  public void enterWith(Context context) {
    context.setTimeOn();
    context.setTraceOn();
  }

  @Override
  public void exitWith(Context context) {
    aggregation.get(new Category(context.getClassName(), context.getMethodName())).increase(context.elapse(),
                                                                                            context.getStackTrace());
  }

  static class Category {

    private final String className;
    private final String methodName;

    public Category(String className, String methodName) {
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
      Category other = (Category) obj;
      if (className == null) {
        if (other.className != null) return false;
      } else if (!className.equals(other.className)) return false;
      if (methodName == null) {
        if (other.methodName != null) return false;
      } else if (!methodName.equals(other.methodName)) return false;
      return true;
    }

  }

  abstract static class Aggregation<K, V> {
    private final ConcurrentHashMap<K, V> map = new ConcurrentHashMap<K, V>();

    public V get(K key) {
      V value = map.get(key);
      if (value != null) return value;
      value = initialValue();
      V absent = map.putIfAbsent(key, value);
      return absent == null ? value : absent;
    }

    protected abstract V initialValue();

    public Map<K, V> snapshot() {
      return new HashMap<K, V>(map);
    }
  }

  static class Statistics {

    private final AtomicLong count = new AtomicLong();
    private final AtomicLong totalElapse = new AtomicLong();

    public void increase(long elapse, StackTraceElement[] stackTraceElements) {
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

  @Override
  public boolean matchs(String className, String methodName, Class<?>[] argumentTypes) {
    // TODO Auto-generated method stub
    return false;
  }

}
