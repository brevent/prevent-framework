package me.piebridge.prevent.framework.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import me.piebridge.prevent.framework.PreventLog;

/**
 * Created by thom on 16/2/3.
 */
public class ReflectUtils {

    private static final Map<String, Method> METHOD_CACHES = new LinkedHashMap<String, Method>();

    private ReflectUtils() {

    }

    public static Field getDeclaredField(Object target, String name) {
        if (target == null) {
            return null;
        }
        Field field = null;
        Class clazz = target.getClass();
        while (clazz != null && field == null) {
            try {
                field = clazz.getDeclaredField(name);
                field.setAccessible(true);
            } catch (NoSuchFieldException e) {
                PreventLog.d("cannot find field " + name + " in " + clazz, e);
                clazz = clazz.getSuperclass();
            }
        }
        if (field == null) {
            PreventLog.e("cannot find field " + name + " in " + target.getClass());
        }
        return field;
    }

    public static Object invoke(Object target, String name) {
        return invoke(target, name, null, null);
    }

    public static Object invoke(Object target, String name, Class<?>[] parameterTypes, Object[] args) {
        String key = target.getClass() + "#" + name;
        Method method = METHOD_CACHES.get(key);
        try {
            if (method == null) {
                if (parameterTypes == null || parameterTypes[0] != null) {
                    method = target.getClass().getDeclaredMethod(name, parameterTypes);
                } else {
                    for (Method m : target.getClass().getDeclaredMethods()) {
                        if (m.getName().equals(name)) {
                            method = m;
                            break;
                        }
                    }
                    if (method == null) {
                        throw new NoSuchMethodException();
                    }
                }
                method.setAccessible(true);
                METHOD_CACHES.put(key, method);
            }
            return method.invoke(target, args);
        } catch (NoSuchMethodException e) {
            PreventLog.e("cannot find method " + name + " in " + target.getClass());
        } catch (InvocationTargetException e) {
            PreventLog.e("cannot invoke " + method + " in " + target.getClass());
        } catch (IllegalAccessException e) {
            PreventLog.e("cannot access " + method + " in " + target.getClass());
        }
        return null;
    }

    public static Object get(Object target, String name) {
        Field field = null;
        try {
            field = getDeclaredField(target, name);
            field.setAccessible(true);
            return field.get(target);
        } catch (IllegalAccessException e) {
            PreventLog.e("cannot access " + field + " in " + target.getClass());
        }
        return null;
    }

}
