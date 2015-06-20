package de.t_battermann.dhbw.todolist;

import com.sun.javafx.collections.ObservableListWrapper;
import com.sun.javafx.collections.ObservableSequentialListWrapper;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public class Controller {
	private Map<String,User> users = null;
	private User currentUser = null;
	private ObservableList<TodoList> todoLists = null;
	private ObservableList<Todo> todos;
	private Todo currentTodo = null;
	private String filename = null;
	private Map<String,Scene> scenes = new HashMap<>();
	private Stage primaryStage;
	private String buttonAction = "new";

	public Controller(Stage primaryStage) {
		this.primaryStage = primaryStage;
		// Initialize needed scenes
		try {
			scenes.put("openFile", new Scene(FXMLLoader.load(getClass().getResource("openFile.fxml")),500,150));
			scenes.put("login", new Scene(FXMLLoader.load(getClass().getResource("login.fxml")),500,350));
			scenes.put("main", new Scene(FXMLLoader.load(getClass().getResource("main.fxml")),600,500));
			scenes.put("saveAs", new Scene(FXMLLoader.load(getClass().getResource("saveAs.fxml")),500,150));
		} catch (IOException e1) {
			this.printError("Could’t load a fxml file!");
			e1.printStackTrace();
		}
		showLoadFileDialog();
	}

	/**
	 * Initialize new empty
	 */
	public void initEmpty() {
		this.users = new TreeMap<>();
	}

	public void initFromFile(String filename) throws IOException, InvalidDataException {
		File f = new File(filename);
		if ( !f.isFile() || f.isDirectory() || !f.canRead()) {
			throw new IOException();
		}
		ExportHandler e;
		if ( filename.endsWith(".csv") ) {
			e = new CSVHandler();
		}else{
			e = new XMLHandler();
		}
		this.users = e.importFromFile(new File(filename));
		this.filename = filename;
	}

	public boolean export(String filename) {
		if ( filename != null) {
			File f = new File(filename);
			if ( f.isDirectory() ) {
				this.updateStatusLine("Couldn’t write to file '" + filename + "'");
				return false;
			}
			ExportHandler e;
			if ( filename.endsWith(".csv") ) {
				e = new CSVHandler();
			}else {
				e = new XMLHandler();
			}
			try {
				e.exportToFile(this.users, f);
			} catch (IOException e1) {
				this.updateStatusLine("Could’t write to file!");
				e1.printStackTrace();
				return false;
			}
			this.updateStatusLine("Saved data to'"+this.filename+"'");
			return true;
		}
		this.updateStatusLine("No filename given. Please choose one!");
		this.showSaveAs();
		return false;
	}

	public void showLoadFileDialog() {
		// log out (doesn't do anything if not logged in)
		this.users = null;
		this.currentUser = null;
		this.todoLists = null;
		this.todos = null;
		this.filename = null;
		// show dialog
		primaryStage.setTitle("TodoList :: Open database");
		primaryStage.setScene(scenes.get("openFile"));
		primaryStage.show();
		// register event handlers
		Button b = (Button) primaryStage.getScene().lookup("#openFileButton");
		b.setOnMouseReleased(event -> {
			Node n = primaryStage.getScene().lookup("#openFilePath");
			if ( n != null && n instanceof TextField ) {
				try {
					this.initFromFile(((TextField) n).getText());
					this.showLoginDialog();
				} catch (InvalidDataException | IOException e1) {
					this.printError("Can’t read file '" + this.filename + "'");
					e1.printStackTrace();
				}
			}else{
				this.printError("Didn’t find #openFilePath!");
			}
		});
		b = (Button) primaryStage.getScene().lookup("#openFileNew");
		b.setOnMouseReleased(event -> {
			this.initEmpty();
			this.showLoginDialog();
		});
	}

	private void showLoginDialog() {
		primaryStage.setTitle("TodoList :: Log in");
		primaryStage.setScene(scenes.get("login"));
		TitledPane a = (TitledPane) primaryStage.getScene().lookup(users.isEmpty() ? "#createNewUserPane" : "#loginPane");
		if ( a != null ) {
			a.setExpanded(true);
		}
		// Log in
		Node n = primaryStage.getScene().lookup("#loginButton");
		if ( n != null && n instanceof Button) {
			((Button) n).setOnAction(event -> {
				Label l = (Label) primaryStage.getScene().lookup("#labelHints");
				String name = null;
				String pass = null;
				Node m = primaryStage.getScene().lookup("#loginUsername");
				if (m != null && m instanceof TextField) {
					name = ((TextField) m).getText();
				} else {
					this.printError("'#loginUsername' not found!");
					return;
				}
				m = primaryStage.getScene().lookup("#loginPassword");
				if (m != null && m instanceof PasswordField) {
					pass = ((PasswordField) m).getText();
				}
				if (this.users.containsKey(name)) {
					this.currentUser = this.users.get(name);
					if (this.currentUser.checkLoginData(pass)) {
						this.todoLists = new ObservableListWrapper<>(currentUser.getTodoLists());
						this.showMainWindow();
					} else {
						this.currentUser = null;
						l.setText("Invalid credentials!");
					}
				} else {
					l.setText("Invalid credentials! 1");
				}
			});
		}else{
			this.printError("'#loginButton' not found!");
		}
		n = primaryStage.getScene().lookup("#registerButton");
		if ( n != null && n instanceof Button) {
			((Button) n).setOnAction(event -> {
				Label l = (Label) primaryStage.getScene().lookup("#labelHintsCreateNewUser");
				// username
				String username;
				Node no = primaryStage.getScene().lookup("#registerUsername");
				if (no != null && no instanceof TextField && User.checkUsername(((TextField) no).getText())) {
					username = ((TextField) no).getText();
				} else {
					l.setText("Invalid username! (Regex: [a-zA-Z0-9_-]{3,})");
					return;
				}
				if (users.containsKey(username)) {
					l.setText("Username already taken!");
					return;
				}
				// password
				String password;
				no = primaryStage.getScene().lookup("#registerPassword");
				if (no != null && no instanceof PasswordField && User.checkPassword(((PasswordField) no).getText())) {
					password = ((PasswordField) no).getText();
					no = primaryStage.getScene().lookup("#registerPasswordRepeat");
					if (no == null || !(no instanceof PasswordField) || !password.equals(((PasswordField) no).getText())) {
						l.setText("The passwords didn’t match!");
					}
				} else {
					l.setText("Password must be longer than 6 chars! And must contain numbers, lower and upper chars!");
					return;
				}
				// eMail
				String email;
				no = primaryStage.getScene().lookup("#registerEmail");
				if (no != null && no instanceof TextField && User.checkEmail(((TextField) no).getText())) {
					email = ((TextField) no).getText();
					no = primaryStage.getScene().lookup("#registerEmailRepeat");
					if (no == null || !(no instanceof TextField) || !email.equals(((TextField) no).getText())) {
						l.setText("The eMail addresses didn’t match!");
					}
				} else {
					l.setText("No valid eMail address given!");
					return;
				}
				User nu = new User(username, password);
				nu.setEmail(email);
				currentUser = nu;
				users.put(username, nu);
				this.todoLists = new ObservableSequentialListWrapper<>(currentUser.getTodoLists());
				// log in
				this.showMainWindow();
			});
		}else{
			this.printError("'#registerButton' not found!");
		}
	}

	private void showMainWindow() {
		primaryStage.setTitle("TodoList :: " + currentUser.getUsername() +  " > Default");
		primaryStage.setScene(scenes.get("main"));
		Node n = primaryStage.getScene().lookup("#todoLists");
		if ( n != null && n instanceof ListView) {
			ListView<TodoList> lv = (ListView<TodoList>) n;
			lv.setItems(this.todoLists);
			lv.scrollTo(currentUser.getTodoList("Default"));
			lv.getSelectionModel().selectedIndexProperty().addListener(event -> {
				this.updateSelectedTodoList();
			});
		}
		this.todos = new ObservableListWrapper<>(currentUser.getTodoList("Default").getTodos());
		n = primaryStage.getScene().lookup("#todos");
		if ( n != null && n instanceof ListView) {
			ListView<Todo> lv = (ListView<Todo>) n;
			lv.setItems(this.todos);
			lv.getSelectionModel().selectedIndexProperty().addListener(event -> {
				this.updateSelectedTodo();
			});
		}
		n = primaryStage.getScene().lookup("#menuSave");
		if ( n != null && n instanceof Button) {
			((Button) n).setOnAction(event -> {
				if (this.filename != null) {
					this.export(this.filename);
				} else {
					this.showSaveAs();
				}
			});
		}
		n = primaryStage.getScene().lookup("#menuSaveAs");
		if ( n != null && n instanceof Button ) {
			((Button) n).setOnAction(event -> this.showSaveAs());
		}
		n = primaryStage.getScene().lookup("#menuClose");
		if ( n != null && n instanceof Button ) {
			((Button) n).setOnAction(event -> Platform.exit());
		}
		this.primaryStage.setOnCloseRequest(event -> Platform.exit());
		n = primaryStage.getScene().lookup("#todoDetailSave");
		if ( n != null && n instanceof Button) {
			((Button) n).setOnAction(event -> this.saveTodoEntry());
		}
		n = primaryStage.getScene().lookup("#todoDetailDueDate");
		if (n != null && n instanceof CheckBox) {
			((CheckBox) n).setOnAction(event -> this.detailUpdateDueDatePicker());
		}
		// handle new  TodoList
		n = primaryStage.getScene().lookup("#todoListNew");
		if ( n != null && n instanceof Button) {
			((Button) n).setOnAction(event -> {
				this.buttonAction = "new";
				this.showTodoListEdit();
			});
		}
		// handle edit TodoList
		n = primaryStage.getScene().lookup("#todoListEdit");
		if ( n != null && n instanceof Button) {
			((Button) n).setOnAction(event -> {
				this.buttonAction = "edit";
				this.showTodoListEdit();
			});
		}
		// handle delete TodoList
		n = primaryStage.getScene().lookup("#todoListDelete");
		if( n != null && n instanceof Button) {
			((Button) n).setOnAction(event -> deleteTodoList());
		}
		// TODO
	}

	private void updateSelectedTodoList() {
		Node n = primaryStage.getScene().lookup("#todoLists");
		if ( n == null || !(n instanceof ListView) ) {
			return;
		}
		ListView l = (ListView) n;
		n = primaryStage.getScene().lookup("#todos");
		if ( n == null || !(n instanceof ListView)) {
			return;
		}
		ListView<Todo> lt = (ListView) n;
		if ( l.getSelectionModel().getSelectedItem() != null && l.getSelectionModel().getSelectedItem() instanceof TodoList ) {
			TodoList t = (TodoList) l.getSelectionModel().getSelectedItem();
			this.todos = new ObservableListWrapper<>(t.getTodos());
			lt.setItems(this.todos);
			lt.getSelectionModel().select(0);
			// update buttons :)
			n = primaryStage.getScene().lookup("#todoListEdit");
			if ( n!=null && n instanceof  Button) {
				n.setDisable(!t.isChangeable());
			}
			n = primaryStage.getScene().lookup("#todoListDelete");
			if (n!=null && n instanceof Button) {
				n.setDisable(!t.isChangeable());
			}
			// if there is no todo item, empty the currentTodo
			this.currentTodo = null;
		}
		updateSelectedTodo();
	}

	private void updateSelectedTodo() {
		Node n = primaryStage.getScene().lookup("#todos");
		if ( n != null && n instanceof ListView) {
			ListView<Todo> lv = (ListView<Todo>) n;
			if(lv.getSelectionModel().getSelectedItem() != null) {
				this.currentTodo = lv.getSelectionModel().getSelectedItem();
			}
		}
		// title
		n = primaryStage.getScene().lookup("#todoDetailTitle");
		if ( n == null || !(n instanceof TextField))
			return;
		((TextField) n).setText( this.currentTodo == null ? "" : this.currentTodo.getTitle() );
		// comment
		n = primaryStage.getScene().lookup("#todoDetailDescription");
		if ( n == null || !(n instanceof TextArea) )
			return;
		((TextArea) n).setText( this.currentTodo == null ? "" : this.currentTodo.getComment() );
		// if dueDate set:
		n = primaryStage.getScene().lookup("#todoDetailDueDate");
		if ( n == null || !(n instanceof CheckBox) )
			return;
		boolean dueDate = this.currentTodo != null && this.currentTodo.getDueDate() != null;
		((CheckBox) n).setSelected(dueDate);
		// datePicker
		n = primaryStage.getScene().lookup("#todoDetailDate");
		if( n == null || !(n instanceof DatePicker))
			return;
		if(dueDate) {
			((DatePicker) n).setValue( LocalDateTime.ofInstant(this.currentTodo.getDueDate().getTime().toInstant(), ZoneId.systemDefault()).toLocalDate() );
			n.setDisable(false);
		}else{
			((DatePicker) n).setValue(null);
			n.setDisable(true);
		}
		// time
		n = primaryStage.getScene().lookup("#todoDetailTime");
		if ( n == null || !(n instanceof TextField))
			return;
		if ( dueDate ) {
			((TextField) n).setText(this.currentTodo.getTime() );
			n.setDisable(false);
		}else{
			n.setDisable(true);
			((TextField) n).setText("00:00");
		}
	}

	private void updateStatusLine(String text) {
		Node n = primaryStage.getScene().lookup("#statusLine");
		if ( n != null && n instanceof Label) {
			((Label) n).setText(text);
		}else{
			this.printError("Couldn’t find status line!");
		}
	}

	private void showSaveAs() {
		Stage save = new Stage();
		save.setScene(scenes.get("saveAs"));
		save.setTitle("Save as ...");
		save.show();
		Button s = (Button) save.getScene().lookup("#save");
		if ( s != null)
			s.setOnAction(event -> {
				TextField f = (TextField) save.getScene().lookup("#filename");
				if (f != null) {
					File file = new File(f.getText());
					if (!file.isDirectory()) {
						if (this.filename == null)
							this.filename = f.getText();
						if (this.export(f.getText()))
							save.close();
					} else {
						this.updateStatusLine("Can’t write to file!");
					}
				}
			});
	}

	private void saveTodoEntry() {
		if ( this.currentTodo == null ) {
			this.updateStatusLine("No item selected!");
			return;
		}
		Node lv = primaryStage.getScene().lookup("#todos");
		if ( lv == null || !(lv instanceof ListView)) {
			this.updateStatusLine("Could’t get todo-ListView!");
			return;
		}
		// title
		Node n = primaryStage.getScene().lookup("#todoDetailTitle");
		if ( n == null || !(n instanceof TextField)) {
			this.updateStatusLine("Couldn’t load data from todoDetailTitle");
			return;
		}
		this.currentTodo.setTitle(((TextField) n).getText());
		// description
		n = primaryStage.getScene().lookup("#todoDetailDescription");
		if ( n == null || !(n instanceof TextArea)) {
			this.updateStatusLine("Couldn’t load data from todoDetailDescription");
			return;
		}
		this.currentTodo.setComment(((TextArea) n).getText());
		// date
		n = primaryStage.getScene().lookup("#todoDetailDueDate");
		if ( n == null || !(n instanceof CheckBox)) {
			this.updateStatusLine("Couldn’t load data from todoDetailDueDate");
			return;
		}
		if ( !((CheckBox) n).isSelected() ) {
			this.currentTodo.setDueDate(null);
		}else{
			n = primaryStage.getScene().lookup("#todoDetailDate");
			if (n == null || !(n instanceof DatePicker)) {
				this.updateStatusLine("Couldn’t load data from todoDetailDate");
				return;
			}
			LocalDate dd = ((DatePicker) n).getValue();
			// time
			n = primaryStage.getScene().lookup("#todoDetailTime");
			if (n == null || !(n instanceof TextField)) {
				this.updateStatusLine("Couldn’t load data from todoDetailDate");
				return;
			}
			if (!this.currentTodo.validateTime(((TextField) n).getText())) {
				this.updateStatusLine("Invalid time format, use HH:MM!");
				return;
			}
			this.currentTodo.setDueDate(dd, ((TextField) n).getText());
		}
		this.notifyList(this.todos, this.currentTodo);
		this.updateStatusLine("Item updated!");
	}
	private void detailUpdateDueDatePicker() {
		Node n = primaryStage.getScene().lookup("#todoDetailDueDate");
		if ( n == null || !(n instanceof CheckBox)) {
			this.updateStatusLine("Couldn’t load data from todoDetailDueDate");
			return;
		}
		boolean enable = ((CheckBox) n).isSelected();
		n = primaryStage.getScene().lookup("#todoDetailDate");
		if ( n == null || !(n instanceof DatePicker)) {
			this.updateStatusLine("Couldn’t load data from todoDetailDate");
			return;
		}
		n.setDisable(!enable);
		n = primaryStage.getScene().lookup("#todoDetailTime");
		if ( n == null || !(n instanceof TextField)) {
			this.updateStatusLine("Couldn’t load data from todoDetailTime");
			return;
		}
		n.setDisable(!enable);
	}

	private void showTodoListEdit() {
		Node n = this.primaryStage.getScene().lookup("#todoListToolBar");
		if ( n == null || !(n instanceof ToolBar)) {
			this.updateStatusLine("Couldn’t get 'todoListToolBar'");
			return;
		}
		n.setDisable(false);
		n.setVisible(true);
		n = primaryStage.getScene().lookup("#todoListNewNameSave");
		if ( n != null && n instanceof Button) {
			((Button) n).setOnAction(event -> this.saveTodoListEdit());
		}else{
			this.printError("Couldn’t read 'todoListNewNameSave'");
		}
		n = primaryStage.getScene().lookup("#todoListNewName");
		if ( n == null || !(n instanceof TextField)) {
			this.updateStatusLine("Couldn’t get 'todoListNewName'");
			return;
		}
		if ( this.buttonAction.equals("edit")) {
			Node l = primaryStage.getScene().lookup("#todoLists");
			if ( l == null || !(l instanceof ListView) ) {
				this.updateStatusLine("Couldn’t get 'todoLists'");
				return;
			}
			ListView lv = (ListView) l;
			if ( lv.getSelectionModel().getSelectedItem() != null && lv.getSelectionModel().getSelectedItem() instanceof TodoList ) {
				((TextField) n).setText(((TodoList)lv.getSelectionModel().getSelectedItem()).getName());
			}else{
				((TextField) n).setText("Unknown Name");
			}
		}else {
			((TextField) n).setText("New TodoList");
		}
	}
	private void saveTodoListEdit() {
		Node n = primaryStage.getScene().lookup("#todoListNewName");
		if ( n == null || !(n instanceof TextField)) {
			this.updateStatusLine("Couldn’t get 'todoListNewName'");
			return;
		}
		String name = ((TextField) n).getText();
		((TextField) n).setText("");
		if ( this.buttonAction.equals("new")) {
			this.todoLists.add(new TodoList(name));
			this.updateStatusLine("New TodoList generated!");
		}else {
			// edit existing one ...
			n = primaryStage.getScene().lookup("#todoLists");
			if (n == null || !(n instanceof ListView)) {
				return;
			}
			ListView l = (ListView) n;
			if (l.getSelectionModel().getSelectedItem() != null && l.getSelectionModel().getSelectedItem() instanceof TodoList) {
				TodoList t = (TodoList) l.getSelectionModel().getSelectedItem();
				t.setName(name);
				this.notifyList(this.todoLists, this.currentTodo);
				this.updateStatusLine("TodoList renamed!");
			}
		}
		n = this.primaryStage.getScene().lookup("#todoListToolBar");
		if ( n == null || !(n instanceof ToolBar)) {
			return;
		}
		n.setDisable(true);
		n.setVisible(false);
	}
	private void deleteTodoList() {
		Node n = primaryStage.getScene().lookup("#todoLists");
		if (n == null || !(n instanceof ListView)) {
			return;
		}
		ListView l = (ListView) n;
		if (l.getSelectionModel().getSelectedItem() != null && l.getSelectionModel().getSelectedItem() instanceof TodoList) {
			TodoList t = (TodoList) l.getSelectionModel().getSelectedItem();
			if (!t.isChangeable() ) {
				this.updateStatusLine("Can’t delete the TodoList!");
				return;
			}
			this.todoLists.remove(t);
			//this.notifyList(this.todoLists, this.currentTodo);
			this.updateStatusLine("TodoList removed!");
		}
	}
	private void printError(String s) {
		SimpleDateFormat format = new SimpleDateFormat();
		format.applyPattern("yyyyMMdd'T'HH:mm:ssZ");
		System.out.println("[" + format.format(GregorianCalendar.getInstance().getTime()) + "] " + s);
	}

	/**
	 * Notify a list about a changed item
	 * Taken from: http://stackoverflow.com/a/21435063
	 * @param list the list containing the changed item
	 * @param changedItem the item itself
	 */
	protected void notifyList(List list, Object changedItem) {
		int index = list.indexOf(changedItem);
		if (index >= 0) {
			// hack around RT-28397
			//https://javafx-jira.kenai.com/browse/RT-28397
			list.set(index, null);
			// good enough since jdk7u40 and jdk8
			list.set(index, changedItem);
		}
	}
}
