package org.redok.telegrambot.model;


import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@ToString
public class User {
    private long chatId;
    private StatusEnum status;
    private Integer price;
    private String firstName;
    private String telegramName;

    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("chat_id", chatId);
        result.put("status", status.toString());
        result.put("price", price);
        result.put("first_name", firstName);
        result.put("telegram_name", telegramName);
        return result;
    }

    public static User getDefaultUser() {
        return User.builder()
                .status(StatusEnum.STARTED)
                .build();
    }
}

