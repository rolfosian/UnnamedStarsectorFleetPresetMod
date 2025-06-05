// Code taken and modified from officer extension mod
package data.scripts.listeners;

import data.scripts.ClassRefs;

public abstract class DialogDismissedListener extends ProxyTriggerNew {
    public DialogDismissedListener() {
        super(ClassRefs.dialogDismissedInterface, "dialogDismissed");
    }
}