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

import com.ait.lienzo.client.core.event.NodeDragEndEvent;
import com.ait.lienzo.client.core.event.NodeMouseClickEvent;
import com.ait.lienzo.client.core.event.NodeMouseDownEvent;
import com.ait.lienzo.client.core.event.NodeMouseUpEvent;
import com.ait.lienzo.client.core.shape.wires.PickerPart;
import com.ait.lienzo.client.core.shape.wires.WiresContainer;
import com.ait.lienzo.client.core.shape.wires.WiresManager;
import com.ait.lienzo.client.core.shape.wires.WiresShape;
import com.ait.lienzo.client.core.shape.wires.handlers.MouseEvent;
import com.ait.lienzo.client.core.shape.wires.handlers.WiresShapeControl;
import com.ait.lienzo.client.core.shape.wires.handlers.WiresShapeHighlight;
import com.ait.lienzo.client.core.types.Point2D;
import com.ait.lienzo.client.widget.DragContext;
import com.ait.tooling.common.api.java.util.function.Consumer;

/**
 * This handler's goals are:
 * - Delegate some mouse interactions for a single wires shape to its wires shape m_control instance
 * - Displays some highlights to provide feedback for containment and docking operations.
 */
public class WiresShapeHandlerImpl extends WiresManager.WiresDragHandler implements WiresShapeHandler
{
    private final WiresShape                                shape;

    private final WiresShapeHighlight<PickerPart.ShapePart> m_highlight;

    private final Consumer<NodeMouseClickEvent>             clickEventConsumer;

    public WiresShapeHandlerImpl(final WiresShape shape,
                             final WiresShapeHighlight<PickerPart.ShapePart> highlight,
                             final WiresManager manager) {
        super(manager);
        this.shape = shape;
        this.m_highlight = highlight;
        this.clickEventConsumer = new Consumer<NodeMouseClickEvent>() {
            @Override
            public void accept(NodeMouseClickEvent event) {
                if (getWiresManager().getSelectionManager() != null) {
                    getWiresManager().getSelectionManager().selected(getShape(),
                                                                     event.isShiftKeyDown());
                }
            }
        };
    }

    public WiresShapeHandlerImpl(final WiresShape shape,
                                 final WiresShapeHighlight<PickerPart.ShapePart> highlight,
                                 final WiresManager manager,
                                 final Consumer<NodeMouseClickEvent> clickEventConsumer) {
        super(manager);
        this.shape = shape;
        this.m_highlight = highlight;
        this.clickEventConsumer = clickEventConsumer;
    }

    @Override
    public void startDrag(final DragContext dragContext)
    {
        super.startDrag(dragContext);

        // Delegate start dragging to shape control.
        getControl().onMoveStart(dragContext.getDragStartX(),
                            dragContext.getDragStartY());

        // Highlights.
        final WiresShape parent = getParentShape();
        if (null != parent)
        {
            if (isDocked(getShape()))
            {
                m_highlight.highlight(getParentShape(), PickerPart.ShapePart.BORDER);
            }
            else
            {
                m_highlight.highlight(getParentShape(), PickerPart.ShapePart.BODY);
            }
        }
    }

    @Override
    protected boolean doAdjust(final Point2D dxy)
    {
        // Keep parent shape and part instances before moving to another location.
        final WiresShape parent = getParentShape();
        final PickerPart.ShapePart parentPart = getParentShapePart();

        boolean adjusted = false;
        // Delegate drag adjustments to shape control.
        if (getControl().onMove(dxy.getX(),
                           dxy.getY())) {
            dxy.set(getControl().getAdjust());
            adjusted = true;
        }

        // Check acceptors' allow methods.
        final boolean isDockAllowed = null != getControl().getDockingControl() && getControl().getDockingControl().isAllow();
        final boolean isContAllow = null != getControl().getContainmentControl() && getControl().getContainmentControl().isAllow();

        // Highlights.
        final WiresShape newParent = getParentShape();
        final PickerPart.ShapePart newParentPart = getParentShapePart();
        if ((parent != newParent) || (parentPart != newParentPart))
        {
            m_highlight.restore();
        }

        if (null != newParent)
        {
            if (isDockAllowed)
            {
                m_highlight.highlight(newParent, PickerPart.ShapePart.BORDER);
            }
            else if (isContAllow)
            {
                m_highlight.highlight(newParent, PickerPart.ShapePart.BODY);
            }
            else
            {
                m_highlight.error(newParent, PickerPart.ShapePart.BODY);
            }
        }
        return adjusted;
    }

    @Override
    protected void doOnNodeDragEnd(final NodeDragEndEvent event)
    {
        final Point2D distanceAdjusted = event.getDragContext().getDistanceAdjusted();
        final Double adjustedX = distanceAdjusted.getX();
        final Double adjustedY = distanceAdjusted.getY();
        final int dx = adjustedX.intValue();
        final int dy = adjustedY.intValue();

        getControl().onMove(dx, dy);

        // Complete the control operation.
        if (getControl().onMoveComplete() && getControl().accept()) {
            getControl().execute();
        } else {
            reset();
        }

        // Restore highlights, if any.
        m_highlight.restore();
    }

    @Override
    protected void doReset()
    {
        super.doReset();
        m_highlight.restore();
    }

    @Override
    public void onNodeMouseClick(NodeMouseClickEvent event) {
        getControl().onMouseClick(new MouseEvent(event.getX(),
                                                 event.getY(),
                                                 event.isShiftKeyDown(),
                                                 event.isAltKeyDown(),
                                                 event.isControlKeyDown()));
        clickEventConsumer.accept(event);
    }

    @Override
    public void onNodeMouseDown(NodeMouseDownEvent event) {
        getControl().onMouseDown(new MouseEvent(event.getX(),
                                           event.getY(),
                                           event.isShiftKeyDown(),
                                           event.isAltKeyDown(),
                                           event.isControlKeyDown()));
    }

    @Override
    public void onNodeMouseUp(NodeMouseUpEvent event) {
        getControl().onMouseUp(new MouseEvent(event.getX(),
                                         event.getY(),
                                         event.isShiftKeyDown(),
                                         event.isAltKeyDown(),
                                         event.isControlKeyDown()));
    }

    @Override
    public WiresShapeControl getControl() {
        return shape.getControl();
    }

    private WiresShape getShape()
    {
        return getControl().getParentPickerControl().getShape();
    }

    private WiresShape getParentShape()
    {
        final WiresContainer parent = getControl().getParentPickerControl().getParent();
        return (null != parent) && (parent instanceof WiresShape) ? (WiresShape) parent : null;
    }

    private PickerPart.ShapePart getParentShapePart()
    {
        return getControl().getParentPickerControl().getParentShapePart();
    }

    private final boolean isDocked(final WiresShape shape) {
        return null != shape.getDockedTo();
    }
}
