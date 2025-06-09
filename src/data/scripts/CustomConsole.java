package data.scripts;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

import java.awt.Color;
import java.awt.Font;
import java.text.*;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

// outputs the log to a console window separate to the game
public class CustomConsole {
    public static class CustomConsoleAppender extends AppenderSkeleton {

        @Override
        protected void append(LoggingEvent event) {
            String message = this.layout.format(event);
            CustomConsoleWindow.getInstance().appendText(message);
        }
    
        @Override
        public void close() {}
    
        @Override
        public boolean requiresLayout() {
            return true;
        }
    }

    public static class CustomConsoleWindow extends JFrame {
        private static CustomConsoleWindow instance;
        private JTextPane textPane;
        private StyledDocument doc;
    
        private Style infoStyle, warnStyle, errorStyle, defaultStyle;
    
        private CustomConsoleWindow() {
            setTitle("Log Console");
            setSize(700, 400);
            setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    
            textPane = new JTextPane();
            textPane.setEditable(false);
            textPane.setBackground(Color.BLACK);
            textPane.setFont(new Font("Monospaced", Font.PLAIN, 14));
    
            doc = textPane.getStyledDocument();
            defineStyles(doc);
    
            JScrollPane scrollPane = new JScrollPane(textPane);
            scrollPane.getViewport().setBackground(Color.BLACK);
    
            add(scrollPane);
            setVisible(true);
        }
    
        private void defineStyles(StyledDocument doc) {
            defaultStyle = doc.addStyle("Default", null);
            StyleConstants.setForeground(defaultStyle, Color.WHITE);
    
            infoStyle = doc.addStyle("Info", null);
            StyleConstants.setForeground(infoStyle, Color.LIGHT_GRAY);
    
            warnStyle = doc.addStyle("Warn", null);
            StyleConstants.setForeground(warnStyle, Color.YELLOW);
    
            errorStyle = doc.addStyle("Error", null);
            StyleConstants.setForeground(errorStyle, Color.RED);
        }
    
        public static CustomConsoleWindow getInstance() {
            if (instance == null) {
                instance = new CustomConsoleWindow();
            }
            return instance;
        }
    
        public void appendText(String text) {
            SwingUtilities.invokeLater(() -> {
                Style styleToUse = defaultStyle;
    
                if (text.contains("ERROR")) {
                    styleToUse = errorStyle;
                } else if (text.contains("WARN")) {
                    styleToUse = warnStyle;
                } else if (text.contains("INFO")) {
                    styleToUse = infoStyle;
                }
    
                try {
                    doc.insertString(doc.getLength(), text, styleToUse);
                    textPane.setCaretPosition(doc.getLength());
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
