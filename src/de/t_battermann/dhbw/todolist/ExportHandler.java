package de.t_battermann.dhbw.todolist;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Interface for exporting/saving the data
 */
public interface ExportHandler {
	/**
	 * Export to file.
	 *
	 * @param users    A Map containing the users
	 * @param filename Path to the file the data should be saved to
	 */
	void exportToFile(Map<String, User> users, File filename) throws IOException;

	/**
	 * Export to string.
	 *
	 * @param users the users
	 * @return A String containing the data
	 */
	String exportToString(Map<String, User> users);

	/**
	 * Import from file.
	 *
	 * @param filename Path to the saved data
	 * @return A Map containing the Users, username as index
	 */
	Map<String, User> importFromFile(File filename) throws IOException, InvalidDataException;

	/**
	 * Import from string.
	 *
	 * @param str A String containing the Data
	 * @return A Map containing the User, username as index
	 */
	Map<String, User> importFromString(String str) throws InvalidDataException;
}
