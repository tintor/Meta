package tintor.devel.work_stealing_deque;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

class WorkStealingDeque {
}

class LinkedDynamicDeque<T> {
	/** Can be safely called only from owner thread. */
	public void PushBottom(final T data) {
		final Node curr = bottom;
		curr.data = data; // Write data in current bottom
		bottom = curr.prev = new Node(curr); // Allocate and link a new node
	}

	/** Steals items from top of the deque. Can be safely called from different threads. */
	@SuppressWarnings("unchecked")
	public T PopTop() {
		final Top currTop = top.get(); // Read Top
		final Node currBottom = bottom; // Read Bottom

		// Emptiness test
		if (currBottom == currTop.node || currBottom == currTop.node.next) {
			if (currTop == top.get()) return null; // EMPTY
			return null; // ABORT;
		}

		final Object value = currTop.node.data; // Read value
		if (!top.compareAndSet(currTop, new Top(currTop.tag + 1, currTop.node.prev))) return null; // ABORT

		// TODO free currTop.node if needed
		return (T) value;
	}

	/** Can be safely called only from owner thread. */
	@SuppressWarnings("unchecked")
	public T PopBottom() {
		final Node oldBottom = bottom; // Read Bottom Data
		final Node newBottom = oldBottom.next;
		bottom = newBottom;

		final Top currTop = top.get(); // Read Top

		// Case 1: if Top has crossed Bottom
		if (oldBottom == currTop.node) {
			// Return bottom to its old position:
			bottom = oldBottom;
			return null; // EMPTY
		}

		final Object retVal = newBottom.data; // Read data to be popped

		// Case 2: When popping the last entry in the deque (i.e. deque is empty after the update of bottom).
		if (newBottom == currTop.node) {
			// Try to update Top’s tag so no concurrent PopTop operation will also pop the same entry:
			final Top newTopVal = new Top(currTop.tag + 1, currTop.node);
			if (top.compareAndSet(currTop, newTopVal)) // TODO free old node if needed
				return (T) retVal;
			// CAS failed (i.e. a concurrent PopTop operation already popped the last entry):
			bottom = oldBottom;
			return null; // EMPTY
		}

		// Case 3: Regular case (i.e. there was at least one entry in the deque after bottom’s update):
		// TODO free old node if needed
		return (T) retVal;
	}

	private volatile Node bottom = new Node(null);
	private final AtomicReference<Top> top = new AtomicReference<Top>(new Top(0, bottom));

	private static class Node {
		volatile Object data;
		volatile Node prev;
		final Node next;

		Node(final Node next) {
			this.next = next;
		}
	}

	private static class Top {
		final int tag;
		final Node node;

		Top(final int tag, final Node node) {
			this.tag = tag;
			this.node = node;
		}
	}
}

// Based on:
// "A Dynamic-Sized Nonblocking Work Stealing Deque", Danny Hendler, Yossi Lev, Mark Moir, and Nir Shavit, 2005
class LinkedArrayDynamicDeque<T> {
	public LinkedArrayDynamicDeque(final int nodeSize) {
		this.nodeSize = nodeSize;
		bottom = new Bottom(new Node(nodeSize), nodeSize - 1);
		top = new AtomicReference<Top>(new Top(0, bottom.node, 0));
	}

	public void PushBottom(final T data) {
		final Bottom curr = bottom;
		curr.node.array.set(curr.index, data); // Write data in current bottom cell
		if (curr.index != 0)
			bottom = new Bottom(curr.node, curr.index - 1); // Update Bottom
		else { // Allocate and link a new node:
			final Node newNode = new Node(nodeSize);
			newNode.next = curr.node;
			curr.node.prev = newNode;
			bottom = new Bottom(newNode, nodeSize - 1); // Update Bottom
		}
	}

	private boolean EmptinessTest(final Bottom b, final Top t) {
		if (b.node == t.node && (b.index == t.index || b.index == t.index + 1)) return true;
		if (b.node == t.node.next && b.index == 0 && t.index == nodeSize - 1) return true;
		return false;
	}

	@SuppressWarnings("unchecked")
	public T PopTop() {
		final Top currTop = top.get(); // Read Top
		final Bottom currBottom = bottom; // Read Bottom

		if (EmptinessTest(currBottom, currTop)) {
			if (currTop == top.get()) return null; // EMPTY
			return null; // ABORT;
		}

		// currTop.index != 0: stay at current node
		// currTop.index == 0: move to next node and update tag
		final Top newTopVal = currTop.index != 0 ? new Top(currTop.tag, currTop.node, currTop.index - 1)
				: new Top(currTop.tag + 1, currTop.node.prev, nodeSize - 1);

		final Object retVal = currTop.node.array.get(currTop.index); // Read value
		if (!top.compareAndSet(currTop, newTopVal)) return null; // ABORT

		// TODO free old node if needed
		return (T) retVal;
	}

	@SuppressWarnings("unchecked")
	public T PopBottom() {
		final Bottom oldBot = bottom; // Read Bottom Data

		final Bottom newBot = oldBot.index != nodeSize - 1 ? new Bottom(oldBot.node, oldBot.index + 1)
				: new Bottom(oldBot.node.next, 0);
		bottom = newBot;

		final Top currTop = top.get(); // Read Top

		// Case 1: if Top has crossed Bottom
		if (oldBot.node == currTop.node && oldBot.index == currTop.index) {
			// Return bottom to its old position:
			bottom = new Bottom(oldBot.node, oldBot.index);
			return null; // EMPTY
		}

		final Object retVal = newBot.node.array.get(newBot.index); // Read data to be popped

		// Case 2: When popping the last entry in the deque (i.e. deque is empty after the update of bottom).
		if (newBot.node == currTop.node && newBot.index == currTop.index) {
			// Try to update Top’s tag so no concurrent PopTop operation will also pop the same entry:
			final Top newTopVal = new Top(currTop.tag + 1, currTop.node, currTop.index);
			if (top.compareAndSet(currTop, newTopVal)) // TODO free old node if needed
				return (T) retVal;
			// if CAS failed (i.e. a concurrent PopTop operation already popped the last entry):
			// Return bottom to its old position:
			bottom = oldBot;
			return null; // EMPTY
		}

		// Case 3: Regular case (i.e. there was at least one entry in the deque after bottom’s update):
		// TODO free old node if needed
		return (T) retVal;
	}

	private final int nodeSize;
	private volatile Bottom bottom;
	private final AtomicReference<Top> top;

	private static class Bottom {
		final Node node;
		final int index;

		Bottom(final Node node, final int index) {
			this.node = node;
			this.index = index;
		}
	}

	private static class Top {
		final int tag;
		final Node node;
		final int index;

		Top(final int tag, final Node node, final int index) {
			this.tag = tag;
			this.node = node;
			this.index = index;
		}
	}

	private static class Node {
		final AtomicReferenceArray<Object> array;
		volatile Node prev, next;

		Node(final int arraySize) {
			array = new AtomicReferenceArray<Object>(arraySize);
		}
	}
}