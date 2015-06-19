package de.t_battermann.dhbw.todolist;

import com.sun.javafx.collections.ObservableListWrapper;
import com.sun.javafx.collections.ObservableSequentialListWrapper;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
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

	public Controller(Stage primaryStage) {
		this.primaryStage = primaryStage;
		// Initialize needed scenes
		try {
			scenes.put("openFile", new Scene(FXMLLoader.load(getClass().getResource("openFile.fxml")),500,150));
			scenes.put("login", new Scene(FXMLLoader.load(getClass().getResource("login.fxml")),500,350));
			scenes.put("main", new Scene(FXMLLoader.load(getClass().getResource("main.fxml")),600,500));
			scenes.put("saveAs", new Scene(FXMLLoader.load(getClass().getResource("saveAs.fxml")),500,150));
		} catch (IOException e1) {
			System.out.println("Could’t load a fxml file!");
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
			return true;
		}
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
					System.out.println("Can’t read file '" + this.filename + "'");
					e1.printStackTrace();
				}
			}else{
				System.out.println("Didn’t find #openFilePath!");
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
			n.setOnMouseReleased(event -> {
				Label l = (Label) primaryStage.getScene().lookup("#labelHints");
				String name = null;
				String pass = null;
				Node m = primaryStage.getScene().lookup("#loginUsername");
				if (m != null && m instanceof TextField) {
					name = ((TextField) m).getText();
				} else {
					System.out.println("'#loginUsername' not found!");
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
					}else{
						this.currentUser = null;
						l.setText("Invalid credentials!");
					}
				}else{
					l.setText("Invalid credentials! 1");
				}
			});
		}else{
			System.out.println("'#loginButton' not found!");
		}
		n = primaryStage.getScene().lookup("#registerButton");
		if ( n != null && n instanceof Button) {
			n.setOnMouseReleased(event -> {
				Label l = (Label) primaryStage.getScene().lookup("#labelHintsCreateNewUser");
				// username
				String username;
				Node no = primaryStage.getScene().lookup("#registerUsername");
				if ( no != null && no instanceof TextField && User.checkUsername(((TextField) no).getText())) {
					username = ((TextField) no).getText();
				}else{
					l.setText("Invalid username! (Regex: [a-zA-Z0-9_-]{3,})");
					return;
				}
				if ( users.containsKey(username)) {
					l.setText("Username already taken!");
					return;
				}
				// password
				String password;
				no = primaryStage.getScene().lookup("#registerPassword");
				if( no != null && no instanceof PasswordField && User.checkPassword(((PasswordField) no).getText())) {
					password = ((PasswordField) no).getText();
					no = primaryStage.getScene().lookup("#registerPasswordRepeat");
					if( no == null || !(no instanceof PasswordField) || !password.equals(((PasswordField) no).getText())) {
						l.setText("The passwords didn’t match!");
					}
				}else{
					l.setText("Password must be longer than 6 chars! And must contain numbers, lower and upper chars!");
					return;
				}
				// eMail
				String email;
				no = primaryStage.getScene().lookup("#registerEmail");
				if( no != null && no instanceof TextField && User.checkEmail( ((TextField) no).getText() ) ) {
					email = ((TextField) no).getText();
					no = primaryStage.getScene().lookup("#registerEmailRepeat");
					if( no == null || !(no instanceof TextField) || !email.equals(((TextField) no).getText())) {
						l.setText("The eMail addresses didn’t match!");
					}
				}else{
					l.setText("No valid eMail address given!");
					return;
				}
				User nu = new User(username,password);
				nu.setEmail(email);
				currentUser = nu;
				users.put(username, nu);
				this.todoLists = new ObservableSequentialListWrapper<>(currentUser.getTodoLists());
			});
		}else{
			System.out.println("'#registerButton' not found!");
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
			n.setOnMouseReleased(event -> {
				if ( this.filename != null) {
					this.export(this.filename);
				}else{
					this.showSaveAs();
				}
			});
		}
		n = primaryStage.getScene().lookup("#menuSaveAs");
		if ( n != null && n instanceof Button ) {
			n.setOnMouseReleased(event -> this.showSaveAs());
		}
		n = primaryStage.getScene().lookup("#menuClose");
		if ( n != null && n instanceof Button ) {
			n.setOnMouseReleased(event -> Platform.exit());
		}
		this.primaryStage.setOnCloseRequest(event -> Platform.exit());
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
		((TextField) n).setText( this.currentTodo.getTitle() );
		// comment
		n = primaryStage.getScene().lookup("#todoDetailDescription");
		if ( n == null || !(n instanceof TextArea) )
			return;
		((TextArea) n).setText( this.currentTodo.getComment() );
		// if dueDate set:
		n = primaryStage.getScene().lookup("#todoDetailDueDate");
		if ( n == null || !(n instanceof CheckBox) )
			return;
		boolean dueDate = this.currentTodo.getDueDate() != null;
		((CheckBox) n).setSelected(dueDate);
		// datePicker
		n = primaryStage.getScene().lookup("#todoDetailDate");
		if( n == null || !(n instanceof DatePicker))
			return;
		if(dueDate) {
			((DatePicker) n).setValue( LocalDateTime.ofInstant(this.currentTodo.getDueDate().getTime().toInstant(), ZoneId.systemDefault()).toLocalDate() );
		}
		// time
		n = primaryStage.getScene().lookup("#todoDetailTime");
		if ( n == null || !(n instanceof TextField))
			return;
		((TextField) n).setText(this.currentTodo.getTime());
	}

	private void updateStatusLine(String text) {
		Node n = primaryStage.getScene().lookup("#statusLine");
		if ( n != null && n instanceof Label) {
			((Label) n).setText(text);
		}else{
			System.out.println("Couldn’t find status line!");
		}
	}

	private void showSaveAs() {
		// TODO
		Stage save = new Stage();
		save.setScene(scenes.get("saveAs"));
		save.setTitle("Save as ...");
		save.show();
		Button s = (Button) save.getScene().lookup("#save");
		if ( s != null )
			s.setOnMouseReleased(event -> {
				TextField f = (TextField)save.getScene().lookup("#filename");
				if ( f != null) {
					File file = new File(f.getText());
					if ( !file.isDirectory() ) {
						if ( this.filename == null)
							this.filename = f.getText();
						if(this.export(f.getText()))
							save.close();
					}else{
						this.updateStatusLine("Can’t write to file!");
					}
				}
			});
	}
}
