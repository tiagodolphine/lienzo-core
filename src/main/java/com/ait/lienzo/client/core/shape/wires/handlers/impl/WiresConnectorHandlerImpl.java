package com.ait.lienzo.client.core.shape.wires.handlers.impl;

import java.util.ArrayList;
import java.util.Collection;

import com.ait.lienzo.client.core.event.NodeDragEndEvent;
import com.ait.lienzo.client.core.event.NodeDragMoveEvent;
import com.ait.lienzo.client.core.event.NodeDragStartEvent;
import com.ait.lienzo.client.core.event.NodeMouseClickEvent;
import com.ait.lienzo.client.core.event.NodeMouseDownEvent;
import com.ait.lienzo.client.core.event.NodeMouseDownHandler;
import com.ait.lienzo.client.core.event.NodeMouseEnterEvent;
import com.ait.lienzo.client.core.event.NodeMouseExitEvent;
import com.ait.lienzo.client.core.event.NodeMouseMoveEvent;
import com.ait.lienzo.client.core.event.NodeMouseMoveHandler;
import com.ait.lienzo.client.core.shape.AbstractMultiPointShape.DefaultMultiPointShapeHandleFactory;
import com.ait.lienzo.client.core.shape.Circle;
import com.ait.lienzo.client.core.shape.Shape;
import com.ait.lienzo.client.core.shape.decorator.IShapeDecorator;
import com.ait.lienzo.client.core.shape.decorator.PointHandleDecorator;
import com.ait.lienzo.client.core.shape.wires.WiresConnector;
import com.ait.lienzo.client.core.shape.wires.WiresManager;
import com.ait.lienzo.client.core.shape.wires.handlers.WiresConnectorControl;
import com.ait.lienzo.client.core.shape.wires.handlers.WiresConnectorHandler;
import com.ait.tooling.common.api.java.util.function.Consumer;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;

public class WiresConnectorHandlerImpl implements WiresConnectorHandler {

    private final WiresConnector m_connector;
    private final WiresManager m_wiresManager;
    private final Consumer<Event> clickEventConsumer;
    private final Consumer<Event> mouseDownEventConsumer;
    private Timer clickTimer;
    private Event event;
    private PointHandleDecorator pointHandleDecorator;

    private Shape<?> transientControlHandle;
    private Timer destroyTransientControlHandleTimer;
    private final Collection<HandlerRegistration> transientControlHandleRegistrations = new ArrayList<>();

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
                                             new PointHandleDecorator(),
                                             consumers.switchVisibility(),
                                             consumers.addControlPoint());
    }

    public WiresConnectorHandlerImpl(final WiresConnector connector,
                                     final WiresManager wiresManager,
                                     final PointHandleDecorator pointHandleDecorator,
                                     final Consumer<Event> clickEventConsumer,
                                     final Consumer<Event> mouseDownEventConsumer) {
        this(connector, wiresManager, pointHandleDecorator, clickEventConsumer, mouseDownEventConsumer, null);

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
                              final PointHandleDecorator pointHandleDecorator,
                              final Consumer<Event> clickEventConsumer,
                              final Consumer<Event> mouseDownEventConsumer,
                              final Timer clickTimer) {
        this.m_connector = connector;
        this.m_wiresManager = wiresManager;
        this.pointHandleDecorator = pointHandleDecorator;
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
    public void onNodeMouseEnter(NodeMouseEnterEvent event) {
        cancelDestroyTransientControlHandleTimer();
    }

    private void cancelDestroyTransientControlHandleTimer() {
        if(destroyTransientControlHandleTimer != null) {
            destroyTransientControlHandleTimer.cancel();
        }
    }

    private Shape<?> createTransientControlHandle() {
        final Shape<?> pointHandleShape = new Circle(DefaultMultiPointShapeHandleFactory.R0);
        pointHandleDecorator.decorate(pointHandleShape, IShapeDecorator.ShapeState.INVALID);
        //increase the selection area to make it easier do drag
        pointHandleShape.getAttributes().setSelectionBoundsOffset(30);
        pointHandleShape.getAttributes().setSelectionStrokeOffset(30);
        pointHandleShape.setFillBoundsForSelection(true);

        //adding the handlers on the transient control handle
        transientControlHandleRegistrations.add(pointHandleShape.addNodeMouseDownHandler(new NodeMouseDownHandler() {
            @Override
            public void onNodeMouseDown(NodeMouseDownEvent event) {
                if(getControl().areControlPointsVisible()) {
                    mouseDownEventConsumer.accept(new Event(event.getX(), event.getY(), event.isShiftKeyDown()));
                    destroyTransientControlHandle();
                }
            }
        }));

        transientControlHandleRegistrations.add(pointHandleShape.addNodeMouseMoveHandler(new NodeMouseMoveHandler() {
            @Override
            public void onNodeMouseMove(NodeMouseMoveEvent event) {
                cancelDestroyTransientControlHandleTimer();
            }
        }));

        //same handlers as the connector
        transientControlHandleRegistrations.add(pointHandleShape.addNodeMouseEnterHandler(this));
        transientControlHandleRegistrations.add(pointHandleShape.addNodeMouseExitHandler(this));

        return pointHandleShape;
    }

    @Override
    public void onNodeMouseExit(NodeMouseExitEvent event) {
        if(transientControlHandle == null){
            return;
        }

        cancelDestroyTransientControlHandleTimer();

        destroyTransientControlHandleTimer = new Timer() {
                @Override
                public void run() {
                    destroyTransientControlHandle();
                    batchConnector();
                }
            };

        destroyTransientControlHandleTimer.schedule(5);
    }

    @Override
    public void onNodeMouseMove(NodeMouseMoveEvent event) {
        if (!getControl().areControlPointsVisible()) {
            return;
        }

        if (destroyTransientControlHandleTimer != null) {
            destroyTransientControlHandleTimer.cancel();
        }

        if (transientControlHandle == null) {
            transientControlHandle = createTransientControlHandle();
        }

        if(transientControlHandle.getParent() == null) {
            //add the shape on the connector line
            getConnector().getLine().getLayer().add(transientControlHandle);
        }

        //setting current position
        transientControlHandle.setX(event.getX()).setY(event.getY());

        batchConnector();
    }

    private void destroyTransientControlHandle() {
        if (transientControlHandle != null && getControl().areControlPointsVisible()) {
            transientControlHandle.removeFromParent();
            transientControlHandle = null;

            for (HandlerRegistration registration : transientControlHandleRegistrations) {
                registration.removeHandler();
            }
            transientControlHandleRegistrations.clear();
        }
    }

    private void batchConnector() {
        getConnector().getLine().getLayer().batch();
    }

    protected Shape<?> getTransientControlHandle() {
        return transientControlHandle;
    }

}