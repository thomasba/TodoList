package de.t_battermann.dhbw.todolist;

import com.opencsv.CSVParser;
import com.opencsv.CSVWriter;

import java.io.*;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TreeMap;

/**
 * Export the user data in a CSV file
 *
 * File format:
 * USER,uuid,username,password,email
 * TODOLIST,username,uuid,name,changeable
 * TO DO,username,todolist,uuid,title,comment,dueDate,done,prio
 */
public class CSVHandler implements ExportHandler {
	/**
	 * Convert a map containing the users to a DOMSource containing XML
	 *
	 * @param users The users map
	 * @return String containing CSV
	 */
	private String doExport(Map<String, User> users) {
		StringWriter sw = new StringWriter();
		CSVWriter w = new CSVWriter(sw);
		StringWriter s = new StringWriter();
		// date formatter
		SimpleDateFormat format = new SimpleDateFormat();
		format.applyPattern("yyyyMMdd'T'HH:mm:ssZ");
		for( User user: users.values()) {
			String userData[] = { "USER", user.getUuid(), user.getUsername(), user.getPassword(), user.getEmail() };
			w.writeNext(userData);
			for( TodoList list: user.getTodoLists() ) {
				String listData[] = { "TODOLIST", user.getUsername(), list.getUuid(), list.getName(), list.isChangeable() ? "true" : "false" };
				w.writeNext(listData);
				for( Todo todo: list.getTodos() ) {
					String todoData[] = { "TODO", user.getUsername(), list.getName(), todo.getUuid(), todo.getTitle(), todo.getComment(),
							todo.getDueDate() == null ? "0" : format.format(todo.getDueDate().getTime()), todo.isDone() ? "true" : "false", todo.isPrio() ? "true" : "false" };
					w.writeNext(todoData);
				}
			}
		}
		return sw.toString();
	}

	@Override
	public void exportToFile(Map<String, User> users, File file) throws IOException {
		FileWriter fw = new FileWriter(file);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write( doExport(users) );
		bw.close();
	}

	@Override
	public String exportToString(Map<String, User> users) {
		return doExport(users);
	}

	/**
	 * Get a date from a element
	 *
	 * @param date The String containing the date
	 * @return Either the date (if found) or null
	 */
	private Calendar stringToDate(String date) {
		if ( date.equals("0") )
			return null;
		SimpleDateFormat format = new SimpleDateFormat();
		format.applyPattern("yyyyMMdd'T'HH:mm:ssZ");
		try {
			Calendar r = new GregorianCalendar();
			r.setTime(format.parse(date));
			return r;
		} catch (ParseException e) {
			e.printStackTrace();
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
		BufferedReader r = new BufferedReader(new InputStreamReader(stream));
		CSVParser c = new CSVParser();
		String csv, line[];
		Map<String, User> users = new TreeMap<>();
		while((csv = r.readLine()) != null) {
			line = c.parseLine(csv);
			switch( line[0] ) {
				case "USER":
					if ( line.length != 5 ) {
						throw new InvalidDataException("Invalid user: line doesn’t contain 5 elements");
					}else if ( users.containsKey( line[2] ) ) {
						throw new InvalidDataException("Invalid user: duplicate User!");
					}else {
						User u = new User(line[1], line[2], line[3], line[4]);
						users.put(line[2], u);
					}
					break;
				case "TODOLIST":
					if ( line.length != 5 ) {
						throw new InvalidDataException("Invalid TodoList: line doesn’t contain 5 elements");
					} else if ( !users.containsKey(line[1]) ) {
						throw new InvalidDataException("Invalid TodoList: User not found!");
					} else if ( users.get(line[1]).getTodoList(line[3]) != null) {
						throw new InvalidDataException("Invalid TodoList: duplicate TodoList!");
					} else {
						TodoList t = new TodoList(line[2],line[3],line[4].equals("true"));
						users.get(line[1]).addTodoList(t);
					}
					break;
				case "TODO":
					if ( line.length != 9 ) {
						throw new InvalidDataException("Invalid Todo: line doesn’t contain 9 elements" + line.length);
					} else if ( !users.containsKey(line[1]) ) {
						throw new InvalidDataException("Invalid Todo: User not found!");
					} else if ( users.get(line[1]).getTodoList(line[2]) == null) {
						throw new InvalidDataException("Invalid Todo: TodoList not found!");
					} else {
						Todo t = new Todo( line[3], line[4], line[5], stringToDate(line[6]), line[7].equals("true"), line[8].equals("true") );
						users.get(line[1]).getTodoList(line[2]).addTodo(t);
					}
					break;
				default:
					throw new InvalidDataException("Unexpected line identifier.");
			}
		}
		return users;
	}

	@Override
	public Map<String, User> importFromFile(File file) throws IOException, InvalidDataException {
		InputStream inputStream = new FileInputStream(file);
		return this.doImport(inputStream);
	}

	@Override
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
