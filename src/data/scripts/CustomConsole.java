package data.scripts;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Toolkit;
import java.text.*;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

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
        private JDialog searchDialog;
        private JTextField searchField;
        private int lastSearchIndex = 0;
        private int currentMatchIndex = 0;
        private java.util.List<Integer> matchPositions = new java.util.ArrayList<>();
        private JLabel matchCounterLabel;
        private Style highlightStyle;
        private JCheckBox caseSensitiveCheckBox;
    
        private Style infoStyle, warnStyle, errorStyle, defaultStyle;
    
        private CustomConsoleWindow() {
            setTitle("Log Console");
            setSize(700, 400);
            setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            setIconImage(Toolkit.getDefaultToolkit().getImage("graphics/ui/s_icon64.png"));
    
            Color darkBackground = new Color(43, 43, 43);
            Color scrollBarThumb = new Color(100, 100, 100);
            Color scrollBarTrack = new Color(60, 60, 60);
    
            getContentPane().setBackground(darkBackground);
    
            textPane = new JTextPane();
            textPane.setEditable(false);
            textPane.setBackground(Color.BLACK);
            textPane.setFont(new Font("Monospaced", Font.PLAIN, 14));
    
            doc = textPane.getStyledDocument();
            defineStyles(doc);
            defineHighlightStyle();
    
            JScrollPane scrollPane = new JScrollPane(textPane);
            scrollPane.getViewport().setBackground(Color.BLACK);
            
            scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
                @Override
                protected void configureScrollBarColors() {
                    this.thumbColor = scrollBarThumb;
                    this.trackColor = scrollBarTrack;
                }
                
                @Override
                protected JButton createDecreaseButton(int orientation) {
                    return createZeroButton();
                }
                
                @Override
                protected JButton createIncreaseButton(int orientation) {
                    return createZeroButton();
                }
                
                private JButton createZeroButton() {
                    JButton button = new JButton();
                    button.setPreferredSize(new java.awt.Dimension(0, 0));
                    button.setMinimumSize(new java.awt.Dimension(0, 0));
                    button.setMaximumSize(new java.awt.Dimension(0, 0));
                    return button;
                }
            });
            
            scrollPane.getHorizontalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
                @Override
                protected void configureScrollBarColors() {
                    this.thumbColor = scrollBarThumb;
                    this.trackColor = scrollBarTrack;
                }
                
                @Override
                protected JButton createDecreaseButton(int orientation) {
                    return createZeroButton();
                }
                
                @Override
                protected JButton createIncreaseButton(int orientation) {
                    return createZeroButton();
                }
                
                private JButton createZeroButton() {
                    JButton button = new JButton();
                    button.setPreferredSize(new java.awt.Dimension(0, 0));
                    button.setMinimumSize(new java.awt.Dimension(0, 0));
                    button.setMaximumSize(new java.awt.Dimension(0, 0));
                    return button;
                }
            });
    
            add(scrollPane);
            setupSearchDialog();
            setupKeyBindings();
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
    
        private void defineHighlightStyle() {
            highlightStyle = doc.addStyle("Highlight", null);
            StyleConstants.setBackground(highlightStyle, new Color(255, 255, 0, 100));
            StyleConstants.setForeground(highlightStyle, Color.WHITE);
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

        private void setupSearchDialog() {
            searchDialog = new JDialog(this, "Search", false);
            searchDialog.setIconImage(Toolkit.getDefaultToolkit().getImage("graphics/ui/s_icon64.png"));
            searchDialog.setLayout(new BorderLayout());
            
            Color darkBackground = new Color(43, 43, 43);
            Color darkForeground = new Color(200, 200, 200);
            Color buttonBackground = new Color(60, 60, 60);
            Color buttonHover = new Color(80, 80, 80);
            
            searchDialog.getContentPane().setBackground(darkBackground);
            
            searchField = new JTextField(20);
            searchField.setBackground(darkBackground);
            searchField.setForeground(darkForeground);
            searchField.setCaretColor(darkForeground);
            searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100)),
                BorderFactory.createEmptyBorder(2, 5, 2, 5)
            ));
            
            JButton findButton = createStyledButton("Find", buttonBackground, darkForeground, buttonHover);
            JButton closeButton = createStyledButton("Close", buttonBackground, darkForeground, buttonHover);
            JButton prevButton = createStyledButton("↑", buttonBackground, darkForeground, buttonHover);
            JButton nextButton = createStyledButton("↓", buttonBackground, darkForeground, buttonHover);
            
            matchCounterLabel = new JLabel("0/0");
            matchCounterLabel.setForeground(darkForeground);

            caseSensitiveCheckBox = new JCheckBox("Case Sensitive");
            caseSensitiveCheckBox.setBackground(darkBackground);
            caseSensitiveCheckBox.setForeground(darkForeground);

            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.setBackground(darkBackground);
            topPanel.add(searchField, BorderLayout.CENTER);
            topPanel.add(caseSensitiveCheckBox, BorderLayout.EAST);
            searchDialog.add(topPanel, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel();
            buttonPanel.setBackground(darkBackground);
            buttonPanel.add(prevButton);
            buttonPanel.add(nextButton);
            buttonPanel.add(findButton);
            buttonPanel.add(closeButton);
            buttonPanel.add(matchCounterLabel);
            
            searchDialog.add(buttonPanel, BorderLayout.SOUTH);
            
            findButton.addActionListener(e -> findNext());
            closeButton.addActionListener(e -> {
                clearHighlights();
                searchDialog.setVisible(false);
            });
            prevButton.addActionListener(e -> navigateToPreviousMatch());
            nextButton.addActionListener(e -> navigateToNextMatch());
            
            searchField.addKeyListener(new java.awt.event.KeyAdapter() {
                public void keyPressed(java.awt.event.KeyEvent e) {
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                        findNext();
                    }
                }
            });
            
            searchDialog.pack();
            searchDialog.setMinimumSize(new java.awt.Dimension(400, searchDialog.getPreferredSize().height));
            searchDialog.setLocationRelativeTo(this);
        }

        private JButton createStyledButton(String text, Color background, Color foreground, Color hoverColor) {
            JButton button = new JButton(text);
            button.setBackground(background);
            button.setForeground(foreground);
            button.setFocusPainted(false);
            button.setBorderPainted(false);
            button.setContentAreaFilled(false);
            button.setOpaque(true);
            
            button.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    button.setBackground(hoverColor);
                }
                public void mouseExited(java.awt.event.MouseEvent e) {
                    button.setBackground(background);
                }
            });
            
            return button;
        }

        private void setupKeyBindings() {
            KeyStroke ctrlF = KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK);
            textPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlF, "search");
            textPane.getActionMap().put("search", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    searchDialog.setVisible(true);
                    searchField.requestFocus();
                }
            });
        }

        private void clearHighlights() {
            try {
                doc.setCharacterAttributes(0, doc.getLength(), defaultStyle, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void highlightAllMatches(String searchText) {
            clearHighlights();
            matchPositions.clear();
            currentMatchIndex = 0;
        
            try {
                String content = doc.getText(0, doc.getLength());
                String search = searchText;
        
                if (!caseSensitiveCheckBox.isSelected()) {
                    content = content.toLowerCase();
                    search = search.toLowerCase();
                }
        
                int index = 0;
                while ((index = content.indexOf(search, index)) != -1) {
                    matchPositions.add(index);
                    doc.setCharacterAttributes(index, searchText.length(), highlightStyle, true);
                    index += searchText.length();
                }
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        
            updateMatchCounter();
        }

        private void updateMatchCounter() {
            if (matchPositions.isEmpty()) {
                matchCounterLabel.setText("0/0");
            } else {
                matchCounterLabel.setText((currentMatchIndex + 1) + "/" + matchPositions.size());
            }
        }

        private void navigateToNextMatch() {
            if (matchPositions.isEmpty()) return;
            
            currentMatchIndex = (currentMatchIndex + 1) % matchPositions.size();
            int position = matchPositions.get(currentMatchIndex);
            textPane.setCaretPosition(position);
            textPane.setSelectionStart(position);
            textPane.setSelectionEnd(position + searchField.getText().length());
            updateMatchCounter();
        }

        private void navigateToPreviousMatch() {
            if (matchPositions.isEmpty()) return;
            
            currentMatchIndex = (currentMatchIndex - 1 + matchPositions.size()) % matchPositions.size();
            int position = matchPositions.get(currentMatchIndex);
            textPane.setCaretPosition(position);
            textPane.setSelectionStart(position);
            textPane.setSelectionEnd(position + searchField.getText().length());
            updateMatchCounter();
        }

        private void showDarkThemedMessage(String message, String title) {
            JPanel panel = new JPanel();
            panel.setBackground(new Color(43, 43, 43));
            JLabel label = new JLabel(message);
            label.setForeground(new Color(200, 200, 200));
            panel.add(label);
        
            JOptionPane optionPane = new JOptionPane(panel, JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION);
            JDialog dialog = optionPane.createDialog(this, title);
            dialog.setIconImage(Toolkit.getDefaultToolkit().getImage("graphics/ui/s_icon64.png"));
        
            applyDarkTheme(dialog.getContentPane());
            applyDarkTheme(dialog.getRootPane());
        
            dialog.setVisible(true);
        }
        
        private void applyDarkTheme(Container container) {
            for (Component comp : container.getComponents()) {
                if (comp instanceof JPanel || comp instanceof JComponent) {
                    comp.setBackground(new Color(43, 43, 43));
                    comp.setForeground(new Color(200, 200, 200));
                    if (comp instanceof Container) {
                        applyDarkTheme((Container) comp);
                    }
                }
            }
        }

        private String lastSearchText = "";
        private void findNext() {
            String searchText = searchField.getText();
            if (searchText.isEmpty()) return;
        
            boolean sameSearch = searchText.equals(lastSearchText) && !matchPositions.isEmpty();
        
            if (sameSearch) {
                navigateToNextMatch();
            } else {
                lastSearchText = searchText;
                highlightAllMatches(searchText);
        
                if (matchPositions.isEmpty()) {
                    showDarkThemedMessage("Text not found", "Search Result");
                    return;
                }
        
                currentMatchIndex = 0;
                int position = matchPositions.get(currentMatchIndex);
                textPane.setCaretPosition(position);
                textPane.setSelectionStart(position);
                textPane.setSelectionEnd(position + searchText.length());
                updateMatchCounter();
            }
        }
    }
}
