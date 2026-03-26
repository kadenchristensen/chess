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

    private List<GameData> lastListedGames = new ArrayList<>();

    // ANSI colors
    private static final String RESET = "\u001B[0m";
    private static final String BLACK_TEXT = "\u001B[30m";
    private static final String RED_TEXT = "\u001B[31m";
    private static final String BLUE_TEXT = "\u001B[34m";
    private static final String WHITE_TEXT = "\u001B[37m";

    private static final String BLACK_BG = "\u001B[40m";
    private static final String LIGHT_BG = "\u001B[47m";
    private static final String DARK_BG = "\u001B[100m";

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
                if (!loggedIn) {
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
            }
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
            if (lastListedGames.isEmpty()) {
                System.out.println("Error: no listed games found");
                return;
            }

            System.out.print("Enter game number: ");
            int choice;
            try {
                choice = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Error: invalid game number");
                return;
            }

            if (choice < 1 || choice > lastListedGames.size()) {
                System.out.println("Error: invalid game number");
                return;
            }

            System.out.print("Enter color (WHITE or BLACK): ");
            String color = scanner.nextLine().trim().toUpperCase();

            if (!color.equals("WHITE") && !color.equals("BLACK")) {
                System.out.println("Error: invalid color");
                return;
            }

            GameData game = lastListedGames.get(choice - 1);

            facade.joinGame(color, game.gameID(), authToken);

            System.out.println("Joined game: " + game.gameName() + " as " + color);

            drawBoard(color.equals("BLACK"));

        } catch (Exception e) {
            System.out.println("Error: " + friendlyMessage(e));
        }
    }

    private void observeGame() {
        try {
            if (lastListedGames.isEmpty()) {
                System.out.println("Error: no listed games found");
                return;
            }

            System.out.print("Enter game number: ");
            int choice;
            try {
                choice = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Error: invalid game number");
                return;
            }

            if (choice < 1 || choice > lastListedGames.size()) {
                System.out.println("Error: invalid game number");
                return;
            }

            GameData game = lastListedGames.get(choice - 1);

            System.out.println("Observing game: " + game.gameName());

            drawBoard(false);

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

        if (blackPerspective) {
            printBlackBoard(board);
        } else {
            printWhiteBoard(board);
        }
    }

    private void printWhiteBoard(String[][] board) {
        System.out.println();
        System.out.println("    a  b  c  d  e  f  g  h");

        for (int row = 0; row < 8; row++) {
            int displayRow = 8 - row;
            System.out.print(" " + displayRow + " ");

            for (int col = 0; col < 8; col++) {
                printSquare(board[row][col]);
            }

            System.out.println(" " + displayRow);
        }

        System.out.println("    a  b  c  d  e  f  g  h");
        System.out.println();
    }

    private void printBlackBoard(String[][] board) {
        System.out.println();
        System.out.println("    h  g  f  e  d  c  b  a");

        for (int row = 7; row >= 0; row--) {
            int displayRow = 8 - row;
            System.out.print(" " + displayRow + " ");

            for (int col = 7; col >= 0; col--) {
                printSquare(board[row][col]);
            }

            System.out.println(" " + displayRow);
        }

        System.out.println("    h  g  f  e  d  c  b  a");
        System.out.println();
    }

    private void printSquare(String piece) {
        if (piece.equals(" ")) {
            System.out.print(" . ");
        } else {
            System.out.print(" " + piece + " ");
        }
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
}