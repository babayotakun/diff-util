package org.outerj.daisy.diff.html.dom;

import org.outerj.daisy.diff.html.modification.Modification;
import org.outerj.daisy.diff.html.modification.ModificationType;

/**
 * Fake text node. This is actual a wrapper for the tag nodes.
 */
public class HiddenNoteNode extends TextNode {
    private TagNode root;

    public HiddenNoteNode(TagNode wrapper, TagNode parent) {
        super(parent, wrapper);
        setModification(new Modification(ModificationType.NONE, ModificationType.NONE));
        root = wrapper;
    }

    public boolean equals(Object other) {
        return other == this;
    }

    @Override
    public boolean isSameText(Object other) {
        return false;
    }

    public TagNode getRealTagNode() {
        return root;
    }
}
