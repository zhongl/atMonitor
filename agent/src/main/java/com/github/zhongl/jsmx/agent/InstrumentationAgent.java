package com.github.zhongl.jsmx.agent;

import java.lang.instrument.*;

import javax.management.*;

import org.softee.management.exception.*;
import org.softee.management.helper.MBeanRegistration;

/**
 * {@link InstrumentationAgent}
 * 
 * @author <a href=mailto:jushi@taobao.com>jushi</a>
 * @created 2011-8-5
 * 
 */
public class InstrumentationAgent {

  public static void agentmain(String agentArgs, Instrumentation instrumentation) throws Exception {
    main(agentArgs, instrumentation);
  }

  public static void premain(String agentArgs, Instrumentation instrumentation) throws Exception {
    main(agentArgs, instrumentation);
  }

  public static void main(String[] args) {
    // TODO attach VM with pid
  }

  private static void main(String agentArgs, Instrumentation instrumentation) throws ManagementException,
                                                                             MalformedObjectNameException {
    System.out.println("loading...");
    new MBeanRegistration(new ProbeInstrumentor(instrumentation)).register();
    System.out.println("loaded instrumentation mbean.");
  }

}
