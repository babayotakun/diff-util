package org.eclipse.compare.rangedifferencer;

import java.util.LinkedList;
import java.util.List;
import org.outerj.daisy.diff.html.TextNodeComparator;
import org.outerj.daisy.diff.html.dom.DelimiterConfigurer;

/**
 * Class for preprocessing differences.
 */
public class DifferencePreprocessor {
    private final TextNodeComparator left;
    private final TextNodeComparator right;
    private final DelimiterConfigurer delimiterConfigurer;

    public DifferencePreprocessor(TextNodeComparator left,
                                  TextNodeComparator right) {
        this.left = left;
        this.right = right;
        delimiterConfigurer = new DelimiterConfigurer();
    }

    public List<RangeDifference> preProcess(RangeDifference[] differences) {
        List<RangeDifference> newRanges = applyUnitePreProcessing(differences);
        applyShiftPreProcessing(newRanges);
        return newRanges;
    }


    /**
     * Apply some difference expansions and shifts for better looking diff.
     */
    private void applyShiftPreProcessing(List<RangeDifference> differences) {
        for (RangeDifference current : differences) {
            applyDelimiterExpansion(current);

            /*
              Apply right start shift in delimiter cases like:
              old: two five
              new: two three four two five

              Without this shift there will be "three four two " marked as added.
              With this shift there will be "two three four " marked as added.
             */
            while (current.rightLength() > 0
                && current.rightStart() > 0
                && right.getTextNode(current.rightStart() - 1).isSameText(right.getTextNode(current.rightEnd() - 1))) {
                current.fRightStart--;
            }
        }
    }

    /**
     Apply length expansion in delimiter cases like:
     old: one, two, three, four.
     new: one.

     Without this expansion there will be ", two, three, four" marked as deleted.

     With this expansion there will be "," changed to "." and "two, three, four." marked as deleted.
     Note that this expansion is working only if "one," and "four." are in the different paragraphs.

     Same expansion is applied with added parts.
     */
    private void applyDelimiterExpansion(RangeDifference current) {
        while (canExpand(current) && delimitersOnMargins(current, true) && marginsInDifferentParagraphs(current, true)) {
            current.fRightLength++;
            current.fLeftLength++;
        }
        while (canExpand(current) && delimitersOnMargins(current, false) && marginsInDifferentParagraphs(current, false)) {
            current.fRightLength++;
            current.fLeftLength++;
        }
    }

    private boolean delimitersOnMargins(RangeDifference current, boolean forLeft) {
        return delimiterConfigurer.isDelimiter((forLeft ? left : right).getTextNode(forLeft ? current.leftEnd() : current.rightEnd()).getText())
            && delimiterConfigurer.isDelimiter((forLeft ? left : right).getTextNode(forLeft ? current.leftStart() : current.rightStart()).getText());
    }

    private boolean canExpand(RangeDifference current) {
        return current.leftEnd() < left.getTextNodes().size() - 1
            && current.rightEnd() < right.getTextNodes().size() - 1;
    }

    private boolean marginsInDifferentParagraphs(RangeDifference current, boolean forLeft) {
        return (forLeft ? left : right).getTextNode(forLeft ? current.leftEnd() : current.rightEnd()).getParent()
            != (forLeft ? left : right).getTextNode(forLeft ? current.leftStart() : current.rightStart()).getParent();
    }

    /**
     * Unite some differences into the single one,
     * based on some deep magic ({@link DifferencePreprocessor#score(int...)}) from daisy diff lib developers.
     */
    private List<RangeDifference> applyUnitePreProcessing(RangeDifference[] differences) {
        List<RangeDifference> newRanges = new LinkedList<RangeDifference>();

        for (int i = 0; i < differences.length; i++) {

            int ancestorStart = differences[i].ancestorStart();
            int ancestorEnd = differences[i].ancestorEnd();
            int leftStart = differences[i].leftStart();
            int leftEnd = differences[i].leftEnd();
            int rightStart = differences[i].rightStart();
            int rightEnd = differences[i].rightEnd();
            int kind = differences[i].kind();

            int ancestorLength = ancestorEnd - ancestorStart;
            int leftLength = leftEnd - leftStart;
            int rightLength = rightEnd - rightStart;

            while (i + 1 < differences.length
                && differences[i + 1].kind() == kind
                && score(leftLength, differences[i + 1].leftLength(), rightLength, differences[i + 1].rightLength())
                > (differences[i + 1].leftStart() - leftEnd)) {
                leftEnd = differences[i + 1].leftEnd();
                rightEnd = differences[i + 1].rightEnd();
                ancestorEnd = differences[i + 1].ancestorEnd();
                leftLength = leftEnd - leftStart;
                rightLength = rightEnd - rightStart;
                ancestorLength = ancestorEnd - ancestorStart;
                i++;
            }

            newRanges.add(new RangeDifference(kind,
                rightStart, rightLength,
                leftStart, leftLength,
                ancestorStart, ancestorLength));
        }

        return newRanges;
    }

    private double score(int... numbers) {
        if ((numbers[0] == 0 && numbers[1] == 0) || (numbers[2] == 0 && numbers[3] == 0))
            return 0;

        double d = 0;
        for (double number : numbers) {
            while (number > 3) {
                d += 3;
                number -= 3;
                number *= 0.5;
            }
            d += number;

        }
        return d / (1.5 * numbers.length);
    }
}
