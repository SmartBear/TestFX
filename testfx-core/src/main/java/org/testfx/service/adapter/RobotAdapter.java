/*
 * Copyright 2013-2014 SmartBear Software
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
 * except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the Licence for the specific language governing permissions
 * and limitations under the Licence.
 */

package org.testfx.service.adapter;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;

public interface RobotAdapter<T> {

    // ROBOT.

    public void robotCreate() ;

    public void robotDestroy();

    public T getRobotInstance();

    // KEY.

    public void keyPress(KeyCode key);

    public void keyRelease(KeyCode key);

    // MOUSE.

    public Point2D getMouseLocation();

    public void mouseMove(Point2D location);

    public void mousePress(MouseButton button);

    public void mouseRelease(MouseButton button);

    public void mouseWheel(int wheelAmount);

    // CAPTURE.

    public Color getCapturePixelColor(Point2D location);

    public Image getCaptureRegion(Rectangle2D region);

    // TIMER.

    public void timerWaitForIdle();

}
