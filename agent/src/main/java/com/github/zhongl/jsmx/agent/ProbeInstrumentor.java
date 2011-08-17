package com.github.zhongl.jsmx.agent;

import static java.text.MessageFormat.*;

import java.io.*;
import java.lang.instrument.*;
import java.security.*;
import java.util.*;
import java.util.logging.*;

import org.objectweb.asm.*;
import org.softee.management.annotation.*;
import org.softee.management.helper.*;

/**
 * {@link ProbeInstrumentor}
 * 
 * @author <a href=mailto:zhong.lunfu@gmail.com>zhongl</a>
 * @created 2011-8-14
 * 
 */
@MBean(objectName = "jsmx:type=ProbeInstrumentor")
public class ProbeInstrumentor {

  private static String slashToDot(String className) {
    return className.replace('/', '.');
  }

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
  }

  @ManagedOperation
  @Description("return a set of methods matched regex.")
  public String addProbeByPattern(@Parameter("classPattern") @Description("regex of class, default is '.*'.") String classPattern,
                                  @Parameter("methodPattern") @Description("regex of method, default is '.*'.") String methodPattern) throws Throwable {
    PointcutFilter pointcutFilter = new PointcutFilter(classPattern, methodPattern);
    try {
      Map<String, Set<String>> result = pointcutFilter.filter(instrumentation.getAllLoadedClasses());
      if (!result.isEmpty()) {
        pointcutFilterSet.add(pointcutFilter);
        classNames.addAll(result.keySet());
      }
      return formatClassAndMethodsOf(result);
    } catch (Throwable t) {
      warning(t);
      throw t;
    }
  }

  private String formatClassAndMethodsOf(Map<String, Set<String>> result) {
    StringBuilder sb = new StringBuilder();
    Set<String> keySet = result.keySet();
    for (String className : keySet) {
      sb.append(className).append('.').append(result.get(className)).append('\n');
    }
    return sb.toString();
  }

  @ManagedAttribute
  public void setAdvice(String name) throws Throwable {
    try {
      Advice instance = (Advice) Class.forName("com.github.zhongl.jsmx.agent." + name).newInstance();
      registerIfIsMBean(instance);
      Probe.setAdvice(instance);
    } catch (Throwable t) {
      warning(t);
      throw t;
    }
  }

  private void registerIfIsMBean(Advice instance) throws Exception {
    if (instance.getClass().getAnnotation(MBean.class) != null) new MBeanRegistration(instance).register();
  }

  @ManagedAttribute
  public String getAdvice() {
    return Probe.adviceName();
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
  @Description("retransform classes matched patterns and set probes.")
  public void probe() throws Throwable {
    if (classNames.isEmpty()) return;
    try {
      add(probeTransformer);
      instrumentation.retransformClasses(classArray());
      remove(probeTransformer);
    } catch (Throwable t) {
      warning(t);
      throw t;
    }
  }

  @ManagedOperation
  @Description("retransform classes matched patterns and reset them.")
  public void reset() throws Throwable {
    if (classNames.isEmpty()) return;
    try {
      add(resetTransformer);
      instrumentation.retransformClasses(classArray());
      remove(resetTransformer);
      pointcutFilterSet.clear();
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

  private boolean containsClass(String name) {
    String value = slashToDot(name);
    for (PointcutFilter filter : pointcutFilterSet) {
      if (filter.matchsClass(value)) return true;
    }
    return false;
  }

  private boolean containsMethod(String className, String methodName) {
    for (PointcutFilter filter : pointcutFilterSet) {
      if (filter.matchsMethod(className, methodName)) return true;
    }
    return false;
  }

  private boolean isCurrent(ClassLoader loader) {
    // return getClass().getClassLoader().equals(loader);
    return true;
  }

  private void remove(ClassFileTransformer transformer) {
    instrumentation.removeTransformer(transformer);
  }

  private final Set<String> classNames = new HashSet<String>();

  private final Set<PointcutFilter> pointcutFilterSet = new HashSet<PointcutFilter>();

  private final ClassFileTransformer resetTransformer = new ClassFileTransformer() {

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
      try {
        if (isCurrent(loader) && containsClass(className)) {
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
  private final ClassFileTransformer probeTransformer = new ClassFileTransformer() {

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
      try {
        if (isCurrent(loader) && containsClass(className)) {
          LOGGER.info(format("probe class {1} from {0}", loader, className));
          final ClassReader cr = new ClassReader(classfileBuffer);
          final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
          cr.accept(new ProbeClassAdapter(cw), ClassReader.EXPAND_FRAMES);
          return cw.toByteArray();
        }
      } catch (Exception e) {
        LOGGER.info(format("transfor class {1} from {0}", loader, className));
        warning(e);
      }
      return classfileBuffer;
    }

  };

  private final Instrumentation instrumentation;

  class ProbeClassAdapter extends ClassAdapter {

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
      return (mv != null && containsMethod(className, name))
        ? new ProbeMethodAdapter(mv, access, name, desc, className) : mv;
    }

    private String className;

  }

}
