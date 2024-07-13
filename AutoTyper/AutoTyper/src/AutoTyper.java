import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

public class AutoTyper {

    private static final Map<Character, Integer> keyMap = new HashMap<>();
    private static final java.util.List<String> history = new ArrayList<>();
    private static final java.util.List<String> clipboardHistory = new ArrayList<>();
    private static final Random random = new Random();
    private static boolean isRunning = false;
    private static boolean isPaused = false;
    private static final Object pauseLock = new Object();
    private static Thread typingThread;
    private static int originalDelay = 0;

    private static JFrame frame;
    private static JPanel inputPanel;
    private static JPanel historyPanel;
    private static JPanel preferencesPanel;
    private static JTabbedPane tabbedPane;
    private static JProgressBar progressBar;
    private static JLabel timeRemainingLabel;
    private static JTextArea textArea1;
    private static JTextField textField2;
    private static JTextField textField3;
    private static JCheckBox alwaysOnTopCheckBox;
    private static JCheckBox darkModeCheckBox;

    static {
        // Populate the key map with relevant key codes for characters
        for (char c = 'a'; c <= 'z'; c++) keyMap.put(c, KeyEvent.getExtendedKeyCodeForChar(c));
        for (char c = 'A'; c <= 'Z'; c++) keyMap.put(c, KeyEvent.getExtendedKeyCodeForChar(c));
        for (char c = '0'; c <= '9'; c++) keyMap.put(c, KeyEvent.getExtendedKeyCodeForChar(c));

        keyMap.put(' ', KeyEvent.VK_SPACE);
        keyMap.put('\n', KeyEvent.VK_ENTER);
        keyMap.put('.', KeyEvent.VK_PERIOD);
        keyMap.put(',', KeyEvent.VK_COMMA);
        keyMap.put('!', KeyEvent.VK_1);
        keyMap.put('@', KeyEvent.VK_2);
        keyMap.put('#', KeyEvent.VK_3);
        keyMap.put('$', KeyEvent.VK_4);
        keyMap.put('%', KeyEvent.VK_5);
        keyMap.put('^', KeyEvent.VK_6);
        keyMap.put('&', KeyEvent.VK_7);
        keyMap.put('*', KeyEvent.VK_8);
        keyMap.put('(', KeyEvent.VK_9);
        keyMap.put(')', KeyEvent.VK_0);
        keyMap.put('-', KeyEvent.VK_MINUS);
        keyMap.put('_', KeyEvent.VK_MINUS);
        keyMap.put('=', KeyEvent.VK_EQUALS);
        keyMap.put('+', KeyEvent.VK_EQUALS);
        keyMap.put('[', KeyEvent.VK_OPEN_BRACKET);
        keyMap.put(']', KeyEvent.VK_CLOSE_BRACKET);
        keyMap.put('{', KeyEvent.VK_OPEN_BRACKET);
        keyMap.put('}', KeyEvent.VK_CLOSE_BRACKET);
        keyMap.put('\\', KeyEvent.VK_BACK_SLASH);
        keyMap.put('|', KeyEvent.VK_BACK_SLASH);
        keyMap.put(';', KeyEvent.VK_SEMICOLON);
        keyMap.put(':', KeyEvent.VK_SEMICOLON);
        keyMap.put('\'', KeyEvent.VK_QUOTE);
        keyMap.put('"', KeyEvent.VK_QUOTE);
        keyMap.put('<', KeyEvent.VK_COMMA);
        keyMap.put('>', KeyEvent.VK_PERIOD);
        keyMap.put('/', KeyEvent.VK_SLASH);
        keyMap.put('?', KeyEvent.VK_SLASH);
        keyMap.put('~', KeyEvent.VK_BACK_QUOTE);
        keyMap.put('`', KeyEvent.VK_BACK_QUOTE);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new javax.swing.plaf.nimbus.NimbusLookAndFeel());
        } catch (Exception e) {
            e.printStackTrace();
        }

        frame = new JFrame("Auto Typer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);

        inputPanel = createInputPanel();
        historyPanel = createHistoryPanel();
        preferencesPanel = createPreferencesPanel();

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Input", inputPanel);
        tabbedPane.addTab("History", historyPanel);
        tabbedPane.addTab("Preferences", preferencesPanel);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        JPanel controlPanel = createControlPanel();
        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        frame.add(mainPanel);
        frame.setVisible(true);

        frame.setAlwaysOnTop(true); // Enable always on top by default
    }

    private static JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;

        textArea1 = createTextAreaWithContextMenu(5, 20);
        JScrollPane scrollPane1 = new JScrollPane(textArea1);

        gbc.gridwidth = 2;
        panel.add(createLabelWithInfoIcon("Enter the text to type:", "Enter the text you want to be automatically typed."), gbc);
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 1;
        gbc.weighty = 1;
        panel.add(scrollPane1, gbc);

        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridy++;
        panel.add(createLabelWithInfoIcon("Enter the delay before typing starts (in seconds):", "Enter the delay before typing starts (recommended: 3-4 seconds)."), gbc);
        gbc.gridy++;
        textField2 = createTextFieldWithContextMenu(5);
        panel.add(textField2, gbc);

        gbc.gridy++;
        panel.add(createLabelWithInfoIcon("Enter the typing speed (interval between keystrokes in milliseconds):", "Enter the typing speed (recommended: 80-100 ms)."), gbc);
        gbc.gridy++;
        textField3 = createTextFieldWithContextMenu(5);
        panel.add(textField3, gbc);

        return panel;
    }

    private static JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        HistoryTableModel historyTableModel = new HistoryTableModel(history);
        JTable historyTable = new JTable(historyTableModel);
        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane historyScrollPane = new JScrollPane(historyTable);
        panel.add(historyScrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.add(new JLabel("Clipboard:"));

        JButton copyButton = new JButton("Copy");
        bottomPanel.add(copyButton);

        JButton clearHistoryButton = new JButton("Clear History");
        clearHistoryButton.addActionListener(e -> {
            history.clear();
            historyTableModel.fireTableDataChanged();
        });
        bottomPanel.add(clearHistoryButton);

        copyButton.addActionListener(e -> {
            int selectedRow = historyTable.getSelectedRow();
            if (selectedRow >= 0) {
                String text = historyTableModel.getValueAt(selectedRow, 1).toString();
                StringSelection stringSelection = new StringSelection(text);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);
                addClipboardHistory(text); // Add copied text to clipboard history
            } else {
                showWarningWindow("Please select a row from the history to copy.");
            }
        });

        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    private static JPanel createPreferencesPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        alwaysOnTopCheckBox = new JCheckBox("Always on Top", true);
        JCheckBox wrapTextCheckBox = new JCheckBox("Wrap Text", true);
        darkModeCheckBox = new JCheckBox("Dark Mode");

        alwaysOnTopCheckBox.addActionListener(e -> SwingUtilities.getWindowAncestor(inputPanel).setAlwaysOnTop(alwaysOnTopCheckBox.isSelected()));
        wrapTextCheckBox.addActionListener(e -> {
            boolean wrap = wrapTextCheckBox.isSelected();
            textArea1.setLineWrap(wrap);
            textArea1.setWrapStyleWord(wrap);
        });

        darkModeCheckBox.addActionListener(e -> {
            if (darkModeCheckBox.isSelected()) {
                setDarkMode();
            } else {
                setLightMode();
            }
        });

        panel.add(alwaysOnTopCheckBox);
        panel.add(wrapTextCheckBox);
        panel.add(darkModeCheckBox);

        return panel;
    }

    private static JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton startButton = new JButton("Start Typing");
        buttonPanel.add(startButton);

        JButton pauseButton = new JButton("Pause");
        JButton stopButton = new JButton("Stop");
        pauseButton.setVisible(false);
        stopButton.setVisible(false);
        buttonPanel.add(pauseButton);
        buttonPanel.add(stopButton);
        controlPanel.add(buttonPanel, BorderLayout.CENTER);

        // Progress bar and time remaining label
        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(700, 20));
        timeRemainingLabel = new JLabel("Time remaining: 00:00", SwingConstants.RIGHT);
        JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.add(timeRemainingLabel, BorderLayout.NORTH);
        progressPanel.add(progressBar, BorderLayout.CENTER);

        controlPanel.add(progressPanel, BorderLayout.SOUTH);

        // Tab change listener to hide/show controls
        tabbedPane.addChangeListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            boolean showControls = selectedIndex == 0; // Show controls only on Input tab
            startButton.setVisible(showControls);
            pauseButton.setVisible(showControls && isRunning);
            stopButton.setVisible(showControls && isRunning);
            progressPanel.setVisible(showControls);
        });

        startButton.addActionListener(e -> startTyping(startButton, pauseButton, stopButton));
        pauseButton.addActionListener(e -> togglePause(pauseButton));
        stopButton.addActionListener(e -> stopTyping(startButton, pauseButton, stopButton));

        return controlPanel;
    }

    private static JPanel createLabelWithInfoIcon(String labelText, String tooltipText) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(labelText);
        JLabel infoIcon = createInfoIcon(tooltipText);
        panel.add(label, BorderLayout.WEST);
        panel.add(infoIcon, BorderLayout.EAST);
        return panel;
    }

    private static JLabel createInfoIcon(String tooltipText) {
        JLabel infoIcon = new JLabel("\uD83D\uDEC8"); // Unicode for information icon
        infoIcon.setToolTipText(tooltipText);
        infoIcon.setHorizontalAlignment(SwingConstants.RIGHT);
        infoIcon.setPreferredSize(new Dimension(30, 30)); // Increase icon size
        return infoIcon;
    }

    private static JTextField createTextFieldWithContextMenu(int columns) {
        JTextField textField = new JTextField(columns);
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem pasteItem = new JMenuItem("Paste");
        JMenuItem clipboardHistoryItem = new JMenuItem("Clipboard History");

        pasteItem.addActionListener(e -> {
            textField.paste();
            addClipboardHistory(textField.getText()); // Add pasted text to clipboard history
        });

        clipboardHistoryItem.addActionListener(e -> showSystemClipboardHistory());

        contextMenu.add(pasteItem);
        contextMenu.add(clipboardHistoryItem);
        textField.setComponentPopupMenu(contextMenu);

        return textField;
    }

    private static JTextArea createTextAreaWithContextMenu(int rows, int columns) {
        JTextArea textArea = new JTextArea(rows, columns);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem pasteItem = new JMenuItem("Paste");
        JMenuItem clipboardHistoryItem = new JMenuItem("Clipboard History");

        pasteItem.addActionListener(e -> {
            textArea.paste();
            addClipboardHistory(textArea.getText()); // Add pasted text to clipboard history
        });

        clipboardHistoryItem.addActionListener(e -> showSystemClipboardHistory());

        contextMenu.add(pasteItem);
        contextMenu.add(clipboardHistoryItem);
        textArea.setComponentPopupMenu(contextMenu);

        return textArea;
    }

    private static void addClipboardHistory(String text) {
        if (!text.isEmpty() && (clipboardHistory.isEmpty() || !text.equals(clipboardHistory.get(clipboardHistory.size() - 1)))) {
            clipboardHistory.add(text);
        }
    }

    private static void setFieldsEditable(boolean editable, JTextArea textArea, JTextField... fields) {
        textArea.setEditable(editable);
        textArea.setEnabled(editable);
        textArea.setBackground(editable ? Color.WHITE : Color.LIGHT_GRAY);
        for (JTextField field : fields) {
            field.setEditable(editable);
            field.setEnabled(editable);
            field.setBackground(editable ? Color.WHITE : Color.LIGHT_GRAY);
        }
    }

    private static void startTyping(JButton startButton, JButton pauseButton, JButton stopButton) {
        String textToType = textArea1.getText();
        String delayBeforeStartStr = textField2.getText();
        String typingSpeedStr = textField3.getText();

        if (textToType == null || textToType.isEmpty()) {
            showWarningWindow("No text entered. Please enter some text.");
        } else if (delayBeforeStartStr == null || delayBeforeStartStr.isEmpty() || typingSpeedStr == null || typingSpeedStr.isEmpty()) {
            showWarningWindow("Please enter valid numbers for delay and typing speed.");
        } else {
            try {
                originalDelay = Integer.parseInt(delayBeforeStartStr) * 1000; // Convert to milliseconds
                int typingSpeed = Integer.parseInt(typingSpeedStr);

                Robot robot = new Robot();

                isRunning = true;
                setFieldsEditable(false, textArea1, textField2, textField3);
                startButton.setVisible(false);
                pauseButton.setVisible(true);
                stopButton.setVisible(true);
                progressBar.setForeground(Color.GREEN);

                SwingUtilities.invokeLater(() -> {
                    JOptionPane optionPane = new JOptionPane("Switch to the target window. Typing will start after the specified delay.", JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION);
                    JDialog dialog = optionPane.createDialog("Information");
                    dialog.setAlwaysOnTop(true); // Ensure the dialog is always on top
                    dialog.setVisible(true);

                    javax.swing.Timer timer = new javax.swing.Timer(originalDelay, event -> {
                        typingThread = new Thread(() -> {
                            try {
                                long startTime = System.currentTimeMillis();
                                int textLength = textToType.length();
                                for (int i = 0; i < textLength; i++) {
                                    if (!isRunning) break;
                                    synchronized (pauseLock) {
                                        while (isPaused) {
                                            pauseLock.wait();
                                        }
                                    }
                                    char c = textToType.charAt(i);
                                    int delay = typingSpeed;

                                    if (random.nextBoolean()) {
                                        delay += random.nextInt(61) - 30;
                                    }

                                    if (Character.isUpperCase(c) || "!@#$%^&*()_+{}|:\"<>?".indexOf(c) >= 0) {
                                        robot.keyPress(KeyEvent.VK_SHIFT);
                                        robot.keyPress(keyMap.get(Character.toLowerCase(c)));
                                        robot.keyRelease(keyMap.get(Character.toLowerCase(c)));
                                        robot.keyRelease(KeyEvent.VK_SHIFT);
                                    } else if (keyMap.containsKey(c)) {
                                        robot.keyPress(keyMap.get(c));
                                        robot.keyRelease(keyMap.get(c));
                                    }

                                    robot.delay(delay);
                                    long elapsed = System.currentTimeMillis() - startTime;
                                    int progress = (int) ((double) i / textLength * 100);
                                    int remainingTime = (int) ((textLength - i) * (elapsed / (i + 1)));
                                    SwingUtilities.invokeLater(() -> {
                                        progressBar.setValue(progress);
                                        timeRemainingLabel.setText("Time remaining: " + formatTime(remainingTime));
                                    });
                                }
                                isRunning = false;
                                SwingUtilities.invokeLater(() -> {
                                    frame.setAlwaysOnTop(true);
                                    frame.setAlwaysOnTop(false);
                                    setFieldsEditable(true, textArea1, textField2, textField3);
                                    startButton.setVisible(true);
                                    pauseButton.setVisible(false);
                                    stopButton.setVisible(false);
                                    progressBar.setValue(0);
                                    timeRemainingLabel.setText("Time remaining: 00:00");
                                    if (alwaysOnTopCheckBox.isSelected()) {
                                        frame.setAlwaysOnTop(true);
                                    }
                                });
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                        });
                        typingThread.start();
                    });
                    timer.setRepeats(false);
                    timer.start();
                });
            } catch (NumberFormatException ex) {
                showWarningWindow("Please enter valid numbers for delay and typing speed.");
            } catch (AWTException ex) {
                ex.printStackTrace();
            }
        }
    }

    private static void togglePause(JButton pauseButton) {
        if (isPaused) {
            isPaused = false;
            JOptionPane.showMessageDialog(null, "Switch to the target window. Typing will resume after the specified delay.");
            new Thread(() -> {
                try {
                    Thread.sleep(originalDelay);
                    synchronized (pauseLock) {
                        pauseLock.notifyAll();
                    }
                    pauseButton.setText("Pause");
                    progressBar.setForeground(Color.GREEN);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }).start();
        } else {
            isPaused = true;
            pauseButton.setText("Resume");
            progressBar.setForeground(Color.ORANGE);
        }
    }

    private static void stopTyping(JButton startButton, JButton pauseButton, JButton stopButton) {
        if (isRunning) {
            isRunning = false;
            typingThread.interrupt();
            setFieldsEditable(true, textArea1, textField2, textField3);
            startButton.setVisible(true);
            pauseButton.setVisible(false);
            stopButton.setVisible(false);
            progressBar.setValue(0);
            timeRemainingLabel.setText("Time remaining: 00:00");
            if (alwaysOnTopCheckBox.isSelected()) {
                frame.setAlwaysOnTop(true);
            }
        }
    }

    private static void setDarkMode() {
        Color darkBackground = new Color(18, 30, 49);
        Color darkForeground = new Color(230, 230, 230);
        Color darkControl = new Color(128, 128, 128);
        Color darkInfo = new Color(128, 128, 128);

        UIManager.put("control", darkControl);
        UIManager.put("info", darkInfo);
        UIManager.put("nimbusBase", darkBackground);
        UIManager.put("nimbusAlertYellow", new Color(248, 187, 0));
        UIManager.put("nimbusDisabledText", darkControl);
        UIManager.put("nimbusFocus", new Color(115, 164, 209));
        UIManager.put("nimbusGreen", new Color(176, 179, 50));
        UIManager.put("nimbusInfoBlue", new Color(66, 139, 221));
        UIManager.put("nimbusLightBackground", darkBackground);
        UIManager.put("nimbusOrange", new Color(191, 98, 4));
        UIManager.put("nimbusRed", new Color(169, 46, 34));
        UIManager.put("nimbusSelectedText", darkForeground);
        UIManager.put("nimbusSelectionBackground", new Color(104, 93, 156));
        UIManager.put("text", darkForeground);

        updateUI();
    }

    private static void setLightMode() {
        Color lightBackground = new Color(255, 255, 255);
        Color lightForeground = new Color(0, 0, 0);
        Color lightControl = new Color(214, 217, 223);
        Color lightInfo = new Color(214, 217, 223);

        UIManager.put("control", lightControl);
        UIManager.put("info", lightInfo);
        UIManager.put("nimbusBase", new Color(51, 98, 140));
        UIManager.put("nimbusAlertYellow", new Color(255, 220, 35));
        UIManager.put("nimbusDisabledText", new Color(142, 143, 145));
        UIManager.put("nimbusFocus", new Color(115, 164, 209));
        UIManager.put("nimbusGreen", new Color(176, 179, 50));
        UIManager.put("nimbusInfoBlue", new Color(47, 92, 180));
        UIManager.put("nimbusLightBackground", lightBackground);
        UIManager.put("nimbusOrange", new Color(191, 98, 4));
        UIManager.put("nimbusRed", new Color(169, 46, 34));
        UIManager.put("nimbusSelectedText", lightForeground);
        UIManager.put("nimbusSelectionBackground", new Color(57, 105, 138));
        UIManager.put("text", lightForeground);

        updateUI();
    }

    private static void updateUI() {
        SwingUtilities.updateComponentTreeUI(frame);
        updateComponentTreeUI(frame.getContentPane());
        updateTableHeaders();
    }

    private static void updateComponentTreeUI(Component component) {
        SwingUtilities.updateComponentTreeUI(component);
        if (component instanceof JComponent) {
            for (Component child : ((JComponent) component).getComponents()) {
                updateComponentTreeUI(child);
            }
        }
    }

    private static void updateTableHeaders() {
        JTable historyTable = (JTable) ((JScrollPane) historyPanel.getComponent(0)).getViewport().getView();
        JTableHeader header = historyTable.getTableHeader();
        header.setBackground(UIManager.getColor("control"));
        header.setForeground(UIManager.getColor("text"));
    }

    private static void showWarningWindow(String message) {
        JOptionPane optionPane = new JOptionPane(message, JOptionPane.WARNING_MESSAGE, JOptionPane.DEFAULT_OPTION);
        JDialog dialog = optionPane.createDialog("Warning");
        dialog.setAlwaysOnTop(true); // Ensure the dialog is always on top
        dialog.setVisible(true);
    }

    private static void showSystemClipboardHistory() {
        try {
            Robot robot = new Robot();
            // Press Windows key
            robot.keyPress(KeyEvent.VK_WINDOWS);
            // Press V key
            robot.keyPress(KeyEvent.VK_V);
            // Release V key
            robot.keyRelease(KeyEvent.VK_V);
            // Release Windows key
            robot.keyRelease(KeyEvent.VK_WINDOWS);
        } catch (AWTException e) {
            e.printStackTrace();
            showWarningWindow("Failed to open clipboard history.");
        }
    }

    private static String formatTime(int milliseconds) {
        int seconds = milliseconds / 1000;
        int minutes = seconds / 60;
        seconds %= 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    static class HistoryTableModel extends AbstractTableModel {
        private final java.util.List<String> history;
        private final String[] columnNames = {"Index", "Typed Text"};

        public HistoryTableModel(java.util.List<String> history) {
            this.history = history;
        }

        @Override
        public int getRowCount() {
            return history.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return rowIndex + 1;
                case 1:
                    return history.get(rowIndex);
                default:
                    return null;
            }
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public void fireTableDataChanged() {
            super.fireTableDataChanged();
        }
    }

    static class TextAreaRenderer extends JTextArea implements TableCellRenderer {
        public TextAreaRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value != null ? value.toString() : "");
            setSize(table.getColumnModel().getColumn(column).getWidth(), getPreferredSize().height);
            if (table.getRowHeight(row) != getPreferredSize().height) {
                table.setRowHeight(row, getPreferredSize().height);
            }
            return this;
        }
    }
}
