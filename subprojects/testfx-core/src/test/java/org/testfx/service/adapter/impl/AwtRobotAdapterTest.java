/*
 * Copyright 2013-2014 SmartBear Software
 * Copyright 2014-2018 The TestFX Contributors
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence"); You may
 * not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * http://ec.europa.eu/idabc/eupl.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the Licence for the
 * specific language governing permissions and limitations under the Licence.
 */
package org.testfx.service.adapter.impl;

import java.awt.GraphicsEnvironment;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testfx.api.FxToolkit;
import org.testfx.cases.InternalTestCaseBase;
import org.testfx.internal.PlatformAdapter;
import org.testfx.internal.PlatformAdapter.OS;
import org.testfx.service.locator.PointLocator;
import org.testfx.service.locator.impl.BoundsLocatorImpl;
import org.testfx.service.locator.impl.PointLocatorImpl;
import org.testfx.util.WaitForAsyncUtils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testfx.util.WaitForAsyncUtils.asyncFx;

public class AwtRobotAdapterTest extends InternalTestCaseBase {

    AwtRobotAdapter robotAdapter;
    Parent sceneRoot;
    Region region;
    Point2D regionPoint;
    static final long SLEEP = 100;

    @BeforeClass
    public static void beforeClass() throws Exception { //shadow super
        assumeFalse("skipping AwtRobotAdapterTest - in headless environment",
            GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance());
        FxToolkit.registerPrimaryStage();
    }
    
    @Override
    public Node createComponent() {
        region = new Region();
        region.setStyle("-fx-background-color: magenta;");

        VBox box = new VBox(region);
        box.setPadding(new Insets(10));
        box.setSpacing(10);
        VBox.setVgrow(region, Priority.ALWAYS);
        box.setPrefSize(300, 100);

        sceneRoot = new StackPane(box);
        return sceneRoot;
    }

    @Before
    public void setup() throws Exception {
        robotAdapter = new AwtRobotAdapter();
        PointLocator pointLocator = new PointLocatorImpl(new BoundsLocatorImpl());
        regionPoint = pointLocator.point(region).atPosition(Pos.CENTER).query();
    }


    @Test
    public void robotCreate() {
        // when:
        robotAdapter.robotCreate();

        // then:
        assertThat(robotAdapter.getRobotInstance(), notNullValue());
    }

    @Test
    public void robotDestroy_initialized_robot() {
        // given:
        robotAdapter.robotCreate();

        // when:
        robotAdapter.robotDestroy();

        // then:
        assertThat(robotAdapter.getRobotInstance(), nullValue());
    }

    @Test
    public void robotDestroy_uninitialized_robot() {
        // when:
        robotAdapter.robotDestroy();

        // then:
        assertThat(robotAdapter.getRobotInstance(), nullValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void keyPress() {
        // given:
        EventHandler<KeyEvent> keyEventHandler = mock(EventHandler.class);
        getTestScene().addEventHandler(KeyEvent.KEY_PRESSED, keyEventHandler);

        // and:
        robotAdapter.mouseMove(regionPoint);
        try {
            WaitForAsyncUtils.waitForFxEvents(); 
            sleep(SLEEP);

            // when:
            robotAdapter.keyPress(KeyCode.A);
    
            // then:
            WaitForAsyncUtils.waitForFxEvents(); 
            sleep(SLEEP);
            verify(keyEventHandler, times(1)).handle(any());
        }
        finally {
            robotAdapter.keyRelease(KeyCode.A);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void keyRelease() {
        // given:
        EventHandler<KeyEvent> keyEventHandler = mock(EventHandler.class);
        getTestScene().addEventHandler(KeyEvent.KEY_RELEASED, keyEventHandler);

        // and:
        robotAdapter.mouseMove(regionPoint);
        WaitForAsyncUtils.waitForFxEvents(); 
        sleep(SLEEP);

        // when:
        robotAdapter.keyPress(KeyCode.A);
        WaitForAsyncUtils.waitForFxEvents(); 
        sleep(SLEEP);
        robotAdapter.keyRelease(KeyCode.A);

        // then:
        WaitForAsyncUtils.waitForFxEvents(); 
        sleep(SLEEP);
        verify(keyEventHandler, times(1)).handle(any());
    }

    @Test
    public void getMouseLocation() {
        // when:
        Point2D mouseLocation = robotAdapter.getMouseLocation();

        // then:
        assertThat("mouseLocation.getX() is greater than or equal to 0.0", mouseLocation.getX() >= 0.0);
        assertThat("mouseLocation.getY() is greater than or equal to 0.0", mouseLocation.getY() >= 0.0);
    }

    @Test
    public void mouseMove() {
        // given:
        robotAdapter.mouseMove(new Point2D(100, 200));
        WaitForAsyncUtils.waitForFxEvents(); 
        sleep(SLEEP);

        // when:
        Point2D mouseLocation = robotAdapter.getMouseLocation();

        // then:
        assertThat(mouseLocation.getX(), is(100.0));
        assertThat(mouseLocation.getY(), is(200.0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void mousePress() {
        // given:
        EventHandler<MouseEvent> mouseEventHandler = mock(EventHandler.class);
        region.addEventHandler(MouseEvent.MOUSE_PRESSED, mouseEventHandler);

        // and:
        robotAdapter.mouseMove(regionPoint);
        WaitForAsyncUtils.waitForFxEvents(); 
        sleep(SLEEP);

        // when:
        robotAdapter.mousePress(MouseButton.PRIMARY);
        try {
    
            // then:
            WaitForAsyncUtils.waitForFxEvents(); 
            sleep(SLEEP);
            verify(mouseEventHandler, times(1)).handle(any());
        }
        finally {
            robotAdapter.mouseRelease(MouseButton.PRIMARY);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void mouseRelease() {
        // given:
        EventHandler<MouseEvent> mouseEventHandler = mock(EventHandler.class);
        region.addEventHandler(MouseEvent.MOUSE_RELEASED, mouseEventHandler);

        // and:
        robotAdapter.mouseMove(regionPoint);
        WaitForAsyncUtils.waitForFxEvents(); 
        sleep(SLEEP);

        // when:
        robotAdapter.mousePress(MouseButton.PRIMARY);
        WaitForAsyncUtils.waitForFxEvents(); 
        sleep(SLEEP);
        robotAdapter.mouseRelease(MouseButton.PRIMARY);

        // then:
        WaitForAsyncUtils.waitForFxEvents(); 
        sleep(SLEEP);
        verify(mouseEventHandler, times(1)).handle(any());
    }

    @Test
    public void getCapturePixelColor() {
        // given:
        assumeThat(PlatformAdapter.getOs(), is(not(OS.mac)));

        // when:
        Color pixelColor = robotAdapter.getCapturePixelColor(regionPoint);

        // then:
        assertThat(pixelColor, is(Color.web("magenta")));
    }

    @Test
    public void getCaptureRegion() {
        // given:
        assumeThat(PlatformAdapter.getOs(), is(not(OS.mac)));

        // when:
        Rectangle2D region = new Rectangle2D(regionPoint.getX(), regionPoint.getY(), 10, 20);
        Image regionImage = robotAdapter.getCaptureRegion(region);

        // then:
        assertThat(regionImage.getWidth(), is(10.0));
        assertThat(regionImage.getHeight(), is(20.0));
        assertThat(regionImage.getPixelReader().getColor(5, 10), is(Color.web("magenta")));
    }

    @Test
    public void timerWaitForIdle() {
        // when:
        AtomicBoolean reachedStatement = new AtomicBoolean(false);
        asyncFx(() -> {
            sleep(100, TimeUnit.MILLISECONDS);
            asyncFx(() -> reachedStatement.set(true));
        });
        WaitForAsyncUtils.waitForFxEvents(); 
        sleep(SLEEP);

        // then:
        assertThat(reachedStatement.get(), is(true));
    }

}
