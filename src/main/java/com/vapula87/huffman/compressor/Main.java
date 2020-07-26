package com.vapula87.huffman.compressor;

import java.io.File;
import java.io.IOException;

import com.vapula87.huffman.compressor.Init;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Dialog;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * com.vapula87.huffman.compressor.Main class that draws the user interface using JavaFX.
 * Launches the main driver class (com.vapula87.huffman.compressor.Init.java) when load button is clicked.
 *
 * @author Michael Hackett
 */
public class Main extends Application {
	private Init run;
	private boolean forcedComp = false;
	@Override
	public void start(Stage primaryStage) {
		Group root = new Group();
		ObservableList<Node> list = root.getChildren();

		Text text1 = new Text();
		text1.setText("Output");
		text1.setFont(Font.font("tahoma",FontWeight.BOLD, FontPosture.REGULAR, 15));
		text1.setFill(Color.BLUE);
		text1.setX(220);
		text1.setY(45);

		Text text2 = new Text();
		text2.setText("Status");
		text2.setFont(Font.font("tahoma",FontWeight.BOLD, FontPosture.REGULAR, 15));
		text2.setFill(Color.BLUE);
		text2.setX(220);
		text2.setY(475);

		ListView<String> output = new ListView<>();
		output.setLayoutX(5);
		output.setLayoutY(55);
		output.setMinHeight(300);
		output.setMinWidth(490);

		TextField message = new TextField();
		message.setMinWidth(490);
		message.setLayoutX(5);
		message.setLayoutY(485);
		message.setEditable(false);

		MenuBar menu = new MenuBar();
		Menu file = new Menu("File"), options = new Menu("Options");
		MenuItem load = new MenuItem("Load");
		RadioMenuItem showcounts = new RadioMenuItem("Charcounts"), showcodings = new RadioMenuItem("Charcodings");
		CheckMenuItem forceComp = new CheckMenuItem("Force Compression");
		SeparatorMenuItem separator = new SeparatorMenuItem();

		Dialog<Void> dialog = new Dialog<>();
		GridPane grid = new GridPane();
		ProgressBar progress = new ProgressBar();
		progress.setProgress(0);
		grid.setHgap(50);
		grid.setVgap(80);
		grid.add(progress, 0, 0);
		dialog.setTitle("Progress");
		dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
		dialog.getDialogPane().setContent(grid);
		dialog.initOwner(primaryStage.getOwner());

		menu.setMinWidth(500);
		menu.getMenus().add(file);
		menu.getMenus().add(options);

		file.getItems().add(load);

		options.getItems().add(showcounts);
		options.getItems().add(showcodings);
		options.getItems().add(separator);
		options.getItems().add(forceComp);

		ToggleGroup toggle = new ToggleGroup();
		toggle.getToggles().add(showcounts);
		toggle.getToggles().add(showcodings);

		list.add(text1);
		list.add(text2);
		list.add(output);
		list.add(message);
		list.add(menu);

		options.getItems().get(0).setDisable(true);
		options.getItems().get(1).setDisable(true);
		load.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				message.setText("Opening file dialog...");
				FileChooser chooser = new FileChooser();
				File loadFile = chooser.showOpenDialog(primaryStage);
				if (loadFile == null) message.setText("");
				else {
					try {
						output.getItems().clear();
						run = new Init();
						run.setViewer(output);
						run.setDiag(dialog);
						run.setProg(progress);
						run.setMsg(message);
						run.setMenu(options);
						run.setToggle(toggle);
						run.setForced(forcedComp);
						run.initialize(loadFile);
					}
					catch (IOException e) { message.setText(e.getMessage()); return; }
				}
			}
		});
		showcounts.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) { run.showCounts(); }
		});
		showcodings.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) { run.showCodings(); }
		});
		forceComp.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				if (forceComp.isSelected()) message.setText("Forced compression enabled.");
				else message.setText("Forced compression disabled.");
				forcedComp = !forcedComp;
			}
		});
		Scene scene = new Scene(root, 500, 515);

		primaryStage.setTitle("Huffman Compressor");
		primaryStage.setScene(scene);
		primaryStage.setResizable(false);
		primaryStage.show();
	}
	public static void main(String[] args) { launch(args); }
}