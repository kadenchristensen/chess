package service;

import chess.ChessGame;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.GameData;

import java.util.Collection;

public class GameService {

    private final DataAccess dataAccess;

    public GameService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public Collection<GameData> listGames(String authToken) throws DataAccessException {
        if (authToken == null || dataAccess.getAuth(authToken) == null) {
            throw new DataAccessException("unauthorized");
        }
        return dataAccess.listGames();
    }

    public int createGame(String authToken, String gameName) throws DataAccessException {
        if (authToken == null || dataAccess.getAuth(authToken) == null) {
            throw new DataAccessException("unauthorized");
        }

        if (gameName == null || gameName.isBlank()) {
            throw new DataAccessException("bad request");
        }

        GameData game = new GameData(
                0,
                null,
                null,
                gameName,
                new ChessGame()
        );

        return dataAccess.createGame(game);
    }

    public void joinGame(String authToken, int gameID, String playerColor) throws DataAccessException {
        if (authToken == null || dataAccess.getAuth(authToken) == null) {
            throw new DataAccessException("unauthorized");
        }

        if (playerColor == null || playerColor.isBlank()) {
            throw new DataAccessException("bad request");
        }

        AuthData auth = dataAccess.getAuth(authToken);
        GameData game = dataAccess.getGame(gameID);

        if (game == null) {
            throw new DataAccessException("game not found");
        }

        String username = auth.username();

        if (playerColor.equalsIgnoreCase("WHITE")) {
            if (game.whiteUsername() != null && !game.whiteUsername().equals(username)) {
                throw new DataAccessException("already taken");
            }
            game = new GameData(
                    game.gameID(),
                    username,
                    game.blackUsername(),
                    game.gameName(),
                    game.game()
            );
        } else if (playerColor.equalsIgnoreCase("BLACK")) {
            if (game.blackUsername() != null && !game.blackUsername().equals(username)) {
                throw new DataAccessException("already taken");
            }
            game = new GameData(
                    game.gameID(),
                    game.whiteUsername(),
                    username,
                    game.gameName(),
                    game.game()
            );
        } else {
            throw new DataAccessException("bad request");
        }

        dataAccess.updateGame(game);
    }
}