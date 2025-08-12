import java.awt.*;
import java.awt.event.*;
import java.sql.*;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.table.DefaultTableModel;

class LeaderboardFrame extends JFrame {
    public LeaderboardFrame(String currentUserEmail) {
        setTitle("Leaderboard");
        setSize(500, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        String[] columnNames = { "Rank", "Username", "Score" };
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);
        JTable table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);

        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:D:\\Java\\Java-Project\\javaapp.db");

            // Get top 10 users
            String top10Query = "SELECT username, score FROM users ORDER BY score DESC LIMIT 5";
            PreparedStatement ps = conn.prepareStatement(top10Query);
            ResultSet rs = ps.executeQuery();

            int rank = 1;
            // List<String> top10Emails = new ArrayList<>();
            while (rs.next()) {
                String username = rs.getString("username");
                double score = rs.getDouble("score");

                model.addRow(new Object[] { rank, username, score });
                rank++;
            }

            // Check if current user is in top 10
            String rankQuery = "SELECT username, score, " +
                    "(SELECT COUNT(*) + 1 FROM users WHERE score > u.score) AS rank " +
                    "FROM users u WHERE email = ?";
            ps = conn.prepareStatement(rankQuery);
            ps.setString(1, currentUserEmail);
            rs = ps.executeQuery();

            if (rs.next()) {
                int userRank = rs.getInt("rank");
                String username = rs.getString("username");
                double userScore = rs.getDouble("score");

                if (userRank > 5) {
                    // Add a separator and current user's rank
                    model.addRow(new Object[] { "---", "---", "---" });
                    model.addRow(new Object[] { userRank, username, userScore });
                }
            }

            conn.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading leaderboard: " + e.getMessage());
        }

        add(scrollPane, BorderLayout.CENTER);
        setVisible(true);
    }
}

class MemoryCardGame extends JFrame {
    private JPanel mainPanel;
    private String selectedTheme;
    private ArrayList<String> imagePaths;
    private JButton[] cardButtons;
    private ImageIcon hiddenIcon;
    private int firstIndex = -1, secondIndex = -1;
    private Timer timer;
    static int numMoves = 0;
    private int matchedPairs = 0;
    private String userEmail;

    public MemoryCardGame(String userEmail) {
        this.userEmail = userEmail;
        setTitle("Memory Card Game");
        setSize(600, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Load hidden card image
        hiddenIcon = loadImage("Images/hidden.png", 600, 600);

        selectTheme();
    }

    private void selectTheme() {
        String[] themes = { "Cricket", "IPL", "Anime" };
        selectedTheme = (String) JOptionPane.showInputDialog(
                this, "Select a Theme:", "Theme Selection",
                JOptionPane.QUESTION_MESSAGE, null, themes, themes[0]);

        if (selectedTheme != null) {
            loadImages(selectedTheme);
            initializeGame();
        } else {
            System.exit(0);
        }
    }

    private void loadImages(String theme) {
        imagePaths = new ArrayList<>();
        String folderPath = "Images/" + theme;

        File folder = new File(folderPath);
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && (file.getName().endsWith(".png") || file.getName().endsWith(".jpg"))) {
                        // System.out.println("Loaded Image: " + file.getAbsolutePath());
                        imagePaths.add(file.getAbsolutePath());
                    }
                }
            }
        }

        if (imagePaths.size() < 8) {
            JOptionPane.showMessageDialog(this, "Not enough images found in " + theme);
            System.exit(0);
        }

        // Duplicate the images for the memory game
        imagePaths = new ArrayList<>(imagePaths.subList(0, 8));
        imagePaths.addAll(new ArrayList<>(imagePaths));
        Collections.shuffle(imagePaths);
    }

    private void initializeGame() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(4, 4));
        int cardWidth = 100;
        int cardHeight = 100;
        cardButtons = new JButton[imagePaths.size()];

        for (int i = 0; i < imagePaths.size(); i++) {
            cardButtons[i] = new JButton(hiddenIcon);
            cardButtons[i].setActionCommand(String.valueOf(i));
            cardButtons[i].setPreferredSize(new Dimension(cardWidth, cardHeight));
            cardButtons[i].addActionListener(new CardClickListener());
            mainPanel.add(cardButtons[i]);
        }

        add(mainPanel);
        pack();
        setVisible(true);
    }

    private class CardClickListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            int index = Integer.parseInt(e.getActionCommand());

            // Ignore clicks on already matched cards
            if (cardButtons[index].getIcon() != hiddenIcon) {
                return;
            }

            // First card selection
            if (firstIndex == -1) {
                firstIndex = index;
                cardButtons[firstIndex].setIcon(loadImage(imagePaths.get(firstIndex), 100, 100));
            }
            // Second card selection
            else if (secondIndex == -1) {
                secondIndex = index;
                cardButtons[secondIndex].setIcon(loadImage(imagePaths.get(secondIndex), 100, 100));
                numMoves += 2;
                // System.out.println(numMoves);

                // Delay before checking for match
                timer = new Timer(1000, event -> checkMatch());
                timer.setRepeats(false);
                timer.start();
            }
        }
    }

    private void checkMatch() {
        if (imagePaths.get(firstIndex).equals(imagePaths.get(secondIndex))) {
            // Match: Disable cards
            cardButtons[firstIndex].setEnabled(false);
            cardButtons[secondIndex].setEnabled(false);
            matchedPairs++;

            if (matchedPairs == (imagePaths.size() / 2)) {
                showScore();
            }
        } else {
            // No match: Flip cards back
            cardButtons[firstIndex].setIcon(hiddenIcon);
            cardButtons[secondIndex].setIcon(hiddenIcon);
        }

        // Reset selected indices
        firstIndex = -1;
        secondIndex = -1;
    }

    private void showScore() {
        int totalCards = imagePaths.size();
        double score = (totalCards * 1.0 / numMoves) * 100000;
        score = Math.round(score * 10000) / 10000.0;
        System.out.println(numMoves);
        JOptionPane.showMessageDialog(this, "Game Over!\n" + "Your Score: " + String.format("%.4f", score),
                "Game Completed", JOptionPane.INFORMATION_MESSAGE);
        SQLDB.updateScore(userEmail, score);
        this.dispose(); // Close current game frame
        new LeaderboardFrame(userEmail);
    }

    private ImageIcon loadImage(String path, int width, int height) {
        File file = new File(path);
        if (!file.exists()) {
            System.err.println("Image not found: " + path);
            return null;
        }

        ImageIcon icon = new ImageIcon(path);
        Image img = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }

    // public static void main(String[] args) {
    // SwingUtilities.invokeLater(() -> new MemoryCardGame());
    // }
}

class SQLDB {
    public static Connection conn = null;
    public static Statement stmt = null;

    public static void connect(String dbpath) {
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbpath);
            stmt = conn.createStatement();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isUnique(String username, String email) {
        ResultSet rs = null;
        try {
            String query = "SELECT * FROM users WHERE username='" + username + "' OR email='" + email + "';";
            rs = stmt.executeQuery(query);
            return !rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (Exception e) {
            }
        }
    }

    public static boolean insertUser(String username, String email, String password) {
        try {
            String query = "INSERT INTO users (username, email, password, score) VALUES ('" + username + "', '" + email
                    + "', '" + password + "', 0);";
            int rowsAffected = stmt.executeUpdate(query);
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean loginValid(String email, String password) {
        try {
            String query = "SELECT * FROM users WHERE email='" + email + "' AND password='" + password + "';";
            ResultSet rs = stmt.executeQuery(query);
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateScore(String email, double score) {
        if (conn == null)
            return false;
        try {
            conn.close(); // This is unnecessary here; better to check if already open and reuse
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + "D:\\Java\\Java-Project\\javaapp.db");
            stmt = conn.createStatement();

            // Step 1: Get current score
            String selectQuery = "SELECT score FROM users WHERE email = ?";
            PreparedStatement selectStmt = conn.prepareStatement(selectQuery);
            selectStmt.setString(1, email);
            ResultSet rs = selectStmt.executeQuery();

            if (rs.next()) {
                double currentScore = rs.getDouble("score");

                // Step 2: Compare and update if new score is higher
                if (score > currentScore) {
                    String updateQuery = "UPDATE users SET score = ? WHERE email = ?";
                    PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
                    updateStmt.setDouble(1, score);
                    updateStmt.setString(2, email);

                    int rowsUpdated = updateStmt.executeUpdate();
                    return rowsUpdated > 0;
                } else {
                    return false;
                }
            } else {
                System.out.println("User not found with email: " + email);
                return false;
            }
        } catch (SQLException e) {
            System.out.println("Failed to update score: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            return false;
        }
    }

    public static void close() {
        try {
            if (stmt != null)
                stmt.close();
            if (conn != null)
                conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

class SignUp extends Frame implements ActionListener {
    Label l1 = new Label("Username:");
    Label l2 = new Label("Email:");
    Label l3 = new Label("Password:");
    TextField t1 = new TextField();
    TextField t2 = new TextField();
    TextField t3 = new TextField();
    Button signupBtn = new Button("Signup");
    Button loginBtn = new Button("Login");

    SignUp() {
        Font labelFont = new Font("Arial", Font.PLAIN, 14);
        l1.setFont(labelFont);
        l2.setFont(labelFont);
        l3.setFont(labelFont);

        setTitle("Signup");
        setSize(350, 200);
        setLayout(new GridLayout(4, 2, 10, 10));
        setLocationRelativeTo(null);

        add(l1);
        add(t1);
        add(l2);
        add(t2);
        add(l3);
        add(t3);
        add(signupBtn);
        add(loginBtn);

        signupBtn.setPreferredSize(new Dimension(100, 35));
        signupBtn.setBackground(new Color(0, 120, 215));
        signupBtn.setForeground(Color.WHITE);
        signupBtn.setFont(new Font("Arial", Font.BOLD, 14));

        loginBtn.setPreferredSize(new Dimension(100, 35));
        loginBtn.setBackground(new Color(0, 120, 215));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFont(new Font("Arial", Font.BOLD, 14));

        signupBtn.addActionListener(this);
        loginBtn.addActionListener(this);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                SQLDB.close();
                dispose();
            }
        });

        setVisible(true);
    }

    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == signupBtn) {
            SQLDB.connect("D:\\Java\\Java-Project\\javaapp.db");
            String username = t1.getText().trim();
            String email = t2.getText().trim();
            String password = t3.getText().trim();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                showErrorDialog("Please fill all fields");
                return;
            }

            if (SQLDB.isUnique(username, email)) {
                boolean inserted = SQLDB.insertUser(username, email, password);
                if (inserted) {
                    showSuccessDialog(); // Show message and wait to launch game
                } else {
                    showErrorDialog("Failed to create account");
                }
            } else {
                showErrorDialog("Username or Email already exists");
            }
        } else if (ae.getSource() == loginBtn) {
            SQLDB.close();
            this.dispose();
            new Login();
        }
    }

    // Dialog without OK button for success
    private void showSuccessDialog() {
        Dialog d = new Dialog(this, "Success", true);
        d.setLayout(new FlowLayout());
        d.setSize(300, 100);
        d.setLocationRelativeTo(this);
        d.add(new Label("Signup successful! Close this window to continue."));

        d.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                d.dispose();
                dispose(); // Close signup window
                SQLDB.close();
                new MemoryCardGame(t2.getText().trim()); // Launch game
            }
        });

        d.setVisible(true);
    }

    // For error messages (with OK button)
    private void showErrorDialog(String msg) {
        Dialog d = new Dialog(this, "Error", true);
        d.setLayout(new FlowLayout());
        d.setSize(250, 100);
        d.setLocationRelativeTo(this);

        d.add(new Label(msg));
        d.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                d.dispose();
            }
        });

        d.setVisible(true);
    }
}

class Login extends Frame implements ActionListener {
    Label l1 = new Label("Email:");
    Label l2 = new Label("Password:");
    TextField t1 = new TextField();
    TextField t2 = new TextField();
    Button loginBtn = new Button("Login");
    Button signupBtn = new Button("Signup");

    Login() {
        setTitle("Login");
        setSize(350, 170);
        setLayout(new GridLayout(3, 2, 10, 10));
        setLocationRelativeTo(null);

        add(l1);
        add(t1);
        add(l2);
        add(t2);
        add(loginBtn);
        add(signupBtn);

        loginBtn.setBackground(new Color(0, 120, 215));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFont(new Font("Arial", Font.BOLD, 14));

        signupBtn.setBackground(new Color(0, 120, 215));
        signupBtn.setForeground(Color.WHITE);
        signupBtn.setFont(new Font("Arial", Font.BOLD, 14));

        loginBtn.addActionListener(this);
        signupBtn.addActionListener(this);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                SQLDB.close();
                dispose();
            }
        });

        setVisible(true);
    }

    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == loginBtn) {
            SQLDB.connect("D:\\Java\\Java-Project\\javaapp.db");
            String email = t1.getText().trim();
            String password = t2.getText().trim();

            if (email.isEmpty() || password.isEmpty()) {
                showErrorDialog("Please enter both fields");
                return;
            }

            if (SQLDB.loginValid(email, password)) {
                showSuccessDialog(); // Start game only after closing dialog
            } else {
                showErrorDialog("Invalid Email or Password");
            }
        } else if (ae.getSource() == signupBtn) {
            SQLDB.close();
            this.dispose();
            new SignUp();
        }
    }

    // Success dialog without OK button
    private void showSuccessDialog() {
        Dialog d = new Dialog(this, "Success", true);
        d.setLayout(new FlowLayout());
        d.setSize(350, 100);
        d.setLocationRelativeTo(this);
        d.add(new Label("Login successful! Close this window to continue."));

        d.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                d.dispose(); // Close dialog
                dispose(); // Close login frame
                String query = "SELECT username FROM users WHERE email = " + t1.getText().trim();

                new MemoryCardGame(t1.getText().trim()); // Start the game
            }
        });

        d.setVisible(true);
    }

    // Dialog for error messages (with OK button)
    private void showErrorDialog(String msg) {
        Dialog d = new Dialog(this, "Error", true);
        d.setLayout(new FlowLayout());
        d.setSize(250, 100);
        d.setLocationRelativeTo(this);

        d.add(new Label(msg));
        d.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                d.dispose();
            }
        });
        d.setVisible(true);
    }
}

public class MainApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SignUp());
    }
}
