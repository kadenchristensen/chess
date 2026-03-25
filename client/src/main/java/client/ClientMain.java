package client;

import java.util.Scanner;

public class ClientMain {

    public static void main(String[] args) {
        new ClientMain().run();
    }

    public void run() {
        System.out.println("Welcome to Chess");
        System.out.println(helpText());

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.print("\n[LOGGED_OUT] >>> ");
            String input = scanner.nextLine().trim().toLowerCase();

            try {
                switch (input) {
                    case "help" -> System.out.println(helpText());
                    case "quit" -> {
                        System.out.println("Goodbye");
                        running = false;
                    }
                    case "login" -> System.out.println("Login not implemented yet.");
                    case "register" -> System.out.println("Register not implemented yet.");
                    default -> System.out.println("Unknown command. Type 'help' to see options.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private String helpText() {
        return """
                Commands:
                  help     - show available commands
                  login    - log in to an existing account
                  register - create a new account
                  quit     - exit the program
                """;
    }
}
