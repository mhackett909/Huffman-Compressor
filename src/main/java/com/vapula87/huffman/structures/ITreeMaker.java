package com.vapula87.huffman.structures;
import com.vapula87.huffman.interfaces.Entry;
import com.vapula87.huffman.utilities.AbstractMap;
import com.vapula87.huffman.compressor.Init;
import java.util.Comparator;
public class ITreeMaker {
	private HuffTree<Entry<Integer,Integer>> theTree;
	private int writeValue = -1;
	private byte[] byteArr;
	private Stack<Entry<Integer,Integer>> remake;
	/**
	 * Turns the byte-value pairs into a Huffman Tree.<br><br>
	 *
	 * Inserts all entries into a minimum-oriented heap priority queue.<br>
	 * The two minimum entries are removed and their keys are added together.<br>
	 * A new entry is created with the combined value and is inserted into the priority queue.<br>
	 * The two removed entries are added to a stack.<br>
	 * The process continues until all entries are removed from the priority queue.<br>
	 * The stack is then sent to the Huffman tree constructor.
	 *
	 * @param counts (int[])
	 * @author Michael Hackett
	 */
	public ITreeMaker(int[] counts) {	
		Stack<Entry<Integer,Integer>> entries = new Stack<>();
		HeapPriorityQueue<Integer,Integer> minHeap = new HeapPriorityQueue<>(new CustomComparator());
		for (int x = 0; x < Init.MAX_COUNT; x++) {
			if (counts[x] == 0) continue;
			minHeap.insert(counts[x], x);
		}
		minHeap.insert(1, Init.PSEUDO_EOF);
		Entry<Integer, Integer> left, right;
		while (minHeap.size() > 1) {
			left = minHeap.removeMin();
			right = minHeap.removeMin();
			int newValue = left.getKey();
			newValue += right.getKey();
			minHeap.insert(newValue,null);
			entries.push(left);
			entries.push(right);
		}
		if (minHeap.size() == 1) entries.push(minHeap.removeMin());	
		theTree = new HuffTree(entries);
	}
	/**
	 * Makes an empty tree maker for use with decompression.
	 */
	public ITreeMaker() { 
		remake = new Stack<>(); 
		byteArr = new byte[8];
		theTree = new HuffTree();
	}
	/**
	 * Reads in single bits from decompression and adds new entries to a stack.
	 * @param b (int)
	 * @return (boolean)
	 */
	public int addStack(int b) {
		int wrote = 0;
		if (writeValue == -1) {
			if (b == 0) {
				remake.push(new AbstractMap.MapEntry<>(1,null));
				wrote = 1;
			}
			else writeValue++;
		}
		else if (writeValue < 7) byteArr[writeValue++] = (byte) b;
		else {
			byteArr[writeValue] = (byte) b;
			remake.push(new AbstractMap.MapEntry<>(1,convert(byteArr)));
			wrote = 1;
			writeValue = -1;		
		}
		return wrote;
	}
	/**
	 * Reverses the entry stack.
	 */
	public void reStack() {
		LinkedQueue<Entry<Integer, Integer>> oldstack = new LinkedQueue<>();
		while (!remake.isEmpty()) oldstack.enqueue(remake.pop());
		while (!oldstack.isEmpty()) remake.push(oldstack.dequeue());
	}
	/**
	 * Removes extraneous entries (placeholders) from stack.
	 */
	public  void resize() {
		while (remake.top().getValue() == null) remake.pop();
	}
	/**
	 * Sends the recreated stack to the Huffman tree class for rebuilding.
	 */
	public void rebuild() { theTree.rebuild(remake); }
	/**
	 * Returns the Huffman tree.
	 * @return (com.vapula87.huffman.structures.HuffTree)
	 */
	public HuffTree<Entry<Integer, Integer>> getTree() { return theTree; }
	/**
	 * Converts a binary number into a decimal.
	 * @param binary (byte[])
	 * @return (int)
	 */
	public int convert(byte[] binary) {
		int converted = 0, tracker = 0;
		for (int w = binary.length-1; w >= 0; w--) {
			if (binary[w] == 1) converted+=Math.pow(2,tracker);
			tracker++;
		}
		return converted;
	}
	/**
	 * Used to ensure the PSEUDO_EOF character "floats" to the very top of the priority queue.
	 * @param <E>
	 */
	public class CustomComparator<E> implements Comparator<E> {
		@SuppressWarnings({"unchecked"})
		public int compare(E a, E b) throws ClassCastException {
			int compared = ((Comparable<E>) a).compareTo(b);
			if (compared == 0) compared = -1;
		    return compared;
		}
	}
}
