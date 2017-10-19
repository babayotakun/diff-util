package org.outerj.daisy.diff.html;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.outerj.daisy.diff.html.dom.TextNode;
import org.outerj.daisy.diff.html.dom.TextNodePreprocessor;

/**
 * Created by d.kalach on 6/26/17.
 */
public class ChunkCreator {
    private final List<Pair<String, List<TextNode>>> segmentsLeft;
    private final List<Pair<String, List<TextNode>>> segmentsRight;

    public ChunkCreator(TextNodeComparator leftComparator, TextNodeComparator rightComparator) {
        TextNodePreprocessor preprocessorLeft = new TextNodePreprocessor(leftComparator.getBodyNode(), leftComparator.getTextNodes());
        TextNodePreprocessor preprocessorRight = new TextNodePreprocessor(rightComparator.getBodyNode(), rightComparator.getTextNodes());
        segmentsLeft = preprocessorLeft.collectSegmentNodes();
        segmentsRight = preprocessorRight.collectSegmentNodes();
    }

    public Collection<Pair<List<TextNode>, List<TextNode>>> getChunks(int maxChunkSize) {
        return reduceToChunks(merge(), maxChunkSize);
    }

    private Collection<Pair<List<TextNode>, List<TextNode>>> merge() {
        List<TextNode> currentLeft = new ArrayList<>();
        List<TextNode> currentRight = new ArrayList<>();
        List<Pair<List<TextNode>, List<TextNode>>> result = new ArrayList<>();

        List<String> leftSegmentIds = segmentsLeft.stream().map(Pair::getLeft).collect(Collectors.toList());
        List<String> rightSegmentIds = segmentsRight.stream().map(Pair::getLeft).collect(Collectors.toList());

        int lastLeftIndex = 0;
        int lastRightIndex = 0;
        for (int leftIndex = 0; leftIndex < leftSegmentIds.size(); leftIndex++) {
            int rightIndex = rightSegmentIds.indexOf(leftSegmentIds.get(leftIndex));
            if (rightIndex > -1 && lastRightIndex < rightIndex) {
                segmentsRight.subList(lastRightIndex, rightIndex).stream().map(Pair::getRight).forEach(currentRight::addAll);
                segmentsLeft.subList(lastLeftIndex, leftIndex).stream().map(Pair::getRight).forEach(currentLeft::addAll);
                result.add(new ImmutablePair<>(currentLeft, currentRight));
                currentLeft = new ArrayList<>();
                currentRight = new ArrayList<>();
                lastLeftIndex = leftIndex;
                lastRightIndex = rightIndex;
            }
        }
        currentLeft = new ArrayList<>();
        currentRight = new ArrayList<>();
        segmentsRight.subList(lastRightIndex, rightSegmentIds.size()).stream().map(Pair::getRight).forEach(currentRight::addAll);
        segmentsLeft.subList(lastLeftIndex, leftSegmentIds.size()).stream().map(Pair::getRight).forEach(currentLeft::addAll);
        result.add(new ImmutablePair<>(currentLeft, currentRight));
        return result;
    }

    private Collection<Pair<List<TextNode>, List<TextNode>>> reduceToChunks(Collection<Pair<List<TextNode>, List<TextNode>>> toChop, int chunkSize) {
        Collection<Pair<List<TextNode>, List<TextNode>>> result = new ArrayList<>();
        List<TextNode> leftChunk = new ArrayList<>();
        List<TextNode> rightChunk = new ArrayList<>();
        for (Pair<List<TextNode>, List<TextNode>> pair : toChop) {
            if (leftChunk.size() + pair.getLeft().size() < chunkSize
                && rightChunk.size() + pair.getRight().size() < chunkSize) {
                leftChunk.addAll(pair.getLeft());
                rightChunk.addAll(pair.getRight());
            } else {
                result.add(new ImmutablePair<>(leftChunk, rightChunk));
                leftChunk = new ArrayList<>();
                rightChunk = new ArrayList<>();
            }
        }
        if (!leftChunk.isEmpty() || !rightChunk.isEmpty()) {
            result.add(new ImmutablePair<>(leftChunk, rightChunk));
        }
        return result;
    }

}
