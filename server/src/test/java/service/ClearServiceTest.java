package service;

import dataaccess.MemoryDataAccess;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class ClearServiceTest {

    @Test
    void clearPositive() {
        var dao = new MemoryDataAccess();
        var service = new ClearService(dao);

        assertDoesNotThrow(service::clear);
    }
}