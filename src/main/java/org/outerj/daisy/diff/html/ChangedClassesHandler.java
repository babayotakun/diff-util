package org.outerj.daisy.diff.html;

import java.util.Collections;
import java.util.List;
import org.outerj.daisy.diff.html.dom.Node;
import org.outerj.daisy.diff.html.dom.TagNode;
import org.outerj.daisy.diff.html.dom.TextNode;
import org.outerj.daisy.diff.html.modification.HtmlLayoutChange;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

import static java.util.stream.Collectors.toList;
import static org.outerj.daisy.diff.html.dom.DomTreeBuilder.FAKE_NON_BREAKING_SPACE;
import static org.outerj.daisy.diff.html.dom.TagNode.CLASS_ATTRIBUTE;
import static org.outerj.daisy.diff.html.dom.TagNode.OLD_CLASS_ATTRIBUTE;
import static org.outerj.daisy.diff.html.modification.HtmlLayoutChange.Type.TAG_ADDED;
import static org.outerj.daisy.diff.html.modification.HtmlLayoutChange.Type.TAG_REMOVED;
import static org.outerj.daisy.diff.html.modification.ModificationType.CHANGED;

public class ChangedClassesHandler {
    private static final List<String> PROCESSABLE_CHANGES_TAGS = Collections.singletonList("span");

    public void processChangedClasses(TagNode tagNode) {
        for (Node child : tagNode) {
            if (child instanceof TagNode) {
                processChangedClasses((TagNode) child);
            } else if (child instanceof TextNode) {
                TextNode childText = (TextNode) child;
                if (childText.getModification().getType().equals(CHANGED)) {
                    addClassesToParent(childText);
                }
            }
        }
    }

    private void addClassesToParent(TextNode textNode) {
        List<HtmlLayoutChange> allChanges = textNode.getModification().getHtmlLayoutChanges().stream()
            .filter(change -> PROCESSABLE_CHANGES_TAGS.contains(change.getChangedTagQName()))
            .filter(change -> !FAKE_NON_BREAKING_SPACE.equals(change.getChangedTagClass()))
            .collect(toList());
        Collections.reverse(allChanges);

        List<HtmlLayoutChange> removedTags = allChanges.stream()
            .filter(change -> change.getType().equals(TAG_REMOVED))
            .collect(toList());
        Node newChild = processRemovedTags(textNode, removedTags);

        List<HtmlLayoutChange> addedTags = allChanges.stream()
            .filter(change -> change.getType().equals(TAG_ADDED))
            .collect(toList());
        processAddedTags(newChild, addedTags);
    }

    private Node processRemovedTags(Node textNode, List<HtmlLayoutChange> removedTags) {
        Node child = textNode;
        TagNode parent = child.getParent();
        for (HtmlLayoutChange change : removedTags) {
            if (parent == null) {
                break;
            }
            TagNode newParent = new TagNode(null, change.getChangedTagQName(), getAttributes(null, change.getChangedTagClass()));
            resetWhiteSpacesIfNeeded(child, parent);
            parent.replaceChildWithIndex(newParent, child);
            newParent.setParent(parent);
            child.setParent(newParent);
            newParent.addChild(child);
            child = newParent;
        }
        return child;
    }

    private void processAddedTags(Node child, List<HtmlLayoutChange> addedTags) {
        TagNode parent = child.getParent();
        for (HtmlLayoutChange change : addedTags) {
            while (parent != null) {
                if (change.getChangedTagQName().equals(parent.getQName()) && change.getChangedTagClass().equals(parent.getAttributes().getValue(CLASS_ATTRIBUTE))) {
                    parent.setAttributes(getAttributes(parent.getAttributes(), ""));
                    break;
                }
                parent = parent.getParent();
            }
        }
    }

    private Attributes getAttributes(Attributes oldAttributes, String oldTagClass) {
        AttributesImpl newAttr = new AttributesImpl();
        if (oldAttributes != null &&  oldAttributes.getValue(CLASS_ATTRIBUTE) != null) {
            newAttr.addAttribute("", CLASS_ATTRIBUTE, CLASS_ATTRIBUTE, "CDATA", oldAttributes.getValue(CLASS_ATTRIBUTE));
        }
        if (oldTagClass != null) {
            newAttr.addAttribute("", OLD_CLASS_ATTRIBUTE, OLD_CLASS_ATTRIBUTE, "CDATA", oldTagClass);
        }
        return newAttr;
    }

    private void resetWhiteSpacesIfNeeded(Node child, TagNode parent) {
        int childIndex = parent.getIndexOf(child);
        if (childIndex > 0 && parent.getChild(childIndex - 1).isWhiteAfter()) {
            child.setWhiteBefore(false);
        }
        if (childIndex < parent.getNbChildren() - 2 && parent.getChild(childIndex + 1).isWhiteBefore()) {
            child.setWhiteAfter(false);
        }
    }
}
