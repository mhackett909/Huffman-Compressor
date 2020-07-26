package com.vapula87.huffman.structures;

import com.vapula87.huffman.interfaces.Entry;
import com.vapula87.huffman.utilities.AbstractMap;
import com.vapula87.huffman.compressor.Init;

import java.util.Iterator;
public class HuffTree<T> implements Iterable<T> {
	private LinkedQueue<Node<T>> queue;
	private Node<T> root, mapped;
	private SortedTableMap<Integer, String> map;
	private StringBuilder code;
	private int size = 0;
	private static class Node<T> {
		private Node<T> leftChild;
		private Node<T> rightChild;
		private T elem;
		private Node(T elem) { this.elem = elem; }
		private void addChild(T elem) { 
			if (leftChild == null) leftChild = new Node<T>(elem);
			else rightChild = new Node<T>(elem);
		}
	}
	/**
	 * Creates an empty Huffman tree for use with the decompressor.
	 *
	 */
	public HuffTree() {	
		queue = new LinkedQueue<>(); 
		mapped = null;
	}
	/**
	 * Creates a new Huffman tree using input from the file.<br><br>
	 * Data is transferred from the PriorityQueue to a Linked Binary Tree via a Stack.<br>
	 * The data is inserted into the tree in a breadth-first manner.
	 * @param input (com.vapula87.huffman.structures.Stack)
	 */
	public HuffTree(Stack<T> input) { 
		root = new Node<T>(input.pop());
		queue = new LinkedQueue<>();
		code = new StringBuilder();
		map = new SortedTableMap<>();
		T first, second;
		Node<T> temp;
		size++;
		while (!input.isEmpty()) {	
			first = input.pop();
			second = input.pop();
			temp = breadthFirst();
			temp.addChild(first);
			temp.addChild(second);
			size+=2;
		}
	}
	/**
	 * This breadth-first insertion algorithm simply ensures entries with values do not receive children.<br>
	 *
	 * @return (Node)
	 */
	protected Node<T> breadthFirst() {
		Node<T> found = null;
		queue.enqueue(root);
		while (!queue.isEmpty()) {
			found = queue.dequeue();
			if (((Entry) found.elem).getValue() == null) {
				if (found.leftChild == null) break;
			}
			if (found.leftChild != null) {
				queue.enqueue(found.leftChild);
				queue.enqueue(found.rightChild);
			}
		}
		while (queue.dequeue() != null);
		return found;
	}
	/** Breadth-first iterator
	 *
	 * @return (Iterator)
	 */
	@Override
	public Iterator<T> iterator() { 
		queue.enqueue(root);
		return new huffIterator(); 
	}
	private class huffIterator implements Iterator<T> {
		Node<T> next = null;
		@Override
		public boolean hasNext() {  return (!queue.isEmpty()); }
		@Override
		public T next() {
			next = queue.dequeue();
			if (next.leftChild != null) queue.enqueue(next.leftChild);
			if (next.rightChild != null) queue.enqueue(next.rightChild);
			return next.elem;
		}
	}
	protected boolean hasLeft(Node<T> parent) { return parent.leftChild != null; } 
	protected Node<T> left(Node<T> parent) { return parent.leftChild; }
	protected boolean hasRight(Node<T> parent) { return parent.rightChild != null; } 
	protected Node<T> right(Node<T> parent) { return parent.rightChild; }
	protected int size() { return size; }
	/**
	 * Encodes each unique byte using the Huffman tree.<br><br>
	 * Works by counting the number of edges between the root and a character.<br>
	 * Left appends "0". Right appends "1".
	 *
	 * @param parent (Node)
	 */
	public void encode(Node<T> parent) {
		if (parent == null) parent = root;
		if (hasLeft(parent)) {
			code.append("0");
			encode(parent.leftChild);
		}
		if (hasRight(parent)) {
			code.append("1");
			encode(parent.rightChild);
		}
		if (((Entry) parent.elem).getValue() != null) {
			int value = (int) ((Entry) parent.elem).getValue();
			map.put(value, code.toString());
		}
		if (code.length() > 0) code.deleteCharAt(code.length()-1);
	}
	/**
	 * Rebuilds the Huffman tree during decompression.<br><br>
	 * Uses breath-first insertion.
	 * @param input (com.vapula87.huffman.structures.Stack)
	 */
	protected void rebuild(Stack<T> input) {
		root = mapped = new Node<T>(input.pop());
		T first = null;
		T second = null;
		Node<T> temp;
		size++;
		while (!input.isEmpty()) {
			first = input.pop();
			second = input.pop();
			temp = breadthFirst();
			temp.addChild(first);
			temp.addChild(second);
			size+=2;
		}
		((AbstractMap.MapEntry) second).setValue(Init.PSEUDO_EOF);
		while (queue.dequeue() != null);
	}
	/**
	 * Assists with the decoding of the compressed file. Turns the decoding path left or right based on the bits.
	 * @param bit (int)
	 * @return (int)
	 */
	public int mapBit(int bit) {
		int value = -1;
		if (bit == 0) mapped = mapped.leftChild;
		else if (bit == 1) mapped = mapped.rightChild;
		if (((Entry) mapped.elem).getValue() != null) {
			value = (int) ((Entry) mapped.elem).getValue();
			mapped = root;
		}
		return value;
		
	}
	/**
	 * Returns the map of unique bytes to their encodings.
	 * @return
	 */
	public SortedTableMap<Integer,String> getMap() { return map; }
}
