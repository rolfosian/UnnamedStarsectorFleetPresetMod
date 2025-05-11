package data.scripts.util;

import org.apache.log4j.Logger;
import org.lwjgl.opengl.GL11;
import java.util.*;
import data.scripts.FleetPresetManagerPlugin;


public class HighlightRectangle {
    
    // Logger logger = Logger.getLogger(FleetPresetManagerPlugin.class);

    private float x, y, width, height;
    // private float centerX, centerY;

    private boolean highlighted = false;
    public float highlightAlpha = 0f;
    private final float maxAlpha = 0.5f;
    private final float fadeInTime = 0.25f;

    int red;
    int green;
    int blue;

    public HighlightRectangle(float x, float y, float width, float height, int red, int green, int blue) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.red = red;
        this.green = green;
        this.blue = blue;
    }
    
    public void setHighlightFalse() {
        highlighted = false;
    }

    public void setCoordsAndSize(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.width = w;
        this.height = h;
        
    }

    public void setHighlightTrue() {
        // logger.info("RECT " + String.valueOf(this.x) + " " + String.valueOf(this.y));

        highlighted = true;
    }

    public void highlight(float amount) {
        if (!highlighted) return;
    
        if (highlightAlpha < maxAlpha) {
            highlightAlpha += amount * (maxAlpha / fadeInTime);
            if (highlightAlpha > maxAlpha) highlightAlpha = maxAlpha;
        }
        highlightAlpha = maxAlpha;
    
        highlighted = true;
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(red, green, blue, highlightAlpha);
    
        GL11.glBegin(GL11.GL_POLYGON);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + width, y);
        GL11.glVertex2f(x + width, y + height);
        GL11.glVertex2f(x, y + height);
        GL11.glEnd();
    }

    // public void highlight(float amount) {
    //     if (!highlighted) return;

    //     float scale = 0.01f;

    //     // GL11.glClearStencil(0);
    //     // GL11.glStencilMask(0xff);
    //     // GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);

    //     GL11.glColorMask(false, false, false, false);
    //     GL11.glEnable(GL11.GL_STENCIL_TEST);

    //     // GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xff);
    //     // GL11.glStencilMask(0xff);
    //     GL11.glStencilOp(GL11.GL_REPLACE, GL11.GL_REPLACE, GL11.GL_REPLACE);

    //     GL11.glBegin(GL11.GL_POLYGON);

    //     // Define the rectangle
    //     GL11.glVertex2f(x, y);
    //     GL11.glVertex2f(x + width, y);
    //     GL11.glVertex2f(x + width, y + height);
    //     GL11.glVertex2f(x, y + height);
    //     GL11.glEnd();

    //     GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
    //     GL11.glColorMask(true, true, true, true);

    //     // GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
    // }
    public static void endStencil() {
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }
}