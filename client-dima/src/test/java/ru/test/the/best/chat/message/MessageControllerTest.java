package ru.test.the.best.chat.message;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.test.the.best.chat.controller.MessageController;
import ru.test.the.best.chat.controller.UserController;
import ru.test.the.best.chat.model.dto.message.CreateMessageRequest;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest
public class MessageControllerTest {

    @Autowired
    private MessageController messageController;

    @Autowired
    private UserController userController;

    @Test
    void getAllMessagesTest() {

        var user = userController.getUserByName("user1");
        var userId = user.getBody().getData().id();
        var user2 = userController.getUserByName("user2");
        var userId2 = user2.getBody().getData().id();

        var mes1 = new CreateMessageRequest(
                Instant.now(),
                userId,
                userId2,
                "Юзер2 отстой, юзер1 реальный тип!!! ",
                "STRING"
        );

        var mes2 = new CreateMessageRequest(
                Instant.now(),
                userId2,
                userId,
                "Юзер1 отстой, юзер2 реальный тип!!! ",
                "STRING"
        );

        messageController.sendMessage(mes1);
        messageController.sendMessage(mes2);

        var messages = messageController.getMessagesByTo(userId);
        var messages2 = messageController.getMessagesByTo(userId2);

        var allMessages = messageController.getAllMessages();
        System.out.println(allMessages.getBody().getData() + "ПЕЧАТАЮ РЕЗУЛЬТАТ!!!");

        messageController.deleteMessage(messages.getBody().getData().getFirst().id());
        messageController.deleteMessage(messages2.getBody().getData().getFirst().id());
        assertTrue(messageController.getAllMessages().getBody().getData().isEmpty());
    }
}
