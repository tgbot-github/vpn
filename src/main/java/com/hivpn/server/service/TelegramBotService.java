package com.hivpn.server.service;

import com.hivpn.server.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBotService extends TelegramLongPollingBot {
    private final UserService userService;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${app.base-url}")
    private String baseUrl;

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            String username = update.getMessage().getFrom().getUserName();
            Long telegramId = update.getMessage().getFrom().getId();

            if (messageText.startsWith("/start")) {
                handleStartCommand(username, telegramId, update.getMessage().getChatId());
            } else if (messageText.startsWith("/feedback")) {
                handleFeedbackCommand(update.getMessage().getChatId());
            } else {
                handleFeedbackMessage(messageText, username, telegramId, update.getMessage().getChatId());
            }
        }
    }

    private void handleStartCommand(String username, Long telegramId, Long chatId) {
        CompletableFuture<User> userFuture = userService.getOrCreateUserByTelegramId(telegramId, username);
        userFuture.thenAccept(user -> {
            String dashboardUrl = baseUrl + "/profile/" + user.getAccessToken();
            String welcomeMessage = String.format(
                "Добро пожаловать в HiVPN, %s! 🚀\n\n" +
                "Ваш аккаунт успешно зарегистрирован.\n" +
                "Для доступа к личному кабинету перейдите по ссылке:\n%s",
                username, dashboardUrl
            );
            sendMessage(chatId, welcomeMessage);
        }).exceptionally(error -> {
            log.error("Error handling start command: {}", error.getMessage());
            sendMessage(chatId, "Произошла ошибка при регистрации. Пожалуйста, попробуйте позже.");
            return null;
        });
    }

    private void handleFeedbackCommand(Long chatId) {
        String message = "Пожалуйста, напишите ваш отзыв или предложение в одном сообщении.";
        sendMessage(chatId, message);
    }

    private void handleFeedbackMessage(String message, String username, Long telegramId, Long chatId) {
        String response = "Спасибо за ваш отзыв! Мы обязательно его рассмотрим.";
        sendMessage(chatId, response);
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message: {}", e.getMessage());
        }
    }
} 