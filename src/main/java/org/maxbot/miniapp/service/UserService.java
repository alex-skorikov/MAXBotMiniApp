package org.maxbot.miniapp.service;

import org.maxbot.miniapp.dto.bot.CallbackDto;
import org.maxbot.miniapp.dto.bot.SenderDto;
import org.maxbot.miniapp.dto.bot.UpdateDto;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.TimeZone;

@Service
public class UserService {

    public static String getUserInfo(CallbackDto cb, UpdateDto update) {
        SenderDto user = cb.getUser();
        LocalDateTime lastTime =
                LocalDateTime.ofInstant(Instant.ofEpochMilli(user.getLastActivityTime()),
                        TimeZone.getDefault().toZoneId());
        return String.format(
                "Информация о вас:\n" +
                        "ID: %d\n" +
                        "Имя: %s\n" +
                        "Username: %s\n" +
                        "Роль: %s\n" +
                        "Последняя активность: %s\n" +
                        "Тип чата: %s\n" +
                        "Ваш язык: %s\n" ,
                user.getUserId(),
                user.getName(),
                user.getUsername() == null ? "не задан" : user.getUsername(),
                "неизвестно",
                lastTime,
                update.getMessage().getRecipient().getChatType(),
                update.getUserLocale()
        );
    }
}
