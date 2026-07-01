package org.example;

import java.awt.image.BufferedImage;
import com.formdev.flatlaf.FlatDarkLaf;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
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
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class LogReader {
    private static String LOG_DIR;
    private static final String LOG_FILE = "latest.log";
    private static String BOT_TOKEN;
    private static String CHAT_ID;
    private static String REGEX_PATTERN;
    private static String TELEGRAM_TEMPLATE;
    private static String CLIPBOARD_TEMPLATE;

    private static SystemTray systemTray;
    private static TrayIcon trayIcon;
    private static volatile boolean isRunning = false;
    private static JFrame mainFrame;
    private static MsgBot bot;
    private static TelegramBotsApi botsApi;

    private static final Path CONFIG_PATH = Paths.get(System.getProperty("user.home"), ".logreader.properties");
    private static final Path LOCK_FILE = Paths.get(System.getProperty("user.home"), ".logreader.lock");
    private static final Properties props = new Properties();

    public static void main(String[] args) {
        if (!tryCreateLock()) {
            JOptionPane.showMessageDialog(null, "LogReader уже запущен!", "Ошибка", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        setupLookAndFeel();
        createMainFrame();
        loadSettings();

        if (showConfigDialog()) {
            setupSystemTray();
            startBot();
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
        } catch (Exception ignored) {
        }
    }

    private static void createMainFrame() {
        mainFrame = new JFrame("LogReader");
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
            InputStream iconStream = LogReader.class.getResourceAsStream("/app-icon.png");
            if (iconStream == null) {
                iconStream = LogReader.class.getResourceAsStream("/icon.png");
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
        String text = "L";
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

        MenuItem statusItem = new MenuItem("LogReader - Работает");
        statusItem.setEnabled(false);
        popup.add(statusItem);
        popup.addSeparator();

        MenuItem configItem = new MenuItem("Настройки");
        configItem.addActionListener(e -> showConfigDialog());
        popup.add(configItem);

        MenuItem exitItem = new MenuItem("Выход");
        exitItem.addActionListener(e -> {
            isRunning = false;
            if (systemTray != null && trayIcon != null) {
                systemTray.remove(trayIcon);
            }
            if (mainFrame != null) {
                mainFrame.dispose();
            }
            deleteLock();
            System.exit(0);
        });
        popup.add(exitItem);

        trayIcon = new TrayIcon(trayImage, "LogReader", popup);
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
        BOT_TOKEN = props.getProperty("bot.token", "");
        CHAT_ID = props.getProperty("chat.id", "");
        REGEX_PATTERN = props.getProperty("regex.pattern", ".*\\[CHAT] 㨏 ЛС \\[([^\\]]*?) » Я\\] (.*)");
        TELEGRAM_TEMPLATE = props.getProperty("telegram.template", "New message from {sender}: {message}");
        CLIPBOARD_TEMPLATE = props.getProperty("clipboard.template", "/msg {sender} получил твое сообщение");
    }

    private static void saveSettings() {
        props.setProperty("log.dir", LOG_DIR);
        props.setProperty("bot.token", BOT_TOKEN);
        props.setProperty("chat.id", CHAT_ID);
        props.setProperty("regex.pattern", REGEX_PATTERN);
        props.setProperty("telegram.template", TELEGRAM_TEMPLATE);
        props.setProperty("clipboard.template", CLIPBOARD_TEMPLATE);
        try (FileOutputStream out = new FileOutputStream(CONFIG_PATH.toFile())) {
            props.store(out, "LogReader Configuration");
        } catch (IOException ignored) {
        }
    }

    private static boolean showConfigDialog() {
        JDialog dialog = new JDialog(mainFrame, "Настройки LogReader", true);
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
        JTextField botTokenField = createField(BOT_TOKEN, 30);
        JTextField chatIdField = createField(CHAT_ID, 20);
        JTextField regexField = createField(REGEX_PATTERN, 50);
        JTextArea telegramTemplateArea = createArea(TELEGRAM_TEMPLATE, 3);
        JTextArea clipboardTemplateArea = createArea(CLIPBOARD_TEMPLATE, 3);

        addLabel(dialog, "Путь к логам:", 0, 0, gbc);
        addComponent(dialog, logDirField, 1, 0, gbc);
        addLabel(dialog, "Telegram Bot Token:", 0, 1, gbc);
        addComponent(dialog, botTokenField, 1, 1, gbc);
        addLabel(dialog, "Chat ID:", 0, 2, gbc);
        addComponent(dialog, chatIdField, 1, 2, gbc);
        addLabel(dialog, "Regex паттерн:", 0, 3, gbc);
        addComponent(dialog, regexField, 1, 3, gbc);
        addLabel(dialog, "Шаблон для Telegram:", 0, 4, gbc);
        addComponent(dialog, new JScrollPane(telegramTemplateArea), 1, 4, gbc);
        addLabel(dialog, "Шаблон для буфера:", 0, 5, gbc);
        addComponent(dialog, new JScrollPane(clipboardTemplateArea), 1, 5, gbc);

        JLabel infoLabel = new JLabel("<html>Используйте {sender} и {message} в шаблонах<br>Команды для Telegram бота: kill, status, screenshot, help</html>");
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.ITALIC, 11f));
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        dialog.add(infoLabel, gbc);

        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(new Color(34, 34, 34));
        btnPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        JButton ok = createButton("Запустить");
        JButton cancel = createButton("Отмена");

        ok.addActionListener(e -> {
            LOG_DIR = logDirField.getText().trim();
            BOT_TOKEN = botTokenField.getText().trim();
            CHAT_ID = chatIdField.getText().trim();
            REGEX_PATTERN = regexField.getText().trim();
            TELEGRAM_TEMPLATE = telegramTemplateArea.getText().trim();
            CLIPBOARD_TEMPLATE = clipboardTemplateArea.getText().trim();

            if (LOG_DIR.isEmpty() || BOT_TOKEN.isEmpty() || CHAT_ID.isEmpty() || REGEX_PATTERN.isEmpty()) {
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
        gbc.gridy = 7;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        dialog.add(btnPanel, gbc);

        JLabel dopamineLabel = new JLabel("dopamine");
        dopamineLabel.setFont(dopamineLabel.getFont().deriveFont(Font.ITALIC, 10f));
        dopamineLabel.setForeground(new Color(128, 128, 128));
        gbc.gridy = 8;
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.SOUTHEAST;
        gbc.insets = new Insets(5, 0, 5, 10);
        dialog.add(dopamineLabel, gbc);

        dialog.pack();
        dialog.setLocationRelativeTo(mainFrame);
        dialog.setVisible(true);
        return result[0] && !LOG_DIR.isEmpty() && !BOT_TOKEN.isEmpty() && !CHAT_ID.isEmpty() && !REGEX_PATTERN.isEmpty();
    }

    private static JTextField createField(String text, int cols) {
        JTextField field = new JTextField(text, cols);
        field.setBorder(new EmptyBorder(6, 10, 6, 10));
        return field;
    }

    private static JTextArea createArea(String text, int rows) {
        JTextArea area = new JTextArea(text, rows, 30);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(new EmptyBorder(6, 10, 6, 10));
        return area;
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

    private static void startBot() {
        try {
            bot = new MsgBot(BOT_TOKEN, CHAT_ID);

            botsApi = new TelegramBotsApi(DefaultBotSession.class);

            botsApi.registerBot(bot);

            isRunning = true;

            new Thread(() -> monitorLog()).start();

            bot.send("LogReader запущен и готов к работе!");

        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null,
                        "Ошибка при запуске бота: " + e.getMessage() +
                                "\nПроверьте правильность токена и настроек сети.",
                        "Ошибка",
                        JOptionPane.ERROR_MESSAGE);
            });

            isRunning = false;

            System.err.println("Ошибка при запуске бота:");
            e.printStackTrace();
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
        try {
            Pattern pattern = Pattern.compile(REGEX_PATTERN);
            Matcher m = pattern.matcher(line);

            if (m.find()) {
                String sender = m.group(1);
                String message = m.group(2);

                String telegramText = TELEGRAM_TEMPLATE.replace("{sender}", sender).replace("{message}", message);
                if (bot != null) {
                    bot.send(telegramText);
                }


            }
        } catch (Exception e) {
            System.err.println("Ошибка при обработке строки: " + line);
            e.printStackTrace();
        }
    }

    public static boolean killActiveWindow() {
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            return killActiveWindowLinux();
        }
        return killActiveWindowWindows();
    }

    private static boolean killActiveWindowWindows() {
        try {
            WinDef.HWND activeWindow = User32.INSTANCE.GetForegroundWindow();
            if (activeWindow == null) {
                return false;
            }

            IntByReference processId = new IntByReference();
            User32.INSTANCE.GetWindowThreadProcessId(activeWindow, processId);

            if (processId.getValue() == 0) {
                return false;
            }

            int pid = processId.getValue();

            String processName = getProcessName(pid);
            System.out.println("Попытка завершить процесс: " + processName + " (PID: " + pid + ")");

            if (terminateProcessAPI(pid)) {
                System.out.println("Процесс завершен через API");
                return true;
            }

            if (terminateProcessTaskkill(pid)) {
                System.out.println("Процесс завершен через taskkill");
                return true;
            }

            if (terminateProcessWMI(pid)) {
                System.out.println("Процесс завершен через WMI");
                return true;
            }

            if (terminateProcessTree(pid)) {
                System.out.println("Дерево процессов завершено");
                return true;
            }

            System.out.println("Не удалось завершить процесс всеми способами");
            return false;

        } catch (Exception e) {
            System.err.println("Ошибка при завершении процесса: " + e.getMessage());
            return false;
        }
    }

    private static String getProcessName(int pid) {
        try {
            Process process = Runtime.getRuntime().exec("tasklist /FI \"PID eq " + pid + "\" /FO CSV /NH");
            process.waitFor();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream())
            );
            String line = reader.readLine();
            if (line != null && !line.isEmpty()) {
                String[] parts = line.split(",");
                if (parts.length > 0) {
                    return parts[0].replace("\"", "");
                }
            }
        } catch (Exception ignored) {
        }
        return "Unknown";
    }

    private static boolean terminateProcessAPI(int pid) {
        try {
            int[] accessRights = {
                    0x0001,
                    0x1F0FFF,
                    0x0200 | 0x0400 | 0x0001,
                    0x0040 | 0x0001
            };

            for (int access : accessRights) {
                WinNT.HANDLE processHandle = Kernel32.INSTANCE.OpenProcess(access, false, pid);
                if (processHandle != null) {
                    boolean result = Kernel32.INSTANCE.TerminateProcess(processHandle, 1);
                    Kernel32.INSTANCE.CloseHandle(processHandle);

                    if (result) {
                        Thread.sleep(500);
                        if (!isProcessRunning(String.valueOf(pid))) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка в terminateProcessAPI: " + e.getMessage());
        }
        return false;
    }

    private static boolean terminateProcessTaskkill(int pid) {
        try {
            Process process = Runtime.getRuntime().exec("taskkill /PID " + pid + " /F");
            process.waitFor();

            if (process.exitValue() == 0) {
                Thread.sleep(500);
                if (!isProcessRunning(String.valueOf(pid))) {
                    return true;
                }
            }

            process = Runtime.getRuntime().exec("taskkill /PID " + pid + " /F /T");
            process.waitFor();

            if (process.exitValue() == 0) {
                Thread.sleep(500);
                if (!isProcessRunning(String.valueOf(pid))) {
                    return true;
                }
            }

        } catch (Exception e) {
            System.err.println("Ошибка в terminateProcessTaskkill: " + e.getMessage());
        }
        return false;
    }

    private static boolean terminateProcessWMI(int pid) {
        try {
            String command = "wmic process where \"ProcessId=" + pid + "\" call terminate";
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();

            if (process.exitValue() == 0) {
                Thread.sleep(500);
                if (!isProcessRunning(String.valueOf(pid))) {
                    return true;
                }
            }

            command = "wmic process where \"ProcessId=" + pid + "\" delete";
            process = Runtime.getRuntime().exec(command);
            process.waitFor();

            if (process.exitValue() == 0) {
                Thread.sleep(500);
                if (!isProcessRunning(String.valueOf(pid))) {
                    return true;
                }
            }

        } catch (Exception e) {
            System.err.println("Ошибка в terminateProcessWMI: " + e.getMessage());
        }
        return false;
    }

    private static boolean terminateProcessTree(int pid) {
        try {
            String command = "wmic process where \"ParentProcessId=" + pid + "\" call terminate";
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();

            Thread.sleep(500);

            return terminateProcessAPI(pid) || terminateProcessTaskkill(pid);

        } catch (Exception e) {
            System.err.println("Ошибка в terminateProcessTree: " + e.getMessage());
        }
        return false;
    }

    private static boolean killActiveWindowLinux() {
        try {
            Process process = Runtime.getRuntime().exec("xdotool getactivewindow getwindowpid");
            process.waitFor();

            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream())
            );

            String pid = reader.readLine();
            if (pid != null && !pid.trim().isEmpty()) {
                String[] signals = {"TERM", "KILL", "9", "15"};

                for (String signal : signals) {
                    Process killProcess = Runtime.getRuntime().exec("kill -" + signal + " " + pid.trim());
                    killProcess.waitFor();

                    if (killProcess.exitValue() == 0) {
                        Thread.sleep(500);
                        Process checkProcess = Runtime.getRuntime().exec("kill -0 " + pid.trim());
                        checkProcess.waitFor();
                        if (checkProcess.exitValue() != 0) {
                            return true;
                        }
                    }
                }
            }

            return false;
        } catch (Exception e) {
            System.err.println("Ошибка в killActiveWindowLinux: " + e.getMessage());
            return false;
        }
    }

    private static void copyToClipboard(String text) {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new StringSelection(text), null);
        } catch (Exception ignored) {
        }
    }

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