package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MsgBot extends TelegramLongPollingBot {
    private final String token;
    private final String chatId;
    private boolean awaitingMessage = false;

    public MsgBot(String token, String chatId) {
        this.token = token;
        this.chatId = chatId;
    }

    @Override
    public String getBotUsername() {
        return "LogReaderBot";
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText().trim();
            String userChatId = update.getMessage().getChatId().toString();

            if (userChatId.equals(chatId)) {
                if (awaitingMessage) {
                    sendMessageToGame(messageText);
                    awaitingMessage = false;
                } else {
                    handleCommand(messageText);
                }
            }
        }
    }

    private void handleCommand(String messageText) {
        if (messageText.equalsIgnoreCase("kill")) {
            boolean success = LogReader.killActiveWindow();
            String response = success ? "Процесс активного окна завершен" : "Не удалось завершить процесс";
            send(response);
        } else if (messageText.equalsIgnoreCase("status")) {
            send("LogReader работает и готов к получению команд");
        } else if (messageText.equalsIgnoreCase("screenshot")) {
            sendScreenshot();
        } else if (messageText.equalsIgnoreCase("chat")) {
            awaitingMessage = true;
            send("Введите сообщение для отправки в чат игры:");
        } else if (messageText.equalsIgnoreCase("help")) {
            send("Доступные команды:\n" +
                    "• kill - завершить процесс активного окна\n" +
                    "• status - проверить статус бота\n" +
                    "• screenshot - отправить скриншот экрана\n" +
                    "• chat - отправить сообщение в игровой чат\n" +
                    "• help - показать эту справку");
        }
    }

    private void sendMessageToGame(String message) {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new StringSelection(message), null);

            Robot robot = new Robot();
            robot.setAutoDelay(50);

            Thread.sleep(100);

            robot.keyPress(KeyEvent.VK_CONTROL);
            robot.keyPress(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_CONTROL);

            Thread.sleep(100);

            robot.keyPress(KeyEvent.VK_ENTER);
            robot.keyRelease(KeyEvent.VK_ENTER);

            Thread.sleep(100);

            robot.keyPress(KeyEvent.VK_T);
            robot.keyRelease(KeyEvent.VK_T);

            send("Сообщение отправлено в игровой чат: " + message);

        } catch (Exception e) {
            System.err.println("Ошибка при отправке сообщения в игру:");
            e.printStackTrace();
            send("Ошибка при отправке сообщения: " + e.getMessage());
        }
    }

    public void send(String text) {
        try {
            SendMessage sm = new SendMessage();
            sm.setChatId(chatId);
            sm.setText(text);
            execute(sm);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка при отправке сообщения в Telegram:");
            e.printStackTrace();
        }
    }

    private void sendScreenshot() {
        try {
            BufferedImage screenshot = captureScreen();

            if (screenshot == null) {
                send("Не удалось сделать скриншот");
                return;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(screenshot, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            String fileName = "screenshot_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".png";
            InputFile inputFile = new InputFile(new ByteArrayInputStream(imageBytes), fileName);

            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(chatId);
            sendPhoto.setPhoto(inputFile);
            sendPhoto.setCaption("Скриншот экрана " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));

            execute(sendPhoto);

        } catch (Exception e) {
            System.err.println("Ошибка при отправке скриншота:");
            e.printStackTrace();
            send("Ошибка при создании или отправке скриншота: " + e.getMessage());
        }
    }

    private BufferedImage captureScreen() {
        try {
            Robot robot = new Robot();
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            return robot.createScreenCapture(screenRect);
        } catch (AWTException e) {
            System.err.println("Ошибка при создании скриншота:");
            e.printStackTrace();
            return null;
        }
    }
}