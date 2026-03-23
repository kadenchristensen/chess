package server;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MySqlDataAccess;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import model.GameData;
import service.ClearService;
import service.GameService;
import service.LoginRequest;
import service.LogoutRequest;
import service.RegisterRequest;
import service.UserService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

public class Server {

    private final Gson gson = new Gson();
    private Javalin javalin;

    // Use interface instead of MemoryDataAccess
    private final DataAccess dao;

    // Services share same DAO
    private final ClearService clearService;
    private final UserService userService;
    private final GameService gameService;

    // Used only for parsing PUT /game body
    private record JoinBody(String playerColor, Integer gameID) {}

    // Used only for GET /game response entries
    private record ListGame(
            int gameID,
            String whiteUsername,
            String blackUsername,
            String gameName
    ) {}

    // Used only for wrapping GET /game response
    private record ListBody(Collection<ListGame> games) {}

    // Small DTOs for create-game
    public record CreateGameRequest(String gameName) {}
    public record CreateGameResult(int gameID) {}

    // Constructor initializes MySQL DAO
    public Server() {
        try {
            dao = new MySqlDataAccess();
        } catch (DataAccessException e) {
            throw new RuntimeException(e);
        }

        clearService = new ClearService(dao);
        userService = new UserService(dao);
        gameService = new GameService(dao);
    }

    public int run(int port) {
        javalin = Javalin.create(config -> {
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/web";
                staticFiles.location = Location.CLASSPATH;
            });
        });

        // CLEAR DATABASE
        javalin.delete("/db", ctx -> {
            try {
                clearService.clear();
                ctx.status(200).result("{}");
            } catch (DataAccessException e) {
                handleDataAccessError(ctx, e);
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result(gson.toJson(error("internal server error")));
            }
        });

        // REGISTER USER
        javalin.post("/user", ctx -> {
            try {
                RegisterRequest request = gson.fromJson(ctx.body(), RegisterRequest.class);
                var auth = userService.register(request);
                ctx.status(200).result(gson.toJson(auth));
            } catch (DataAccessException e) {
                handleDataAccessError(ctx, e);
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result(gson.toJson(error("internal server error")));
            }
        });

        // LOGIN
        javalin.post("/session", ctx -> {
            try {
                LoginRequest request = gson.fromJson(ctx.body(), LoginRequest.class);
                var auth = userService.login(request);
                ctx.status(200).result(gson.toJson(auth));
            } catch (DataAccessException e) {
                handleDataAccessError(ctx, e);
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result(gson.toJson(error("internal server error")));
            }
        });

        // LOGOUT
        javalin.delete("/session", ctx -> {
            try {
                String authToken = ctx.header("Authorization");
                userService.logout(new LogoutRequest(authToken));
                ctx.status(200).result("{}");
            } catch (DataAccessException e) {
                String msg = safeLower(e.getMessage());
                if (msg.contains("bad request")) {
                    ctx.status(401).result(gson.toJson(error("unauthorized")));
                } else {
                    handleDataAccessError(ctx, e);
                }
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result(gson.toJson(error("internal server error")));
            }
        });

        // CREATE GAME
        javalin.post("/game", ctx -> {
            try {
                String authToken = ctx.header("Authorization");
                CreateGameRequest body = gson.fromJson(ctx.body(), CreateGameRequest.class);

                if (body == null || body.gameName() == null || body.gameName().isBlank()) {
                    ctx.status(400).result(gson.toJson(error("bad request")));
                    return;
                }

                int gameID = gameService.createGame(authToken, body.gameName());
                ctx.status(200).result(gson.toJson(new CreateGameResult(gameID)));
            } catch (DataAccessException e) {
                handleDataAccessError(ctx, e);
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result(gson.toJson(error("internal server error")));
            }
        });

        // JOIN GAME
        javalin.put("/game", ctx -> {
            try {
                String authToken = ctx.header("Authorization");
                JoinBody body = gson.fromJson(ctx.body(), JoinBody.class);

                if (body == null || body.gameID() == null) {
                    ctx.status(400).result(gson.toJson(error("bad request")));
                    return;
                }

                String color = body.playerColor();
                if (color == null || color.isBlank()) {
                    ctx.status(400).result(gson.toJson(error("bad request")));
                    return;
                }

                String upper = color.toUpperCase(Locale.ROOT);
                if (!upper.equals("WHITE") && !upper.equals("BLACK")) {
                    ctx.status(400).result(gson.toJson(error("bad request")));
                    return;
                }

                gameService.joinGame(authToken, body.gameID(), upper);
                ctx.status(200).result("{}");

            } catch (DataAccessException e) {
                handleDataAccessError(ctx, e);
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result(gson.toJson(error("internal server error")));
            }
        });

        // LIST GAMES
        javalin.get("/game", ctx -> {
            try {
                String authToken = ctx.header("Authorization");
                Collection<GameData> games = gameService.listGames(authToken);

                Collection<ListGame> result = new ArrayList<>();
                for (GameData game : games) {
                    result.add(new ListGame(
                            game.gameID(),
                            game.whiteUsername(),
                            game.blackUsername(),
                            game.gameName()
                    ));
                }

                ctx.status(200).result(gson.toJson(new ListBody(result)));
            } catch (DataAccessException e) {
                handleDataAccessError(ctx, e);
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result(gson.toJson(error("internal server error")));
            }
        });

        javalin.start(port);
        return javalin.port();
    }

    public void stop() {
        if (javalin != null) {
            javalin.stop();
        }
    }

    private void handleDataAccessError(Context ctx, DataAccessException e) {
        String msg = safeLower(e.getMessage());

        int code;
        if (msg.contains("unauthorized")) {
            code = 401;
        } else if (msg.contains("already taken")) {
            code = 403;
        } else if (msg.contains("bad request") || msg.contains("game not found")) {
            code = 400;
        } else {
            code = 500;
        }

        ctx.status(code).result(gson.toJson(error(e.getMessage())));
    }

    private static ErrorBody error(String message) {
        String safe = (message == null) ? "" : message;
        return new ErrorBody("Error: " + safe);
    }

    private record ErrorBody(String message) {}

    private static String safeLower(String s) {
        return (s == null) ? "" : s.toLowerCase(Locale.ROOT);
    }
}