package com.github.zhongl.jsmx.agent;

import static java.text.MessageFormat.*;

import java.lang.instrument.*;
import java.security.*;
import java.util.*;
import java.util.logging.*;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;

/**
 * {@link Trace}
 * 
 * @author <a href=mailto:zhong.lunfu@gmail.com>zhongl</a>
 * @created 2011-8-8
 * 
 */
public class Trace {
  public static void enter(String clazz, String method, String desc) {
    System.out.print("enter ");
    System.out.print(clazz);
    System.out.print(".");
    System.out.print(method);
    System.out.print(desc);
    System.out.println();
  }

  public static void exit(String clazz, String method, String desc, int returnCode) {
    System.out.print("exit ");
    System.out.print(clazz);
    System.out.print(".");
    System.out.print(method);
    System.out.print(desc);
    System.out.print(" - ");
    System.out.print(returnCode);
    System.out.println();
  }

  private static String dotToSlash(String value) {
    return value.replace('.', '/');
  }

  private static String ownerOf(Class<?> cls) {
    return dotToSlash(cls.getName());
  }

  public static final Transformer transformer = new Transformer(new ClassAdapterFactory() {

    @Override
    public ClassAdapter createWith(ClassWriter cw) {
      return new AddTrackClassAdapter(cw);
    }
  });

  private Trace() {}

  static class AddTrackClassAdapter extends ClassAdapter {

    private String className;

    public AddTrackClassAdapter(ClassVisitor cv) {
      super(cv);
    }
    
    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
      this.className = name;
      super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      final MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
      if (mv == null) return mv;
      
      return new AddTrackMethodAdapter(mv, access, name, desc, className);
    }
  }

  static class AddTrackMethodAdapter extends AdviceAdapter {

    private final String className;
    private final String methodName;
    private final String desc;

    protected AddTrackMethodAdapter(MethodVisitor mv, int access, String name, String desc, String className) {
      super(mv, access, name, desc);
      this.className = className;
      this.methodName = name;
      this.desc = desc;
    }

    @Override
    protected void onMethodEnter() {
      visitLdcInsn(className);
      visitLdcInsn(methodName);
      visitLdcInsn(desc);
      visitMethodInsn(Opcodes.INVOKESTATIC, ownerOf(Trace.class), "enter", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
    }

    @Override
    protected void onMethodExit(int arg0) {
      visitLdcInsn(className);
      visitLdcInsn(methodName);
      visitLdcInsn(desc);
      visitLdcInsn(arg0);
      visitMethodInsn(Opcodes.INVOKESTATIC, ownerOf(Trace.class), "exit", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V");
    }

  }

  static class Transformer implements ClassFileTransformer {

    private final static Logger LOGGER = Logger.getLogger(Transformer.class.getName());

    public Transformer(ClassAdapterFactory classAdapterFactory) {
      includeClassNames = new HashSet<String>();
      this.classAdapterFactory = classAdapterFactory;
    }

    public void addIncludeClassName(String value) {
      includeClassNames.add(dotToSlash(value));
    }

    public void removeIncludeClassName(String value) {
      includeClassNames.remove(dotToSlash(value));
    }

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
      if (!includeClassNames.contains(className)) return classfileBuffer;
      LOGGER.info(format("transform => {1} from {0}", loader, className));
      final ClassReader cr = new ClassReader(classfileBuffer);
      final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
      final ClassAdapter ca = classAdapterFactory.createWith(cw);
      cr.accept(ca, ClassReader.SKIP_DEBUG);
      return cw.toByteArray();
    }

    private final Set<String> includeClassNames;
    private final ClassAdapterFactory classAdapterFactory;

  }
}
