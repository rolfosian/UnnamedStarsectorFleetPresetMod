package data.scripts.listeners;

import data.scripts.util.ReflectionUtilis;

public class ProxyTriggerNew implements Triggerable {
    private final Object listener;

    public ProxyTriggerNew(Class<?> interfc, final String methodName) {
        try {
            listener = ReflectionUtilis.ListenerFactory.getListener(interfc, this, methodName);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override public void trigger(Object... args) {};
    
    public Object getProxy() {
        return listener;
    }
}