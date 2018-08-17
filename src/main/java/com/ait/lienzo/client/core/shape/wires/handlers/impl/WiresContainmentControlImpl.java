/*
 * Copyright (c) 2018 Ahome' Innovation Technologies. All rights reserved.
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

package com.ait.lienzo.client.core.shape.wires.handlers.impl;

import com.ait.lienzo.client.core.shape.wires.IContainmentAcceptor;
import com.ait.lienzo.client.core.shape.wires.WiresContainer;
import com.ait.lienzo.client.core.shape.wires.WiresLayer;
import com.ait.lienzo.client.core.shape.wires.WiresManager;
import com.ait.lienzo.client.core.shape.wires.WiresShape;
import com.ait.lienzo.client.core.shape.wires.handlers.WiresContainmentControl;
import com.ait.lienzo.client.core.shape.wires.picker.ColorMapBackedPicker;
import com.ait.lienzo.client.core.types.Point2D;

public class WiresContainmentControlImpl extends AbstractWiresParentPickerControl implements WiresContainmentControl
{
    public WiresContainmentControlImpl(final WiresShape shape, final ColorMapBackedPicker.PickerOptions pickerOptions)
    {
        super(shape, pickerOptions);
    }

    public WiresContainmentControlImpl(final WiresParentPickerControlImpl parentPickerControl)
    {
        super(parentPickerControl);
    }

    @Override
    public WiresContainmentControl setEnabled(final boolean enabled)
    {
        if (enabled)
        {
            enable();
        }
        else
        {
            disable();
        }
        return this;
    }

    @Override
    public Point2D getAdjust()
    {
        return new Point2D(0, 0);
    }

    @Override
    public boolean isAllow()
    {
        return runAcceptor(true);
    }

    @Override
    public boolean accept()
    {
        return runAcceptor(false);
    }

    private boolean runAcceptor(final boolean allowNotAccept)
    {
        if (!isEnabled())
        {
            return false;
        }
        final WiresShape shape = getShape();

        final WiresContainer parent = getParent();

        final WiresLayer layer = getWiresLayer();

        final WiresManager wiresManager = layer.getWiresManager();

        final IContainmentAcceptor containmentAcceptor = wiresManager.getContainmentAcceptor();

        final WiresShape[] shapes = { shape };

        final boolean isParentLayer = (null == parent) || (parent instanceof WiresLayer);

        final WiresContainer candidateParent = isParentLayer ? layer : parent;

        final boolean isAllowed = containmentAcceptor.containmentAllowed(candidateParent, shapes);

        if (!allowNotAccept && isAllowed)
        {
            return containmentAcceptor.acceptContainment(candidateParent, shapes);
        }
        return isAllowed;
    }

    @Override
    public Point2D getCandidateLocation()
    {
        return calculateCandidateLocation(getParentPickerControl());
    }

    @Override
    public void execute()
    {
        if (isEnabled())
        {
            addIntoParent(getShape(), getParent(), getCandidateLocation());
        }
    }

    @Override
    public void clear()
    {
    }

    @Override
    public void reset()
    {
        if (isEnabled())
        {
            if (!getParentPickerControl().getShapeLocationControl().isStartDocked() && (getParentPickerControl().getInitialParent() != getShape().getParent()))
            {
                addIntoParent(getShape(), getParentPickerControl().getInitialParent(), getParentPickerControl().getShapeLocationControl().getShapeInitialLocation());

                getShape().setDockedTo(null);
            }
        }
    }

    @Override
    public void destroy() {
        clear();
    }

    private void addIntoParent(final WiresShape shape,
                               final WiresContainer parent,
                               final Point2D location) {
        final WiresLayer m_layer = getWiresLayer();
        if (parent == null || parent == m_layer) {
            m_layer.getLayoutHandler().add(shape,
                                           m_layer,
                                           location);
        } else {
            parent.getLayoutHandler().add(shape,
                                          parent,
                                          location);
        }
        shape.setDockedTo(null);
    }

    public static Point2D calculateCandidateLocation(final WiresParentPickerControlImpl parentPickerControl)
    {
        final WiresLayer layer = parentPickerControl.getShape().getWiresManager().getLayer();

        final WiresContainer parent = parentPickerControl.getParent();

        final Point2D current = parentPickerControl.getShapeLocation();

        if ((parent == null) || (parent == layer))
        {
            return current;
        }
        else
        {
            final Point2D trgAbsOffset = parent.getComputedLocation();

            return current.minus(trgAbsOffset);
        }
    }

    private WiresLayer getWiresLayer()
    {
        return getParentPickerControl().getWiresLayer();
    }
}
