package org.outerj.daisy.diff.html.dom;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by d.kalach on 6/22/17.
 */
public class TextNodePreprocessor {
    private static final String DISPLAY_NONE_CLASS = "color__800000 display_none";
    private static final String CLASS_ATTRIBUTE = "class";
    private static final int NEXT_NODES_IN_SEGMENT_DEFINITION = 5;
    private BodyNode bodyNode;
    private List<TextNode> textNodes;
    private SortedMap<String, List<TextNode>> segments = new TreeMap<>();


    public TextNodePreprocessor(BodyNode bodyNode, List<TextNode> textNodes) {
        this.bodyNode = bodyNode;
        this.textNodes = textNodes;
    }

    public SortedMap<String, List<TextNode>> collectSegmentNodes() {
        collectSegmentNodes(bodyNode);
        return segments;
    }

    public static void removeUnprocessableNodes(TagNode parent, List<TextNode> textNodes) {
        TextNodePreprocessor.removeUnprocessableNodesRecursive(parent, textNodes);
    }

    private static void removeUnprocessableNodesRecursive(TagNode parent, List<TextNode> textNodes) {
        for (Node current : parent) {
            if (current instanceof TagNode) {
                TagNode currentTag = (TagNode) current;
                if (Objects.equals(DISPLAY_NONE_CLASS, currentTag.getAttributes().getValue(CLASS_ATTRIBUTE))) {
                    for (Node child : currentTag) {
                        if (child instanceof TextNode) {
                            // Batch removing is significantly slower!
                            textNodes.remove(child);
                        }
                    }
                } else {
                    removeUnprocessableNodesRecursive(currentTag, textNodes);
                }
            }
        }
    }

    private String currentSegmentId = "0";
    private List<TextNode> currentTextNodes = new ArrayList<>();

    private void collectSegmentNodes(TagNode parent) {
        Iterator<Node> iterator = parent.iterator();
        while (iterator.hasNext()) {
            Node current = iterator.next();
            if (current instanceof TagNode) {
                TagNode currentTag = (TagNode) current;
                String segmentId = getSegmentId(currentTag);
                if (segmentId != null) {
                    segments.put(currentSegmentId, currentTextNodes);
                    currentSegmentId = segmentId;
                    currentTextNodes = new ArrayList<>();
                } else if (!Objects.equals(DISPLAY_NONE_CLASS, currentTag.getAttributes().getValue(CLASS_ATTRIBUTE))) {
                    collectSegmentNodes(currentTag);
                }
            } else if (current instanceof TextNode) {
                currentTextNodes.add((TextNode) current);
            }
        }
    }

    private String getSegmentId(TagNode tag) {
        if (tag.getNbChildren() == NEXT_NODES_IN_SEGMENT_DEFINITION
            && isTextNodeContainingText(tag.getChild(0), "{")
            // Cyrillic M !!!
            && isTextNodeContainingText(tag.getChild(1), "М")) {
            return ((TextNode) tag.getChild(3)).getText();
        }
        return null;
    }

    private boolean isTextNodeContainingText(Node node, String text) {
        return node instanceof TextNode && text.equals(((TextNode) node).getText());
    }
}
