package tintor.devel.xml;

import java.util.Iterator;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class XPath {
	private final XPathExpression expr;

	public XPath(final String path) {
		try {
			expr = XPathFactory.newInstance().newXPath().compile(path);
		}
		catch (final XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}

	public Iterable<Node> each(final Node node) {
		try {
			final NodeList list = (NodeList) expr.evaluate(node, XPathConstants.NODESET);
			return new Iterable<Node>() {
				@Override
				public Iterator<Node> iterator() {
					return new Iterator<Node>() {
						int index = 0;

						@Override
						public boolean hasNext() {
							return index < list.getLength();
						}

						@Override
						public Node next() {
							return list.item(index++);
						}

						@Override
						public void remove() {
							throw new UnsupportedOperationException();
						}
					};
				}
			};
		}
		catch (final XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}
}

public class XML {
	public static Iterable<Node> each(final Node node, final String xpath) {
		try {
			final NodeList list = (NodeList) XPathFactory.newInstance().newXPath().compile(xpath).evaluate(
					node, XPathConstants.NODESET);
			return new Iterable<Node>() {
				@Override
				public Iterator<Node> iterator() {
					return new Iterator<Node>() {
						int index = 0;

						@Override
						public boolean hasNext() {
							return index < list.getLength();
						}

						@Override
						public Node next() {
							return list.item(index++);
						}

						@Override
						public void remove() {
							throw new UnsupportedOperationException();
						}
					};
				}
			};
		}
		catch (final XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}
}
