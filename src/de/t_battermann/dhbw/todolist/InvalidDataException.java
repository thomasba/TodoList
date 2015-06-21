package de.t_battermann.dhbw.todolist;

/**
 * This exception is thrown when the imported data is not as expected.
 */
public class InvalidDataException extends Exception {
	public InvalidDataException() {
	}

	public InvalidDataException(String message) {
		super(message);
	}
}
