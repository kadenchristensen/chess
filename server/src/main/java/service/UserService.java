package service;

import model.UserData;
import model.AuthData;

import java.util.UUID;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;

public class UserService {

    private final DataAccess dataAccess;

    public UserService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }
    // register + login + logout will go here next


    public AuthData register(RegisterRequest request) throws DataAccessException {

        if (request == null ||
                request.username() == null ||
                request.password() == null ||
                request.email() == null) {
            throw new DataAccessException("bad request");
        }

        // check if user already exists
        if (dataAccess.getUser(request.username()) != null) {
            throw new DataAccessException("already taken");
        }

        // create user
        UserData user = new UserData(
                request.username(),
                request.password(),
                request.email()
        );

        dataAccess.createUser(user);

        // generate auth token
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

        if (user == null || !user.password().equals(request.password())) {
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

        // must exist to be valid
        if (dataAccess.getAuth(request.authToken()) == null) {
            throw new DataAccessException("unauthorized");
        }

        dataAccess.deleteAuth(request.authToken());
    }


}

