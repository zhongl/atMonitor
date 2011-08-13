package com.github.zhongl.jsmx.agent;

/**
 * {@link ASMCodeExample}
 * 
 * @author <a href=mailto:zhong.lunfu@gmail.com>zhongl</a>
 * 
 */
public class ASMCodeExample {
  // public String say(String words) {
  // Pointcut.enter("c", "m", "s");

  // return words;
  // Pointcut.exit("c", "m", "s");
  // }

  // public int calc(int i, int j) {
  // if (Integer.MAX_VALUE - i < j) return Integer.MAX_VALUE;
  // else return sum(i, j);
  // }

  public int sum(int i, int j) {
    return i + j;
  }

  public static void sleep(long d) throws InterruptedException {

    try {
      Thread.sleep(d);
    } catch (InterruptedException e) {
      // e.printStackTrace();
    }
  }
}
