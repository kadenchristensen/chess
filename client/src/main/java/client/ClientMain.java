package client;

import chess.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import model.GameData;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.*;
import java.util.concurrent.CompletionStage;

public class ClientMain {

    private final Scanner scanner = new Scanner(System.in);
    private final ServerFacade facade;
    private final Gson gson = new Gson();

    private boolean running = true;
    private boolean loggedIn = false;

    private String authToken = null;
    private String username = null;

    private List<GameData> lastListedGames = new ArrayList<>();

    // gameplay state
    private boolean inGameplay = false;
    private Integer currentGameID = null;
    private boolean blackPerspective = false;
    private boolean observerMode = false;
    private ChessGame currentGame = null;
    private WebSocket ws = null;

    private static final String RESET = "\u001B[0m";
    private static final String BLACK_TEXT = "\u001B[30m";
    private static final String RED_TEXT = "\u001B[31m";
    private static final String BLUE_TEXT = "\u001B[34m";

    private static final String LIGHT_BG = "\u001B[47m";
    private static final String DARK_BG = "\u001B[100m";
    private static final String HIGHLIGHT_BG = "\u001B[42m";
    private static final String SELECT_BG = "\u001B[43m";

    public ClientMain() {
        this("http://localhost:8080");
    }

    public ClientMain(String serverUrl) {
        this.facade = new ServerFacade(serverUrl);
    }

    public static void main(String[] args) {
        String serverUrl = "http://localhost:8080";
        if (args.length > 0 && args[0] != null && !args[0].isBlank()) {
            serverUrl = args[0].trim();
        }
        new ClientMain(serverUrl).run();
    }

    public void run() {
        System.out.println("Welcome to Chess");
        System.out.println("Connected to server");

        while (running) {
            try {
                if (inGameplay) {
                    System.out.print("\n[GAMEPLAY] >>> ");
                    evalGameplay(scanner.nextLine().trim().toLowerCase());
                } else if (!loggedIn) {
                    System.out.print("\n[LOGGED_OUT] >>> ");
                    evalLoggedOut(scanner.nextLine().trim().toLowerCase());
                } else {
                    System.out.print("\n[LOGGED_IN] >>> ");
                    evalLoggedIn(scanner.nextLine().trim().toLowerCase());
                }
            } catch (Exception e) {
                System.out.println("Error: " + friendlyMessage(e));
            }
        }
    }

    private void evalLoggedOut(String input) {
        switch (input) {
            case "help" -> System.out.println(loggedOutHelp());
            case "register" -> register();
            case "login" -> login();
            case "quit" -> {
                System.out.println("Goodbye");
                running = false;
                closeWebSocket();
            }
            default -> System.out.println("Unknown command");
        }
    }

    private void evalLoggedIn(String input) {
        switch (input) {
            case "help" -> System.out.println(loggedInHelp());
            case "logout" -> logout();
            case "create game" -> createGame();
            case "list games" -> listGames();
            case "play game" -> playGame();
            case "observe game" -> observeGame();
            case "quit" -> {
                System.out.println("Goodbye");
                running = false;
                closeWebSocket();
            }
            default -> System.out.println("Unknown command");
        }
    }

    private void evalGameplay(String input) {
        switch (input) {
            case "help" -> System.out.println(gameplayHelp());
            case "redraw", "redraw chess board" -> redrawBoard();
            case "leave" -> leaveGame();
            case "move", "make move" -> makeMove();
            case "resign" -> resignGame();
            case "highlight", "highlight legal moves" -> highlightLegalMoves();
            default -> System.out.println("Unknown command");
        }
    }

    private void register() {
        try {
            System.out.print("username: ");
            String username = scanner.nextLine();

            System.out.print("password: ");
            String password = scanner.nextLine();

            System.out.print("email: ");
            String email = scanner.nextLine();

            var auth = facade.register(username, password, email);

            this.authToken = auth.authToken();
            this.username = auth.username();
            this.loggedIn = true;

            System.out.println("Successfully registered and logged in as " + username);

        } catch (Exception e) {
            System.out.println("Error: " + friendlyMessage(e));
        }
    }

    private void login() {
        try {
            System.out.print("username: ");
            String username = scanner.nextLine();

            System.out.print("password: ");
            String password = scanner.nextLine();

            var auth = facade.login(username, password);

            this.authToken = auth.authToken();
            this.username = auth.username();
            this.loggedIn = true;

            System.out.println("Logged in as " + username);

        } catch (Exception e) {
            System.out.println("Error: " + friendlyMessage(e));
        }
    }

    private void logout() {
        try {
            facade.logout(authToken);
            loggedIn = false;
            authToken = null;
            username = null;
            lastListedGames.clear();
            System.out.println("Logged out");

        } catch (Exception e) {
            System.out.println("Error: " + friendlyMessage(e));
        }
    }

    private void createGame() {
        try {
            System.out.print("game name: ");
            String name = scanner.nextLine();

            var result = facade.createGame(name, authToken);
            System.out.println("Created game ID: " + result.gameID());

        } catch (Exception e) {
            System.out.println("Error: " + friendlyMessage(e));
        }
    }

    private void listGames() {
        try {
            var result = facade.listGames(authToken);

            lastListedGames = new ArrayList<>(result.games());

            if (lastListedGames.isEmpty()) {
                System.out.println("No games found");
                return;
            }

            int i = 1;
            for (var g : lastListedGames) {
                String white = (g.whiteUsername() == null) ? "(open)" : g.whiteUsername();
                String black = (g.blackUsername() == null) ? "(open)" : g.blackUsername();

                System.out.println(i++ + ". " + g.gameName() +
                        " | White: " + white +
                        " | Black: " + black);
            }

        } catch (Exception e) {
            System.out.println("Error: " + friendlyMessage(e));
        }
    }

    private void playGame() {
        try {
            GameData game = chooseGame();
            if (game == null) {
                return;
            }

            System.out.print("Enter color (WHITE or BLACK): ");
            String color = scanner.nextLine().trim().toUpperCase();

            if (!color.equals("WHITE") && !color.equals("BLACK")) {
                System.out.println("Error: invalid color");
                return;
            }

            facade.joinGame(color, game.gameID(), authToken);

            observerMode = false;
            blackPerspective = color.equals("BLACK");
            currentGameID = game.gameID();

            connectWebSocket();
            sendCommand(new UserGameCommand(UserGameCommand.CommandType.CONNECT, authToken, currentGameID));

            inGameplay = true;
            System.out.println("Joined game: " + game.gameName() + " as " + color);
            System.out.println(gameplayHelp());

        } catch (Exception e) {
            System.out.println("Error: " + friendlyMessage(e));
        }
    }

    private void observeGame() {
        try {
            GameData game = chooseGame();
            if (game == null) {
                return;
            }

            observerMode = true;
            blackPerspective = false;
            currentGameID = game.gameID();

            connectWebSocket();
            sendCommand(new UserGameCommand(UserGameCommand.CommandType.CONNECT, authToken, currentGameID));

            inGameplay = true;
            System.out.println("Observing game: " + game.gameName());
            System.out.println(gameplayHelp());

        } catch (Exception e) {
            System.out.println("Error: " + friendlyMessage(e));
        }
    }

    private GameData chooseGame() {
        try {
            if (lastListedGames.isEmpty()) {
                System.out.println("Error: no listed games found");
                return null;
            }

            System.out.print("Enter game number: ");
            int choice = Integer.parseInt(scanner.nextLine());

            if (choice < 1 || choice > lastListedGames.size()) {
                System.out.println("Error: invalid game number");
                return null;
            }

            return lastListedGames.get(choice - 1);
        } catch (NumberFormatException e) {
            System.out.println("Error: invalid game number");
            return null;
        }
    }

    private void connectWebSocket() throws Exception {
        closeWebSocket();

        String httpUrl = facade.getServerUrl();
        String wsUrl;
        if (httpUrl.startsWith("https://")) {
            wsUrl = "wss://" + httpUrl.substring("https://".length()) + "/ws";
        } else if (httpUrl.startsWith("http://")) {
            wsUrl = "ws://" + httpUrl.substring("http://".length()) + "/ws";
        } else {
            wsUrl = "ws://" + httpUrl + "/ws";
        }

        ws = HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), new ChessWebSocketListener())
                .join();
    }

    private void closeWebSocket() {
        try {
            if (ws != null) {
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye").join();
            }
        } catch (Exception ignored) {
        }
        ws = null;
    }

    private void sendCommand(UserGameCommand command) {
        if (ws == null) {
            System.out.println("Error: not connected to gameplay");
            return;
        }
        ws.sendText(gson.toJson(command), true);
    }

    private void makeMove() {
        try {
            if (observerMode) {
                System.out.println("Error: observers cannot make moves");
                return;
            }

            System.out.print("Start square (example e2): ");
            ChessPosition start = parseSquare(scanner.nextLine());

            System.out.print("End square (example e4): ");
            ChessPosition end = parseSquare(scanner.nextLine());

            ChessPiece.PieceType promotion = null;
            if (isPromotionRow(end)) {
                System.out.print("Promotion piece (QUEEN/ROOK/BISHOP/KNIGHT or blank): ");
                String promoText = scanner.nextLine().trim().toUpperCase();
                if (!promoText.isBlank()) {
                    promotion = ChessPiece.PieceType.valueOf(promoText);
                }
            }

            ChessMove move = new ChessMove(start, end, promotion);
            sendCommand(new MakeMoveCommand(authToken, currentGameID, move));

        } catch (IllegalArgumentException e) {
            System.out.println("Error: invalid square or promotion");
        } catch (Exception e) {
            System.out.println("Error: " + friendlyMessage(e));
        }
    }

    private void resignGame() {
        try {
            if (observerMode) {
                System.out.println("Error: observers cannot resign");
                return;
            }

            System.out.print("Are you sure you want to resign? (yes/no): ");
            String answer = scanner.nextLine().trim().toLowerCase();
            if (!answer.equals("yes")) {
                System.out.println("Resign cancelled");
                return;
            }

            sendCommand(new UserGameCommand(UserGameCommand.CommandType.RESIGN, authToken, currentGameID));

        } catch (Exception e) {
            System.out.println("Error: " + friendlyMessage(e));
        }
    }

    private void leaveGame() {
        try {
            sendCommand(new UserGameCommand(UserGameCommand.CommandType.LEAVE, authToken, currentGameID));
        } catch (Exception e) {
            System.out.println("Error: " + friendlyMessage(e));
        } finally {
            inGameplay = false;
            observerMode = false;
            currentGameID = null;
            currentGame = null;
            closeWebSocket();
            System.out.println("Returned to post-login menu");
        }
    }

    private void redrawBoard() {
        if (currentGame == null) {
            System.out.println("Board not loaded yet");
            return;
        }
        drawBoard(currentGame, blackPerspective, null, null);
    }

    private void highlightLegalMoves() {
        try {
            if (currentGame == null) {
                System.out.println("Board not loaded yet");
                return;
            }

            System.out.print("Square to highlight (example e2): ");
            ChessPosition pos = parseSquare(scanner.nextLine());

            Collection<ChessMove> moves = currentGame.validMoves(pos);
            if (moves == null) {
                moves = new ArrayList<>();
            }

            Set<String> highlights = new HashSet<>();
            for (ChessMove move : moves) {
                highlights.add(key(move.getEndPosition()));
            }

            drawBoard(currentGame, blackPerspective, key(pos), highlights);

        } catch (Exception e) {
            System.out.println("Error: " + friendlyMessage(e));
        }
    }

    private void drawBoard(ChessGame game, boolean blackPerspective, String selected, Set<String> highlights) {
        ChessBoard board = game.getBoard();

        System.out.println();
        if (!blackPerspective) {
            System.out.println("    a  b  c  d  e  f  g  h");
            for (int row = 8; row >= 1; row--) {
                System.out.print(" " + row + " ");
                for (int col = 1; col <= 8; col++) {
                    printSquare(board, row, col, selected, highlights);
                }
                System.out.println(RESET + " " + row);
            }
            System.out.println("    a  b  c  d  e  f  g  h");
        } else {
            System.out.println("    h  g  f  e  d  c  b  a");
            for (int row = 1; row <= 8; row++) {
                System.out.print(" " + row + " ");
                for (int col = 8; col >= 1; col--) {
                    printSquare(board, row, col, selected, highlights);
                }
                System.out.println(RESET + " " + row);
            }
            System.out.println("    h  g  f  e  d  c  b  a");
        }
        System.out.println();
    }

    private void printSquare(ChessBoard board, int row, int col, String selected, Set<String> highlights) {
        ChessPosition pos = new ChessPosition(row, col);
        ChessPiece piece = board.getPiece(pos);

        boolean lightSquare = (row + col) % 2 == 0;
        String background = lightSquare ? LIGHT_BG : DARK_BG;

        String key = key(pos);
        if (selected != null && selected.equals(key)) {
            background = SELECT_BG;
        } else if (highlights != null && highlights.contains(key)) {
            background = HIGHLIGHT_BG;
        }

        String display = pieceToSymbol(piece);
        String textColor = BLACK_TEXT;

        if (piece != null) {
            if (piece.getTeamColor() == ChessGame.TeamColor.WHITE) {
                textColor = RED_TEXT;
            } else {
                textColor = BLUE_TEXT;
            }
        }

        System.out.print(background + textColor + " " + display + " " + RESET);
    }

    private String pieceToSymbol(ChessPiece piece) {
        if (piece == null) return ".";
        return switch (piece.getPieceType()) {
            case KING -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? "K" : "k";
            case QUEEN -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? "Q" : "q";
            case ROOK -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? "R" : "r";
            case BISHOP -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? "B" : "b";
            case KNIGHT -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? "N" : "n";
            case PAWN -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? "P" : "p";
        };
    }

    private ChessPosition parseSquare(String text) {
        text = text.trim().toLowerCase();
        if (text.length() != 2) throw new IllegalArgumentException();

        char file = text.charAt(0);
        char rank = text.charAt(1);

        if (file < 'a' || file > 'h') throw new IllegalArgumentException();
        if (rank < '1' || rank > '8') throw new IllegalArgumentException();

        int col = file - 'a' + 1;
        int row = rank - '0';
        return new ChessPosition(row, col);
    }

    private boolean isPromotionRow(ChessPosition end) {
        return end.getRow() == 1 || end.getRow() == 8;
    }

    private String key(ChessPosition pos) {
        return pos.getRow() + "," + pos.getColumn();
    }

    private String friendlyMessage(Exception e) {
        String msg = e.getMessage();
        if (msg == null) return "operation failed";

        msg = msg.toLowerCase();

        if (msg.contains("connect")) return "could not connect to server";
        if (msg.contains("taken")) return "username already taken";
        if (msg.contains("unauthorized")) return "invalid login";
        if (msg.contains("bad request")) return "bad request";

        return msg;
    }

    private String loggedOutHelp() {
        return """
                help      - show commands
                register  - create account
                login     - log in
                quit      - exit
                """;
    }

    private String loggedInHelp() {
        return """
                help          - show commands
                logout        - log out
                create game   - create a game
                list games    - list games
                play game     - join a game
                observe game  - watch a game
                quit          - exit
                """;
    }

    private String gameplayHelp() {
        return """
                help       - show gameplay commands
                redraw     - redraw chess board
                leave      - leave the game
                move       - make a move
                resign     - resign the game
                highlight  - highlight legal moves for a piece
                """;
    }

    private class ChessWebSocketListener implements WebSocket.Listener {
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                String message = buffer.toString();
                buffer.setLength(0);
                handleServerMessage(message);
            }
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        private void handleServerMessage(String json) {
            try {
                JsonObject obj = gson.fromJson(json, JsonObject.class);
                String type = obj.get("serverMessageType").getAsString();

                switch (type) {
                    case "LOAD_GAME" -> {
                        currentGame = gson.fromJson(obj.get("game"), ChessGame.class);
                        drawBoard(currentGame, blackPerspective, null, null);
                    }
                    case "NOTIFICATION" -> {
                        System.out.println("\n[Notification] " + obj.get("message").getAsString());
                    }
                    case "ERROR" -> {
                        System.out.println("\n" + obj.get("errorMessage").getAsString());
                    }
                    default -> System.out.println("\nUnknown server message: " + json);
                }
            } catch (Exception e) {
                System.out.println("\nError reading server message: " + e.getMessage());
            }
        }
    }
}