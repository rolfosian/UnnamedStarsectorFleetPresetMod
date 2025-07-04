package data.scripts.ui;

import data.scripts.util.UtilReflection;
import data.scripts.util.UtilReflection.ConfirmDialogData;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;

import data.scripts.util.ReflectionUtilis;
import data.scripts.util.ReflectionUtilis.ListenerFactory.DialogDismissedListener;

public class MessageBox {
    private ConfirmDialogData master;

    public MessageBox(String message) {
        LabelAPI labbel = Global.getSettings().createLabel(message, Fonts.ORBITRON_16);
        labbel.setAlignment(Alignment.MID);
        labbel.setColor(Misc.getBasePlayerColor());
        labbel.setHighlightColor(Misc.getBrightPlayerColor());
        float width = labbel.computeTextWidth(message);
        float height = labbel.computeTextHeight(message);

        master = UtilReflection.showConfirmationDialog("graphics/icons/industry/battlestation.png",
        "",
        "",
        "Ok",
        250f,
        100f,
        new DialogDismissedListener() {
            @Override
            public void trigger(Object arg0, Object arg1) {}
        });

        master.panel.removeComponent((UIComponentAPI)master.confirmButton.getInstance());
        master.panel.removeComponent((UIComponentAPI)master.textLabel);
        PositionAPI buttonPos = master.cancelButton.getInstance().getPosition();

        CustomPanelAPI labbelPanel = Global.getSettings().createCustom(width, height, null);
        TooltipMakerAPI tt = labbelPanel.createUIElement(width, height, false);
        tt.setParaFont(Fonts.ORBITRON_16);
        LabelAPI messageText = tt.addPara(message, 0f);

        messageText.setColor(Misc.getBasePlayerColor());
        messageText.setHighlightColor(Misc.getBrightPlayerColor());
        messageText.setHighlightOnMouseover(true);
        messageText.setAlignment(Alignment.MID);
        labbelPanel.addUIElement(tt);

        master.panel.addComponent(labbelPanel).inMid().setYAlignOffset(0f);

        width = buttonPos.getWidth() / 2;
        height = buttonPos.getHeight();
        buttonPos.setSize(width, height);
    }
}
