package com.fairysword.objectcopy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by xiongjunhui on 2016-06-15.
 */
public class InstancePool {

    private static Map<Class<?>, Object> defaultValues = new HashMap<>();
    private static Map<Class<?>, Constructor<?>> defaultConstructors = new ConcurrentHashMap<>();
    private static Map<Constructor<?>, Object[]> constructorDefaultParams = new ConcurrentHashMap<>();

    // default values
    static {
        // Boolean/boolean
        defaultValues.put(Boolean.TYPE, Boolean.FALSE);
        defaultValues.put(Boolean.class, Boolean.FALSE);
        // Byte/byte
        defaultValues.put(Byte.TYPE, Byte.valueOf((byte) 0));
        defaultValues.put(Byte.class, Byte.valueOf((byte) 0));
        // Character/char
        defaultValues.put(Character.TYPE, Character.valueOf((char) 0));
        defaultValues.put(Character.class, Character.valueOf((char) 0));
        // Short/short
        defaultValues.put(Short.TYPE, Short.valueOf((short) 0));
        defaultValues.put(Short.class, Short.valueOf((short) 0));
        // Integer/int
        defaultValues.put(Integer.TYPE, Integer.valueOf(0));
        defaultValues.put(Integer.class, Integer.valueOf(0));
        // Long/long
        defaultValues.put(Long.TYPE, Long.valueOf(0));
        defaultValues.put(Long.class, Long.valueOf(0));
        // Float/float
        defaultValues.put(Float.TYPE, Float.valueOf(0));
        defaultValues.put(Float.class, Float.valueOf(0));
        // Double/double
        defaultValues.put(Double.TYPE, Double.valueOf(0));
        defaultValues.put(Double.class, Double.valueOf(0));
    }

    static Object newInstance(Class<?> clazz) {
        Constructor<?> constructor = defaultConstructors.get(clazz);
        if (constructor == null) {
            constructor = findConstructor(clazz);
            constructor.setAccessible(true);
            defaultConstructors.put(clazz, constructor);
        }

        if (constructor == null) {
            return null;
        }

        return newInstance(constructor);
    }

    private static Object newInstance(Constructor<?> constructor) {
        // get the default params for this constructor
        Object[] constructorParams = constructorDefaultParams.get(constructor);
        if (constructorParams == null) {
            constructorParams = makeDefaultParams(constructor);
            constructorDefaultParams.put(constructor, constructorParams);
        }

        Object object = null;
        try {
            object = constructor.newInstance(constructorParams);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return object;
    }

    private static Constructor<?> findConstructor(Class<?> clazz) {
        Constructor<?>[] allConstructors = clazz.getDeclaredConstructors();
        if (allConstructors != null && allConstructors.length > 0) {
            return allConstructors[0];
        }
        return null;
    }

    private static Object[] makeDefaultParams(Constructor<?> constructor) {
        Class<?>[] paramTypes = constructor.getParameterTypes();
        Object[] constructorParams = new Object[paramTypes.length];
        if (paramTypes != null && paramTypes.length > 0) {
            for (int i = 0; i < paramTypes.length; i++) {
                constructorParams[i] = defaultValues.get(paramTypes[i]);
            }
        }
        return constructorParams;
    }

}
