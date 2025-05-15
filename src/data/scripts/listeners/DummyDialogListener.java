package data.scripts.listeners;

import org.apache.log4j.Logger;

public class DummyDialogListener extends DialogDismissedListener {
    public static final Logger logger = Logger.getLogger(DummyDialogListener.class);
    
    @Override
    public void trigger(Object... args) {
        // int option = (int) args[1];

        // if (option == 0) {
        //     // confirm
        //     return;
        // } else if (option == 1) {
        //     // cancel
        //     return;
        // }
    }
}