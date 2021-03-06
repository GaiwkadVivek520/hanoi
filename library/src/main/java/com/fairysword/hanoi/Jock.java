package com.fairysword.hanoi;

import com.fairysword.hanoi.instance.InstancePool;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * The java object copy kit (now only support android)
 */
@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class Jock {

    private static Jock sInstance = null;

    private static final ConcurrentHashMap<Class<?>, Map<String, Field>> nonStaticFieldsCache = new ConcurrentHashMap<>();
    private final Set<Class<?>> ignoredClasses = new HashSet<>();
    private final Set<Class<?>> immutableClasses = new HashSet<>();

    /**
     * @return get Jock instance
     */
    public synchronized static Jock getInstance() {
        if (sInstance == null) {
            sInstance = new Jock();
            sInstance.init();
        }
        return sInstance;
    }

    /**
     * @param original the source object
     * @return the copy of the source object
     * @throws CopyException
     */
    public Object copy(Object original) throws CopyException {
        return copyInternal(original);
    }

    /**
     * register the class you don not want to deep copy
     *
     * @param clazz the class registered
     */
    public void registerIgnoredClass(Class<?> clazz) {
        ignoredClasses.add(clazz);
    }

    private void init() {
        registerJdkImmutableClasses();
        registerIgnoredClasses();
    }

    private boolean isImmutable(Class<?> clazz) {
        return immutableClasses.contains(clazz);
    }

    private boolean shouldNotCopy(Class<?> clazz) {
        return ignoredClasses.contains(clazz);
    }

    private Object copyInternal(Object original) throws CopyException {
        if (original == null) {
            return null;
        }

        Class<?> clazz = original.getClass();
        if (Jock.class.equals(clazz)) {
            throw new CopyException("can not copy Jock self");
        }

        if (isImmutable(clazz) || shouldNotCopy(clazz)) {
            return original;
        }

        if (clazz.isArray()) {
            return copyArray(original);
        }

        return copyObject(original);
    }

    private Object copyObject(Object original) throws CopyException {
        Class<?> clazz = original.getClass();
        Object copy = InstancePool.newInstance(clazz);
        if (copy == null) {
            return null;
        }

        copyFields(allNonStaticFields(clazz).values(), original, copy);
        return copy;
    }

    private Object copyArray(Object original) throws CopyException {
        Class<?> clazz = original.getClass();

        final int length = Array.getLength(original);
        final Object newInstance = Array.newInstance(clazz.getComponentType(), length);

        if (clazz.getComponentType().isPrimitive()) { // TODO custom immutable ?
            //noinspection SuspiciousSystemArraycopy
            System.arraycopy(original, 0, newInstance, 0, length);
        } else {
            for (int i = 0; i < length; i++) {
                final Object v = Array.get(original, i);
                final Object clone = copyInternal(v);
                Array.set(newInstance, i, clone);
            }
        }
        return newInstance;
    }

    private static void addAll(final Map<String, Field> l, final Field[] fields) {
        for (final Field field : fields) {
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            l.put(field.getName(), field);
        }
    }

    /**
     * @param c class
     * @return all non-static fileds
     */
    static Map<String, Field> allNonStaticFields(final Class<?> c) {
        Map<String, Field> fieldList = nonStaticFieldsCache.get(c);
        if (fieldList == null) {
            fieldList = new HashMap<>();
            final Field[] fields = c.getDeclaredFields();
            addAll(fieldList, fields);
            Class<?> sc = c;
            while ((sc = sc.getSuperclass()) != Object.class && sc != null) {
                addAll(fieldList, sc.getDeclaredFields());
            }
            nonStaticFieldsCache.putIfAbsent(c, fieldList);
        }
        return fieldList;
    }

    private void copyFields(Collection<Field> fields, Object from, Object to) throws CopyException {
        if (fields == null || fields.size() <= 0) {
            return;
        }

        for (Field field : fields) {
            if (field == null) {
                continue;
            }

            try {
                field.setAccessible(true);
                field.set(to, copyInternal(field.get(from)));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void registerJdkImmutableClasses() {
        immutableClasses.add(String.class);

        immutableClasses.add(Boolean.class);
        immutableClasses.add(Boolean.TYPE);
        immutableClasses.add(Character.class);
        immutableClasses.add(Character.TYPE);
        immutableClasses.add(Byte.class);
        immutableClasses.add(Byte.TYPE);
        immutableClasses.add(Short.class);
        immutableClasses.add(Short.TYPE);
        immutableClasses.add(Integer.class);
        immutableClasses.add(Integer.TYPE);
        immutableClasses.add(Long.class);
        immutableClasses.add(Long.TYPE);
        immutableClasses.add(Float.class);
        immutableClasses.add(Float.TYPE);
        immutableClasses.add(Double.class);
        immutableClasses.add(Double.TYPE);

        immutableClasses.add(BigDecimal.class);
        immutableClasses.add(BigInteger.class);
        immutableClasses.add(URI.class);
        immutableClasses.add(URL.class);
        immutableClasses.add(UUID.class);
        immutableClasses.add(Pattern.class);
    }

    private void registerIgnoredClasses() {
        ignoredClasses.add(Class.class);
        ignoredClasses.add(Void.class);
    }

}
