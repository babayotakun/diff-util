package org.outerj.daisy.diff.html;

import java.util.Iterator;
import java.util.List;
import org.eclipse.compare.rangedifferencer.IRangeComparator;
import org.outerj.daisy.diff.html.dom.TextNode;

/**
 * Created by d.kalach on 6/22/17.
 */
public class IterableTextNodeComparator implements IRangeComparator, Iterable<TextNode> {
    private List<TextNode> textNodes;

    public IterableTextNodeComparator(List<TextNode> textNodes) {
        this.textNodes = textNodes;
    }

    @Override
    public Iterator<TextNode> iterator() {
        return textNodes.iterator();
    }

    @Override
    public int getRangeCount() {
        return textNodes.size();
    }

    public boolean rangesEqual(int i1, IRangeComparator rangeComp, int i2) {
        return rangeComp instanceof IterableTextNodeComparator
            && getTextNode(i1).isSameText(((IterableTextNodeComparator) rangeComp).getTextNode(i2));
    }

    @Override
    public boolean skipRangeComparison(int length, int maxLength, IRangeComparator other) {
        return false;
    }

    public TextNode getTextNode(int index) {
        return textNodes.get(index);
    }
}
