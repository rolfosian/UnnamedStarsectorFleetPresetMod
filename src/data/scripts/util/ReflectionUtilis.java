package data.scripts.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.Pair;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandle;

import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;

import org.apache.log4j.Logger;
public class ReflectionUtilis {
    private static final Logger logger = Logger.getLogger(ReflectionUtilis.class);
    public static void print(Object... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i] instanceof String ? (String) args[i] : String.valueOf(args[i]));
            if (i < args.length - 1) sb.append(' ');
        }
        logger.info(sb.toString());
    }

    public static List<Class<?>> getAllObfClasses() {
        try {
            JarFile jarFile = new JarFile("starfarer_obf.jar");
            Enumeration<JarEntry> entries = jarFile.entries();
            List<Class<?>> obfClasses = new ArrayList<>();
    
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
    
                String name = entry.getName();
                if (name.endsWith(".class")) {
                    String className = name.replace("/", ".").substring(0, name.length() - ".class".length());
                    obfClasses.add(Class.forName(className, false, Global.class.getClassLoader()));
                }
            }
    
            jarFile.close();
            return obfClasses;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class ObfuscatedClasses {
        private static final Class<?>[] obfClazzes;
        private static final Class<?>[] obfInterfaces;
        private static final Class<?>[] obfEnums;

        static {
            List<Class<?>> obfClasses = getAllObfClasses();
    
            List<Class<?>> enumz = new ArrayList<>();
            List<Class<?>> interfeces = new ArrayList<>();
            List<Class<?>> clses = new ArrayList<>();
    
            for (Class<?> cls : obfClasses) {
                if (cls.isEnum()) enumz.add(cls);
                else if (cls.isInterface()) interfeces.add(cls);
                else clses.add(cls);
            }
    
            Class<?>[] clsArr = new Class<?>[0];
            obfClazzes = clses.toArray(clsArr);
            obfInterfaces = interfeces.toArray(clsArr);
            obfEnums = enumz.toArray(clsArr);
        }

        public static Class<?>[] getClasses() {
            return obfClazzes;
        }

        public static Class<?>[] getInterfaces() {
            return obfInterfaces;
        }

        public static Class<?>[] getEnums() {
            return obfEnums;
        }
    }

    // Code taken and modified from Grand Colonies and Ashes of the Domain
    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();

    private static final Class<?> fieldClass;
    private static final Class<?> fieldArrayClass;
    private static final Class<?> methodClass;
    private static final Class<?> typeClass;
    private static final Class<?> typeArrayClass;
    private static final Class<?> constructorClass;

    private static final MethodHandle getFieldTypeHandle;
    private static final MethodHandle setFieldHandle;
    private static final MethodHandle getFieldHandle;
    private static final MethodHandle getFieldNameHandle;
    private static final MethodHandle setFieldAccessibleHandle;
    private static final MethodHandle getFieldModifiersHandle;

    private static final MethodHandle getMethodNameHandle;
    private static final MethodHandle getMethodDeclaringClassHandle;
    private static final MethodHandle invokeMethodHandle;
    private static final MethodHandle setMethodAccessable;
    private static final MethodHandle getModifiersHandle;
    private static final MethodHandle getParameterTypesHandle;
    private static final MethodHandle getReturnTypeHandle;
    private static final MethodHandle getGenericReturnTypeHandle;
    
    private static final MethodHandle getGenericTypeHandle;
    private static final MethodHandle getGenericParameterTypesHandle;
    
    private static final MethodHandle setConstructorAccessibleHandle;
    private static final MethodHandle getConstructorParameterTypesHandle;
    private static final MethodHandle constructorNewInstanceHandle;
    private static final MethodHandle getConstructorDeclaringClassHandle;
    private static final MethodHandle getConstructorGenericParameterTypesHandle;
    private static final MethodHandle getConstructorNameHandle;

    static {
        try {
            fieldClass = Class.forName("java.lang.reflect.Field", false, Class.class.getClassLoader());
            fieldArrayClass = Class.forName("[Ljava.lang.reflect.Field;", false, Class.class.getClassLoader());
            methodClass = Class.forName("java.lang.reflect.Method", false, Class.class.getClassLoader());
            typeClass = Class.forName("java.lang.reflect.Type", false, Class.class.getClassLoader());
            typeArrayClass = Class.forName("[Ljava.lang.reflect.Type;", false, Class.class.getClassLoader());
            constructorClass = Class.forName("java.lang.reflect.Constructor", false, Class.class.getClassLoader());

            setFieldHandle = lookup.findVirtual(fieldClass, "set", MethodType.methodType(void.class, Object.class, Object.class));
            getFieldHandle = lookup.findVirtual(fieldClass, "get", MethodType.methodType(Object.class, Object.class));
            getFieldNameHandle = lookup.findVirtual(fieldClass, "getName", MethodType.methodType(String.class));
            getFieldTypeHandle = lookup.findVirtual(fieldClass, "getType", MethodType.methodType(Class.class));
            getFieldModifiersHandle = lookup.findVirtual(fieldClass, "getModifiers", MethodType.methodType(int.class));
            setFieldAccessibleHandle = lookup.findVirtual(fieldClass, "setAccessible", MethodType.methodType(void.class, boolean.class));

            getMethodNameHandle = lookup.findVirtual(methodClass, "getName", MethodType.methodType(String.class));
            getMethodDeclaringClassHandle = lookup.findVirtual(methodClass, "getDeclaringClass", MethodType.methodType(Class.class));
            invokeMethodHandle = lookup.findVirtual(methodClass, "invoke", MethodType.methodType(Object.class, Object.class, Object[].class));
            setMethodAccessable = lookup.findVirtual(methodClass, "setAccessible", MethodType.methodType(void.class, boolean.class));
            getModifiersHandle = lookup.findVirtual(methodClass, "getModifiers", MethodType.methodType(int.class));
            getParameterTypesHandle = lookup.findVirtual(methodClass, "getParameterTypes", MethodType.methodType(Class[].class));
            getReturnTypeHandle = lookup.findVirtual(methodClass, "getReturnType", MethodType.methodType(Class.class));
            getGenericReturnTypeHandle = lookup.findVirtual(methodClass, "getGenericReturnType", MethodType.methodType(typeClass));

            getGenericTypeHandle = lookup.findVirtual(fieldClass, "getGenericType", MethodType.methodType(typeClass));
            getGenericParameterTypesHandle = lookup.findVirtual(methodClass, "getGenericParameterTypes", MethodType.methodType(typeArrayClass));

            setConstructorAccessibleHandle = lookup.findVirtual(constructorClass, "setAccessible", MethodType.methodType(void.class, boolean.class));
            getConstructorParameterTypesHandle = lookup.findVirtual(constructorClass, "getParameterTypes", MethodType.methodType(Class[].class));
            constructorNewInstanceHandle = lookup.findVirtual(constructorClass, "newInstance", MethodType.methodType(Object.class, Object[].class));
            getConstructorDeclaringClassHandle = lookup.findVirtual(constructorClass, "getDeclaringClass", MethodType.methodType(Class.class));
            getConstructorGenericParameterTypesHandle = lookup.findVirtual(constructorClass, "getGenericParameterTypes", MethodType.methodType(typeArrayClass));
            getConstructorNameHandle = lookup.findVirtual(constructorClass, "getName", MethodType.methodType(String.class));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Class<?> getMethodDeclaringClass(Object method) {
        try {
            return (Class<?>) getMethodDeclaringClassHandle.invoke(method);
        } catch (Throwable e) {
            throw new RuntimeException();
        }
    }

    public static boolean hasFieldOfType(Class<?> cls, Class<?> fieldType) {
        try {
            for (Object field : cls.getDeclaredFields()) {
                if (((Class<?>)getFieldTypeHandle.invoke(field)).equals(fieldType)) {
                    return true;
                }
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    public static int getFieldModifiers(Object field) throws Throwable {
        return (int)getFieldModifiersHandle.invoke(field);
    }

    public static void transplant(Object original, Object template) {
        try {
            Class<?> currentClass = original.getClass();
            while ((currentClass = currentClass.getSuperclass()) != null) {

                for (Object field : currentClass.getDeclaredFields()) {
                    if (isFinal(getFieldModifiers(field))) continue;

                    String fieldName = (String) getFieldNameHandle.invoke(field);
                    Object variable = getPrivateVariableFromSuperClass(fieldName, original);
                    if (variable == original) {
                        setPrivateVariableFromSuperclass(fieldName, template, template);
                    } else {
                        setPrivateVariableFromSuperclass(fieldName, template, variable);
                    }
                }
            }

            boolean isExactClass = original.getClass().equals(template.getClass());
            for (Object field : original.getClass().getDeclaredFields()) {
                if (isFinal(getFieldModifiers(field))) continue;

                Object variable = getPrivateVariable(field, original);
                if (isExactClass) {
                    if (variable == original) {
                        setPrivateVariable(field, template, template);
                    } else {
                        setPrivateVariable(field, template, variable);
                    }
                } else {
                    if (variable == original) {
                        setPrivateVariableByName((String)getFieldNameHandle.invoke(field), template, template);
                    } else {
                        setPrivateVariableByName((String)getFieldNameHandle.invoke(field), template, variable);
                    }
                }
            }
            return;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Object> getAllVariables(Object instanceToGetFrom) {
        List<Object> lst = new ArrayList<>();
        Class<?> currentClass = instanceToGetFrom.getClass();
        while (currentClass != null) {
            for (Object field : currentClass.getDeclaredFields()) {
                lst.add(getPrivateVariable(field, instanceToGetFrom));
            }
            currentClass = currentClass.getSuperclass();
        }
        return lst;
    }

    // public static List<Object> getAllFields(Object instance) {
    //     return getAllFields(instance.getClass());
    // }

    public static List<Object> getAllFields(Class<?> cls) {
        List<Object> lst = new ArrayList<>();
        Class<?> currentClass = cls;
        while (currentClass != null) {
            for (Object field : currentClass.getDeclaredFields()) {
                lst.add(field);
            }
            currentClass = currentClass.getSuperclass();
        }
        return lst;
    }

    public static void setPrivateVariableByName(String fieldName, Object instanceToModify, Object newValue) throws Throwable {
        Object field = instanceToModify.getClass().getDeclaredField(fieldName);
        setFieldAccessibleHandle.invoke(field, true);
        setFieldHandle.invoke(field, instanceToModify, newValue);
    }

    public static String getMethodName(Object method) {
        try {
            return (String) getMethodNameHandle.invoke(method);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Class<?> getReturnType(Object method) {
        try {
            return (Class<?>) getReturnTypeHandle.invoke(method);
        } catch (Throwable e) {
            print(e);
            return null;
        }
    }

    public static String getFieldName(Object field) {
        try {
            return (String) getFieldNameHandle.invoke(field);
        } catch (Throwable e) {
            print(e);
            return null;
        }
    }

    public static Object getFieldByName(String name, Class<?> cls) {
        try {
            for (Object field : cls.getDeclaredFields()) {
                if (((String)getFieldNameHandle.invoke(field)).equals(name)) {
                    return field;
                }
            }
        } catch (Throwable e) {
            print(e);
        }
        return null;
    }

    public static Object getPrivateVariable(Object field, Object instanceToGetFrom) {
        try {
            setFieldAccessibleHandle.invoke(field, true);
            return getFieldHandle.invoke(field, instanceToGetFrom);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getPrivateVariable(String fieldName, Object instanceToGetFrom) {
        try {
            Class<?> instances = instanceToGetFrom.getClass();
            while (instances != null) {
                for (Object obj : instances.getDeclaredFields()) {
                    setFieldAccessibleHandle.invoke(obj, true);
                    String name = (String) getFieldNameHandle.invoke(obj);
                    if (name.equals(fieldName)) {
                        return getFieldHandle.invoke(obj, instanceToGetFrom);
                    }
                }
                for (Object obj : instances.getFields()) {
                    setFieldAccessibleHandle.invoke(obj, true);
                    String name = (String) getFieldNameHandle.invoke(obj);
                    if (name.equals(fieldName)) {
                        return getFieldHandle.invoke(obj, instanceToGetFrom);
                    }
                }
                instances = instances.getSuperclass();
            }
            return null;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getPrivateVariableFromSuperClass(String fieldName, Object instanceToGetFrom) {
        try {
            Class<?> instances = instanceToGetFrom.getClass();
            while (instances != null) {
                for (Object obj : instances.getDeclaredFields()) {
                    setFieldAccessibleHandle.invoke(obj, true);
                    String name = (String) getFieldNameHandle.invoke(obj);
                    if (name.equals(fieldName)) {
                        return getFieldHandle.invoke(obj, instanceToGetFrom);
                    }
                }
                for (Object obj : instances.getFields()) {
                    setFieldAccessibleHandle.invoke(obj, true);
                    String name = (String) getFieldNameHandle.invoke(obj);
                    if (name.equals(fieldName)) {
                        return getFieldHandle.invoke(obj, instanceToGetFrom);
                    }
                }
                instances = instances.getSuperclass();
            }
            return null;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void setPrivateVariable(Object field, Object instanceToModify, Object newValue) {
        try {
            setFieldAccessibleHandle.invoke(field, true);
            setFieldHandle.invoke(field, instanceToModify, newValue);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void setPrivateVariableFromSuperclass(String fieldName, Object instanceToModify, Object newValue) {
        try {
            Class<?> instances = instanceToModify.getClass();
            while (instances != null) {
                for (Object obj : instances.getDeclaredFields()) {
                    setFieldAccessibleHandle.invoke(obj, true);
                    String name = (String) getFieldNameHandle.invoke(obj);
                    if (name.equals(fieldName)) {
                        setFieldHandle.invoke(obj, instanceToModify, newValue);
                        return;
                    }
                }
                for (Object obj : instances.getFields()) {
                    setFieldAccessibleHandle.invoke(obj, true);
                    String name = (String) getFieldNameHandle.invoke(obj);
                    if (name.equals(fieldName)) {
                        setFieldHandle.invoke(obj, instanceToModify, newValue);
                        return;
                    }
                }
                instances = instances.getSuperclass();
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean hasMethodOfName(String name, Object instance) {
        try {
            for (Object method : instance.getClass().getMethods()) {
                if (getMethodNameHandle.invoke(method).equals(name)) {
                    return true;
                }
            }
            return false;
        } catch (Throwable e) {
            print(e);
            return false;
        }
    }

    public static Class<?>[] getMethodParamTypes(Object method) {
        try {
            return (Class<?>[]) getParameterTypesHandle.invoke(method);
        } catch (Throwable e) {
            print(e);
            throw new RuntimeException(e);
        }
    }

    public static void logClasses(Object instance) {
        print("---------------------------------");
        print("CLASSES FOR:", instance.getClass());
        print("---------------------------------");
        try {
            Class<?>[] classes = instance.getClass().getDeclaredClasses();
            for (Class<?> cls : classes) {
                logger.info("Class: " + cls.getCanonicalName());
    
                Object[] constructors = cls.getDeclaredConstructors();
                for (Object constructor : constructors) {
                    StringBuilder paramString = new StringBuilder();
                    Class<?>[] paramTypes = (Class<?>[]) getConstructorParameterTypesHandle.invoke(constructor);
                    for (int i = 0; i < paramTypes.length; i++) {
                        if (i > 0) paramString.append(", ");
                        paramString.append(paramTypes[i].getCanonicalName());
                    }
                    logger.info("  Constructor: " + cls.getSimpleName() + "(" + paramString.toString() + ")");
                }
            }
        } catch (Throwable e) {
            print(e);
            logger.info("Error logging classes: ", e);
        }
    }

    public static void logClasses(Class<?> masterClass) {
        print("---------------------------------");
        print("CLASSES FOR:", masterClass);
        print("---------------------------------");
        try {
            Class<?>[] classes = masterClass.getDeclaredClasses();
            for (Class<?> cls : classes) {
                logger.info("Class: " + cls.getCanonicalName());
    
                Object[] constructors = cls.getDeclaredConstructors();
                for (Object constructor : constructors) {
                    StringBuilder paramString = new StringBuilder();
                    Class<?>[] paramTypes = (Class<?>[]) getConstructorParameterTypesHandle.invoke(constructor);

                    for (int i = 0; i < paramTypes.length; i++) {
                        if (i > 0) paramString.append(", ");
                        paramString.append(paramTypes[i].getCanonicalName());
                    }
                    logger.info("  Constructor: " + cls.getSimpleName() + "(" + paramString.toString() + ")");
                }
            }
        } catch (Throwable e) {
            print(e);
            logger.info("Error logging classes: ", e);
        }
    }

    public static void logMethod(Object method) throws Throwable {
        String methodName = (String) getMethodNameHandle.invoke(method);
        Object genericReturnType = getGenericReturnTypeHandle.invoke(method);
        Object[] paramTypes = (Object[]) getGenericParameterTypesHandle.invoke(method);
        int modifiers = (int) getModifiersHandle.invoke(method);
        String static_ = isStatic(modifiers) ? " static " : " ";
        String final_ = isFinal(modifiers) ? "final " : "";

        StringBuilder paramString = new StringBuilder();
        for (Object paramType : paramTypes) {
            if (paramString.length() > 0) paramString.append(", ");
            paramString.append(String.valueOf(paramType));
        }
        logger.info(getVisibility(modifiers) + static_ + final_ + String.valueOf(genericReturnType) + " " + methodName + "(" + paramString.toString() + ")");
    }

    public static void logConstructor(Object constructor, Class<?> cls) throws Throwable {
        Object[] paramTypes = (Object[]) getConstructorGenericParameterTypesHandle.invoke(constructor);

        StringBuilder paramString = new StringBuilder();
        for (Object paramType : paramTypes) {
            if (paramString.length() > 0) paramString.append(", ");
            paramString.append(String.valueOf(paramType));
        }
        logger.info("public " + cls.getSimpleName() + "(" + paramString.toString() + ")");
    }

    public static void logConstructors(Object instance) {
        logConstructors(instance.getClass());
    }

    public static void logConstructors(Class<?> cls) {
        try {
            Object[] ctors = cls.getDeclaredConstructors();
            for (Object ctor : ctors) {
                logConstructor(ctor, cls);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void logMethods(Object instance) {
        logMethods(instance.getClass());
    }

    public static void logMethods(Class<?> cls) {
        print("---------------------------------");
        print("METHODS FOR:", cls);
        print("---------------------------------");
        try {
            logConstructors(cls);
            List<Class<?>> classHierarchy = new ArrayList<>();
            Class<?> currentClass = cls;
            while (currentClass != null) {
                classHierarchy.add(0, currentClass);
                currentClass = currentClass.getSuperclass();
            }
            for (Class<?> hierarchyClass : classHierarchy) {
                for (Object method : hierarchyClass.getDeclaredMethods()) {
                    logMethod(method);
                }
            }
        } catch (Throwable e) {
            print(e);
            logger.info("Error logging methods: ", e);
        }
    }

    public static HashMap<String, Object> getMethods(Object instance) {
        HashMap<String, Object> methods = new HashMap<>();
        try {
            for (Object method : instance.getClass().getDeclaredMethods()) {
                String methodName = (String) getMethodNameHandle.invoke(method);
                methods.put(methodName, method);
            }
        } catch (Throwable ignored) {}
        return methods;
    }

    public static Object getMethod(String methodName, Object instance, int paramCount) {
        for (Object method : instance.getClass().getMethods()) {
            try {
                if (((String)getMethodNameHandle.invoke(method)).equals(methodName) && 
                    ((Object[])getParameterTypesHandle.invoke(method)).length == paramCount) {
                    return method;
                }
            } catch (Throwable e) {
                print(e);
            }
        }
        return null;
    }

    public static Object getMethod(String methodName, Class<?> cls, int paramCount) {
        for (Object method : cls.getMethods()) {
            try {
                if (((String)getMethodNameHandle.invoke(method)).equals(methodName) && 
                    ((Object[])getParameterTypesHandle.invoke(method)).length == paramCount) {
                    return method;
                }
            } catch (Throwable e) {
                print(e);
            }
        }
        return null;
    }

    public static Object getMethodDeclared(String methodName, Class<?> cls, int paramCount) {
        for (Object method : cls.getDeclaredMethods()) {
            try {
                if (((String)getMethodNameHandle.invoke(method)).equals(methodName) && 
                    ((Object[])getParameterTypesHandle.invoke(method)).length == paramCount) {
                    return method;
                }
            } catch (Throwable e) {
                print(e);
            }
        }
        return null;
    }

    public static Object getMethodByParamTypes(Object instance, Class<?>[] parameterTypes) {
        for (Object method : instance.getClass().getDeclaredMethods()) {
            try {
                Class<?>[] targetParameterTypes = (Class<?>[]) getParameterTypesHandle.invoke(method);
                if (targetParameterTypes.length != parameterTypes.length)
                    continue;

                boolean match = true;
                for (int i = 0; i < targetParameterTypes.length; i++) {
                    if (!targetParameterTypes[i].equals(parameterTypes[i])) {
                        match = false;
                        break;
                    }
                }

                if (match) return method;
            } catch (Throwable e) {
                print(e);
            }
        }
        return null;
    }

    public static List<Object> getMethodsByParamTypes(Object instance, Class<?>[] parameterTypes) {
        List<Object> methods = new ArrayList<>();

        for (Object method : instance.getClass().getDeclaredMethods()) {
            try {
                Class<?>[] targetParameterTypes = (Class<?>[]) getParameterTypesHandle.invoke(method);
                if (targetParameterTypes.length != parameterTypes.length)
                    continue;

                boolean match = true;
                for (int i = 0; i < targetParameterTypes.length; i++) {
                    if (!targetParameterTypes[i].equals(parameterTypes[i])) {
                        match = false;
                        break;
                    }
                }

                if (match) methods.add(method);
            } catch (Throwable e) {
                print(e);
            }
        }
        return methods;
    }

    public static Object getMethodExplicit(String methodName, Object instance, Class<?>[] parameterTypes) {
        for (Object method : instance.getClass().getMethods()) {
            try {
                if (((String) getMethodNameHandle.invoke(method)).equals(methodName)) {
                    Class<?>[] targetParameterTypes = (Class<?>[]) getParameterTypesHandle.invoke(method);
                    if (targetParameterTypes.length != parameterTypes.length)
                        continue;
    
                    boolean match = true;
                    for (int i = 0; i < targetParameterTypes.length; i++) {
                        if (!targetParameterTypes[i].getCanonicalName().equals(parameterTypes[i].getCanonicalName())) {
                            match = false;
                            break;
                        }
                    }
    
                    if (match) return method;
                }
            } catch (Throwable e) {
                print(e);
            }
        }
        return null;
    }

    public static Object getMethodExplicit(String methodName, Class<?> cls, Class<?>[] parameterTypes) {
        for (Object method : cls.getMethods()) {
            try {
                if (((String) getMethodNameHandle.invoke(method)).equals(methodName)) {
                    Class<?>[] targetParameterTypes = (Class<?>[]) getParameterTypesHandle.invoke(method);
                    if (targetParameterTypes.length != parameterTypes.length)
                        continue;
    
                    boolean match = true;
                    for (int i = 0; i < targetParameterTypes.length; i++) {
                        if (!targetParameterTypes[i].getCanonicalName().equals(parameterTypes[i].getCanonicalName())) {
                            match = false;
                            break;
                        }
                    }
    
                    if (match) return method;
                }
            } catch (Throwable e) {
                print(e);
            }
        }
        return null;
    }

    public static List<Object> getMethodsByReturnType(Class<?> cls, Class<?> returnType, int numArgs) {
        List<Object> methods = new ArrayList<>();
        for (Object method : cls.getDeclaredMethods()) {
            try {
                Class<?>[] targetParamTypes = (Class<?>[]) getParameterTypesHandle.invoke(method);
                if (numArgs != targetParamTypes.length) continue;

                Class<?> targetReturnType = (Class<?>) getReturnTypeHandle.invoke(method);
                if (targetReturnType.equals(returnType)) methods.add(method);

            } catch (Throwable e) {
                print(e);
            }
        }
        return methods;
    }

    public static List<Object> getMethodsByReturnType(Class<?> cls, Class<?> returnType) {
        List<Object> methods = new ArrayList<>();
        for (Object method : cls.getDeclaredMethods()) {
            try {
                Class<?> targetReturnType = (Class<?>) getReturnTypeHandle.invoke(method);
                if (targetReturnType.equals(returnType)) methods.add(method);
            } catch (Throwable e) {
                print(e);
            }
        }
        return methods;
    }

    public static Object invokeMethod(String methodName, Object instance, Object... arguments) {
        try {
            Object method = instance.getClass().getMethod(methodName);
            return invokeMethodHandle.invoke(method, instance, arguments);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getMethodAndInvokeDirectly(String methodName, Object instance, int argumentsNum, Object... arguments) {
        Object method = getMethod(methodName, instance, argumentsNum);
        if (method == null) return null;
        return invokeMethodDirectly(method, instance, arguments);
    }

    public static Object getMethodDeclaredAndInvokeDirectly(String methodName, Object instance, int argumentsNum, Object... arguments) {
        Object method = getMethodDeclared(methodName, instance.getClass(), argumentsNum);
        if (method == null) return null;
        return invokePrivateMethodDirectly(method, instance, arguments);
    }

    public static Object getMethodExplicitAndInvokeDirectly(String methodName, Object instance, Class<?>[] parameterTypes, Object... arguments) {
        Object method = getMethodExplicit(methodName, instance, parameterTypes);
        if (method == null) return null;
        return invokeMethodDirectly(method, instance, arguments);
    }

    public static Object invokeMethodDirectly(Object method, Object instance, Object... arguments) {
        try {
            return invokeMethodHandle.invoke(method, instance, arguments);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Object invokePrivateMethodDirectly(Object method, Object instance, Object... arguments) {
        try {
            setMethodAccessable.invoke(method, true);
            return invokeMethodHandle.invoke(method, instance, arguments);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Object invokeNonPrivateMethodDirectly(Object method, Object instance, Object... arguments) {
        try {
            if (isPrivate((int)getModifiersHandle.invoke(method))) return null;
            return invokeMethodHandle.invoke(method, instance, arguments);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Object invokeMethodDirectly(Object method, Object instance) {
        try {
            return invokeMethodHandle.invoke(method, instance);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Object instantiateClass(String canonicalName, Class<?>[] paramTypes, Object... params) {
        try {
            Class<?> clazz = Class.forName(canonicalName, false, Class.class.getClassLoader());
            Object ctor = clazz.getDeclaredConstructor(paramTypes);
            setConstructorAccessibleHandle.invoke(ctor, true);
            return constructorNewInstanceHandle.invoke(ctor, params);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Class<?>[]> getConstructorParamTypes(Class<?> cls) {
        Object[] ctors = cls.getDeclaredConstructors();
        List<Class<?>[]> lst = new ArrayList<>();

        try {
            for (Object ctor : ctors) {
                Class<?>[] ctorParams = (Class<?>[]) getConstructorParameterTypesHandle.invoke(ctor);
                lst.add(ctorParams);
            }
            return lst;

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Class<?>[] getConstructorParamTypesSingleConstructor(Class<?> cls) {
        Object ctor = cls.getDeclaredConstructors()[0];
        try {
            return (Class<?>[]) getConstructorParameterTypesHandle.invoke(ctor);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Object instantiateClass(Class<?> clazz, Class<?>[] paramTypes, Object... params) {
        try {
            Object ctor = clazz.getDeclaredConstructor(paramTypes);
            setConstructorAccessibleHandle.invoke(ctor, true);
            return constructorNewInstanceHandle.invoke(ctor, params);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Class<?> getClazz(String canonicalName) {
        try {
            return Class.forName(canonicalName);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void logEnumConstantNames(Class<?> clazz) {
        try {
            if (!clazz.isEnum()) throw new IllegalArgumentException("Not an enum");
    
            Object[] constants = clazz.getEnumConstants();

            for (Object constant : constants) {
                print(constant);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Object getEnumConstantByName(String canonicalName, String constantName) {
        try {
            Class<?> clazz = Class.forName(canonicalName);
            if (!clazz.isEnum()) throw new IllegalArgumentException("Not an enum");
            return Enum.valueOf((Class<? extends Enum>) clazz, constantName);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean doInstantiationParamsMatch(Class<?> cls, Class<?>[] targetParams) {
        Object[] ctors = cls.getDeclaredConstructors();
        for (Object ctor : ctors) {
            try {
                Class<?>[] ctorParams = (Class<?>[]) getConstructorParameterTypesHandle.invoke(ctor);
                if (ctorParams.length != targetParams.length) continue;

                boolean match = true;
                for (int i = 0; i < ctorParams.length; i++) {
                    if (!ctorParams[i].equals(targetParams[i])) {
                        match = false;
                        break;
                    }
                }
                if (match) return true;

            } catch (Throwable e) {
                print(e);
            }
        }
        return false;
    }

    public static Pair<Object, Class<?>[]> getMethodFromSuperclass(String methodName, Object instance) {
        Class<?> currentClass = instance.getClass();

        while (currentClass != null) {
            // Retrieve all declared methods in the current class
            Object[] methods = currentClass.getDeclaredMethods();

            for (Object method : methods) {
                try {
                    // Check if the method name matches
                    if (getMethodNameHandle.invoke(method).equals(methodName)) {
                        // Invoke the MethodHandle to get the parameter types
                        Class<?>[] parameterTypes = (Class<?>[]) getParameterTypesHandle.invoke(method);
                        return new Pair<>(method, parameterTypes);
                    }
                } catch (Throwable e) {
                    print(e);
                    e.printStackTrace();  // Handle any reflection errors
                }
            }
            // Move to the superclass if no match is found
            currentClass = currentClass.getSuperclass();
        }

        // Return null if the method was not found in the class hierarchy
        return null;
    }
    public static Object invokeStaticMethodWithAutoProjection(Class<?> targetClass, String methodName, Object... arguments) {
        try {
            // Find the method by its name and parameter types
            Object[] methods = targetClass.getDeclaredMethods();

            Object matchingMethod = null;
            Class<?>[] parameterTypes = null;

            for (Object method : methods) {
                // Get the method name dynamically
                String currentName = (String) getMethodNameHandle.invoke(method);

                // Check if names match and method is static
                int modifiers = (int) getModifiersHandle.invoke(method);
                if (currentName.equals(methodName) && (modifiers & 0x0008) != 0) { // Static check
                    // Retrieve parameter types
                    parameterTypes = (Class<?>[]) getParameterTypesHandle.invoke(method);
                    if(parameterTypes.length== arguments.length){
                        matchingMethod = method;
                        break;
                    }

                }
            }

            if (matchingMethod == null) {
                throw new NoSuchMethodException("Static method " + methodName + " not found in class " + targetClass.getName());
            }

            // Project arguments to the correct types
            Object[] projectedArgs = new Object[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                Object arg = (arguments.length > i) ? arguments[i] : null;

                if (arg == null) {
                    if (parameterTypes[i].isPrimitive()) {
                        throw new IllegalArgumentException("Null cannot be used for primitive type: " + parameterTypes[i].getName());
                    }
                    projectedArgs[i] = null;
                } else {
                    projectedArgs[i] = convertArgument(arg, parameterTypes[i]);
                }
            }

            // Ensure the method is accessible
            setMethodAccessable.invoke(matchingMethod, true);

            // Invoke the static method (pass null as the instance for static methods)
            return invokeMethodHandle.invoke(matchingMethod, null, projectedArgs);
        } catch (Throwable e) {
            // if (e instanceof InvocationTargetException) {
            //     Throwable cause = ((InvocationTargetException) e).getTargetException();
            //     System.err.println("Root cause of InvocationTargetException: " + cause.getClass().getName());
            //     cause.printStackTrace(); // Print root cause
            // } else {
            //     e.printStackTrace();
            // }
            throw new RuntimeException(e);
        }
    }

    public static Object invokeMethodWithAutoProjection(String methodName, Object instance, Object... arguments) {
        // Retrieve the method and its parameter types
        Pair<Object, Class<?>[]> methodPair = getMethodFromSuperclass(methodName, instance);

        // Check if the method was found
        if (methodPair == null) {
            try {
                throw new NoSuchMethodException("Method " + methodName + " not found in class hierarchy of " + instance.getClass().getName());
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        Object method = methodPair.one;
        Class<?>[] parameterTypes = methodPair.two;

        // Prepare arguments by projecting them to the correct types
        Object[] projectedArgs = new Object[parameterTypes.length];
        for (int index = 0; index < parameterTypes.length; index++) {
            Object arg = (arguments.length > index) ? arguments[index] : null;

            if (arg == null) {
                // If the expected type is a primitive type, throw an exception
                if (parameterTypes[index].isPrimitive()) {
                    throw new IllegalArgumentException("Argument at index " + index + " cannot be null for primitive type " + parameterTypes[index].getName());
                }
                projectedArgs[index] = null; // Keep nulls as null for reference types
            } else {
                // Try to convert the argument to the expected parameter type
                try {
                    projectedArgs[index] = convertArgument(arg, parameterTypes[index]);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Cannot convert argument at index " + index + " to " + parameterTypes[index].getName(), e);
                }
            }
        }

        // Ensure the method is accessible
        try {
            setMethodAccessable.invoke(method, true);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        // Invoke the method with the projected arguments
        try {
            return invokeMethodHandle.invoke(method, instance, projectedArgs);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // Helper function to convert an argument to the expected type
    public static Object convertArgument(Object arg, Class<?> targetType) {
        if (targetType.isAssignableFrom(arg.getClass())) {
            return arg; // Use as-is if types match
        } else if (targetType.isPrimitive()) {
            // Handle primitive types by boxing
            if (targetType == int.class) {
                return ((Number) arg).intValue();
            } else if (targetType == long.class) {
                return ((Number) arg).longValue();
            } else if (targetType == double.class) {
                return ((Number) arg).doubleValue();
            } else if (targetType == float.class) {
                return ((Number) arg).floatValue();
            } else if (targetType == short.class) {
                return ((Number) arg).shortValue();
            } else if (targetType == byte.class) {
                return ((Number) arg).byteValue();
            } else if (targetType == boolean.class) {
                return arg;
            } else if (targetType == char.class) {
                return arg;
            } else {
                throw new IllegalArgumentException("Unsupported primitive type: " + targetType.getName());
            }
        } else {
            // For reference types, perform a cast if possible
            return targetType.cast(arg);
        }
    }
    public static Object invokeStaticMethod(Class<?> targetClass, String methodName, Object... arguments) {
        try {
            // Retrieve the parameter types of the arguments
            Class<?>[] parameterTypes = new Class[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                parameterTypes[i] = arguments[i].getClass();
            }

            // Find the method by its name and parameter types
            Object method = findStaticMethodByParameterTypes(targetClass, parameterTypes);
            if (method == null) {
                throw new NoSuchMethodException("Static method " + methodName + " not found in class " + targetClass.getName());
            }

            // Invoke the method (static methods do not need an instance)
            return invokeMethodHandle.invoke(method, null, arguments);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    
    @SuppressWarnings("unused")
    public static Object findFieldByType(Object targetObject, Class<?> fieldType) {
        try {
            Class<?> currentClass = targetObject.getClass();

            while (currentClass != null) {
                // Retrieve all declared fields dynamically
                Object[] fields = currentClass.getDeclaredFields();

                for (Object field : fields) {
                    try {
                        // Retrieve field type dynamically
                        Class<?> fieldClass = (Class<?>) invokeMethodWithAutoProjection("getType",field);

                        // Check if the field type matches or is assignable
                        if (fieldClass.isAssignableFrom(fieldType)) {
                            setFieldAccessibleHandle.invoke(field, true);
                            String name = (String) getFieldNameHandle.invoke(field);
                            return  getFieldHandle.invoke(field, targetObject);
                        }
                    } catch (Throwable e) {
                        print(e);
                        // Handle exceptions gracefully during field inspection
                        e.printStackTrace();
                    }
                }

                // Move to the superclass dynamically
                currentClass = (Class<?>) invokeMethodHandle.invoke(currentClass, "getSuperclass");
            }

            // Return null if no matching field is found
            return null;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    public static Object findStaticMethodByParameterTypes(Class<?> targetClass, Class<?>... parameterTypes) {
        try {
            Class<?> currentClass = targetClass;

            while (currentClass != null) {
                // Retrieve all declared methods dynamically
                Object[] methods = currentClass.getDeclaredMethods();

                for (Object method : methods) {
                    try {
                        // Retrieve method modifiers dynamically
                        int modifiers = (int) getModifiersHandle.invoke(method);

                        // Check if the method is static
                        if ((modifiers & 0x0008) != 0) { // 0x0008 is the `static` modifier bit
                            // Retrieve parameter types dynamically
                            Class<?>[] methodParamTypes = (Class<?>[]) getParameterTypesHandle.invoke(method);

                            // Compare parameter types
                            if (areParameterTypesMatching(methodParamTypes, parameterTypes)) {
                                return method; // Return the matching method
                            }
                        }
                    } catch (Throwable e) {
                        print(e);
                        // Handle exceptions gracefully during method inspection
                        e.printStackTrace();
                    }
                }

                // Move to the superclass dynamically
                currentClass = (Class<?>) invokeMethodHandle.invoke(currentClass, "getSuperclass");
            }

            // Return null if no matching method is found
            return null;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // Helper function to compare parameter types
    private static boolean areParameterTypesMatching(Class<?>[] methodParamTypes, Class<?>[] targetParamTypes) {
        if (methodParamTypes.length != targetParamTypes.length) {
            return false;
        }

        for (int i = 0; i < methodParamTypes.length; i++) {
            if (!methodParamTypes[i].isAssignableFrom(targetParamTypes[i])) {
                return false;
            }
        }

        return true;
    }

    public static void logField(String fieldName, Class<?> fieldType, Object field, int i) throws Throwable {
        if (List.class.isAssignableFrom(fieldType) || Map.class.isAssignableFrom(fieldType)
        || Set.class.isAssignableFrom(fieldType) || Collection.class.isAssignableFrom(fieldType)) {
            print(getGenericTypeHandle.invoke(field), fieldName, i);
        } else {
            print(fieldType.getCanonicalName(), fieldName, i);
        }
    }

    public static void logField(String fieldName, Class<?> fieldType, Object field, int i, Object instance) throws Throwable {
        setFieldAccessibleHandle.invoke(field, true);

        if (List.class.isAssignableFrom(fieldType) || Map.class.isAssignableFrom(fieldType)
        || Set.class.isAssignableFrom(fieldType) || Collection.class.isAssignableFrom(fieldType)) {
            print(getGenericTypeHandle.invoke(field), fieldName, i, getFieldHandle.invoke(field, instance));
        } else {
            print(fieldType.getCanonicalName(), fieldName, i, getFieldHandle.invoke(field, instance));
        }
    }

    public static boolean isNativeJavaClass(Class<?> clazz) {
        if (clazz == null || clazz.getPackage() == null) return false;
        String pkg = clazz.getPackage().getName();
        return "java.lang".equals(pkg) || "java.util".equals(pkg);
    }

    public static Object getFieldAtIndex(Object instance, int index) {
        try {
            int i = 0;
            Class<?> currentClass = instance.getClass();
            while (currentClass != null) {
                for (Object field : currentClass.getDeclaredFields()) {
                    if (i == index) {
                        setFieldAccessibleHandle.invoke(field, true);
                        return getFieldHandle.invoke(field, instance);
                    }
                    i++;
                }
                currentClass = currentClass.getSuperclass();
            }
            return null;
        } catch (Throwable e) {
            print(e);
            return null;
        }
    }

    public static void setFieldAtIndex(Object instance, int index, Object value) {
        try {
            int i = 0;
            Class<?> currentClass = instance.getClass();
            while (currentClass != null) {
                for (Object field : currentClass.getDeclaredFields()) {
                    if (i == index) {
                        setFieldAccessibleHandle.invoke(field, true);
                        setFieldHandle.invoke(field, instance, value);
                        return;
                    }
                    i++;
                }
                currentClass = currentClass.getSuperclass();
            }
        } catch (Throwable e) {
            print(e);
        }
    }

    public static Class<?> getFieldTypeAtIndex(Object instance, int index) {
        try {
            int i = 0;
            Class<?> currentClass = instance.getClass();
            while (currentClass != null) {
                for (Object field : currentClass.getDeclaredFields()) {
                    if (i == index) {
                        return (Class<?>) getFieldTypeHandle.invoke(field);
                    }
                    i++;
                }
                currentClass = currentClass.getSuperclass();
            }
            return null;
        } catch (Throwable e) {
            print(e);
            return null;
        }
    }

    public static Class<?> getFieldType(Object field) {
        try {
            return (Class<?>) getFieldTypeHandle.invoke(field);
        } catch (Throwable e) {
            print(e);
            return null;
        }
    }

    public static void logFields(Object instance) {
        if (instance == null) return;
        try {
            print("---------------------------------");
            print("FIELDS FOR:", instance.getClass());
            print("---------------------------------");
            int i = 0;
            Class<?> currentClass = instance.getClass();
            while (currentClass != null) {
                for (Object field : currentClass.getDeclaredFields()) {
                    String fieldName = (String) getFieldNameHandle.invoke(field);
                    Class<?> fieldType = (Class<?>) getFieldTypeHandle.invoke(field);
                    
                    // if (fieldType.isPrimitive() || (fieldType.isArray() && fieldType.getComponentType().isPrimitive())) {
                    //     print(fieldType.getCanonicalName() + " " + fieldName + " " + i);
                    //     i++;
                    //     continue;
                    // } else {
                        logField(fieldName, fieldType, field, i, instance);
                    // }
                    try {
                        ReflectionUtilis.logConstructors(fieldType);
                    } catch (Exception e) {
                        print(e);
                    }
                    i++;
                }
                currentClass = currentClass.getSuperclass();
            }
        } catch (Throwable e) {
            logger.info("Error logging fields: ", e);
        }
    }

    public static void logFields(Class<?> cls) {
        try {
            print("---------------------------------");
            print("FIELDS FOR:", cls);
            print("---------------------------------");
            int i = 0;
            Class<?> currentClass = cls;
            while (currentClass != null) {
                for (Object field : currentClass.getDeclaredFields()) {
                    String fieldName = (String) getFieldNameHandle.invoke(field);
                    Class<?> fieldType = (Class<?>) getFieldTypeHandle.invoke(field);
                    
                    if (fieldType.isPrimitive() || (fieldType.isArray() && fieldType.getComponentType().isPrimitive())) {
                        print(fieldType.getCanonicalName() + " " + fieldName + " " + i);
                        i++;
                        continue;
                    } else {
                        logField(fieldName, fieldType, field, i);
                    }
                    try {
                        ReflectionUtilis.logConstructors(fieldType);
                    } catch (Exception e) {
                        print(e);
                    }
                    i++;
                }
                currentClass = currentClass.getSuperclass();
            }
        } catch (Throwable e) {
            logger.info("Error logging fields: ", e);
        }
    }

    public static void logFieldsOfFieldIndex(Object instance, int index) {
        try {
            int i = 0;
            Class<?> currentClass = instance.getClass();
            while (currentClass != null) {
                for (Object field : currentClass.getDeclaredFields()) {
                    if (i == index) {
                        logger.info("---------------------------------------------");
                        Class<?> fieldType = (Class<?>) getFieldTypeHandle.invoke(field);
                        if (fieldType.isPrimitive()) return;
                        int j = 0;
                        for (Object childField : fieldType.getDeclaredFields()) {
                            String childFieldName = (String) getFieldNameHandle.invoke(childField);
                            Class<?> childFieldType = (Class<?>) getFieldTypeHandle.invoke(childField);

                            logField(childFieldName, childFieldType, childField, j);
                            j++;
                        }
            
                        try {
                            ReflectionUtilis.logConstructors(fieldType);
                        } catch (Exception e) {
                            print(e);
                        }
                        return;
                    }
                    i++;
                }
                currentClass = currentClass.getSuperclass();
            }
        } catch (Throwable e) {
            logger.info("Error logging fields: ", e);
        }
    }

    public static boolean isPublic(int modifiers) {
        return (modifiers & 1) != 0;
    }

    public static boolean isStatic(int modifiers) {
        return (modifiers & 8) != 0;
    }

    public static boolean isFinal(int modifiers) {
        return (modifiers & 16) != 0;
    }

    public static boolean isPrivate(int modifiers) {
        return (modifiers & 2) != 0;
    }

    public static boolean isProtected(int modifiers) {
        return (modifiers & 4) != 0;
    }

    public static String getVisibility(int modifiers) {
        if (isPublic(modifiers)) return "public";
        if (isPrivate(modifiers)) return "private";
        if (isProtected(modifiers)) return "protected";
        return "package-private";
    }
}