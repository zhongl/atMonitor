package com.github.zhongl.jsmx.agent;

import java.io.*;

import org.ow2.asm.*;
import org.ow2.asm.commons.*;

/**
 * {@link ASMCodeExample}
 * 
 * @author <a href=mailto:zhong.lunfu@gmail.com>zhongl</a>
 * 
 */
public class ASMCodeExample {

  static class InnerClassAdapter extends ClassAdapter {

    public InnerClassAdapter(ClassVisitor cv) {
      super(cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
//      if ("<init>".equals(name)) return mv;
      System.out.println(name);
      return new InnerMethodAdapter(mv, access, name, desc);
    }

  }

  static class InnerMethodAdapter extends AdviceAdapter {

    protected InnerMethodAdapter(MethodVisitor mv, int access, String name, String desc) {
      super(mv, access, name, desc);
    }

    private Label start = new Label();
    private Label end = new Label();

    @Override
    protected void onMethodEnter() {
      invokeStatic(Type.getType(ASMCodeExample.class), new Method("onEnter","()V"));
//      visitMethodInsn(INVOKESTATIC, Type.getInternalName(ASMCodeExample.class), "onEnter", "()V");
//      visitLabel(start);
      mark(start);
    }

    @Override
    protected void onMethodExit(int opcode) {
      if (opcode == RETURN) {
        push((Type)null);
//        mv.visitInsn(ACONST_NULL);
      } else if (opcode == ARETURN || opcode == ATHROW) {
        dup();
      } else {
        if (opcode == LRETURN || opcode == DRETURN) {
          dup2();
        } else {
          dup();
        }
        box(Type.getReturnType(this.methodDesc));
      }
//      mv.visitIntInsn(SIPUSH, opcode);
      push(opcode);
      invokeStatic(Type.getType(ASMCodeExample.class), new Method("onExit","(Ljava/lang/Object;I)V"));
//      mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ASMCodeExample.class), "onExit", "(Ljava/lang/Object;I)V");
      
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
//      visitLabel(end);
      mark(end);
//      visitTryCatchBlock(start, end, end, Type.getInternalName(Throwable.class));
      catchException(start, end, Type.getType(Throwable.class));
      throwException();
//      visitInsn(Opcodes.ATHROW);
      super.visitMaxs(maxStack, maxLocals);
    }
  }

  public static void onEnter() {
    System.out.println("enter ...");
  }

  public static void onExit(Object result, int opcode) {
    System.out.println("exit with " + result);
  }

  public static void handle(Throwable throwable) {
    System.err.println("catched : " + throwable);
  }

  public static class Target {
    public void show(String words) {
      System.out.println(words.toString());
    }
  }

  public static void main(String[] args) throws Exception {
    Class<?> clazz = new InnerClassLoader().loadClass(" ");
    Object newInstance = clazz.newInstance();
    clazz.getMethod("show", String.class).invoke(newInstance, new Object[] { null });
  }

  static class InnerClassLoader extends ClassLoader {
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
      if (!" ".equals(name)) return super.loadClass(name);
      try {
        byte[] bs = bytesOf(Target.class);
        ClassReader cr = new ClassReader(bs);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cr.accept(new ProbeInstrumentor.ProbeClassAdapter(cw), ClassReader.SKIP_DEBUG);
        byte[] byteArray = cw.toByteArray();
        FileOutputStream stream = new FileOutputStream("EnhanceTarget.class");
        stream.write(byteArray);
        stream.close();
        return defineClass(null, byteArray, 0, byteArray.length);
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }
    }

    private static byte[] bytesOf(Class<?> clazz) throws IOException {
      InputStream resourceAsStream = clazz.getClassLoader().getResourceAsStream(Type.getInternalName(clazz) + ".class");
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      int read = 0;
      while ((read = resourceAsStream.read()) > -1)
        bytes.write(read);
      return bytes.toByteArray();
    }
  }

}
