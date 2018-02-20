package com.ait.lienzo.client.core.shape.wires.handlers.impl;

import java.util.Collection;
import java.util.HashSet;

import com.ait.lienzo.client.core.shape.wires.IDockingAcceptor;
import com.ait.lienzo.client.core.shape.wires.MagnetManager;
import com.ait.lienzo.client.core.shape.wires.PickerPart;
import com.ait.lienzo.client.core.shape.wires.WiresContainer;
import com.ait.lienzo.client.core.shape.wires.WiresLayer;
import com.ait.lienzo.client.core.shape.wires.WiresMagnet;
import com.ait.lienzo.client.core.shape.wires.WiresManager;
import com.ait.lienzo.client.core.shape.wires.WiresShape;
import com.ait.lienzo.client.core.shape.wires.event.WiresResizeStepEvent;
import com.ait.lienzo.client.core.shape.wires.event.WiresResizeStepHandler;
import com.ait.lienzo.client.core.shape.wires.handlers.WiresDockingControl;
import com.ait.lienzo.client.core.shape.wires.picker.ColorMapBackedPicker;
import com.ait.lienzo.client.core.types.BoundingBox;
import com.ait.lienzo.client.core.types.Point2D;
import com.ait.lienzo.client.core.util.Geometry;
import com.google.gwt.event.shared.HandlerRegistration;

import static java.lang.Math.abs;

public class WiresDockingControlImpl extends AbstractWiresParentPickerControl
        implements WiresDockingControl {

    private Point2D initialPathLocation;
    private Point2D intersection;
    private Point2D dockPosition;
    private final Collection<HandlerRegistration> handlerRegistrations = new HashSet<>();


    public WiresDockingControlImpl(WiresShape shape,
                                   ColorMapBackedPicker.PickerOptions pickerOptions) {
        super(shape,
              pickerOptions);
    }

    public WiresDockingControlImpl(WiresParentPickerControlImpl parentPickerControl) {
        super(parentPickerControl);
    }

    @Override
    protected void beforeMoveStart(double x,
                                   double y) {
        super.beforeMoveStart(x,
                              y);
        initialPathLocation = getShape().getPath().getComputedLocation();
    }

    @Override
    public WiresDockingControl setEnabled(final boolean enabled) {
        if (enabled) {
            enable();
        } else {
            disable();
        }
        return this;
    }

    @Override
    protected void afterMoveStart(double x,
                                  double y) {
        super.afterMoveStart(x,
                             y);
        final WiresShape shape = getShape();
        if (null != shape.getDockedTo()) {
            shape.setDockedTo(null);
        }
    }

    @Override
    protected boolean afterMove(double dx,
                                double dy) {
        super.afterMove(dx,
                        dy);
        intersection = null;
        if (isAllow()) {
            final Point2D location = getParentPickerControl().getCurrentLocation();
            final Point2D absLoc = getParent().getComputedLocation();// convert to local xy of the path
            this.intersection =
                    Geometry.findIntersection((int) (location.getX() - absLoc.getX()),
                                              (int) (location.getY() - absLoc.getY()),
                                              ((WiresShape) getParent()).getPath());
        }
        return null != this.intersection;
    }

    @Override
    protected boolean afterMoveComplete() {
        super.afterMoveComplete();
        return true;
    }

    @Override
    public void clear() {
        initialPathLocation = null;
        intersection = null;
        removeHandlers();
    }

    private void removeHandlers() {
        for (HandlerRegistration registration: handlerRegistrations) {
            registration.removeHandler();
        }
        handlerRegistrations.clear();
    }

    @Override
    public Point2D getAdjust() {
        if (isEnabled() && intersection != null) {
            dockPosition = calculateCandidateLocation(getShape(), getParent());
            final Point2D absLoc = getParent().getComputedLocation();
            return new Point2D(absLoc.getX() + dockPosition.getX() - initialPathLocation.getX(),
                               absLoc.getY() + dockPosition.getY() - initialPathLocation.getY());
        }
        return new Point2D(0, 0);
    }

    @Override
    public boolean isAllow() {
        final WiresLayer m_layer = getParentPickerControl().getWiresLayer();
        final WiresManager wiresManager = m_layer.getWiresManager();
        final IDockingAcceptor dockingAcceptor = wiresManager.getDockingAcceptor();
        return !isEnabled() ||
                null != getParent() &&
                        null != getParentShapePart() &&
                        getParent() instanceof WiresShape &&
                        getParentShapePart() == PickerPart.ShapePart.BORDER &&
                        (dockingAcceptor.dockingAllowed(getParent(),
                                                        getShape()));
    }

    @Override
    public boolean accept() {
        return !isEnabled() || (isAllow() && _isAccept());
    }

    @Override
    public Point2D getCandidateLocation() {
        return dockPosition;
    }

    @Override
    public void execute() {
        if (isEnabled()) {
            dock(getShape(),
                 getParent(),
                 getCandidateLocation());
        }
    }

    @Override
    public void reset() {
        if (isEnabled()) {
            if (getParentPickerControl().getShapeLocationControl().isStartDocked() &&
                    getParentPickerControl().getInitialParent() != getShape().getParent()) {
                dock(getShape(),
                     getParentPickerControl().getInitialParent(),
                     getParentPickerControl().getShapeLocationControl().getShapeInitialLocation());
            }
        }
    }

    private boolean _isAccept() {
        final WiresLayer m_layer = getParentPickerControl().getWiresLayer();
        final WiresManager wiresManager = m_layer.getWiresManager();
        final IDockingAcceptor dockingAcceptor = wiresManager.getDockingAcceptor();
        return null != getParent() &&
                null != getParentShapePart()
                && getParentShapePart() == PickerPart.ShapePart.BORDER
                && dockingAcceptor.acceptDocking(getParent(),
                                                 getShape());
    }

    private Point2D calculateCandidateLocation(WiresShape shape, WiresContainer parent) {
        return calculateCandidateLocation(shape, getCloserMagnet(shape, parent));
    }

    private Point2D calculateCandidateLocation(WiresShape shape, WiresMagnet shapeMagnet) {
        final Point2D location = new Point2D(shapeMagnet.getX(), shapeMagnet.getY());
        final BoundingBox box = shape.getPath().getBoundingBox();
        final double newX = location.getX() - (box.getWidth() / 2);
        final double newY = location.getY() - (box.getHeight() / 2);
        return new Point2D(newX, newY);
    }

    private WiresMagnet getCloserMagnet(WiresShape shape,
                                        WiresContainer parent) {
        final WiresShape parentShape = (WiresShape) parent;
        final MagnetManager.Magnets magnets = parentShape.getMagnets();
        final double shapeWidth = shape.getPath().getBoundingBox().getMaxX();
        final double shapeHeight = shape.getPath().getBoundingBox().getMaxY();

        int magnetIndex = -1;
        Double minDistance = null;
        //not considering the zero magnet, that is the center.
        for (int i = 1; i < magnets.size(); i++) {
            WiresMagnet magnet = magnets.getMagnet(i);
            final Point2D shapeLocation = shape.getComputedLocation();
            final double magnetX = magnet.getControl().getLocation().getX();
            final double magnetY = magnet.getControl().getLocation().getY();
            double distance = abs((abs(shapeLocation.getX()) - abs(magnetX)))
                    + abs((abs(shapeLocation.getY()) - abs(magnetY)));
            double distanceEnd = abs((abs(shapeLocation.getX() + shapeWidth) - abs(magnetX)))
                    + abs((abs(shapeLocation.getY() + shapeHeight) - abs(magnetY)));
            //getting shorter distance
            distance = (distance < distanceEnd ? distance : distanceEnd);
            if (minDistance == null || distance < minDistance) {
                minDistance = distance;
                magnetIndex = i;
            }
        }
        return magnets.getMagnet(magnetIndex);
    }

    public void dock(final WiresShape shape,
                     final WiresContainer parent,
                     final Point2D location) {
        shape.setLocation(location);
        shape.removeFromParent();
        parent.add(shape);
        shape.setDockedTo(parent);

        final WiresShape parentWireShape = (WiresShape) parent;
        final WiresMagnet magnet = getCloserMagnet(shape, parent);

        //adjust the location if necessary
        final Point2D adjust = calculateCandidateLocation(shape, magnet);
        if (!location.equals(adjust)) {
            shape.setLocation(adjust);
        }
        shape.shapeMoved();

        //recalculate location during shape resizing
        handlerRegistrations.add(parentWireShape.addWiresResizeStepHandler(new WiresResizeStepHandler() {
            @Override
            public void onShapeResizeStep(WiresResizeStepEvent event) {
                shape.setLocation(calculateCandidateLocation(shape, magnet));
            }
        }));
    }
}