package com.github.zhongl.jsmx.agent;

import static java.text.MessageFormat.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.softee.management.annotation.*;

/**
 * {@link Performance}
 * 
 * @author <a href=mailto:zhong.lunfu@gmail.com>zhongl</a>
 * @created 2011-8-16
 * 
 */
@MBean(objectName = "jsmx:type=PerformanceCollector")
public class Performance extends Advice {

  @Override
  public void enterWith(Context context) {
    context.setTimeOn();
    context.setTraceOn();
  }

  @Override
  public void exitWith(Context context) {
    Category key = new Category(context.getClassName(), context.getMethodName());
    aggregation.get(key).increase(context.elapse(), context.getStackTrace());
  }

  @ManagedOperation
  public String getStatistics(int stackDepth) {
    Map<Category, Statistics> snapshot = aggregation.snapshot();
    StringBuilder sb = new StringBuilder();
    for (Category category : snapshot.keySet()) {
      sb.append(category).append(":\n").append(snapshot.get(category).toString(stackDepth)).append('\n');
    }
    return sb.toString();
  }

  private final ThreadSafeMap<Category, Statistics> aggregation = new ThreadSafeMap<Category, Statistics>() {

    @Override
    protected Statistics initialValue() {
      return new Statistics();
    }
  };

  static class Category {

    public Category(String className, String methodName) {
      this.className = className;
      this.methodName = methodName;
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

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((className == null) ? 0 : className.hashCode());
      result = prime * result + ((methodName == null) ? 0 : methodName.hashCode());
      return result;
    }

    @Override
    public String toString() {
      return className + '.' + methodName;
    }

    private final String className;

    private final String methodName;

  }

  static class Statistics {

    public void increase(long elapse, StackTraceElement[] stackTraceElements) {
      aggregation.get(key(stackTraceElements)).increase(elapse);
    }

    public String toString(int stackDepth) {
      Map<Key, Statistic> snapshot = aggregation.snapshot();
      StringBuilder sb = new StringBuilder();

      for (Key key : snapshot.keySet()) {
        Statistic statistic = snapshot.get(key);
        sb.append(format("invoked [avg: {0}ns , cnt: {1}] by ", statistic.average(), statistic.count()));
        sb.append(key.getCaller());
        if (stackDepth > 1) key.appendTo(sb, 1, stackDepth);
        sb.append('\n');
      }
      return sb.toString();
    }

    private Key key(StackTraceElement[] stackTraceElements) {
      return new Key(stackTraceElements);
    }

    private final ThreadSafeMap<Key, Statistic> aggregation = new ThreadSafeMap<Key, Statistic>() {

      @Override
      protected Statistic initialValue() {
        return new Statistic();
      }
    };

    static class Key {

      public Key(StackTraceElement[] stackTraceElements) {
        this.stackTraceElements = stackTraceElements;
      }

      public void appendTo(StringBuilder sb, int begin, int end) {
        for (int i = 1; i < stackTraceElements.length && i <= end; i++) {
          sb.append("\n\t").append(stackTraceElements[i]);
        }
      }

      @Override
      public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Key other = (Key) obj;
        if (!Arrays.equals(stackTraceElements, other.stackTraceElements)) return false;
        return true;
      }

      public Object getCaller() {
        return stackTraceElements[0];
      }

      @Override
      public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(stackTraceElements);
        return result;
      }

      private final StackTraceElement[] stackTraceElements;
    }

    static class Statistic {

      public void increase(long elapse) {
        count.incrementAndGet();
        totalElapse.addAndGet(elapse);
      }

      long average() {
        return totalElapse.get() / count.get();
      }

      long count() {
        return count.get();
      }

      private final AtomicLong count = new AtomicLong();

      private final AtomicLong totalElapse = new AtomicLong();

    }

  }

}
