package com.fairysword.objectcopy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by pal on 2016-06-15.
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
        defaultValues.put(Byte.TYPE, (byte) 0);
        defaultValues.put(Byte.class, (byte) 0);
        // Character/char
        defaultValues.put(Character.TYPE, (char) 0);
        defaultValues.put(Character.class, (char) 0);
        // Short/short
        defaultValues.put(Short.TYPE, (short) 0);
        defaultValues.put(Short.class, (short) 0);
        // Integer/int
        defaultValues.put(Integer.TYPE, 0);
        defaultValues.put(Integer.class, 0);
        // Long/long
        defaultValues.put(Long.TYPE, 0L);
        defaultValues.put(Long.class, 0L);
        // Float/float
        defaultValues.put(Float.TYPE, 0f);
        defaultValues.put(Float.class, 0f);
        // Double/double
        defaultValues.put(Double.TYPE, 0d);
        defaultValues.put(Double.class, 0d);
    }

    static Object newInstance(Class<?> clazz) {
        // fast new instance
        if (ArrayList.class.equals(clazz)) {
            return new ArrayList<>();
        } else if (HashMap.class.equals(clazz)) {
            return new HashMap<>();
        }

        // using reflect constructor
        Constructor<?> constructor = defaultConstructors.get(clazz);
        if (constructor == null) {
            constructor = findConstructor(clazz);
            //noinspection ConstantConditions
            constructor.setAccessible(true);
            defaultConstructors.put(clazz, constructor);
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
        } catch (InstantiationException | IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
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
        if (paramTypes.length > 0) {
            for (int i = 0; i < paramTypes.length; i++) {
                constructorParams[i] = defaultValues.get(paramTypes[i]);
            }
        }
        return constructorParams;
    }

}
