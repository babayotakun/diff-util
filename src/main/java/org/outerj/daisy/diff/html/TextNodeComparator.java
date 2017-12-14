/*
 * Copyright 2007 Guy Van den Broeck
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.outerj.daisy.diff.html;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.compare.rangedifferencer.IRangeComparator;
import org.outerj.daisy.diff.html.ancestor.AncestorComparator;
import org.outerj.daisy.diff.html.ancestor.AncestorComparatorResult;
import org.outerj.daisy.diff.html.dom.BodyNode;
import org.outerj.daisy.diff.html.dom.DomTree;
import org.outerj.daisy.diff.html.dom.HiddenNoteNode;
import org.outerj.daisy.diff.html.dom.Node;
import org.outerj.daisy.diff.html.dom.SeparatingNode;
import org.outerj.daisy.diff.html.dom.TagNode;
import org.outerj.daisy.diff.html.dom.TextNode;
import org.outerj.daisy.diff.html.dom.helper.LastCommonParentResult;
import org.outerj.daisy.diff.html.modification.Modification;
import org.outerj.daisy.diff.html.modification.ModificationType;

import static org.outerj.daisy.diff.html.dom.DomTreeBuilder.FAKE_NON_BREAKING_SPACE;
import static org.outerj.daisy.diff.html.dom.TextNodePreprocessor.isHiddenElement;
import static org.outerj.daisy.diff.html.dom.TextNodePreprocessor.isHiddenElementRecursive;

/**
 * A comparator that generates a DOM tree of sorts from handling SAX events.
 * Then it can be used to compute the difference between DOM trees and mark
 * elements accordingly.
 */
public class TextNodeComparator implements IRangeComparator, Iterable<TextNode> {

    private List<TextNode> textNodes;

    private List<Modification> lastModified = new ArrayList<>();

    private BodyNode bodyNode;

    private Locale locale;

    public TextNodeComparator(DomTree tree, Locale locale) {
        super();
        this.locale = locale;
        textNodes = tree.getTextNodes();
        bodyNode = tree.getBodyNode();
    }

    public BodyNode getBodyNode() {
        return bodyNode;
    }

    public int getRangeCount() {
        return textNodes.size();
    }

    public List<TextNode> getTextNodes() {
        return textNodes;
    }

    public void setTextNodes(List<TextNode> textNodes) {
        this.textNodes = textNodes;
    }

    public TextNode getTextNode(int i) {
        return textNodes.get(i);
    }

    private long newID = 0;

    /**
     * Marks the given range as new. In the output, the range will be formatted as
     * specified by the anOutputFormat parameter.
     *
     * @param start
     * @param end
     * @param outputFormat specifies how this range shall be formatted in the output
     */
    public void markAsNew(int start, int end, ModificationType outputFormat) {
        if (end <= start)
            return;

        if (whiteAfterLastChangedPart)
            getTextNode(start).setWhiteBefore(false);

        List<Modification> nextLastModified = new ArrayList<Modification>();
        boolean onlyNonBreakingSpaces = getTextNodes().subList(start, end).stream()
            .map(TextNode::getText)
            .allMatch(text -> StringUtils.containsOnly(text, '\u00A0'));

        if (onlyNonBreakingSpaces) {
            return;
        }

        for (int i = start; i < end; i++) {
            Modification mod = new Modification(ModificationType.ADDED, outputFormat);
            mod.setID(newID);
            linkPreviousModificationsWithCurrent(mod);
            nextLastModified.add(mod);
            getTextNode(i).setModification(mod);
        }
        getTextNode(start).getModification().setFirstOfID(true);
        newID++;
        lastModified = nextLastModified;
    }

    /**
     * Marks the given range as new. In the output, the range will be formatted
     * as "added".
     *
     * @param start
     * @param end
     */
    public void markAsNew(int start, int end) {
        markAsNew(start, end, ModificationType.ADDED);
    }

    public boolean rangesEqual(int i1, IRangeComparator rangeComp, int i2) {
        return rangeComp instanceof TextNodeComparator
            && getTextNode(i1).isSameText(((TextNodeComparator) rangeComp).getTextNode(i2));
    }

    public boolean skipRangeComparison(int arg0, int arg1, IRangeComparator arg2) {
        return false;
    }

    private long changedID = 0;

    private boolean changedIDUsed = false;

    public void handlePossibleChangedPart(int leftstart, int leftend,
                                          int rightstart, int rightend, TextNodeComparator leftComparator) {
        int i = rightstart;
        int j = leftstart;

        if (changedIDUsed) {
            changedID++;
            changedIDUsed = false;
        }

        List<Modification> nextLastModified = new ArrayList<Modification>();

        String changes = null;
        while (i < rightend && j < leftend) {
            AncestorComparator acthis = new AncestorComparator(getTextNode(i).getParentTree());
            AncestorComparator acother = new AncestorComparator(leftComparator.getTextNode(j).getParentTree());

            AncestorComparatorResult result = acthis.getResult(acother, locale);

            if (result.isChanged()) {

                Modification mod = new Modification(ModificationType.CHANGED, ModificationType.CHANGED);

                if (!changedIDUsed) {
                    mod.setFirstOfID(true);
                    if (nextLastModified.size() > 0) {
                        lastModified = nextLastModified;
                        nextLastModified = new ArrayList<>();
                    }
                } else if (result.getChanges() != null && !result.getChanges().equals(changes)) {
                    changedID++;
                    mod.setFirstOfID(true);
                    if (nextLastModified.size() > 0) {
                        lastModified = nextLastModified;
                        nextLastModified = new ArrayList<>();
                    }
                }

                linkPreviousModificationsWithCurrent(mod);
                nextLastModified.add(mod);

                mod.setChanges(result.getChanges());
                mod.setHtmlLayoutChanges(result.getHtmlLayoutChanges());
                mod.setID(changedID);

                getTextNode(i).setModification(mod);
                changes = result.getChanges();
                changedIDUsed = true;
            } else if (changedIDUsed) {
                changedID++;
                changedIDUsed = false;
            }

            i++;
            j++;
        }

        if (nextLastModified.size() > 0)
            lastModified = nextLastModified;

    }

    // used to remove the whitespace between a red and green block
    private boolean whiteAfterLastChangedPart = false;
    private long deletedID = 0;

    /**
     * Marks the given range as deleted. In the output, the range will be
     * formatted as specified by the parameter anOutputFormat.
     */
    public void markAsDeleted(int start, int end, TextNodeComparator oldComp,
                              int before, int after, ModificationType outputFormat) {

        if (end <= start)
            return;

        whiteAfterLastChangedPart = before > 0 && getTextNode(before - 1).isWhiteAfter();

        List<Modification> nextLastModified = new ArrayList<Modification>();

        List<Node> initialDeletedNodes = new ArrayList<>(end - start);
        for (int i = start; i < end; i++) {
            Modification mod = new Modification(ModificationType.REMOVED, outputFormat);
            mod.setID(deletedID);
            linkPreviousModificationsWithCurrent(mod);
            nextLastModified.add(mod);

            // oldComp is used here because we're going to move its deleted
            // elements
            // to this tree!
            oldComp.getTextNode(i).setModification(mod);
            initialDeletedNodes.add(oldComp.getTextNode(i));
        }
        oldComp.getTextNode(start).getModification().setFirstOfID(true);

        List<Node> deletedNodes = getDeletedNodes(initialDeletedNodes);
        //deletedNodes = getDeletedNodes(oldComp, start);
        // helps in case of non-formatted text changed to the usual.
        boolean onlyNonBreakingSpaces = deletedNodes.stream()
            .allMatch(node ->
                (TextNode.class.isInstance(node) && StringUtils.containsOnly(((TextNode) node).getText(), '\u00A0'))
                    || (TagNode.class.isInstance(node) && Objects.equals(FAKE_NON_BREAKING_SPACE,
                    ((TagNode) node).getAttributes().getValue(TagNode.CLASS_ATTRIBUTE))));
        if (onlyNonBreakingSpaces) {
            deletedNodes = new ArrayList<>();
        }

        // Set prevLeaf to the leaf after which the old HTML needs to be
        // inserted
        Node prevLeaf = null;
        if (before > 0)
            prevLeaf = getTextNode(before - 1);

        // Set nextLeaf to the leaf before which the old HTML needs to be
        // inserted
        Node nextLeaf = null;
        boolean useAfter = false;

        if (after < getRangeCount()) {

            LastCommonParentResult orderResult = getTextNode(before).getLastCommonParent(getTextNode(after));
            List<TagNode> check = getTextNode(before).getParentTree();
            Collections.reverse(check);
            for (TagNode curr : check) {
                if (curr == orderResult.getLastCommonParent()) {
                    break;
                } else if (curr.isBlockLevel()) {
                    useAfter = true;
                    break;
                }
            }
            if (!useAfter) {
                check = getTextNode(after).getParentTree();
                Collections.reverse(check);
                for (TagNode curr : check) {
                    if (curr == orderResult.getLastCommonParent()) {
                        break;
                    } else if (curr.isBlockLevel()) {
                        useAfter = true;
                        break;
                    }
                }
            }
        } else {
            useAfter = false;
        }
        if (useAfter)
            nextLeaf = getTextNode(after);
        else if (before < getRangeCount())
            nextLeaf = getTextNode(before);

        while (deletedNodes.size() > 0) {
            LastCommonParentResult prevResult, nextResult;
            if (prevLeaf != null) {
                prevResult = prevLeaf.getLastCommonParent(deletedNodes.get(0));
            } else {
                prevResult = new LastCommonParentResult();
                prevResult.setLastCommonParent(getBodyNode());
                prevResult.setIndexInLastCommonParent(-1);
            }
            if (nextLeaf != null) {
                nextResult = nextLeaf.getLastCommonParent(deletedNodes.get(deletedNodes.size() - 1));
            } else {
                nextResult = new LastCommonParentResult();
                nextResult.setLastCommonParent(getBodyNode());
                nextResult.setIndexInLastCommonParent(getBodyNode().getNbChildren());
            }

            if (prevResult.getLastCommonParentDepth() == nextResult.getLastCommonParentDepth()) {
                // We need some metric to choose which way to add...
                if (deletedNodes.get(0).getParent() == deletedNodes.get(deletedNodes.size() - 1).getParent()
                    && prevResult.getLastCommonParent() == nextResult.getLastCommonParent()) {
                    // The difference is not in the parent
                    prevResult.setLastCommonParentDepth(prevResult.getLastCommonParentDepth() + 1);

                } else {
                    // The difference is in the parent, so compare them
                    // now THIS is tricky
                    double distancePrev = deletedNodes
                        .get(0)
                        .getParent()
                        .getMatchRatio(prevResult.getLastCommonParent());
                    double distanceNext = deletedNodes
                        .get(deletedNodes.size() - 1)
                        .getParent()
                        .getMatchRatio(nextResult.getLastCommonParent());
                    //nextResult.setLastCommonParentDepth(nextResult.getLastCommonParentDepth() + 1);

                    if (distancePrev <= distanceNext) {
                        // insert after the previous node
                        prevResult.setLastCommonParentDepth(prevResult.getLastCommonParentDepth() + 1);
                    } else {
                        // insert before the next node
                        nextResult.setLastCommonParentDepth(nextResult.getLastCommonParentDepth() + 1);
                    }
                }

            }

            if (prevResult.getLastCommonParentDepth() > nextResult.getLastCommonParentDepth()) {

                // Inserting at the front

                // Temporal disabled.
               /* if (prevResult.isSplittingNeeded()) {
                        prevLeaf.getParent().splitUntill(prevResult.getLastCommonParent(), prevLeaf, true);
                }*/
                prevLeaf = deletedNodes.remove(0).copyTree();
                prevLeaf.setParent(prevResult.getLastCommonParent());
                prevResult.getLastCommonParent().addChild(prevResult.getIndexInLastCommonParent() + 1, prevLeaf);

            } else if (prevResult.getLastCommonParentDepth() < nextResult.getLastCommonParentDepth()) {
                // Inserting at the back
                if (nextResult.isSplittingNeeded()) {
                    boolean splitOccured = nextLeaf.getParent().splitUntill(nextResult.getLastCommonParent(), nextLeaf, false);

                    if (splitOccured) {
                        // The place where to insert is shifted one place to the
                        // right
                        nextResult.setIndexInLastCommonParent(nextResult.getIndexInLastCommonParent() + 1);
                    }
                }
                nextLeaf = deletedNodes.remove(deletedNodes.size() - 1).copyTree();
                nextLeaf.setParent(nextResult.getLastCommonParent());
                nextResult.getLastCommonParent().addChild(nextResult.getIndexInLastCommonParent(), nextLeaf);
            } else
                throw new IllegalStateException();

        }
        lastModified = nextLastModified;
        deletedID++;
    }

    private List<Node> getDeletedNodes(List<Node> initialDeletedNodes) {
        List<Node> deletedNodes = new ArrayList<>();
        boolean parentWasAdded;
        do {
            parentWasAdded = false;
            for (Node text : initialDeletedNodes) {
                TagNode possiblyDeletedParent = text.getParent();
                // was processed
                if (possiblyDeletedParent.isDeletedSetMark() != null && possiblyDeletedParent.isDeletedSetMark()) {
                    continue;
                }
                boolean parentWasDeleted = possiblyDeletedParent.isDeletedSetMark() == null
                    && possiblyDeletedParent.getChildren().size() < 1000
                    && possiblyDeletedParent.getChildren().stream().allMatch(this::wasDeleted);
                if (parentWasDeleted) {
                    deletedNodes.add(possiblyDeletedParent);
                    parentWasAdded = true;
                    possiblyDeletedParent.setDeletedSetMark(true);
                } else {
                    deletedNodes.add(text);
                    possiblyDeletedParent.setDeletedSetMark(false);
                }
            }

            if (parentWasAdded) {
                initialDeletedNodes = deletedNodes;
                deletedNodes = new ArrayList<>();
            }

        } while (parentWasAdded);
        return deletedNodes;
    }

    private boolean wasDeleted(Node child) {
        boolean hidden = child instanceof TagNode
            && ((isHiddenElement((TagNode) child) || isHiddenElementRecursive((TagNode) child))
            || ((TagNode) child).getChildren().stream().allMatch(this::wasDeleted));
        boolean note = child instanceof HiddenNoteNode || child instanceof SeparatingNode;
        boolean deleted = child instanceof TextNode
            && ((TextNode) child).getModification().getType() == ModificationType.REMOVED
            && ((TextNode) child).getModification().getID() == deletedID;
        return hidden || deleted || note;
    }

    private List<Node> getDeletedNodes(TextNodeComparator oldComp, int nodeNumber) {
        return oldComp.getBodyNode().getMinimalDeletedSet(deletedID);
    }

    /**
     * Marks the given range as deleted. In the output, the range will be
     * formatted as "removed".
     *
     * @param start
     * @param end
     * @param oldComp
     * @param before
     */
    public void markAsDeleted(int start, int end, TextNodeComparator oldComp,
                              int before, int after) {
        markAsDeleted(start, end, oldComp, before, after, ModificationType.REMOVED);
    }

    public void expandWhiteSpace() {
        getBodyNode().expandWhiteSpace();
    }

    public Iterator<TextNode> iterator() {
        return textNodes.iterator();
    }

    private void linkPreviousModificationsWithCurrent(Modification mod) {
        if (lastModified.size() > 0) {
            mod.setPrevious(lastModified.get(0));
            if (lastModified.get(0).getNext() == null) {
                for (Modification lastMod : lastModified) {
                    lastMod.setNext(mod);
                }
            }
        }
    }
}