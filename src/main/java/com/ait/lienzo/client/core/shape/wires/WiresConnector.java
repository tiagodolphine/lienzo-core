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

package com.ait.lienzo.client.core.shape.wires;

import com.ait.lienzo.client.core.Context2D;
import com.ait.lienzo.client.core.shape.*;
import com.ait.lienzo.client.core.shape.wires.handlers.WiresConnectorControl;
import com.ait.lienzo.client.core.types.*;
import com.ait.lienzo.client.core.shape.AbstractDirectionalMultiPointShape;
import static com.ait.lienzo.client.core.shape.wires.IControlHandle.ControlHandleStandardType.POINT;

import com.ait.lienzo.client.core.shape.Group;
import com.ait.lienzo.client.core.shape.IDirectionalMultiPointShape;
import com.ait.lienzo.client.core.shape.Layer;
import com.ait.lienzo.client.core.shape.MultiPath;
import com.ait.lienzo.client.core.shape.MultiPathDecorator;
import com.ait.lienzo.client.core.shape.OrthogonalPolyLine;
import com.ait.lienzo.client.core.shape.wires.handlers.WiresConnectorHandler;
import com.ait.lienzo.client.core.types.BoundingBox;
import com.ait.lienzo.client.core.types.Point2D;
import com.ait.lienzo.client.core.types.Point2DArray;
import com.ait.lienzo.client.core.util.Geometry;
import com.ait.lienzo.client.core.util.ScratchPad;
import com.ait.lienzo.shared.core.types.ArrowEnd;
import com.ait.lienzo.shared.core.types.Direction;
import com.ait.lienzo.shared.core.types.EventPropagationMode;
import com.ait.tooling.nativetools.client.collection.NFastDoubleArrayJSO;
import com.ait.tooling.nativetools.client.collection.NFastStringMap;

public class WiresConnector
{
    public static final int MINIMUM_STROKE_WITH = 4;

    private WiresConnection                      m_headConnection;

    private WiresConnection                      m_tailConnection;

    private IControlHandleList                   m_pointHandles;

    private IDirectionalMultiPointShape<?>        m_line;

    private final MultiPathDecorator             m_headDecorator;

    private final MultiPathDecorator             m_tailDecorator;

    private final Group                          m_group;

    private WiresConnectorControl                 m_connectorControl;

    public WiresConnector(final IDirectionalMultiPointShape<?> line, final MultiPathDecorator headDecorator, final MultiPathDecorator tailDecorator)
    {
        m_line = line;

        if (m_line instanceof OrthogonalPolyLine)
        {
            final OrthogonalPolyLine polyline = (OrthogonalPolyLine) m_line;

            final BoundingBox headBB = headDecorator.getPath().getBoundingBox();

            final double breakDistance = Math.min(headBB.getWidth(), headBB.getHeight());

            polyline.setBreakDistance(breakDistance);
        }
        m_headDecorator = headDecorator;

        m_tailDecorator = tailDecorator;

        setHeadConnection(new WiresConnection(this, m_headDecorator.getPath(), ArrowEnd.HEAD));

        setTailConnection(new WiresConnection(this, m_tailDecorator.getPath(), ArrowEnd.TAIL));

        m_line.setEventPropagationMode(EventPropagationMode.FIRST_ANCESTOR);

        m_group = new Group();
        m_group.add(m_line);
        m_group.add(m_headDecorator.getPath());
        m_group.add(m_tailDecorator.getPath());

        // these are not draggable, only the group may or may not be draggable, depending if the line is connected or not
        m_line.setDraggable(false);
        m_headDecorator.getPath().setDraggable(false);
        m_tailDecorator.getPath().setDraggable(false);

        // The Line is only draggable if both Connections are unconnected
        setDraggable();
    }

    public WiresConnector(final WiresMagnet headMagnet, final WiresMagnet tailMagnet, final IDirectionalMultiPointShape<?> line, final MultiPathDecorator headDecorator, final MultiPathDecorator tailDecorator)
    {
        this(line, headDecorator, tailDecorator);

        setHeadMagnet(headMagnet);

        setTailMagnet(tailMagnet);
    }

    public WiresConnector setHeadMagnet(final WiresMagnet headMagnet)
    {
        m_headConnection.setMagnet(headMagnet);

        return this;
    }

    public WiresConnector setTailMagnet(final WiresMagnet tailMagnet)
    {
        m_tailConnection.setMagnet(tailMagnet);

        return this;
    }

    public void select()
    {
        m_connectorControl.showControlPoints();
        m_group.getLayer().batch();
    }

    public void unselect()
    {
        m_connectorControl.hideControlPoints();
        m_group.getLayer().batch();
    }

    public void setControl( final WiresConnectorControl control)
    {
        this.m_connectorControl = control;
    }

    public WiresConnectorControl getControl()
    {
        return m_connectorControl;
    }

    public void destroy()
    {
        destroyPointHandles();

        removeFromLayer();
    }

    public void addToLayer(final Layer layer)
    {
        layer.add(m_group);
    }

    public void removeFromLayer()
    {
        m_group.removeFromParent();
    }

    public WiresConnection getHeadConnection()
    {
        return m_headConnection;
    }

    public void setHeadConnection(final WiresConnection headConnection)
    {
        m_headConnection = headConnection;
    }

    public void setDraggable()
    {
        // The line can only be dragged if both Magnets are null
        m_group.setDraggable(isDraggable());
    }

    private boolean isDraggable()
    {
        return (getHeadConnection().getMagnet() == null) && (getTailConnection().getMagnet() == null);
    }

    public WiresConnection getTailConnection()
    {
        return m_tailConnection;
    }

    public boolean isSpecialConnection()
    {
        return ((m_headConnection != null) && m_headConnection.isSpecialConnection()) || ((m_tailConnection != null) && m_tailConnection.isSpecialConnection());
    }

    public void setTailConnection(final WiresConnection tailConnection)
    {
        m_tailConnection = tailConnection;
    }

    public void setPointHandles(final IControlHandleList pointHandles)
    {
        m_pointHandles = pointHandles;
    }

    public IDirectionalMultiPointShape<?> getLine()
    {
        return m_line;
    }

    public MultiPathDecorator getHeadDecorator()
    {
        return m_headDecorator;
    }

    public MultiPathDecorator getTailDecorator()
    {
        return m_tailDecorator;
    }

    public MultiPath getHead()
    {
        return m_headDecorator.getPath();
    }

    public MultiPath getTail()
    {
        return m_tailDecorator.getPath();
    }

    public Group getGroup()
    {
        return m_group;
    }

    public String uuid()
    {
        return getGroup().uuid();
    }

    public void destroyPointHandles()
    {
        if (m_pointHandles != null)
        {
            m_pointHandles.destroy();
        }
        m_pointHandles = null;
    }

    public IControlHandleList getPointHandles()
    {
        if (m_pointHandles == null)
        {
            m_pointHandles = m_line.getControlHandles(POINT).get(POINT);
        }
        return m_pointHandles;
    }

    public void updateForSpecialConnections(final boolean isAcceptOp)
    {
        updateForCenterConnection();

        updateForAutoConnections(isAcceptOp);
    }

    public void updateForCenterConnection()
    {
        final WiresConnection headC = getHeadConnection();

        final WiresConnection tailC = getTailConnection();

        updateForCenterConnection(this, headC, 1);

        updateForCenterConnection(this, tailC, getLine().getPoint2DArray().size() - 2);
    }

    public static void updateForCenterConnection(final WiresConnector connector, final WiresConnection connection, final int pointIndex)
    {
        if ((connection.getMagnet() != null) && (connection.getMagnet().getIndex() == 0))
        {
            final MultiPath path = connection.getMagnet().getMagnets().getWiresShape().getPath();
            final BoundingBox box = path.getBoundingBox();
            final Point2D c = Geometry.findCenter(box);
            Point2D intersectPoint = Geometry.getPathIntersect(connection, path, c, pointIndex);
            if (null == intersectPoint)
            {
                intersectPoint = new Point2D();
            }
            final Direction d = MagnetManager.getDirection(intersectPoint, box);

            final Point2D loc = path.getComputedLocation().copy();

            connection.setXOffset(intersectPoint.getX() - c.getX());
            connection.setYOffset(intersectPoint.getY() - c.getY());

            if (connector.getHeadConnection() == connection)
            {
                connector.getLine().setHeadDirection(d);
            }
            else
            {
                connector.getLine().setTailDirection(d);
            }
            connection.move(loc.getX() + c.getX(), loc.getY() + c.getY());
        }
    }

    public boolean updateForAutoConnections(final boolean isAcceptOp)
    {
        // used when a connection is not being dragged
        final WiresConnection headC = getHeadConnection();
        final WiresConnection tailC = getTailConnection();

        final WiresShape headS = (headC.getMagnet() != null) ? headC.getMagnet().getMagnets().getWiresShape() : null;
        final WiresShape tailS = (tailC.getMagnet() != null) ? tailC.getMagnet().getMagnets().getWiresShape() : null;

        return updateForAutoConnections(headS, tailS, isAcceptOp);
    }

    public boolean updateForAutoConnections(final WiresShape headS, final WiresShape tailS, final boolean isAcceptOp)
    {
        boolean accept = true;

        // Allowed conections has already been checked, but for consistency and notifications will be rechecked via acceptor
        // Will not set a magnet if the connection is already set to that returned magnet
        final WiresMagnet[] magnets = getMagnetsOnAutoConnection(headS, tailS);
        if (magnets != null)
        {
            if ((magnets[0] != null) && (getHeadConnection().getMagnet() != magnets[0]))
            {
                accept = accept && executeHeadConnectionOperation(headS, magnets[0], isAcceptOp);
                if (accept)
                {
                    getHeadConnection().setMagnet(magnets[0]);
                }
            }
            if (accept && (magnets[1] != null) && (getTailConnection().getMagnet() != magnets[1]))
            {
                accept = accept && executeTailConnectionOperation(tailS, magnets[1], isAcceptOp);
                if (accept)
                {
                    getTailConnection().setMagnet(magnets[1]);
                }
            }
        }
        return accept;
    }

    private boolean executeHeadConnectionOperation(final WiresShape shape,
                                                   final WiresMagnet magnet,
                                                   final boolean isAcceptOp) {
        final IConnectionAcceptor connectionAcceptor = shape.getWiresManager().getConnectionAcceptor();
        return isAcceptOp ?
                connectionAcceptor.acceptHead(getHeadConnection(), magnet) :
               connectionAcceptor.headConnectionAllowed(getHeadConnection(), shape);
    }

    private boolean executeTailConnectionOperation(final WiresShape shape,
                                                   final WiresMagnet magnet,
                                                   final boolean isAcceptOp) {
        final IConnectionAcceptor connectionAcceptor = shape.getWiresManager().getConnectionAcceptor();
        return isAcceptOp ?
               connectionAcceptor.acceptTail(getTailConnection(), magnet) :
               connectionAcceptor.tailConnectionAllowed(getTailConnection(), shape);
    }

    /**
     * This is making some assumptions that will have to be fixed for anythign other than 8 Magnets at compass ordinal points.
     * If there is no shape overlap, and one box is in the corner of the other box, then use nearest corner connections, else use nearest mid connection.
     * Else there is overlap. This is now much more difficult, so just pick which every has the the shortest distanceto connections not contained by the other shape.
     */
    public WiresMagnet[] getMagnetsOnAutoConnection(final WiresShape headS, final WiresShape tailS)
    {
        final WiresConnection headC = getHeadConnection();

        final WiresConnection tailC = getTailConnection();

        if (!(headC.isAutoConnection() || tailC.isAutoConnection()))
        {
            // at least one side must be connected with auto connection on
            return null;
        }
        WiresMagnet[] magnets;
        final BoundingBox headBox = (headS != null) ? headS.getGroup().getComputedBoundingPoints().getBoundingBox() : null;
        final BoundingBox tailBox = (tailS != null) ? tailS.getGroup().getComputedBoundingPoints().getBoundingBox() : null;

        if (getLine().getPoint2DArray().size() > 2)
        {
            magnets = getMagnetsWithMidPoint(headC, tailC, headS, tailS, headBox, tailBox);
        }
        else
        {
            if ((headBox != null) && (tailBox != null) && !headBox.intersects(tailBox))
            {
                magnets = getMagnetsNonOverlappedShapes(headS, tailS, headBox, tailBox);
            }
            else
            {
                magnets = getMagnetsOverlappedShapesOrNoShape(headC, tailC, headS, tailS, headBox, tailBox);
            }
        }
        return magnets;
    }

    private WiresMagnet[] getMagnetsWithMidPoint(final WiresConnection headC, final WiresConnection tailC, final WiresShape headS, final WiresShape tailS, final BoundingBox headBox, final BoundingBox tailBox)
    {
        // make BB's of 1 Point2D, then we can reuse existing code.
        final Point2D pAfterHead = getLine().getPoint2DArray().get(1);
        final Point2D pBeforeTail = getLine().getPoint2DArray().get(getLine().getPoint2DArray().size() - 2);

        final BoundingBox firstBB = new BoundingBox(pAfterHead, pAfterHead);
        final BoundingBox lastBB = new BoundingBox(pBeforeTail, pBeforeTail);

        WiresMagnet headM;
        WiresMagnet tailM;
        if ((headBox != null) && !headBox.intersects(firstBB))
        {
            final WiresMagnet[] magnets = getMagnetsNonOverlappedShapes(headS, null, headBox, firstBB);

            headM = magnets[0];
        }
        else
        {
            final WiresMagnet[] headMagnets = getMagnets(headC, headS);

            headM = getShortestMagnetToPoint(pAfterHead, headMagnets);
        }
        if ((tailBox != null) && !tailBox.intersects(lastBB))
        {
            final WiresMagnet[] magnets = getMagnetsNonOverlappedShapes(null, tailS, lastBB, tailBox);
            tailM = magnets[1];
        }
        else
        {
            final WiresMagnet[] tailMagnets = getMagnets(tailC, tailS);
            tailM = getShortestMagnetToPoint(pAfterHead, tailMagnets);
        }
        return new WiresMagnet[] { headM, tailM };
    }

    private WiresMagnet getShortestMagnetToPoint(final Point2D point, final WiresMagnet[] magnets)
    {
        double shortest = Double.MAX_VALUE;
        WiresMagnet shortestM = null;

        // Start at 1, as we don't include the center
        for (int i = 1, size0 = magnets.length; i < size0; i++)
        {
            final WiresMagnet m = magnets[i];
            if (m != null)
            {
                final double distance = point.distance(m.getControl().getComputedLocation());
                if (distance < shortest)
                {
                    shortest = distance;
                    shortestM = m;
                }
            }
        }
        return shortestM;
    }

    private WiresMagnet[] getMagnetsNonOverlappedShapes(final WiresShape headS, final WiresShape tailS, final BoundingBox headBox, final BoundingBox tailBox)
    {
        // There is no shape overlap.
        // If one box is in the corner of the other box, then use nearest corner connections
        // else use nearest mid connection.
        final boolean headAbove = headBox.getMaxY() < tailBox.getMinY();
        final boolean headBelow = headBox.getMinY() > tailBox.getMaxY();
        final boolean headLeft = headBox.getMaxX() < tailBox.getMinX();
        final boolean headRight = headBox.getMinX() > tailBox.getMaxX();

        WiresMagnet[] magets = null;

        if (headAbove)
        {
            if (headLeft)
            {
                magets = getMagnets(headS, 4, tailS, 8);
            }
            else if (headRight)
            {
                magets = getMagnets(headS, 6, tailS, 2);
            }
            else
            {
                magets = getMagnets(headS, 5, tailS, 1);
            }
        }
        else if (headBelow)
        {
            if (headLeft)
            {
                magets = getMagnets(headS, 2, tailS, 6);
            }
            else if (headRight)
            {
                magets = getMagnets(headS, 8, tailS, 4);
            }
            else
            {
                magets = getMagnets(headS, 1, tailS, 5);
            }
        }
        else
        {
            if (headLeft)
            {
                magets = getMagnets(headS, 3, tailS, 7);
            }
            else if (headRight)
            {
                magets = getMagnets(headS, 7, tailS, 3);
            }
        }
        return magets;
    }

    private WiresMagnet[] getMagnetsOverlappedShapesOrNoShape(final WiresConnection headC, final WiresConnection tailC, final WiresShape headS, final WiresShape tailS, final BoundingBox headBox, final BoundingBox tailBox)
    {
        // There is shape overlap, this is now much more difficult, so just pick which every has the the shortest distance
        // while giving preference to connections not contained by the other shape.
        double shortest = Double.MAX_VALUE;
        WiresMagnet shortestHeadM = null;
        WiresMagnet shortestTailM = null;
        final double headOffset = headC.getLine().getHeadOffset();
        final double tailOffset = tailC.getLine().getTailOffset();
        final double correction = headC.getLine().getCorrectionOffset();

        int minContained = Integer.MAX_VALUE;

        // to be able to handle auto connection and fixed, in the same logic. simply copy the available magnets to an array. Fixed is an array of 1
        final WiresMagnet[] headMagnets = getMagnets(headC, headS);
        final WiresMagnet[] tailMagnets = getMagnets(tailC, tailS);

        // Start at 1, as we don't include the center
        for (int i = 1, size0 = headMagnets.length; i < size0; i++)
        {
            final WiresMagnet headM = headMagnets[i];
            Point2D headOffSettedPoint;
            Point2D headOriginalPoint;

            if ((headS != null) && (headM != null))
            {
                headOriginalPoint = headM.getControl().getComputedLocation();
                headOffSettedPoint = headOriginalPoint.copy();
                OrthogonalPolyLine.correctEndWithOffset(headOffset, headM.getDirection(), headOffSettedPoint);
                OrthogonalPolyLine.correctEndWithOffset(correction, headM.getDirection(), headOffSettedPoint);
            }
            else
            {
                // Ideally this would have also applied corrections, but at this point it's not easy to determine the Direction,
                // but should probably be done at some point in the future.

                headOriginalPoint = headC.getControl().getComputedLocation();
                headOffSettedPoint = headOriginalPoint.copy();
            }
            final boolean headContained = (tailBox != null) ? tailBox.contains(headOriginalPoint) : false;

            for (int j = 1, size1 = tailMagnets.length; j < size1; j++)
            {
                final WiresMagnet tailM = tailMagnets[j];

                Point2D tailOffSettedPoint;
                Point2D tailOriginalPoint;

                if ((tailS != null) && (tailM != null))
                {
                    tailOriginalPoint = tailM.getControl().getComputedLocation();
                    tailOffSettedPoint = tailOriginalPoint.copy();
                    OrthogonalPolyLine.correctEndWithOffset(tailOffset, tailM.getDirection(), tailOffSettedPoint);
                    OrthogonalPolyLine.correctEndWithOffset(correction, tailM.getDirection(), tailOffSettedPoint);
                }
                else
                {
                    // Ideally this would have also applied corrections, but at this point it's not easy to determine the Direction,
                    // but should probably be done at some point in the future.

                    tailOriginalPoint = tailC.getControl().getComputedLocation();
                    tailOffSettedPoint = tailOriginalPoint.copy();
                }

                final boolean tailContained = (headBox != null) ? headBox.contains(tailOriginalPoint) : false;

                final double distance = headOffSettedPoint.distance(tailOffSettedPoint);
                int contained = 0;
                if (headContained)
                {
                    contained++;
                }
                if (tailContained)
                {
                    contained++;
                }
                if ((contained < minContained) || ((contained == minContained) && (distance <= shortest)))
                {
                    minContained = contained;
                    shortest = distance;
                    shortestHeadM = headM;
                    shortestTailM = tailM;
                }
            }
        }
        return new WiresMagnet[] { shortestHeadM, shortestTailM };
    }

    private WiresMagnet[] getMagnets(final WiresConnection connection, final WiresShape shape)
    {
        WiresMagnet[] magnets;

        if (connection.isAutoConnection())
        {
            magnets = new WiresMagnet[shape.getMagnets().size()];
            for (int i = 0, size = shape.getMagnets().size(); i < size; i++)
            {
                magnets[i] = shape.getMagnets().getMagnet(i);
            }
        }
        else if (shape == null)
        {
            // set it to 2, as centre is first, which is skipped.
            magnets = new WiresMagnet[2];
        }
        else
        {
            // set it to 2, as centre is first, which is skipped. And only populate the second
            magnets = new WiresMagnet[] { null, connection.getMagnet() };
        }
        return magnets;
    }

    WiresMagnet[] getMagnets(WiresShape headS, int headMagnetIndex, WiresShape tailS, int tailMagnetIndex)
    {
        // ony set the side if it's auto connect, else null

        // it uses 9, as center is 0
        int[] headMappng = null;
        int[] tailMappng = null;
        final boolean hasHeadMagnets = null != headS && null != headS.getMagnets();
        final boolean hasTailMagnets = null != tailS && null != tailS.getMagnets();
        if (hasHeadMagnets)
        {
            headMappng = headS.getMagnets().size() == 9 ? MagnetManager.EIGHT_CARDINALS_MAPPING : MagnetManager.FOUR_CARDINALS_MAPPING;
        }
        if (hasTailMagnets)
        {
            tailMappng = tailS.getMagnets().size() == 9 ? MagnetManager.EIGHT_CARDINALS_MAPPING : MagnetManager.FOUR_CARDINALS_MAPPING;
        }
        WiresMagnet headM = null;
        if ( hasHeadMagnets && getHeadConnection().isAutoConnection())
        {
            headM = headS.getMagnets().getMagnet(headMappng[headMagnetIndex]);
        }
        WiresMagnet tailM = null;
        if ( hasTailMagnets && getTailConnection().isAutoConnection())
        {
            tailM = tailS.getMagnets().getMagnet(tailMappng[tailMagnetIndex]);
        }
        return new WiresMagnet[] { headM, tailM };
    }


    public Point2DArray getControlPoints()
    {
        return m_line.getPoint2DArray();
    }

    public void addControlPoint(final double x,
                               final double y,
                               final int index) {
        if (index > -1) {
            final Point2D actual = getControlPoints().get(index);
            if (actual.getX() == x && actual.getY() == y) {
                return;
            }
            final Point2DArray oldPoints = getControlPoints();
            final Point2DArray newPoints = new Point2DArray();
            for (int i = 0; i < oldPoints.size(); i++) {
                if (i == index) {
                    newPoints.push(new Point2D(x, y));
                }
                newPoints.push(oldPoints.get(i));
            }
            m_line.setPoint2DArray(newPoints);
        }
    }

    public void destroyControlPoints(final int[] indexes) {
        final Point2DArray oldPoints = getLine().getPoint2DArray();
        final Point2DArray newPoints = new Point2DArray();
        for (int i = 0; i < oldPoints.size(); i++) {
            if (!contains(indexes, i)) {
                newPoints.push(oldPoints.get(i));
            }
        }
        getLine().setPoint2DArray(newPoints);
    }

    private static boolean contains(final int[] indexes,
                                    final int index) {
        for (int i : indexes) {
            if (i == index) {
                return true;
            }
        }
        return false;
    }

    public void moveControlPoint(final int index,
                                 final Point2D location) {
        getControlPoints().get(index).set(location);
        final IPrimitive<?> point = getControlPoint(index);
        if (null != point) {
            point.setLocation(location);
        }
    }

    private IPrimitive<?> getControlPoint(final int index) {
        return index < getPointHandles().size() ?
                getPointHandles().getHandle(index).getControl() :
                null;
    }

    public int getControlPointIndex(final double x,
                                     final double y) {
        int i = 0;
        for (IControlHandle handle : getPointHandles()) {
            if (handle.getControl().getX() == x &&
                    handle.getControl().getY() == y) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public int getIndexForSelectedSegment(final int x,
                                          final int y) {
        return getIndexForSelectedSegment(this, x, y, getControlPoints());
    }

    public static int getIndexForSelectedSegment(final WiresConnector connector,
                                                 final int mouseX,
                                                 final int mouseY,
                                                 final Point2DArray oldPoints) {
        NFastStringMap<Integer> colorMap = new NFastStringMap<Integer>();

        IDirectionalMultiPointShape<?> line = connector.getLine();
        ScratchPad scratch = line.getScratchPad();
        scratch.clear();
        PathPartList path = line.asShape().getPathPartList();
        int pointsIndex = 1;
        String color = MagnetManager.m_c_rotor.next();
        colorMap.put(color,
                     pointsIndex);
        Context2D ctx = scratch.getContext();
        double strokeWidth = line.asShape().getStrokeWidth();
        //setting a minimum stroke width to make finding a close point to the connector easier
        ctx.setStrokeWidth((strokeWidth < MINIMUM_STROKE_WITH ? MINIMUM_STROKE_WITH : strokeWidth));

        Point2D absolutePos = connector.getLine().getComputedLocation();
        double offsetX = absolutePos.getX();
        double offsetY = absolutePos.getY();

        Point2D pathStart = new Point2D(offsetX,
                                        offsetY);
        Point2D segmentStart = pathStart;

        for (int i = 0; i < path.size(); i++) {
            PathPartEntryJSO entry = path.get(i);
            NFastDoubleArrayJSO points = entry.getPoints();

            switch (entry.getCommand()) {
                case PathPartEntryJSO.MOVETO_ABSOLUTE: {
                    double x0 = points.get(0) + offsetX;
                    double y0 = points.get(1) + offsetY;
                    Point2D m = new Point2D(x0,
                                            y0);
                    if (i == 0) {
                        // this is position is needed, if we close the path.
                        pathStart = m;
                    }
                    segmentStart = m;
                    break;
                }
                case PathPartEntryJSO.LINETO_ABSOLUTE: {
                    points = entry.getPoints();
                    double x0 = points.get(0) + offsetX;
                    double y0 = points.get(1) + offsetY;
                    Point2D end = new Point2D(x0,
                                              y0);

                    if (oldPoints.get(pointsIndex).equals(segmentStart)) {
                        pointsIndex++;
                        color = MagnetManager.m_c_rotor.next();
                        colorMap.put(color,
                                     pointsIndex);
                    }
                    ctx.setStrokeColor(color);

                    ctx.beginPath();
                    ctx.moveTo(segmentStart.getX(),
                               segmentStart.getY());
                    ctx.lineTo(x0,
                               y0);
                    ctx.stroke();
                    segmentStart = end;
                    break;
                }
                case PathPartEntryJSO.CLOSE_PATH_PART: {
                    double x0 = pathStart.getX() + offsetX;
                    double y0 = pathStart.getY() + offsetY;
                    Point2D end = new Point2D(x0,
                                              y0);
                    if (oldPoints.get(pointsIndex).equals(segmentStart)) {
                        pointsIndex++;
                        color = MagnetManager.m_c_rotor.next();
                        colorMap.put(color,
                                     pointsIndex);
                    }
                    ctx.setStrokeColor(color);
                    ctx.beginPath();
                    ctx.moveTo(segmentStart.getX(),
                               segmentStart.getY());
                    ctx.lineTo(x0,
                               y0);
                    ctx.stroke();
                    segmentStart = end;
                    break;
                }
                case PathPartEntryJSO.CANVAS_ARCTO_ABSOLUTE: {
                    points = entry.getPoints();

                    double x0 = points.get(0) + offsetX;
                    double y0 = points.get(1) + offsetY;
                    Point2D p0 = new Point2D(x0,
                                             y0);

                    double x1 = points.get(2) + offsetX;
                    double y1 = points.get(3) + offsetY;
                    double r = points.get(4);

                    if (p0.equals(oldPoints.get(pointsIndex))) {
                        pointsIndex++;
                        color = MagnetManager.m_c_rotor.next();
                        colorMap.put(color,
                                     pointsIndex);
                    }
                    ctx.setStrokeColor(color);
                    ctx.beginPath();
                    ctx.moveTo(segmentStart.getX(),
                               segmentStart.getY());
                    ctx.arcTo(x0,
                              y0,
                              x1,
                              y1,
                              r);
                    ctx.stroke();

                    segmentStart = new Point2D(x1,
                                               y1);
                    break;
                }
            }
        }

        BoundingBox box = connector.getLine().getBoundingBox();

        // Keep the ImageData small by clipping just the visible line area
        // But remember the mouse must be offset for this clipped area.
        int sx = (int) (box.getX() - strokeWidth - offsetX);
        int sy = (int) (box.getY() - strokeWidth - offsetY);
        ImageData backing = ctx.getImageData(sx,
                                             sy,
                                             (int) (box.getWidth() + strokeWidth + strokeWidth),
                                             (int) (box.getHeight() + strokeWidth + strokeWidth));
        color = BackingColorMapUtils.findColorAtPoint(backing,
                                                      mouseX - sx,
                                                      mouseY - sy);
        return null != color ? colorMap.get(color) : -1;
    }

    @Override public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass()))
        {
            return false;
        }
        final WiresConnector that = (WiresConnector) o;

        return getGroup().uuid().equals(that.getGroup().uuid());
    }

    @Override
    public int hashCode()
    {
        return getGroup().uuid().hashCode();
    }

    public static boolean updateHeadTailForRefreshedConnector(WiresConnector c)
    {
        // Iterate each refreshed line and get the new points for the decorators
        if (c.getLine().asShape().getPathPartList().size() < 1)
        {
            // only do this for lines that have had refresh called
            IDirectionalMultiPointShape<?> line = c.getLine();

            if ( c.isSpecialConnection() && line.asShape().getPathPartList().size() == 0)
            {
                // if getPathPartList is empty, it was refreshed due to a point change
                c.updateForSpecialConnections(false);
            }

            final boolean prepared = line.isPathPartListPrepared(c.getLine().getAttributes());

            if (!prepared)
            {
                return true;
            }

            Point2DArray points     = line.getPoint2DArray();
            Point2D      p0         = points.get(0);
            Point2D      p1         = line.getHeadOffsetPoint();
            Point2DArray headPoints = new Point2DArray(p1, p0);
            c.getHeadDecorator().draw(headPoints);

            p0 = points.get(points.size() - 1);
            p1 = line.getTailOffsetPoint();
            Point2DArray tailPoints = new Point2DArray(p1, p0);
            c.getTailDecorator().draw(tailPoints);
        }
        return false;
    }
}
