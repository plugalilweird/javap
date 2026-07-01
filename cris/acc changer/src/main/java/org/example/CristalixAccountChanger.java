package org.example;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.awt.Desktop;
import java.io.*;
import java.util.*;
import java.util.List;

public class CristalixAccountChanger extends Application {
    private SavedData savedData;
    private TextField entryFilePath;
    private TextField entryProgramPath;
    private TextField entryTxtPath;
    private Spinner<Integer> memoryAmountSpinner;
    private Spinner<Integer> renderDistanceSpinner;
    private Spinner<Integer> maxFpsSpinner;
    private VBox accountListVBox;
    private Label statusLabel;

    private static final String SAVE_FILE = "saved_data.json";
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void start(Stage primaryStage) {
        savedData = loadSavedData();

        primaryStage.setTitle("CristalixAccountChanger");
        primaryStage.setScene(createScene());
        primaryStage.setResizable(false);
        primaryStage.show();

        updateAccountList();

        // Загрузка сохранённых путей
        if (savedData.getLastFilePath() != null) entryFilePath.setText(savedData.getLastFilePath());
        if (savedData.getLastProgramPath() != null) entryProgramPath.setText(savedData.getLastProgramPath());
        if (savedData.getLastTxtPath() != null) entryTxtPath.setText(savedData.getLastTxtPath());
    }

    private Scene createScene() {
        VBox mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(10));

        // Кнопка настроек
        Button openSettingsButton = new Button("Открыть настройки");
        openSettingsButton.setOnAction(e -> openSettingsWindow());

        // Спиннеры для настроек
        Label memoryLabel = new Label("Укажите количество памяти (MB):");
        memoryAmountSpinner = new Spinner<>(1024, 8192, 1024, 256);
        Label renderDistanceLabel = new Label("Укажите чанки:");
        renderDistanceSpinner = new Spinner<>(0, 32, 0, 1);
        Label maxFpsLabel = new Label("Укажите ФПС:");
        maxFpsSpinner = new Spinner<>(1, 255, 1, 1);

        // Кнопки управления
        Button saveTxtSettingsButton = new Button("Сохранить настройки в options");
        saveTxtSettingsButton.setOnAction(e -> saveTxtSettings());
        Button saveMemoryButton = new Button("Сохранить память");
        saveMemoryButton.setOnAction(e -> saveMemoryAmount());
        Button loadAccountButton = new Button("Загрузить аккаунт");
        loadAccountButton.setOnAction(e -> onSelectFile());

        HBox buttonLayout1 = new HBox(10, saveTxtSettingsButton, saveMemoryButton, loadAccountButton);

        Button runSelectedButton = new Button("Запустить выбранные аккаунты");
        runSelectedButton.setOnAction(e -> runSelectedAccounts());
        Button runAllButton = new Button("Запустить все аккаунты");
        runAllButton.setOnAction(e -> runAllAccounts());

        HBox buttonLayout2 = new HBox(10, runSelectedButton, runAllButton);

        // Статус
        statusLabel = new Label("");

        // Список аккаунтов
        accountListVBox = new VBox(5);
        ScrollPane scrollPane = new ScrollPane(accountListVBox);
        scrollPane.setFitToWidth(true);

        // Нижний колонтитул
        Label footerLabel = new Label("Coded by: iJuspBentley");
        footerLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 12px; -fx-font-weight: bold;");
        HBox footerLayout = new HBox();
        footerLayout.getChildren().addAll(new Region(), footerLabel);
        HBox.setHgrow(footerLayout.getChildren().get(0), Priority.ALWAYS);

        mainLayout.getChildren().addAll(
                openSettingsButton,
                memoryLabel, memoryAmountSpinner,
                renderDistanceLabel, renderDistanceSpinner,
                maxFpsLabel, maxFpsSpinner,
                buttonLayout1,
                buttonLayout2,
                statusLabel,
                scrollPane,
                footerLayout
        );

        return new Scene(mainLayout, 600, 600);
    }

    private void openSettingsWindow() {
        Stage settingsStage = new Stage();
        settingsStage.setTitle("Настройки");

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));

        // Путь к .launcher
        Label filePathLabel = new Label("Введите путь к файлу .launcher:");
        entryFilePath = new TextField(savedData.getLastFilePath());
        Button browseFileButton = new Button("Обзор");
        browseFileButton.setOnAction(e -> browseFile());
        HBox filePathLayout = new HBox(10, entryFilePath, browseFileButton);

        // Путь к Cristalix
        Label programPathLabel = new Label("Введите путь к Cristalix:");
        entryProgramPath = new TextField(savedData.getLastProgramPath());
        Button browseProgramButton = new Button("Обзор");
        browseProgramButton.setOnAction(e -> browseProgram());
        HBox programPathLayout = new HBox(10, entryProgramPath, browseProgramButton);

        // Путь к options.txt
        Label txtPathLabel = new Label("Введите путь к options.txt (опционально):");
        entryTxtPath = new TextField(savedData.getLastTxtPath());
        Button browseTxtButton = new Button("Обзор");
        browseTxtButton.setOnAction(e -> browseTxtFile());
        HBox txtPathLayout = new HBox(10, entryTxtPath, browseTxtButton);

        Button closeButton = new Button("Закрыть");
        closeButton.setOnAction(e -> settingsStage.close());

        layout.getChildren().addAll(
                filePathLabel, filePathLayout,
                programPathLabel, programPathLayout,
                txtPathLabel, txtPathLayout,
                closeButton
        );

        settingsStage.setScene(new Scene(layout, 400, 200));
        settingsStage.show();
    }

    private void browseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите JSON файл");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.launcher"));
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            entryFilePath.setText(file.getAbsolutePath());
            savedData.setLastFilePath(file.getAbsolutePath());
            saveData();
        }
    }

    private void browseProgram() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите программу");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Executables", "*.exe"));
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            entryProgramPath.setText(file.getAbsolutePath());
            savedData.setLastProgramPath(file.getAbsolutePath());
            saveData();
        }
    }

    private void browseTxtFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите TXT файл");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            entryTxtPath.setText(file.getAbsolutePath());
            savedData.setLastTxtPath(file.getAbsolutePath());
            saveData();
        }
    }

    private void saveTxtSettings() {
        String txtPath = entryTxtPath.getText();
        if (txtPath.isEmpty()) {
            showAlert(
                    Alert.AlertType.WARNING,
                    "Путь не указан",
                    "Введите путь к .txt файлу."
            );
            return;
        }

        try {
            Map<String, String> settings = readTxtFile(txtPath);
            settings.put("maxFps", String.valueOf(maxFpsSpinner.getValue()));
            settings.put("renderDistance", String.valueOf(renderDistanceSpinner.getValue()));
            writeTxtFile(txtPath, settings);
            showAlert(Alert.AlertType.INFORMATION, "Успех", "Настройки успешно сохранены в options.txt файл.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.WARNING, "Ошибка", "Не удалось сохранить настройки: " + e.getMessage());
        }
    }

    private void saveMemoryAmount() {
        String filePath = entryFilePath.getText();
        if (filePath.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Ошибка", "Введите путь к .launcher файлу.");
            return;
        }

        try {
            JsonNode data = objectMapper.readTree(new File(filePath));
            ((ObjectNode) data).put("memoryAmount", memoryAmountSpinner.getValue());
            objectMapper.writeValue(new File(filePath), data);
            showAlert(Alert.AlertType.INFORMATION, "Успех", "Память успешно обновлена.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.WARNING, "Ошибка", "Не удалось сохранить изменения: " + e.getMessage());
        }
    }

    private void onSelectFile() {
        String filePath = entryFilePath.getText();
        if (filePath.isEmpty()) {
            statusLabel.setText("Введите путь к файлу.");
            return;
        }

        try {
            JsonNode data = objectMapper.readTree(new File(filePath));
            String currentAccount = data.path("currentAccount").asText();
            String token = data.path("accounts").path(currentAccount).asText();

            if (!currentAccount.isEmpty() && !token.isEmpty()) {
                savedData.getAccounts().put(currentAccount, token);
                saveData();
                updateAccountList();
                statusLabel.setText("Данные успешно загружены.");
            } else {
                statusLabel.setText("Не удалось найти никнейм или токен.");
            }
        } catch (Exception e) {
            statusLabel.setText("Ошибка при чтении файла: " + e.getMessage());
        }
    }

    private void updateAccountList() {
        accountListVBox.getChildren().clear();
        for (Map.Entry<String, String> entry : savedData.getAccounts().entrySet()) {
            String nickname = entry.getKey();
            String token = entry.getValue();
            String hiddenToken = token.length() > 5 ? token.substring(0, 5) + "..." : token;

            HBox itemLayout = new HBox(10);
            CheckBox checkBox = new CheckBox(nickname);
            Label label = new Label(hiddenToken);
            Button deleteButton = new Button("Удалить");
            deleteButton.setOnAction(e -> deleteAccount(nickname));

            itemLayout.getChildren().addAll(checkBox, label, deleteButton);
            accountListVBox.getChildren().add(itemLayout);
        }
    }

    private void deleteAccount(String nickname) {
        savedData.getAccounts().remove(nickname);
        saveData();
        updateAccountList();
        showAlert(Alert.AlertType.INFORMATION, "Успех", "Аккаунт " + nickname + " успешно удалён.");
    }

    private void runSelectedAccounts() {
        List<Map.Entry<String, String>> selectedAccounts = getSelectedAccounts();
        if (selectedAccounts.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Ошибка", "Выберите хотя бы один аккаунт.");
            return;
        }
        runAccounts(selectedAccounts);
    }

    private void runAllAccounts() {
        List<Map.Entry<String, String>> allAccounts = new ArrayList<>(savedData.getAccounts().entrySet());
        if (allAccounts.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Ошибка", "Нет доступных аккаунтов для запуска.");
            return;
        }
        runAccounts(allAccounts);
    }

    private List<Map.Entry<String, String>> getSelectedAccounts() {
        List<Map.Entry<String, String>> selected = new ArrayList<>();
        for (Node node : accountListVBox.getChildren()) {
            HBox item = (HBox) node;
            CheckBox checkBox = (CheckBox) item.getChildren().get(0);
            if (checkBox.isSelected()) {
                String nickname = checkBox.getText();
                String token = savedData.getAccounts().get(nickname);
                selected.add(new AbstractMap.SimpleEntry<>(nickname, token));
            }
        }
        return selected;
    }

    private void runAccounts(List<Map.Entry<String, String>> accounts) {
        String filePath = entryFilePath.getText();
        String programPath = entryProgramPath.getText();

        if (filePath.isEmpty() || programPath.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Ошибка", "Убедитесь, что указаны пути к .launcher и Cristalix.");
            return;
        }

        for (Map.Entry<String, String> account : accounts) {
            String nickname = account.getKey();
            String token = account.getValue();

            try {
                JsonNode data = objectMapper.readTree(new File(filePath));
                ((ObjectNode) data).put("currentAccount", nickname);
                ((ObjectNode) data.path("accounts")).put(nickname, token);
                objectMapper.writeValue(new File(filePath), data);

                ProcessBuilder pb = new ProcessBuilder(programPath);
                pb.start();

                Thread.sleep(5000);
            } catch (Exception e) {
                showAlert(Alert.AlertType.WARNING, "Ошибка", "Не удалось запустить Cristalix для аккаунта " + nickname + ": " + e.getMessage());
            }
        }

        showAlert(Alert.AlertType.INFORMATION, "Готово", "Запуск аккаунтов завершён.");
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

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }

    // Внутренний класс для хранения данных
    private static class SavedData {
        private Map<String, String> accounts = new HashMap<>();
        private String lastFilePath;
        private String lastProgramPath;
        private String lastTxtPath;

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
    }
}
