// Code taken and modified from officer extension mod
package data.scripts.listeners;

import data.scripts.ClassRefs;

public abstract class ActionListener extends ProxyTriggerNew {
    public ActionListener() {
        super(ClassRefs.actionListenerInterface, "actionPerformed");
    }
}