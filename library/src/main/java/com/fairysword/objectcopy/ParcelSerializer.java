package com.fairysword.objectcopy;

import android.os.Parcel;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serializer using {@link Parcel}
 */
public class ParcelSerializer {

    static Map<String, Class<?>> primitiveTypes = new HashMap<>();
    private static Map<Class<?>, SerializableHandler> serializableHandlerMap = new HashMap<>();

    static {
        primitiveTypes.put("char", Character.TYPE);
        primitiveTypes.put("boolean", Boolean.TYPE);

        primitiveTypes.put("byte", Byte.TYPE);
        primitiveTypes.put("short", Short.TYPE);
        primitiveTypes.put("int", Integer.TYPE);
        primitiveTypes.put("long", Long.TYPE);
        primitiveTypes.put("float", Float.TYPE);
        primitiveTypes.put("double", Double.TYPE);
    }

    /**
     * atomic type serializer handler
     */
    static {
        serializableHandlerMap.clear();

        serializableHandlerMap.put(String.class, new SerializableHandler() {
            @Override
            public void write(Parcel p, Object v) {
                p.writeString((String) v);
            }

            @Override
            public Object read(Parcel p) {
                return p.readString();
            }
        });
        serializableHandlerMap.put(CharSequence.class, new SerializableHandler() {
            @Override
            public void write(Parcel p, Object v) {
                Hack.into(Parcel.class).method("writeCharSequence").withParam(CharSequence.class).invokeWithParam((CharSequence) v).on(p);
            }

            @Override
            public Object read(Parcel p) {
                return Hack.into(Parcel.class).method("readCharSequence").withoutParams().invoke().on(p);
            }
        });   // CharSequence is non-immutable

        SerializableHandler booleans = new SerializableHandler() {
            @Override
            public void write(Parcel p, Object v) {
                p.writeInt((Boolean) v ? 1 : 0);
            }

            @Override
            public Object read(Parcel p) {
                return p.readInt() == 1;
            }
        };
        serializableHandlerMap.put(Boolean.class, booleans);
        serializableHandlerMap.put(Boolean.TYPE, booleans);

        SerializableHandler byteNumbers = new IntNumberHandler() {
            @Override
            public Object read(Parcel p) {
                return (byte) (p.readInt() & 0xff);
            }
        };
        serializableHandlerMap.put(Byte.class, byteNumbers);
        serializableHandlerMap.put(Byte.TYPE, byteNumbers);

        SerializableHandler shortNumbers = new IntNumberHandler() {
            @Override
            public Object read(Parcel p) {
                return (short) p.readInt();
            }
        };
        serializableHandlerMap.put(Short.class, shortNumbers);
        serializableHandlerMap.put(Short.TYPE, shortNumbers);

        SerializableHandler intNumbers = new IntNumberHandler() {
            @Override
            public Object read(Parcel p) {
                return p.readInt();
            }
        };
        serializableHandlerMap.put(Integer.class, intNumbers);
        serializableHandlerMap.put(Integer.TYPE, intNumbers);

        SerializableHandler longNumbers = new SerializableHandler() {
            @Override
            public void write(Parcel p, Object v) {
                p.writeLong((Long) v);
            }

            @Override
            public Object read(Parcel p) {
                return p.readLong();
            }
        };
        serializableHandlerMap.put(Long.class, longNumbers);
        serializableHandlerMap.put(Long.TYPE, longNumbers);

        SerializableHandler floatNumbers = new SerializableHandler() {
            @Override
            public void write(Parcel p, Object v) {
                p.writeFloat((Float) v);
            }

            @Override
            public Object read(Parcel p) {
                return p.readFloat();
            }
        };
        serializableHandlerMap.put(Float.class, floatNumbers);
        serializableHandlerMap.put(Float.TYPE, floatNumbers);

        SerializableHandler doubleNumbers = new SerializableHandler() {
            @Override
            public void write(Parcel p, Object v) {
                p.writeDouble((Double) v);
            }

            @Override
            public Object read(Parcel p) {
                return p.readDouble();
            }
        };
        serializableHandlerMap.put(Double.class, doubleNumbers);
        serializableHandlerMap.put(Double.TYPE, doubleNumbers);
    }

    /**
     * serialize object to bytes
     *
     * @param original the source object
     * @return serializable bytes using {@link Parcel}
     */
    public static byte[] serialize(Object original) {
        Parcel parcel = Parcel.obtain();
        writeObject(parcel, original);
        byte[] bytes = parcel.marshall();
        parcel.recycle();
        return bytes;
    }

    /**
     * deserialize bytes to object
     *
     * @param bytes serializable bytes using {@link Parcel}
     * @return the instance represented by the serializable bytes
     */
    @SuppressWarnings("TryWithIdenticalCatches")
    public static Object deserialize(byte[] bytes) {
        Parcel p = Parcel.obtain();
        p.unmarshall(bytes, 0, bytes.length);
        p.setDataPosition(0);

        Object instance = null;
        try {
            instance = readObject(p);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        p.recycle();
        return instance;
    }

    private static void writeObject(Parcel p, Object value) {
        writeTypeValuePair(p, value);
    }

    private static Object readObject(Parcel p) throws ClassNotFoundException {
        return readTypeValuePair(p);
    }

    private static void writeType(Parcel p, Class<?> type) {
        p.writeString(type.getName());
    }

    private static void writeValueByType(Parcel p, Class<?> clazz, Object value) {
        p.writeInt(value == null ? 0 : 1); // indicate value is null
        if (value == null) {
            return;
        }

        if (clazz.isArray()) {  // write array value
            writeArray(p, value);
            return;
        } else if (List.class.isAssignableFrom(clazz)) {
            writeList(p, (List<?>) value);
            return;
        } else if (Map.class.isAssignableFrom(clazz)) {
            writeMap(p, (Map<?, ?>) value);
            return;
        } else {
            SerializableHandler handler = getSerializableHandler(clazz);
            if (handler != null) {
                handler.write(p, value);
                return;
            }
        }

        writeObjectInner(p, clazz, value);
    }

    private static void writeArray(Parcel p, Object value) {
        int len = Array.getLength(value);
        p.writeInt(len);
        for (int i = 0; i < len; i++) {
            writeTypeValuePair(p, Array.get(value, i));
        }
    }

    private static void writeList(Parcel p, List<?> value) {
        int len = value.size();
        p.writeInt(len);
        if (len <= 0) {
            return;
        }

        for (int i = 0; i < len; i++) {
            writeTypeValuePair(p, value.get(i));
        }
    }

    private static String readType(Parcel p) {
        return p.readString();
    }

    private static Object readValueByType(Parcel p, String clazzName) throws ClassNotFoundException {
        boolean isNull = p.readInt() == 0;
        if (isNull) {
            return null;
        }

        Class<?> clazz = findClassFromName(clazzName);
        if (clazz.isArray()) {  // read array
            return readArray(p, clazz);
        } else if (List.class.isAssignableFrom(clazz)) {
            return readList(p, clazz);
        } else if (Map.class.isAssignableFrom(clazz)) {
            return readMap(p, clazz);
        } else {
            SerializableHandler handler = getSerializableHandler(clazz);
            if (handler != null) {
                return handler.read(p);
            }
        }

        return readObjectInner(p, clazz);
    }

    private static Object readArray(Parcel p, Class<?> clazz) throws ClassNotFoundException {
        int len = p.readInt();

        Object arr = Array.newInstance(clazz.getComponentType(), len);
        for (int i = 0; i < len; i++) {
            Array.set(arr, i, readTypeValuePair(p));
        }
        return arr;
    }

    private static Object readList(Parcel p, Class<?> clazz) throws ClassNotFoundException {
        int len = p.readInt();
        List obj = (List) InstancePool.newInstance(clazz);
        if (len <= 0) {
            return obj;
        }

        if (obj == null) {
            return null;
        }

        for (int i = 0; i < len; i++) {
            //noinspection unchecked
            obj.add(readTypeValuePair(p));
        }
        return obj;
    }

    private static Class<?> findClassFromName(String clazzName) throws ClassNotFoundException {
        Class<?> clazz = primitiveTypes.get(clazzName);
        if (clazz == null) {
            clazz = Class.forName(clazzName);
        }
        return clazz;
    }

    private static void writeObjectInner(Parcel p, Class<?> clazz, Object object) {
        Collection<Field> fields = Jock.allNonStaticFields(clazz).values();
        for (Field field : fields) {
            Object fieldValue = null;
            try {
                fieldValue = field.get(object);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            p.writeString(field.getName()); // write object field name
            writeTypeValuePair(p, fieldValue);
        }
    }

    private static Object readObjectInner(Parcel p, Class<?> clazz) throws ClassNotFoundException {
        Object object = InstancePool.newInstance(clazz);
        Map<String, Field> fieldMap = Jock.allNonStaticFields(clazz);
        for (int i = 0; i < fieldMap.size(); i++) {
            String fieldName = p.readString();
            Object fieldValue = readTypeValuePair(p);

            Field field = fieldMap.get(fieldName);
            if (field != null) {
                try {
                    field.set(object, fieldValue);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return object;
    }

    private static void writeMap(Parcel p, Map<?, ?> map) {
        p.writeInt(map.size()); // write size
        for (Map.Entry entry : map.entrySet()) {
            writeTypeValuePair(p, entry.getKey());
            writeTypeValuePair(p, entry.getValue());
        }
    }

    private static Object readMap(Parcel p, Class<?> mapClazz) throws ClassNotFoundException {
        Map map = (Map) InstancePool.newInstance(mapClazz);
        int size = p.readInt();
        for (int i = 0; i < size; i++) {
            //noinspection unchecked
            map.put(readTypeValuePair(p), readTypeValuePair(p));
        }
        return map;
    }

    private static Object readTypeValuePair(Parcel p) throws ClassNotFoundException {
        return readValueByType(p, readType(p));
    }

    private static void writeTypeValuePair(Parcel p, Object value) {
        Class<?> valueClazz = getTypeClass(value);
        writeType(p, valueClazz);
        writeValueByType(p, valueClazz, value);
    }

    private static Class<?> getTypeClass(Object value) {
        return value == null ? NullValue.class : value.getClass();
    }

    private static SerializableHandler getSerializableHandler(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }

        SerializableHandler handler;

        if (String.class.equals(clazz)) { // String can not be extended
            handler = serializableHandlerMap.get(String.class);
        } else if (CharSequence.class.isAssignableFrom(clazz)) {
            handler = serializableHandlerMap.get(CharSequence.class);
        } else if (clazz.isAssignableFrom(Number.class)) {
            handler = serializableHandlerMap.get(clazz);
        } else {
            handler = serializableHandlerMap.get(clazz);
        }

        return handler;
    }

    // a placeholder interface for null value
    private interface NullValue {

    }

    private interface SerializableHandler {
        void write(Parcel p, Object v);

        Object read(Parcel p);
    }

    private static abstract class IntNumberHandler implements SerializableHandler {
        @Override
        public void write(Parcel p, Object v) {
            p.writeInt(((Number) v).intValue());
        }
    }

}
