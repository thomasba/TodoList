package de.t_battermann.dhbw.todolist;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * This class contains a todo list with all its items.
 */
public class TodoList {
	private String uuid = UUID.randomUUID().toString();
	private List<Todo> todos = new LinkedList<>();
	private String name;
	private boolean changeable;

	/**
	 * Instantiates a new Todo list.
	 *
	 * @param name the name
	 */
	public TodoList(String name) {
		this.changeable = true;
		this.name = name;
	}

	/**
	 * Instantiates a new Todo list.
	 *
	 * @param name       the name
	 * @param changeable Can the name be changed?
	 */
	public TodoList(String name, boolean changeable) {
		this.changeable = changeable;
		this.name = name;
		this.initList();
	}

	/**
	 * Instantiates a new Todo list.
	 *
	 * @param uuid       the uuid
	 * @param name       the name
	 * @param changeable Can the name be changed?
	 */
	protected TodoList(String uuid, String name, boolean changeable) {
		this.uuid = uuid;
		this.todos = new LinkedList<>();
		this.name = name;
		this.changeable = changeable;
	}

	private void initList() {
		this.todos.add(new Todo("Start using your TodoList", "Add, delete and modify entries."));
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
	 * Gets name.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets name.
	 *
	 * @param name the name
	 */
	public void setName(String name) {
		if (this.isChangeable())
			this.name = name;
	}

	/**
	 * Gets todos.
	 *
	 * @return the todos
	 */
	public List<Todo> getTodos() {
		return todos;
	}

	/**
	 * Add todo.
	 *
	 * @param todo the todo
	 * @return the boolean
	 */
	public boolean addTodo(Todo todo) {
		if (todos.contains(todo)) {
			return false;
		}
		todos.add(todo);
		return true;
	}

	/**
	 * Delete a todo item
	 *
	 * @param todo The Item to be deleted
	 */
	public void deleteTodo(Todo todo) {
		if (todos.contains(todo)) {
			todos.remove(todo);
		}
	}

	/**
	 * Is changeable.
	 *
	 * @return true if the name can be changed
	 */
	public boolean isChangeable() {
		return changeable;
	}

	@Override
	public String toString() {
		return this.getName();
	}
}
