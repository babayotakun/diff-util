package org.outerj.daisy.diff.html.dom;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Created by d.kalach on 6/22/17.
 */
public class TextNodePreprocessor {
    private static final String DISPLAY_NONE_CLASS = "color__800000 display_none";
    private static final String HIDDEN_NOTE = "hidden-note";
    private static final String CLASS_ATTRIBUTE = "class";
    private static final int NEXT_NODES_IN_SEGMENT_DEFINITION = 5;
    private BodyNode bodyNode;
    private List<TextNode> textNodes;
    private List<Pair<String, List<TextNode>>> segments = new ArrayList<>();


    public TextNodePreprocessor(BodyNode bodyNode, List<TextNode> textNodes) {
        this.bodyNode = bodyNode;
        this.textNodes = textNodes;
    }

    public List<Pair<String, List<TextNode>>> collectSegmentNodes() {
        collectSegmentNodes(bodyNode);
        return segments;
    }

    public static void removeUnprocessableNodes(TagNode parent, List<TextNode> textNodes) {
        removeUnprocessableNodesRecursive(parent, textNodes);
        markAllHiddenNotesAsAlwaysDifferent(parent, textNodes);
    }

    private static void removeUnprocessableNodesRecursive(TagNode parent, List<TextNode> textNodes) {
        for (Node current : parent) {
            if (current instanceof TagNode) {
                TagNode currentTag = (TagNode) current;
                String classAttr = currentTag.getAttributes().getValue(CLASS_ATTRIBUTE);
                if (Objects.equals(DISPLAY_NONE_CLASS, classAttr)) {
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

    private static void markAllHiddenNotesAsAlwaysDifferent(TagNode parent, List<TextNode> textNodes) {
        for (int i = 0; i < parent.getNbChildren(); i++) {
            Node current = parent.getChild(i);
            if (current instanceof TagNode) {
                TagNode currentTag = (TagNode) current;
                String classAttr = currentTag.getAttributes().getValue(CLASS_ATTRIBUTE);
                if (classAttr != null && classAttr.contains(HIDDEN_NOTE)) {
                    Node fakeNode = new HiddenNoteNode(currentTag, parent);
                    deleteAllTextNodesRecursiveWithReplacement(currentTag, textNodes, (TextNode) fakeNode, new AtomicBoolean(true));
                } else {
                    markAllHiddenNotesAsAlwaysDifferent(currentTag, textNodes);
                }
            }
        }
    }

    private static void deleteAllTextNodesRecursiveWithReplacement(TagNode parent, List<TextNode> textNodes, TextNode replacement,
                                                                   AtomicBoolean onlyOnceMarker) {
        for (Node child : parent) {
            if (child instanceof TextNode && !(child instanceof HiddenNoteNode)) {
                if (onlyOnceMarker.get()) {
                    int index = textNodes.indexOf(child);
                    if (index > -1) {
                        textNodes.set(index, replacement);
                        onlyOnceMarker.set(false);
                    }
                } else {
                    textNodes.remove(child);
                }
            } else if (child instanceof TagNode) {
                deleteAllTextNodesRecursiveWithReplacement((TagNode) child, textNodes, replacement, onlyOnceMarker);
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
                    segments.add(new ImmutablePair<>(currentSegmentId, currentTextNodes));
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
