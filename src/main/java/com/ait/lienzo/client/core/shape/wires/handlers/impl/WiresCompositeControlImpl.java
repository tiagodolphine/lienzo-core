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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.ait.lienzo.client.core.shape.wires.WiresConnector;
import com.ait.lienzo.client.core.shape.wires.WiresContainer;
import com.ait.lienzo.client.core.shape.wires.WiresManager;
import com.ait.lienzo.client.core.shape.wires.WiresShape;
import com.ait.lienzo.client.core.shape.wires.handlers.MouseEvent;
import com.ait.lienzo.client.core.shape.wires.handlers.WiresCompositeControl;
import com.ait.lienzo.client.core.shape.wires.handlers.WiresConnectorHandler;
import com.ait.lienzo.client.core.shape.wires.handlers.WiresParentPickerControl;
import com.ait.lienzo.client.core.shape.wires.handlers.WiresShapeControl;
import com.ait.lienzo.client.core.types.Point2D;

/**
 * The default WiresCompositeControl implementation.
 * It orchestrates different controls for handling interactions with multiple wires shapes and connectors.
 * Notice that docking capabilities are not being considered when handling multiple wires objects.
 */
public class WiresCompositeControlImpl extends AbstractWiresBoundsConstraintControl implements WiresCompositeControl
{
    private Context                    m_selectionContext;

    private Point2D                    m_delta;

    private Collection<WiresShape>     m_selectedShapes;

    private Collection<WiresConnector> m_selectedConnectors;

    private WiresConnector[]           m_connectorsWithSpecialConnections;

    public WiresCompositeControlImpl(final Context selectionContext)
    {
        this.m_selectionContext = selectionContext;
    }

    @Override
    public void setContext(final Context provider)
    {
        this.m_selectionContext = provider;
    }

    @Override
    public void onMoveStart(final double x, final double y)
    {
        m_delta = new Point2D(0, 0);

        m_selectedShapes = new ArrayList<>(m_selectionContext.getShapes());

        m_selectedConnectors = new ArrayList<>(m_selectionContext.getConnectors());

        for (WiresShape shape : m_selectedShapes)
        {
            setShapesToSkipFromIndex(shape);
        }

        for (WiresShape shape : m_selectedShapes) {

            ShapeControlUtils.collectionSpecialConnectors(shape, connectors);

            if (shape.getMagnets() != null)
            {
                shape.getMagnets().onNodeDragStart(null); // Must do magnets first, to avoid attribute change updates being processed.
                // Don't need to do this for nested objects, as those just move with their containers, without attribute changes
            }

            disableDocking(shape.getControl());

            final Point2D location = shape.getComputedLocation();

            final double sx = location.getX();

            final double sy = location.getY();

            shape.getControl().onMoveStart(sx, sy);
        }
        m_connectorsWithSpecialConnections = connectors.values().toArray(new WiresConnector[connectors.size()]);

        for (final WiresConnector connector : m_selectedConnectors)
        {
            final WiresConnectorHandler handler = connector.getWiresConnectorHandler();
            handler.getControl().onMoveStart(x, y); // records the start position of all the points
            WiresConnector.updateHeadTailForRefreshedConnector(connector);
        }
    }

    private void setShapesToSkipFromIndex(final WiresShape shape) {
        final WiresParentPickerControl.Index index = shape.getControl().getParentPickerControl().getIndex();
        for (WiresShape candidate : selectedShapes) {
            index.addShapeToSkip(candidate);
        }
    }

    @Override
    public boolean isOutOfBounds(final double dx, final double dy)
    {
        for (final WiresShape shape : m_selectedShapes)
        {
            if (shape.getControl().isOutOfBounds(dx, dy))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onMove(final double dx, final double dy)
    {
        if (isOutOfBounds(dx, dy))
        {
            return true;
        }
        m_delta = new Point2D(dx, dy);

        // Delegate location deltas to shape controls and obtain current locations for each one.
        final Collection<WiresShape> shapes = m_selectedShapes;

        if (!shapes.isEmpty())
        {
            final WiresManager wiresManager = shapes.iterator().next().getWiresManager();

            final Point2D[] locs = new Point2D[shapes.size()];

            int i = 0;

            for (final WiresShape shape : shapes)
            {
                shape.getControl().onMove(dx, dy);

                locs[i++] = getCandidateShapeLocationRelativeToInitialParent(shape);
            }
            // Check if new locations are allowed.
            final WiresShape[] shapesArray = toArray(shapes);

            final boolean locationAllowed = wiresManager.getLocationAcceptor().allow(shapesArray, locs);

            // Do the updates.
            if (locationAllowed)
            {
                i = 0;

                for (final WiresShape shape : shapes)
                {
                    shape.getControl().getParentPickerControl().setShapeLocation(locs[i++]);

                    postUpdateShape(shape);
                }
            }
        }

        ShapeControlUtils.updateConnectors(m_selectedConnectors, dx, dy);

        ShapeControlUtils.updateSpecialConnections(m_connectorsWithSpecialConnections,
                                                   false);

        return false;
    }

    static Point2D getCandidateShapeLocationRelativeToInitialParent(final WiresShape shape) {
        final Point2D candidate = shape.getControl().getContainmentControl().getCandidateLocation();
        final WiresParentPickerControlImpl parentPickerControl =
                (WiresParentPickerControlImpl) shape.getControl().getParentPickerControl();
        Point2D co = null != parentPickerControl.getParent() ?
                parentPickerControl.getParent().getComputedLocation().add(candidate) :
                candidate;
        if (null != parentPickerControl.getInitialParent()) {
            co = co.minus(parentPickerControl.getInitialParent().getComputedLocation());
        }
        return co;
    }

    @Override
    public boolean isAllowed()
    {
        // Check parents && allow acceptors.
        final WiresContainer parent = getSharedParent();

        return (null != parent) && parent.getWiresManager().getContainmentAcceptor().containmentAllowed(parent, toArray(m_selectedShapes));
    }

    @Override
    public WiresContainer getSharedParent()
    {
        final Collection<WiresShape> shapes = m_selectedShapes;

        if (shapes.isEmpty())
        {
            return null;
        }
        final WiresContainer shared = shapes.iterator().next().getControl().getParentPickerControl().getParent();

        for (final WiresShape shape : shapes)
        {
            final WiresContainer parent = shape.getControl().getParentPickerControl().getParent();

            if (parent != shared)
            {
                return null;
            }
        }
        return shared;
    }

    @Override
    public boolean onMoveComplete()
    {
        boolean completeResult = true;

        final Collection<WiresShape> shapes = m_selectedShapes;

        if (!shapes.isEmpty())
        {
            shapes.iterator().next().getWiresManager();

            for (final WiresShape shape : shapes)
            {
                if (!shape.getControl().onMoveComplete())
                {
                    completeResult = false;
                }
            }
        }
        final Collection<WiresConnector> connectors = m_selectedConnectors;

        if (!connectors.isEmpty())
        {
            // Update connectors and connections.
            for (final WiresConnector connector : connectors)
            {
                if (!connector.getWiresConnectorHandler().getControl().onMoveComplete())
                {
                    completeResult = false;
                }
            }
        }
        m_delta = new Point2D(0, 0);

        return completeResult;
    }

    @Override
    public boolean accept()
    {
        final Collection<WiresShape> shapes = m_selectedShapes;

        if (!shapes.isEmpty())
        {
            final WiresManager wiresManager = shapes.iterator().next().getWiresManager();

            final Point2D[] shapeCandidateLocations = new Point2D[shapes.size()];

            int i = 0;

            for (final WiresShape shape : shapes)
            {
                shapeCandidateLocations[i] = shape.getControl().getContainmentControl().getCandidateLocation();

                i++;
            }
            final WiresShape[] shapesArray = toArray(shapes);

            final WiresContainer parent = getSharedParent();

            boolean completeResult = (null != parent) && wiresManager.getContainmentAcceptor().acceptContainment(parent, shapesArray);

            if (completeResult)
            {
                completeResult = wiresManager.getLocationAcceptor().accept(shapesArray, shapeCandidateLocations);
            }
            return completeResult;
        }
        return false;
    }

    @Override
    public void execute()
    {
        for (final WiresShape shape : m_selectedShapes)
        {
            shape.getControl().getContainmentControl().execute();

            postUpdateShape(shape);
        }

        for (final WiresConnector connector : m_selectedConnectors)
        {
            WiresConnector.updateHeadTailForRefreshedConnector(connector);
        }
        ShapeControlUtils.updateSpecialConnections(m_connectorsWithSpecialConnections, true);

        clear();
    }

    @Override
    public void clear()
    {
        for (final WiresShape shape : m_selectedShapes)
        {
            shape.getControl().clear();

            enableDocking(shape.getControl());
        }
        clearState();
    }

    @Override
    public void reset()
    {
        for (final WiresShape shape : m_selectedShapes)
        {
            shape.getControl().reset();

            enableDocking(shape.getControl());
        }

        for (final WiresConnector connector : m_selectedConnectors)
        {
            final WiresConnectorHandler handler = connector.getWiresConnectorHandler();
            handler.getControl().reset();

            WiresConnector.updateHeadTailForRefreshedConnector(connector);
        }
        clearState();
    }

    @Override
    public Point2D getAdjust()
    {
        return m_delta;
    }

    public Context getContext()
    {
        return m_selectionContext;
    }

    @Override
    public void onMouseClick(final MouseEvent event)
    {
        for (final WiresShape shape : m_selectionContext.getShapes())
        {
            shape.getControl().onMouseClick(event);
        }
    }

    @Override
    public void onMouseDown(final MouseEvent event)
    {
        for (final WiresShape shape : m_selectionContext.getShapes())
        {
            shape.getControl().onMouseDown(event);
        }
    }

    @Override
    public void onMouseUp(final MouseEvent event)
    {
        for (final WiresShape shape : m_selectionContext.getShapes())
        {
            shape.getControl().onMouseUp(event);
        }
    }

    private void clearState()
    {
        m_delta = new Point2D(0, 0);

        m_selectedShapes = null;

        m_selectedConnectors = null;

        m_connectorsWithSpecialConnections = null;
    }

    private static void disableDocking(final WiresShapeControl control)
    {
        control.getDockingControl().setEnabled(false);
    }

    private static void enableDocking(final WiresShapeControl control)
    {
        control.getDockingControl().setEnabled(true);
    }

    private void postUpdateShape(final WiresShape shape)
    {
        shape.getControl().getMagnetsControl().shapeMoved();

        ShapeControlUtils.updateNestedShapes(shape);
    }

    private static WiresShape[] toArray(final Collection<WiresShape> shapes)
    {
        return shapes.toArray(new WiresShape[shapes.size()]);
    }
}
