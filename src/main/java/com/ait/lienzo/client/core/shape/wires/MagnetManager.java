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

import com.ait.lienzo.client.core.Attribute;
import com.ait.lienzo.client.core.Context2D;
import com.ait.lienzo.client.core.event.AttributesChangedEvent;
import com.ait.lienzo.client.core.event.AttributesChangedHandler;
import com.ait.lienzo.client.core.event.NodeDragEndEvent;
import com.ait.lienzo.client.core.event.NodeDragEndHandler;
import com.ait.lienzo.client.core.event.NodeDragMoveEvent;
import com.ait.lienzo.client.core.event.NodeDragMoveHandler;
import com.ait.lienzo.client.core.event.NodeDragStartEvent;
import com.ait.lienzo.client.core.event.NodeDragStartHandler;
import com.ait.lienzo.client.core.shape.Circle;
import com.ait.lienzo.client.core.shape.Group;
import com.ait.lienzo.client.core.shape.IPrimitive;
import com.ait.lienzo.client.core.shape.Shape;
import com.ait.lienzo.client.core.shape.wires.handlers.WiresMagnetsControl;
import com.ait.lienzo.client.core.types.BoundingBox;
import com.ait.lienzo.client.core.types.ColorKeyRotor;
import com.ait.lienzo.client.core.types.ImageData;
import com.ait.lienzo.client.core.types.Point2D;
import com.ait.lienzo.client.core.types.Point2DArray;
import com.ait.lienzo.client.core.util.Geometry;
import com.ait.lienzo.client.core.util.ScratchPad;
import com.ait.lienzo.shared.core.types.ColorName;
import com.ait.lienzo.shared.core.types.Direction;
import com.ait.lienzo.shared.core.types.DragMode;
import com.ait.tooling.nativetools.client.collection.NFastStringMap;
import com.ait.tooling.nativetools.client.event.HandlerRegistrationManager;

public class MagnetManager
{
    public static final Direction[]       FOUR_CARDINALS          = new Direction[] { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST };

    public static final int[]             FOUR_CARDINALS_MAPPING  = new int[] { 0, 1, 1, 2, 3, 3, 3, 4, 1 };

    public static final Direction[]       EIGHT_CARDINALS         = new Direction[] { Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };

    public static final int[]             EIGHT_CARDINALS_MAPPING = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 };

    private static final int              CONTROL_RADIUS          = 7;

    public static final ColorKeyRotor     m_c_rotor               = new ColorKeyRotor();

    private final NFastStringMap<Magnets> m_magnetRegistry        = new NFastStringMap<>();

    private int                           m_ctrlSize              = CONTROL_RADIUS;

    public ImageData drawMagnetsToBack(final Magnets magnets, final NFastStringMap<WiresShape> shapeColors, final NFastStringMap<WiresMagnet> magnetColors, final ScratchPad scratch)
    {
        scratch.clear();
        final Context2D ctx = scratch.getContext();

        drawShapeToBacking(magnets, shapeColors, ctx);

        magnetColors.clear();
        for (int i = 0; i < magnets.size(); i++)
        {
            drawMagnet(magnetColors, ctx, magnets.getMagnet(i));
        }
        return ctx.getImageData(0, 0, scratch.getWidth(), scratch.getHeight());
    }

    protected void drawShapeToBacking(final Magnets magnets, final NFastStringMap<WiresShape> shapeColorMap, final Context2D ctx)
    {
        // the Shape doesn't need recording, we just need to know the mouse is over something
        BackingColorMapUtils.drawShapeToBacking(ctx, magnets.getWiresShape(), m_c_rotor.next(), shapeColorMap);
    }

    protected void drawMagnet(final NFastStringMap<WiresMagnet> magnetColorMap, final Context2D ctx, final WiresMagnet m)
    {
        final String c = m_c_rotor.next();
        magnetColorMap.put(c, m);
        ctx.beginPath();
        ctx.setStrokeWidth(m_ctrlSize);
        ctx.setStrokeColor(c);
        ctx.setFillColor(c);
        ctx.arc(m.getControl().getX(), m.getControl().getY(), m_ctrlSize, 0, 2 * Math.PI, false);
        ctx.stroke();
        ctx.fill();
    }

    public Magnets createMagnets(final WiresShape wiresShape)
    {
        return createMagnets(wiresShape, EIGHT_CARDINALS);
    }

    /**
     * Right now it only works with provided FOUR or EIGHT cardinals, anything else will break WiresConnector autoconnection
     *
     * @param wiresShape
     * @param requestedCardinals
     * @return
     */
    public Magnets createMagnets(final WiresShape wiresShape, final Direction[] requestedCardinals)
    {
        final IPrimitive<?> primTarget = wiresShape.getGroup();
        final Point2DArray points = getWiresIntersectionPoints(wiresShape, requestedCardinals);
        final ControlHandleList list = new ControlHandleList(primTarget);
        final BoundingBox box = wiresShape.getPath().getBoundingBox();

        final Point2D primLoc = primTarget.getComputedLocation();
        final Magnets magnets = new Magnets(this, list, wiresShape);

        int i = 0;
        for (final Point2D p : points)
        {
            final double mx = primLoc.getX() + p.getX();
            final double my = primLoc.getY() + p.getY();
            final WiresMagnet m = new WiresMagnet(magnets, null, i++, p.getX(), p.getY(), getControlPrimitive(mx, my), true);
            final Direction d = getDirection(p, box);
            m.setDirection(d);
            list.add(m);
        }

        final String uuid = wiresShape.uuid();
        m_magnetRegistry.put(uuid, magnets);

        wiresShape.setMagnets(magnets);

        return magnets;
    }

    public Magnets getMagnets(final IPrimitive<?> shape)
    {
        return m_magnetRegistry.get(shape.uuid());
    }

    public static Direction getDirection(final Point2D point, final BoundingBox box)
    {
        final double left = box.getMinX();
        final double right = box.getMaxX();
        final double top = box.getMinY();
        final double bottom = box.getMaxY();

        final double x = point.getX();
        final double y = point.getY();

        final double leftDist = Math.abs(x - left);
        final double rightDist = Math.abs(x - right);

        final double topDist = Math.abs(y - top);
        final double bottomDist = Math.abs(y - bottom);

        final boolean moreLeft = leftDist < rightDist;
        final boolean moreTop = topDist < bottomDist;

        if ((leftDist == rightDist) && (topDist == bottomDist))
        {
            // this is the center, so return NONE
            return Direction.NONE;
        }
        if (moreLeft)
        {
            if (moreTop)
            {
                if (topDist < leftDist)
                {
                    return Direction.NORTH;
                }
                else if (topDist > leftDist)
                {
                    return Direction.WEST;
                }
                else
                {
                    return Direction.WEST;//Direction.NORTH_WEST;
                }
            }
            else
            {
                if (bottomDist < leftDist)
                {
                    return Direction.SOUTH;
                }
                else if (bottomDist > leftDist)
                {
                    return Direction.WEST;
                }
                else
                {
                    return Direction.WEST;//Direction.SOUTH_WEST;
                }
            }
        }
        else
        {
            if (moreTop)
            {
                if (topDist < rightDist)
                {
                    return Direction.NORTH;
                }
                else if (topDist > rightDist)
                {
                    return Direction.EAST;
                }
                else
                {
                    return Direction.EAST;//Direction.NORTH_EAST;
                }
            }
            else
            {
                if (bottomDist < rightDist)
                {
                    return Direction.SOUTH;
                }
                else if (bottomDist > rightDist)
                {
                    return Direction.EAST;
                }
                else
                {
                    return Direction.EAST;//Direction.SOUTH_EAST;
                }
            }
        }
    }

    public void setHotspotSize(final int m_ctrlSize)
    {
        this.m_ctrlSize = m_ctrlSize;
    }

    public static Point2DArray getWiresIntersectionPoints(final WiresShape wiresShape, final Direction[] requestedCardinals)
    {
        return Geometry.getCardinalIntersects(wiresShape.getPath(), requestedCardinals);
    }

    private Circle getControlPrimitive(final double x, final double y)
    {
        return new Circle(m_ctrlSize).setX(x).setY(y).setFillColor(ColorName.DARKRED).setFillAlpha(0.8).setStrokeAlpha(0).setDraggable(true).setDragMode(DragMode.SAME_LAYER);
    }

    public static class Magnets implements AttributesChangedHandler, NodeDragStartHandler, NodeDragMoveHandler, NodeDragEndHandler
    {
        private final IControlHandleList         m_list;

        private final MagnetManager              m_magnetManager;

        private final WiresShape                 m_wiresShape;

        private boolean                          m_isDragging;

        private final HandlerRegistrationManager m_registrationManager = new HandlerRegistrationManager();

        public Magnets(final MagnetManager magnetManager, final IControlHandleList list, final WiresShape wiresShape)
        {
            m_list = list;
            m_magnetManager = magnetManager;
            m_wiresShape = wiresShape;

            final Group shapeGroup = wiresShape.getGroup();
            m_registrationManager.register(shapeGroup.addAttributesChangedHandler(Attribute.X, this));
            m_registrationManager.register(shapeGroup.addAttributesChangedHandler(Attribute.Y, this));
            m_registrationManager.register(shapeGroup.addNodeDragStartHandler(this));
            m_registrationManager.register(shapeGroup.addNodeDragMoveHandler(this));
            m_registrationManager.register(shapeGroup.addNodeDragEndHandler(this));
        }

        public boolean isEmpty()
        {
            return m_list.isEmpty();
        }

        public WiresShape getWiresShape()
        {
            return m_wiresShape;
        }

        @Override
        public void onAttributesChanged(final AttributesChangedEvent event)
        {
            if (!m_isDragging && event.any(Attribute.X, Attribute.Y))
            {
                getControl().shapeMoved();
            }
        }

        @Override
        public void onNodeDragStart(final NodeDragStartEvent event)
        {
            m_isDragging = true;
        }

        @Override
        public void onNodeDragEnd(final NodeDragEndEvent event)
        {
            m_isDragging = false;
            getControl().shapeMoved();
        }

        @Override
        public void onNodeDragMove(final NodeDragMoveEvent event)
        {
            getControl().shapeMoved();
        }

        public void show()
        {
            m_list.show();
            batch();
        }

        public void hide()
        {
            m_list.hide();
            batch();
        }

        public void destroy()
        {
            m_list.destroy();

            m_registrationManager.removeHandler();

            m_magnetManager.m_magnetRegistry.remove(m_wiresShape.uuid());

        }

        public void destroy(final WiresMagnet magnet)
        {
            m_list.remove(magnet);
        }

        public IControlHandleList getMagnets()
        {
            return m_list;
        }

        public int size()
        {
            return m_list.size();
        }

        public Shape<?> getShape()
        {
            return m_wiresShape.getPath();
        }

        public Group getGroup()
        {
            return m_wiresShape.getGroup();
        }

        public WiresMagnet getMagnet(final int index)
        {
            return (WiresMagnet) m_list.getHandle(index);
        }

        private WiresMagnetsControl getControl()
        {
            return getWiresShape().getControl().getMagnetsControl();
        }

        private void batch()
        {
            if (null != m_wiresShape.getPath().getLayer())
            {
                m_wiresShape.getPath().getLayer().batch();
            }
        }
    }
}
