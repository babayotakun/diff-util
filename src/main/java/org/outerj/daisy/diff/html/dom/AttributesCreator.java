package org.outerj.daisy.diff.html.dom;

import org.outerj.daisy.diff.html.modification.Modification;
import org.outerj.daisy.diff.html.modification.ModificationType;
import org.xml.sax.helpers.AttributesImpl;

import static org.outerj.daisy.diff.html.dom.TagNode.CLASS_ATTRIBUTE;

/**
 * Class for the creation attributes for added, removed or changed elements.
 */
public class AttributesCreator {
    private static final String ADDED_CLASS = "diff-html-added";
    private static final String REMOVED_CLASS = "diff-html-removed";
    private static final String CHANGED_CLASS = "diff-html-changed";
    private static final String CONFLICT_CLASS = "diff-html-conflict";
    private static final String NEXT_CHANGE_ID_ATTR = "next";
    private static final String CURRENT_CHANGE_ID_ATTR = "changeId";
    private static final String PREV_CHANGE_ID_ATTR = "previous";
    private static final String ID_ATTR = "id";
    private static final String CHANGES_ATTR = "changes";
    private static final String CDATA = "CDATA";
    private static final String NOTE_ADDED_CLASS = "diff-note-added";
    private static final String NOTE_REMOVED_CLASS = "diff-note-removed";
    private String prefix;

    public AttributesCreator(String prefix) {
        this.prefix = prefix;
    }

    public AttributesImpl createAttributes(Modification mod) {
        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute("", CLASS_ATTRIBUTE, CLASS_ATTRIBUTE, CDATA, getClassFromModification(mod));
        addIdIfNeeded(mod, attributes);
        addChangesIfNeeded(mod, attributes);
        addChangeIds(mod, attributes);
        return attributes;
    }

    public AttributesImpl createHiddenNodeAttributes(TagNode realChild, Modification mod) {
        AttributesImpl newAttr = new AttributesImpl();
        String oldClassAttr = realChild.getAttributes().getValue(CLASS_ATTRIBUTE);
        if (mod.getOutputType() == ModificationType.ADDED) {
            newAttr.addAttribute("", CLASS_ATTRIBUTE, CLASS_ATTRIBUTE, CDATA, oldClassAttr + " " + NOTE_ADDED_CLASS);
        } else {
            newAttr.addAttribute("", CLASS_ATTRIBUTE, CLASS_ATTRIBUTE, CDATA, oldClassAttr + " " + NOTE_REMOVED_CLASS);
        }
        return newAttr;
    }

    private void addChangesIfNeeded(Modification mod, AttributesImpl attributes) {
        if (mod.getOutputType() == ModificationType.CHANGED) {
            String changes = mod.getChanges();
            attributes.addAttribute("", CHANGES_ATTR, CHANGES_ATTR, CDATA, changes);
        }
    }

    private void addIdIfNeeded(Modification mod, AttributesImpl attributes) {
        if (mod.isFirstOfID()) {
            attributes.addAttribute("", ID_ATTR, ID_ATTR, CDATA, createChangeId(mod));
        }
    }

    private void addChangeIds(Modification mod, AttributesImpl attrs) {
        attrs.addAttribute("", PREV_CHANGE_ID_ATTR, PREV_CHANGE_ID_ATTR, CDATA, createPreviousChangeId(mod));
        attrs.addAttribute("", CURRENT_CHANGE_ID_ATTR, CURRENT_CHANGE_ID_ATTR, CDATA, createChangeId(mod));
        attrs.addAttribute("", NEXT_CHANGE_ID_ATTR, NEXT_CHANGE_ID_ATTR, CDATA, createNextChangeId(mod));
    }

    private String getClassFromModification(Modification modification) {
        switch (modification.getType()) {
            case CHANGED:
                return CHANGED_CLASS;
            case ADDED:
                return ADDED_CLASS;
            case REMOVED:
                return REMOVED_CLASS;
            case CONFLICT:
                return CONFLICT_CLASS;
            case NONE:
            default:
                throw new IllegalArgumentException("Not supported difference type!");
        }
    }

    private String createPreviousChangeId(Modification modification) {
        return modification.getPrevious() == null ? "first-" + prefix : createChangeId(modification.getPrevious());
    }

    private String createNextChangeId(Modification modification) {
        return modification.getNext() == null ? "last-" + prefix : createChangeId(modification.getNext());
    }

    private String createChangeId(Modification mod) {
        return mod.getOutputType() + "-" + prefix + "-" + mod.getID();
    }
}
