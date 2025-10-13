package ru.test.the.best.chat.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.test.the.best.chat.controller.UserController;
import ru.test.the.best.chat.model.dto.user.CreateUserRequest;
import ru.test.the.best.chat.service.UserService;

import java.util.UUID;

@SpringBootTest
public class UserControllerTest {

    private UserService userService;

    @Autowired
    private UserController userController;

    @Test
    public void getAllTest() {
        var res = userController.getAllUsers();
        System.out.println(res.getBody().getData() + "ПЕЧАТАЮ РЕЗУЛЬТАТ!!!");
    }

    @Test
    public void createUserTest() {

        userController.createUser(new CreateUserRequest("user1"));
        var res = userController.getAllUsers();
        System.out.println(res.getBody().getData() + "ПЕЧАТАЮ РЕЗУЛЬТАТ СОЗДАНИЯ!!!");
    }

    @Test
    public void getUserByNameTest() {
        var res = userController.getUserByName("user1");
        System.out.println(res.getBody().getData() + "ПЕЧАТАЮ РЕЗУЛЬТАТ ПОИСКА ПО ИМЕНИ!!!");

        UUID id = res.getBody().getData().id();
        var res2 = userController.getUserById(id);
        System.out.println(res2.getBody().getData() + "ПЕЧАТАЮ РЕЗУЛЬТАТ ПОИСКА ПО ID!!!");
    }

}
