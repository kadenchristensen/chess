package service;

import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import model.AuthData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GameServiceTest {

    @Test
    void createGamePositive() throws Exception {
        var dao = new MemoryDataAccess();
        var service = new GameService(dao);

        // Put an auth token in the DAO so auth passes
        var auth = new AuthData("token123", "kaden");
        dao.createAuth(auth);

        int gameID = service.createGame("token123", "My Game");

        assertTrue(gameID > 0);
        assertNotNull(dao.getGame(gameID));
        assertEquals("My Game", dao.getGame(gameID).gameName());
    }

    @Test
    void createGameNegative_Unauthorized() {
        var dao = new MemoryDataAccess();
        var service = new GameService(dao);

        assertThrows(DataAccessException.class, () ->
                service.createGame("badToken", "My Game"));
    }

    @Test
    void listGamesPositive() throws Exception {
        var dao = new MemoryDataAccess();
        var service = new GameService(dao);

        // auth passes
        dao.createAuth(new AuthData("token123", "kaden"));

        // create 2 games directly through service (or DAO if your service needs different args)
        service.createGame("token123", "Game A");
        service.createGame("token123", "Game B");

        var games = service.listGames("token123");

        assertNotNull(games);
        assertEquals(2, games.size());
    }

    @Test
    void listGamesNegative_Unauthorized() {
        var dao = new MemoryDataAccess();
        var service = new GameService(dao);

        assertThrows(DataAccessException.class, () ->
                service.listGames("badToken"));
    }

}