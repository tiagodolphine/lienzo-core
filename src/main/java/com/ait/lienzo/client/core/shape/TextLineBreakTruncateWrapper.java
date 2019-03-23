/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
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

package com.ait.lienzo.client.core.shape;

import java.util.ArrayList;
import java.util.List;

import com.ait.lienzo.client.core.Context2D;
import com.ait.lienzo.client.core.types.BoundingBox;

/**
 * ITextWrapper implementation that truncates text and appends "..." if there is no space left.
 */
@SuppressWarnings("Duplicates") public class TextLineBreakTruncateWrapper extends TextTruncateWrapper
{

    public static final String WHITESPACE_REGEX   = " |\\t";
    public static final String LINEBREAK          = "\n";
    public static final int    MAX_LENGHT_TO_WRAP = 10;

    public TextLineBreakTruncateWrapper(final Text text, final BoundingBox wrapBoundaries)
    {
        super(text, wrapBoundaries);
    }

    private String[] splitWords(final String text)
    {
        return text.replaceAll(LINEBREAK, " " + LINEBREAK + " ").split(WHITESPACE_REGEX);
    }

    @Override protected double[] calculateWrapBoundaries()
    {
        final List<String> lines = getWrappedTextLines(textSupplier.get());
        final double height = getHeightByLines(lines.size());

        double maxWidth = 0;
        for (String line : lines)
        {
            double lineWidth = getBoundingBoxForString(line).getWidth();
            maxWidth = (lineWidth > maxWidth) ? lineWidth : maxWidth;
        }

        return new double[] { maxWidth, height };
    }

    private double getRemainingHeight(int numOfLines)
    {
        return getWrapBoundaries().getHeight() - (Y_OFFSET * numOfLines) - Y_OFFSET * 2;
    }

    @Override public void drawString(final Context2D context, final Attributes attr, final IDrawString drawCommand)
    {
        final List<String> lines = getWrappedTextLines(attr.getText());
        drawLines(context, drawCommand, lines, getBoundingBox().getWidth());
    }

    private List<String> getWrappedTextLines(final String text)
    {
        final String[] words = splitWords(text);
        final List<String> lines = new ArrayList<>();
        final double boundariesWidth = getWrapBoundariesWidth();
        final StringBuilder currentLine = new StringBuilder();

        for (int i = 0; i < words.length; i++)
        {

            if (!hasVerticalSpace(lines.size(), getLineHeight(), getRemainingHeight(lines.size())) && !lines.isEmpty())
            {
                String endWord = lines.get(lines.size() - 1);
                String truncated =
                        (endWord.length() > 3 ? endWord.substring(0, endWord.length() - 4) : endWord) + "...";
                lines.remove(lines.size() - 1);
                lines.add(truncated);
                break;
            }

            //set current world + whitespace if applicable
            final String currentWord = words[i];
            if (currentWord.contains(LINEBREAK))
            {
                flushLine(lines, currentLine);
                continue;
            }

            if (!hasHorizontalSpaceToDraw(currentLine.toString(), currentWord, boundariesWidth))
            {
                //find the currentWord max char index that fits the boundariesWidth
                final int splitCharIndex = getSplitCharIndexToFitWidth(boundariesWidth, currentLine.toString(),
                        currentWord);

                //spliting the word to fit the boundaries width
                String remainingWord = currentWord.substring(splitCharIndex);
                String truncated = currentWord.substring(0, splitCharIndex);

                //handle splited word in case it is short, breaking to the next line
                if (remainingWord.length() < MAX_LENGHT_TO_WRAP && currentLine.length() > 0)
                {
                    truncated = "";
                    remainingWord = currentWord;
                }

                currentLine.append(truncated);
                flushLine(lines, currentLine);

                //reprocess the remainingWord (i--)
                words[i--] = remainingWord;
                continue;
            }

            currentLine.append(currentWord + ((i + 1 < words.length && !" ".equals(words[i])) ? " " : ""));

            //handle last line
            if (i == words.length - 1)
            {
                lines.add(currentLine.toString());
                break;
            }
        }
        return lines;
    }

    private int getSplitCharIndexToFitWidth(double boundariesWidth, String currentLine, String currentWord)
    {
        int remainingCharIndex = 0;
        while (hasHorizontalSpaceToDraw(currentLine, currentWord.substring(0, ++remainingCharIndex), boundariesWidth))
        {
        }
        return remainingCharIndex;
    }

    private void flushLine(final List<String> lines, final StringBuilder currentLine)
    {
        lines.add(currentLine.toString());
        currentLine.setLength(0);
    }
}