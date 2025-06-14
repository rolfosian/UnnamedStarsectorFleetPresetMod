package data.scripts.util;

import java.util.*;
import java.awt.Robot;
import java.awt.AWTException;
import java.awt.event.InputEvent;
import org.apache.log4j.Logger;
import com.fs.starfarer.api.Global;

public class PresetMiscUtils {
    private static final Logger logger = Global.getLogger(PresetMiscUtils.class);

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

    public static LinkedHashMap<String, String> sortByKeyAlphanumerically(HashMap<String, String> input, boolean ascending) {
        List<String> keys = new ArrayList<>(input.keySet());
    
        keys.sort((s1, s2) -> {
            int i = 0, j = 0;
            while (i < s1.length() && j < s2.length()) {
                char c1 = s1.charAt(i);
                char c2 = s2.charAt(j);
    
                if (Character.isDigit(c1) && Character.isDigit(c2)) {
                    int start1 = i, start2 = j;
                    while (i < s1.length() && Character.isDigit(s1.charAt(i))) i++;
                    while (j < s2.length() && Character.isDigit(s2.charAt(j))) j++;
                    String num1 = s1.substring(start1, i);
                    String num2 = s2.substring(start2, j);
                    int cmp = Long.compare(Long.parseLong(num1), Long.parseLong(num2));
                    if (cmp != 0) return ascending ? cmp : -cmp;
                } else {
                    int cmp = Character.compare(
                        Character.toLowerCase(c1),
                        Character.toLowerCase(c2)
                    );
                    if (cmp != 0) return ascending ? cmp : -cmp;
                    i++;
                    j++;
                }
            }
            return ascending ? Integer.compare(s1.length(), s2.length()) : Integer.compare(s2.length(), s1.length());
        });
    
        LinkedHashMap<String, String> sortedMap = new LinkedHashMap<>();
        for (String key : keys) {
            sortedMap.put(key, input.get(key));
        }
    
        return sortedMap;
    }

    public static void print(Object... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i] instanceof String ? (String) args[i] : String.valueOf(args[i]));
            if (i < args.length - 1) sb.append(' ');
        }
        logger.info(sb.toString());
    }

}
