package client;

import org.junit.jupiter.api.*;
import server.Server;

public class ServerFacadeTests {

    private static Server server;
    private static ServerFacade facade;

    @BeforeAll
    public static void init() {
        server = new Server();
        var port = server.run(0);
        facade = new ServerFacade(port);
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @BeforeEach
    void clearDatabase() throws Exception {
        facade.clear();
    }

    @Test
    void registerPositive() throws Exception {
        var auth = facade.register("user1", "pass", "email@test.com");
        Assertions.assertNotNull(auth);
    }

    @Test
    void registerNegative() {
        Assertions.assertThrows(Exception.class, () -> {
            facade.register(null, null, null);
        });
    }



    @Test
    void loginPositive() throws Exception {
        facade.register("user2", "pass", "user2@test.com");
        var auth = facade.login("user2", "pass");
        Assertions.assertNotNull(auth);
    }

    @Test
    void loginNegative() {
        Assertions.assertThrows(Exception.class, () -> {
            facade.login("ghost", "wrong");
        });
    }

    @Test
    void createGamePositive() throws Exception {
        var auth = facade.register("user3", "pass", "user3@test.com");
        var result = facade.createGame("test game", auth.authToken());
        Assertions.assertTrue(result.gameID() > 0);
    }

    @Test
    void createGameNegative() {
        Assertions.assertThrows(Exception.class, () -> {
            facade.createGame("bad game", "not-a-real-token");
        });
    }


}