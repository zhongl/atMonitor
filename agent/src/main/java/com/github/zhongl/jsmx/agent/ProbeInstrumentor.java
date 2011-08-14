package com.github.zhongl.jsmx.agent;

import static java.text.MessageFormat.*;

import java.io.*;
import java.lang.instrument.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

import org.ow2.asm.*;
import org.ow2.asm.commons.*;
import org.softee.management.annotation.*;

/**
 * {@link ProbeInstrumentor}
 * 
 * @author <a href=mailto:zhong.lunfu@gmail.com>zhongl</a>
 * @created 2011-8-14
 * 
 */
@MBean(objectName = "jsmx:type=ProbeInstrumentor")
public class ProbeInstrumentor {

  private static byte[] toBytes(InputStream stream) throws IOException {
    if (stream == null) throw new FileNotFoundException();
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    int read = 0;
    while ((read = stream.read()) > -1)
      bytes.write(read);
    return bytes.toByteArray();
  }

  private static String toString(Class<?>... classes) {
    final StringBuilder sb = new StringBuilder("" + classes.length);
    for (final Class<?> clazz : classes) {
      sb.append("\n").append(clazz);
    }
    return sb.toString();
  }

  private static void warning(Throwable t) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    t.printStackTrace(new PrintStream(out));
    LOGGER.warning(new String(out.toByteArray()));
  }

  private final static Logger LOGGER = Logger.getLogger(ProbeInstrumentor.class.getName());

  public ProbeInstrumentor(Instrumentation instrumentation) {
    this.instrumentation = instrumentation;
    // add(resetTransformer);
  }

  @ManagedOperation
  public void addProbeClass(String name) throws Exception {
    classNames.add(name.trim());
  }

  @ManagedOperation
  public String classes() {
    return classNames.toString();
  }

  @ManagedOperation
  public String getAllLoadedClasses() {
    return toString(instrumentation.getAllLoadedClasses());
  }

  @ManagedOperation
  public String getClassLoaderOf(String className) {
    for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
      if (clazz.getName().equals(className)) return clazz.getClassLoader().toString();
    }
    return "Unknown";
  }

  @ManagedOperation
  public boolean isRetransformClassesSupported() {
    return instrumentation.isRetransformClassesSupported();
  }

  @ManagedOperation
  public void probe() throws Throwable {
    try {
      flag.set(1);
      add(probeTransformer);
      instrumentation.retransformClasses(classArray());
    } catch (Throwable t) {
      warning(t);
      throw t;
    }
  }

  @ManagedOperation
  public void removeProbeClass(String name) throws Exception {
    classNames.remove(name.trim());
  }

  @ManagedOperation
  public void reset() throws Throwable {
    try {
      flag.set(-1);
      instrumentation.retransformClasses(classArray());
      classNames.clear();
    } catch (Throwable t) {
      warning(t);
      throw t;
    }
  }

  private void add(ClassFileTransformer transformer) {
    instrumentation.addTransformer(transformer, true);
  }

  private Class<?>[] classArray() {
    ArrayList<Class<?>> classes = new ArrayList<Class<?>>(classNames.size());
    for (String name : classNames) {
      try {
        classes.add(classOf(name));
      } catch (ClassNotFoundException e) {
        warning(e);
      }
    }
    return classes.toArray(new Class<?>[0]);
  }

  private Class<?> classOf(String name) throws ClassNotFoundException {
    return Class.forName(name.trim());
  }

  private boolean contains(String className) {
    return classNames.contains(slashToDot(className));
  }

  private static String slashToDot(String className) {
    return className.replace('/', '.');
  }

  private boolean isCurrent(ClassLoader loader) {
    return getClass().getClassLoader().equals(loader);
  }

  private boolean isProbe() {
    return flag.get() == 1;
  }

  private boolean isReset() {
    return flag.get() == -1;
  }

  private final ClassFileTransformer resetTransformer = new ClassFileTransformer() {

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
      try {
        if (isReset() && isCurrent(loader) && contains(className)) {
          byte[] bytes = toBytes(loader.getResourceAsStream(className + ".class"));
          LOGGER.info(format("reset class {1} from {0}", loader, className));
          return bytes;
        }
      } catch (Exception e) {
        LOGGER.info(format("transfor but not reset class {1} from {0}", loader, className));
        warning(e);
      }
      return classfileBuffer;
    }

  };

  private final AtomicInteger flag = new AtomicInteger(0);

  private final ClassFileTransformer probeTransformer = new ClassFileTransformer() {

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
      try {
        if (isProbe() && isCurrent(loader) && contains(className)) {
          LOGGER.info(format("probe class {1} from {0}", loader, className));
          final ClassReader cr = new ClassReader(classfileBuffer);
          final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
          cr.accept(new ProbeClassAdapter(cw), ClassReader.SKIP_DEBUG);
          byte[] bytes = cw.toByteArray();
          FileOutputStream outputStream = new FileOutputStream("debug.class");
          outputStream.write(bytes);
          outputStream.close();
          return bytes;
        }
      } catch (Exception e) {
        LOGGER.info(format("transfor class {1} from {0}", loader, className));
        warning(e);
      }
      return classfileBuffer;
    }

  };
  private final Instrumentation instrumentation;

  private final Set<String> classNames = new HashSet<String>();

  static class ProbeClassAdapter extends org.ow2.asm.ClassAdapter {

    public ProbeClassAdapter(ClassVisitor cv) {
      super(cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
      className = slashToDot(name);
      super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
      return (mv != null) ? new ProbeMethodAdapter(mv, access, name, desc, className) : mv;
    }

    private String className;

  }

  private static class ProbeMethodAdapter extends AdviceAdapter {

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

}
