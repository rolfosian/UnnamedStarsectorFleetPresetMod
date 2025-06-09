package data.scripts.ui;

import java.util.List;

import com.fs.starfarer.api.Global;

import data.scripts.util.PresetMiscUtils;

public class UiConfig {
    public static final int DISPLAY_WIDTH = (int)Global.getSettings().getScreenWidthPixels();
    public static final int DISPLAY_HEIGHT = (int)Global.getSettings().getScreenHeightPixels();

    public static final float CONFIRM_DIALOG_WIDTH_DIVISOR;
    public static final float CONFIRM_DIALOG_HEIGHT_DIVISOR;
    public static final float PANEL_WIDTH_SUBTRACTOR;
    public static final float PANEL_HEIGHT_SUBTRACTOR;
    public static final float NAME_COLUMN_WIDTH_DIVISOR;
    public static final float SHIP_COLUMN_WIDTH_DIVISOR;
    public static final float SHIPLIST_Y_OFFSET_MULTIPLIER;

    // These are teh values for 1080p
    // CONFIRM_DIALOG_WIDTH_DIVISOR_ = 2.64f;
    // CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 2.365f;

    // PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.4f) + 10f;
    // PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.4f);

    // NAME_COLUMN_WIDTH_DIVISOR_ = 5.32f;
    // SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;

    // SHIPLIST_PANEL_HEIGHT_SUBTRACTOR_ = 10f;
    // SHIPLIST_Y_OFFSET_MULTIPLIER_ = 5f;

    // THERE HAS TO BE A BETTER WAY TO DO THIS
    // TODO ULTRAWIDE RESOLUTIONS
    static {
        double ratio = (double)DISPLAY_HEIGHT / (double)DISPLAY_WIDTH;
        double epsilon = 1e-6;

        float CONFIRM_DIALOG_WIDTH_DIVISOR_;
        float CONFIRM_DIALOG_HEIGHT_DIVISOR_;
        
        float PANEL_WIDTH_SUBTRACTOR_;
        float PANEL_HEIGHT_SUBTRACTOR_;
        
        float NAME_COLUMN_WIDTH_DIVISOR_;
        float SHIP_COLUMN_WIDTH_DIVISOR_;

        float SHIPLIST_Y_OFFSET_MULTIPLIER_;

        // 1920x1080 2560x1440 3840x2160 (16:9)
        if (Math.abs(ratio - 0.5625) < epsilon) {
            switch(DISPLAY_WIDTH) {
                case 1920:
                    CONFIRM_DIALOG_WIDTH_DIVISOR_ = 2.64f;
                    CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 2.365f;

                    PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.4f) + 10f;
                    PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.4f);

                    NAME_COLUMN_WIDTH_DIVISOR_ = 5.32f;
                    SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;

                    SHIPLIST_Y_OFFSET_MULTIPLIER_ = 5f;
                    break;
                    
                case 2560:
                    CONFIRM_DIALOG_WIDTH_DIVISOR_ = 3.55f;
                    CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 3.0f;

                    PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.62f) + 10f;
                    PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.1f);

                    NAME_COLUMN_WIDTH_DIVISOR_ = 5.32f;
                    SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;

                    SHIPLIST_Y_OFFSET_MULTIPLIER_ = 10f;
                    break;

                case 3840:
                    CONFIRM_DIALOG_WIDTH_DIVISOR_ = 5.1f;
                    CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 4.5f;

                    PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.4f) - 5f;
                    PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.5f);

                    NAME_COLUMN_WIDTH_DIVISOR_ = 5.32f;
                    SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;

                    SHIPLIST_Y_OFFSET_MULTIPLIER_ = 20f;
                    break;

                // 1600x900
                default:
                    CONFIRM_DIALOG_WIDTH_DIVISOR_ = 2.15f;
                    CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 1.95f;
                    PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.4f) - 5f;
                    PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.5f);

                    NAME_COLUMN_WIDTH_DIVISOR_ = 5.32f;
                    SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;

                    SHIPLIST_Y_OFFSET_MULTIPLIER_ = 10f;
                    break;
            }
        } else {
                // 1280x800 1440x900 1680x1050 1920x1200 2560x1600 (16:10)
                if (Math.abs(ratio - 0.625) < epsilon) {
                    switch(DISPLAY_WIDTH) {
                        case 1280:
                            CONFIRM_DIALOG_WIDTH_DIVISOR_ = 1.75f;
                            CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 1.7f;
                            
                            PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.7f) - 5f;
                            PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.7f);
                            
                            NAME_COLUMN_WIDTH_DIVISOR_ = 5.32f;
                            SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;

                            SHIPLIST_Y_OFFSET_MULTIPLIER_ = 10f;
                            break;
                            
                        case 1440:
                            CONFIRM_DIALOG_WIDTH_DIVISOR_ = 1.95f;
                            CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 1.95f;
                            
                            PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.4f) - 5f;
                            PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.5f);
                            
                            NAME_COLUMN_WIDTH_DIVISOR_ = 5.32f;
                            SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;

                            SHIPLIST_Y_OFFSET_MULTIPLIER_ = 10f;
                            break;
                            
                        case 1680:
                            CONFIRM_DIALOG_WIDTH_DIVISOR_ = 2.25f;
                            CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 2.25f;
                            
                            PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.4f) - 5f;
                            PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.4f);
                            
                            NAME_COLUMN_WIDTH_DIVISOR_ = 5.32f;
                            SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;

                            SHIPLIST_Y_OFFSET_MULTIPLIER_ = 10f;
                            break;

                        case 1920:
                            CONFIRM_DIALOG_WIDTH_DIVISOR_ = 2.6f;
                            CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 2.6f;
                            
                            PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.4f) - 5f;
                            PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.4f);
                            
                            NAME_COLUMN_WIDTH_DIVISOR_ = 5.32f;
                            SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;

                            SHIPLIST_Y_OFFSET_MULTIPLIER_ = 10f;
                            break;

                        // 2560
                        default:
                            CONFIRM_DIALOG_WIDTH_DIVISOR_ = 3.5f;
                            CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 3.4f;

                            PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.62f) + 10f;
                            PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.1f);
        
                            NAME_COLUMN_WIDTH_DIVISOR_ = 5.32f;
                            SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;

                            SHIPLIST_Y_OFFSET_MULTIPLIER_ = 10f;
                            break;
                    }

                // 1280x768
                } else if (Math.abs(ratio - 0.6) < epsilon) {
                    CONFIRM_DIALOG_WIDTH_DIVISOR_ = 1.75f;
                    CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 1.65f;
                    
                    PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.7f) - 5f;
                    PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 2f);
                    
                    NAME_COLUMN_WIDTH_DIVISOR_ = 5.32f;
                    SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;

                    SHIPLIST_Y_OFFSET_MULTIPLIER_ = 10f;

                // 1600x1024
                } else if (Math.abs(ratio - 0.64) < epsilon) {
                    CONFIRM_DIALOG_WIDTH_DIVISOR_ = 2.15f;
                    CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 2.2f;
                    
                    PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.4f) - 5f;
                    PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.4f);
                    
                    NAME_COLUMN_WIDTH_DIVISOR_ = 5.32f;
                    SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;

                    SHIPLIST_Y_OFFSET_MULTIPLIER_ = 10f;

                // 1280x960 1600x1200 1920x1440 2048x1536
                } else if (Math.abs(ratio - 0.75) < epsilon) {
                    switch(DISPLAY_WIDTH) {
                        case 1280:
                            CONFIRM_DIALOG_WIDTH_DIVISOR_ = 1.75f;
                            CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 2.05f;
                            
                            PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.7f) - 5f;
                            PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 2f);
                            
                            NAME_COLUMN_WIDTH_DIVISOR_ = 5.32f;
                            SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;

                            SHIPLIST_Y_OFFSET_MULTIPLIER_ = 10f;
                            break;

                        case 1600:
                            CONFIRM_DIALOG_WIDTH_DIVISOR_ = 2.15f;
                            CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 2.55f;
                            
                            PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.4f) - 5f;
                            PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.4f);
                            
                            NAME_COLUMN_WIDTH_DIVISOR_ = 5.32f;
                            SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;

                            SHIPLIST_Y_OFFSET_MULTIPLIER_ = 10f;
                            break;

                        case 1920:
                            CONFIRM_DIALOG_WIDTH_DIVISOR_ = 2.6f;
                            CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 3f;
                            
                            PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.4f) - 5f;
                            PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.4f);
                            
                            NAME_COLUMN_WIDTH_DIVISOR_ = 5.32f;
                            SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;

                            SHIPLIST_Y_OFFSET_MULTIPLIER_ = 10f;
                            break;
                            
                        // 2048
                        default:
                            CONFIRM_DIALOG_WIDTH_DIVISOR_ = 2.7f;
                            CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 3.2f;
        
                            PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.4f) - 5f;
                            PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.4f);
        
                            NAME_COLUMN_WIDTH_DIVISOR_ = 5.32f;
                            SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;

                            SHIPLIST_Y_OFFSET_MULTIPLIER_ = 10f;
                            break;
                    }

                // 1280x1024 
                } else if (Math.abs(ratio - 0.8) < epsilon) {
                    CONFIRM_DIALOG_WIDTH_DIVISOR_ = 1.75f;
                    CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 2.05f;
                    
                    PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.7f) - 5f;
                    PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 2f);
                    
                    NAME_COLUMN_WIDTH_DIVISOR_ = 5.32f;
                    SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;

                    SHIPLIST_Y_OFFSET_MULTIPLIER_ = 10f;
                    
                // 2048x1080 4096x2160
                } else if (Math.abs(ratio - 0.52734375) < epsilon) {
                    switch(DISPLAY_WIDTH) {
                        case 2048:
                            CONFIRM_DIALOG_WIDTH_DIVISOR_ = 2.75f;
                            CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 2.3f;

                            PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.4f) + 10f;
                            PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.4f);

                            NAME_COLUMN_WIDTH_DIVISOR_ = 5.32f;
                            SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;

                            SHIPLIST_Y_OFFSET_MULTIPLIER_ = 5f;
                            break;

                        // 4096
                        default:
                            CONFIRM_DIALOG_WIDTH_DIVISOR_ = 5.5f;
                            CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 4.5f;
        
                            PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.4f) - 5f;
                            PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.5f);
        
                            NAME_COLUMN_WIDTH_DIVISOR_ = 5.32f;
                            SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;
        
                            SHIPLIST_Y_OFFSET_MULTIPLIER_ = 20f;
                            break;
                    }


                } else if (DISPLAY_WIDTH == 1366 || DISPLAY_WIDTH == 1360) {
                    CONFIRM_DIALOG_WIDTH_DIVISOR_ = 1.85f;
                    CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 1.6f;
                    
                    PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.7f) - 5f;
                    PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 2.1f);
                    
                    NAME_COLUMN_WIDTH_DIVISOR_ = 5.32f;
                    SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;

                    SHIPLIST_Y_OFFSET_MULTIPLIER_ = 10f;

                } else {
                    // fallback using 1080p values
                    CONFIRM_DIALOG_WIDTH_DIVISOR_ = 2.64f;
                    CONFIRM_DIALOG_HEIGHT_DIVISOR_ = 2.365f;

                    PANEL_WIDTH_SUBTRACTOR_ = (DISPLAY_WIDTH / 100 * 0.4f) + 10f;
                    PANEL_HEIGHT_SUBTRACTOR_ = (DISPLAY_HEIGHT / 100 * 1.4f);

                    NAME_COLUMN_WIDTH_DIVISOR_ = 5.32f;
                    SHIP_COLUMN_WIDTH_DIVISOR_ = 1.8f;

                    SHIPLIST_Y_OFFSET_MULTIPLIER_ = 5f;
                }
            }

        CONFIRM_DIALOG_WIDTH_DIVISOR = CONFIRM_DIALOG_WIDTH_DIVISOR_;
        CONFIRM_DIALOG_HEIGHT_DIVISOR = CONFIRM_DIALOG_HEIGHT_DIVISOR_;

        PANEL_WIDTH_SUBTRACTOR = PANEL_WIDTH_SUBTRACTOR_;
        PANEL_HEIGHT_SUBTRACTOR = PANEL_HEIGHT_SUBTRACTOR_;

        NAME_COLUMN_WIDTH_DIVISOR = NAME_COLUMN_WIDTH_DIVISOR_;
        SHIP_COLUMN_WIDTH_DIVISOR = SHIP_COLUMN_WIDTH_DIVISOR_ ;

        SHIPLIST_Y_OFFSET_MULTIPLIER = SHIPLIST_Y_OFFSET_MULTIPLIER_;
    }
}