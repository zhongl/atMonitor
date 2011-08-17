package com.github.zhongl.jsmx.agent;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;

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
    loadThis();
    invokeStatic(Probe.TYPE, Probe.EXIT);
    visitInsn(ATHROW);
    super.visitMaxs(maxStack, maxLocals);
  }

  @Override
  protected void onMethodEnter() {
    push(className);
    push(methodName);
    push(methodDesc);
    loadThis();
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
    loadThis();
    invokeStatic(Probe.TYPE, Probe.EXIT);
  }

  private void prepareResultBy(int opcode) {
    if (opcode == RETURN) { // void
      push((Type) null);
    } else if (opcode == ARETURN) { // object
      dup();
    } else {
      if (opcode == LRETURN || opcode == DRETURN) { // long or double
        dup2();
      } else {
        dup();
      }
      box(Type.getReturnType(methodDesc));
    }
  }

  private final String className;
  private final String methodName;
  private final Label start;
  private final Label end;

}