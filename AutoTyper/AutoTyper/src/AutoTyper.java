import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

public class AutoTyper {

    private static final Map<Character, Integer> keyMap = new HashMap<>();
    private static final List<String> history = new ArrayList<>();
    private static final List<String> clipboardHistory = new ArrayList<>();
    private static final Random random = new Random();
    private static boolean isRunning = false;
    private static boolean isPaused = false;
    private static final Object pauseLock = new Object();
    private static Thread typingThread;
    private static int originalDelay = 0;

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

        JPanel inputPanel = new JPanel(new GridLayout(0, 1, 10, 10));
        JTextField textField1 = createTextFieldWithContextMenu(20);
        JTextField textField2 = createTextFieldWithContextMenu(5);
        JTextField textField3 = createTextFieldWithContextMenu(5);

        inputPanel.add(new JLabel("Enter the text to type:"));
        inputPanel.add(createPanelWithTextFieldAndButton(textField1));

        inputPanel.add(new JLabel("Enter the delay before typing starts (in seconds):"));
        inputPanel.add(createPanelWithTextFieldAndButton(textField2));

        inputPanel.add(new JLabel("Enter the typing speed (interval between keystrokes in milliseconds):"));
        inputPanel.add(createPanelWithTextFieldAndButton(textField3));

        JCheckBox alwaysOnTopCheckBox = new JCheckBox("Always on Top");
        alwaysOnTopCheckBox.addActionListener(e -> SwingUtilities.getWindowAncestor(inputPanel).setAlwaysOnTop(alwaysOnTopCheckBox.isSelected()));
        inputPanel.add(alwaysOnTopCheckBox);

        HistoryTableModel historyTableModel = new HistoryTableModel(history);
        JTable historyTable = new JTable(historyTableModel);
        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane historyScrollPane = new JScrollPane(historyTable);

        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.add(historyScrollPane, BorderLayout.CENTER);

        JCheckBox wrapTextCheckBox = new JCheckBox("Wrap Text");
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.add(wrapTextCheckBox);

        JButton copyButton = new JButton("Copy");
        bottomPanel.add(copyButton);

        JButton clearHistoryButton = new JButton("Clear History");
        clearHistoryButton.addActionListener(e -> {
            history.clear();
            historyTableModel.fireTableDataChanged();
        });
        bottomPanel.add(clearHistoryButton);

        historyPanel.add(bottomPanel, BorderLayout.SOUTH);

        wrapTextCheckBox.addActionListener(e -> {
            boolean wrap = wrapTextCheckBox.isSelected();
            if (wrap) {
                historyTable.getColumnModel().getColumn(1).setCellRenderer(new TextAreaRenderer());
            } else {
                historyTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer());
            }
            historyTableModel.fireTableDataChanged();
        });

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

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Input", inputPanel);
        tabbedPane.addTab("History", historyPanel);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        JFrame frame = new JFrame("Auto Typer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLocationRelativeTo(null);
        frame.add(mainPanel, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton startButton = new JButton("Start Typing");
        controlPanel.add(startButton);

        JButton pauseButton = new JButton("Pause");
        JButton stopButton = new JButton("Stop");
        pauseButton.setVisible(false);
        stopButton.setVisible(false);
        controlPanel.add(pauseButton);
        controlPanel.add(stopButton);

        frame.add(controlPanel, BorderLayout.SOUTH);

        startButton.addActionListener(e -> {
            String textToType = textField1.getText();
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
                    setFieldsEditable(false, textField1, textField2, textField3);
                    startButton.setVisible(false);
                    pauseButton.setVisible(true);
                    stopButton.setVisible(true);
                    JOptionPane.showMessageDialog(null, "Switch to the target window. Typing will start after the specified delay.");
                    typingThread = new Thread(() -> {
                        try {
                            Thread.sleep(originalDelay);
                            typeText(robot, textToType, typingSpeed, historyTableModel);
                            isRunning = false;
                            SwingUtilities.invokeLater(() -> {
                                frame.setAlwaysOnTop(true);
                                frame.setAlwaysOnTop(false);
                                setFieldsEditable(true, textField1, textField2, textField3);
                                startButton.setVisible(true);
                                pauseButton.setVisible(false);
                                stopButton.setVisible(false);
                                if (alwaysOnTopCheckBox.isSelected()) {
                                    frame.setAlwaysOnTop(true);
                                }
                            });
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    });
                    typingThread.start();
                } catch (NumberFormatException ex) {
                    showWarningWindow("Please enter valid numbers for delay and typing speed.");
                } catch (AWTException ex) {
                    ex.printStackTrace();
                }
            }
        });

        pauseButton.addActionListener(e -> {
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
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }).start();
            } else {
                isPaused = true;
                pauseButton.setText("Resume");
            }
        });

        stopButton.addActionListener(e -> {
            if (isRunning) {
                isRunning = false;
                typingThread.interrupt();
                setFieldsEditable(true, textField1, textField2, textField3);
                startButton.setVisible(true);
                pauseButton.setVisible(false);
                stopButton.setVisible(false);
                if (alwaysOnTopCheckBox.isSelected()) {
                    frame.setAlwaysOnTop(true);
                }
            }
        });

        frame.setVisible(true);
    }

    private static JPanel createPanelWithTextFieldAndButton(JTextField textField) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(textField, BorderLayout.CENTER);
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> textField.setText(""));
        panel.add(clearButton, BorderLayout.EAST);
        return panel;
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

    private static void addClipboardHistory(String text) {
        if (!text.isEmpty() && (clipboardHistory.isEmpty() || !text.equals(clipboardHistory.get(clipboardHistory.size() - 1)))) {
            clipboardHistory.add(text);
        }
    }

    private static void setFieldsEditable(boolean editable, JTextField... fields) {
        for (JTextField field : fields) {
            field.setEditable(editable);
            field.setEnabled(editable);
        }
    }

    private static void typeText(Robot robot, String text, int baseDelay, HistoryTableModel historyTableModel) {
        int burstCount = 0;
        StringBuilder typedText = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (!isRunning) {
                break;
            }
            synchronized (pauseLock) {
                while (isPaused) {
                    try {
                        pauseLock.wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }

            int delay = baseDelay;

            if (random.nextBoolean()) { // 50% chance to adjust the delay
                delay += random.nextInt(61) - 30; // Randomize within ±30ms
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
            typedText.append(c);
            burstCount++;

            // Introduce natural pauses and bursts
            if (burstCount >= random.nextInt(8) + 8) { // Random burst of 8 to 15 characters
                robot.delay(random.nextInt(201) + 100); // Pause for 100 to 300 ms
                burstCount = 0;
            }
        }
        addTypedTextToHistory(typedText.toString(), historyTableModel);
    }

    private static void addTypedTextToHistory(String typedText, HistoryTableModel historyTableModel) {
        history.add(typedText);
        SwingUtilities.invokeLater(historyTableModel::fireTableDataChanged);
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

    static class HistoryTableModel extends AbstractTableModel {
        private final List<String> history;
        private final String[] columnNames = {"Index", "Typed Text"};

        public HistoryTableModel(List<String> history) {
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
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value != null ? value.toString() : "");
            setSize(table.getColumnModel().getColumn(column).getWidth(), getPreferredSize().height);
            if (table.getRowHeight(row) != getPreferredSize().height) {
                table.setRowHeight(row, getPreferredSize().height);
            }
            return this;
        }
    }
}
