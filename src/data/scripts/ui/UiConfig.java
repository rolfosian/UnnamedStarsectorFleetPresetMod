package data.scripts.ui;

import java.util.List;

import com.fs.starfarer.api.Global;

public class UiConfig {
    public static final int DISPLAY_WIDTH = (int)Global.getSettings().getScreenWidthPixels();
    public static final int DISPLAY_HEIGHT = (int)Global.getSettings().getScreenHeightPixels();

    public static final float CONFIRM_DIALOG_WIDTH_DIVISOR;
    public static final float CONFIRM_DIALOG_HEIGHT_DIVISOR;
    public static final float PANEL_WIDTH_SUBTRACTOR;
    public static final float PANEL_HEIGHT_SUBTRACTOR;
    public static final float NAME_COLUMN_WIDTH_DIVISOR;
    public static final float SHIP_COLUMN_WIDTH_DIVISOR;
    // public static final float TABLE_HEADER_Y_OFFSET_PERCENT_BTM;
    // public static final float TABLE_HEADER_Y_OFFSET_PERCENT_TOP;
    public static final float SHIPLIST_PANEL_HEIGHT_SUBTRACTOR;
    public static final float SHIPLIST_PANEL_PADDING_DIVISOR;
    public static final float SHIPLIST_SCALE;
    public static final float SHIPLIST_SIZE;
    public static final float SHIPLIST_Y_OFFSET_MULTIPLIER;

    public static final float ROW_HEIGHT;

    // These are teh values for 1080p
    // CONFIRM_DIALOG_WIDTH_DIVISOR = 3.3f;
    // CONFIRM_DIALOG_HEIGHT_DIVISOR = 2f;
    // PANEL_WIDTH_SUBTRACTOR = (DISPLAY_WIDTH / 100 * 0.4f) + 10f;
    // PANEL_HEIGHT_SUBTRACTOR = (DISPLAY_HEIGHT / 100 * 1.4f);
    // NAME_COLUMN_WIDTH_DIVISOR = 3.8f;
    // SHIP_COLUMN_WIDTH_DIVISOR = 1.8f;
    // TABLE_HEADER_Y_OFFSET_PERCENT_BTM = 4f;
    // TABLE_HEADER_Y_OFFSET_PERCENT_TOP = 2.6f;
    // SHIPLIST_PANEL_HEIGHT_SUBTRACTOR = 10f;
    // SHIPLIST_Y_OFFSET_MULTIPLIER = 5f;
    // SHIPLIST_SCALE = 0.9f;
    // SHIPLIST_PANEL_PADDING_DIVISOR = 1.8f;

    // THERE HAS TO BE A BETTER WAY TO DO THIS HOLY FUCKING SHIT AM I RETARDED OR WHAT?
    // THANK GOD THE WINDOW CANT BE RESIZED OR ID HAVE TO ACTUALLY LEARN ABOUT SCALING AND TRANSFORMATIONS
    // TODO ULTRAWIDE RESOLUTIONS
    static {
        double ratio = (double)DISPLAY_HEIGHT / (double)DISPLAY_WIDTH;
        double epsilon = 1e-6;

        // float screenScaleMult = Global.getSettings().getScreenScaleMult();

        float CONFIRM_DIALOG_WIDTH_DIVISOR_;
        float CONFIRM_DIALOG_HEIGHT_DIVISOR_;
        
        float PANEL_WIDTH_SUBTRACTOR_;
        float PANEL_HEIGHT_SUBTRACTOR_;
        
        float NAME_COLUMN_WIDTH_DIVISOR_;
        float SHIP_COLUMN_WIDTH_DIVISOR_;
        // TABLE_HEADER_Y_OFFSET_PERCENT_BTM = 4f;
        // TABLE_HEADER_Y_OFFSET_PERCENT_TOP = 2.6f;

        float SHIPLIST_PANEL_HEIGHT_SUBTRACTOR_;
        float SHIPLIST_Y_OFFSET_MULTIPLIER_;
        float SHIPLIST_SCALE_;
        float SHIPLIST_PANEL_PADDING_DIVISOR_;

        ROW_HEIGHT = 30f;

        // 1920x1080 2560x1440 3840x2160 16:9
        if (Math.abs(ratio - 0.5625) < epsilon) {
            switch(DISPLAY_WIDTH) {
                case 1920:
                    CONFIRM_DIALOG_WIDTH_DIVISOR_ = 3.3f;
                    CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 2.15f;

                    PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.4f) + 10f;
                    PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.4f);

                    NAME_COLUMN_WIDTH_DIVISOR_ = 3.8f;
                    SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;
                    // TABLE_HEADER_Y_OFFSET_PERCENT_BTM = 4f;
                    // TABLE_HEADER_Y_OFFSET_PERCENT_TOP = 2.6f;

                    SHIPLIST_PANEL_HEIGHT_SUBTRACTOR_ = 10f;
                    SHIPLIST_Y_OFFSET_MULTIPLIER_ = 5f;
                    SHIPLIST_SCALE_ = 0.9f;
                    SHIPLIST_PANEL_PADDING_DIVISOR_ = 1.8f;
                    break;
                    
                case 2560:
                    CONFIRM_DIALOG_WIDTH_DIVISOR_ = 3.9f;
                    CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 2.9f;

                    PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.62f) + 10f;
                    PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.1f);

                    NAME_COLUMN_WIDTH_DIVISOR_ = 3.8f;
                    SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;
                    // TABLE_HEADER_Y_OFFSET_PERCENT_BTM = 4f;
                    // TABLE_HEADER_Y_OFFSET_PERCENT_TOP = 2.6f;

                    SHIPLIST_PANEL_HEIGHT_SUBTRACTOR_ = 20f;
                    SHIPLIST_Y_OFFSET_MULTIPLIER_ = 17f;
                    SHIPLIST_SCALE_ = 0.9f;
                    SHIPLIST_PANEL_PADDING_DIVISOR_ = 1.8f;
                    break;

                case 3840:
                    CONFIRM_DIALOG_WIDTH_DIVISOR_ = 5.0f;
                    CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 3.25f;

                    PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.4f) - 5f;
                    PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.5f);

                    NAME_COLUMN_WIDTH_DIVISOR_ = 3.8f;
                    SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;
                    // TABLE_HEADER_Y_OFFSET_PERCENT_BTM = 4f;
                    // TABLE_HEADER_Y_OFFSET_PERCENT_TOP = 2.6f;

                    SHIPLIST_PANEL_HEIGHT_SUBTRACTOR_ = 20f;
                    SHIPLIST_Y_OFFSET_MULTIPLIER_ = 40f;
                    SHIPLIST_SCALE_ = 1.2f;
                    SHIPLIST_PANEL_PADDING_DIVISOR_ = 4.4f;
                    break;

                // 1600x900
                default:
                    CONFIRM_DIALOG_WIDTH_DIVISOR_ = 3.3f;
                    CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 2f;
                    PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.4f) - 5f;
                    PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.5f);

                    NAME_COLUMN_WIDTH_DIVISOR_ = 3.8f;
                    SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;
                    // TABLE_HEADER_Y_OFFSET_PERCENT_BTM = 4f;
                    // TABLE_HEADER_Y_OFFSET_PERCENT_TOP = 2.6f;

                    SHIPLIST_PANEL_HEIGHT_SUBTRACTOR_ = 10f;
                    SHIPLIST_Y_OFFSET_MULTIPLIER_ = 10f;
                    SHIPLIST_SCALE_ = 0.7f;
                    SHIPLIST_PANEL_PADDING_DIVISOR_ = 1.8f;
                    break;
            }
        } else {
                // 1280x800 1440x900 1680x1050 1920x1200 2560x1600 (16:10)
                if (Math.abs(ratio - 0.625) < epsilon) {
                    switch(DISPLAY_WIDTH) {
                        case 1280:
                            CONFIRM_DIALOG_WIDTH_DIVISOR_ = 2.8f;
                            CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 2f;
                            
                            PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.7f) - 5f;
                            PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.5f);
                            
                            NAME_COLUMN_WIDTH_DIVISOR_ = 3.8f;
                            SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;
                            // TABLE_HEADER_Y_OFFSET_PERCENT_BTM = 4f;
                            // TABLE_HEADER_Y_OFFSET_PERCENT_TOP = 2.6f;

                            SHIPLIST_PANEL_HEIGHT_SUBTRACTOR_ = 10f;
                            SHIPLIST_Y_OFFSET_MULTIPLIER_ = 10f;
                            SHIPLIST_SCALE_ = 0.6f;
                            SHIPLIST_PANEL_PADDING_DIVISOR_ = 1.485f;
                            break;
                            
                        case 1440:
                            CONFIRM_DIALOG_WIDTH_DIVISOR_ = 2.8f;
                            CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 2f;
                            
                            PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.4f) - 5f;
                            PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.5f);
                            
                            NAME_COLUMN_WIDTH_DIVISOR_ = 3.8f;
                            SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;
                            // TABLE_HEADER_Y_OFFSET_PERCENT_BTM = 4f;
                            // TABLE_HEADER_Y_OFFSET_PERCENT_TOP = 2.6f;
                            
                            SHIPLIST_PANEL_HEIGHT_SUBTRACTOR_ = 5f;
                            SHIPLIST_Y_OFFSET_MULTIPLIER_ = 10f;
                            SHIPLIST_SCALE_ = 0.7f;
                            SHIPLIST_PANEL_PADDING_DIVISOR_ = 1.65f;
                            break;
                            
                        case 1680:
                            CONFIRM_DIALOG_WIDTH_DIVISOR_ = 2.8f;
                            CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 2f;
                            
                            PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.4f) - 5f;
                            PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.4f);
                            
                            NAME_COLUMN_WIDTH_DIVISOR_ = 3.8f;
                            SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;
                            // TABLE_HEADER_Y_OFFSET_PERCENT_BTM = 4f;
                            // TABLE_HEADER_Y_OFFSET_PERCENT_TOP = 2.6f;

                            SHIPLIST_PANEL_HEIGHT_SUBTRACTOR_ = 10f;
                            SHIPLIST_Y_OFFSET_MULTIPLIER_ = 10f;
                            SHIPLIST_SCALE_ = 0.9f;
                            SHIPLIST_PANEL_PADDING_DIVISOR_ = 2f;
                            break;

                        // ...GOOD ENOUGH
                        case 1920:
                            CONFIRM_DIALOG_WIDTH_DIVISOR_ = 2.8f;
                            CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 2f;
                            
                            PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.4f) - 5f;
                            PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.4f);
                            
                            NAME_COLUMN_WIDTH_DIVISOR_ = 3.8f;
                            SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;
                            // TABLE_HEADER_Y_OFFSET_PERCENT_BTM = 4f;
                            // TABLE_HEADER_Y_OFFSET_PERCENT_TOP = 2.6f;

                            SHIPLIST_PANEL_HEIGHT_SUBTRACTOR_ = 10f;
                            SHIPLIST_Y_OFFSET_MULTIPLIER_ = 10f;
                            SHIPLIST_SCALE_ = 0.9f;
                            SHIPLIST_PANEL_PADDING_DIVISOR_ = 2f;
                            break;

                        // 2560
                        default:
                            CONFIRM_DIALOG_WIDTH_DIVISOR_ = 3.9f;
                            CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 2.9f;
                            PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.62f) + 10f;
                            PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.1f);
        
                            NAME_COLUMN_WIDTH_DIVISOR_ = 3.8f;
                            SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;
                            // TABLE_HEADER_Y_OFFSET_PERCENT_BTM = 4f;
                            // TABLE_HEADER_Y_OFFSET_PERCENT_TOP = 2.6f;
        
                            SHIPLIST_PANEL_HEIGHT_SUBTRACTOR_ = 20f;
                            SHIPLIST_Y_OFFSET_MULTIPLIER_ = 17f;
                            SHIPLIST_SCALE_ = 0.9f;
                            SHIPLIST_PANEL_PADDING_DIVISOR_ = 1.8f;
                            break;
                    }

                // 1280x768
                } else if (Math.abs(ratio - 0.6) < epsilon) {
                    CONFIRM_DIALOG_WIDTH_DIVISOR_ = 2.8f;
                    CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 2f;
                    
                    PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.7f) - 5f;
                    PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 2f);
                    
                    NAME_COLUMN_WIDTH_DIVISOR_ = 3.8f;
                    SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;
                    // TABLE_HEADER_Y_OFFSET_PERCENT_BTM = 4f;
                    // TABLE_HEADER_Y_OFFSET_PERCENT_TOP = 2.6f;

                    SHIPLIST_PANEL_HEIGHT_SUBTRACTOR_ = 10f;
                    SHIPLIST_Y_OFFSET_MULTIPLIER_ = 10f;
                    SHIPLIST_SCALE_ = 0.6f;
                    SHIPLIST_PANEL_PADDING_DIVISOR_ = 1.485f;

                // 1600x1024
                } else if (Math.abs(ratio - 0.64) < epsilon) {
                    CONFIRM_DIALOG_WIDTH_DIVISOR_ = 2.8f;
                    CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 2f;

                    PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.4f) - 5f;
                    PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.4f);

                    NAME_COLUMN_WIDTH_DIVISOR_ = 3.8f;
                    SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;
                    // TABLE_HEADER_Y_OFFSET_PERCENT_BTM = 4f;
                    // TABLE_HEADER_Y_OFFSET_PERCENT_TOP = 2.6f;

                    SHIPLIST_PANEL_HEIGHT_SUBTRACTOR_ = 10f;
                    SHIPLIST_Y_OFFSET_MULTIPLIER_ = 10f;
                    SHIPLIST_SCALE_ = 0.9f;
                    SHIPLIST_PANEL_PADDING_DIVISOR_ = 2.2f;

                // 1280x960 1600x1200 1920x1400 2048x1536 
                } else if (Math.abs(ratio - 0.75) < epsilon) {
                    switch(DISPLAY_WIDTH) {
                        case 1280:
                            CONFIRM_DIALOG_WIDTH_DIVISOR_ = 2.8f;
                            CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 2.5f;
                            
                            PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.5f) - 5f;
                            PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.5f);
                            
                            NAME_COLUMN_WIDTH_DIVISOR_ = 3.8f;
                            SHIP_COLUMN_WIDTH_DIVISOR_ = 3.6f;
                            // TABLE_HEADER_Y_OFFSET_PERCENT_BTM = 4f;
                            // TABLE_HEADER_Y_OFFSET_PERCENT_TOP = 2.6f;

                            SHIPLIST_PANEL_HEIGHT_SUBTRACTOR_ = -10f;
                            SHIPLIST_Y_OFFSET_MULTIPLIER_ = 10f;
                            SHIPLIST_SCALE_ = 0.5f;
                            SHIPLIST_PANEL_PADDING_DIVISOR_ = -3.1f;
                            break;

                        case 1600:
                            CONFIRM_DIALOG_WIDTH_DIVISOR_ = 2.8f;
                            CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 2.5f;
        
                            PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.4f) - 5f;
                            PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.4f);
        
                            NAME_COLUMN_WIDTH_DIVISOR_ = 3.8f;
                            SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;
                            // TABLE_HEADER_Y_OFFSET_PERCENT_BTM = 4f;
                            // TABLE_HEADER_Y_OFFSET_PERCENT_TOP = 2.6f;
        
                            SHIPLIST_PANEL_HEIGHT_SUBTRACTOR_ = 25f;
                            SHIPLIST_Y_OFFSET_MULTIPLIER_ = 30f;
                            SHIPLIST_SCALE_ = 0.85f;
                            SHIPLIST_PANEL_PADDING_DIVISOR_ = 2.2f;
                            break;

                        // GOOD ENOUGH
                        case 1920:
                            CONFIRM_DIALOG_WIDTH_DIVISOR_ = 2.8f;
                            CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 2.5f;
        
                            PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.4f) - 5f;
                            PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.4f);
        
                            NAME_COLUMN_WIDTH_DIVISOR_ = 3.8f;
                            SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;
                            // TABLE_HEADER_Y_OFFSET_PERCENT_BTM = 4f;
                            // TABLE_HEADER_Y_OFFSET_PERCENT_TOP = 2.6f;
        
                            SHIPLIST_PANEL_HEIGHT_SUBTRACTOR_ = -10f;
                            SHIPLIST_Y_OFFSET_MULTIPLIER_ = -20f;
                            SHIPLIST_SCALE_ = 0.85f;
                            SHIPLIST_PANEL_PADDING_DIVISOR_ = 3.5f;
                            break;
                            
                        // 2048
                        default:
                            CONFIRM_DIALOG_WIDTH_DIVISOR_ = 2.8f;
                            CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 2.5f;
        
                            PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.4f) - 5f;
                            PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.4f);
        
                            NAME_COLUMN_WIDTH_DIVISOR_ = 3.8f;
                            SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;
                            // TABLE_HEADER_Y_OFFSET_PERCENT_BTM = 4f;
                            // TABLE_HEADER_Y_OFFSET_PERCENT_TOP = 2.6f;
        
                            SHIPLIST_PANEL_HEIGHT_SUBTRACTOR_ = 25f;
                            SHIPLIST_Y_OFFSET_MULTIPLIER_ = 70f;
                            SHIPLIST_SCALE_ = 1f;
                            SHIPLIST_PANEL_PADDING_DIVISOR_ = 2.2f;
                            break;
                    }

                // 1280x1024 
                } else if (Math.abs(ratio - 0.8) < epsilon) {
                    CONFIRM_DIALOG_WIDTH_DIVISOR_ = 2.2f;
                    CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 2.4f;
                    
                    PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.4f) - 5f;
                    PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.4f);
                    
                    NAME_COLUMN_WIDTH_DIVISOR_ = 3.8f;
                    SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;
                    // TABLE_HEADER_Y_OFFSET_PERCENT_BTM = 4f;
                    // TABLE_HEADER_Y_OFFSET_PERCENT_TOP = 2.6f;

                    SHIPLIST_PANEL_HEIGHT_SUBTRACTOR_ = 10f;
                    SHIPLIST_Y_OFFSET_MULTIPLIER_ = 10f;
                    SHIPLIST_SCALE_ = 0.7f;
                    SHIPLIST_PANEL_PADDING_DIVISOR_ = 1.65f;
                    
                // 2048x1080 4096x2160
                } else if (Math.abs(ratio - 0.52734375) < epsilon) {
                    switch(DISPLAY_WIDTH) {
                        case 2048:
                            CONFIRM_DIALOG_WIDTH_DIVISOR_ = 3.3f;
                            CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 2.15f;

                            PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.4f) + 10f;
                            PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.4f);

                            NAME_COLUMN_WIDTH_DIVISOR_ = 3.8f;
                            SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;
                            // TABLE_HEADER_Y_OFFSET_PERCENT_BTM = 4f;
                            // TABLE_HEADER_Y_OFFSET_PERCENT_TOP = 2.6f;

                            SHIPLIST_PANEL_HEIGHT_SUBTRACTOR_ = 10f;
                            SHIPLIST_Y_OFFSET_MULTIPLIER_ = 5f;
                            SHIPLIST_SCALE_ = 0.9f;
                            SHIPLIST_PANEL_PADDING_DIVISOR_ = 1.8f;
                            break;
                        // 4096
                        default:
                            CONFIRM_DIALOG_WIDTH_DIVISOR_ = 5.0f;
                            CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 3.25f;
        
                            PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.4f) - 5f;
                            PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.5f);
        
                            NAME_COLUMN_WIDTH_DIVISOR_ = 3.8f;
                            SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;
                            // TABLE_HEADER_Y_OFFSET_PERCENT_BTM = 4f;
                            // TABLE_HEADER_Y_OFFSET_PERCENT_TOP = 2.6f;
        
                            SHIPLIST_PANEL_HEIGHT_SUBTRACTOR_ = 20f;
                            SHIPLIST_Y_OFFSET_MULTIPLIER_ = 40f;
                            SHIPLIST_SCALE_ = 1.2f;
                            SHIPLIST_PANEL_PADDING_DIVISOR_ = 4.4f;
                            break;
                    }


                } else if (DISPLAY_WIDTH == 1366 || DISPLAY_WIDTH == 1360) {
                    CONFIRM_DIALOG_WIDTH_DIVISOR_ = 2.8f;
                    CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 2f;

                    PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.4f) - 5f;
                    PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 2.5f);

                    NAME_COLUMN_WIDTH_DIVISOR_ = 3.8f;
                    SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;
                    // TABLE_HEADER_Y_OFFSET_PERCENT_BTM = 4f;
                    // TABLE_HEADER_Y_OFFSET_PERCENT_TOP = 2.6f;

                    SHIPLIST_PANEL_HEIGHT_SUBTRACTOR_ = 10f;
                    SHIPLIST_Y_OFFSET_MULTIPLIER_ = -2f;
                    SHIPLIST_SCALE_ = 0.65f;
                    SHIPLIST_PANEL_PADDING_DIVISOR_ = 1.7f;

                } else {
                    // fallback using 1080p values
                    CONFIRM_DIALOG_WIDTH_DIVISOR_ = 2.8f;
                    CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 2f;

                    PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.4f) - 5f;
                    PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.4f);

                    NAME_COLUMN_WIDTH_DIVISOR_ = 3.8f;
                    SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;
                    // TABLE_HEADER_Y_OFFSET_PERCENT_BTM = 4f;
                    // TABLE_HEADER_Y_OFFSET_PERCENT_TOP = 2.6f;

                    SHIPLIST_PANEL_HEIGHT_SUBTRACTOR_ = 10f;
                    SHIPLIST_Y_OFFSET_MULTIPLIER_ = 10f;
                    SHIPLIST_SCALE_ = 0.9f;
                    SHIPLIST_PANEL_PADDING_DIVISOR_ = 1.8f;
                }
            }
    
        CONFIRM_DIALOG_WIDTH_DIVISOR = CONFIRM_DIALOG_WIDTH_DIVISOR_ / 1.3f;
        CONFIRM_DIALOG_HEIGHT_DIVISOR = CONFIRM_DIALOG_HEIGHT_DIVISOR_;

        PANEL_WIDTH_SUBTRACTOR = PANEL_WIDTH_SUBTRACTOR_;
        PANEL_HEIGHT_SUBTRACTOR = PANEL_HEIGHT_SUBTRACTOR_;

        NAME_COLUMN_WIDTH_DIVISOR = NAME_COLUMN_WIDTH_DIVISOR_;
        SHIP_COLUMN_WIDTH_DIVISOR = SHIP_COLUMN_WIDTH_DIVISOR_;
        // public static final float TABLE_HEADER_Y_OFFSET_PERCENT_BTM;
        // public static final float TABLE_HEADER_Y_OFFSET_PERCENT_TOP;
        SHIPLIST_PANEL_HEIGHT_SUBTRACTOR = SHIPLIST_PANEL_HEIGHT_SUBTRACTOR_;
        SHIPLIST_PANEL_PADDING_DIVISOR = SHIPLIST_PANEL_PADDING_DIVISOR_;
        SHIPLIST_SCALE = SHIPLIST_SCALE_;
        SHIPLIST_Y_OFFSET_MULTIPLIER = SHIPLIST_Y_OFFSET_MULTIPLIER_;

        SHIPLIST_SIZE = 60f * SHIPLIST_SCALE;
    }
}