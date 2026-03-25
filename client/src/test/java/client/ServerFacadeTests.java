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
}