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

package com.ait.lienzo.client.core.shape.wires.handlers;

import com.ait.lienzo.client.core.shape.wires.PickerPart;
import com.ait.lienzo.client.core.shape.wires.WiresContainer;

public interface WiresParentPickerControl extends WiresShapeLocationControl
{
    public WiresContainer getParent();

    public PickerPart.ShapePart getParentShapePart();

    public Index getIndex();

    /**
     * An indexed shape part picker for the given control's layer.
     */
    public interface Index {
        /**
         * Excludes the given shape from the resulting index.
         */
        public void exclude(WiresContainer shape);

        /**
         * Returns the picker part for the shape located at the given coordinates, if any.
         */
        PickerPart findShapeAt(int x, int y);

        /**
         * Clears the index.
         */
        public void clear();
    }
}
