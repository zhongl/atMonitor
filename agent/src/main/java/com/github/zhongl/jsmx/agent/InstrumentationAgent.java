package com.github.zhongl.jsmx.agent;

import java.lang.instrument.*;

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
    new MBeanRegistration(new ProbeInstrumentor(instrumentation)).register();
    System.out.println("loaded instrumentation mbean.");
  }

}
