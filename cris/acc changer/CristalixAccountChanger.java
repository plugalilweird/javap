package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class CristalixAccountChanger extends JFrame {
    private SavedData savedData;
    private JTextField entryFilePath;
    private JTextField entryProgramPath;
    private JTextField entryTxtPath;
    private JSpinner memoryAmountSpinner;
    private JSpinner renderDistanceSpinner;
    private JSpinner maxFpsSpinner;
    private JPanel accountListPanel;
    private JLabel statusLabel;

    private static final String SAVE_FILE = "saved_data.json";
    private ObjectMapper objectMapper = new ObjectMapper();

    public CristalixAccountChanger() {
        super("Cristalix Account Changer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(750, 650);
        setResizable(true);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        savedData = loadSavedData();

        entryFilePath = new JTextField(savedData.getLastFilePath() != null ? savedData.getLastFilePath() : "");
        entryProgramPath = new JTextField(savedData.getLastProgramPath() != null ? savedData.getLastProgramPath() : "");
        entryTxtPath = new JTextField(savedData.getLastTxtPath() != null ? savedData.getLastTxtPath() : "");

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel titleLabel = new JLabel("Cristalix Account Manager");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        mainPanel.add(titleLabel);

        JButton openSettingsButton = new JButton("⚙ Настройки");
        openSettingsButton.setPreferredSize(new Dimension(150, 35));
        openSettingsButton.addActionListener(e -> openSettingsWindow());
        JPanel settingsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        settingsPanel.add(openSettingsButton);
        mainPanel.add(settingsPanel);

        JPanel gameSettingsPanel = new JPanel(new GridBagLayout());
        gameSettingsPanel.setBorder(BorderFactory.createTitledBorder("Настройки игры"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        gameSettingsPanel.add(new JLabel("Память (MB):"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        memoryAmountSpinner = new JSpinner(new SpinnerNumberModel(savedData.getMemoryAmount() != 0 ? savedData.getMemoryAmount() : 1024, 1024, 8192, 256));
        gameSettingsPanel.add(memoryAmountSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE;
        gameSettingsPanel.add(new JLabel("Дистанция рендера:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        renderDistanceSpinner = new JSpinner(new SpinnerNumberModel(savedData.getRenderDistance() != 0 ? savedData.getRenderDistance() : 8, 0, 32, 1));
        gameSettingsPanel.add(renderDistanceSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE;
        gameSettingsPanel.add(new JLabel("Макс. FPS:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        maxFpsSpinner = new JSpinner(new SpinnerNumberModel(savedData.getMaxFps() != 0 ? savedData.getMaxFps() : 60, 1, 255, 1));
        gameSettingsPanel.add(maxFpsSpinner, gbc);

        mainPanel.add(gameSettingsPanel);

        JPanel buttonPanel1 = new JPanel(new FlowLayout());
        JButton saveTxtSettingsButton = new JButton("💾 Сохранить в options.txt");
        saveTxtSettingsButton.addActionListener(e -> saveTxtSettings());
        buttonPanel1.add(saveTxtSettingsButton);

        JButton saveMemoryButton = new JButton("💾 Сохранить память");
        saveMemoryButton.addActionListener(e -> saveMemoryAmount());
        buttonPanel1.add(saveMemoryButton);

        JButton loadAccountButton = new JButton("📥 Загрузить аккаунт");
        loadAccountButton.addActionListener(e -> onSelectFile());
        buttonPanel1.add(loadAccountButton);

        mainPanel.add(buttonPanel1);

        JPanel buttonPanel2 = new JPanel(new FlowLayout());
        JButton runSelectedButton = new JButton("Запустить выбранные");
        runSelectedButton.setBackground(new Color(76, 175, 80));
        runSelectedButton.setForeground(Color.WHITE);
        runSelectedButton.setPreferredSize(new Dimension(180, 35));
        runSelectedButton.addActionListener(e -> runSelectedAccounts());
        buttonPanel2.add(runSelectedButton);

        JButton runAllButton = new JButton("Запустить все");
        runAllButton.setBackground(new Color(33, 150, 243));
        runAllButton.setForeground(Color.WHITE);
        runAllButton.setPreferredSize(new Dimension(150, 35));
        runAllButton.addActionListener(e -> runAllAccounts());
        buttonPanel2.add(runAllButton);

        mainPanel.add(buttonPanel2);

        statusLabel = new JLabel(" ");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        mainPanel.add(statusLabel);

        accountListPanel = new JPanel();
        accountListPanel.setLayout(new BoxLayout(accountListPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(accountListPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Аккаунты"));
        scrollPane.setPreferredSize(new Dimension(700, 200));
        mainPanel.add(scrollPane);

        add(mainPanel, BorderLayout.CENTER);

        updateAccountList();
    }

    private void openSettingsWindow() {
        JDialog settingsDialog = new JDialog(this, "Настройки путей", true);
        settingsDialog.setSize(600, 300);
        settingsDialog.setLocationRelativeTo(this);
        settingsDialog.setLayout(new BorderLayout());

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);

        // Launcher path
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        contentPanel.add(new JLabel("Путь к .launcher:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        contentPanel.add(entryFilePath, gbc);
        gbc.gridx = 2; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        JButton browseLauncherButton = new JButton("Обзор");
        browseLauncherButton.addActionListener(e -> browseFile(entryFilePath, "JSON files", "json"));
        contentPanel.add(browseLauncherButton, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE;
        contentPanel.add(new JLabel("Путь к программе:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        contentPanel.add(entryProgramPath, gbc);
        gbc.gridx = 2; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        JButton browseProgramButton = new JButton("Обзор");
        browseProgramButton.addActionListener(e -> browseFile(entryProgramPath, "Executable files", "exe"));
        contentPanel.add(browseProgramButton, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE;
        contentPanel.add(new JLabel("Путь к options.txt:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        contentPanel.add(entryTxtPath, gbc);
        gbc.gridx = 2; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        JButton browseTxtButton = new JButton("Обзор");
        browseTxtButton.addActionListener(e -> browseFile(entryTxtPath, "Text files", "txt"));
        contentPanel.add(browseTxtButton, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton saveButton = new JButton("Сохранить");
        saveButton.addActionListener(e -> {
            savedData.setLastFilePath(entryFilePath.getText());
            savedData.setLastProgramPath(entryProgramPath.getText());
            savedData.setLastTxtPath(entryTxtPath.getText());
            savedData.setMemoryAmount((Integer) memoryAmountSpinner.getValue());
            savedData.setRenderDistance((Integer) renderDistanceSpinner.getValue());
            savedData.setMaxFps((Integer) maxFpsSpinner.getValue());
            saveData();
            settingsDialog.dispose();
            showAlert("Успех", "Настройки сохранены.");
        });
        buttonPanel.add(saveButton);

        JButton cancelButton = new JButton("Отмена");
        cancelButton.addActionListener(e -> settingsDialog.dispose());
        buttonPanel.add(cancelButton);

        settingsDialog.add(contentPanel, BorderLayout.CENTER);
        settingsDialog.add(buttonPanel, BorderLayout.SOUTH);
        settingsDialog.setVisible(true);
    }

    private void browseFile(JTextField textField, String description, String extension) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter(description, extension));
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            textField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void onSelectFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON files", "json"));
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String selectedFile = fileChooser.getSelectedFile().getAbsolutePath();
            try {
                JsonNode data = objectMapper.readTree(new File(selectedFile));
                JsonNode accounts = data.path("accounts");
                Iterator<Map.Entry<String, JsonNode>> fields = accounts.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    savedData.getAccounts().put(entry.getKey(), entry.getValue().asText());
                }
                savedData.setLastFilePath(selectedFile);
                saveData();
                updateAccountList();
                showAlert("Успех", "Аккаунты успешно загружены.");
            } catch (IOException e) {
                showAlert("Ошибка", "Не удалось загрузить файл: " + e.getMessage());
            }
        }
    }

    private void saveTxtSettings() {
        String txtPath = entryTxtPath.getText();
        if (txtPath.isEmpty()) {
            showAlert("Ошибка", "Укажите путь к options.txt");
            return;
        }
        try {
            Map<String, String> settings = readTxtFile(txtPath);
            settings.put("renderDistanceChunks", String.valueOf(renderDistanceSpinner.getValue()));
            settings.put("maxFps", String.valueOf(maxFpsSpinner.getValue()));
            writeTxtFile(txtPath, settings);
            savedData.setRenderDistance((Integer) renderDistanceSpinner.getValue());
            savedData.setMaxFps((Integer) maxFpsSpinner.getValue());
            saveData();
            showAlert("Успех", "Настройки сохранены в options.txt");
        } catch (IOException e) {
            showAlert("Ошибка", "Не удалось сохранить настройки: " + e.getMessage());
        }
    }

    private void saveMemoryAmount() {
        savedData.setMemoryAmount((Integer) memoryAmountSpinner.getValue());
        saveData();
        showAlert("Успех", "Настройка памяти сохранена.");
    }

    private void updateAccountList() {
        accountListPanel.removeAll();
        for (Map.Entry<String, String> entry : savedData.getAccounts().entrySet()) {
            String nickname = entry.getKey();
            String token = entry.getValue();

            JPanel itemPanel = new JPanel(new BorderLayout(5, 5));
            itemPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            JCheckBox checkBox = new JCheckBox();
            checkBox.setSelected(false);

            JPanel infoPanel = new JPanel(new GridLayout(2, 1));
            JLabel nameLabel = new JLabel(nickname);
            nameLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            JLabel tokenLabel = new JLabel("Токен: " + token.substring(0, Math.min(token.length(), 20)) + "...");
            tokenLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            infoPanel.add(nameLabel);
            infoPanel.add(tokenLabel);

            JButton deleteButton = new JButton("🗑");
            deleteButton.setPreferredSize(new Dimension(40, 30));
            deleteButton.setBackground(new Color(244, 67, 54));
            deleteButton.setForeground(Color.WHITE);
            deleteButton.setBorder(BorderFactory.createEmptyBorder());
            deleteButton.setToolTipText("Удалить аккаунт");
            deleteButton.addActionListener(e -> deleteAccount(nickname));

            itemPanel.add(checkBox, BorderLayout.WEST);
            itemPanel.add(infoPanel, BorderLayout.CENTER);
            itemPanel.add(deleteButton, BorderLayout.EAST);

            accountListPanel.add(itemPanel);
            accountListPanel.add(Box.createVerticalStrut(5));
        }

        accountListPanel.revalidate();
        accountListPanel.repaint();
    }

    private void deleteAccount(String nickname) {
        savedData.getAccounts().remove(nickname);
        saveData();
        updateAccountList();
        showAlert("Успех", "Аккаунт " + nickname + " успешно удалён.");
    }

    private void runSelectedAccounts() {
        List<Map.Entry<String, String>> selectedAccounts = getSelectedAccounts();
        if (selectedAccounts.isEmpty()) {
            showAlert("Ошибка", "Выберите хотя бы один аккаунт.");
            return;
        }
        runAccounts(selectedAccounts);
    }

    private void runAllAccounts() {
        List<Map.Entry<String, String>> allAccounts = new ArrayList<>(savedData.getAccounts().entrySet());
        if (allAccounts.isEmpty()) {
            showAlert("Ошибка", "Нет доступных аккаунтов для запуска.");
            return;
        }
        runAccounts(allAccounts);
    }

    private List<Map.Entry<String, String>> getSelectedAccounts() {
        List<Map.Entry<String, String>> selected = new ArrayList<>();
        for (Component component : accountListPanel.getComponents()) {
            if (component instanceof JPanel) {
                JPanel itemPanel = (JPanel) component;
                if (itemPanel.getComponentCount() > 0 && itemPanel.getComponent(0) instanceof JCheckBox) {
                    JCheckBox checkBox = (JCheckBox) itemPanel.getComponent(0);
                    if (checkBox.isSelected()) {
                        JPanel infoPanel = (JPanel) itemPanel.getComponent(1);
                        JLabel nameLabel = (JLabel) infoPanel.getComponent(0);
                        String nickname = nameLabel.getText();
                        String token = savedData.getAccounts().get(nickname);
                        selected.add(new AbstractMap.SimpleEntry<>(nickname, token));
                    }
                }
            }
        }
        return selected;
    }

    private void runAccounts(List<Map.Entry<String, String>> accounts) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                String filePath = entryFilePath.getText();
                String programPath = entryProgramPath.getText();

                for (Map.Entry<String, String> account : accounts) {
                    String nickname = account.getKey();
                    String token = account.getValue();

                    JsonNode data = objectMapper.readTree(new File(filePath));
                    ((ObjectNode) data).put("currentAccount", nickname);
                    ((ObjectNode) data.path("accounts")).put(nickname, token);
                    objectMapper.writeValue(new File(filePath), data);

                    ProcessBuilder pb = new ProcessBuilder(programPath);
                    pb.start();

                    Thread.sleep(5000);
                }
                return null;
            }

            @Override
            protected void done() {
                showAlert("Готово", "Запуск аккаунтов завершён.");
            }
        }.execute();
    }

    private Map<String, String> readTxtFile(String filePath) throws IOException {
        Map<String, String> settings = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    settings.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        return settings;
    }

    private void writeTxtFile(String filePath, Map<String, String> settings) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (Map.Entry<String, String> entry : settings.entrySet()) {
                writer.write(entry.getKey() + ":" + entry.getValue());
                writer.newLine();
            }
        }
    }

    private SavedData loadSavedData() {
        try {
            return objectMapper.readValue(new File(SAVE_FILE), SavedData.class);
        } catch (IOException e) {
            return new SavedData();
        }
    }

    private void saveData() {
        try {
            objectMapper.writeValue(new File(SAVE_FILE), savedData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CristalixAccountChanger frame = new CristalixAccountChanger();
            frame.setVisible(true);
        });
    }

    private static class SavedData {
        private Map<String, String> accounts = new HashMap<>();
        private String lastFilePath;
        private String lastProgramPath;
        private String lastTxtPath;
        private int memoryAmount;
        private int renderDistance;
        private int maxFps;

        public Map<String, String> getAccounts() {
            return accounts;
        }

        public void setAccounts(Map<String, String> accounts) {
            this.accounts = accounts;
        }

        public String getLastFilePath() {
            return lastFilePath;
        }

        public void setLastFilePath(String lastFilePath) {
            this.lastFilePath = lastFilePath;
        }

        public String getLastProgramPath() {
            return lastProgramPath;
        }

        public void setLastProgramPath(String lastProgramPath) {
            this.lastProgramPath = lastProgramPath;
        }

        public String getLastTxtPath() {
            return lastTxtPath;
        }

        public void setLastTxtPath(String lastTxtPath) {
            this.lastTxtPath = lastTxtPath;
        }

        public int getMemoryAmount() {
            return memoryAmount;
        }

        public void setMemoryAmount(int memoryAmount) {
            this.memoryAmount = memoryAmount;
        }

        public int getRenderDistance() {
            return renderDistance;
        }

        public void setRenderDistance(int renderDistance) {
            this.renderDistance = renderDistance;
        }

        public int getMaxFps() {
            return maxFps;
        }

        public void setMaxFps(int maxFps) {
            this.maxFps = maxFps;
        }
    }
}