// credit for this goes to the author of the code in the officer extension mod

package data.scripts.listeners;

import com.fs.starfarer.api.Global;
import data.scripts.ClassRefs;

public abstract class DialogDismissedListener extends ProxyTrigger {
    public DialogDismissedListener() {
        super(ClassRefs.dialogDismissedInterface, "dialogDismissed");
    }
}