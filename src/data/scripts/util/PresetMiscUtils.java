package data.scripts.util;

import java.util.*;
import java.awt.Robot;
import java.awt.AWTException;
import java.awt.event.InputEvent;
import org.apache.log4j.Logger;
import com.fs.starfarer.api.Global;

public class PresetMiscUtils {
    private static final Logger logger = Global.getLogger(PresetMiscUtils.class);
    public static void print(Object... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i] instanceof String ? (String) args[i] : String.valueOf(args[i]));
            if (i < args.length - 1) sb.append(' ');
        }
        logger.info(sb.toString());
    }

    public static boolean isVersionAfter(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        for (int i = 0; i < 3; i++) {
            String num1 = parts1[i].replaceAll("[^0-9]", "");
            String num2 = parts2[i].replaceAll("[^0-9]", "");

            int n1 = Integer.parseInt(num1);
            int n2 = Integer.parseInt(num2);

            if (n1 != n2) {
                return n1 > n2;
            }

            String letter1 = parts1[i].replaceAll("[0-9]", "");
            String letter2 = parts2[i].replaceAll("[0-9]", "");

            if (!letter1.equals(letter2)) {
                if (letter1.isEmpty()) return false;
                if (letter2.isEmpty()) return true;
                return letter1.compareTo(letter2) > 0;
            }
        }

        return false;
    }
}
