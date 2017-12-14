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
package org.outerj.daisy.diff;

import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.compare.internal.LCSSettings;
import org.eclipse.compare.rangedifferencer.DifferencePreprocessor;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.rangedifferencer.RangeDifferencer;
import org.outerj.daisy.diff.html.ChangedClassesHandler;
import org.outerj.daisy.diff.html.ChunkCreator;
import org.outerj.daisy.diff.html.IterableTextNodeComparator;
import org.outerj.daisy.diff.html.TextNodeComparator;
import org.outerj.daisy.diff.html.dom.TextNode;
import org.outerj.daisy.diff.output.DiffOutput;
import org.outerj.daisy.diff.output.Differ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class HTMLDiffer implements Differ {
    private static final Logger LOGGER = LoggerFactory.getLogger(HTMLDiffer.class);

    private DiffOutput output;
    public HTMLDiffer(DiffOutput dm) {
        output = dm;
    }

    /**
     * {@inheritDoc}
     */
    public int diff(TextNodeComparator leftComparator,
                    TextNodeComparator rightComparator,
                    DiffMode mode,
                    int chunkSize,
                    int maxChunkSize) throws SAXException {
        long findDiffStart = System.currentTimeMillis();
        LOGGER.info("Diff started in " + mode.name() + " mode");
        int diffCount;
        switch (mode) {
            case CHUNKED:
                diffCount = chunkedDiff(leftComparator, rightComparator, chunkSize, maxChunkSize);
                break;
            case FULL:
                diffCount = fullDiff(leftComparator, rightComparator, false);
                break;
            case GREEDY:
                diffCount = fullDiff(leftComparator, rightComparator, true);
                break;
            default:
                throw new RuntimeException("Unsupported");

        }
        LOGGER.info("Difference search completed in " + (System.currentTimeMillis() - findDiffStart));
        LOGGER.info("Total found " + diffCount + " differences");

        new ChangedClassesHandler().processChangedClasses(rightComparator.getBodyNode());
        rightComparator.expandWhiteSpace();
        output.generateOutput(rightComparator.getBodyNode());
        return diffCount;
    }

    private int chunkedDiff(TextNodeComparator leftComparator, TextNodeComparator rightComparator, int chunkSize, int maxChunkSize) {
        int diffCount = 0;
        ChunkCreator chunkCreator = new ChunkCreator(leftComparator, rightComparator, maxChunkSize);
        DifferencePreprocessor preprocessor = new DifferencePreprocessor(leftComparator, rightComparator);
        for (Pair<List<TextNode>, List<TextNode>> diffPair : chunkCreator.getChunks(chunkSize, maxChunkSize)) {
            LOGGER.info("Started diff of pair, left size: {}, right size: {}", diffPair.getLeft().size(), diffPair.getRight().size());
            long pairStart = System.currentTimeMillis();
            RangeDifference[] differences = RangeDifferencer.findDifferences(
                new LCSSettings(),
                new IterableTextNodeComparator(diffPair.getLeft()),
                new IterableTextNodeComparator(diffPair.getRight()));
            leftComparator.setTextNodes(diffPair.getLeft());
            rightComparator.setTextNodes(diffPair.getRight());
            List<RangeDifference> diffToProcess = preprocessor.preProcess(differences);
            diffCount += diffToProcess.size();
            processDifferences(leftComparator, rightComparator, diffToProcess);
            LOGGER.info("Pair processed in {} ms, found {} differences", System.currentTimeMillis() - pairStart, diffToProcess.size());
        }
        return diffCount;
    }

    private int fullDiff(TextNodeComparator leftComparator, TextNodeComparator rightComparator, boolean greedy) {
        int diffCount;
        LCSSettings settings = new LCSSettings();
        if (greedy) {
            settings.setTooLong(Integer.MAX_VALUE);
            settings.setUseGreedyMethod(true);
        }
        RangeDifference[] differences = RangeDifferencer.findDifferences(settings, leftComparator, rightComparator);
        DifferencePreprocessor preprocessor = new DifferencePreprocessor(leftComparator, rightComparator);
        List<RangeDifference> diffToProcess = preprocessor.preProcess(differences);
        diffCount = diffToProcess.size();
        processDifferences(leftComparator, rightComparator, diffToProcess);
        return diffCount;
    }

    private void processDifferences(TextNodeComparator leftComparator, TextNodeComparator rightComparator, List<RangeDifference> processedDifferences) {
        int currentIndexLeft = 0;
        int currentIndexRight = 0;
        int counter = 0;
        for (RangeDifference d : processedDifferences) {
            long iterationStart = System.currentTimeMillis();
            if (d.leftStart() > currentIndexLeft) {
                rightComparator.handlePossibleChangedPart(currentIndexLeft, d.leftStart(), currentIndexRight, d.rightStart(), leftComparator);
            }
            if (d.leftLength() > 0) {
                rightComparator.markAsDeleted(d.leftStart(), d.leftEnd(), leftComparator, d.rightStart(), d.rightEnd());
            }
            rightComparator.markAsNew(d.rightStart(), d.rightEnd());

            currentIndexLeft = d.leftEnd();
            currentIndexRight = d.rightEnd();
            //LOGGER.info("Iteration complete in " + (System.currentTimeMillis() - iterationStart) + " ms; number " + counter++);
        }
        if (currentIndexLeft < leftComparator.getRangeCount()) {
            rightComparator.handlePossibleChangedPart(currentIndexLeft, leftComparator.getRangeCount(),
                currentIndexRight, rightComparator.getRangeCount(), leftComparator);
        }
    }

}
