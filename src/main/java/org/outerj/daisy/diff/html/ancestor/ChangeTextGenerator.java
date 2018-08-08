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
package org.outerj.daisy.diff.html.ancestor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.outerj.daisy.diff.html.ancestor.tagtostring.TagToString;
import org.outerj.daisy.diff.html.ancestor.tagtostring.TagToStringFactory;
import org.outerj.daisy.diff.html.dom.TagNode;
import org.outerj.daisy.diff.html.modification.HtmlLayoutChange;

public class ChangeTextGenerator {
    private static final String LIST_ITEM_START_TAG = "<li>";
    private static final String LIST_ITEM_END_TAG = "</li>";
    private static final String LIST_START_TAG = "<ul class='changelist'>";
    private static final String LIST_END_TAG = "</ul>";
    private List<HtmlLayoutChange> htmlLayoutChanges;
    private AncestorComparator ancestorComparator;
    private AncestorComparator other;
    private TagToStringFactory factory;
    private Locale locale;
    //   Lines won't go longer than this unless a single word it longer than this.
    private static final int MAX_OUTPUT_LINE_LENGTH = 55;

    public ChangeTextGenerator(AncestorComparator ancestorComparator, AncestorComparator other, Locale locale) {
        this.ancestorComparator = ancestorComparator;
        this.other = other;
        this.factory = new TagToStringFactory();
        this.locale = locale;

        htmlLayoutChanges = new ArrayList<>();
    }

    public ChangeText getChanged(RangeDifference... differences) {
        ChangeText txt = new ChangeText(ChangeTextGenerator.MAX_OUTPUT_LINE_LENGTH);
        boolean rootListOpened = false;
        if (differences.length > 1) {
            txt.addHtml(LIST_START_TAG);
            rootListOpened = true;
        }
        for (int j = 0; j < differences.length; j++) {
            RangeDifference d = differences[j];
            boolean nestedListOpened = false;
            if (rootListOpened) {
                txt.addHtml(LIST_ITEM_START_TAG);
            }
            if (d.leftLength() + d.rightLength() > 1) {
                txt.addHtml(LIST_START_TAG);
                nestedListOpened = true;
            }
            processOldStyles(txt, d, nestedListOpened);
            processNewStyles(txt, d, nestedListOpened);
            if (nestedListOpened) {
                txt.addHtml(LIST_END_TAG);
            }
            if (rootListOpened) {
                txt.addHtml(LIST_ITEM_END_TAG);
            }
        }
        if (rootListOpened) {
            txt.addHtml(LIST_END_TAG);
        }
        return txt;

    }

    private void processNewStyles(ChangeText txt, RangeDifference d, boolean nestedListOpened) {
        for (int i = d.rightStart(); i < d.rightEnd(); i++) {
            if (nestedListOpened) {
                txt.addHtml(LIST_ITEM_START_TAG);
            }
            addTagNew(txt, this.getAncestor(i));
            if (nestedListOpened) {
                txt.addHtml(LIST_ITEM_END_TAG);
            }
        }
    }

    private void processOldStyles(ChangeText txt, RangeDifference d, boolean nestedListOpened) {
        for (int i = d.leftStart(); i < d.leftEnd(); i++) {
            if (nestedListOpened) {
                txt.addHtml(LIST_ITEM_START_TAG);
            }
            addTagOld(txt, other.getAncestor(i));
            if (nestedListOpened) {
                txt.addHtml(LIST_ITEM_END_TAG);
            }

        }
    }

    private void addTagOld(ChangeText txt, TagNode ancestor) {
    	TagToString tagToString = factory.create(ancestor, locale);
    	tagToString.getRemovedDescription(txt);
    	htmlLayoutChanges.add(tagToString.getHtmlLayoutChange());
    }

    private void addTagNew(ChangeText txt, TagNode ancestor) {
    	TagToString tagToString = factory.create(ancestor, locale);
    	tagToString.getAddedDescription(txt);
    	htmlLayoutChanges.add(tagToString.getHtmlLayoutChange());
    }

    private TagNode getAncestor(int i) {
        return ancestorComparator.getAncestor(i);
    }

	public List<HtmlLayoutChange> getHtmlLayoutChanges() {
		return htmlLayoutChanges;
	}

}
