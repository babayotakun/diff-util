package org.outerj.daisy.diff.html.dom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Created by d.kalach on 6/22/17.
 */
public class TextNodePreprocessor {
    private static final String DISPLAY_NONE_CLASS = "color__800000 display_none";
    private static final Pattern CONTENTS_LABEL = Pattern.compile("\\{ОГЛ_.*=.*_(.*)}");
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
        segments.add(new ImmutablePair<>(currentContentsLabel, currentTextNodes));
        return segments;
    }

    public static List<TextNode> removeUnprocessableNodes(TagNode parent, List<TextNode> textNodes) {
        long start = System.currentTimeMillis();
        Map<TextNode, TextNode> textNodesToReplace = new HashMap<>();
        Set<TextNode> textNodesToRemove = new HashSet<>();

        removeUnprocessableNodesRecursive(parent, textNodesToRemove);
        markAllHiddenNotesAsAlwaysDifferent(parent, textNodesToRemove, textNodesToReplace);
        ArrayList<TextNode> newNodes = new ArrayList<>(textNodes.size() - textNodesToRemove.size());
        for (TextNode node : textNodes) {
            if (!(textNodesToRemove.contains(node))) {
                newNodes.add(textNodesToReplace.getOrDefault(node, node));
            }
        }

        System.out.println("Remove unprocessable nodes in " + (System.currentTimeMillis() - start) + " ms");
        return newNodes;
    }

    private static void removeUnprocessableNodesRecursive(TagNode parent, Set<TextNode> textNodesToRemove) {
        for (Node current : parent) {
            if (current instanceof TagNode) {
                TagNode currentTag = (TagNode) current;
                String classAttr = currentTag.getAttributes().getValue(CLASS_ATTRIBUTE);
                if (Objects.equals(DISPLAY_NONE_CLASS, classAttr)) {
                    for (Node child : currentTag) {
                        if (child instanceof TextNode) {
                            textNodesToRemove.add((TextNode) child);
                        }
                    }
                } else {
                    removeUnprocessableNodesRecursive(currentTag, textNodesToRemove);
                }
            }
        }
    }

    private static void markAllHiddenNotesAsAlwaysDifferent(TagNode parent,
                                                            Set<TextNode> textNodesToRemove,
                                                            Map<TextNode, TextNode> textNodesToReplace) {
        for (int i = 0; i < parent.getNbChildren(); i++) {
            Node current = parent.getChild(i);
            if (current instanceof TagNode) {
                TagNode currentTag = (TagNode) current;
                String classAttr = currentTag.getAttributes().getValue(CLASS_ATTRIBUTE);
                if (classAttr != null && classAttr.contains(HIDDEN_NOTE)) {
                    TextNode fakeNode = new HiddenNoteNode(currentTag, parent);
                    deleteAllTextNodesRecursiveWithReplacement(
                        currentTag,
                        fakeNode,
                        new AtomicBoolean(true),
                        textNodesToRemove,
                        textNodesToReplace);
                } else {
                    markAllHiddenNotesAsAlwaysDifferent(currentTag, textNodesToRemove, textNodesToReplace);
                }
            }
        }
    }

    private static void deleteAllTextNodesRecursiveWithReplacement(TagNode parent,
                                                                   TextNode replacement,
                                                                   AtomicBoolean onlyOnceMarker,
                                                                   Set<TextNode> textNodesToRemove,
                                                                   Map<TextNode, TextNode> textNodesToReplace) {
        for (Node child : parent) {
            if (child instanceof TextNode && !(child instanceof HiddenNoteNode)) {
                if (onlyOnceMarker.get()) {
                    textNodesToReplace.put((TextNode) child, replacement);
                    onlyOnceMarker.set(false);
                } else {
                    textNodesToRemove.add((TextNode) child);
                }
            } else if (child instanceof TagNode
                && !Objects.equals(DISPLAY_NONE_CLASS, ((TagNode) child).getAttributes().getValue(CLASS_ATTRIBUTE))) {
                deleteAllTextNodesRecursiveWithReplacement((TagNode) child, replacement, onlyOnceMarker, textNodesToRemove, textNodesToReplace);
            }
        }
    }

    private String currentContentsLabel = "0";
    private List<TextNode> currentTextNodes = new ArrayList<>();

    private void collectSegmentNodes(TagNode parent) {
        Iterator<Node> iterator = parent.iterator();
        while (iterator.hasNext()) {
            Node current = iterator.next();
            if (current instanceof TagNode) {
                TagNode currentTag = (TagNode) current;
                String contentsLabel = getContentsLabel(currentTag);
                if (contentsLabel != null) {
                    segments.add(new ImmutablePair<>(currentContentsLabel, currentTextNodes));
                    currentContentsLabel = contentsLabel;
                    currentTextNodes = new ArrayList<>();
                } else if (!Objects.equals(DISPLAY_NONE_CLASS, currentTag.getAttributes().getValue(CLASS_ATTRIBUTE))) {
                    collectSegmentNodes(currentTag);
                }
            } else if (current instanceof TextNode) {
                currentTextNodes.add((TextNode) current);
            }
        }
    }

    private String getContentsLabel(TagNode tag) {
        if (tag.getNbChildren() >= NEXT_NODES_IN_SEGMENT_DEFINITION
            && isTextNodeContainingText(tag.getChild(0), "{")
            && isTextNodeContainingText(tag.getChild(1), "ОГЛ_В")) {
            StringBuilder id = new StringBuilder();
            for (Node child : tag) {
                if (child instanceof TextNode) {
                    id.append(((TextNode) child).getText());
                } else {
                    return null;
                }
            }
            Matcher matcher = CONTENTS_LABEL.matcher(id.toString());
            if (matcher.matches()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    private boolean isTextNodeContainingText(Node node, String text) {
        return node instanceof TextNode && text.equals(((TextNode) node).getText());
    }
}
