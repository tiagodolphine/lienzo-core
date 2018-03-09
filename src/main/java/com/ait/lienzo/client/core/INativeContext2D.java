/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
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

package com.ait.lienzo.client.core;

import com.ait.lienzo.client.core.types.ImageData;
import com.ait.lienzo.client.core.types.LinearGradient;
import com.ait.lienzo.client.core.types.PathPartList;
import com.ait.lienzo.client.core.types.PatternGradient;
import com.ait.lienzo.client.core.types.RadialGradient;
import com.ait.lienzo.client.core.types.Shadow;
import com.ait.lienzo.client.core.types.TextMetrics;
import com.ait.lienzo.client.core.types.Transform;
import com.ait.tooling.nativetools.client.collection.NFastDoubleArrayJSO;
import com.google.gwt.dom.client.Element;

public interface INativeContext2D {

    void initDeviceRatio();

    void saveGroup();

    void restoreGroup();

    void save()
    /*-{
		this.save();
    }-*/;

    void restore()
    /*-{
		this.restore();
    }-*/;

    void beginPath()
    /*-{
		this.beginPath();
    }-*/;

    void closePath()
    /*-{
		this.closePath();
    }-*/;

    void moveTo(double x, double y)
    /*-{
		this.moveTo(x, y);
    }-*/;

    void lineTo(double x, double y)
    /*-{
		this.lineTo(x, y);
    }-*/;

    void setGlobalCompositeOperation(String operation)
    /*-{
		this.globalCompositeOperation = operation || "source-over";
    }-*/;

    void setLineCap(String lineCap)
    /*-{
		this.lineCap = lineCap || "butt";
    }-*/;

    void setLineJoin(String lineJoin)
    /*-{
		this.lineJoin = lineJoin || "miter";
    }-*/;

    void quadraticCurveTo(double cpx, double cpy, double x, double y)
    /*-{
		this.quadraticCurveTo(cpx, cpy, x, y);
    }-*/;

    void arc(double x, double y, double radius, double startAngle, double endAngle)
    /*-{
		this.arc(x, y, radius, startAngle, endAngle, false);
    }-*/;

    void arc(double x, double y, double radius, double startAngle, double endAngle, boolean antiClockwise)
    /*-{
		this.arc(x, y, radius, startAngle, endAngle, antiClockwise);
    }-*/;

    void ellipse(double x, double y, double rx, double ry, double ro, double sa, double ea, boolean ac)
    /*-{
		this.ellipse(x, y, rx, ry, ro, sa, ea, ac);
    }-*/;

    void ellipse(double x, double y, double rx, double ry, double ro, double sa, double ea)
    /*-{
		this.ellipse(x, y, rx, ry, ro, sa, ea, false);
    }-*/;

    void arcTo(double x1, double y1, double x2, double y2, double radius)
    /*-{
		this.arcTo(x1, y1, x2, y2, radius);
    }-*/;

    void bezierCurveTo(double cp1x, double cp1y, double cp2x, double cp2y, double x, double y)
    /*-{
		this.bezierCurveTo(cp1x, cp1y, cp2x, cp2y, x, y);
    }-*/;

    void clearRect(double x, double y, double w, double h)
    /*-{
		if ((w <= 0) || (h <= 0)) {
			return;
		}
		this.clearRect(x, y, w, h);
    }-*/;

    void clip()
    /*-{
		this.clip();
    }-*/;

    void fill()
    /*-{
		this.fill();
    }-*/;

    void stroke()
    /*-{
		this.stroke();
    }-*/;

    void fillRect(double x, double y, double w, double h)
    /*-{
		if ((w <= 0) || (h <= 0)) {
			return;
		}
		this.fillRect(x, y, w, h);
    }-*/;

    void fillText(String text, double x, double y)
    /*-{
		this.fillText(text, x, y);
    }-*/;

    void fillTextWithGradient(String text, double x, double y, double sx, double sy, double ex, double ey, String color)
    /*-{
        var grad = this.createLinearGradient(sx, sy, ex, ey);

        grad.addColorStop(0, color);

        grad.addColorStop(1, color);

        this.fillStyle = grad;

        this.fillText(text, x, y);
    }-*/;

    void fillText(String text, double x, double y, double maxWidth)
    /*-{
		this.fillText(text, x, y, maxWidth);
    }-*/;

    void setFillColor(String fill)
    /*-{
		this.fillStyle = fill;
    }-*/;

    void rect(double x, double y, double w, double h)
    /*-{
		if ((w <= 0) || (h <= 0)) {
			return;
		}
		this.rect(x, y, w, h);
    }-*/;

    void rotate(double angle)
    /*-{
		this.rotate(angle);
    }-*/;

    void scale(double sx, double sy)
    /*-{
		this.scale(sx*scalingRatio, sy*scalingRatio);
    }-*/;

    void setStrokeColor(String color)
    /*-{
		this.strokeStyle = color;
    }-*/;

    void setStrokeWidth(double width)
    /*-{
		this.lineWidth = width;
    }-*/;

    void setImageSmoothingEnabled(boolean enabled)
    /*-{
		this.imageSmoothingEnabled = enabled;
    }-*/;

    void setFillGradient(LinearGradient.LinearGradientJSO grad)
    /*-{
		if (grad) {
			var that = this.createLinearGradient(grad.start.x, grad.start.y,
					grad.end.x, grad.end.y);

			var list = grad.colorStops;

			for (i = 0; i < list.length; i++) {
				that.addColorStop(list[i].stop, list[i].color);
			}
			this.fillStyle = that;
		} else {
			this.fillStyle = null;
		}
    }-*/;

    void setFillGradient(PatternGradient.PatternGradientJSO grad)
    /*-{
		if ((grad) && ((typeof grad.image) === 'function')) {
			var elem = grad.image();
			if (elem) {
				this.fillStyle = this.createPattern(elem, grad.repeat);
			} else {
				this.fillStyle = null;
			}
		} else {
			this.fillStyle = null;
		}
    }-*/;

    void setFillGradient(RadialGradient.RadialGradientJSO grad)
    /*-{
		if (grad) {
			var that = this.createRadialGradient(grad.start.x, grad.start.y,
					grad.start.radius, grad.end.x, grad.end.y, grad.end.radius);

			var list = grad.colorStops;

			for (i = 0; i < list.length; i++) {
				that.addColorStop(list[i].stop, list[i].color);
			}
			this.fillStyle = that;
		} else {
			this.fillStyle = null;
		}
    }-*/;

    void transform(Transform.TransformJSO jso)
    /*-{
		if (jso) {
			this.transform(jso[0], jso[1], jso[2], jso[3], jso[4], jso[5]);
		}
    }-*/;

    void transform(double d0, double d1, double d2, double d3, double d4, double d5)
    /*-{
		this.transform(d0, d1, d2, d3, d4, d5);
    }-*/;

    void setTransform(Transform.TransformJSO jso)
    /*-{
		if (jso) {
			this.setTransform(jso[0], jso[1], jso[2], jso[3], jso[4], jso[5]);
		}
    }-*/;

    void setTransform(double d0, double d1, double d2, double d3, double d4, double d5)
    /*-{
		this.setTransform(d0, d1, d2, d3, d4, d5);
    }-*/;

    void setToIdentityTransform()
    /*-{
		this.setTransform(1, 0, 0, 1, 0, 0);
    }-*/;

    void setTextFont(String font)
    /*-{
		this.font = font;
    }-*/;

    void setTextBaseline(String baseline)
    /*-{
		this.textBaseline = baseline || "alphabetic";
    }-*/;

    void setTextAlign(String align)
    /*-{
		this.textAlign = align || "start";
    }-*/;

    void strokeText(String text, double x, double y)
    /*-{
		this.strokeText(text, x, y);
    }-*/;

    void setGlobalAlpha(double alpha)
    /*-{
		this.globalAlpha = alpha;
    }-*/;

    void translate(double x, double y)
    /*-{
		this.translate(x, y);
    }-*/;

    void setShadow(Shadow.ShadowJSO shadow)
    /*-{
		if (shadow) {
			this.shadowColor = shadow.color;
			this.shadowOffsetX = shadow.offset.x;
			this.shadowOffsetY = shadow.offset.y;
			this.shadowBlur = shadow.blur;
		} else {
			this.shadowColor = "transparent";
			this.shadowOffsetX = 0;
			this.shadowOffsetY = 0;
			this.shadowBlur = 0;
		}
    }-*/;

    boolean isSupported(String feature)
    /*-{
		return (this[feature] !== undefined);
    }-*/;

    boolean isPointInPath(double x, double y)
    /*-{
		return this.isPointInPath(x, y);
    }-*/;

    ImageData getImageData(double x, double y, double width, double height)
    /*-{
		return this.getImageData(x, y, width, height);
    }-*/;

    ImageData createImageData(double width, double height)
    /*-{
		return this.createImageData(width, height);
    }-*/;

    ImageData createImageData(ImageData data)
    /*-{
		return this.createImageData(data);
    }-*/;

    void putImageData(ImageData imageData, double x, double y)
    /*-{
		this.putImageData(imageData, x, y);
    }-*/;

    void putImageData(ImageData imageData, double x, double y, double dx, double dy, double dw, double dh)
    /*-{
		if ((dw <= 0) || (dh <= 0)) {
			return;
		}
		this.putImageData(imageData, x, y, dx, dy, dw, dh);
    }-*/;

    TextMetrics measureText(String text)
    /*-{
		return this.measureText(text);
    }-*/;

    void drawImage(Element image, double x, double y)
    /*-{
		this.drawImage(image, x, y);
    }-*/;

    void drawImage(Element image, double x, double y, double w, double h)
    /*-{
		if ((w <= 0) || (h <= 0)) {
			return;
		}
		this.drawImage(image, x, y, w, h);
    }-*/;

    void drawImage(Element image, double sx, double sy, double sw, double sh, double x, double y, double w, double h)
    /*-{
		if ((w <= 0) || (h <= 0)) {
			return;
		}
		if ((sw <= 0) || (sh <= 0)) {
			return;
		}
		this.drawImage(image, sx, sy, sw, sh, x, y, w, h);
    }-*/;

    void resetClip()
    /*-{
		this.resetClip();
    }-*/;

    void setMiterLimit(double limit)
    /*-{
		this.miterLimit = limit;
    }-*/;

    void setLineDash(NFastDoubleArrayJSO dashes)
    /*-{
		this.setLineDash(dashes || []);
    }-*/;

    void setLineDashOffset(double offset)
    /*-{
		this.setLineDashOffset(offset);
    }-*/;

    double getBackingStorePixelRatio()
    /*-{
		return this.backingStorePixelRatio || 1;
    }-*/;

    boolean path(PathPartList.PathPartListJSO list)
    /*-{
		if (!list) {
			return false;
		}
		var leng = list.length;
		if (leng < 1) {
			return false;
		}
		var indx = 0;
		var fill = false;
		this.beginPath();
		while (indx < leng) {
			var e = list[indx++];
			var p = e.points;
			switch (e.command) {
			case 1:
				this.lineTo(p[0], p[1]);
				break;
			case 2:
				this.moveTo(p[0], p[1]);
				break;
			case 3:
				this.bezierCurveTo(p[0], p[1], p[2], p[3], p[4], p[5]);
				break;
			case 4:
				this.quadraticCurveTo(p[0], p[1], p[2], p[3]);
				break;
			case 5:
				this.ellipse(p[0], p[1], p[2], p[3], p[6], p[4], p[4] + p[5],
						(1 - p[7]) > 0);
				break;
			case 6:
				this.closePath();
				fill = true;
				break;
			case 7:
				this.arcTo(p[0], p[1], p[2], p[3], p[4]);
				break;
			}
		}
		return fill;
    }-*/;

    boolean clip(PathPartList.PathPartListJSO list)
    /*-{
		if (!list) {
			return false;
		}
		var leng = list.length;
		if (leng < 1) {
			return false;
		}
		var indx = 0;
		var fill = false;
		while (indx < leng) {
			var e = list[indx++];
			var p = e.points;
			switch (e.command) {
			case 1:
				this.lineTo(p[0], p[1]);
				break;
			case 2:
				this.moveTo(p[0], p[1]);
				break;
			case 3:
				this.bezierCurveTo(p[0], p[1], p[2], p[3], p[4], p[5]);
				break;
			case 4:
				this.quadraticCurveTo(p[0], p[1], p[2], p[3]);
				break;
			case 5:
				this.ellipse(p[0], p[1], p[2], p[3], p[6], p[4], p[4] + p[5],
						(1 - p[7]) > 0);
				break;
			case 6:
				return true;
				break;
			case 7:
				this.arcTo(p[0], p[1], p[2], p[3], p[4]);
				break;
			}
		}
		return fill;
    }-*/;

    void fill(Path2D.NativePath2D path)
    /*-{
		if (path) {
			this.fill(path);
		}
    }-*/;

    void stroke(Path2D.NativePath2D path)
    /*-{
		if (path) {
			this.stroke(path);
		}
    }-*/;

    void clip(Path2D.NativePath2D path)
    /*-{
		if (path) {
			this.clip(path);
		}
    }-*/;

    Path2D.NativePath2D getCurrentPath()
    /*-{
		return this.currentPath || null;
    }-*/;

    void setCurrentPath(Path2D.NativePath2D path)
    /*-{
		if (path) {
			this.currentPath = path;
		}
    }-*/;
}
