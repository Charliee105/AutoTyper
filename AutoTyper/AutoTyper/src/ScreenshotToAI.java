import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class ScreenshotToAI {

    private static List<String> chatHistory = new ArrayList<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("AI Interaction");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(600, 400);
            frame.setResizable(true);
            frame.setLayout(new BorderLayout());
            frame.getContentPane().setBackground(new Color(60, 63, 65));

            JPanel chatPanel = new JPanel();
            chatPanel.setLayout(new BorderLayout());
            chatPanel.setBackground(new Color(43, 43, 43));

            JTextArea chatArea = new JTextArea();
            chatArea.setFont(new Font("Arial", Font.PLAIN, 14));
            chatArea.setLineWrap(true);
            chatArea.setWrapStyleWord(true);
            chatArea.setEditable(false);
            chatArea.setBackground(new Color(43, 43, 43));
            chatArea.setForeground(Color.WHITE);

            JScrollPane scrollPane = new JScrollPane(chatArea);
            chatPanel.add(scrollPane, BorderLayout.CENTER);

            JPopupMenu chatAreaPopupMenu = new JPopupMenu();
            JMenuItem copyMenuItem = new JMenuItem("Copy");
            copyMenuItem.addActionListener(e -> chatArea.copy());
            chatAreaPopupMenu.add(copyMenuItem);
            chatArea.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        chatAreaPopupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        chatAreaPopupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            });

            JPanel inputPanel = new JPanel();
            inputPanel.setLayout(new BorderLayout());
            inputPanel.setBackground(new Color(43, 43, 43));
            inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JTextArea userInputField = new JTextArea(3, 30);
            userInputField.setFont(new Font("Arial", Font.PLAIN, 14));
            userInputField.setBackground(new Color(60, 63, 65));
            userInputField.setForeground(Color.WHITE);
            userInputField.setBorder(BorderFactory.createLineBorder(new Color(75, 110, 175), 1));
            userInputField.setCaretColor(Color.WHITE);
            userInputField.setLineWrap(true);
            userInputField.setWrapStyleWord(true);

            userInputField.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isAltDown()) {
                        e.consume();
                        sendMessage(userInputField, chatArea);
                    } else if (e.getKeyCode() == KeyEvent.VK_ENTER && e.isAltDown()) {
                        userInputField.append("\n");
                    }
                }
            });

            JPopupMenu userInputPopupMenu = new JPopupMenu();
            JMenuItem pasteMenuItem = new JMenuItem("Paste");
            pasteMenuItem.addActionListener(e -> userInputField.paste());
            userInputPopupMenu.add(pasteMenuItem);
            userInputField.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        userInputPopupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        userInputPopupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            });

            JButton sendButton = new JButton("Send");
            sendButton.setFont(new Font("Arial", Font.PLAIN, 16));
            sendButton.setBackground(new Color(75, 110, 175));
            sendButton.setForeground(Color.WHITE);
            sendButton.setFocusPainted(false);
            sendButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(75, 110, 175), 2),
                    BorderFactory.createEmptyBorder(5, 10, 5, 10))
            );
            sendButton.addActionListener(e -> sendMessage(userInputField, chatArea));
            inputPanel.add(new JScrollPane(userInputField), BorderLayout.CENTER);
            inputPanel.add(sendButton, BorderLayout.EAST);

            frame.add(chatPanel, BorderLayout.CENTER);
            frame.add(inputPanel, BorderLayout.SOUTH);
            frame.setVisible(true);
        });
    }

    private static void sendMessage(JTextArea userInputField, JTextArea chatArea) {
        String userInput = userInputField.getText().trim();
        if (!userInput.isEmpty()) {
            chatHistory.add("You: " + userInput);
            chatArea.append("You: " + userInput + "\n");
            userInputField.setText("");

            String aiResponse = sendTextToAI(userInput);
            String parsedResponse = parseAIResponse(aiResponse);
            chatHistory.add("AI: " + parsedResponse);
            chatArea.append("AI: " + parsedResponse + "\n");
        }
    }

    private static String sendTextToAI(String userInput) {
        try {
            URL url = new URL("https://api.openai.com/v1/chat/completions");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer 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");

            String jsonBody = "{" +
                    "\"model\": \"gpt-3.5-turbo\"," +
                    "\"messages\": [{\"role\": \"user\", \"content\": \"" + userInput + "\"}]," +
                    "\"max_tokens\": 100" +
                    "}";

            connection.getOutputStream().write(jsonBody.getBytes("UTF-8"));

            int statusCode = connection.getResponseCode();
            if (statusCode != HttpURLConnection.HTTP_OK) {
                ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
                int ch;
                while ((ch = connection.getErrorStream().read()) != -1) {
                    errorStream.write(ch);
                }
                System.err.println("Error Response: " + errorStream.toString());
                return "Error: " + errorStream.toString();
            }

            ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
            int ch;
            while ((ch = connection.getInputStream().read()) != -1) {
                responseStream.write(ch);
            }

            return responseStream.toString();
        } catch (Exception e) {
            System.err.println("Error communicating with AI: " + e.getMessage());
            e.printStackTrace();
            return "Error communicating with AI.";
        }
    }

    private static String parseAIResponse(String aiResponse) {
        try {
            int startIndex = aiResponse.indexOf("\"choices\":");
            if (startIndex == -1) {
                return "No choices found in the response.";
            }
            startIndex = aiResponse.indexOf("\"content\": \"", startIndex) + 12;
            int endIndex = aiResponse.indexOf("\"", startIndex);
            if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
                return aiResponse.substring(startIndex, endIndex);
            } else {
                return "Error extracting content from AI response.";
            }
        } catch (Exception e) {
            System.err.println("Error parsing AI response: " + e.getMessage());
            e.printStackTrace();
            return "Error parsing AI response.";
        }
    }
}
