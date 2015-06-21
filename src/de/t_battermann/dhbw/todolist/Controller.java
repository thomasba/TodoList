package de.t_battermann.dhbw.todolist;

import com.sun.javafx.collections.ObservableListWrapper;
import com.sun.javafx.collections.ObservableSequentialListWrapper;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
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
	private Stage primaryStage;
	private String buttonAction = "new";

	public Controller(Stage primaryStage) {
		this.primaryStage = primaryStage;
		showLoadFileDialog();
	}

	static class TodoListCell extends ListCell<Todo> {
		@Override
		public void updateItem(Todo item, boolean empty) {
			super.updateItem(item, empty);
			if ( !empty && item != null ) {
				if ( item.isPrio() && !item.isDone() ) {
					this.setStyle("-fx-graphic:url(/de/t_battermann/dhbw/todolist/star.png);");
				}else{
					this.setStyle("-fx-graphic:null;");
				}
				this.setTextFill(Paint.valueOf(item.isDone() ? "#999999" : (item.pastDue() ? "#aa0000" :"#000000") ));
				this.setText(item.getTitle() + (item.getDueDate() != null ? " (due: "+item.getDateTime()+")" : ""));
			}else{
				this.setStyle("-fx-graphic:null;");
				this.setTextFill(Paint.valueOf("#000000"));
				this.setText("");
			}
		}
	}

	/**
	 * Initialize new empty
	 */
	public void initEmpty() {
		ErrorPrinter.printInfo("initEmpty > Initialized empty database");
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
				ErrorPrinter.printError("export > Couldn’t write to file '" + filename + "'");
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
				this.updateStatusLine("Couldn’t write to file '" + filename + "'");
				ErrorPrinter.printError("export > Could’t write to file '" + filename + "'");
				e1.printStackTrace();
				return false;
			}
			this.updateStatusLine("Saved data to'"+filename+"'");
			ErrorPrinter.printInfo("export > Saved data to'"+filename+"'");
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
		try {
			primaryStage.setScene(new Scene(FXMLLoader.load(getClass().getResource("openFile.fxml")),500,150));
		} catch (IOException e) {
			ErrorPrinter.printError("showLoadFileDialog > Could’t open window 'openFile'! Goodbye!");
			e.printStackTrace();
			Platform.exit();
			return;
		}
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
					ErrorPrinter.printError("showLoadFileDialog > Can’t read file '" + this.filename + "'");
					e1.printStackTrace();
				}
			}else{
				ErrorPrinter.printWarning("showLoadFileDialog > Didn’t find element #openFilePath!");
			}
		});
		b = (Button) primaryStage.getScene().lookup("#openFileNew");
		b.setOnMouseReleased(event -> {
			this.initEmpty();
			this.showLoginDialog();
		});
	}

	private void showLoginDialog() {
		// log out ...
		this.currentUser = null;
		this.todoLists = null;
		this.todos = null;
		primaryStage.setTitle("TodoList :: Log in");
		try {
			primaryStage.setScene(new Scene(FXMLLoader.load(getClass().getResource("login.fxml")),500,350));
		} catch (IOException e) {
			ErrorPrinter.printError("showLoginDialog > Failed to open window 'login'! Goodbye!");
			e.printStackTrace();
			Platform.exit();
			return;
		}
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
					ErrorPrinter.printWarning("showLoginDialog > Didn’t find element #loginUsername!");
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
					l.setText("Invalid credentials!");
				}
			});
		}else{
			ErrorPrinter.printWarning("showLoginDialog > Didn’t find element #loginButton!");
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
			ErrorPrinter.printWarning("showLoginDialog > Didn’t find element #registerButton!");
		}
	}

	private void showMainWindow() {
		primaryStage.setTitle("TodoList :: " + currentUser.getUsername() +  " > Default");
		try {
			primaryStage.setScene(new Scene(FXMLLoader.load(getClass().getResource("main.fxml")),600,500));
		} catch (IOException e) {
			ErrorPrinter.printError("showMainWindow > Failed to open window 'main'! Goodbye!");
			e.printStackTrace();
			Platform.exit();
			return;
		}
		Node n = primaryStage.getScene().lookup("#todoLists");
		if ( n != null && n instanceof ListView) {
			ListView<TodoList> lv = (ListView<TodoList>) n;
			lv.setItems(this.todoLists);
			lv.scrollTo(currentUser.getTodoList("Default"));
			lv.getSelectionModel().selectedIndexProperty().addListener(event -> {
				this.updateSelectedTodoList();
			});
		}else{
			ErrorPrinter.printWarning("showMainWindow > Couldn’t find element '#todoLists'");
		}
		this.todos = new ObservableListWrapper<>(currentUser.getTodoList("Default").getTodos());
		n = primaryStage.getScene().lookup("#todos");
		if ( n != null && n instanceof ListView) {
			ListView<Todo> lv = (ListView<Todo>) n;
			lv.setItems(this.todos);
			lv.getSelectionModel().selectedIndexProperty().addListener(event -> {
				this.updateSelectedTodo();
			});
			lv.setCellFactory(param -> new TodoListCell());
		}else{
			ErrorPrinter.printWarning("showMainWindow > Couldn’t find element '#todos'");
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
		}else{
			ErrorPrinter.printWarning("showMainWindow > Couln’t find element '#menuSave'");
		}
		n = primaryStage.getScene().lookup("#menuSaveAs");
		if ( n != null && n instanceof Button ) {
			((Button) n).setOnAction(event -> this.showSaveAs());
		}else{
			ErrorPrinter.printWarning("showMainWindow > Couldn’t find element '#menuSaveAs'");
		}
		n = primaryStage.getScene().lookup("#menuClose");
		if ( n != null && n instanceof Button ) {
			((Button) n).setOnAction(event -> showCloseDialog());
		}else{
			ErrorPrinter.printWarning("showMainWindow > Couldn’t find element '#menuClose'");
		}
		this.primaryStage.setOnCloseRequest(event -> showCloseDialog());
		n = primaryStage.getScene().lookup("#todoDetailSave");
		if ( n != null && n instanceof Button) {
			((Button) n).setOnAction(event -> this.saveTodoEntry());
		}else{
			ErrorPrinter.printWarning("showMainWindow > Couldn’t find element '#todoDetailSave'");
		}
		n = primaryStage.getScene().lookup("#todoDetailDueDate");
		if (n != null && n instanceof CheckBox) {
			((CheckBox) n).setOnAction(event -> this.detailUpdateDueDatePicker());
		}else{
			ErrorPrinter.printWarning("showMainWindow > Couldn’t find element '#todoDetailDueDate'");
		}
		// handle new  TodoList
		n = primaryStage.getScene().lookup("#todoListNew");
		if ( n != null && n instanceof Button) {
			((Button) n).setOnAction(event -> {
				this.buttonAction = "new";
				this.showTodoListEdit();
			});
		}else{
			ErrorPrinter.printWarning("showMainWindow > Couln’t find element '#todoListNew'");
		}
		// handle edit TodoList
		n = primaryStage.getScene().lookup("#todoListEdit");
		if ( n != null && n instanceof Button) {
			((Button) n).setOnAction(event -> {
				this.buttonAction = "edit";
				this.showTodoListEdit();
			});
		}else{
			ErrorPrinter.printWarning("showMainWindow > Couldn’t find element '#todoListEdit'");
		}
		// handle delete TodoList
		n = primaryStage.getScene().lookup("#todoListDelete");
		if( n != null && n instanceof Button) {
			((Button) n).setOnAction(event -> showDeleteList());
		}else{
			ErrorPrinter.printWarning("showMainWindow > Couldn’t find element '#todoListDelete'");
		}
		// toggle todo
		n = primaryStage.getScene().lookup("#todoToggleDone");
		if ( n!= null && n instanceof ToggleButton) {
			((ToggleButton) n).setOnAction(event -> toggleDone());
		}else{
			ErrorPrinter.printWarning("showMainWindow > Couldn’t find element '#todoToggleDone'");
		}
		// toggle star
		n = primaryStage.getScene().lookup("#todoToggleStar");
		if ( n!= null && n instanceof ToggleButton) {
			((ToggleButton) n).setOnAction(event -> toggleStar());
		}else{
			ErrorPrinter.printWarning("showMainWindow > Couldn’t find element '#todoToggleStar'");
		}
		// add new todo item
		n = primaryStage.getScene().lookup("#todoNew");
		if ( n!= null && n instanceof Button) {
			((Button) n).setOnAction(event -> newTodoItem());
		}else{
			ErrorPrinter.printWarning("showMainWindow > Couldn’t find element '#todoNew'");
		}
		// delete todo item
		n = primaryStage.getScene().lookup("#todoDelete");
		if ( n != null && n instanceof Button ) {
			((Button) n).setOnAction(event -> showDeleteItem());
		}else{
			ErrorPrinter.printWarning("showMainWindow > Couldn’t find element '#todoDelete'");
		}
		// change password
		n = primaryStage.getScene().lookup("#menuChangePassword");
		if ( n != null && n instanceof Button ) {
			((Button) n).setOnAction(event -> showChangePassword());
		}else{
			ErrorPrinter.printWarning("showMainWindow > Couldn’t find element '#menuChangePassword'");
		}
		// change eMail
		n = primaryStage.getScene().lookup("#menuChangeEmail");
		if ( n != null && n instanceof Button ) {
			((Button) n).setOnAction(event -> showChangeEmail());
		}else{
			ErrorPrinter.printWarning("showMainWindow > Couldn’t find element '#menuChangeEmail'");
		}
		// log out
		n = primaryStage.getScene().lookup("#menuLogout");
		if ( n != null && n instanceof Button) {
			((Button) n).setOnAction(event -> showLoginDialog());
		}else{
			ErrorPrinter.printWarning("showMainWindow > Couldn’t find element '#menuLogout'");
		}
		// move todo item
		n = primaryStage.getScene().lookup("#todoMove");
		if ( n != null && n instanceof Button) {
			((Button) n).setOnAction(event -> showMoveTodoItem());
		}else{
			ErrorPrinter.printWarning("showMainWindow > Couldn’t find element '#todoMove'");
		}
	}

	private void updateSelectedTodoList() {
		Node n = primaryStage.getScene().lookup("#todoLists");
		if ( n == null || !(n instanceof ListView) ) {
			ErrorPrinter.printWarning("updateSelectedTodoList > updateSelectedTodoList > Couldn’t find element '#todoLists'");
			return;
		}
		ListView l = (ListView) n;
		n = primaryStage.getScene().lookup("#todos");
		if ( n == null || !(n instanceof ListView)) {
			ErrorPrinter.printWarning("updateSelectedTodoList > updateSelectedTodoList > Couldn’t find element '#todos'");
			return;
		}
		ListView<Todo> lt = (ListView) n;
		if ( l.getSelectionModel().getSelectedItem() != null && l.getSelectionModel().getSelectedItem() instanceof TodoList ) {
			TodoList t = (TodoList) l.getSelectionModel().getSelectedItem();
			primaryStage.setTitle("TodoList :: " + currentUser.getUsername() + " > " + t.getName());
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
		if ( n == null || !(n instanceof TextField)) {
			ErrorPrinter.printWarning("updateSelectedTodo > Couldn’t find element '#todoDetailTitle'");
			return;
		}
		((TextField) n).setText( this.currentTodo == null ? "" : this.currentTodo.getTitle() );
		// comment
		n = primaryStage.getScene().lookup("#todoDetailDescription");
		if ( n == null || !(n instanceof TextArea) ) {
			ErrorPrinter.printWarning("updateSelectedTodo > Couldn’t find element '#todoDetailDescription'");
			return;
		}
		((TextArea) n).setText( this.currentTodo == null ? "" : this.currentTodo.getComment() );
		// if dueDate set:
		n = primaryStage.getScene().lookup("#todoDetailDueDate");
		if ( n == null || !(n instanceof CheckBox) ) {
			ErrorPrinter.printWarning("updateSelectedTodo > Couldn’t find element '#todoDetailDueDate'");
			return;
		}
		boolean dueDate = this.currentTodo != null && this.currentTodo.getDueDate() != null;
		((CheckBox) n).setSelected(dueDate);
		// datePicker
		n = primaryStage.getScene().lookup("#todoDetailDate");
		if( n == null || !(n instanceof DatePicker)) {
			ErrorPrinter.printWarning("updateSelectedTodo > Couldn’t find element '#todoDetailDate'");
			return;
		}
		if(dueDate) {
			((DatePicker) n).setValue( LocalDateTime.ofInstant(this.currentTodo.getDueDate().getTime().toInstant(), ZoneId.systemDefault()).toLocalDate() );
			n.setDisable(false);
		}else{
			((DatePicker) n).setValue(null);
			n.setDisable(true);
		}
		// time
		n = primaryStage.getScene().lookup("#todoDetailTime");
		if ( n == null || !(n instanceof TextField)) {
			ErrorPrinter.printWarning("updateSelectedTodo > Couldn’t find element '#todoDetailTime'");
			return;
		}
		if ( dueDate ) {
			((TextField) n).setText(this.currentTodo.getTime() );
			n.setDisable(false);
		}else{
			n.setDisable(true);
			((TextField) n).setText("00:00");
		}
		// stared
		n = primaryStage.getScene().lookup("#todoToggleStar");
		if ( n != null && n instanceof ToggleButton ) {
			((ToggleButton) n).setSelected(currentTodo != null && currentTodo.isPrio());
		}else{
			ErrorPrinter.printWarning("updateSelectedTodo > Couldn’t find element '#todoToggleStar'");
		}
		// done
		n = primaryStage.getScene().lookup("#todoToggleDone");
		if ( n != null && n instanceof ToggleButton) {
			((ToggleButton) n).setSelected(currentTodo != null && currentTodo.isDone());
		}else{
			ErrorPrinter.printWarning("updateSelectedTodo > Couldn’t find element '#todoToggleDone'");
		}
	}

	private void updateStatusLine(String text) {
		Node n = primaryStage.getScene().lookup("#statusLine");
		if ( n != null && n instanceof Label) {
			((Label) n).setText(text);
		}else{
			ErrorPrinter.printWarning("updateStatusLine > Couldn’t find element '#statusLine'");
		}
	}

	private void showSaveAs() {
		showSaveAs(false);
	}

	private void showSaveAs(boolean exitAfterSave) {
		Stage save = new Stage();
		try {
			save.setScene(new Scene(FXMLLoader.load(getClass().getResource("saveAs.fxml")),500,150));
		} catch (IOException e) {
			this.updateStatusLine("Failed to open window!");
			ErrorPrinter.printError("showSaveAs > Failed to open window 'saveAs'");
			e.printStackTrace();
			return;
		}
		save.setTitle("Save as ...");
		save.show();
		Node n = primaryStage.getScene().lookup("#filename");
		if ( n != null && n instanceof TextField && this.filename != null ) {
			((TextField)n).setText(this.filename);
		}
		Button s = (Button) save.getScene().lookup("#save");
		if ( s != null)
			s.setOnAction(event -> {
				TextField f = (TextField) save.getScene().lookup("#filename");
				if (f != null) {
					File file = new File(f.getText());
					if (!file.isDirectory()) {
						if (this.filename == null)
							this.filename = f.getText();
						if (this.export(f.getText())) {
							if (exitAfterSave) {
								Platform.exit();
							} else {
								save.close();
							}
						}
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
			ErrorPrinter.printWarning("saveTodoEntry > Didn’t find element #todos!");
			return;
		}
		// title
		Node n = primaryStage.getScene().lookup("#todoDetailTitle");
		if ( n == null || !(n instanceof TextField)) {
			this.updateStatusLine("Couldn’t load data from todoDetailTitle");
			ErrorPrinter.printWarning("saveTodoEntry > Didn’t find element #todoDetailTitle!");
			return;
		}
		this.currentTodo.setTitle(((TextField) n).getText());
		// description
		n = primaryStage.getScene().lookup("#todoDetailDescription");
		if ( n == null || !(n instanceof TextArea)) {
			this.updateStatusLine("Couldn’t load data from todoDetailDescription");
			ErrorPrinter.printWarning("saveTodoEntry > Didn’t find element #statusLine!");
			return;
		}
		this.currentTodo.setComment(((TextArea) n).getText());
		// date
		n = primaryStage.getScene().lookup("#todoDetailDueDate");
		if ( n == null || !(n instanceof CheckBox)) {
			this.updateStatusLine("Couldn’t load data from todoDetailDueDate");
			ErrorPrinter.printWarning("saveTodoEntry > Didn’t find element #todoDetailDueDate!");
			return;
		}
		if ( !((CheckBox) n).isSelected() ) {
			this.currentTodo.setDueDate(null);
		}else{
			n = primaryStage.getScene().lookup("#todoDetailDate");
			if (n == null || !(n instanceof DatePicker)) {
				this.updateStatusLine("Couldn’t load data from todoDetailDate");
				ErrorPrinter.printWarning("saveTodoEntry > Didn’t find element #todoDetailDate!");
				return;
			}
			LocalDate dd = ((DatePicker) n).getValue();
			// time
			n = primaryStage.getScene().lookup("#todoDetailTime");
			if (n == null || !(n instanceof TextField)) {
				this.updateStatusLine("Couldn’t load data from todoDetailDate");
				ErrorPrinter.printWarning("saveTodoEntry > Didn’t find element #todoDetailDate!");
				return;
			}
			if ( dd == null ) {
				this.updateStatusLine("Invalid date!");
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
			ErrorPrinter.printWarning("detailUpdateDueDatePicker > Didn’t find element #todoDetailDueDate!");
			return;
		}
		boolean enable = ((CheckBox) n).isSelected();
		n = primaryStage.getScene().lookup("#todoDetailDate");
		if ( n == null || !(n instanceof DatePicker)) {
			this.updateStatusLine("Couldn’t load data from todoDetailDate");
			ErrorPrinter.printWarning("detailUpdateDueDatePicker > Didn’t find element #todoDetailDue!");
			return;
		}
		n.setDisable(!enable);
		n = primaryStage.getScene().lookup("#todoDetailTime");
		if ( n == null || !(n instanceof TextField)) {
			this.updateStatusLine("Couldn’t load data from todoDetailTime");
			ErrorPrinter.printWarning("detailUpdateDueDatePicker > Didn’t find element #todoDetailTime!");
			return;
		}
		n.setDisable(!enable);
	}

	private void showTodoListEdit() {
		Node n = this.primaryStage.getScene().lookup("#todoListToolBar");
		if ( n == null || !(n instanceof ToolBar)) {
			this.updateStatusLine("Couldn’t get 'todoListToolBar'");
			ErrorPrinter.printWarning("showTodoListEdit > Didn’t find element #todoListToolBar!");
			return;
		}
		n.setDisable(false);
		n.setVisible(true);
		n = primaryStage.getScene().lookup("#todoListNewNameSave");
		if ( n != null && n instanceof Button) {
			((Button) n).setOnAction(event -> this.saveTodoListEdit());
		}else{
			ErrorPrinter.printError("showTodoListEdit > Couldn’t read 'todoListNewNameSave'");
		}
		n = primaryStage.getScene().lookup("#todoListNewName");
		if ( n == null || !(n instanceof TextField)) {
			this.updateStatusLine("Couldn’t get 'todoListNewName'");
			ErrorPrinter.printWarning("showTodoListEdit > Didn’t find element #todoListNewName!");
			return;
		}
		if ( this.buttonAction.equals("edit")) {
			Node l = primaryStage.getScene().lookup("#todoLists");
			if ( l == null || !(l instanceof ListView) ) {
				this.updateStatusLine("Couldn’t get 'todoLists'");
				ErrorPrinter.printWarning("showTodoListEdit > Didn’t find element #todoLists!");
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
			ErrorPrinter.printWarning("saveTodoListEdit > Didn’t find element #todoListNewName!");
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
			ErrorPrinter.printWarning("saveTodoListEdit > Didn’t find element #todoListToolBar!");
			return;
		}
		n.setDisable(true);
		n.setVisible(false);
	}

	private void toggleDone() {
		if(this.currentTodo == null)
			return;
		Node n = primaryStage.getScene().lookup("#todoToggleDone");
		if ( n == null || !(n instanceof ToggleButton) ) {
			ErrorPrinter.printWarning("toggleDone > Didn’t find element #todoToggleDone!");
			return;
		}
		this.currentTodo.setDone(!this.currentTodo.isDone());
		((ToggleButton) n).setSelected(this.currentTodo.isDone());
		this.notifyList(todos,currentTodo);
	}

	private void toggleStar() {
		if(this.currentTodo == null)
			return;
		Node n = primaryStage.getScene().lookup("#todoToggleStar");
		if ( n == null || !(n instanceof ToggleButton) ) {
			ErrorPrinter.printWarning("toggleStar > Didn’t find element #todoToggleStar!");
			return;
		}
		this.currentTodo.setPrio(!this.currentTodo.isPrio());
		((ToggleButton) n).setSelected(this.currentTodo.isPrio());
		this.notifyList(todos,currentTodo);
	}

	private void newTodoItem() {
		Todo t = new Todo("New Item", "Edit this item :-)");
		this.todos.add(t);
		this.updateStatusLine("Item added!");
		Node n = primaryStage.getScene().lookup("#todos");
		if ( n == null || !(n instanceof ListView) ) {
			ErrorPrinter.printWarning("newTodoItem > Didn’t find element #todos!");
			return;
		}
		((ListView) n).getSelectionModel().select(t);
		((ListView) n).scrollTo(t);
	}

	private void showDeleteItem() {
		Stage delete = new Stage();
		try {
			delete.setScene(new Scene(FXMLLoader.load(getClass().getResource("deleteTodoItem.fxml"))));
		} catch (IOException e) {
			this.updateStatusLine("Failed to open window!");
			ErrorPrinter.printError("showDeleteItem > Failed to open window 'deleteTodoItem'!");
			e.printStackTrace();
			return;
		}
		delete.setTitle("Delete '"+this.currentTodo.getTitle()+"'");
		delete.show();
		Node n = delete.getScene().lookup("#no");
		if ( n != null && n instanceof Button)
			((Button)n).setOnAction(event -> delete.close());
		n = delete.getScene().lookup("#yes");
		if ( n != null && n instanceof Button )
			((Button) n).setOnAction(event -> {
				this.todos.remove(this.currentTodo);
				this.updateStatusLine("Deleted item!");
				delete.close();
			});
	}

	private void showDeleteList() {
		Stage delete = new Stage();
		try {
			delete.setScene(new Scene(FXMLLoader.load(getClass().getResource("deleteTodoItem.fxml"))));
		} catch (IOException e) {
			this.updateStatusLine("Failed to open window!");
			ErrorPrinter.printError("showDeleteList > Failed to open window 'deleteList'!");
			e.printStackTrace();
			return;
		}
		Node n = primaryStage.getScene().lookup("#todoLists");
		if (n == null || !(n instanceof ListView)) {
			ErrorPrinter.printWarning("showDeleteList > Didn’t find element #todoLists!");
			return;
		}
		ListView l = (ListView) n;
		TodoList t;
		if (l.getSelectionModel().getSelectedItem() != null && l.getSelectionModel().getSelectedItem() instanceof TodoList) {
			t = (TodoList) l.getSelectionModel().getSelectedItem();
		}else {
			ErrorPrinter.printWarning("showDeleteList > Didn’t find selected item!");
			return;
		}
		delete.setTitle("Delete '"+t.getName()+"'");
		delete.show();
		n = delete.getScene().lookup("#no");
		if ( n != null && n instanceof Button)
			((Button)n).setOnAction(event -> delete.close());
		n = delete.getScene().lookup("#yes");
		if ( n != null && n instanceof Button )
			((Button) n).setOnAction(event -> {
				this.todoLists.remove(t);
				this.updateStatusLine("Deleted TodoList!");
				delete.close();
			});
	}

	private void showChangePassword() {
		Stage change = new Stage();
		try {
			change.setScene(new Scene(FXMLLoader.load(getClass().getResource("changePassword.fxml"))));
		} catch (IOException e) {
			e.printStackTrace();
			ErrorPrinter.printError("showChangePassword > Failed to open window 'changePassword'!");
			return;
		}
		change.setTitle("Change password");
		change.show();
		Node n = change.getScene().lookup("#status");
		if ( n == null || !(n instanceof Label)) {
			ErrorPrinter.printWarning("showChangePassword > Didn’t find element #status!");
			return;
		}
		Label status = (Label) n;
		n = change.getScene().lookup("#abort");
		if ( n == null || !(n instanceof Button)) {
			ErrorPrinter.printWarning("showChangePassword > Didn’t find element #abort!");
			return;
		}
		((Button) n).setOnAction(event -> change.close());
		n = change.getScene().lookup("#save");
		if ( n == null || !(n instanceof Button)) {
			ErrorPrinter.printWarning("showChangePassword > Didn’t find element #save!");
			return;
		}
		((Button) n).setOnAction(event -> {
			String pw;
			// validate passwords ...
			Node l = change.getScene().lookup("#password");
			if ( l == null || !(l instanceof PasswordField)) {
				ErrorPrinter.printWarning("showChangePassword > Didn’t find element #password!");
				return;
			}
			if (!currentUser.checkLoginData( ((PasswordField) l).getText() )) {
				status.setText("Wrong password!");
				return;
			}
			l = change.getScene().lookup("#newPassword");
			if ( l == null || !(l instanceof PasswordField)) {
				ErrorPrinter.printWarning("showChangePassword > Didn’t find element #newPassword!");
				return;
			}
			pw = ((PasswordField)l).getText();
			if (!User.checkPassword(pw)) {
				status.setText("Please use at least 6 chars, upper- and lowercase and numbers!");
				return;
			}
			l = change.getScene().lookup("#newPasswordRepeat");
			if ( l == null || !(l instanceof PasswordField)) {
				status.setText("Couldn’t access newPasswordRepeat field!");
				ErrorPrinter.printWarning("showChangePassword > Didn’t find element #newPasswordRepeat!");
				return;
			}
			if ( !pw.equals( ((PasswordField) l).getText() )) {
				status.setText("Passwords didn’t match!");
				return;
			}
			this.currentUser.setPassword(pw);
			change.close();
		});
	}

	private void showChangeEmail() {
		Stage change = new Stage();
		try {
			change.setScene(new Scene(FXMLLoader.load(getClass().getResource("changeEmail.fxml"))));
		} catch (IOException e) {
			ErrorPrinter.printError("showChangeEmail > Failed to open window 'changePassword'!");
			e.printStackTrace();
			return;
		}
		change.setTitle("Change eMail");
		change.show();
		Node n = change.getScene().lookup("#status");
		if ( n == null || !(n instanceof Label)) {
			ErrorPrinter.printWarning("showChangeEmail > Didn’t find element #status!");
			return;
		}
		Label status = (Label) n;
		// load current email address
		n = change.getScene().lookup("#eMail");
		if ( n == null || !(n instanceof TextField)) {
			ErrorPrinter.printWarning("showChangeEmail > Didn’t find element #eMail!");
			return;
		}
		((TextField) n).setText(this.currentUser.getEmail());
		n = change.getScene().lookup("#eMailRepeat");
		if ( n == null || !(n instanceof TextField)) {
			ErrorPrinter.printWarning("showChangeEmail > Didn’t find element #eMailRepeat!");
			return;
		}
		((TextField) n).setText(this.currentUser.getEmail());
		// actions
		n = change.getScene().lookup("#abort");
		if ( n == null || !(n instanceof Button)) {
			ErrorPrinter.printWarning("showChangeEmail > Didn’t find element #abort!");
			return;
		}
		((Button) n).setOnAction(event -> change.close());
		n = change.getScene().lookup("#save");
		if ( n == null || !(n instanceof Button)) {
			ErrorPrinter.printWarning("showChangeEmail > Didn’t find element #save!");
			return;
		}
		((Button) n).setOnAction(event -> {
			String email;
			// validate passwords ...
			Node l = change.getScene().lookup("#eMail");
			if ( l == null || !(l instanceof TextField)) {
				status.setText("Couldn’t access eMail field!");
				ErrorPrinter.printWarning("showChangeEmail > Didn’t find element #eMail!");
				return;
			}
			email = ((TextField) l).getText();
			if (!User.checkEmail(email)) {
				status.setText("Invalid format!");
				return;
			}
			l = change.getScene().lookup("#eMailRepeat");
			if ( l == null || !(l instanceof TextField)) {
				status.setText("Couldn’t access eMailRepeat field!");
				ErrorPrinter.printWarning("showChangeEmail > Didn’t find element #eMailRepeat!");
				return;
			}
			if ( !email.equals( ((TextField) l).getText() )) {
				status.setText("eMails didn’t match!");
				return;
			}
			this.currentUser.setEmail(email);
			change.close();
		});
	}

	private void showCloseDialog() {
		Stage close = new Stage();
		try {
			close.setScene(new Scene(FXMLLoader.load(getClass().getResource("close.fxml"))));
		} catch (IOException e) {
			ErrorPrinter.printError("showCloseDialog > Failed to open window 'close'! Goodbye!");
			e.printStackTrace();
			Platform.exit();
		}
		close.setTitle("Close program?");
		close.show();
		Node n = close.getScene().lookup("#abort");
		if ( n == null || !(n instanceof Button)) {
			ErrorPrinter.printWarning("showCloseDialog > Didn’t find element #abort");
			return;
		}
		((Button) n).setOnAction(event -> close.close());
		n = close.getScene().lookup("#save");
		if ( n == null || !(n instanceof Button)) {
			ErrorPrinter.printWarning("showCloseDialog > Didn’t find element #save");
			return;
		}
		((Button) n).setOnAction(event -> {
			if (this.filename != null) {
				this.export(this.filename);
				Platform.exit();
			} else {
				this.showSaveAs(true);
			}
		});
		n = close.getScene().lookup("#close");
		if ( n == null || !(n instanceof Button)) {
			ErrorPrinter.printWarning("showCloseDialog > Didn’t find element #close");
			return;
		}
		((Button) n).setOnAction(event -> Platform.exit());
	}

	private void showMoveTodoItem() {
		Stage move = new Stage();
		try {
			move.setScene(new Scene(FXMLLoader.load(getClass().getResource("moveTodoItem.fxml"))));
		} catch (IOException e) {
			ErrorPrinter.printError("showMoveTodoItem > Failed to open window 'moveTodoItem'! Goodbye!");
			e.printStackTrace();
			Platform.exit();
		}
		move.setTitle("Move item '" + this.currentTodo.getTitle() + "'");
		move.show();
		// fill in the gaps :)
		Node n = move.getScene().lookup("#title");
		if ( n == null || !(n instanceof TextField)) {
			ErrorPrinter.printWarning("showMoveTodoItem > Didn’t find element #title");
			return;
		}
		((TextField) n).setText(this.currentTodo.getTitle());
		// get current TodoList
		n = primaryStage.getScene().lookup("#todoLists");
		if (n == null || !(n instanceof ListView)) {
			ErrorPrinter.printWarning("showDeleteList > Didn’t find element #todoLists!");
			return;
		}
		ListView l = (ListView) n;
		TodoList t;
		if (l.getSelectionModel().getSelectedItem() != null && l.getSelectionModel().getSelectedItem() instanceof TodoList) {
			t = (TodoList) l.getSelectionModel().getSelectedItem();
		}else {
			ErrorPrinter.printWarning("showDeleteList > Didn’t find selected item!");
			return;
		}
		n = move.getScene().lookup("#source");
		if ( n == null || !(n instanceof TextField)) {
			ErrorPrinter.printWarning("showDeleteList > Didn’t find element #source");
			return;
		}
		((TextField) n).setText(t.getName());
		// populate dropdown
		n = move.getScene().lookup("#destination");
		if ( n == null || !(n instanceof ChoiceBox)) {
			ErrorPrinter.printWarning("showDeleteList > Didn’t find element #destination");
			return;
		}
		ChoiceBox<TodoList> c = (ChoiceBox) n;
		c.setItems(this.todoLists);
		// event handlers
		n = move.getScene().lookup("#abort");
		if ( n == null || !(n instanceof Button)) {
			ErrorPrinter.printWarning("showDeleteList > Didn’t find element #abort");
			move.close();
			return;
		}
		((Button) n).setOnAction(event -> move.close());
		n = move.getScene().lookup("#move");
		if ( n == null || !(n instanceof Button)) {
			ErrorPrinter.printWarning("showDeleteList > Didn’t find element #move");
			move.close();
			return;
		}
		((Button) n).setOnAction(event -> {
			// first add the item to the destination
			TodoList list = c.getSelectionModel().getSelectedItem();
			if ( list == null ) {
				ErrorPrinter.printWarning("showDeleteList > Invalid selection!");
				this.updateStatusLine("Invalid selection!");
				return;
			}
			list.addTodo(this.currentTodo);
			// then delete the item in the source list
			todos.remove(this.currentTodo);
			// update current list
			move.close();
		});
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
