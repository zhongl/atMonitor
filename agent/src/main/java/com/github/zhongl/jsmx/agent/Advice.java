package com.github.zhongl.jsmx.agent;

import java.util.*;
import java.util.concurrent.*;

/**
 * {@link Advice}
 * 
 * @author <a href=mailto:zhong.lunfu@gmail.com>zhongl</a>
 * @created 2011-8-16
 * 
 */
public abstract class Advice {
  public abstract void enterWith(Context context);

  public abstract void exitWith(Context context);

  protected abstract static class ThreadSafeMap<K, V> {
    public void clear() {
      map.clear();
    }

    public V get(K key) {
      V value = map.get(key);
      if (value != null) return value;
      value = initialValue();
      V absent = map.putIfAbsent(key, value);
      return absent == null ? value : absent;
    }

    public Map<K, V> snapshot() {
      return new HashMap<K, V>(map);
    }

    protected abstract V initialValue();

    private final ConcurrentHashMap<K, V> map = new ConcurrentHashMap<K, V>();
  }
}
