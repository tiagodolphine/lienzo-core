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

import static com.ait.lienzo.client.core.AttributeOp.any;

import java.util.Objects;

import com.ait.lienzo.client.core.Attribute;
import com.ait.lienzo.client.core.event.AnimationFrameAttributesChangedBatcher;
import com.ait.lienzo.client.core.event.AttributesChangedEvent;
import com.ait.lienzo.client.core.event.AttributesChangedHandler;
import com.ait.lienzo.client.core.event.IAttributesChangedBatcher;
import com.ait.lienzo.client.core.event.NodeDragEndEvent;
import com.ait.lienzo.client.core.event.NodeDragEndHandler;
import com.ait.lienzo.client.core.event.NodeDragMoveEvent;
import com.ait.lienzo.client.core.event.NodeDragMoveHandler;
import com.ait.lienzo.client.core.event.NodeDragStartEvent;
import com.ait.lienzo.client.core.event.NodeDragStartHandler;
import com.ait.lienzo.client.core.shape.Group;
import com.ait.lienzo.client.core.shape.IContainer;
import com.ait.lienzo.client.core.shape.IPrimitive;
import com.ait.lienzo.client.core.shape.wires.event.WiresDragEndEvent;
import com.ait.lienzo.client.core.shape.wires.event.WiresDragEndHandler;
import com.ait.lienzo.client.core.shape.wires.event.WiresDragMoveEvent;
import com.ait.lienzo.client.core.shape.wires.event.WiresDragMoveHandler;
import com.ait.lienzo.client.core.shape.wires.event.WiresDragStartEvent;
import com.ait.lienzo.client.core.shape.wires.event.WiresDragStartHandler;
import com.ait.lienzo.client.core.shape.wires.event.WiresMoveEvent;
import com.ait.lienzo.client.core.shape.wires.event.WiresMoveHandler;
import com.ait.lienzo.client.core.types.Point2D;
import com.ait.tooling.common.api.flow.Flows;
import com.ait.tooling.nativetools.client.collection.NFastArrayList;
import com.ait.tooling.nativetools.client.event.HandlerRegistrationManager;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;

public class WiresContainer
{
    private static final Flows.BooleanOp     XYWH_OP         = any(Attribute.X, Attribute.Y);

    private final NFastArrayList<WiresShape> m_childShapes;

    private final IContainer<?, IPrimitive<?>>     m_container;

    private WiresContainer                   m_parent;

    private final HandlerManager             m_events;

    private final IAttributesChangedBatcher  m_attributesChangedBatcher;

    private final HandlerRegistrationManager m_registrationManager;

    private WiresContainer                   m_dockedTo;

    private WiresManager                     m_wiresManager;

    private boolean                          m_drag_initialized;

    private boolean                          m_dragging;

    private ILayoutHandler                   m_layoutHandler = ILayoutHandler.NONE;

    public WiresContainer(final IContainer<?, IPrimitive<?>> container)
    {
        this(container, null, new HandlerRegistrationManager(), new AnimationFrameAttributesChangedBatcher());
    }

    WiresContainer(final IContainer<?, IPrimitive<?>> container, final HandlerManager events, final HandlerRegistrationManager registrationManager, final IAttributesChangedBatcher attributesChangedBatcher)
    {
        m_container = container;

        m_events = null != events ? events : new HandlerManager(this);

        m_dragging = false;

        m_drag_initialized = false;

        m_childShapes = new NFastArrayList<>();

        m_registrationManager = registrationManager;

        m_attributesChangedBatcher = attributesChangedBatcher;
    }

    public WiresManager getWiresManager()
    {
        return m_wiresManager;
    }

    public void setWiresManager(final WiresManager wiresManager)
    {
        m_wiresManager = wiresManager;
    }

    public IContainer<?, IPrimitive<?>> getContainer()
    {
        return m_container;
    }

    public Group getGroup()
    {
        return getContainer().asGroup();
    }

    public double getX()
    {
        return getGroup().getX();
    }

    public double getY()
    {
        return getGroup().getY();
    }

    public WiresContainer setLocation(final Point2D p)
    {
        getGroup().setLocation(p);

        shapeMoved();

        return this;
    }

    public Point2D getLocation()
    {
        return getGroup().getLocation();
    }

    public Point2D getComputedLocation()
    {
        return getGroup().getComputedLocation();
    }

    public WiresContainer getParent()
    {
        return m_parent;
    }

    public void setParent(final WiresContainer parent)
    {
        m_parent = parent;
    }

    public NFastArrayList<WiresShape> getChildShapes()
    {
        return m_childShapes;
    }

    public ILayoutHandler getLayoutHandler()
    {
        return m_layoutHandler;
    }

    public void setLayoutHandler(final ILayoutHandler layoutHandler)
    {
        this.m_layoutHandler = layoutHandler;
    }

    public void add(final WiresShape shape)
    {
        if (shape.getParent() == this)
        {
            return;
        }
        if (shape.getParent() != null)
        {
            shape.removeFromParent();
        }
        m_childShapes.add(shape);

        // This is needed as a workaround, due to getComputedBoundingBox needed atleast x and y set to something, else it won't work.
        Group group = shape.getGroup();
        if ( !group.getAttributes().isDefined(Attribute.X) )
        {
            group.setX(0);
        }
        if ( !group.getAttributes().isDefined(Attribute.Y) )
        {
            group.setY(0);
        }

        m_container.add(shape.getGroup());

        shape.setParent(this);

        shape.shapeMoved();

        if (null != m_wiresManager && m_wiresManager.getAlignAndDistribute().isShapeIndexed(shape.uuid()))
        {
            m_wiresManager.getAlignAndDistribute().getControlForShape(shape.uuid()).updateIndex();
        }
        getLayoutHandler().requestLayout(this);
    }

    public void shapeMoved()
    {
        // Delegate to children.
        if ((getChildShapes() != null) && !getChildShapes().isEmpty())
        {
            for (final WiresShape child : getChildShapes())
            {
                child.shapeMoved();
            }
        }
        // Notify the update..
        fireMove();
    }

    public void remove(final WiresShape shape)
    {
        if (m_childShapes != null)
        {
            m_childShapes.remove(shape);

            m_container.remove(shape.getGroup());

            shape.setParent(null);
        }
        getLayoutHandler().requestLayout(this);
    }

    public void setDockedTo(final WiresContainer dockedTo)
    {
        m_dockedTo = dockedTo;
    }

    public WiresContainer getDockedTo()
    {
        return m_dockedTo;
    }

    public WiresContainer setDraggable(final boolean draggable)
    {
        ensureHandlers();

        getGroup().setDraggable(draggable);

        return this;
    }

    private void ensureHandlers()
    {
        if (!m_drag_initialized && (null != m_container))
        {
            m_registrationManager.register(m_container.addNodeDragStartHandler(new NodeDragStartHandler()
            {
                @Override
                public void onNodeDragStart(final NodeDragStartEvent event)
                {
                    WiresContainer.this.m_dragging = true;

                    m_events.fireEvent(new WiresDragStartEvent(WiresContainer.this, event));
                }
            }));
            m_registrationManager.register(m_container.addNodeDragMoveHandler(new NodeDragMoveHandler()
            {
                @Override
                public void onNodeDragMove(final NodeDragMoveEvent event)
                {
                    WiresContainer.this.m_dragging = true;

                    m_events.fireEvent(new WiresDragMoveEvent(WiresContainer.this, event));
                }
            }));
            m_registrationManager.register(m_container.addNodeDragEndHandler(new NodeDragEndHandler()
            {
                @Override
                public void onNodeDragEnd(final NodeDragEndEvent event)
                {
                    WiresContainer.this.m_dragging = false;

                    m_events.fireEvent(new WiresDragEndEvent(WiresContainer.this, event));
                }
            }));
            m_container.setAttributesChangedBatcher(m_attributesChangedBatcher);

            final AttributesChangedHandler handler = new AttributesChangedHandler()
            {
                @Override
                public void onAttributesChanged(final AttributesChangedEvent event)
                {
                    if (!WiresContainer.this.m_dragging && event.evaluate(XYWH_OP))
                    {
                        fireMove();
                    }
                }
            };
            // Attribute change handlers.
            m_registrationManager.register(m_container.addAttributesChangedHandler(Attribute.X, handler));

            m_registrationManager.register(m_container.addAttributesChangedHandler(Attribute.Y, handler));

            m_drag_initialized = true;
        }
    }

    private void fireMove()
    {
        m_events.fireEvent(new WiresMoveEvent(WiresContainer.this, (int) getLocation().getX(), (int) getLocation().getY()));
    }

    public final HandlerRegistration addWiresMoveHandler(final WiresMoveHandler handler)
    {
        return m_events.addHandler(WiresMoveEvent.TYPE, Objects.requireNonNull(handler));
    }

    public final HandlerRegistration addWiresDragStartHandler(final WiresDragStartHandler dragHandler)
    {
        return m_events.addHandler(WiresDragStartEvent.TYPE, Objects.requireNonNull(dragHandler));
    }

    public final HandlerRegistration addWiresDragMoveHandler(final WiresDragMoveHandler dragHandler)
    {
        return m_events.addHandler(WiresDragMoveEvent.TYPE, Objects.requireNonNull(dragHandler));
    }

    public final HandlerRegistration addWiresDragEndHandler(final WiresDragEndHandler dragHandler)
    {
        return m_events.addHandler(WiresDragEndEvent.TYPE, Objects.requireNonNull(dragHandler));
    }

    public void destroy()
    {
        for (WiresShape shape : m_childShapes) {
            remove(shape);
        }
        m_childShapes.clear();
        m_registrationManager.removeHandler();
        m_container.setAttributesChangedBatcher(null);
        m_attributesChangedBatcher.cancelAttributesChangedBatcher();
        // TODO: m_events.removeHandler();
        m_container.removeFromParent();

        m_parent = null;
        m_dockedTo = null;
        m_wiresManager = null;
        m_layoutHandler = null;
    }

    protected HandlerManager getHandlerManager()
    {
        return m_events;
    }
}
