package com.vapula87.huffman.structures;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * com.vapula87.huffman.structures.SinglyLinkedList class
 * @param <T>
 * @author Michael Hackett
 */
public class SinglyLinkedList<T> implements Iterable<T> {
	private Node<T> head;
	protected int size = 0;
	private static class Node<T> {
		private Node<T> tail;
		private T element;
		private static int num = 0;
		private Node(T elem) {
			tail = null;
			element = elem;
		}
		//See descriptions down below
		private void addNode(T elem) {
			if (tail == null) tail = new Node<>(elem);
			else tail.addNode(elem);
		}
		private void printNodes() {
			num++;
			if (num == 1) System.out.print("[");
			if (tail == null) {
				System.out.println(element + "]");
				num = 0;
			}
			else {
				System.out.print(element + ", ");
				tail.printNodes();
			}
		}
		private void insert(T elem, int loc, Node<T> last) {
			num++;
			if (num == loc) {
				Node<T> newNode = new Node<>(elem);
				newNode.tail = this;
				last.tail = newNode;
				num = 0;
			}
			else tail.insert(elem, loc, this);
		}
		private void delete(Node<T> last, int loc) {
			num++;
			if (loc == num) {
				last.tail = tail;
				num = 0;
			}
			else tail.delete(this, loc);
		}
		private T retrieve(int loc) {
			num++;
			T elem;
			if (loc == num) {
				num = 0;
				return element;
			}
			else elem = tail.retrieve(loc);
			return elem;
		}		
		private void replace(T elem, int loc) {
			num++;
			if (num == loc) {
				element = elem;
				num = 0;
			}
			else tail.replace(elem, loc);
		}
	}
	public SinglyLinkedList() { head = null; }
	//Add element to end
	public void addLast(T elem) {
		size++;
		if (head == null) head = new Node<>(elem);
		else head.addNode(elem);
	}
	//Remove last element
	public void removeLast() { 
		if (isEmpty()) return;
		else if (size == 1) head = null;
		else head.delete(head, size); 
		size--;
	}
	//Remove first element
	public T removeFirst() { 
		if (isEmpty()) return null;
		size--;
		Node<T> temp = head;
		head = head.tail;
		return temp.element;
	}
	//Create new head (new first element)
	public void addFirst(T elem) {
		if (head == null) addLast(elem);
		else {
			size++;
			Node<T> temp = head;
			head = new Node<>(elem);
			head.tail = temp;		
		}
	}
	//Insert an element 
	public void insert(T elem, int loc) {
		if (loc > size+1) System.out.println("No element at location "+loc);
		else if (loc < 1) System.out.println("List starts at 1.");
		else if (loc == 1) addFirst(elem);
		else if (loc == size+1) addLast(elem);
		else {
			size++;
			head.insert(elem, loc, head);
		}
	}
	//Delete an element
	public void delete(int loc) {
		if (size == 0) System.out.println("Nothing to delete.");
		else if (loc > size) System.out.println("No element at location "+loc);
		else if (loc < 1) System.out.println("List starts at 1");
		else if (loc == 1) removeFirst();
		else if (loc == size) removeLast();
		else {
			size --;
			head.delete(head, loc);
		}
	}
	public boolean isEmpty() { return size == 0; }
	public int size() { return size; }
	//Retrieve first element
	public T first() { return head.element; }
	//Retrieve last element
	public T last() { return retrieve(size); }
	//Retrieve an element
	public T retrieve(int loc) { 
		T elem;
		if (size == 0) throw new IllegalArgumentException("List is empty.");
		else if (loc > size) throw new IllegalArgumentException("List stops at "+size);
		else if (loc == 0) throw new IllegalArgumentException("List starts at 1");
		elem = head.retrieve(loc);
		return elem;
	}
	//Replace an element
	public void replace(T elem, int loc) {
		if (size == 0) System.out.println("List is empty.");
		else if (loc < 1) System.out.println("List starts at 1.");
		else if (loc > size) System.out.println("No element at location "+loc);
		else head.replace(elem, loc);
	}
	//Swap two elements
	public void swap(int loc1, int loc2) {
		if (size == 0) System.out.println("List is empty.");
		else if (loc1 < 0 || loc2 < 0) System.out.println("List starts at 1");
		else if (loc1 > size || loc2 > size) System.out.println("List stops at "+size);
		else {
			T elem1 = retrieve(loc1), elem2 = retrieve(loc2);
			replace(elem1, loc2);
			replace(elem2, loc1);
		}
	}
	//Print all elements
	public void printList() {
		if (head == null) System.out.println("List empty.");
		else head.printNodes();
	}
	//Lazy iterator
	@Override
	public Iterator<T> iterator() { return new ListIterator(); }
	private class ListIterator implements Iterator<T> {
		int loops = 0;
		Node<T> lastNode = null;
		@Override
		public boolean hasNext() {  return (loops < size); }
		@Override
		public T next() {
			loops++;
			if (lastNode == null) lastNode = head;
			else lastNode = lastNode.tail;
			return lastNode.element;
		}

	}
	//Snapshot iterator
	public Iterable<T> snapshot() { return snapIt(); }
	private Iterable<T> snapIt() {
		ArrayList<T> list = new ArrayList<>();
		Node<T> node = head;
		while (node != null) {
			list.add(node.element);
			node = node.tail;
		}
		return list;
	}
}
