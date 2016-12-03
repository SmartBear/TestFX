/*
 * Copyright 2013-2014 SmartBear Software
 * Copyright 2014-2015 The TestFX Contributors
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence"); You may
 * not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the Licence for the
 * specific language governing permissions and limitations under the Licence.
 */
package org.testfx.robot;

import org.testfx.service.query.PointQuery;

public interface MoveRobot {

    /**
     * Moves mouse to {@link PointQuery#query()}.
     *
     * @param pointQuery the pointQuery
     */
    public void moveTo(PointQuery pointQuery);

    /**
     * Moves mouse from current location to new location by {@code x} on the horizontal axis and by {@code y} on
     * the vertical axis.
     *
     * @param x the amount by which to move the mouse horizontally
     * @param y the amount by which to move the mouse vertically
     */
    public void moveBy(double x,
                       double y);

}
