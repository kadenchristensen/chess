package server;

import chess.ChessGame;
import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MySqlDataAccess;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import io.javalin.websocket.WsContext;
import model.AuthData;
import model.GameData;
import service.ClearService;
import service.GameService;
import service.LoginRequest;
import service.LogoutRequest;
import service.RegisterRequest;
import service.UserService;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Server {

    private final Gson gson = new Gson();
    private Javalin javalin;

    private final DataAccess dao;

    private final ClearService clearService;
    private final UserService userService;
    private final GameService gameService;

    private final Map<Integer, Set<WsContext>> gameSessions = new HashMap<>();

    private record JoinBody(String playerColor, Integer gameID) {}

    private record ListGame(
            int gameID,
            String whiteUsername,
            String blackUsername,
            String gameName
    ) {}

    private record ListBody(Collection<ListGame> games) {}

    public record CreateGameRequest(String gameName) {}
    public record CreateGameResult(int gameID) {}

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

        javalin.ws("/ws", ctx -> {
            ctx.onConnect(session -> {
                System.out.println("WebSocket connected");
            });

            ctx.onMessage(messageContext -> {
                try {
                    UserGameCommand command = gson.fromJson(messageContext.message(), UserGameCommand.class);
                    System.out.println("WebSocket command: " + command.getCommandType());

                    if (command.getCommandType() == UserGameCommand.CommandType.CONNECT) {

                        AuthData auth = dao.getAuth(command.getAuthToken());
                        if (auth == null) {
                            ErrorMessage error = new ErrorMessage("Error: unauthorized");
                            messageContext.send(gson.toJson(error));
                            return;
                        }

                        GameData game = dao.getGame(command.getGameID());
                        if (game == null) {
                            ErrorMessage error = new ErrorMessage("Error: bad request");
                            messageContext.send(gson.toJson(error));
                            return;
                        }

                        gameSessions.putIfAbsent(command.getGameID(), new HashSet<>());
                        Set<WsContext> sessions = gameSessions.get(command.getGameID());

                        String username = auth.username();
                        String noticeText = username + " joined the game";

                        for (WsContext session : sessions) {
                            if (session != messageContext) {
                                NotificationMessage notification = new NotificationMessage(noticeText);
                                session.send(gson.toJson(notification));
                            }
                        }

                        sessions.add(messageContext);

                        LoadGameMessage loadGameMessage = new LoadGameMessage(game.game());
                        messageContext.send(gson.toJson(loadGameMessage));
                        return;
                    }

                    if (command.getCommandType() == UserGameCommand.CommandType.MAKE_MOVE) {
                        MakeMoveCommand moveCommand = gson.fromJson(messageContext.message(), MakeMoveCommand.class);

                        AuthData auth = dao.getAuth(moveCommand.getAuthToken());
                        if (auth == null) {
                            messageContext.send(gson.toJson(new ErrorMessage("Error: unauthorized")));
                            return;
                        }

                        GameData game = dao.getGame(moveCommand.getGameID());
                        if (game == null) {
                            messageContext.send(gson.toJson(new ErrorMessage("Error: bad request")));
                            return;
                        }

                        if (game.game().isGameOver()) {
                            messageContext.send(gson.toJson(new ErrorMessage("Error: game already over")));
                            return;
                        }

                        if (isObserver(auth, game)) {
                            messageContext.send(gson.toJson(new ErrorMessage("Error: observers cannot make moves")));
                            return;
                        }

                        if (!isPlayersTurn(auth, game)) {
                            messageContext.send(gson.toJson(new ErrorMessage("Error: not your turn")));
                            return;
                        }

                        game.game().makeMove(moveCommand.getMove());

                        dao.updateGame(new GameData(
                                game.gameID(),
                                game.whiteUsername(),
                                game.blackUsername(),
                                game.gameName(),
                                game.game()
                        ));

                        broadcastToGame(game.gameID(), new LoadGameMessage(game.game()));
                        broadcastToOthers(
                                game.gameID(),
                                messageContext,
                                new NotificationMessage(auth.username() + " made a move")
                        );
                        return;
                    }

                    if (command.getCommandType() == UserGameCommand.CommandType.RESIGN) {
                        AuthData auth = dao.getAuth(command.getAuthToken());
                        if (auth == null) {
                            messageContext.send(gson.toJson(new ErrorMessage("Error: unauthorized")));
                            return;
                        }

                        GameData game = dao.getGame(command.getGameID());
                        if (game == null) {
                            messageContext.send(gson.toJson(new ErrorMessage("Error: bad request")));
                            return;
                        }

                        if (isObserver(auth, game)) {
                            messageContext.send(gson.toJson(new ErrorMessage("Error: observers cannot resign")));
                            return;
                        }

                        if (game.game().isGameOver()) {
                            messageContext.send(gson.toJson(new ErrorMessage("Error: game already over")));
                            return;
                        }

                        game.game().setGameOver(true);

                        dao.updateGame(new GameData(
                                game.gameID(),
                                game.whiteUsername(),
                                game.blackUsername(),
                                game.gameName(),
                                game.game()
                        ));

                        broadcastToGame(
                                game.gameID(),
                                new NotificationMessage(auth.username() + " resigned the game")
                        );
                        return;
                    }

                    ErrorMessage error = new ErrorMessage("Error: websocket reached server");
                    messageContext.send(gson.toJson(error));

                } catch (Exception e) {
                    ErrorMessage error = new ErrorMessage("Error: " + e.getMessage());
                    messageContext.send(gson.toJson(error));
                }
            });

            ctx.onClose(closeContext -> {
                for (Set<WsContext> sessions : gameSessions.values()) {
                    sessions.remove(closeContext);
                }
            });
        });

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

    private void broadcastToGame(int gameID, Object message) {
        Set<WsContext> sessions = gameSessions.get(gameID);
        if (sessions == null) {
            return;
        }

        String json = gson.toJson(message);
        for (WsContext session : sessions) {
            session.send(json);
        }
    }

    private void broadcastToOthers(int gameID, WsContext exclude, Object message) {
        Set<WsContext> sessions = gameSessions.get(gameID);
        if (sessions == null) {
            return;
        }

        String json = gson.toJson(message);
        for (WsContext session : sessions) {
            if (session != exclude) {
                session.send(json);
            }
        }
    }

    private boolean isObserver(AuthData auth, GameData game) {
        return !auth.username().equals(game.whiteUsername()) &&
                !auth.username().equals(game.blackUsername());
    }

    private boolean isPlayersTurn(AuthData auth, GameData game) {
        ChessGame.TeamColor teamTurn = game.game().getTeamTurn();

        if (teamTurn == ChessGame.TeamColor.WHITE) {
            return auth.username().equals(game.whiteUsername());
        } else {
            return auth.username().equals(game.blackUsername());
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