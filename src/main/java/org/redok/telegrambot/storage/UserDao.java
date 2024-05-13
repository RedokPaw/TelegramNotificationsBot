package org.redok.telegrambot.storage;

import org.redok.telegrambot.model.StatusEnum;
import org.redok.telegrambot.model.User;

import java.util.List;

public interface UserDao {
    User addUser(User user);

    User getUserByChatId(long id);

    User updateUserStatusByChatId(int chatId, StatusEnum status);

    void deleteUserByChatId(int id);

    List<User> getAllUsers();
}
