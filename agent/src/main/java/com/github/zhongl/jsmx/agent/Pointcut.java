package com.github.zhongl.jsmx.agent;

/**
 * {@link Pointcut}
 * 
 * @author <a href=mailto:zhong.lunfu@gmail.com>zhongl</a>
 * 
 */
public class Pointcut {
  /**
   * Invoked by Method Visitor when enter a method.
   * 
   * @param className
   * @param methodName
   * @param descriptor of method.
   */
  public static void enter(String className, String methodName, String descriptor) {

  }
  
  /**
   * Invoked by Method Visitor when exit a method.
   * 
   * @param className
   * @param methodName
   * @param descriptor of method.
   */
  public static void exit(String className, String methodName, String descriptor){
    
  }
}
