/*
 * Copyright 2004 Guy Van den Broeck
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
package org.outerj.daisy.diff;

import java.io.IOException;
import java.util.Locale;
import org.eclipse.compare.rangedifferencer.HTMLDiffer;
import org.outerj.daisy.diff.html.HtmlSaxDiffOutput;
import org.outerj.daisy.diff.html.TextNodeComparator;
import org.outerj.daisy.diff.html.dom.DomTreeBuilder;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class DaisyDiff {
    private final DiffMode mode;
    private final int chunkSize;
    private final boolean forcedChunks;

    public DaisyDiff(DiffMode mode, int chunkSize, boolean forcedChunks) {
        this.mode = mode;
        this.chunkSize = chunkSize;
        this.forcedChunks = forcedChunks;
    }

    public void diffHTML(InputSource oldSource, InputSource newSource, ContentHandler consumer, String prefix, Locale locale)
        throws SAXException, IOException {

        DomTreeBuilder oldHandler = new DomTreeBuilder(true);
        XMLReader xr1 = XMLReaderFactory.createXMLReader();
        xr1.setContentHandler(oldHandler);
        xr1.parse(oldSource);
        TextNodeComparator leftComparator = new TextNodeComparator(oldHandler, locale);

        DomTreeBuilder newHandler = new DomTreeBuilder(true);
        XMLReader xr2 = XMLReaderFactory.createXMLReader();
        xr2.setContentHandler(newHandler);
        xr2.parse(newSource);

        TextNodeComparator rightComparator = new TextNodeComparator(newHandler, locale);

        HtmlSaxDiffOutput output = new HtmlSaxDiffOutput(consumer, prefix);
        HTMLDiffer differ = new HTMLDiffer(output);

        DiffMode currentMode = mode;
        if (forcedChunks && (leftComparator.getTextNodes().size() > chunkSize || rightComparator.getTextNodes().size() > chunkSize)) {
            currentMode = DiffMode.CHUNKED;
        }
        differ.diff(leftComparator, rightComparator, currentMode, chunkSize);
    }
}
