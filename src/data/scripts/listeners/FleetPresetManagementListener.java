// THIS UI CODE IS A RAGING DUMPSTER FIRE SPAGHETTI ABOMINATION READ AT YOUR OWN RISK
package data.scripts.listeners;

import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;

import com.fs.starfarer.api.impl.campaign.ids.Submarkets;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.ScrollPanelAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TextFieldAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.input.InputEventType;

import com.fs.starfarer.api.util.Misc;

// import data.scripts.listeners.SaveFleetPreset;
import data.scripts.listeners.DialogDismissedListener;
import data.scripts.listeners.DummyDialogListener;
import data.scripts.ui.BaseSelfRefreshingPanel;
import data.scripts.ui.UIComponent;
import data.scripts.ui.UIPanel;
import data.scripts.listeners.DockingListener;

import data.scripts.util.RandomStringList;
import data.scripts.util.ReflectionUtilis;
import data.scripts.util.UtilReflection;
import data.scripts.util.PresetUtils;
import data.scripts.util.MiscUtils;
import data.scripts.FleetPresetManagerCoreScript;

import java.awt.Color;


import java.lang.reflect.Method;
import java.util.*;

import javax.swing.text.TableView.TableRow;

import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class FleetPresetManagementListener extends ActionListener {
    public static final Logger logger = Logger.getLogger(FleetPresetManagementListener.class);
    private static void print(Object... args) {
        MiscUtils.print(args);
    }

    private static final int DISPLAY_WIDTH = (int)Global.getSettings().getScreenWidthPixels();
    private static final int DISPLAY_HEIGHT = (int)Global.getSettings().getScreenHeightPixels();

    private static final float CONFIRM_DIALOG_WIDTH_DIVISOR;
    private static final float CONFIRM_DIALOG_HEIGHT_DIVISOR;
    private static final float PANEL_WIDTH_SUBTRACTOR;
    private static final float PANEL_HEIGHT_SUBTRACTOR;
    private static final float NAME_COLUMN_WIDTH_DIVISOR;
    private static final float SHIP_COLUMN_WIDTH_DIVISOR;
    // private static final float TABLE_HEADER_Y_OFFSET_PERCENT_BTM;
    // private static final float TABLE_HEADER_Y_OFFSET_PERCENT_TOP;
    private static final float SHIPLIST_PANEL_HEIGHT_SUBTRACTOR;
    private static final float SHIPLIST_PANEL_PADDING_DIVISOR;
    private static final float SHIPLIST_SCALE;
    private static final float SHIPLIST_SIZE;
    private static final float SHIPLIST_Y_OFFSET_MULTIPLIER;

    private static final float ROW_HEIGHT;

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

        float screenScaleMult = Global.getSettings().getScreenScaleMult();

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
    
    CONFIRM_DIALOG_WIDTH_DIVISOR = CONFIRM_DIALOG_WIDTH_DIVISOR_;
    CONFIRM_DIALOG_HEIGHT_DIVISOR = CONFIRM_DIALOG_HEIGHT_DIVISOR_;

    PANEL_WIDTH_SUBTRACTOR = PANEL_WIDTH_SUBTRACTOR_;
    PANEL_HEIGHT_SUBTRACTOR = PANEL_HEIGHT_SUBTRACTOR_;

    NAME_COLUMN_WIDTH_DIVISOR = NAME_COLUMN_WIDTH_DIVISOR_;
    SHIP_COLUMN_WIDTH_DIVISOR = SHIP_COLUMN_WIDTH_DIVISOR_;
    // private static final float TABLE_HEADER_Y_OFFSET_PERCENT_BTM;
    // private static final float TABLE_HEADER_Y_OFFSET_PERCENT_TOP;
    SHIPLIST_PANEL_HEIGHT_SUBTRACTOR = SHIPLIST_PANEL_HEIGHT_SUBTRACTOR_;
    SHIPLIST_PANEL_PADDING_DIVISOR = SHIPLIST_PANEL_PADDING_DIVISOR_;
    SHIPLIST_SCALE = SHIPLIST_SCALE_;
    SHIPLIST_Y_OFFSET_MULTIPLIER = SHIPLIST_Y_OFFSET_MULTIPLIER_;

    SHIPLIST_SIZE = 60f * SHIPLIST_SCALE;
    }

    private static final float CONFIRM_DIALOG_WIDTH = DISPLAY_WIDTH / CONFIRM_DIALOG_WIDTH_DIVISOR;
    private static final float CONFIRM_DIALOG_HEIGHT = DISPLAY_HEIGHT / CONFIRM_DIALOG_HEIGHT_DIVISOR;

    private static final float PANEL_WIDTH = DISPLAY_WIDTH / CONFIRM_DIALOG_WIDTH_DIVISOR - PANEL_WIDTH_SUBTRACTOR;
    private static final float PANEL_HEIGHT = DISPLAY_HEIGHT / CONFIRM_DIALOG_HEIGHT_DIVISOR - PANEL_HEIGHT_SUBTRACTOR;
    
    private static final float NAME_COLUMN_WIDTH = PANEL_WIDTH / NAME_COLUMN_WIDTH_DIVISOR;
    private static final float SHIP_COLUMN_WIDTH = PANEL_WIDTH / SHIP_COLUMN_WIDTH_DIVISOR;

    private static final float FLOAT_ZERO = 0f;
    
    private static float CANCEL_CONFIRM_BUTTON_WIDTH;

    private static final String EMPTY_STRING = "";
    private static final String QUESTON_MARK = "?";
    private static final String CLOSE_TEXT = "Close";
    private static final String CONFIRM_TEXT = "Confirm";
    private static final String CANCEL_TEXT = "Cancel";

    private static final String SAVE_DIALOG_HEADER = "Enter Preset Name:";
    private static final String SAVE_DIALOG_YES_TEXT = "Save preset";
    private static final String DELETE_DIALOG_HEADER_PREFIX = "Are you sure you want to delete ";
    private static final String OVERWRITE_DIALOG_HEADE_PREFIX = "Are you sure you want to overwrite ";
    
    private static final String SAVE_DIALOG_BUTTON_ID = "saveDialogButton";
    private static final String SAVE_DIALOG_BUTTON_TOOLTIP_PARA_TEXT = "Saves the current fleet as preset.";
    private static final String SAVE_DIALOG_BUTTON_TEXT = "SAVE FLEET";

    private static final String RESTORE_BUTTON_ID = "restoreButton"; 
    private static final String RESTORE_BUTTON_TOOLTIP_PARA_TEXT = "Restores the selected preset.";
    private static final String RESTORE_BUTTON_TEXT  = "RESTORE";

    private static final String STORE_BUTTON_ID = "storeButton";
    private static final String STORE_BUTTON_TOOLTIP_PARA_TEXT = "Store current fleet in storage.";
    private static final String STORE_BUTTON_TEXT  = "STORE FLEET";

    private static final String DELETE_BUTTON_ID = "deleteButton";
    private static final String DELETE_BUTTON_TOOLTIP_PARA_TEXT = "Deletes the selected preset.";
    private static final String DELETE_BUTTON_TEXT = "DELETE";

    private static final String OVERWRITE_PRESET_BUTTON_ID = "overwriteToPresetButton";
    private static final String OVERWRITE_PRESET_BUTTON_TOOLTIP_PARA_TEXT = "Overwrites the current fleet to the selected preset.";
    private static final String OVERWRITE_PRESET_BUTTON_TEXT = "OVERWRITE";
    
    private static final String BLANK_TABLE_TEXT = "Presets Go Here";
    private static final Color c1 = Global.getSettings().getBasePlayerColor();
    private static final Color c2 = Global.getSettings().getDarkPlayerColor();
    private static final Color TEXT_HIGHLIGHT_COLOR = Misc.getHighlightColor();

    private TextFieldAPI saveNameField;

    private String selectedPresetName = EMPTY_STRING;
    private LabelAPI selectedPresetNamePara;
    private String selectedPresetNameParaFormat = "Selected Preset: %s";
    private int selectedRowIndex = -1;
    private int currentPresetsNum = 0;

    private Map<String, ButtonAPI> theButtons = new HashMap<>();
    private ButtonAPI MasterCancelButton;
    private final HashMap<String, String> buttonToolTipParas = new HashMap<>();
    private FenaglePanele fenaglePanele;

    private List<TableRowListener> tableRowListeners = new ArrayList<>();
    private TablePlugin tablePlugin;
    private PositionAPI tableCanvasPos;
    private LinkedHashMap<String, PresetUtils.FleetPreset> currentTableMap;
    private boolean tableUp = true;
    private boolean tableRight = false;
    private String tablePresetNamesColumnHeader = "Presets <Ascending>";
    // private String tableShipsColumnHeader = "Ships <Descending>";

    public FleetPresetManagementListener() {
        super();

        this.selectedPresetName = "";
        this.tablePlugin = new TablePlugin();

        buttonToolTipParas.put(SAVE_DIALOG_BUTTON_ID, SAVE_DIALOG_BUTTON_TOOLTIP_PARA_TEXT);
        buttonToolTipParas.put(RESTORE_BUTTON_ID, RESTORE_BUTTON_TOOLTIP_PARA_TEXT);
        buttonToolTipParas.put(STORE_BUTTON_ID, STORE_BUTTON_TOOLTIP_PARA_TEXT);
        buttonToolTipParas.put(DELETE_BUTTON_ID, DELETE_BUTTON_TOOLTIP_PARA_TEXT);
        buttonToolTipParas.put(OVERWRITE_PRESET_BUTTON_ID, OVERWRITE_PRESET_BUTTON_TOOLTIP_PARA_TEXT);
    }

    @Override
    public void trigger(Object... args) {

        List<String> buttonIds = new ArrayList<>();
        buttonIds.add(SAVE_DIALOG_BUTTON_ID);
        buttonIds.add(RESTORE_BUTTON_ID);

        CustomPanelAPI tableMasterPanel = Global.getSettings().createCustom(PANEL_WIDTH - CANCEL_CONFIRM_BUTTON_WIDTH - 5f, PANEL_HEIGHT, new BaseCustomUIPanelPlugin() );
        DialogDismissedListener dummyListener = new DummyDialogListener();
        UtilReflection.ConfirmDialogData master = UtilReflection.showConfirmationDialog(
            EMPTY_STRING,
            EMPTY_STRING,
            CLOSE_TEXT,
            CONFIRM_DIALOG_WIDTH,
            CONFIRM_DIALOG_HEIGHT,
            dummyListener);
        if (master == null) {
            return;
        }

        ButtonAPI confirmButton = master.confirmButton.getInstance();
        PositionAPI confirmButtonPosition = confirmButton.getPosition();
        ButtonAPI cancelButton = master.cancelButton.getInstance();
        PositionAPI cancelButtonPosition = cancelButton.getPosition();
        CANCEL_CONFIRM_BUTTON_WIDTH = cancelButtonPosition.getWidth();

        ButtonPlugin buttonPlugin = new ButtonPlugin(buttonIds);
        CustomPanelAPI buttonsPanel = Global.getSettings().createCustom(CANCEL_CONFIRM_BUTTON_WIDTH, PANEL_HEIGHT, buttonPlugin);
        TooltipMakerAPI tooltipMaker = buttonsPanel.createUIElement(CANCEL_CONFIRM_BUTTON_WIDTH, PANEL_HEIGHT, true);
        buttonPlugin.init(buttonsPanel, tooltipMaker);

        addTheButtons(tooltipMaker, confirmButtonPosition, cancelButtonPosition);

        String storageAvailableText;
        Color storageAvailableColor;
        if (DockingListener.getPlayerCurrentMarket() != null && DockingListener.canPlayerAccessStorage(DockingListener.getPlayerCurrentMarket())) {
            storageAvailableText = "Storage Available";
            storageAvailableColor = Misc.getPositiveHighlightColor();
        } else {
            storageAvailableText = "Storage Unavailable";
            storageAvailableColor = Misc.getNegativeHighlightColor();
        }
        tooltipMaker.addPara(storageAvailableText, storageAvailableColor, 5f);
        selectedPresetNamePara = tooltipMaker.addParaWithMarkup(String.format(selectedPresetNameParaFormat, selectedPresetName), c1, 5f);

        master.panel.removeComponent(confirmButton);
        // data.panel.removeComponent(cancelButton);
        this.MasterCancelButton = cancelButton;
        tablePlugin = new TablePlugin();
        CustomPanelAPI canvasPanel = Global.getSettings().createCustom(PANEL_WIDTH - CANCEL_CONFIRM_BUTTON_WIDTH - SHIP_COLUMN_WIDTH - 10f, PANEL_HEIGHT, tablePlugin);
        canvasPanel.addComponent(tableMasterPanel).inTL(FLOAT_ZERO, FLOAT_ZERO);
        fenaglePanele = new FenaglePanele(master.panel, canvasPanel);

        buttonsPanel.addUIElement(tooltipMaker);
        master.panel.addComponent(buttonsPanel).inTL(FLOAT_ZERO, FLOAT_ZERO);
        master.panel.addComponent(canvasPanel).rightOfTop(buttonsPanel, 10f);

        tablePlugin.setRoot(tableMasterPanel);
    }

    private void addTheButtons(TooltipMakerAPI tooltipMaker, PositionAPI confirmPosition, PositionAPI cancelPosition) {
        float buttonWidth = confirmPosition.getWidth();
        float buttonHeight = cancelPosition.getHeight();
        
        ButtonAPI saveDialogButton = tooltipMaker.addButton(SAVE_DIALOG_BUTTON_TEXT, SAVE_DIALOG_BUTTON_ID, c1, c2,
        Alignment.BR, CutStyle.ALL, buttonWidth, buttonHeight, 5f);
        saveDialogButton.setShortcut(Keyboard.KEY_1, false);

        ButtonAPI restorePresetButton = tooltipMaker.addButton(RESTORE_BUTTON_TEXT, RESTORE_BUTTON_ID, c1, c2,
        Alignment.BR, CutStyle.ALL, buttonWidth, buttonHeight, 5f);
        restorePresetButton.setShortcut(Keyboard.KEY_2, false);

        ButtonAPI storeAllButton = tooltipMaker.addButton(STORE_BUTTON_TEXT, STORE_BUTTON_ID, c1, c2,
        Alignment.BR, CutStyle.ALL, buttonWidth, buttonHeight, 5f);
        storeAllButton.setShortcut(Keyboard.KEY_3, false);

        ButtonAPI deleteButton = tooltipMaker.addButton(DELETE_BUTTON_TEXT, DELETE_BUTTON_ID, c1, c2,
        Alignment.BR, CutStyle.ALL, buttonWidth, buttonHeight, 5f);
        deleteButton.setShortcut(Keyboard.KEY_4, false);

        ButtonAPI overwriteToPresetButton = tooltipMaker.addButton(OVERWRITE_PRESET_BUTTON_TEXT, OVERWRITE_PRESET_BUTTON_ID, c1, c2,
        Alignment.BR, CutStyle.ALL, buttonWidth, buttonHeight, 5f);
        overwriteToPresetButton.setShortcut(Keyboard.KEY_5, false);

        theButtons.put(SAVE_DIALOG_BUTTON_ID, saveDialogButton);
        theButtons.put(RESTORE_BUTTON_ID, restorePresetButton);
        theButtons.put(STORE_BUTTON_ID, storeAllButton);
        theButtons.put(DELETE_BUTTON_ID, deleteButton);
        theButtons.put(OVERWRITE_PRESET_BUTTON_ID, overwriteToPresetButton);
        disableButtonsRequiringSelection();
        enableButtonsRequiringSelection();

        return;
    }

    private UtilReflection.ConfirmDialogData openOverwriteDialog(boolean overwrite) {

        SaveListener saveListener = new SaveListener(true, overwrite);
        CustomPanelAPI textFieldPanel = Global.getSettings().createCustom(CONFIRM_DIALOG_WIDTH / 2 / 6, CONFIRM_DIALOG_HEIGHT / 2 / 18, null);

        UtilReflection.ConfirmDialogData subData = UtilReflection.showConfirmationDialog(
            OVERWRITE_DIALOG_HEADE_PREFIX + selectedPresetName + QUESTON_MARK,
            CONFIRM_TEXT,
            CANCEL_TEXT,
            CONFIRM_DIALOG_WIDTH / 1.5f,
            CONFIRM_DIALOG_HEIGHT / 4,
            saveListener);

        // PositionAPI subPos = subData.panel.getPosition();
        subData.panel.addComponent(textFieldPanel).inTL(0f, 0f);

        return subData;
    }

    private UtilReflection.ConfirmDialogData openSaveDialog() {

        SaveListener saveListener = new SaveListener(false, true);
        BaseCustomUIPanelPlugin textPanelPlugin = new BaseCustomUIPanelPlugin() {
            @Override 
            public void processInput(List<InputEventAPI> events) {
                for (InputEventAPI event : events) {
                    if (event.isKeyDownEvent() && (Keyboard.isKeyDown(Keyboard.KEY_RETURN) || Keyboard.isKeyDown(Keyboard.KEY_NUMPADENTER))) {
                        MiscUtils.pressKey(Keyboard.KEY_RETURN);
                    }
                }
            }
        };

        CustomPanelAPI textFieldPanel = Global.getSettings().createCustom(CONFIRM_DIALOG_WIDTH / 2 / 6, CONFIRM_DIALOG_HEIGHT / 2 / 12, textPanelPlugin);
        TooltipMakerAPI textFieldTooltipMaker = textFieldPanel.createUIElement(CONFIRM_DIALOG_WIDTH / 2 / 5, CONFIRM_DIALOG_HEIGHT / 2 / 10, false);
        saveNameField = textFieldTooltipMaker.addTextField(CONFIRM_DIALOG_WIDTH/3, CONFIRM_DIALOG_HEIGHT/2/3, "graphics/fonts/orbitron24aabold.fnt", 10f);
        textFieldPanel.addUIElement(textFieldTooltipMaker).inTL(0f, 0f);

        UtilReflection.ConfirmDialogData subData = UtilReflection.showConfirmationDialog(
            SAVE_DIALOG_HEADER,
            SAVE_DIALOG_YES_TEXT,
            CANCEL_TEXT,
            CONFIRM_DIALOG_WIDTH / 1.5f,
            CONFIRM_DIALOG_HEIGHT / 2,
            saveListener);

        // PositionAPI subPos = subData.panel.getPosition();
        subData.panel.addComponent(textFieldPanel).inTL(0f, 0f).setXAlignOffset(CONFIRM_DIALOG_WIDTH / 2 / 2 / 2).setYAlignOffset(-CONFIRM_DIALOG_HEIGHT / 2 / 2 / 2);
        saveNameField.grabFocus();

        return subData;
    }

    private UtilReflection.ConfirmDialogData openDeleteDialog() {

        DeleteListener deleteListener = new DeleteListener();
        CustomPanelAPI textPanel = Global.getSettings().createCustom(CONFIRM_DIALOG_WIDTH / 2 / 10, CONFIRM_DIALOG_HEIGHT / 2 / 20, null);

        UtilReflection.ConfirmDialogData subData = UtilReflection.showConfirmationDialog(
            DELETE_DIALOG_HEADER_PREFIX + selectedPresetName + QUESTON_MARK,
            CONFIRM_TEXT,
            CANCEL_TEXT,
            CONFIRM_DIALOG_WIDTH / 1.5f,
            CONFIRM_DIALOG_HEIGHT / 4,
            deleteListener);

        subData.panel.addComponent(textPanel).inTL(0f, 0f);
        return subData;
    }

    public static boolean isEmptyOrWhitespace(String s) {
        return s != null && s.trim().isEmpty();
    }

    private class ButtonPlugin implements CustomUIPanelPlugin {
        CustomPanelAPI masterPanel;
        TooltipMakerAPI masterTooltip;

        HashMap<String, CustomPanelAPI> tooltipMap;
        List<String> buttonIds;

        boolean isTooltip;
        String currentTooltipId;

        public ButtonPlugin(List<String> buttonIds) {
            this.tooltipMap = new HashMap<>();
            this.isTooltip = false;
            this.buttonIds = buttonIds;

            // for (String buttonId : buttonIds) {
            //     CustomPanelAPI tooltipPanel = Global.getSettings().createCustom(250f, 60f, null);
            //     TooltipMakerAPI tooltip = tooltipPanel.createUIElement(250f, 60f, false);
            //     tooltip.addPara(buttonToolTipParas.get(buttonId), 0f);
            //     tooltipPanel.wrapTooltipWithBox(tooltip, Misc.getBasePlayerColor());
            
            //     tooltipPanel.addUIElement(tooltip).inTL(0f, 0f);
            //     tooltipMap.put(buttonId, tooltipPanel);
            // }


        }
        
        public void init(CustomPanelAPI panel, TooltipMakerAPI tooltip) {
            this.masterPanel = panel;
            this.masterTooltip = tooltip;
        }

        // private void showButtonToolTipAtLocation(String buttonId) {
        //     CustomPanelAPI toolTipPanel = tooltipMap.get(buttonId);
        //     toolTipPanel.setOpacity(100f);
        //     float width = 0f;
        //     float height = 0f;

        //     for (Map.Entry<String, ButtonAPI> entry: theButtons.entrySet()) {
        //         ButtonAPI button = entry.getValue();
        //         if (button.getCustomData().equals(buttonId)) {
        //             PositionAPI pos = button.getPosition();
        //             width = pos.getWidth();
        //             height = pos.getHeight();
        //             break;
        //         }
        //     }

        //     this.masterTooltip.addComponent(toolTipPanel).inTL(width + 5f, height - 10f);
        // }

        // private void destroyButtonToolTip(String buttonId) {
        //     CustomPanelAPI toolTipPanel = tooltipMap.get(buttonId);
        //     toolTipPanel.setOpacity(0f);
        //     this.masterTooltip.removeComponent(toolTipPanel);
        // }

        @Override
        public void advance(float amount) {
    
        }
    
        @Override
        public void buttonPressed(Object arg0) {
            switch ((String) arg0) {
                case SAVE_DIALOG_BUTTON_ID:
                    openSaveDialog();
                    return;
                case RESTORE_BUTTON_ID:
                    PresetUtils.restoreFleetFromPreset(selectedPresetName);
                    return;
                case STORE_BUTTON_ID:
                    PresetUtils.storeFleetInStorage(selectedPresetName);
                    return;
                case DELETE_BUTTON_ID:
                    openDeleteDialog();
                    return;
                case OVERWRITE_PRESET_BUTTON_ID:
                    openOverwriteDialog(false);
                    return;
                default:
                    break;
            }
        }
    
        @Override
        public void positionChanged(PositionAPI arg0) {
    
        }

        @Override
        public void processInput(List<InputEventAPI> arg0) {
            for (InputEventAPI event : arg0) {
            //     if (event.isMouseMoveEvent()) {
            //         int mouseX = event.getX();
            //         int mouseY = event.getY();

            //         ButtonAPI button = getButton(theButtons, mouseX, mouseY);
            //         if (button != null) {
            //             String buttonId = (String) button.getCustomData();
            //             this.isTooltip = true;
            //             this.currentTooltipId = new String(buttonId);
            //             showButtonToolTipAtLocation(buttonId);
            //             return;
            //         }
            //         if (this.isTooltip && this.currentTooltipId != null) {
            //             destroyButtonToolTip(this.currentTooltipId);
            //             this.isTooltip = false;
            //             this.currentTooltipId = null;
            //             return;
            //             // i dont know why we have to do this, probably because concurrency ticks take too long
            //         } else {
            //             for (String buttonId : this.buttonIds) {
            //                 tooltipMap.get(buttonId).setOpacity(0f);
            //             }
            //         }
            //     }
                
                if (event.isKeyDownEvent()) {
                    if (Keyboard.isKeyDown(Keyboard.KEY_RETURN) || Keyboard.isKeyDown(Keyboard.KEY_NUMPADENTER)) {
                        event.consume();
                        continue;
                    }
                    if (!tableRowListeners.isEmpty()) {
                        if (selectedPresetName != EMPTY_STRING) {
                            int rowNum = tableRowListeners.size();

                            if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
                                selectedRowIndex += tableUp ? 1 : -1;
                            
                                if (selectedRowIndex < 0) {
                                    selectedRowIndex = 0;
                                } else if (selectedRowIndex >= rowNum) {
                                    selectedRowIndex = rowNum - 1;
                                } else {
                                    selectedPresetName = tableRowListeners.get(selectedRowIndex).rowName;
                                    tablePlugin.rebuild();
                                }
                            
                            } else if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
                                selectedRowIndex += tableUp ? -1 : 1;
                            
                                if (selectedRowIndex < 0) {
                                    selectedRowIndex = 0;
                                } else if (selectedRowIndex >= rowNum) {
                                    selectedRowIndex = rowNum - 1;
                                } else {
                                    selectedPresetName = tableRowListeners.get(selectedRowIndex).rowName;
                                    tablePlugin.rebuild();
                                }

                            } else if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE) && rowNum > 0 && selectedPresetName != EMPTY_STRING) {
                                disableButtonsRequiringSelection();
                                selectedRowIndex = -1;
                                selectedPresetName = EMPTY_STRING;

                                tablePlugin.rebuild();
                                event.consume();
                            }
                        }
                    }
                }
            }
        }

        private ButtonAPI getButton (HashMap<String, ButtonAPI> buttons, int mouseX, int mouseY) {
            for (Map.Entry<String, ButtonAPI> entry: theButtons.entrySet()) {
                ButtonAPI button = entry.getValue();
                PositionAPI pos = button.getPosition();
                float x = pos.getX();
                float y = pos.getY();
                float width = pos.getWidth();
                float height = pos.getHeight();

                if (mouseX >= x && mouseX <= x + width - 5f &&
                mouseY >= y && mouseY <= y + height - 5f) {
                return button;
                }
            }
            return null;
        }   
    
        @Override
        public void render(float arg0) {
    
        }
    
        @Override
        public void renderBelow(float arg0) {
    
        }
        
    }

    private void enableButtonsRequiringSelection() {
        if (selectedPresetName != EMPTY_STRING) {
            if (DockingListener.getPlayerCurrentMarket() != null && DockingListener.canPlayerAccessStorage(DockingListener.getPlayerCurrentMarket())) {
                theButtons.get(RESTORE_BUTTON_ID).setEnabled(true);
                theButtons.get(STORE_BUTTON_ID).setEnabled(true);
                theButtons.get(OVERWRITE_PRESET_BUTTON_ID).setEnabled(true);
            } else {
                theButtons.get(OVERWRITE_PRESET_BUTTON_ID).setEnabled(true);
            }
            theButtons.get(DELETE_BUTTON_ID).setEnabled(true);
        }
    }

    private void disableButtonsRequiringSelection() {
        theButtons.get(DELETE_BUTTON_ID).setEnabled(false);
        theButtons.get(RESTORE_BUTTON_ID).setEnabled(false);
        theButtons.get(STORE_BUTTON_ID).setEnabled(false);
        theButtons.get(OVERWRITE_PRESET_BUTTON_ID).setEnabled(false);
    }

    public class TablePlugin extends BaseSelfRefreshingPanel {
        public LabelAPI label;
        public boolean rebuild;
        // public UIPanelAPI root;
        public CustomPanelAPI panel;
        private UIPanelAPI tablePanel;
        private TooltipMakerAPI tableTipMaker;
        private CustomPanelAPI shipListPanel;
        public float yScrollOffset;

        public TablePlugin() {
        }

        @Override
        public void rebuild() {
            if (tableTipMaker != null) {
                yScrollOffset = tableTipMaker.getExternalScroller().getYOffset();
            }
            super.rebuild();
        }

        // public void setRoot(CustomPanelAPI root, CustomPanelAPI panel) {
        //     this.root = root;
        //     this.panel = panel;
        //     super.rebuild();
        // }

        @Override
        public void positionChanged(PositionAPI position) {
        }
    
        @Override
        public void renderBelow(float alphaMult) {
    
        }
    
        @Override
        public void render(float alphaMult) {
        }

        private void processRow(Object row, String rowName, TooltipMakerAPI tableTipMaker, int id, PresetUtils.FleetPreset fleetpreset) {
            PositionAPI rowPos = (PositionAPI) ReflectionUtilis.invokeMethod("getPosition", row);
            
            TableRowListener rowListener = new TableRowListener(row, rowPos, rowName, tableRowListeners, id, fleetpreset.fleetMembers);
            CustomPanelAPI rowOverlayPanel = Global.getSettings().createCustom(NAME_COLUMN_WIDTH, 29f, rowListener);
            TooltipMakerAPI rowOverlayTooltipMaker = rowOverlayPanel.createUIElement(NAME_COLUMN_WIDTH, 29f, false);

            tableTipMaker.addComponent(rowOverlayPanel).inTL(rowPos.getX(), rowPos.getY());
            rowListener.init(rowOverlayPanel, rowOverlayTooltipMaker, rowPos);
            
            tableRowListeners.add(rowListener);
        }

        @Override
        public void buildTooltip(CustomPanelAPI panel) {
            refreshTableMap();
            tableTipMaker = panel.createUIElement(NAME_COLUMN_WIDTH, PANEL_HEIGHT, true);
            
            tablePanel = tableTipMaker.beginTable(c1, c2, Misc.getHighlightedOptionColor(), 30f, false, false, 
            new Object[]{tablePresetNamesColumnHeader, NAME_COLUMN_WIDTH - 1f});
            // tablePanel = tableTipMaker.beginTable2(Global.getSector().getPlayerFaction(), 30f, true, true, 
            // new Object[]{tablePresetNamesColumnHeader, NAME_COLUMN_WIDTH - 1f});
            
            tableRowListeners.clear();
            int id;
            int size = currentTableMap.size();
            
            if (tableUp) {
                id = 0;
            } else {
                id = (size == 1) ? 0 : size - 1;
            }

            for (Map.Entry<String, PresetUtils.FleetPreset> entry: currentTableMap.entrySet()) {
                String rowName = entry.getKey();
                Object row;
                if (selectedRowIndex == id) {
                    row = tableTipMaker.addRowWithGlow(
                        TEXT_HIGHLIGHT_COLOR, 
                        rowName
                    );
                } else {
                    row = tableTipMaker.addRowWithGlow(
                        c1, 
                        rowName
                    );
                }
                processRow(row, rowName, tableTipMaker, id, entry.getValue());
                id += tableUp ? 1 : -1;
            }
            tableTipMaker.addTable(BLANK_TABLE_TEXT, 0, 5f);
            panel.addUIElement(tableTipMaker);

            if (selectedPresetName != EMPTY_STRING) {
                selectedPresetNamePara.setText(String.format(selectedPresetNameParaFormat, selectedPresetName));
                addShipList(currentTableMap.get(selectedPresetName).fleetMembers);
            } else {
                selectedPresetNamePara.setText(String.format(selectedPresetNameParaFormat, EMPTY_STRING));
                addShipList(null);
            };

            tableTipMaker.getExternalScroller().setYOffset(yScrollOffset);
        }

        public void addShipList(List<FleetMemberAPI> fleetMembers) {
            fenaglePanele.parent.removeComponent(shipListPanel);
            shipListPanel = null;
            if (fleetMembers != null) {
                shipListPanel = Global.getSettings().createCustom(SHIP_COLUMN_WIDTH, PANEL_HEIGHT - SHIPLIST_PANEL_HEIGHT_SUBTRACTOR, null);
                TooltipMakerAPI shipListTooltip = shipListPanel.createUIElement(SHIP_COLUMN_WIDTH, PANEL_HEIGHT - MasterCancelButton.getPosition().getHeight() + SHIPLIST_PANEL_HEIGHT_SUBTRACTOR, true);
                shipListTooltip.addShipList(4, 8, SHIPLIST_SIZE, Misc.getBasePlayerColor(), fleetMembers, 5f);
                shipListPanel.addUIElement(shipListTooltip);

                // have to do this because if directly added to the refreshing panel then the game crashes when the master panel is closed
                fenaglePanele.parent.addComponent(shipListPanel).rightOfTop(fenaglePanele.panel, NAME_COLUMN_WIDTH / SHIPLIST_PANEL_PADDING_DIVISOR)
                // .setXAlignOffset(-PANEL_WIDTH - NAME_COLUMN_WIDTH)
                .setYAlignOffset(-1f * SHIPLIST_Y_OFFSET_MULTIPLIER);
            }
        }

        @Override
        public void advancePostCreation(float amount) {
        }
    
        private String trimName(String name, String prefix) {
            String trimmed = prefix;
    
            if (name.length() > 6) {
                trimmed += name.toUpperCase().substring(0, 5) + "...";
            } else trimmed += name.toUpperCase();
    
            return trimmed;
        }
    
        @Override
        public void processInput(List<InputEventAPI> events) {
        }
    
        @Override
        public void buttonPressed(Object buttonId) {
        }
    }

    public class TableRowListener implements CustomUIPanelPlugin  {
        public Object row;
        public String rowName;
        public int id;
        // public List<TableRowListener> tableRowListeners;
        public CustomPanelAPI panel;
        public PositionAPI rowPos;
        public TooltipMakerAPI tooltipMaker;
        public LabelAPI label;
        public List<FleetMemberAPI> fleetMembers;
    
        public TableRowListener(Object row,PositionAPI rowPos, String rowPresetName, List<TableRowListener> tableRowListeners, int id, List<FleetMemberAPI> fleetMembers) {
            // this.tableRowListeners = tableRowListeners;

            this.row = row;
            this.id = id;
            this.rowName = rowPresetName;
            this.rowPos = rowPos;
            this.fleetMembers = fleetMembers;
        }
    
        public void init(CustomPanelAPI panel, TooltipMakerAPI tooltipMaker, PositionAPI rowPos) {
            this.panel = panel;
            this.rowPos = rowPos;
        }

        public void clearHighlight() {

        }
    
        @Override
        public void buttonPressed(Object arg0) {

        }
    
        @Override
        public void positionChanged(PositionAPI arg0) {

        }

        @Override
        public void advance(float arg0) {

        }

        private String trimName(String name, String prefix) {
            String trimmed = prefix;

            if (name.length() > 6) {
                trimmed += name.toUpperCase().substring(0, 5) + "...";
            } else trimmed += name.toUpperCase();

            return trimmed;
        }

        @Override
        public void processInput(List<InputEventAPI> arg0) {
            for (InputEventAPI event : arg0) {
                // if (event.isMouseMoveEvent()) {
                //     int eventX = event.getX();
                //     int eventY = event.getY();
                //     float rX = rowPos.getX();
                //     float rY = rowPos.getY();
                //     float rW = rowPos.getWidth();
                //     float rH = rowPos.getHeight();

                //     if (eventX >= rX &&
                //     eventX <= rX + rW &&
                //     eventY >= rY &&
                //     eventY <= rY + rH) {
                        // break;
                //     }
                // }

                 if (event.isLMBDownEvent()) {
                    int eventX = event.getX();
                    int eventY = event.getY();
                    float rX = rowPos.getX();
                    float rY = rowPos.getY();
                    float rW = rowPos.getWidth();
                    float rH = rowPos.getHeight();

                    
                    // For table headers. I couldnt get mouse events to register on the header itself, idk what is blocking them. Above and below worked fine lol
                    // if (tableUp && this.id == 0 || (!tableUp && this.id == tableRowListeners.size() - 1)) {
                    //     float yOffsetBottom = rY / 100 * 4;
                    //     float yOffsetTop = rY / 100 * 2.6f;

                        // logger.info(String.valueOf(rX));
                        // logger.info(String.valueOf(rY));
                        // if (eventX >= rX + 4f &&
                        //     eventX <= rX + NAME_COLUMN_WIDTH - 5f &&
                        //     eventY >= rY + yOffsetBottom &&
                        //     eventY <= rY + rH + yOffsetTop ) {
                            
                        //     if (tableUp) {
                        //         tablePresetNamesColumnHeader = "Presets <Descending>";
                        //         tableUp = false;
                        //     } else {
                        //         tablePresetNamesColumnHeader = "Presets <Ascending>";
                        //         tableUp = true;
                        //     }
                        //     tablePlugin.rebuild();
                        //     // event.consume();
                        //     break;
                        // }
                        // } else if (eventX >= rX + 5f + NAME_COLUMN_WIDTH
                        //     && eventX <= rX + rW + 5f
                        //     && eventY >= rY + yOffsetBottom
                        //     && eventY <= rY + rH + yOffsetTop ) {
                        //     if (tableRight) {
                        //         tableShipsColumnHeader = "Ships <";
                        //         tableRight = false;
                        //     } else {
                        //         tableShipsColumnHeader = "Ships >";
                        //         tableRight = true;
                        //     }
                        //     tablePlugin.rebuild();
                        //     // event.consume();
                        //     break;
                        // }
                    // }

                    if (eventX >= rX &&
                    eventX <= rX + rW &&
                    eventY >= rY &&
                    eventY <= rY + rH) {
                        selectedPresetName = rowName;
                        selectedRowIndex = id;
                        enableButtonsRequiringSelection();
                        tablePlugin.rebuild();
                        // event.consume();
                        break;
                    }
                    
                }
            }
        }
    
        @Override
        public void render(float arg0) {
        }
    
        @Override
        public void renderBelow(float arg0) {
        }
    }

    private void refreshTableMap() {
        currentTableMap = PresetUtils.getFleetPresetsMapForTable(tableUp, tableRight);
        currentPresetsNum = currentTableMap.size();
    }

    private int getTableMapIndex(String text) {
        List<String> keys = new ArrayList<>(currentTableMap.keySet());
        if (!tableUp) Collections.reverse(keys);
        for (int i = 0; i < keys.size(); i++) {
            if (keys.get(i).equals(text)) return i;
        }
        return -1;
    }
    private class SaveListener extends DialogDismissedListener {
        private boolean overwrite;
        private boolean cancel;
        public SaveListener(boolean overwrite, boolean cancel) {
            this.overwrite = overwrite;
            this.cancel = cancel;
        }
    
        @Override
        public void trigger(Object... args) {
            int option = (int) args[1];

            if (option == 0) {
                // confirm
                if (overwrite && !cancel) {
                    PresetUtils.saveFleetPreset(selectedPresetName);
                    currentTableMap = PresetUtils.getFleetPresetsMapForTable(tableUp, tableRight);
                } else {
                    String text = saveNameField.getText();
                    if (!isEmptyOrWhitespace(text)) {
                        if (currentTableMap.containsKey(text)) {
                            selectedPresetName = text;
                            openOverwriteDialog(true);
                            selectedRowIndex = getTableMapIndex(text);
                        } else {
                            selectedPresetName = text;
                            PresetUtils.saveFleetPreset(text);
                            refreshTableMap();
                            selectedRowIndex = getTableMapIndex(text);
                            enableButtonsRequiringSelection();
                        }
                    }
                }
                tablePlugin.rebuild();
                return;
            } else if (option == 1) {
                if (overwrite && cancel) {
                    openSaveDialog();
                }
                // cancel
                return;
            }
        }
    }

   public class DeleteListener extends DialogDismissedListener {
        @Override
        public void trigger(Object... args) {
            int option = (int) args[1];
    
            if (option == 0) {
                // confirm
                PresetUtils.deleteFleetPreset(selectedPresetName);
                
                if (currentPresetsNum == 1) {
                    disableButtonsRequiringSelection();
                    selectedRowIndex = -1;
                    selectedPresetName = EMPTY_STRING;
                    tablePlugin.rebuild();
                    return;
                }
                
                if (tableUp) {
                    tableRowListeners.remove(selectedRowIndex);
                    selectedRowIndex--;
                    if (selectedRowIndex >= 0) {
                        selectedPresetName = tableRowListeners.get(selectedRowIndex).rowName;
                    } else if (!tableRowListeners.isEmpty()) {
                        selectedRowIndex = 0;
                        selectedPresetName = tableRowListeners.get(0).rowName;
                    }
                    tablePlugin.rebuild(); 
                    return;
                } else {
                    int actualIndex = tableRowListeners.size() - selectedRowIndex - 1;
                    tableRowListeners.remove(actualIndex);
                    
                    if (tableRowListeners.isEmpty()) {
                        selectedRowIndex = -1;
                        selectedPresetName = EMPTY_STRING;
                    } else {
                        selectedRowIndex = Math.min(selectedRowIndex, tableRowListeners.size() - 1);
                        selectedPresetName = tableRowListeners.get(tableRowListeners.size() - selectedRowIndex - 1).rowName;
                        enableButtonsRequiringSelection();
                    }
                    tablePlugin.rebuild();
                    return;
                }
            } else if (option == 1) {
                // cancel
                return;
            }
        }
    }

    public class FenaglePanele  {
        public UIPanelAPI parent;
        public CustomPanelAPI panel;
        public FenaglePanele(UIPanelAPI parent, CustomPanelAPI panel) {
            this.panel = panel;
            this.parent = parent;
        }
    }
}