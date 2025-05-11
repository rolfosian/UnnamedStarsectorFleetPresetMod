// credit for this goes to the author of the code in the officer extension mod
package data.scripts.listeners;

import data.scripts.ClassRefs;

public abstract class ActionListener extends ProxyTrigger {
    public ActionListener() {
        super(ClassRefs.actionListenerInterface, "actionPerformed");
    }
}