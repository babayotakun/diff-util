package org.outerj.daisy.diff.html.dom;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.outerj.daisy.diff.html.dom.TagNode.CLASS_ATTRIBUTE;

/**
 * Created by d.kalach on 6/22/17.
 */
public class TextNodePreprocessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(TextNodePreprocessor.class);
    private static final String DISPLAY_NONE_CLASS = "color__800000 display_none";
    private static final String NOT_VISIBLE_ELEMENT = "not-visible-element";
    private static final Pattern CONTENTS_LABEL = Pattern.compile("\\{ОГЛ_[^=]=[*\\d]_([^}]*)}");
    private static final int NEXT_NODES_IN_SEGMENT_DEFINITION = 5;
    private BodyNode bodyNode;
    private List<TextNode> textNodes;
    private List<Pair<String, List<TextNode>>> segments = new ArrayList<>();


    public TextNodePreprocessor(BodyNode bodyNode, List<TextNode> textNodes) {
        this.bodyNode = bodyNode;
        this.textNodes = textNodes;
    }

    public List<Pair<String, List<TextNode>>> collectSegmentNodes(int maxChunkSize) {
        collectSegmentNodes(bodyNode, maxChunkSize);
        segments.add(new ImmutablePair<>(currentContentsLabel, currentTextNodes));
        return segments;
    }

    private String currentContentsLabel = "0";
    private List<TextNode> currentTextNodes = new ArrayList<>();

    private void collectSegmentNodes(TagNode parent, int maxChunkSize) {
        for (Node current : parent) {
            if (current instanceof TagNode) {
                TagNode currentTag = (TagNode) current;
                String contentsLabel = getContentsLabel(currentTag);
                if (contentsLabel != null) {
                    segments.add(new ImmutablePair<>(currentContentsLabel, currentTextNodes));
                    currentContentsLabel = contentsLabel;
                    currentTextNodes = new ArrayList<>();
                } else if (!isHiddenElement(currentTag)) {
                    collectSegmentNodes(currentTag, maxChunkSize);
                }
            } else if (current instanceof TextNode) {
                currentTextNodes.add((TextNode) current);
                if (currentTextNodes.size() > maxChunkSize) {
                    LOGGER.warn("*** Document contains a chuck too large to process, chuck will be split");
                    segments.add(new ImmutablePair<>(currentContentsLabel, currentTextNodes));
                    currentContentsLabel = currentContentsLabel + "0";
                    currentTextNodes = new ArrayList<>();
                }
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

    static boolean isHiddenElement(TagNode tag) {
        return Optional.ofNullable(tag.getAttributes().getValue(CLASS_ATTRIBUTE))
            .map(clazz -> DISPLAY_NONE_CLASS.equals(clazz) || NOT_VISIBLE_ELEMENT.equals(clazz))
            .orElse(false);
    }

    static boolean isHiddenElementRecursive(TagNode tag) {
        if (isHiddenElement(tag)) {
            return true;
        }
        boolean allChildrenAreHidden = true;
        for (Node child : tag) {
            if (child instanceof TagNode) {
                allChildrenAreHidden = allChildrenAreHidden && isHiddenElementRecursive((TagNode) child);
            } else {
                return false;
            }
        }
        return allChildrenAreHidden;
    }

    private boolean isTextNodeContainingText(Node node, String text) {
        return node instanceof TextNode && text.equals(((TextNode) node).getText());
    }
}
