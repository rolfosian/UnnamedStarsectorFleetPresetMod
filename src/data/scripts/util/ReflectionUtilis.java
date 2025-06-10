package data.scripts.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.Pair;

import data.scripts.ClassRefs;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;

import java.awt.Color;
import java.util.*;

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
    
    // Code taken and modified from Grand Colonies and Ashes of the Domain
    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();

    private static final Class<?> fieldClass;
    private static final Class<?> fieldArrayClass;
    private static final Class<?> methodClass;
    private static final Class<?> typeClass;
    private static final Class<?> typeArrayClass;
    private static final Class<?> parameterizedTypeClass;
    private static final Class<?> constructorClass;
    private static final Class<?> constructorArrayClass;

    private static final MethodHandle getFieldTypeHandle;
    private static final MethodHandle setFieldHandle;
    private static final MethodHandle getFieldHandle;
    private static final MethodHandle getFieldNameHandle;
    private static final MethodHandle setFieldAccessibleHandle;
    
    private static final MethodHandle getMethodNameHandle;
    private static final MethodHandle invokeMethodHandle;
    private static final MethodHandle setMethodAccessable;
    private static final MethodHandle getModifiersHandle;
    private static final MethodHandle getParameterTypesHandle;
    private static final MethodHandle getReturnTypeHandle;
    
    private static final MethodHandle getGenericTypeHandle;
    private static final MethodHandle getTypeNameHandle;
    private static final MethodHandle getActualTypeArgumentsHandle;
    
    private static final MethodHandle setConstructorAccessibleHandle;
    private static final MethodHandle getDeclaredConstructorsHandle;
    private static final MethodHandle getConstructorParameterTypesHandle;
    private static final MethodHandle constructorNewInstanceHandle;

    static {
        try {
            fieldClass = Class.forName("java.lang.reflect.Field", false, Class.class.getClassLoader());
            fieldArrayClass = Class.forName("[Ljava.lang.reflect.Field;", false, Class.class.getClassLoader());
            methodClass = Class.forName("java.lang.reflect.Method", false, Class.class.getClassLoader());
            typeClass = Class.forName("java.lang.reflect.Type", false, Class.class.getClassLoader());
            typeArrayClass = Class.forName("[Ljava.lang.reflect.Type;", false, Class.class.getClassLoader());
            parameterizedTypeClass = Class.forName("java.lang.reflect.ParameterizedType", false, Class.class.getClassLoader());
            constructorClass = Class.forName("java.lang.reflect.Constructor", false, Class.class.getClassLoader());
            constructorArrayClass = Class.forName("[Ljava.lang.reflect.Constructor;", false, Class.class.getClassLoader());

            setFieldHandle = lookup.findVirtual(fieldClass, "set", MethodType.methodType(void.class, Object.class, Object.class));
            getFieldHandle = lookup.findVirtual(fieldClass, "get", MethodType.methodType(Object.class, Object.class));
            getFieldNameHandle = lookup.findVirtual(fieldClass, "getName", MethodType.methodType(String.class));
            getFieldTypeHandle = lookup.findVirtual(fieldClass, "getType", MethodType.methodType(Class.class));
            setFieldAccessibleHandle = lookup.findVirtual(fieldClass, "setAccessible", MethodType.methodType(void.class, boolean.class));

            getMethodNameHandle = lookup.findVirtual(methodClass, "getName", MethodType.methodType(String.class));
            invokeMethodHandle = lookup.findVirtual(methodClass, "invoke", MethodType.methodType(Object.class, Object.class, Object[].class));
            setMethodAccessable = lookup.findVirtual(methodClass, "setAccessible", MethodType.methodType(void.class, boolean.class));
            getModifiersHandle = lookup.findVirtual(methodClass, "getModifiers", MethodType.methodType(int.class));
            getParameterTypesHandle = lookup.findVirtual(methodClass, "getParameterTypes", MethodType.methodType(Class[].class));
            getReturnTypeHandle = lookup.findVirtual(methodClass, "getReturnType", MethodType.methodType(Class.class));

            getGenericTypeHandle = lookup.findVirtual(fieldClass, "getGenericType", MethodType.methodType(typeClass));
            getTypeNameHandle = lookup.findVirtual(typeClass, "getTypeName", MethodType.methodType(String.class));
            getActualTypeArgumentsHandle = lookup.findVirtual(parameterizedTypeClass, "getActualTypeArguments", MethodType.methodType(typeArrayClass));

            setConstructorAccessibleHandle = lookup.findVirtual(constructorClass, "setAccessible", MethodType.methodType(void.class, boolean.class));
            getConstructorParameterTypesHandle = lookup.findVirtual(constructorClass, "getParameterTypes", MethodType.methodType(Class[].class));
            constructorNewInstanceHandle = lookup.findVirtual(constructorClass, "newInstance", MethodType.methodType(Object.class, Object[].class));
            getDeclaredConstructorsHandle = lookup.findVirtual(Class.class, "getDeclaredConstructors", MethodType.methodType(constructorArrayClass));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class ListenerFactory {
        private static final CallSite dialogDismissedCallSite;
        private static final CallSite actionListenerCallSite;

        static {
            ClassRefs.findAllClasses();
            Class<?> dialogDismissedParamClass = getMethodParamTypes(ClassRefs.dialogDismissedInterface.getDeclaredMethods()[0])[0];

            try {
                MethodHandle implementationMethodHandle;
                MethodType actualSamMethodType;
                MethodType factoryType;
                MethodType implSignature;
        
                actualSamMethodType = MethodType.methodType(void.class, dialogDismissedParamClass, int.class);
                implSignature = MethodType.methodType(void.class, Object.class, int.class);
                implementationMethodHandle = lookup.findVirtual(DialogDismissedListenerProxy.class, "dialogDismissed", implSignature);

                factoryType = MethodType.methodType(ClassRefs.dialogDismissedInterface, DialogDismissedListenerProxy.class);
                dialogDismissedCallSite = LambdaMetafactory.metafactory(
                    lookup,
                    "dialogDismissed",
                    factoryType,
                    actualSamMethodType,
                    implementationMethodHandle,
                    actualSamMethodType
                );
        
                actualSamMethodType = MethodType.methodType(void.class, Object.class, Object.class);
                implSignature = MethodType.methodType(void.class, Object.class, Object.class);
                implementationMethodHandle = lookup.findVirtual(ActionListenerProxy.class, "actionPerformed", implSignature);
        
                factoryType = MethodType.methodType(ClassRefs.actionListenerInterface, ActionListenerProxy.class);
                actionListenerCallSite = LambdaMetafactory.metafactory(
                    lookup,
                    "actionPerformed",
                    factoryType,
                    actualSamMethodType,
                    implementationMethodHandle,
                    actualSamMethodType
                );

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @FunctionalInterface
        private static interface Triggerable {
            void trigger(Object arg0, Object arg1);
        }

        // Rewritten proxy class from officer extension mod that works without using java.lang.reflect.Proxy and InvocationHandler imports
        private static abstract class ProxyTrigger implements Triggerable {
            private final Object listener;
        
            public ProxyTrigger(final String methodName) {
                try {
                    listener = createListener(methodName, this);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        
            @Override public abstract void trigger(Object arg0, Object arg1);
            
            public Object getProxy() {
                return listener;
            }
        }

        public static abstract class ActionListener extends ProxyTrigger {
            public ActionListener() {
                super("actionPerformed");
            }
        }

        public static abstract class DialogDismissedListener extends ProxyTrigger {
            public DialogDismissedListener() {
                super("dialogDismissed");
            }
        }

        @FunctionalInterface
        private static interface DummyActionListenerInterface {
            public void actionPerformed(Object arg0, Object arg1);
        }

        @FunctionalInterface
        private static interface DummyDialogDismissedInterface {
            public void dialogDismissed(Object arg0, int arg1);
        }
    
        private static class DialogDismissedListenerProxy implements DummyDialogDismissedInterface {
            private final ProxyTrigger proxyTriggerClassInstance;
    
            public DialogDismissedListenerProxy(ProxyTrigger proxyTriggerClassInstance) {
                this.proxyTriggerClassInstance = proxyTriggerClassInstance;
            }
    
            public void dialogDismissed(Object arg0, int arg1) {
                this.proxyTriggerClassInstance.trigger(arg0, arg1);
            };
        }
    
        private static class ActionListenerProxy implements DummyActionListenerInterface {
            private final ProxyTrigger proxyTriggerClassInstance;
    
            public ActionListenerProxy(ProxyTrigger proxyTriggerClassInstance) {
                this.proxyTriggerClassInstance = proxyTriggerClassInstance;
            }
    
            @Override
            public void actionPerformed(Object arg0, Object arg1) {
                proxyTriggerClassInstance.trigger(arg0, arg1);
            }
        }

        private static Object createListener(String targetMethodName, ProxyTrigger proxyTriggerInstance) throws Throwable {
            switch (targetMethodName) {
                case "dialogDismissed":
                    return dialogDismissedCallSite.getTarget().invoke(new DialogDismissedListenerProxy(proxyTriggerInstance));
                case "actionPerformed":
                    return actionListenerCallSite.getTarget().invoke(new ActionListenerProxy(proxyTriggerInstance));
                default:
                    throw new IllegalArgumentException("Unsupported method: " + targetMethodName);
            }
        }
    }

    public static void transplant(Object original, Object template) {
        try {
            Class<?> currentClass = original.getClass();
            while ((currentClass = currentClass.getSuperclass()) != null) {
                for (Object field : currentClass.getDeclaredFields()) {
                    String fieldName = (String) getFieldNameHandle.invoke(field);
                    setPrivateVariableFromSuperclass(fieldName, template, getPrivateVariableFromSuperClass(fieldName, original));
                }
            }
    
            for (Object field : original.getClass().getDeclaredFields()) {
                setPrivateVariable(field, template, getPrivateVariable((String)getFieldNameHandle.invoke(field), original));
            }
            return;
        } catch (Throwable e) {
            print(e);
            return;
        }
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
        } catch (Throwable ignored) {}
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
    
    public static void logMethods(Object instance) {
        try {
            for (Object method : instance.getClass().getMethods()) {
                logger.info("---------------------------------------------");
                String methodName = (String) getMethodNameHandle.invoke(method);
                Class<?>[] paramTypes = (Class<?>[]) getParameterTypesHandle.invoke(method);
                StringBuilder paramString = new StringBuilder();
                for (Class<?> paramType : paramTypes) {
                    if (paramString.length() > 0) paramString.append(", ");
                    paramString.append(paramType.getCanonicalName());
                }
                logger.info(methodName + "(" + paramString.toString() + ")");
            }
        } catch (Throwable e) {
            print(e);
            logger.info("Error logging methods: ", e);
        }
    }

    public static void logMethods(Class<?> cls) {
        try {
            for (Object method : cls.getMethods()) {
                logger.info("---------------------------------------------");
                String methodName = (String) getMethodNameHandle.invoke(method);
                Class<?>[] paramTypes = (Class<?>[]) getParameterTypesHandle.invoke(method);
                StringBuilder paramString = new StringBuilder();
                for (Class<?> paramType : paramTypes) {
                    if (paramString.length() > 0) paramString.append(", ");
                    paramString.append(paramType.getCanonicalName());
                }
                logger.info(methodName + "(" + paramString.toString() + ")");
            }
        } catch (Throwable e) {
            print(e);
            logger.info("Error logging methods: ", e);
        }
    }

    public static HashMap<String, Object> getMethods(Object instance) {
        HashMap<String, Object> methods = new HashMap<>();
        try {
            for (Object method : instance.getClass().getMethods()) {
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

    public static Object getMethodExplicit(String methodName, Object instance, Class<?>[] parameterTypes) {
        for (Object method : instance.getClass().getMethods()) {
            try {
                if (((String) getMethodNameHandle.invoke(method)).equals(methodName)) {
                    Class<?>[] targetParameterTypes = (Class<?>[]) getParameterTypesHandle.invoke(method);
                    if (targetParameterTypes.length != parameterTypes.length)
                        continue;
    
                    boolean match = true;
                    for (int i = 0; i < targetParameterTypes.length; i++) {
                        Class<?> targetType = targetParameterTypes[i];
                        Class<?> inputType = parameterTypes[i];
                        if (!inputType.isAssignableFrom(targetType)) {
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

    public static void logEnumConstantNames(String canonicalName) {
        try {
            Class<?> clazz = Class.forName(canonicalName);
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

    public static Object[] getConstructorParams(String canonicalName) {
        try {
            Class<?> clazz = Class.forName(canonicalName);
            return clazz.getDeclaredConstructors();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void logConstructorParams(String canonicalName) {
        Object[] ctors = getConstructorParams(canonicalName);
        for (Object ctor : ctors) {
            print(ctor);
        }
    }

    public static boolean doInstantiationParamsMatch(String canonicalName, Class<?>[] targetParams) {
        Object[] ctors = getConstructorParams(canonicalName);
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

    public static boolean isNativeJavaClass(Class<?> clazz) {
        if (clazz == null || clazz.getPackage() == null) return false;
        String pkg = clazz.getPackage().getName();
        return "java.lang".equals(pkg) || "java.util".equals(pkg);
    }

    public static Object getFieldAtIndex(Object instance, int index) {
        try {
            Object[] fields = instance.getClass().getDeclaredFields();
            setFieldAccessibleHandle.invoke(fields[index], true);
            // return getPrivateVariable((String)getFieldNameHandle.invoke(fields[index]), instance);
            return getFieldHandle.invoke(fields[index], instance);
        } catch (Throwable e) {
            print(e);
            return null;
        }
    }

    public static Class<?> getFieldTypeAtIndex(Object instance, int index) {
        try {
            Object[] fields = instance.getClass().getDeclaredFields();
            return (Class<?>) getFieldTypeHandle.invoke(fields[index]);
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
        try {
            int i = 0;
            for (Object field : instance.getClass().getDeclaredFields()) {
                print("---------------------------------------------");
                String fieldName = (String) getFieldNameHandle.invoke(field);
                Class<?> fieldType = (Class<?>) getFieldTypeHandle.invoke(field);
                
                if (fieldType.isPrimitive()) {
                    print(fieldType.getCanonicalName() + " " + fieldName + " " + i);
                    i++;
                    continue;
                } else {
                    logField(fieldName, fieldType, field, i);
                }
                try {
                    ReflectionUtilis.logConstructorParams(fieldType.getCanonicalName());
                } catch (Exception e) {
                    print(e);
                }
                i++;
            }
        } catch (Throwable e) {
            logger.info("Error logging fields: ", e);
        }
    }

    public static void logFieldsOfFieldIndex(Object instance, int index) {
        try {
            int i = 0;
            for (Object field : instance.getClass().getDeclaredFields()) {
                if (i != index) {
                    i++;
                    continue;
                }
                logger.info("---------------------------------------------");
                Class<?> fieldType = (Class<?>) getFieldTypeHandle.invoke(field);
                if (fieldType.isPrimitive()) continue;
                int j = 0;
                for (Object childField : fieldType.getDeclaredFields()) {
                    String childFieldName = (String) getFieldNameHandle.invoke(childField);
                    Class<?> childFieldType = (Class<?>) getFieldTypeHandle.invoke(childField);

                    logField(childFieldName, childFieldType, childField, j);
                    j++;
                }
    
                try {
                    ReflectionUtilis.logConstructorParams(fieldType.getCanonicalName());
                } catch (Exception e) {
                    print(e);
                }
                i++;
            }
    
        } catch (Throwable e) {
            logger.info("Error logging fields: ", e);
        }
    }
}