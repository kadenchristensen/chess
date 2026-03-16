package service;

import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import model.AuthData;
import model.GameData;
import chess.ChessGame;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JoinGameServiceTest {

    @Test
    void joinGamePositive_White() throws Exception {
        var dao = new MemoryDataAccess();
        var service = new GameService(dao);

        // auth passes
        dao.createAuth(new AuthData("token123", "kaden"));

        // create a game directly in DAO (no players yet)
        int gameID = dao.createGame(new GameData(0, null, null, "Game 1", new ChessGame()));

        // join as WHITE
        assertDoesNotThrow(() -> service.joinGame("token123", gameID, "WHITE"));

        var updated = dao.getGame(gameID);
        assertNotNull(updated);
        assertEquals("kaden", updated.whiteUsername());
        assertNull(updated.blackUsername());
    }

    @Test
    void joinGameNegative_AlreadyTaken() throws Exception {
        var dao = new MemoryDataAccess();
        var service = new GameService(dao);

        // auth passes
        dao.createAuth(new AuthData("token123", "kaden"));
        dao.createAuth(new AuthData("token456", "olivia"));

        // create game and pre-fill white
        int gameID = dao.createGame(new GameData(0, "kaden", null, "Game 1", new ChessGame()));

        // second user tries to take WHITE -> should fail
        assertThrows(DataAccessException.class, () ->
                service.joinGame("token456", gameID, "WHITE"));
    }
}
