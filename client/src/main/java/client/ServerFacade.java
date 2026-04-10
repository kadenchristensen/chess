package client;

import com.google.gson.Gson;
import model.AuthData;
import model.GameData;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Collection;

public class ServerFacade {
    private final String serverUrl;
    private final Gson gson = new Gson();

    public ServerFacade(int port) {
        this.serverUrl = "http://localhost:" + port;
    }

    public ServerFacade(String url) {
        this.serverUrl = url;
    }

    // ✅ THIS IS THE FIX YOU NEEDED
    public String getServerUrl() {
        return serverUrl;
    }

    public AuthData register(String username, String password, String email) throws Exception {
        var body = new RegisterRequest(username, password, email);
        return makeRequest("POST", "/user", body, null, AuthData.class);
    }

    public AuthData login(String username, String password) throws Exception {
        var body = new LoginRequest(username, password);
        return makeRequest("POST", "/session", body, null, AuthData.class);
    }

    public void logout(String authToken) throws Exception {
        makeRequest("DELETE", "/session", null, authToken, null);
    }

    public CreateGameResult createGame(String gameName, String authToken) throws Exception {
        var body = new CreateGameRequest(gameName);
        return makeRequest("POST", "/game", body, authToken, CreateGameResult.class);
    }

    public ListGamesResult listGames(String authToken) throws Exception {
        return makeRequest("GET", "/game", null, authToken, ListGamesResult.class);
    }

    public void joinGame(String playerColor, int gameID, String authToken) throws Exception {
        var body = new JoinGameRequest(playerColor, gameID);
        makeRequest("PUT", "/game", body, authToken, null);
    }

    public void clear() throws Exception {
        makeRequest("DELETE", "/db", null, null, null);
    }

    private <T> T makeRequest(String method, String path, Object requestBody, String authToken, Class<T> responseClass) throws Exception {
        URL url = new URI(serverUrl + path).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setDoInput(true);

        if (authToken != null) {
            connection.setRequestProperty("Authorization", authToken);
        }

        if (requestBody != null) {
            connection.setDoOutput(true);
            connection.addRequestProperty("Content-Type", "application/json");
            try (OutputStream reqBody = connection.getOutputStream()) {
                reqBody.write(gson.toJson(requestBody).getBytes());
            }
        }

        connection.connect();

        int status = connection.getResponseCode();
        if (status / 100 != 2) {
            String message = "request failed";
            try (InputStream err = connection.getErrorStream()) {
                if (err != null) {
                    message = new String(err.readAllBytes());
                }
            }
            throw new RuntimeException(message);
        }

        if (responseClass == null) {
            return null;
        }

        try (InputStream respBody = connection.getInputStream()) {
            InputStreamReader reader = new InputStreamReader(respBody);
            return gson.fromJson(reader, responseClass);
        }
    }

    public record RegisterRequest(String username, String password, String email) {}
    public record LoginRequest(String username, String password) {}
    public record CreateGameRequest(String gameName) {}
    public record CreateGameResult(int gameID) {}
    public record JoinGameRequest(String playerColor, int gameID) {}
    public record ListGamesResult(Collection<GameData> games) {}
}