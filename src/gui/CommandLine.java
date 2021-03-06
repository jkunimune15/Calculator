/**
 * MIT License
 *
 * Copyright (c) 2016 Justin Kunimune
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package gui;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import maths.Expression;
import maths.Statement;
import maths.auxiliary.Notation;

/**
 * The set of Nodes that manages basic user input and memory.
 *
 * @author jkunimune
 */
public class CommandLine {

	public static final int PREF_WIDTH = 400;
	public static final int PREF_HEIGHT = 400;
	
	private TextArea history; //the textarea that shows all old entries and answers
	private List<String> lines; //stores the same info as history, but easier to access
	private TextField cmdLine; //the field that takes user input
	private ImageView displaySpace; //the space to display formatted math
	
	private VBox container; //the node that holds it all
	
	private Graph graph;
	private Workspace workspace;
	
	private Statement currentMath;
	private String errorMsg;
	private int histPosition; //current index in the history, 0 being current line and 
	
	private int caretPosition;
	private int anchor;
	
	
	
	public CommandLine(Graph gr, Workspace ws) {
		container = new VBox();
		container.setPrefWidth(PREF_WIDTH);
		
		history = new TextArea();
		history.setEditable(false);
		history.setPrefHeight(PREF_HEIGHT);
		container.getChildren().add(history);
		
		lines = new ArrayList<String>();
		
		cmdLine = new TextField();
		cmdLine.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {
				evaluate();
			}
		});
		cmdLine.setOnKeyPressed(new EventHandler<KeyEvent>() {
			public void handle(KeyEvent event) {
				checkKey(event);
			}
		});
		cmdLine.textProperty().addListener(new ChangeListener<String>() {
			public void changed(ObservableValue<? extends String> observable,
					String oldValue, String newValue) {
				update(newValue);
			}
		});
		cmdLine.caretPositionProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				if (cmdLine.isFocused())
					caretPosition = newValue.intValue();
			}
		});
		cmdLine.anchorProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				if (cmdLine.isFocused())
					anchor = newValue.intValue();
			}
		});
		container.getChildren().add(cmdLine);
		
		container.getChildren().add(new Separator());
		
		displaySpace = new ImageView();
		container.getChildren().add(displaySpace);
		
		graph = gr;
		workspace = ws;
		currentMath = Expression.NULL;
		errorMsg = "";
		histPosition = 0;
		caretPosition = 0;
		anchor = 0;
	}
	
	
	
	public Node getNode() {	// get all components in JavaFX format
		return container;
	}
	
	
	public void typeText(String text, boolean select) {	// add text to the command line
		cmdLine.selectRange(anchor, caretPosition);
		cmdLine.replaceSelection(text);
		
		if (select)
			cmdLine.selectRange(cmdLine.getCaretPosition()-text.length(),
					cmdLine.getCaretPosition());
		else if (!cmdLine.isFocused()) {
			final int caretPosition0 = caretPosition;
			cmdLine.requestFocus();
			cmdLine.positionCaret(caretPosition0+text.length());
		}
	}
	
	
	public void requestFocus() {
		cmdLine.requestFocus();
	}
	
	
	private void checkKey(KeyEvent event) {
		if (event.getCode() == KeyCode.UP) {
			moveUp();
			event.consume();
		}
		else if (event.getCode() == KeyCode.DOWN) {
			moveDown();
			event.consume();
		}
		else {
			histPosition = 0; //reset the history tracking
		}
	}
	
	
	private void moveUp() {
		
		histPosition ++;
		if (histPosition > lines.size()) {
			histPosition = lines.size();
		}
		if (!lines.isEmpty())
			typeText(lines.get(lines.size()-histPosition), true);
	}
	
	
	private void moveDown() {
		histPosition --;
		if (histPosition <= 0) {
			histPosition = 0;
			typeText("", false);
		}
		else if (!lines.isEmpty())
			typeText(lines.get(lines.size()-histPosition), true);
	}
	
	
	private void evaluate() {	// called when enter is pressed
		final String text = cmdLine.getText();
		Statement math = currentMath; // save the math and the message
		String errMsg = errorMsg;
		cmdLine.clear();
		history.appendText("\n"+text);	// write the current line to history
		lines.add(text);
		
		if (text.isEmpty())	return;
		
		if (!errMsg.isEmpty()) {
			history.appendText("\nSYNTAX ERROR: "+errMsg);
			return;
		}
		
		try {
			final Statement ans = math.simplified(workspace);	// evaluate the expression
			if (ans != null) {
				if (ans instanceof Expression) {
					history.appendText("\n\t= "+ans.toString());	// write the answer
					graph.setPlot((Expression) ans);
				}
				else
					history.appendText("\n\t"+ans.toString());
				lines.add(ans.toString());
				displaySpace.setImage(ans.toImage());
			}
		} catch (ArithmeticException e) {
			history.appendText("\nERROR: "+e.getMessage());	// print the error if there is one
		} catch (IllegalArgumentException e) {
			history.appendText("\nMEMORY ERROR: "+e.getMessage());
		}
	}
	
	
	private void update(String input) {	// called when something is typed
		try {
			currentMath = Notation.parseStatement(input);
			errorMsg = "";
		} catch (IllegalArgumentException e) {
			currentMath = Expression.ERROR;
			errorMsg = e.getMessage();
		}
		
		displaySpace.setImage(currentMath.toImage());
	}

}
