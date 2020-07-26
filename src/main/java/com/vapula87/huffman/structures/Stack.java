package com.vapula87.huffman.structures;

/**
 * Basic stack.
 * @param <E>
 * @author Michael Hackett
 */
public class Stack<E> {
	private Node<E> top = null;
	private int size = 0;
	private static class Node<E> {
		E elem;
		Node<E> next;
		Node(E elem) { this.elem = elem; }
	}
	protected void push(E elem) {
		Node<E> newNode = new Node<>(elem);
		newNode.next = top;
		top = newNode;
		size++;
	}
	protected E pop() { 
		if (isEmpty()) throw new NullPointerException("List is empty.");
		E temp = top.elem;
		top = top.next; 
		size--;
		return temp;
	}
	protected E top() { 
		if (isEmpty()) throw new NullPointerException("List is empty.");
		return top.elem; 
	}
	protected int size() { return size; }
	protected boolean isEmpty() { return Boolean.valueOf(size == 0 ? true : false);	}
}
