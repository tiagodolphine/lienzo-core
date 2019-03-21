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

package com.ait.lienzo.client.core.shape.wires.layout.direction;

import com.ait.lienzo.client.core.shape.IPrimitive;
import com.ait.lienzo.client.core.shape.wires.layout.AbstractContainerLayout;
import com.ait.lienzo.client.core.shape.wires.layout.IContainerLayout;
import com.ait.lienzo.client.core.shape.wires.layout.direction.DirectionLayout.Direction;
import com.ait.lienzo.client.core.shape.wires.layout.direction.DirectionLayout.HorizontalAlignment;
import com.ait.lienzo.client.core.shape.wires.layout.direction.DirectionLayout.Orientation;
import com.ait.lienzo.client.core.shape.wires.layout.direction.DirectionLayout.ReferencePosition;
import com.ait.lienzo.client.core.shape.wires.layout.direction.DirectionLayout.VerticalAlignment;
import com.ait.lienzo.client.core.shape.wires.layout.size.SizeConstraintsContainerLayout;
import com.ait.lienzo.client.core.types.BoundingBox;
import com.ait.tooling.common.api.java.util.function.Function;

public class DirectionContainerLayout extends AbstractContainerLayout<DirectionLayout>
{

    private SizeConstraintsContainerLayout m_sizeConstraintsContainerLayout;
    private double                         marginBottom;
    private double                         marginTop;
    private double                         marginRight;
    private double                         marginLeft;
    IPrimitive parentBoundingBox;

    public DirectionContainerLayout(final IPrimitive parentBoundingBox,
            final SizeConstraintsContainerLayout sizeConstraintsContainerLayout)
    {
        super(parentBoundingBox);
        this.m_sizeConstraintsContainerLayout = sizeConstraintsContainerLayout;
        this.parentBoundingBox = parentBoundingBox;
    }

    @Override
    public IContainerLayout add(final IPrimitive<?> child, final DirectionLayout layout)
    {
        BoundingBox childBoundingBox  = child.getBoundingBox();
        BoundingBox parentBoundingBox = this.parentBoundingBox.getBoundingBox();

        if (Orientation.VERTICAL.equals(layout.getOrientation()))
        {
            childBoundingBox = new BoundingBox(childBoundingBox.getMinY(), childBoundingBox.getMinX(),
                    childBoundingBox.getHeight(), childBoundingBox.getWidth());
            //parentBoundingBox = new BoundingBox(parentBoundingBox.getMinY(),parentBoundingBox.getMinX(),parentBoundingBox.getMaxY(), parentBoundingBox.getMaxX());
        }

        Function<Direction, Double> margins = new Function<Direction, Double>()
        {
            @Override
            public Double apply(final Direction direction)
            {
                return layout.getMargin(direction);
            }
        };


        //Horizontal Alignment
        child.setX(HorizontalLayoutFactory.get(layout.getReferencePosition())
                .apply(parentBoundingBox, childBoundingBox, layout.getHorizontalAlignment(), layout.getOrientation(),
                        margins));

        //Vertical Alignment
        child.setY(VerticalLayoutFactory.get(layout.getReferencePosition())
                .apply(parentBoundingBox, childBoundingBox, layout.getVerticalAlignment(), layout.getOrientation(),
                        margins));

        return super.add(child, layout);
    }

    @Override
    public DirectionLayout getDefaultLayout()
    {
        return new DirectionLayout.Builder().horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.MIDDLE).referencePosition(ReferencePosition.INSIDE).build();
    }
}