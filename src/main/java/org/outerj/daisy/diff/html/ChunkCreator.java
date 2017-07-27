package org.outerj.daisy.diff.html;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.outerj.daisy.diff.html.dom.TextNode;
import org.outerj.daisy.diff.html.dom.TextNodePreprocessor;

/**
 * Created by d.kalach on 6/26/17.
 */
public class ChunkCreator {

    private final SortedMap<String, List<TextNode>> segmentsLeft;
    private final SortedMap<String, List<TextNode>> segmentsRight;

    public ChunkCreator(TextNodeComparator leftComparator, TextNodeComparator rightComparator) {
        TextNodePreprocessor preprocessorLeft = new TextNodePreprocessor(leftComparator.getBodyNode(), leftComparator.getTextNodes());
        TextNodePreprocessor preprocessorRight = new TextNodePreprocessor(rightComparator.getBodyNode(), rightComparator.getTextNodes());
        segmentsLeft = preprocessorLeft.collectSegmentNodes();
        segmentsRight = preprocessorRight.collectSegmentNodes();
    }

    public Collection<Pair<List<TextNode>, List<TextNode>>> getChunks(int maxChunkSize) {
        return reduceToChunks(merge().values(), maxChunkSize);
    }

    private SortedMap<String, Pair<List<TextNode>, List<TextNode>>> merge() {
        List<TextNode> currentLeft = new ArrayList<>();
        List<TextNode> currentRight = new ArrayList<>();
        SortedMap<String, Pair<List<TextNode>, List<TextNode>>> result = new TreeMap<>();
        String prevSegment = segmentsLeft.firstKey();
        boolean firstSkipped = false;
        for (String segmentId : segmentsLeft.keySet()) {
            // skip first one
            if (!firstSkipped) {
                firstSkipped = true;
                continue;
            }

            if (segmentsRight.containsKey(segmentId)) {
                segmentsRight.subMap(prevSegment, segmentId).values().forEach(currentRight::addAll);
                segmentsLeft.subMap(prevSegment, segmentId).values().forEach(currentLeft::addAll);
                result.put(prevSegment, new ImmutablePair<>(currentLeft, currentRight));
                prevSegment = segmentId;
                currentLeft = new ArrayList<>();
                currentRight = new ArrayList<>();
            }
        }
        currentLeft = new ArrayList<>();
        currentRight = new ArrayList<>();
        segmentsRight.tailMap(prevSegment).values().forEach(currentRight::addAll);
        segmentsLeft.tailMap(prevSegment).values().forEach(currentLeft::addAll);
        result.put(prevSegment, new ImmutablePair<>(currentLeft, currentRight));
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
