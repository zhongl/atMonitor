package com.github.zhongl.jsmx.agent;

import static org.objectweb.asm.Type.*;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;

/**
 * {@link ProbeMethodAdapter}
 * 
 * @author <a href=mailto:zhong.lunfu@gmail.com>zhongl</a>
 * 
 */
class ProbeMethodAdapter extends AdviceAdapter {

  protected ProbeMethodAdapter(MethodVisitor mv, int access, String name, String desc, String className) {
    super(mv, access, name, desc);
    start = new Label();
    end = new Label();
    methodName = name;
    this.className = className;
  }

  @Override
  public void visitMaxs(int maxStack, int maxLocals) {
    mark(end);
    catchException(start, end, Type.getType(Throwable.class));
    dup();
    push(className);
    push(methodName);
    push(methodDesc);
    loadThisOrPushNullIfIsStatic();
    invokeStatic(Probe.TYPE, Probe.EXIT);
    throwException();
    super.visitMaxs(maxStack, maxLocals);
  }

  @Override
  protected void onMethodEnter() {
    push(className);
    push(methodName);
    push(methodDesc);
    loadThisOrPushNullIfIsStatic();
    loadArgArray();
    invokeStatic(Probe.TYPE, Probe.ENTRY);
    mark(start);
  }

  @Override
  protected void onMethodExit(int opcode) {
    if (opcode == ATHROW) return; // do nothing, @see visitMax
    prepareResultBy(opcode);
    push(className);
    push(methodName);
    push(methodDesc);
    loadThisOrPushNullIfIsStatic();
    invokeStatic(Probe.TYPE, Probe.EXIT);
  }

  private boolean isStaticMethod() {
    return (methodAccess & ACC_STATIC) != 0;
  }

  private void loadThisOrPushNullIfIsStatic() {
    if (isStaticMethod()) pushNull();
    else loadThis();
  }

  private void prepareResultBy(int opcode) {
    if (opcode == RETURN) { // void
      pushNull();
      return;
    }

    if (opcode == ARETURN) { // object
      dup();
      return;
    }

    if (opcode == LRETURN || opcode == DRETURN) { // long or double
      dup2();
    } else { // boolean or byte or char or short or int
      dup();
    }

    box(getReturnType(methodDesc));
  }

  private void pushNull() {
    push((Type) null);
  }

  private final String className;
  private final String methodName;
  private final Label start;
  private final Label end;

}
