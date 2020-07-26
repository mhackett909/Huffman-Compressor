package com.vapula87.huffman.interfaces;

import javafx.scene.control.ListView;
import java.io.File;
import java.io.IOException;

public interface IHuffModel extends ICharCounter {
	/* 
	 * Reads a stream and updates local state
	 * Store counts of characters using com.vapula87.huffman.interfaces.ICharCounter
	 * 
	 */
	public void initialize(File readFile) throws IOException;
	public void showCounts();
	public void showCodings();
	public void setViewer(ListView<String> output);
}
