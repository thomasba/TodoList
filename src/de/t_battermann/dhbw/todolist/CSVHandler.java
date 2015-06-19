package de.t_battermann.dhbw.todolist;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Export the user data in a CSV file
 */
public class CSVHandler implements ExportHandler {
    @Override
    public void exportToFile(Map<String, User> users, File filename) throws IOException {
        // TODO: implement method
    }

    @Override
    public String exportToString(Map<String, User> users) {
        // TODO: implement method
        return null;
    }

    @Override
    public Map<String, User> importFromFile(File filename) throws IOException, InvalidDataException {
        // TODO: implement method
        return null;
    }

    @Override
    public Map<String, User> importFromString(String str) throws InvalidDataException {
        // TODO: implement method
        return null;
    }
}
