package com.github.zhongl.jsmx.agent;

/**
 * {@link Advice}
 * @author  <a href=mailto:zhong.lunfu@gmail.com>zhongl</a>
 * @created 2011-8-16
 * 
 */
public abstract class Advice {
  public abstract void enterWith(Context context);

  public abstract void exitWith(Context context);

  public abstract boolean matchs(String className, String methodName, Class<?>[] argumentTypes);
}