package org.example;

import java.awt.event.KeyEvent;
import java.awt.Robot;
import java.awt.AWTException;
import java.awt.image.BufferedImage;
import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlayerTracker {
    private static String LOG_DIR;
    private static final String LOG_FILE = "latest.log";
    private static String PLAYER_NAME;
    private static int BIND_FIND_KEY;
    private static int BIND_REPORT_KEY;
    private static int START_KEY = KeyEvent.VK_NUMPAD1; // Изменено на более простую клавишу (NumPad 1 для старта)
    private static int STOP_KEY = KeyEvent.VK_NUMPAD2;  // Изменено на более простую клавишу (NumPad 2 для стопа)

    private static SystemTray systemTray;
    private static TrayIcon trayIcon;
    private static volatile boolean isRunning = false;
    private static volatile boolean isTracking = false;
    private static JFrame mainFrame;

    private static String lastRealm = "";
    private static Robot robot;

    private static final Path CONFIG_PATH = Paths.get(System.getProperty("user.home"), ".playertracker.properties");
    private static final Path LOCK_FILE = Paths.get(System.getProperty("user.home"), ".playertracker.lock");
    private static final Properties props = new Properties();

    public static void main(String[] args) {
        if (!tryCreateLock()) {
            JOptionPane.showMessageDialog(null, "PlayerTracker уже запущен!", "Ошибка", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        setupLookAndFeel();
        createMainFrame();
        loadSettings();

        try {
            robot = new Robot();
        } catch (AWTException e) {
            JOptionPane.showMessageDialog(null, "Не удалось инициализировать Robot для имитации клавиш", "Ошибка", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // Инициализация JNativeHook для глобальных горячих клавиш
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.WARNING);
        logger.setUseParentHandlers(false);

        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException ex) {
            JOptionPane.showMessageDialog(null, "Не удалось зарегистрировать глобальные клавиши: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        // Добавляем слушатель клавиш
        GlobalScreen.addNativeKeyListener(new NativeKeyListener() {
            @Override
            public void nativeKeyPressed(NativeKeyEvent e) {
                if (e.getKeyCode() == NativeKeyEvent.VC_F7) { // Соответствует START_KEY
                    startTracking();
                } else if (e.getKeyCode() == NativeKeyEvent.VC_F8) { // Соответствует STOP_KEY
                    stopTracking();
                }
            }

            @Override
            public void nativeKeyReleased(NativeKeyEvent e) {
                // Не используется
            }

            @Override
            public void nativeKeyTyped(NativeKeyEvent e) {
                // Не используется
            }
        });

        if (showConfigDialog()) {
            setupSystemTray();
            startMonitoring();
            hideMainFrame();
        } else {
            deleteLock();
            System.exit(0);
        }
    }

    private static void setupLookAndFeel() {
        try {
            FlatDarkLaf.setup();
            UIManager.put("Component.arc", 15);
            UIManager.put("Button.background", Color.WHITE);
            UIManager.put("Button.foreground", Color.BLACK);
            UIManager.put("TextField.background", Color.WHITE);
            UIManager.put("TextField.foreground", Color.BLACK);
            UIManager.put("TextArea.background", Color.WHITE);
            UIManager.put("TextArea.foreground", Color.BLACK);
            UIManager.put("Panel.background", new Color(34, 34, 34));
            UIManager.put("Label.foreground", Color.WHITE);
            UIManager.put("ComboBox.background", Color.WHITE);
            UIManager.put("ComboBox.foreground", Color.BLACK);
        } catch (Exception ignored) {
        }
    }

    private static void createMainFrame() {
        mainFrame = new JFrame("Player Tracker");
        mainFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        mainFrame.setSize(1, 1);
        Image icon = loadIcon();
        if (icon != null) {
            mainFrame.setIconImage(icon);
            try {
                Taskbar taskbar = Taskbar.getTaskbar();
                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                    taskbar.setIconImage(icon);
                }
            } catch (Exception ignored) {
            }
        }
        mainFrame.setLocation(-100, -100);
        mainFrame.setVisible(true);
        mainFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                mainFrame.setVisible(false);
            }
        });
    }

    private static void hideMainFrame() {
        if (mainFrame != null) {
            mainFrame.setVisible(false);
        }
    }

    private static Image loadIcon() {
        try {
            InputStream iconStream = PlayerTracker.class.getResourceAsStream("/app-icon.png");
            if (iconStream == null) {
                iconStream = PlayerTracker.class.getResourceAsStream("/icon.png");
            }
            if (iconStream != null) {
                return Toolkit.getDefaultToolkit().createImage(iconStream.readAllBytes());
            }
        } catch (Exception ignored) {
        }
        return createDefaultIcon();
    }

    private static Image createDefaultIcon() {
        BufferedImage icon = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = icon.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(new Color(50, 50, 50));
        g2d.fillRect(0, 0, 32, 32);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        FontMetrics fm = g2d.getFontMetrics();
        String text = "T";
        int x = (32 - fm.stringWidth(text)) / 2;
        int y = (32 - fm.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(text, x, y);
        g2d.dispose();
        return icon;
    }

    private static void setupSystemTray() {
        if (!SystemTray.isSupported()) {
            return;
        }
        systemTray = SystemTray.getSystemTray();
        Image trayImage = loadIcon();
        PopupMenu popup = new PopupMenu();

        MenuItem statusItem = new MenuItem("PlayerTracker - Готов");
        statusItem.setEnabled(false);
        popup.add(statusItem);
        popup.addSeparator();

        MenuItem configItem = new MenuItem("Настройки");
        configItem.addActionListener(e -> showConfigDialog());
        popup.add(configItem);

        MenuItem exitItem = new MenuItem("Выход");
        exitItem.addActionListener(e -> {
            isRunning = false;
            isTracking = false;
            if (systemTray != null && trayIcon != null) {
                systemTray.remove(trayIcon);
            }
            if (mainFrame != null) {
                mainFrame.dispose();
            }
            try {
                GlobalScreen.unregisterNativeHook();
            } catch (NativeHookException ignored) {
            }
            deleteLock();
            System.exit(0);
        });
        popup.add(exitItem);

        trayIcon = new TrayIcon(trayImage, "PlayerTracker", popup);
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(e -> showConfigDialog());

        try {
            systemTray.add(trayIcon);
        } catch (AWTException ignored) {
        }
    }

    private static void loadSettings() {
        if (Files.exists(CONFIG_PATH)) {
            try (FileInputStream in = new FileInputStream(CONFIG_PATH.toFile())) {
                props.load(in);
            } catch (IOException ignored) {
            }
        }
        LOG_DIR = props.getProperty("log.dir", "");
        PLAYER_NAME = props.getProperty("player.name", "");
        BIND_FIND_KEY = Integer.parseInt(props.getProperty("bind.find.key", String.valueOf(KeyEvent.VK_F1)));
        BIND_REPORT_KEY = Integer.parseInt(props.getProperty("bind.report.key", String.valueOf(KeyEvent.VK_F2)));
    }

    private static void saveSettings() {
        props.setProperty("log.dir", LOG_DIR);
        props.setProperty("player.name", PLAYER_NAME);
        props.setProperty("bind.find.key", String.valueOf(BIND_FIND_KEY));
        props.setProperty("bind.report.key", String.valueOf(BIND_REPORT_KEY));
        try (FileOutputStream out = new FileOutputStream(CONFIG_PATH.toFile())) {
            props.store(out, "PlayerTracker Configuration");
        } catch (IOException ignored) {
        }
    }

    private static boolean showConfigDialog() {
        JDialog dialog = new JDialog(mainFrame, "Настройки PlayerTracker", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new GridBagLayout());
        dialog.getContentPane().setBackground(new Color(34, 34, 34));
        Image icon = loadIcon();
        if (icon != null) {
            dialog.setIconImage(icon);
        }
        final boolean[] result = {false};
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        JTextField logDirField = createField(LOG_DIR, 30);
        JTextField playerNameField = createField(PLAYER_NAME, 20);

        // Комбобоксы для выбора клавиш
        JComboBox<KeyItem> findKeyCombo = createKeyComboBox(BIND_FIND_KEY);
        JComboBox<KeyItem> reportKeyCombo = createKeyComboBox(BIND_REPORT_KEY);

        addLabel(dialog, "Путь к логам:", 0, 0, gbc);
        addComponent(dialog, logDirField, 1, 0, gbc);
        addLabel(dialog, "Ник игрока:", 0, 1, gbc);
        addComponent(dialog, playerNameField, 1, 1, gbc);
        addLabel(dialog, "Bind Find (клавиша для поиска):", 0, 2, gbc);
        addComponent(dialog, findKeyCombo, 1, 2, gbc);
        addLabel(dialog, "Bind Report (клавиша отчета):", 0, 3, gbc);
        addComponent(dialog, reportKeyCombo, 1, 3, gbc);

        JLabel infoLabel = new JLabel("<html>NumPad1 - запуск трекинга<br>NumPad2 - остановка трекинга<br>Скрипт будет отслеживать перемещение с HUB-*/ARCL-* на другие сервера</html>"); // Обновлено для новых клавиш
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.ITALIC, 11f));
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        dialog.add(infoLabel, gbc);

        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(new Color(34, 34, 34));
        btnPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        JButton ok = createButton("Сохранить");
        JButton cancel = createButton("Отмена");

        ok.addActionListener(e -> {
            LOG_DIR = logDirField.getText().trim();
            PLAYER_NAME = playerNameField.getText().trim();
            BIND_FIND_KEY = ((KeyItem) findKeyCombo.getSelectedItem()).keyCode;
            BIND_REPORT_KEY = ((KeyItem) reportKeyCombo.getSelectedItem()).keyCode;

            if (LOG_DIR.isEmpty() || PLAYER_NAME.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Пожалуйста, заполните все обязательные поля!", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            saveSettings();
            result[0] = true;
            dialog.dispose();
        });

        cancel.addActionListener(e -> {
            if (!isRunning) {
                System.exit(0);
            }
            dialog.dispose();
        });

        btnPanel.add(ok);
        btnPanel.add(cancel);
        gbc.gridy = 5;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        dialog.add(btnPanel, gbc);

        dialog.pack();
        dialog.setLocationRelativeTo(mainFrame);
        dialog.setVisible(true);
        return result[0] && !LOG_DIR.isEmpty() && !PLAYER_NAME.isEmpty();
    }

    private static JComboBox<KeyItem> createKeyComboBox(int selectedKey) {
        KeyItem[] keys = {
                new KeyItem("F1", KeyEvent.VK_F1),
                new KeyItem("F2", KeyEvent.VK_F2),
                new KeyItem("F3", KeyEvent.VK_F3),
                new KeyItem("F4", KeyEvent.VK_F4),
                new KeyItem("F5", KeyEvent.VK_F5),
                new KeyItem("F6", KeyEvent.VK_F6),
                new KeyItem("F9", KeyEvent.VK_F9),
                new KeyItem("F10", KeyEvent.VK_F10),
                new KeyItem("F11", KeyEvent.VK_F11),
                new KeyItem("F12", KeyEvent.VK_F12),
                new KeyItem("1", KeyEvent.VK_1),
                new KeyItem("2", KeyEvent.VK_2),
                new KeyItem("3", KeyEvent.VK_3),
                new KeyItem("4", KeyEvent.VK_4),
                new KeyItem("5", KeyEvent.VK_5),
                new KeyItem("6", KeyEvent.VK_6),
                new KeyItem("7", KeyEvent.VK_7),
                new KeyItem("8", KeyEvent.VK_8),
                new KeyItem("9", KeyEvent.VK_9),
                new KeyItem("0", KeyEvent.VK_0)
        };

        JComboBox<KeyItem> combo = new JComboBox<>(keys);

        // Выбираем текущую клавишу
        for (KeyItem key : keys) {
            if (key.keyCode == selectedKey) {
                combo.setSelectedItem(key);
                break;
            }
        }

        return combo;
    }

    private static class KeyItem {
        String name;
        int keyCode;

        KeyItem(String name, int keyCode) {
            this.name = name;
            this.keyCode = keyCode;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static JTextField createField(String text, int cols) {
        JTextField field = new JTextField(text, cols);
        field.setBorder(new EmptyBorder(6, 10, 6, 10));
        return field;
    }

    private static JButton createButton(String text) {
        JButton btn = new JButton(text);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(6, 16, 6, 16));
        return btn;
    }

    private static void addLabel(JDialog d, String text, int x, int y, GridBagConstraints gbc) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(14f));
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        d.add(lbl, gbc);
    }

    private static void addComponent(JDialog d, Component comp, int x, int y, GridBagConstraints gbc) {
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        d.add(comp, gbc);
        gbc.weightx = 0;
    }

    private static void startMonitoring() {
        isRunning = true;

        // Поток для мониторинга лога
        new Thread(() -> monitorLog()).start();

        // Поток для глобальных горячих клавиш теперь обрабатывается JNativeHook, так что отдельный поток не нужен

        System.out.println("PlayerTracker запущен. NumPad1 - старт, NumPad2 - стоп");
    }

    private static void startTracking() {
        if (isTracking) return; // Избежать повторного запуска
        isTracking = true;
        lastRealm = "";
        System.out.println("Трекинг начат для игрока: " + PLAYER_NAME);

        new Thread(() -> {
            while (isTracking) {
                try {
                    // Имитируем нажатие клавиши Find
                    pressKey(BIND_FIND_KEY);
                    Thread.sleep(60); // Изменено на 60 мс
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    private static void stopTracking() {
        isTracking = false;
        System.out.println("Трекинг остановлен");
    }

    private static void pressKey(int keyCode) {
        try {
            robot.keyPress(keyCode);
            Thread.sleep(50);
            robot.keyRelease(keyCode);
        } catch (Exception e) {
            System.err.println("Ошибка при имитации нажатия клавиши: " + e.getMessage());
        }
    }

    private static void monitorLog() {
        Path logPath = Paths.get(LOG_DIR, LOG_FILE);

        if (!Files.exists(logPath)) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null,
                        "Файл лога не найден: " + logPath.toString(),
                        "Ошибка",
                        JOptionPane.ERROR_MESSAGE);
            });
            return;
        }

        long lastLen = logPath.toFile().length();

        try (var raf = new java.io.RandomAccessFile(logPath.toFile(), "r")) {
            raf.seek(lastLen);

            while (isRunning) {
                long len = logPath.toFile().length();
                if (len < lastLen) {
                    raf.seek(0);
                    lastLen = 0;
                } else if (len > lastLen) {
                    byte[] bytes = new byte[(int) (len - lastLen)];
                    raf.readFully(bytes);
                    String content = new String(bytes, StandardCharsets.UTF_8);
                    String[] lines = content.split("\n");

                    for (String line : lines) {
                        if (!line.trim().isEmpty()) {
                            processLine(line.trim());
                        }
                    }
                    lastLen = raf.getFilePointer();
                }

                Thread.sleep(1000);
            }
        } catch (Exception e) {
            System.err.println("Ошибка при мониторинге лога:");
            e.printStackTrace();
        }
    }

    private static void processLine(String line) {
        if (!isTracking) return;

        try {
            // Паттерн для поиска информации о местоположении игрока
            // С привилегией: [19:48:22] [Client thread/INFO] [CHAT] Игрок H ┃ loftik находится на сервере ANRC-1
            // Без привилегии: [19:48:22] [Client thread/INFO] [CHAT] Игрок loftik находится на сервере ANRC-1
            String pattern = "\\[CHAT\\] Игрок (?:.* ┃ )?" + Pattern.quote(PLAYER_NAME) + " находится на сервере (.+)";
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(line);

            if (m.find()) {
                String currentRealm = m.group(1).trim();

                if (lastRealm.isEmpty()) {
                    lastRealm = currentRealm;
                    System.out.println("Текущий реалм игрока " + PLAYER_NAME + ": " + currentRealm);
                } else if (!lastRealm.equals(currentRealm)) {
                    System.out.println("Игрок " + PLAYER_NAME + " переместился с " + lastRealm + " на " + currentRealm);

                    // Проверяем, был ли игрок на HUB-* или ARCL-* и переместился на другой сервер
                    if ((lastRealm.startsWith("HUB-") || lastRealm.startsWith("ARCL-")) &&
                            !currentRealm.startsWith("HUB-") && !currentRealm.startsWith("ARCL-")) {

                        System.out.println("Обнаружен переход с " + lastRealm + " на " + currentRealm + ". Выполняем действие.");

                        // Имитируем нажатие клавиши Report
                        pressKey(BIND_REPORT_KEY);

                        // Останавливаем трекинг
                        stopTracking();
                    }

                    lastRealm = currentRealm;
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка при обработке строки: " + line);
            e.printStackTrace();
        }
    }

    // Методы для работы с блокировкой приложения
    private static boolean tryCreateLock() {
        try {
            if (Files.exists(LOCK_FILE)) {
                String pid = Files.readString(LOCK_FILE).trim();
                if (isProcessRunning(pid)) {
                    return false;
                } else {
                    Files.delete(LOCK_FILE);
                }
            }
            String currentPid = String.valueOf(ProcessHandle.current().pid());
            Files.write(LOCK_FILE, currentPid.getBytes());
            Runtime.getRuntime().addShutdownHook(new Thread(() -> deleteLock()));
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isProcessRunning(String pid) {
        try {
            long pidLong = Long.parseLong(pid);
            return ProcessHandle.of(pidLong).isPresent() && ProcessHandle.of(pidLong).get().isAlive();
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void deleteLock() {
        try {
            Files.deleteIfExists(LOCK_FILE);
        } catch (Exception ignored) {
        }
    }
}