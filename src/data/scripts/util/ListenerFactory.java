package data.scripts.util;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import data.scripts.ClassRefs;

public class ListenerFactory {
    private static final CallSite dialogDismissedCallSite;
    private static final CallSite actionListenerCallSite;

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Class<?> dialogDismissedParamClass = ReflectionUtilis.getMethodParamTypes(ClassRefs.dialogDismissedInterface.getDeclaredMethods()[0])[0];

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
        void trigger(Object... args);
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
    
        @Override public abstract void trigger(Object... args);
        
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
