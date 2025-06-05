package data.scripts.listeners;

import data.scripts.util.ReflectionUtilis;


public class ProxyTriggerNew implements Triggerable {
    private final Object proxy;

    public ProxyTriggerNew(Class<?> interfc, final String methodName) {
        proxy = ReflectionUtilis.getProxyInstance(this,
            methodName,
            interfc.getClassLoader(),
            new Class<?>[] {interfc}
        );
    }

    @Override public void trigger(Object... args) {};
    
    public Object getProxy() {
        return proxy;
    }
}