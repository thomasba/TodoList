package de.t_battermann.dhbw.todolist;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.validator.routines.EmailValidator;

import java.io.Serializable;
import java.util.*;

/**
 * This class contains all the users data.
 */
public class User implements Serializable {
	private String username;
	private String email;
	private String password;
	private String uuid = UUID.randomUUID().toString();
	private List<TodoList> todoLists;

	/**
	 * Instantiates a new User.
	 *
	 * @param username the username
	 * @param password the password
	 */
	public User(String username, String password) {
		this.username = username;
		this.password = hashPassword(password);
		this.email = "";

		todoLists = new LinkedList<>();
		TodoList tmp = new TodoList("Default", false);
		todoLists.add(tmp);
	}

	/**
	 * Instantiates a new User.
	 * Used to restore saved data
	 *
	 * @param uuid		   the uuid
	 * @param username	   the username
	 * @param hashedPassword the hashed password
	 * @param email		  the email
	 */
	protected User(String uuid, String username, String hashedPassword, String email) {
		this.username = username;
		this.uuid = uuid;
		this.password = hashedPassword;
		this.email = email;
		this.todoLists = new LinkedList<>();
	}

	/**
	 * Gets username.
	 *
	 * @return the username
	 */
	public String getUsername() {
		return this.username;
	}

	/**
	 * Gets password.
	 *
	 * @return the password
	 */
	protected String getPassword() {
		return this.password;
	}

	/**
	 * Gets uuid.
	 *
	 * @return the uuid
	 */
	public String getUuid() {
		return this.uuid;
	}

	/**
	 * Gets email.
	 *
	 * @return the email
	 */
	public String getEmail() {
		return this.email;
	}

	/**
	 * Gets todo list.
	 *
	 * @param name the name
	 * @return the todo list
	 */
	public TodoList getTodoList(String name) {
		for(TodoList l: todoLists)
			if(l.getName().equals(name))
				return l;
		System.out.println("TodoList not found: " + name);
		return null;
	}

	/**
	 * Gets todo lists.
	 *
	 * @return the todo lists
	 */
	public List<TodoList> getTodoLists() {
		return todoLists;
	}

	/**
	 * Check login data.
	 *
	 * @param password the password
	 * @return the boolean
	 */
	public boolean checkLoginData(String password) {
		System.out.println("pw:     " + password + " -> " + this.hashPassword(password));
		System.out.println("stored: " + this.password);
		return this.hashPassword(password).equals(this.password);
	}

	@Override
	public String toString() {
		return "Username: " + username + "\n"
				+ "eMail:	" + email + "\n";
	}

	/**
	 * Add a todo list.
	 *
	 * @param name the name for the new list
	 * @return false if a list with the given name already exists
	 */
	public boolean addTodoList(String name) {
		return this.addTodoList(new TodoList(name));
	}

	/**
	 * Add todo list.
	 *
	 * @param todoList the todo list
	 * @return false if a list with the given name already exists
	 */
	public boolean addTodoList(TodoList todoList) {
		if ( this.getTodoList( todoList.getName()) == null ) {
			this.todoLists.add(todoList);
			return true;
		}
		System.out.println("A TodoList named '" + todoList.getName() + "' already exists!");
		return false;
	}

	/**
	 * Update the users password
	 *
	 * @param password the password (cleartext)
	 */
	public void setPassword(String password) {
		this.password = hashPassword(password);
	}

	/**
	 * Sets email.
	 *
	 * @param email the email
	 * @return the email
	 */
	public boolean setEmail(String email) {
		if (User.checkEmail(email)) {
			this.email = email;
			return true;
		}
		System.out.println("Invalid eMail: '" + email + "'");
		return false;
	}

	/**
	 * Generate a salted hash
	 *
	 * @param password the password to salt and hash
	 * @return salted and hashed password
	 */
	private String hashPassword(String password) {
		return DigestUtils.sha256Hex(uuid + password);
	}

	/**
	 * Checks if eMail has correct syntax
	 * @param email string containing a eMail address
	 * @return true if valid syntax
	 */
	public static boolean checkEmail(String email) {
		return email.length() != 0 && EmailValidator.getInstance().isValid(email);
	}

	public static boolean checkUsername(String username) {
		return username.matches("[a-zA-Z0-9_-]{3,}");
	}
	public static boolean checkPassword(String password) {
		return password.length() > 6 && password.matches(".*[A-Z].*") && password.matches(".*[a-z].*") && password.matches(".*[0-9].*");
	}
}
