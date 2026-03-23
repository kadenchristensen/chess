package service;

import model.UserData;
import model.AuthData;

import java.util.UUID;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import org.mindrot.jbcrypt.BCrypt;

public class UserService {

    private final DataAccess dataAccess;

    public UserService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public AuthData register(RegisterRequest request) throws DataAccessException {

        if (request == null ||
                request.username() == null ||
                request.password() == null ||
                request.email() == null) {
            throw new DataAccessException("bad request");
        }

        if (dataAccess.getUser(request.username()) != null) {
            throw new DataAccessException("already taken");
        }

        String hashedPassword = BCrypt.hashpw(request.password(), BCrypt.gensalt());

        UserData user = new UserData(
                request.username(),
                hashedPassword,
                request.email()
        );

        dataAccess.createUser(user);

        String token = UUID.randomUUID().toString();
        AuthData auth = new AuthData(token, request.username());

        dataAccess.createAuth(auth);

        return auth;
    }

    public AuthData login(LoginRequest request) throws DataAccessException {

        if (request == null ||
                request.username() == null ||
                request.password() == null) {
            throw new DataAccessException("bad request");
        }

        UserData user = dataAccess.getUser(request.username());

        if (user == null || !BCrypt.checkpw(request.password(), user.password())) {
            throw new DataAccessException("unauthorized");
        }

        String token = UUID.randomUUID().toString();
        AuthData auth = new AuthData(token, request.username());

        dataAccess.createAuth(auth);

        return auth;
    }

    public void logout(LogoutRequest request) throws DataAccessException {
        if (request == null || request.authToken() == null) {
            throw new DataAccessException("bad request");
        }

        if (dataAccess.getAuth(request.authToken()) == null) {
            throw new DataAccessException("unauthorized");
        }

        dataAccess.deleteAuth(request.authToken());
    }
}