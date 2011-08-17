package com.github.zhongl.jsmx.agent;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

/**
 * {@link PointcutFilter}
 * 
 * @author <a href=mailto:zhong.lunfu@gmail.com>zhongl</a>
 * @created 2011-8-16
 * 
 */
public class PointcutFilter {

  private static boolean isEmpty(String value) {
    return value == null || value.length() == 0;
  }

  private static final Pattern ALL = Pattern.compile(".*");

  public PointcutFilter(String classPattern, String methodPattern) {
    this.classPattern = isEmpty(classPattern) ? ALL : Pattern.compile(classPattern);
    this.methodPattern = isEmpty(methodPattern) ? ALL : Pattern.compile(methodPattern);
  }

  /**
   * @param classes
   * @return map key is class name, and value is method names.
   */
  public Map<String, Set<String>> filter(Class<?>[] classes) {
    Map<String, Set<String>> map = new HashMap<String, Set<String>>();

    for (Class<?> clazz : classes) {
      String className = clazz.getName();
      if (!matchsClass(className)) continue;
      if (clazz.getClassLoader() == null)
        throw new IllegalArgumentException("Class loaded by Non-Application class loader is not permit.");
      Set<String> methodNames = matchMethodsOf(clazz);
      if (methodNames.isEmpty()) continue;
      map.put(className, methodNames);
    }

    return map;
  }

  public boolean matchsClass(String name) {
    return classPattern.matcher(name).matches();
  }

  public boolean matchsMethod(String className, String methodName) {
    return matchsClass(className) && matchsMethod(methodName);
  }

  private Set<String> matchMethodsOf(final Class<?> clazz) {
    final Set<String> methodNames = new HashSet<String>();
    for (Method method : clazz.getDeclaredMethods()) {
      String name = method.getName();
      if (matchsMethod(name)) methodNames.add(name);
    }
    return methodNames;
  }

  private boolean matchsMethod(String name) {
    return methodPattern.matcher(name).matches();
  }

  private final Pattern classPattern;

  private final Pattern methodPattern;

}
