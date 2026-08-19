package org.maxbot.miniapp.service;

import org.junit.jupiter.api.Test;
import org.maxbot.miniapp.dto.bot.CallbackDto;
import org.maxbot.miniapp.dto.bot.MessageDto;
import org.maxbot.miniapp.dto.bot.RecipientDto;
import org.maxbot.miniapp.dto.bot.SenderDto;
import org.maxbot.miniapp.dto.bot.UpdateDto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserServiceTest {

    @Test
    void shouldFormatUserInfoWithFullDataSuccessfully() {
        // Given
        long epochMilli = 1711800000000L; // Пример: 2024-03-30
        LocalDateTime expectedLastTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(epochMilli),
                TimeZone.getDefault().toZoneId()
        );

        SenderDto user = new SenderDto();
        user.setUserId(12345);
        user.setName("Алексей");
        user.setUsername("alex_dev");
        user.setLastActivityTime(epochMilli);

        CallbackDto cb = new CallbackDto();
        cb.setUser(user);

        UpdateDto update = new UpdateDto();
        update.setUserLocale("ru");

        MessageDto messageDto = new MessageDto();
        RecipientDto recipientDto = new RecipientDto();
        recipientDto.setChatType("private");
        messageDto.setRecipient(recipientDto);
        update.setMessage(messageDto);

        // When
        String result = UserService.getUserInfo(cb, update);

        // Then
        String expected = String.format(
                "Информация о вас:\n" +
                        "ID: 12345\n" +
                        "Имя: Алексей\n" +
                        "Username: alex_dev\n" +
                        "Роль: неизвестно\n" +
                        "Последняя активность: %s\n" +
                        "Тип чата: private\n" +
                        "Ваш язык: ru\n",
                expectedLastTime
        );

        assertEquals(expected, result);
    }

    @Test
    void shouldFormatUserInfoWhenUsernameIsNull() {
        // Given
        long epochMilli = 1711800000000L;
        LocalDateTime expectedLastTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(epochMilli),
                TimeZone.getDefault().toZoneId()
        );

        SenderDto user = new SenderDto();
        user.setUserId(67890);
        user.setName("Иван");
        user.setUsername(null); // Проверяем ветку "не задан"
        user.setLastActivityTime(epochMilli);

        CallbackDto cb = new CallbackDto();
        cb.setUser(user);

        UpdateDto update = new UpdateDto();
        update.setUserLocale("en");

        MessageDto messageDto = new MessageDto();
        RecipientDto recipientDto = new RecipientDto();
        recipientDto.setChatType("group");
        messageDto.setRecipient(recipientDto);
        update.setMessage(messageDto);

        // When
        String result = UserService.getUserInfo(cb, update);

        // Then
        String expected = String.format(
                "Информация о вас:\n" +
                        "ID: 67890\n" +
                        "Имя: Иван\n" +
                        "Username: не задан\n" +
                        "Роль: неизвестно\n" +
                        "Последняя активность: %s\n" +
                        "Тип чата: group\n" +
                        "Ваш язык: en\n",
                expectedLastTime
        );

        assertEquals(expected, result);
    }
}
