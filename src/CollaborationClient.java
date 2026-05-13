import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

/**
 * CollaborationClient.java
 * A Swing client that connects to CollaborationServer, sends chat and draw messages,
 * and updates its UI based on incoming broadcasts.
 */
public class CollaborationClient extends JFrame {
    private JTextArea chatArea;
    private JTextField messageField;
    private DrawPanel drawPanel;
    private PrintWriter out;
    private BufferedReader in;

    public CollaborationClient(String serverAddress, int port) {
        setTitle("Remote Chat & Drawing Collaboration");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initUI();
        connectToServer(serverAddress, port);
        setVisible(true);
    }

    private void initUI() {
        // Chat area on the right
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScroll = new JScrollPane(chatArea);

        messageField = new JTextField();
        JButton sendButton = new JButton("Send");

        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.add(chatScroll, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        // Drawing area on the left
        drawPanel = new DrawPanel();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, drawPanel, chatPanel);
        splitPane.setDividerLocation(700);
        add(splitPane, BorderLayout.CENTER);

        // send chat action
        sendButton.addActionListener(e -> sendChat());
        messageField.addActionListener(e -> sendChat());
    }

    private void connectToServer(String serverAddress, int port) {
        try {
            Socket socket = new Socket(serverAddress, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Thread to read incoming messages and update UI
            new Thread(() -> {
                String msg;
                try {
                    while ((msg = in.readLine()) != null) {
                        final String message = msg;
                        SwingUtilities.invokeLater(() -> {
                            if (message.startsWith("CHAT:")) {
                                chatArea.append(message.substring(5) + "\\n");
                            } else if (message.startsWith("DRAW:")) {
                                drawPanel.updateDrawing(message);
                            } else {
                                chatArea.append(message + "\\n");
                            }
                        });
                    }
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "Connection to server lost.");
                    System.exit(0);
                }
            }).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Unable to connect to server: " + e.getMessage());
            System.exit(0);
        }
    }

    private void sendChat() {
        String text = messageField.getText().trim();
        if (!text.isEmpty() && out != null) {
            out.println("CHAT:" + text);
            messageField.setText("");
        }
    }

    // Drawing panel inner class
    class DrawPanel extends JPanel {
        private int lastX, lastY;
        private Color color = Color.BLACK;

        public DrawPanel() {
            setBackground(Color.WHITE);
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    lastX = e.getX();
                    lastY = e.getY();
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    int x = e.getX();
                    int y = e.getY();
                    drawLine(lastX, lastY, x, y, color);
                    if (out != null) {
                        out.println("DRAW:LINE:" + lastX + "," + lastY + "," + x + "," + y + ":" + color.getRGB());
                    }
                    lastX = x;
                    lastY = y;
                }
            });
        }

        private void drawLine(int x1, int y1, int x2, int y2, Color c) {
            Graphics2D g = (Graphics2D) getGraphics();
            g.setColor(c);
            g.drawLine(x1, y1, x2, y2);
            g.dispose();
        }

        public void updateDrawing(String msg) {
            try {
                // Expected format: DRAW:LINE:x1,y1,x2,y2:rgb
                String[] parts = msg.split(":");
                if (parts.length >= 4 && parts[0].equals("DRAW")) {
                    String coords = parts[2];
                    String[] c = coords.split(",");
                    int x1 = Integer.parseInt(c[0]);
                    int y1 = Integer.parseInt(c[1]);
                    int x2 = Integer.parseInt(c[2]);
                    int y2 = Integer.parseInt(c[3]);
                    Color col = new Color(Integer.parseInt(parts[3]));
                    drawLine(x1, y1, x2, y2, col);
                }
            } catch (Exception e) {
                // ignore parse errors
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CollaborationClient("127.0.0.1", 5000));
    }
}