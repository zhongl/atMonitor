package com.github.zhongl.jsmx.demo;

import org.softee.management.annotation.*;
import org.softee.management.helper.MBeanRegistration;

/**
 * {@link Demo}
 * 
 * @author <a href=mailto:jushi@taobao.com>jushi</a>
 * @created Jul 20, 2011
 * 
 */
public class Demo {

  /**
   * @param args
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    ManagableServer mBean = new ManagableServer();
    new MBeanRegistration(mBean).register();
    mBean.start();
    System.in.read();
  }

  @MBean(objectName="jsmx:type=Demo")
  static class ManagableServer extends Thread {
    public ManagableServer() {
      super("ManagableBean");
      setDaemon(true);
    }

    @ManagedAttribute
    public long getCount() {
      if(count == 60) throwRuntimeException();
      return count;
    }

    private void throwRuntimeException() {
      throw new RuntimeException("for demo.");
    }

    @ManagedAttribute
    public int getGauge() {
      return gauge;
    }

    @ManagedOperation
    public void shutdown() {
      running = false;
      try {
        join();
      } catch (InterruptedException e) {
        interrupt();
      }
    }

    @Override
    public void run() {
      while (running) {
        try {
          sleep(1000L);
        } catch (InterruptedException e) {
          interrupt();
          e.printStackTrace();
        }
        count++;
        gauge = Math.round((float) Math.random() * 1000);
      }
    }

    private volatile long count = 0L;

    private volatile int gauge = 0;

    private volatile boolean running = true;
  }

}
