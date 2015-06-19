package de.t_battermann.dhbw.todolist;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;

/**
 * This class represents a todo item containing all the data.
 */
public class Todo {
	private String uuid;
	private String title = "No title";
	private boolean done = false;
	private boolean prio = false;
	private String comment = "";
	private Calendar dueDate = null;

	/**
	 * Instantiates a new empty Todo item.
	 */
	public Todo() {
		this.uuid = UUID.randomUUID().toString();
	}

	/**
	 * Instantiates a new Todo.
	 *
	 * @param uuid	the uuid
	 * @param title   the title
	 * @param comment the comment
	 * @param dueDate the due date
	 * @param done	Is the item done?
	 * @param prio	Has the item high priority?
	 */
	protected Todo(String uuid, String title, String comment, Calendar dueDate, boolean done, boolean prio) {
		this.uuid = uuid;
		this.title = title;
		this.comment = comment;
		this.dueDate = dueDate;
		this.done = done;
		this.prio = prio;
	}

	/**
	 * Instantiates a new Todo.
	 *
	 * @param title   the title
	 * @param comment the comment
	 */
	public Todo(String title, String comment) {
		this.uuid = UUID.randomUUID().toString();
		this.title = title;
		this.comment = comment;
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
	 * Gets title.
	 *
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets title.
	 *
	 * @param title the title
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Is it done?
	 *
	 * @return the boolean
	 */
	public boolean isDone() {
		return done;
	}

	/**
	 * Sets done.
	 *
	 * @param done the done
	 */
	public void setDone(boolean done) {
		this.done = done;
	}

	/**
	 * Is prio.
	 *
	 * @return the boolean
	 */
	public boolean isPrio() {
		return prio;
	}

	/**
	 * Sets prio.
	 *
	 * @param prio the prio
	 */
	public void setPrio(boolean prio) {
		this.prio = prio;
	}

	/**
	 * Gets comment.
	 *
	 * @return the comment
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * Sets comment.
	 *
	 * @param comment the comment
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}

	/**
	 * Gets due date.
	 *
	 * @return the due date
	 */
	public Calendar getDueDate() {
		return dueDate;
	}

	/**
	 * Sets due date.
	 *
	 * @param dueDate the due date
	 */
	public void setDueDate(Calendar dueDate) {
		this.dueDate = dueDate;
	}

	public String getTime() {
		SimpleDateFormat format = new SimpleDateFormat();
		format.applyPattern("yyyyMMdd'T'HH:mm:ssZ");
		return this.getDueDate() != null ? format.format(this.getDueDate().getTime()) : "00:00";
	}

	@Override
	public String toString() {
		return this.getTitle();
	}
}
