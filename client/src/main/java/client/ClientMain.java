package client;

import model.GameData;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ClientMain {

    private final Scanner scanner = new Scanner(System.in);
    private final ServerFacade facade;

    private boolean running = true;
    private boolean loggedIn = false;
    private String authToken = null;
    private String username = null;

    // Stores the last list of games shown to the user
    private List<GameData> lastListedGames = new ArrayList<>();

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
        System.out.println("Connected to " + getServerLabel());

        while (running) {
            try {
                if (!loggedIn) {
                    System.out.print("\n[LOGGED_OUT] >>> ");
                    String input = scanner.nextLine().trim().toLowerCase();
                    evalLoggedOut(input);
                } else {
                    System.out.print("\n[LOGGED_IN] >>> ");
                    String input = scanner.nextLine().trim().toLowerCase();
                    evalLoggedIn(input);
                }
            } catch (Exception e) {
                System.out.println("Error: " + friendlyMessage(e));
            }
        }
    }

    private void evalLoggedOut(String input) {
        switch (input) {
            case "help" -> System.out.println(loggedOutHelp());
            case "quit" -> {
                System.out.println("Goodbye");
                running = false;
            }
            case "register" -> register();
            case "login" -> login();
            default -> System.out.println("Unknown command. Type 'help' to see options.");
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
            }
            default -> System.out.println("Unknown command. Type 'help' to see options.");
        }
    }

    private void register() {
        try {
            System.out.print("username: ");
            String username = scanner.nextLine().trim();

            System.out.print("password: ");
            String password = scanner.nextLine().trim();

            System.out.print("email: ");
            String email = scanner.nextLine().trim();

            var auth = facade.register(username, password, email);
            this.authToken = auth.authToken();
            this.username = auth.username();
            this.loggedIn = true;

            System.out.println("Successfully registered and logged in as " + this.username);
            System.out.println(loggedInHelp());
        } catch (Exception e) {
            System.out.println("Error: " + friendlyMessage(e));
        }
    }

    private void login() {
        try {
            System.out.print("username: ");
            String username = scanner.nextLine().trim();

            System.out.print("password: ");
            String password = scanner.nextLine().trim();

            var auth = facade.login(username, password);
            this.authToken = auth.authToken();
            this.username = auth.username();
            this.loggedIn = true;

            System.out.println("Successfully logged in as " + this.username);
            System.out.println(loggedInHelp());
        } catch (Exception e) {
            System.out.println("Error: " + friendlyMessage(e));
        }
    }

    private void logout() {
        try {
            facade.logout(authToken);
            authToken = null;
            username = null;
            loggedIn = false;
            lastListedGames.clear();
            System.out.println("Logged out successfully.");
            System.out.println(loggedOutHelp());
        } catch (Exception e) {
            System.out.println("Error: " + friendlyMessage(e));
        }
    }

    private void createGame() {
        try {
            System.out.print("game name: ");
            String gameName = scanner.nextLine().trim();

            var result = facade.createGame(gameName, authToken);
            System.out.println("Game created successfully. Game ID: " + result.gameID());
        } catch (Exception e) {
            System.out.println("Error: " + friendlyMessage(e));
        }
    }

    private void listGames() {
        try {
            var result = facade.listGames(authToken);

            lastListedGames = new ArrayList<>();
            if (result.games() != null) {
                lastListedGames.addAll(result.games());
            }

            if (lastListedGames.isEmpty()) {
                System.out.println("No games found.");
                return;
            }

            int index = 1;
            for (var game : lastListedGames) {
                String white = (game.whiteUsername() == null) ? "(open)" : game.whiteUsername();
                String black = (game.blackUsername() == null) ? "(open)" : game.blackUsername();

                System.out.printf("%d. %s | White: %s | Black: %s%n",
                        index++, game.gameName(), white, black);
            }
        } catch (Exception e) {
            System.out.println("Error: " + friendlyMessage(e));
        }
    }

    private void playGame() {
        try {
            if (lastListedGames.isEmpty()) {
                System.out.println("No previously listed games. Use 'list games' first.");
                return;
            }

            System.out.print("Enter game number: ");
            int selection = Integer.parseInt(scanner.nextLine().trim());

            if (selection < 1 || selection > lastListedGames.size()) {
                System.out.println("Invalid game number.");
                return;
            }

            GameData chosenGame = lastListedGames.get(selection - 1);

            System.out.print("Enter color (WHITE or BLACK): ");
            String color = scanner.nextLine().trim().toUpperCase();

            if (!color.equals("WHITE") && !color.equals("BLACK")) {
                System.out.println("Invalid color. Choose WHITE or BLACK.");
                return;
            }

            facade.joinGame(color, chosenGame.gameID(), authToken);
            System.out.println("Joined game: " + chosenGame.gameName() + " as " + color);

            if (color.equals("BLACK")) {
                drawBoard(true);
            } else {
                drawBoard(false);
            }

        } catch (NumberFormatException e) {
            System.out.println("Error: please enter a valid number.");
        } catch (Exception e) {
            System.out.println("Error: " + friendlyMessage(e));
        }
    }

    private void observeGame() {
        try {
            if (lastListedGames.isEmpty()) {
                System.out.println("No previously listed games. Use 'list games' first.");
                return;
            }

            System.out.print("Enter game number: ");
            int selection = Integer.parseInt(scanner.nextLine().trim());

            if (selection < 1 || selection > lastListedGames.size()) {
                System.out.println("Invalid game number.");
                return;
            }

            GameData chosenGame = lastListedGames.get(selection - 1);
            System.out.println("Observing game: " + chosenGame.gameName());

            drawBoard(false);

        } catch (NumberFormatException e) {
            System.out.println("Error: please enter a valid number.");
        } catch (Exception e) {
            System.out.println("Error: " + friendlyMessage(e));
        }
    }

    private void drawBoard(boolean blackPerspective) {
        String[][] board = {
                {"r", "n", "b", "q", "k", "b", "n", "r"},
                {"p", "p", "p", "p", "p", "p", "p", "p"},
                {" ", " ", " ", " ", " ", " ", " ", " "},
                {" ", " ", " ", " ", " ", " ", " ", " "},
                {" ", " ", " ", " ", " ", " ", " ", " "},
                {" ", " ", " ", " ", " ", " ", " ", " "},
                {"P", "P", "P", "P", "P", "P", "P", "P"},
                {"R", "N", "B", "Q", "K", "B", "N", "R"}
        };

        if (!blackPerspective) {
            printWhiteBoard(board);
        } else {
            printBlackBoard(board);
        }
    }

    private void printWhiteBoard(String[][] board) {
        System.out.println();
        System.out.println("    a   b   c   d   e   f   g   h");
        for (int row = 7; row >= 0; row--) {
            System.out.print(" " + (row + 1) + " ");
            for (int col = 0; col < 8; col++) {
                String piece = board[row][col];
                if (piece.equals(" ")) {
                    System.out.print(" . ");
                } else {
                    System.out.print(" " + piece + " ");
                }
            }
            System.out.println(" " + (row + 1));
        }
        System.out.println("    a   b   c   d   e   f   g   h");
        System.out.println();
    }

    private void printBlackBoard(String[][] board) {
        System.out.println();
        System.out.println("    h   g   f   e   d   c   b   a");
        for (int row = 0; row < 8; row++) {
            System.out.print(" " + (row + 1) + " ");
            for (int col = 7; col >= 0; col--) {
                String piece = board[row][col];
                if (piece.equals(" ")) {
                    System.out.print(" . ");
                } else {
                    System.out.print(" " + piece + " ");
                }
            }
            System.out.println(" " + (row + 1));
        }
        System.out.println("    h   g   f   e   d   c   b   a");
        System.out.println();
    }

    private String getServerLabel() {
        return "server";
    }

    private String friendlyMessage(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            return "operation failed";
        }

        String lower = msg.toLowerCase();

        if (lower.contains("connection refused") || lower.contains("failed to connect")) {
            return "could not connect to the server";
        }
        if (lower.contains("already taken")) {
            return "that username is already taken";
        }
        if (lower.contains("unauthorized")) {
            return "username/password or authorization was rejected";
        }
        if (lower.contains("bad request")) {
            return "the server rejected the request";
        }

        return msg;
    }

    private String loggedOutHelp() {
        return """
                Commands:
                  help      - show available commands
                  login     - log in to an existing account
                  register  - create a new account
                  quit      - exit the program
                """;
    }

    private String loggedInHelp() {
        return """
                Commands:
                  help          - show available commands
                  logout        - log out
                  create game   - create a new game
                  list games    - list current games
                  play game     - join a game as white or black
                  observe game  - observe an existing game
                  quit          - exit the program
                """;
    }
}