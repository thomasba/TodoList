package de.t_battermann.dhbw.todolist;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TreeMap;

/**
 * This class implement the ExportHandler interface. It converts the data to XML and vice versa.
 */
public class XMLHandler implements ExportHandler {
	/**
	 * Convert a map containing the users to a DOMSource containing XML
	 *
	 * @param users The users map
	 * @return DOMSource containing XML
	 */
	private DOMSource doExport(Map<String, User> users) {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("todolistapp");
			doc.appendChild(rootElement);

			for (User userEntry : users.values()) {
				Element user = doc.createElement("user");

				Element username = doc.createElement("username");
				username.appendChild(doc.createTextNode(userEntry.getUsername()));
				user.appendChild(username);

				Element password = doc.createElement("password");
				password.appendChild(doc.createTextNode(userEntry.getPassword()));
				user.appendChild(password);

				Element password_salt = doc.createElement("uuid");
				password_salt.appendChild(doc.createTextNode(userEntry.getUuid()));
				user.appendChild(password_salt);

				Element email = doc.createElement("email");
				email.appendChild(doc.createTextNode(userEntry.getEmail()));
				user.appendChild(email);

				for (TodoList todoListEntry : userEntry.getTodoLists()) {
					Element list = doc.createElement("TodoList");
					list.setAttribute("changeable", todoListEntry.isChangeable() ? "true" : "false");

					Element title = doc.createElement("name");
					title.appendChild(doc.createTextNode(todoListEntry.getName()));
					list.appendChild(title);

					Element uuid = doc.createElement("uuid");
					uuid.appendChild(doc.createTextNode(todoListEntry.getUuid()));
					list.appendChild(uuid);

					for (Todo entry : todoListEntry.getTodos()) {
						SimpleDateFormat format = new SimpleDateFormat();
						format.applyPattern("yyyyMMdd'T'HH:mm:ssZ");
						Element todo = doc.createElement("item");
						todo.setAttribute("prio", entry.isPrio() ? "true" : "false");
						todo.setAttribute("done", entry.isDone() ? "true" : "false");

						Element todoTitle = doc.createElement("title");
						todoTitle.appendChild(doc.createTextNode(entry.getTitle()));
						todo.appendChild(todoTitle);

						Element todoUuid = doc.createElement("uuid");
						todoUuid.appendChild(doc.createTextNode(entry.getUuid()));
						todo.appendChild(todoUuid);

						Element comment = doc.createElement("comment");
						comment.appendChild(doc.createTextNode(entry.getComment()));
						todo.appendChild(comment);

						if (entry.getDueDate() != null) {
							Element duedate = doc.createElement("duedate");
							duedate.appendChild(doc.createTextNode(format.format(entry.getDueDate().getTime())));
							todo.appendChild(duedate);
						} // if duedate
						list.appendChild(todo);
					} // for todos
					user.appendChild(list);
				} // for todoLists
				rootElement.appendChild(user);
			} // for users

			return new DOMSource(doc);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void exportToFile(Map<String, User> users, File file) {
		try {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = this.doExport(users);
			StreamResult result = new StreamResult(file);
			transformer.transform(source, result);
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}

	public String exportToString(Map<String, User> users) {
		try {
			StringWriter sw = new StringWriter();
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = this.doExport(users);
			transformer.transform(source, new StreamResult(sw));
			return sw.toString();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
			return "";
		} catch (TransformerException e) {
			e.printStackTrace();
			return "";
		}
	}

	/**
	 * Get a string-element from a node
	 *
	 * @param node The node to be searched in
	 * @param name The elements name
	 * @return the desired string or empty string if not found
	 */
	private String elementGetString(Element node, String name) {
		NodeList nodes = node.getElementsByTagName(name);
		if (nodes.getLength() >= 1) {
			return nodes.item(0).getTextContent();
		}
		return "";
	}

	/**
	 * Get a boolean value from a attribute
	 *
	 * @param node         The node to be searched in
	 * @param name         The attributes name
	 * @param defaultValue if the attribute is not set use this value
	 * @return either the value of the attribute or the default value if attribute not found
	 */
	private boolean elementGetBool(Element node, String name, boolean defaultValue) {
		if (node.hasAttribute(name)) {
			return node.getAttribute(name).compareTo("true") == 0;
		}
		return defaultValue;
	}

	/**
	 * Get a date from a element
	 *
	 * @param node The node to be searched in
	 * @param name The elements name
	 * @return Either the date (if found) or null
	 */
	private Calendar elementGetDate(Element node, String name) {
		SimpleDateFormat format = new SimpleDateFormat();
		format.applyPattern("yyyyMMdd'T'HH:mm:ssZ");
		NodeList nodes = node.getElementsByTagName(name);
		if (nodes.getLength() >= 1) {
			try {
				Calendar r = new GregorianCalendar();
				r.setTime(format.parse(nodes.item(0).getTextContent()));
				return r;
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * Helper function to convert the xml to a map containing the user data
	 *
	 * @param stream InputStream containing XML data
	 * @return The user object
	 * @throws IOException
	 * @throws InvalidDataException
	 */
	private Map<String, User> doImport(InputStream stream) throws IOException, InvalidDataException {
		// Temporary variables
		TreeMap<String, User> users = new TreeMap<>();
		User user;
		TodoList todoList;
		Todo todo;
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			Document doc = docBuilder.parse(stream);
			doc.getDocumentElement().normalize();

			// the actual import ...
			if (!"todolistapp".equals(doc.getDocumentElement().getNodeName())) {
				throw new InvalidDataException("Expected 'todolistapp' as root node");
			}

			NodeList nUsers = doc.getElementsByTagName("user");
			for (int i = 0; i < nUsers.getLength(); i++) {
				Node nUser = nUsers.item(i);
				if (nUser.getNodeType() == Node.ELEMENT_NODE) {
					Element eUser = (Element) nUser;
					user = new User(
							this.elementGetString(eUser, "uuid"),
							this.elementGetString(eUser, "username"),
							this.elementGetString(eUser, "password"),
							this.elementGetString(eUser, "email")
					);
					NodeList nTodoLists = eUser.getElementsByTagName("TodoList");
					for (int j = 0; j < nTodoLists.getLength(); j++) {
						Node nTodoList = nTodoLists.item(j);
						if (nTodoList.getNodeType() == Node.ELEMENT_NODE) {
							Element eTodoList = (Element) nTodoList;
							todoList = new TodoList(
									this.elementGetString(eTodoList, "uuid"),
									this.elementGetString(eTodoList, "name"),
									this.elementGetBool(eTodoList, "changeable", true)
							);
							NodeList nTodos = eTodoList.getElementsByTagName("item");
							for (int k = 0; k < nTodos.getLength(); k++) {
								Node nTodo = nTodos.item(k);
								if (nTodo.getNodeType() == Node.ELEMENT_NODE) {
									Element eTodo = (Element) nTodo;
									// String uuid, String title, String comment, Calendar dueDate, boolean done, boolean prio
									todo = new Todo(
											this.elementGetString(eTodo, "uuid"),
											this.elementGetString(eTodo, "title"),
											this.elementGetString(eTodo, "comment"),
											this.elementGetDate(eTodo, "duedate"),
											this.elementGetBool(eTodo, "done", false),
											this.elementGetBool(eTodo, "prio", false)
									);
									todoList.addTodo(todo);
								}
							}
							user.addTodoList(todoList);
						}
					}
					users.put(user.getUsername(), user);
				}
			}


		} catch (ParserConfigurationException | SAXException e) {
			e.printStackTrace();
		}
		return users;
	}

	public Map<String, User> importFromFile(File file) throws IOException, InvalidDataException {
		InputStream inputStream = new FileInputStream(file);
		return this.doImport(inputStream);
	}

	public Map<String, User> importFromString(String str) throws InvalidDataException {
		try {
			InputStream inputStream = new ByteArrayInputStream(str.getBytes(Charset.forName("UTF-8")));
			return this.doImport(inputStream);
		} catch (IOException e) {
			e.printStackTrace();
			return new TreeMap<>();
		}
	}
}
