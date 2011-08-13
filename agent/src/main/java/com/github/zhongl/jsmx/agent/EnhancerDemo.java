package com.github.zhongl.jsmx.agent;

import java.io.*;
import java.lang.reflect.*;


import net.sf.cglib.asm.*;
import net.sf.cglib.proxy.*;

/**
 * {@link EnhancerDemo}
 * 
 * @author <a href=mailto:jushi@taobao.com>jushi</a>
 * 
 */
public class EnhancerDemo {

  /**
   * @param args
   * @throws Exception 
   */
  public static void main(String[] args) throws Exception {
    Enhancer enhancer = new Enhancer();
    enhancer.setSuperclass(Demo.class);
    enhancer.setCallback(new AdviceInterceptor());
//    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
//    enhancer.generateClass(classWriter);
//    FileOutputStream output = new FileOutputStream(new File("EnhancedDemo.class"));
//    output.write(classWriter.toByteArray());
//    output.close();
    
    Demo d = (Demo) enhancer.create();
    d.show();
    System.out.println("done.");
  }

  static final class AdviceInterceptor implements MethodInterceptor {
    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
      System.out.println("advice.");
      return proxy.invokeSuper(obj, args);
    }
  }

  static class Demo {

    public void show() {
      System.out.println("demo show.");
    }
  }

}
