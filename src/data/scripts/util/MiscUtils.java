package data.scripts.util;

import java.util.*;
import java.awt.Robot;
import java.awt.AWTException;
import java.awt.event.InputEvent;

public class MiscUtils {
    public static void clickAt(float x, float y) {
        try {
            Robot robot = new Robot();
            robot.mouseMove(Math.round(x), Math.round(y));
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public static void pressKey(int keyCode) {
        try {
            Robot robot = new Robot();
            robot.keyPress(keyCode);
            robot.keyRelease(keyCode);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }
    public static List<Integer> range(int num) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            result.add(i);
        }
        return result;
    }
    public static double calculatePercentage(double rY, double percentage) {
        return rY / 100 * percentage;
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
}
