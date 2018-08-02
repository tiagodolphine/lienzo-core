package com.ait.lienzo.client.core.shape.wires.handlers.impl;

import com.ait.lienzo.client.core.event.NodeDragEndEvent;
import com.ait.lienzo.client.core.event.NodeDragMoveEvent;
import com.ait.lienzo.client.core.event.NodeDragStartEvent;
import com.ait.lienzo.client.core.event.NodeMouseClickEvent;
import com.ait.lienzo.client.core.event.NodeMouseDoubleClickEvent;
import com.ait.lienzo.client.core.event.NodeMouseDownEvent;
import com.ait.lienzo.client.core.shape.wires.WiresConnector;
import com.ait.lienzo.client.core.shape.wires.WiresManager;
import com.ait.lienzo.client.core.shape.wires.handlers.WiresConnectorControl;
import com.ait.lienzo.client.core.shape.wires.handlers.WiresConnectorHandler;
import com.ait.tooling.common.api.java.util.function.Consumer;
import com.google.gwt.user.client.Timer;

public class WiresConnectorHandlerImpl implements WiresConnectorHandler {

    private final WiresConnector m_connector;
    private final WiresManager m_wiresManager;
    private final Consumer<Event> clickEventConsumer;
    private final Consumer<Event> doubleClickEventConsumer;
    private final Consumer<Event> mouseDownEventConsumer;
    private Timer clickTimer;
    private Timer mouseDownTimer;
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
                                             consumers.addControlPoint(),
                                             consumers.addControlPoint());
    }

    public WiresConnectorHandlerImpl(final WiresConnector connector,
                                     final WiresManager wiresManager,
                                     final Consumer<Event> clickEventConsumer,
                                     final Consumer<Event> doubleClickEventConsumer,
                                     final Consumer<Event> mouseDownEventConsumer) {
        this(connector, wiresManager, clickEventConsumer, doubleClickEventConsumer, mouseDownEventConsumer, null, null);

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

        this.mouseDownTimer = new Timer() {
            @Override
            public void run() {
                mouseDownEventConsumer.accept(event);
                event = null;
            }
        };
    }

    WiresConnectorHandlerImpl(final WiresConnector connector,
                              final WiresManager wiresManager,
                              final Consumer<Event> clickEventConsumer,
                              final Consumer<Event> doubleClickEventConsumer,
                              final Consumer<Event> mouseDownEventConsumer,
                              final Timer clickTimer,
                              final Timer mouseDownTimer) {
        this.m_connector = connector;
        this.m_wiresManager = wiresManager;
        this.clickEventConsumer = clickEventConsumer;
        this.doubleClickEventConsumer = doubleClickEventConsumer;
        this.mouseDownEventConsumer = mouseDownEventConsumer;
        this.clickTimer = clickTimer;
        this.mouseDownTimer = mouseDownTimer;
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
        checkRunningTimer(mouseDownTimer);
        setEvent(event.getX(), event.getY(), event.isShiftKeyDown());
        clickTimer.schedule(50);
    }

    @Override
    public void onNodeMouseDown(final NodeMouseDownEvent event) {
        checkRunningTimer(clickTimer);
        checkRunningTimer(mouseDownTimer);
        setEvent(event.getX(), event.getY(), event.isShiftKeyDown());
        mouseDownTimer.schedule(150);
    }

    private void checkRunningTimer(Timer timer) {
        if (timer.isRunning()) {
            timer.cancel();
        }
    }

    @Override
    public void onNodeMouseDoubleClick(final NodeMouseDoubleClickEvent event) {
        clickTimer.cancel();
        doubleClickEventConsumer.accept(setEvent(event.getX(), event.getY(), event.isShiftKeyDown()));
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

}