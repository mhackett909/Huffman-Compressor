package com.vapula87.huffman.compressor;

import java.io.*;
import java.util.ArrayList;

import com.vapula87.huffman.interfaces.Entry;
import com.vapula87.huffman.interfaces.IHuffModel;
import com.vapula87.huffman.structures.ITreeMaker;
import com.vapula87.huffman.structures.SortedTableMap;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Dialog;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
/**
 * Huffman Compressor (by Michael Hackett)<br><br>
 *
 * CSCI 230 (Data Structures & Algorithms)<br>
 * Professor Madrid<br>
 * April 2020
 *
 * @author Michael Hackett
 *
 */
public class Init implements IHuffModel {
	public static final byte[] 				MAGIC_NUM = new byte[]{ 0,0,0,0,1,1,1,1,0,0,0,1,1,1,0,1 };
	public static final int 				MAX_COUNT = 256, MAX_ITEMS = 513;
    public static final int 				BITS_PER_WORD = 8, BMASK = 1;
	public static final int 				PSEUDO_EOF= (1 << BITS_PER_WORD);
	private int[] 							counts;
	private int 							byteTotal = 0, bitLoc = 0, storeBit = 0;
	private byte[] 							byteArr, tempByte;
	private long 							fileSize = 0;
	private boolean 						success, remFile, forcedComp, magicAuth;
	private String 							file, filename, extension, errMsg;
	private ArrayList<String> 				countsView, codings;
	private ITreeMaker tree;
	private SortedTableMap<Integer, String> map;
	private File 							readFile, testFile;
	private BufferedInputStream 			buffRead;
	private BufferedOutputStream 			buffWrite;
	private ListView<String> 				output;
	private TextField 						msg;
	private Dialog<Void> 					dialog;
	private Menu 							options;
	private ProgressBar 					progress;
	private ToggleGroup 					toggle;
	/**
	 * Initializes the Huffman compressor.<br>
	 *
	 * @param readFile (File)
	 * @throws IOException
	 */
	@Override
	public void initialize(File readFile) throws IOException {
		this.readFile = readFile;
		byteArr = new byte[BITS_PER_WORD];
		tempByte = new byte[BITS_PER_WORD];
		counts = new int[MAX_COUNT];
		codings = new ArrayList<>();
		countsView = new ArrayList<>();
		msg.setText("Initializing...");
		errMsg = "Operation failed";
		output.getItems().clear();
		setOptions(false);
		success = false;
		remFile = false;
		magicAuth = false;
		file = readFile.toString();
		if (file.contains(".")) {
			filename = file.substring(0, file.lastIndexOf("."));
			extension = file.substring(file.lastIndexOf("."), file.length());
		}
		else {
			filename = file;
			extension = "";
		}
		fileSize = readFile.length();
		if (extension.equals(".huff")) {
			msg.setText("Decompression started...");
			tree = new ITreeMaker();
			decompress();
		}
		else {
			byteTotal = countAll(buffRead);
			if (byteTotal == 0) {
				msg.setText("Compression failed: File is empty.");
				return;
			}
			tree = new ITreeMaker(counts);
			tree.getTree().encode(null);
			map = tree.getTree().getMap();
			if (!forcedComp) {
				if (!checkSavings()) {
					msg.setText("Compressing this file does not save space. Enable forced compression.");
					return;
				}
			}
			for (Entry x : map.entrySet()) codings.add(x.getKey().toString() + " "+x.getValue().toString());
			msg.setText("Compression started...");
			showCounts();
			compress();
		}
	}
	/**
	 * Counts the number of unique bytes in a file. Stores the information in an integer array.
	 *
	 * @param stream is source of data
	 * @return (int)
	 * @throws IOException
	 */
	@Override
	public int countAll(InputStream stream) throws IOException {
		//Looked up how to implement Task<>
		progress.setProgress(0);
		dialog.setHeaderText("Loading...");
		Task<Boolean> task = new Task<>() {
			@Override
			protected Boolean call() {
				try {
					int bytes, last = 0;
					double percentage;
					testFile = new File(filename+".huff");
					if (testFile.exists()) {
						errMsg = "Compression failed: "+filename+".huff already exists";
						return success;
					}
					buffRead = new BufferedInputStream(new FileInputStream(file));
					while ((bytes = buffRead.read()) != -1) {
						add(bytes);
						byteTotal++;
						percentage = (double) byteTotal / fileSize;
						percentage *= 100;
						percentage = Math.floor(percentage);
						if (percentage != last) {
							last = (int) percentage;
							updateProg(percentage/100);
						}
					}
					success = true;
					buffRead.close();
				}catch (Exception e) { errMsg = "Compression failed: Error loading file.";	}
				return success;
			}
		};
		task.setOnSucceeded(e -> { dialog.close(); });
		task.setOnCancelled(e -> { 
			errMsg = "Compression failed: Error loading file.";
			dialog.close();
		});
		task.setOnFailed(e -> { 
			errMsg = "Compression failed: Error loading file.";
			dialog.close();
		});
		new Thread(task).start();
		dialog.showAndWait();
		if (!success) {
			try { buffRead.close(); }
			catch(NullPointerException e) { }
			throw new IOException(errMsg);
		}
		else success = false;
		return byteTotal;
	}
	/**
	 * Compresses the file.<br><br>
	 *
	 * Operations in order:<p>
	 * ** Writes the magic number.<br>
	 * ** Writes file extension.<br>
	 * ** Writes the dictionary.<br>
	 * ** Writes encodings (with PSEUDO_EOF).</p>
	 *
	 * @throws IOException
	 */
	public void compress() throws IOException {
		progress.setProgress(0);
		dialog.setHeaderText("Compressing...");
		Task<Boolean> task = new Task<>() {
			@Override
			protected Boolean call() {
				try {
					int bytes, last = 0, total = 0, items = 0;
					double percentage;
					String temp;
					buffWrite = new BufferedOutputStream(new FileOutputStream(filename+".huff"));
					buffRead = new BufferedInputStream(new FileInputStream(file));
					for (int w = 0; w < MAGIC_NUM.length; w++) addBuff(MAGIC_NUM[w]);
					extension += ":";
					for (int w = 0; w < extension.length(); w++) buffWrite.write((int) extension.charAt(w)); //Write file extension
					extension = extension.substring(0,extension.length()-1); //Removes : for GUI printing
					for (Entry nodes : tree.getTree()) { //Writes dictionary
						if (nodes.getValue() == null) addBuff((byte) 0);
						else {
							addBuff((byte) 1);
							byteArr = binaryConvert((int) nodes.getValue());
							for (int w = 0; w < byteArr.length; w++) addBuff(byteArr[w]);
						}
						items++;
					}
					for (; items < MAX_ITEMS; items++) addBuff((byte) 0);  //Filler for keeping track of location in bit stream
					while ((bytes = buffRead.read()) != -1) { //Writing encoded map values
						temp = map.get(bytes);
						for (int w = 0; w < temp.length(); w++) addBuff((byte) (temp.charAt(w) - 48));
						total++;
						percentage = (double) total / fileSize;
						percentage *= 100;
						percentage = Math.floor(percentage);
						if (percentage != last) {
							last = (int) percentage;
							updateProg(percentage/100);
						}
					}
					temp = map.get(PSEUDO_EOF); //Writes eof 
					for (int w = 0; w < temp.length(); w++) addBuff((byte) (temp.charAt(w) - 48));
					success = true;
					buffRead.close();
					flushBuff();
				}catch (Exception e) { errMsg = "Compression failed: Error in read/write process."; }
				return success;
			}
		};	
		task.setOnSucceeded(e -> { dialog.close(); });
		task.setOnCancelled(e -> {
			errMsg = "Compression cancelled.";
			dialog.close();
		});
		task.setOnFailed(e -> {
			errMsg = "Compression failed.";
			dialog.close();
		});
		new Thread(task).start();
		dialog.showAndWait();
		if (!success) {
			try {
				buffRead.close();
				buffWrite.close();
			}catch(NullPointerException e) { }
			if (testFile.exists()) testFile.delete();
			setOptions(false);
			output.getItems().clear();
			throw new IOException(errMsg);
		}
		msg.setText("Compression successful: "+filename+extension);
	}
	/**
	 * Decompresses the file.<br><br>
	 *
	 * Operations in order:<p>
	 *  ** Verify magic number.<br>
	 *  ** Get the file extension.<br>
	 *  ** Rebuild the Huffman tree.<br>
	 *  ** Decode the values until PSEUDO_EOF reached.</p>
	 */
	private void decompress() throws IOException {
		progress.setProgress(0);
		dialog.setHeaderText("Decompressing...");
		Task<Boolean> task = new Task<>() {
			@Override
			protected Boolean call() {
				try {
					buffRead = new BufferedInputStream(new FileInputStream(file));
					int items, bytes, last, total, ascii;
					double percentage;
					boolean getExt = false;
					items = last = total = ascii = 0;
					extension = "";
					while (ascii != PSEUDO_EOF) {
						bytes = buffRead.read();
						if (bytes == -1) {
							errMsg = "Decompression failed: Error reading bits.";
							return success;
						}
						else if (!magicAuth) {
							if (total == 0) byteArr = binaryConvert(bytes);
							else if (total == 1) {
								tempByte = binaryConvert(bytes);
								byte[] checkMagic = byteCombine(byteArr,tempByte);
								if (tree.convert(checkMagic) != tree.convert(MAGIC_NUM)) {
									errMsg = "Decompression failed: File corrupted.";
									return success;
								}
								else magicAuth = getExt = true;
							}
						}
						else if (getExt) {
							if (bytes == 58) {
								testFile = new File(filename + extension);
								if (testFile.exists()) {
									errMsg = "Decompression failed: " + filename + extension + " already exists.";
									return success;
								} else {
									remFile = true;
									getExt = false;
									buffWrite = new BufferedOutputStream(new FileOutputStream(filename+extension));
								}
							}
							else extension += (char) bytes;
						}
						else if (items < MAX_ITEMS) {
							byteArr = binaryConvert(bytes);
							for (int w = 0; w < BITS_PER_WORD; w++) {
								items+=tree.addStack(byteArr[w]);
								if (items == MAX_ITEMS) {
									tree.resize();
									tree.reStack();
									tree.rebuild();
									for (int z = w+1; z < BITS_PER_WORD; z++) {
										ascii = tree.getTree().mapBit(byteArr[z]);
										if (ascii == PSEUDO_EOF) break;
										else if (ascii > -1) buffWrite.write(ascii);
									}
									break;
								}
							}
						}
						else {
							byteArr = binaryConvert(bytes);
							for (int w = 0; w < BITS_PER_WORD; w++) {
								ascii = tree.getTree().mapBit(byteArr[w]);
								if (ascii == PSEUDO_EOF) break;
								else if (ascii > -1) buffWrite.write(ascii);
							}
						}
						total++;
						percentage = (double) total / fileSize;
						percentage *= 100;
						percentage = Math.floor(percentage);
						if (percentage != last) {
							last = (int) percentage;
							updateProg(percentage/100);
						}				
					}
					success = true;
					buffRead.close();
					buffWrite.close();
				}catch (Exception e) { errMsg = "Decompression failed: Error in read/write process."; }
				return success;
			}
		};
		task.setOnSucceeded(e -> { dialog.close(); });
		task.setOnCancelled(e -> {  
			dialog.close();
			errMsg = "Decompression cancelled.";
		});
		task.setOnFailed(e -> {  
			dialog.close();
			errMsg = "Decompression failed.";
		});
		new Thread(task).start();
		dialog.showAndWait();
		if (!success) {
			try {
				buffRead.close();
				buffWrite.close();
			}catch(NullPointerException e) { }
			if (testFile != null) {
				if (testFile.exists() && remFile) testFile.delete();
			}
			dialog.close();
			throw new IOException(errMsg);
		}
		msg.setText("Decompression successful: "+filename+extension);
	}
	@Override
	public void add(int i) { counts[i]++; }
	/**
	 * Adds a bit to the buffer using bitwise operations.
	 * @param b
	 * @throws IOException
	 */
	private void addBuff(byte b) throws IOException {
		storeBit += b;
		if (bitLoc == BITS_PER_WORD - 1) {
			buffWrite.write(storeBit);
			bitLoc = storeBit = 0;
			return;
		}
		storeBit <<= 1;
		bitLoc++;
	}
	/**
	 * Flushes the buffer.
	 * @throws IOException
	 */
	private void flushBuff() throws IOException {
		while (bitLoc < BITS_PER_WORD) {
			if (bitLoc == 0) break;
			addBuff((byte) 0);
		}
		buffWrite.close();
	}
	@Override
	public int getCount(int ch) { return counts[ch]; }
	/**
	 * Transfers unique byte counts into a string array for use with GUI.
	 */
	public void loadCounts() {
		for (int x = 0; x < MAX_COUNT; x++) {
			if (counts[x] == 0) continue;
			countsView.add(Integer.toString(x)+" "+Integer.toString(counts[x]));
		}
	}
	/**
	 * Calculates whether compression will save any space.
	 * @return (boolean)
	 */
	private boolean checkSavings() {
		int bitTotal = 0, key;
		bitTotal+= MAGIC_NUM.length;
		bitTotal+= filename.length();
		bitTotal += extension.length()+1;
		bitTotal += MAX_ITEMS;
		bitTotal += (BITS_PER_WORD * map.size());
		for (Entry x : map.entrySet()) {
			key = (int) x.getKey();
			if (key < MAX_COUNT) bitTotal += (counts[key] * x.getValue().toString().length());
			else bitTotal += x.getValue().toString().length();
		}
		bitTotal /= BITS_PER_WORD;
		return (bitTotal < fileSize);
	}
	/**
	 * Converts decimal to binary.
	 *
	 * @param decimal (int)
	 * @return (byte[])
	 */
	private byte[] binaryConvert(int decimal) {
		byte[] binary = new byte[BITS_PER_WORD];
		int index = BITS_PER_WORD;
		while (index > 0) {
			index--;
			binary[index] = (byte) (decimal % 2);
			decimal /= 2;
		}
		return binary;
	}
	/**
	 * Combines two bytes. Used for magic number.
	 * @param arr1 (byte[])
	 * @param arr2 (byte[])
	 * @return
	 */
	private byte[] byteCombine(byte[] arr1, byte[] arr2) {
		byte[] longByte = new byte[MAGIC_NUM.length];
		for (int w = 0; w < BITS_PER_WORD; w++) longByte[w] = arr1[w];
		for (int w = 0; w < BITS_PER_WORD; w++) longByte[w+8] = arr2[w];
		return longByte;
	}
	@Override
	public void showCounts() {
		output.getItems().clear();
		clear();
		loadCounts();
		output.getItems().addAll(countsView);
		setOptions(true);
	}
	@Override
	public void showCodings() {	
		output.getItems().clear();
		output.getItems().addAll(codings);
	}
	/**
	 * Clears the counts viewer string array
	 */
	@Override
	public void clear() {
		countsView.clear();
	}
	@Override
	public void set(int i, String value) { 
		counts[i] = Integer.parseInt(value);
		countsView.clear();
		showCounts();
	}
	@Override
	public void setViewer(ListView<String> output) { this.output = output; }
	public void setDiag(Dialog<Void> dialog) { this.dialog = dialog; }
	public void setProg(ProgressBar progress) { this.progress = progress; }
	public void setMsg(TextField msg) { this.msg = msg; }
	public void setMenu(Menu options) { this.options = options; }
	public void setToggle(ToggleGroup toggle) { this.toggle = toggle; }
	public void setForced(boolean forcedComp) { this.forcedComp = forcedComp; }
	/**
	 * Disables/reenables GUI options for compressed file.
	 *
	 * @param x
	 */
	private void setOptions(boolean x) {
		toggle.getToggles().get(0).setSelected(x);
		options.getItems().get(0).setDisable(!x);
		options.getItems().get(1).setDisable(!x);
	}
	/**
	 * Updates the progress bar.
	 *
	 * @param percentage
	 */
	private void updateProg(double percentage) {
		Platform.runLater(new Runnable() {
        	@Override
			public void run() { progress.setProgress(percentage); }
         });
	}
}