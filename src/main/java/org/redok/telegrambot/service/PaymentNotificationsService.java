package org.redok.telegrambot.service;

import lombok.extern.slf4j.Slf4j;
import org.redok.telegrambot.model.StatusEnum;
import org.redok.telegrambot.model.User;
import org.redok.telegrambot.storage.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;

@Component
@Slf4j
public class PaymentNotificationsService {
    private final UserDao userDao;

    @Autowired
    public PaymentNotificationsService(UserDao userDao) {
        this.userDao = userDao;
    }

    public SendMessage messageReceiver(Update update) {
        if (update.hasMessage()) {
            Long chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();
            log.info("Message received for chat id {}", chatId);
            log.info("Message text {}", text);
            return handleCommandFromMessage(update);
        }
        return null;
    }

    private SendMessage handleCommandFromMessage(Update update) {
        Long chatId = update.getMessage().getChatId();
        if (!update.getMessage().isCommand()) {
            return SendMessage.builder()
                    .chatId(chatId)
                    .text("Введите, пожалуйста, команду")
                    .build();
        }

        String command = update.getMessage().getText();
        SendMessage sendMessage;
        switch (command) {
            case ("/start"):
                log.info("start command received");
                sendMessage = getStartCommandMessageWithButton(update);
                User user = User.getDefaultUser();
                user.setChatId(chatId);
                addNewUserToDb(user);
                break;
            case ("/reset"):
                log.info("reset command received");
                sendMessage = getResetCommandMessage(update);
                deleteUserFromDbByChatId(chatId.intValue());
                break;
            case ("/getinfo"):
                log.info("get info command received");
                sendMessage = getInfoCommandMessage(update);
                break;
            case ("/getchatid"):
                log.info("get chat id command received");
                sendMessage = getChatIdCommandMessage(update);
                break;
            default:
                sendMessage = getUnknownCommandMessage(update);
                break;
        }
        return sendMessage;
    }

    private SendMessage getStartCommandMessageWithButton(Update update) {
        long chatId = update.getMessage().getChatId();
        String textMessage = "Бот активирован. Хотите включить напоминания?";
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        rowInline.add(InlineKeyboardButton.builder()
                .text("Подключить уведомления")
                .callbackData("startNotifications")
                .build());
        InlineKeyboardRow inlineKeyboardRow = new InlineKeyboardRow(rowInline);
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup(Collections.singletonList(inlineKeyboardRow));
        return SendMessage.builder()
                .chatId(chatId)
                .text(textMessage)
                .replyMarkup(markupInline)
                .build();
    }

    private SendMessage getResetCommandMessage(Update update) {
        return SendMessage.builder()
                .chatId(update.getMessage().getChatId())
                .text("Сбрасываем состояние")
                .build();
    }

    private SendMessage getInfoCommandMessage(Update update) {
        try {
            String textMessage = userDao.getUserByChatId(update.getMessage().getChatId()).toString();
            return SendMessage.builder()
                    .chatId(update.getMessage().getChatId())
                    .text(textMessage)
                    .build();
        } catch (EmptyResultDataAccessException e) {
            return SendMessage.builder()
                    .chatId(update.getMessage().getChatId())
                    .text("Ошибка: пользователь не найден")
                    .build();
        }
    }

    private SendMessage getChatIdCommandMessage(Update update) {
        long chatId = update.getMessage().getChatId();
        return SendMessage.builder()
                .chatId(chatId)
                .text(format("Твой id = %d", chatId))
                .build();
    }

    private SendMessage getUnknownCommandMessage(Update update) {
        return SendMessage.builder()
                .chatId(update.getMessage().getChatId())
                .text("Неизвестная команда")
                .build();

    }

    public void addNewUserToDb(User user) {
        try {
            userDao.addUser(user);
        } catch (DuplicateKeyException e) {
            log.info("User {} already exists", user.getChatId());
        }
    }

    public void deleteUserFromDbByChatId(int id) {
        userDao.deleteUserByChatId(id);
    }

    public List<User> getAllUsers() {
        return userDao.getAllUsers();
    }

    public User updateUserDbStatusByChatId(int chatId, StatusEnum status) {
        return userDao.updateUserStatusByChatId(chatId, status);
    }

}
