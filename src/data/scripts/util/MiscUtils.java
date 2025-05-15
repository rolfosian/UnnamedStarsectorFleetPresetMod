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

    public static int getSmallerAtIndex(ArrayList<Integer> larger, ArrayList<Integer> smaller, int index) {
        if (index < 0 || index >= larger.size() || index >= smaller.size()) {
            throw new IndexOutOfBoundsException("Index out of bounds");
        }
        int val1 = larger.get(index);
        int val2 = smaller.get(smaller.size() - 1 - index);
        return Math.min(val1, val2);
    }
    public static int getLargerAtIndex(ArrayList<Integer> larger, ArrayList<Integer> smaller, int index) {
        if (index < 0 || index >= larger.size() || index >= smaller.size()) {
            throw new IndexOutOfBoundsException("Index out of bounds");
        }
        int val1 = larger.get(index);
        int val2 = smaller.get(smaller.size() - 1 - index);
        return Math.max(val1, val2);
    }
}
