// credit for this goes to the author of the code in the Ashes of the Domain mod

package data.scripts.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class ReflectionBetterUtilis {
    // Get a MethodHandle for the specified method's getParameterTypes method
    public static MethodHandle getParameterTypesHandle(Class<?> ref, String name) {
        MethodType methodType = MethodType.methodType(Class[].class);
        try {
            // Find the getParameterTypes method and return its MethodHandle
            return MethodHandles.lookup().findVirtual(ref, name, methodType);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}