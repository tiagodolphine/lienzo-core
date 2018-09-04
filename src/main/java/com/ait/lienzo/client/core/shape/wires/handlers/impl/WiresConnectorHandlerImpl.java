package com.ait.lienzo.client.core.shape.wires.handlers.impl;

import java.util.concurrent.atomic.AtomicBoolean;

import com.ait.lienzo.client.core.event.NodeDragEndEvent;
import com.ait.lienzo.client.core.event.NodeDragMoveEvent;
import com.ait.lienzo.client.core.event.NodeDragStartEvent;
import com.ait.lienzo.client.core.event.NodeMouseClickEvent;
import com.ait.lienzo.client.core.event.NodeMouseDownEvent;
import com.ait.lienzo.client.core.event.NodeMouseMoveEvent;
import com.ait.lienzo.client.core.shape.Shape;
import com.ait.lienzo.client.core.shape.wires.IControlHandle;
import com.ait.lienzo.client.core.shape.wires.WiresConnector;
import com.ait.lienzo.client.core.shape.wires.WiresManager;
import com.ait.lienzo.client.core.shape.wires.handlers.WiresConnectorControl;
import com.ait.lienzo.client.core.shape.wires.handlers.WiresConnectorHandler;
import com.ait.lienzo.client.core.types.BoundingPoints;
import com.ait.lienzo.client.core.types.Point2D;
import com.ait.lienzo.client.core.types.Point2DArray;
import com.ait.lienzo.client.core.util.Geometry;
import com.ait.tooling.common.api.java.util.function.Consumer;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.user.client.Timer;

import static com.ait.lienzo.client.core.shape.AbstractMultiPointShape.DefaultMultiPointShapeHandleFactory.SELECTION_OFFSET;

public class WiresConnectorHandlerImpl implements WiresConnectorHandler {

    private final WiresConnector m_connector;
    private final WiresManager m_wiresManager;
    private final Consumer<Event> clickEventConsumer;
    private final Consumer<Event> mouseDownEventConsumer;
    private Timer clickTimer;
    private Event event;
    private boolean hasTransientControlHandleToken;

    //Token to control the concurrency between connectors when creating transient control handle
    private static AtomicBoolean transientControlHandleToken = new AtomicBoolean(false);

    public static class Event {
        final double x;
        final double y;
        final boolean isShiftKeyDown;

        public Event(final double x,
                     final double y,
                     final boolean isShiftKeyDown)
        {
            this.x = x;
            this.y = y;
            this.isShiftKeyDown = isShiftKeyDown;
        }
    }

    public static WiresConnectorHandlerImpl build(final WiresConnector connector,
                                                  final WiresManager wiresManager) {
        final WiresConnectorEventConsumers consumers = new WiresConnectorEventConsumers(connector);
        return new WiresConnectorHandlerImpl(connector,
                                             wiresManager,
                                             consumers.switchVisibility(),
                                             consumers.addControlPoint());
    }

    public WiresConnectorHandlerImpl(final WiresConnector connector,
                                     final WiresManager wiresManager,
                                     final Consumer<Event> clickEventConsumer,
                                     final Consumer<Event> mouseDownEventConsumer) {
        this(connector, wiresManager, clickEventConsumer, mouseDownEventConsumer, null);

        this.clickTimer = new Timer() {
            @Override
            public void run() {
                if (getWiresManager().getSelectionManager() != null) {
                    getWiresManager().getSelectionManager().selected(connector,
                                                                     event.isShiftKeyDown);
                }
                clickEventConsumer.accept(event);
                event = null;
            }
        };
    }

    WiresConnectorHandlerImpl(final WiresConnector connector,
                              final WiresManager wiresManager,
                              final Consumer<Event> clickEventConsumer,
                              final Consumer<Event> mouseDownEventConsumer,
                              final Timer clickTimer) {
        this.m_connector = connector;
        this.m_wiresManager = wiresManager;
        this.clickEventConsumer = clickEventConsumer;
        this.mouseDownEventConsumer = mouseDownEventConsumer;
        this.clickTimer = clickTimer;
    }

    @Override
    public void onNodeDragStart(final NodeDragStartEvent event) {
        this.getControl().onMoveStart(event.getDragContext().getDragStartX(),
                                      event.getDragContext().getDragStartY());
    }

    @Override
    public void onNodeDragMove(final NodeDragMoveEvent event) {
        this.getControl().onMove(event.getDragContext().getDragStartX(),
                                 event.getDragContext().getDragStartY());
    }

    @Override
    public void onNodeDragEnd(final NodeDragEndEvent event) {
        if (getControl().onMoveComplete()) {
            getControl().execute();
        } else {
            getControl().reset();
        }
    }

    @Override
    public void onNodeMouseClick(final NodeMouseClickEvent event) {
        clickTimer.cancel();
        setEvent(event.getX(), event.getY(), event.isShiftKeyDown());
        clickTimer.schedule(50);
    }

    private Event setEvent(int x, int y, boolean shiftKeyDown) {
        this.event = new Event(x, y, shiftKeyDown);
        return this.event;
    }

    public static class WiresConnectorEventConsumers {
        private final WiresConnector connector;

        public WiresConnectorEventConsumers(final WiresConnector connector) {
            this.connector = connector;
        }

        public Consumer<Event> switchVisibility() {
            return new Consumer<Event>() {
                @Override
                public void accept(Event event) {
                    final WiresConnectorControl control = connector.getControl();
                    if (control.areControlPointsVisible()) {
                        control.hideControlPoints();
                    } else {
                        control.showControlPoints();
                    }
                }
            };
        }

        public Consumer<Event> addControlPoint() {
            return new Consumer<Event>() {
                @Override
                public void accept(Event event) {
                    connector.getControl().addControlPoint(event.x, event.y);
                }
            };
        }

    }

    public WiresConnectorControl getControl() {
        return m_connector.getControl();
    }

    WiresConnector getConnector() {
        return m_connector;
    }

    WiresManager getWiresManager() {
        return m_wiresManager;
    }

    private void addControlPoint(final Point2D point) {
        destroyTransientControlHandle();
        mouseDownEventConsumer.accept(new Event(point.getX(), point.getY(), false));
    }

    @Override
    public void onNodeMouseMove(final NodeMouseMoveEvent event) {
        if (!isOverConnector(event.getX(), event.getY())) {
            destroyTransientControlHandle();
            return;
        }

        final Point2DArray linePoints = getConnector().getLine().getPoint2DArray();
        final Point2D closestPoint = Geometry.findClosestPointOnLine(event.getX(), event.getY(), linePoints);
        if (closestPoint == null) {
            destroyTransientControlHandle();
            return;
        }

        //check it the closest point is overlapping or it is very close to any line point
        for (int i = 0; i < linePoints.size() - 1; i++) {
            Point2D point = linePoints.get(i);
            if (Geometry.distance(closestPoint.getX(), closestPoint.getY(), point.getX(), point.getY()) < SELECTION_OFFSET) {
                destroyTransientControlHandle();
                return;
            }
        }

        //check if the closest point is too far from the current mouse location
        double distance = Geometry.distance(event.getX(), event.getY(), closestPoint.getX(), closestPoint.getY());
        if (distance > SELECTION_OFFSET) {
            destroyTransientControlHandle();
            return;
        }

        Shape<?> transientControlHandle = getControl().getTransientControlHandle();
        if (transientControlHandle == null) {
            transientControlHandle = getControl().createTransientControlHandle(new Consumer<Point2D>() {
                @Override
                public void accept(Point2D point2D) {
                    addControlPoint(point2D);
                }
            });
        }

        //setting current position
        transientControlHandle.setX(closestPoint.getX()).setY(closestPoint.getY());

        batchConnector();
    }

    private void destroyTransientControlHandle() {
        //release the token of the transient control handle, concurrency between connectors
        transientControlHandleToken.set(false);
        hasTransientControlHandleToken = false;
        getControl().destroyTransientControlHandle();
    }

    private boolean isOverConnector(double x, double y) {
        //check and get the token of the transient control handle, concurrency between connectors
        if(transientControlHandleToken.compareAndSet(false, true)){
            hasTransientControlHandleToken = true;
        }
        //skip in case the token was not gotten
        if (!hasTransientControlHandleToken) {
            return false;
        }
        //check if the connector is already selected
        if (getControl().areControlPointsVisible()) {
            return true;
        }
        //check if the mouse is within the connector bounding box area
        final BoundingPoints computedBoundingPoints = getConnector().getLine().getComputedBoundingPoints();
        final Point2D a0 = new Point2D(computedBoundingPoints.getBoundingBox().getX(), computedBoundingPoints.getBoundingBox().getY());
        final Point2D a1 = new Point2D(computedBoundingPoints.getBoundingBox().getMaxX(), computedBoundingPoints.getBoundingBox().getMaxY());
        return Geometry.intersectPointWithinBounding(new Point2D(x, y), a0, a1);
    }

    private void batchConnector() {
        m_connector.getLine().getLayer().batch();
    }
}