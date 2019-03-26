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

package com.ait.lienzo.client.core.shape.wires.layout.size;

public class SizeConstraints
{
    public enum Type
    {
        PERCENTAGE, RAW
    }

    private double width;
    private double height;
    private double marginX;
    private double marginY;
    private Type   m_type;

    public SizeConstraints(final double width, final double height, final Type type, final double marginX,
            final double marginY)
    {
        this(width, height, type);
        this.marginX = marginX;
        this.marginY = marginY;
    }

    public SizeConstraints(final double width, final double height)
    {
        this(width, height, Type.RAW);
    }

    public SizeConstraints(final double width, final double height, final Type type)
    {
        this.width = width;
        this.height = height;
        this.m_type = type;
    }

    public SizeConstraints(){
        this(100, 100, Type.PERCENTAGE);
    }

    public double getWidth()
    {
        return width;
    }

    public double getHeight()
    {
        return height;
    }

    public Type getType()
    {
        return m_type;
    }

    public double getMarginX()
    {
        return marginX;
    }

    public double getMarginY()
    {
        return marginY;
    }
}
