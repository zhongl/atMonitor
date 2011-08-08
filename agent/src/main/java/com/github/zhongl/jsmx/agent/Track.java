package com.github.zhongl.jsmx.agent;

import static java.text.MessageFormat.*;

import java.lang.instrument.*;
import java.security.*;
import java.util.*;
import java.util.logging.*;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;

/**
 * {@link Track}
 * 
 * @author <a href=mailto:zhong.lunfu@gmail.com>zhongl</a>
 * @created 2011-8-8
 * 
 */
public class Track {
  public static void enter() {
    System.out.println("enter method.");
  }

  public static void exit() {
    System.out.println("exit method.");
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

  private Track() {}

  static class AddTrackClassAdapter extends ClassAdapter {

    public AddTrackClassAdapter(ClassVisitor cv) {
      super(cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      final MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
      if (mv == null) return mv;
      return new AddTrackMethodAdapter(mv, access, name, desc);
    }
  }

  static class AddTrackMethodAdapter extends AdviceAdapter {

    protected AddTrackMethodAdapter(MethodVisitor mv, int access, String name, String desc) {
      super(mv, access, name, desc);
    }

    @Override
    protected void onMethodEnter() {
      visitMethodInsn(Opcodes.INVOKESTATIC, ownerOf(Track.class), "enter", "()V");
    }

    @Override
    protected void onMethodExit(int arg0) {
      visitMethodInsn(Opcodes.INVOKESTATIC, ownerOf(Track.class), "exit", "()V");
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
