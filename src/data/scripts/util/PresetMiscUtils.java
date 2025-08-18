package data.scripts.util;

import java.awt.Color;
import java.util.*;
import org.apache.log4j.Logger;
import org.lwjgl.opengl.GL11;

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

    public static void print(Class<?> cls, Object... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i] instanceof String ? (String) args[i] : String.valueOf(args[i]));
            if (i < args.length - 1) sb.append(' ');
        }
        Global.getLogger(cls).info(sb.toString());
    }

    public static void drawTaperedLine(float red, float green, float blue, float x, float y, float rotation, float length, float width, float coreRatio) {
        // float coreRatio = 0.6f; // Controls how long the middle section is
        float taperRatio = (1f - coreRatio) / 2f; // Controls taper length on each end
        float halfLength = length / 2f;
        float halfWidth = width / 2f;
    
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0f);
        GL11.glRotatef(rotation, 0f, 0f, 1f);
        GL11.glBegin(GL11.GL_QUADS);
    
        GL11.glColor4f(red, green, blue, 0f);
        GL11.glVertex2f(-halfLength, -halfWidth);
        GL11.glVertex2f(-halfLength, halfWidth);
        GL11.glColor4f(red, green, blue, 1f);
        GL11.glVertex2f(-halfLength + length * taperRatio, halfWidth);
        GL11.glVertex2f(-halfLength + length * taperRatio, -halfWidth);
    
        GL11.glColor4f(red, green, blue, 1f);
        GL11.glVertex2f(-halfLength + length * taperRatio, -halfWidth);
        GL11.glVertex2f(-halfLength + length * taperRatio, halfWidth);
        GL11.glVertex2f(halfLength - length * taperRatio, halfWidth);
        GL11.glVertex2f(halfLength - length * taperRatio, -halfWidth);
    
        GL11.glColor4f(red, green, blue, 1f);
        GL11.glVertex2f(halfLength - length * taperRatio, -halfWidth);
        GL11.glVertex2f(halfLength - length * taperRatio, halfWidth);
        GL11.glColor4f(red, green, blue, 0f);
        GL11.glVertex2f(halfLength, halfWidth);
        GL11.glVertex2f(halfLength, -halfWidth);
    
        GL11.glEnd();
        GL11.glPopMatrix();
    }

    public static float getMostCommon(float[] arr) {
        Map<Float, Integer> countMap = new HashMap<>();
        float mostCommon = arr[0];
        int maxCount = 0;

        for (float num : arr) {
            int count = countMap.getOrDefault(num, 0) + 1;
            countMap.put(num, count);
            if (count > maxCount) {
                maxCount = count;
                mostCommon = num;
            }
        }

        return mostCommon;
    }

    public static float getSmallest(float[] arr) {
        float min = arr[0];
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] < min) {
                min = arr[i];
            }
        }
        return min;
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
