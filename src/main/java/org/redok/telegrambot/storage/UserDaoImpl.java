package org.redok.telegrambot.storage;

import org.redok.telegrambot.model.StatusEnum;
import org.redok.telegrambot.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Component
public class UserDaoImpl implements UserDao {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public User addUser(User user) {
        String sql = "INSERT INTO users (chat_id, status) VALUES (?, ?)";
        jdbcTemplate.update(sql,
                user.getChatId(),
                user.getStatus().toString()
        );
        return user;
    }

    @Override
    public User getUserByChatId(long id) {
        String sql = "SELECT * FROM users WHERE chat_id = ?";
        return jdbcTemplate.queryForObject(sql, this::mapRowToUser, id);
    }

    @Override
    public User updateUserStatusByChatId(int chatId, StatusEnum status) {
        String sql = "UPDATE users SET status = ? WHERE chat_id = ?";
        jdbcTemplate.update(sql, status.toString(), chatId);
        return getUserByChatId(chatId);
    }

    @Override
    public List<User> getAllUsers() {
        String sql = "SELECT * FROM users";
        return jdbcTemplate.query(sql, this::mapRowToUser);
    }

    @Override
    public void deleteUserByChatId(int id) {
        String sqlQuery = "DELETE FROM users WHERE chat_id = ?";
        jdbcTemplate.update(sqlQuery, id);
    }

    private User mapRowToUser(ResultSet rs, int rowNum) throws SQLException {
        return User.builder()
                .chatId(rs.getInt("chat_id"))
                .firstName(rs.getString("first_name"))
                .telegramName(rs.getString("telegram_name"))
                .status(StatusEnum.valueOf(rs.getString("status")))
                .price(rs.getInt("price"))
                .build();
    }
}
