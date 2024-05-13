package org.redok.telegrambot.bot;

import lombok.extern.slf4j.Slf4j;
import org.redok.telegrambot.model.StatusEnum;
import org.redok.telegrambot.model.User;
import org.redok.telegrambot.properties.BotProperties;
import org.redok.telegrambot.service.PaymentNotificationsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.ArrayList;
import java.util.List;

@Component
@EnableScheduling
@Slf4j
public class PaymentNotificationsBot implements LongPollingSingleThreadUpdateConsumer, SpringLongPollingBot {

    private final PaymentNotificationsService paymentNotificationsService;

    private final TelegramClient telegramClient;

    private final String botToken;

    @Autowired
    public PaymentNotificationsBot(BotProperties botProperties, PaymentNotificationsService paymentNotificationsService) {
        this.paymentNotificationsService = paymentNotificationsService;
        botToken = botProperties.getToken();
        telegramClient = new OkHttpTelegramClient(botToken);

        List<BotCommand> commandList = new ArrayList<>();
        commandList.add(new BotCommand("/start", "Starts the bot"));
        commandList.add(new BotCommand("/getchatid", "Get id"));
        commandList.add(new BotCommand("/getinfo", "Get all information about user"));
        commandList.add(new BotCommand("/reset", "Reset the notifications"));

        try {
            telegramClient.execute(new SetMyCommands(commandList));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void consume(Update update) {
        log.info("update received");
        if (update.hasMessage() && update.getMessage().hasText()) {
            SendMessage sendMessage = paymentNotificationsService.messageReceiver(update);
            try {
                telegramClient.execute(sendMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        } else if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery());
        }
    }

    private void handleCallback(CallbackQuery callbackQuery) {
        log.info("callback method: {}", callbackQuery.getData());
        String call_data = callbackQuery.getData();
        if (call_data.equals("startNotifications")) {
            startNotificationsCallback(callbackQuery);
        }
    }

    @Scheduled(cron = "${interval-in-cron}")
    private void remindAboutPayment() {
        log.info("Reminder for users started");
        List<User> users = paymentNotificationsService.getAllUsers();
        for (User user : users) {
            try {
                String notificationMessage = String.format("Привет, " + user.getFirstName() + ". Это напоминание" +
                        " об оплате за VPN. Тебе нужно скинуть: " + user.getPrice() + " рублей. Напомню телефон: " +
                        "+79128262156 тинькофф.");
                telegramClient.execute(SendMessage.builder().text(notificationMessage).chatId(user.getChatId()).build());
                log.info("Notification for chat id id: {} send", user.getChatId());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void startNotificationsCallback(CallbackQuery callbackQuery) {
        long messageId = callbackQuery.getMessage().getMessageId();
        long chatId = callbackQuery.getMessage().getChatId();
        try {
            paymentNotificationsService.updateUserDbStatusByChatId((int) chatId, StatusEnum.WITH_NOTIFICATIONS);
        } catch (EmptyResultDataAccessException e) {
            sendUserNotFoundEditMessage(chatId, messageId);
            log.warn("Notification start error for chat id: {} ", chatId);
            return;
        }
        log.info("Notification for chat: {} started", chatId);
        String responseText = "Уведомления включены. Пожалуйста, сохрани свой id: " + chatId + " и " +
                "сообщи его мне. Не выполняй команду /reset без нужды.";
        EditMessageText callbackMessage = EditMessageText
                .builder()
                .chatId(chatId)
                .messageId(Math.toIntExact(messageId))
                .text(responseText)
                .build();
        try {
            telegramClient.execute(callbackMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendUserNotFoundEditMessage(long chatId, long messageId) {
        String responseText = "Ошибка: пользователь не найден. Пожалуйста, выполните команду /start";
        EditMessageText callbackMessage = EditMessageText
                .builder()
                .chatId(chatId)
                .messageId(Math.toIntExact(messageId))
                .text(responseText)
                .build();
        try {
            telegramClient.execute(callbackMessage);
        } catch (TelegramApiException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }
}

