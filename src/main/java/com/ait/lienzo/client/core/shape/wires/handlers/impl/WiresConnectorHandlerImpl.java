package com.ait.lienzo.client.core.shape.wires.handlers.impl;

import com.ait.lienzo.client.core.event.NodeDragEndEvent;
import com.ait.lienzo.client.core.event.NodeDragMoveEvent;
import com.ait.lienzo.client.core.event.NodeDragStartEvent;
import com.ait.lienzo.client.core.event.NodeMouseClickEvent;
import com.ait.lienzo.client.core.event.NodeMouseDownEvent;
import com.ait.lienzo.client.core.event.NodeMouseMoveEvent;
import com.ait.lienzo.client.core.shape.Shape;
import com.ait.lienzo.client.core.shape.wires.WiresConnector;
import com.ait.lienzo.client.core.shape.wires.WiresManager;
import com.ait.lienzo.client.core.shape.wires.handlers.WiresConnectorControl;
import com.ait.lienzo.client.core.shape.wires.handlers.WiresConnectorHandler;
import com.ait.lienzo.client.core.types.Point2D;
import com.ait.lienzo.client.core.types.Point2DArray;
import com.ait.lienzo.client.core.util.Geometry;
import com.ait.tooling.common.api.java.util.function.Consumer;
import com.google.gwt.user.client.Timer;

public class WiresConnectorHandlerImpl implements WiresConnectorHandler {

    private final WiresConnector m_connector;
    private final WiresManager m_wiresManager;
    private final Consumer<Event> clickEventConsumer;
    private final Consumer<Event> mouseDownEventConsumer;
    private Timer clickTimer;
    private Event event;

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
        checkRunningTimer(clickTimer);
        setEvent(event.getX(), event.getY(), event.isShiftKeyDown());
        clickTimer.schedule(50);
    }

    private void checkRunningTimer(Timer timer) {
        if (timer.isRunning()) {
            timer.cancel();
        }
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

    @Override
    public void onNodeMouseDown(NodeMouseDownEvent event) {
        addControlPoint(new Point2D(event.getX(), event.getY()));
    }

    private void addControlPoint(Point2D point) {
        getControl().destroyTransientControlHandle();
        if(getControl().areControlPointsVisible()) {
            mouseDownEventConsumer.accept(new Event(point.getX(), point.getY(), false));
        }
    }

    @Override
    public void onNodeMouseMove(NodeMouseMoveEvent event) {
        if (!getControl().areControlPointsVisible()) {
            return;
        }

        Point2D nearestMouse = Geometry.findClosestPointOnLine(event.getX(), event.getY(), getConnector().getLine().getPoint2DArray(), 10);
        if(nearestMouse == null){
            getControl().destroyTransientControlHandle();
            return;
        }

        double distance = Geometry.distance(event.getX(), event.getY(), nearestMouse.getX(), nearestMouse.getY());
        if(distance > 30){
            getControl().destroyTransientControlHandle();
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
        transientControlHandle.setX(nearestMouse.getX()).setY(nearestMouse.getY());

        batchConnector();
    }

    private void batchConnector() {
        m_connector.getLine().getLayer().batch();
    }
}