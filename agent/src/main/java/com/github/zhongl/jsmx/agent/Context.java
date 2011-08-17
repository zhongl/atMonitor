package com.github.zhongl.jsmx.agent;

import java.util.*;

/**
 * {@link Context}
 * 
 * @author <a href=mailto:zhong.lunfu@gmail.com>zhongl</a>
 * @created 2011-8-16
 * 
 */
public final class Context {

  public Context(String className, String methodName, boolean voidReturn, Object thisObject, Object[] arguments) {
    this.className = className;
    this.methodName = methodName;
    this.voidReturn = voidReturn;
    this.thisObject = thisObject.toString();
    this.arguments = Arrays.toString(arguments);
  }

  public long elapse() {
    return stoped - started;
  }

  public String getArguments() {
    return arguments;
  }

  public String getClassName() {
    return className;
  }

  public String getMethodName() {
    return methodName;
  }

  public Object getResult() {
    return result;
  }

  public StackTraceElement[] getStackTrace() {
    return stackTrace;
  }

  public long getStarted() {
    return started;
  }

  public long getStoped() {
    return stoped;
  }

  public Object getThisObject() {
    return thisObject;
  }

  public boolean isVoidReturn() {
    return voidReturn;
  }

  public void setTimeOn() {
    timeOn = true;
  }

  public void setTraceOn() {
    traceOn = true;
  }

  @Override
  public String toString() {
    return String.format("Context [className=%s, methodName=%s, voidReturn=%s, thisObject=%s, arguments=%s, result=%s, started=%s, stoped=%s, timeOn=%s, traceOn=%s, stackTrace=%s]",
                         className,
                         methodName,
                         voidReturn,
                         thisObject,
                         arguments,
                         result,
                         started,
                         stoped,
                         timeOn,
                         traceOn,
                         Arrays.toString(stackTrace));
  }

  void exitWith(Object result) {
    this.result = result;
  }

  boolean isTimerOn() {
    return timeOn;
  }

  boolean isTraceOn() {
    return traceOn;
  }

  void setStackTrace(StackTraceElement[] stackTrace) {
    this.stackTrace = stackTrace;
  }

  void startAt(long nanoTime) {
    started = nanoTime;
  }

  void stopAt(long nanoTime) {
    stoped = nanoTime;
  }

  private final String className;
  private final String methodName;
  private final boolean voidReturn;
  private final String thisObject;
  private final String arguments;

  private Object result;

  private long started = -1L;
  private long stoped = -1L;
  private boolean timeOn = false;

  private boolean traceOn = false;
  private StackTraceElement[] stackTrace = null;

}
