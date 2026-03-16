package service;

import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import model.AuthData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

    @Test
    void registerPositive() throws Exception {
        var dao = new MemoryDataAccess();
        var service = new UserService(dao);

        var req = new RegisterRequest("kaden", "pw", "kaden@email.com");
        AuthData auth = service.register(req);

        assertNotNull(auth);
        assertNotNull(auth.authToken());
        assertEquals("kaden", auth.username());

        // verify it really got stored
        assertNotNull(dao.getUser("kaden"));
        assertNotNull(dao.getAuth(auth.authToken()));
    }

    @Test
    void registerDuplicateUsername() throws Exception {
        var dao = new MemoryDataAccess();
        var service = new UserService(dao);

        service.register(new RegisterRequest("kaden", "pw", "kaden@email.com"));

        var ex = assertThrows(DataAccessException.class, () ->
                service.register(new RegisterRequest("kaden", "pw2", "kaden2@email.com"))
        );

        // optional, but nice:
        assertTrue(ex.getMessage().toLowerCase().contains("already"));
    }

    @Test
    void loginPositive() throws Exception {
        var dao = new MemoryDataAccess();
        var service = new UserService(dao);

        // first register the user so they exist
        service.register(new RegisterRequest("bob", "pass", "bob@email.com"));

        // now login
        AuthData auth = service.login(new LoginRequest("bob", "pass"));

        assertNotNull(auth);
        assertNotNull(auth.authToken());
        assertEquals("bob", auth.username());
    }

    @Test
    void loginUnauthorized() throws Exception {
        var dao = new MemoryDataAccess();
        var service = new UserService(dao);

        // user exists
        service.register(new RegisterRequest("bob", "pass", "bob@email.com"));

        // wrong password
        assertThrows(DataAccessException.class, () ->
                service.login(new LoginRequest("bob", "wrong")));
    }


    @Test
    void logoutPositive() throws Exception {
        var dao = new MemoryDataAccess();
        var service = new UserService(dao);

        AuthData auth = service.register(new RegisterRequest("bob", "pass", "bob@email.com"));

        assertDoesNotThrow(() -> service.logout(new LogoutRequest(auth.authToken())));
        assertNull(dao.getAuth(auth.authToken())); // token should be gone
    }

    @Test
    void logoutUnauthorized() throws Exception {
        var dao = new MemoryDataAccess();
        var service = new UserService(dao);

        assertThrows(DataAccessException.class, () ->
                service.logout(new LogoutRequest("not-a-real-token")));
    }


}