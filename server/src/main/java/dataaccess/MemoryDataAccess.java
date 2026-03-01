package dataaccess;

import model.AuthData;
import model.GameData;
import model.UserData;

import java.util.Collection;
import java.util.List;

import java.util.HashMap;
import java.util.Map;

public class MemoryDataAccess implements DataAccess {


    private final Map<String, UserData> users = new HashMap<>();
    private final Map<String, AuthData> authTokens = new HashMap<>();
    private final Map<Integer, GameData> games = new HashMap<>();

    private int nextGameID = 1;

    @Override
    public void clear() throws DataAccessException {

        users.clear();
        authTokens.clear();
        games.clear();
        nextGameID = 1;

    }

    @Override
    public void createUser(UserData user) throws DataAccessException {

        if (user == null || user.username() == null) {
            throw new DataAccessException("User is null");
        }

        users.put(user.username(), user);

    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        if (username == null) {
            throw new DataAccessException("username is null");
        }
        return users.get(username); // returns null if not found (good)
    }

    @Override
    public void createAuth(AuthData auth) throws DataAccessException {

        if (auth == null || auth.authToken() == null) {
            throw new DataAccessException("Auth is null");
        }

        authTokens.put(auth.authToken(), auth);

    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        if (authToken == null) {
            throw new DataAccessException("authToken is null");
        }
        return authTokens.get(authToken);
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {

        if (authToken == null || authTokens.remove(authToken) == null) {
            throw new DataAccessException("bad authToken");
        }

    }

    @Override
    public int createGame(GameData game) throws DataAccessException {
        if (game == null) {
            throw new DataAccessException("game is null");
        }

        int gameID = nextGameID++;

        GameData newGame = new GameData(
                gameID,
                game.whiteUsername(),
                game.blackUsername(),
                game.gameName(),
                game.game()
        );

        games.put(gameID, newGame);

        return gameID;
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {

        return games.get(gameID);

    }

    @Override
    public Collection<GameData> listGames() throws DataAccessException {
        return List.of();
    }

    @Override
    public void updateGame(GameData game) throws DataAccessException {

    }
}
