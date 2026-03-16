// ServerMain.java
package server;

public class ServerMain {
    public static void main(String[] args) {
        var server = new Server();
        server.run(8080);
    }
}