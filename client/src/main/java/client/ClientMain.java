package client;

import java.util.Scanner;

public class ClientMain {

    private final Scanner scanner = new Scanner(System.in);
    private final ServerFacade facade = new ServerFacade("http://localhost:8080");

    private boolean running = true;
    private boolean loggedIn = false;
    private String authToken = null;
    private String username = null;

    public static void main(String[] args) {
        new ClientMain().run();
    }

    public void run() {
        System.out.println("Welcome to Chess");

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
                System.out.println("Error: " + e.getMessage());
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
            case "play game" -> System.out.println("Play game not implemented yet.");
            case "observe game" -> System.out.println("Observe game not implemented yet.");
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
            System.out.println("Error: unable to register");
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
            System.out.println("Error: unable to login");
        }
    }

    private void logout() {
        try {
            facade.logout(authToken);
            authToken = null;
            username = null;
            loggedIn = false;
            System.out.println("Logged out successfully.");
            System.out.println(loggedOutHelp());
        } catch (Exception e) {
            System.out.println("Error: unable to logout");
        }
    }

    private void createGame() {
        try {
            System.out.print("game name: ");
            String gameName = scanner.nextLine().trim();

            var result = facade.createGame(gameName, authToken);
            System.out.println("Game created successfully. Game number: " + result.gameID());
        } catch (Exception e) {
            System.out.println("Error: unable to create game");
        }
    }

    private void listGames() {
        try {
            var result = facade.listGames(authToken);

            if (result.games() == null || result.games().isEmpty()) {
                System.out.println("No games found.");
                return;
            }

            int index = 1;
            for (var game : result.games()) {
                String white = (game.whiteUsername() == null) ? "(open)" : game.whiteUsername();
                String black = (game.blackUsername() == null) ? "(open)" : game.blackUsername();

                System.out.printf("%d. %s | White: %s | Black: %s%n",
                        index++, game.gameName(), white, black);
            }
        } catch (Exception e) {
            System.out.println("Error: unable to list games");
        }
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