package com.github.zhongl.jsmx.agent;

import org.objectweb.asm.*;

/**
 * {@link ClassAdapterFactory}
 * @author  <a href=mailto:zhong.lunfu@gmail.com>zhongl</a>
 * @created 2011-8-8
 * 
 */
public interface ClassAdapterFactory {
  ClassAdapter createWith(ClassWriter cw);
}
