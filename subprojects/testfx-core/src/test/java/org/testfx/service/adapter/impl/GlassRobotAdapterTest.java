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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.event.EventHandler;
import javafx.geometry.Bounds;
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
import org.junit.Test;
import org.testfx.cases.InternalTestCaseBase;
import org.testfx.internal.PlatformAdapter;
import org.testfx.internal.PlatformAdapter.OS;
import org.testfx.service.locator.PointLocator;
import org.testfx.service.locator.impl.BoundsLocatorImpl;
import org.testfx.service.locator.impl.PointLocatorImpl;
import org.testfx.util.BoundsQueryUtils;
import org.testfx.util.WaitForAsyncUtils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testfx.util.WaitForAsyncUtils.asyncFx;

public class GlassRobotAdapterTest extends InternalTestCaseBase {

    GlassRobotAdapter robotAdapter;
    Parent sceneRoot;
    Region region;
    Point2D regionCenter;
    static final long SLEEP = 100;


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
        robotAdapter = new GlassRobotAdapter();
        PointLocator pointLocator = new PointLocatorImpl(new BoundsLocatorImpl());
        regionCenter = pointLocator.point(region).atPosition(Pos.CENTER).query();
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
        getTestStage().addEventHandler(KeyEvent.KEY_PRESSED, keyEventHandler);

        // and:
        robotAdapter.mouseMove(regionCenter);
        WaitForAsyncUtils.waitForFxEvents(); 
        sleep(SLEEP);

        // when:
        robotAdapter.keyPress(KeyCode.A);
        try {

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
        getTestStage().addEventHandler(KeyEvent.KEY_RELEASED, keyEventHandler);

        // and:
        robotAdapter.mouseMove(regionCenter);
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
        assumeThat("skipping: Robot's mouseMove broken on Windows + HiDPI (JDK-8196031)",
                System.getProperty("glass.win.uiScale", "100%"), is(equalTo("100%")));
        // given:
        robotAdapter.mouseMove(new Point2D(100, 200));

        // when:
        WaitForAsyncUtils.waitForFxEvents(); 
        sleep(SLEEP);
        // note: if this test fails in the future on HDPI Unix with Java > 10 see
        // comment in GlassRobotAdapter.getMouseLocation()
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
        robotAdapter.mouseMove(regionCenter);
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
        robotAdapter.mouseMove(regionCenter);
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
        assumeThat(System.getProperty("prism.order", ""), is(not(equalTo("d3d"))));

        // when:
        Color pixelColor = robotAdapter.getCapturePixelColor(regionCenter);

        // then:
        assertThat(pixelColor, is(Color.web("magenta")));
    }

    @Test
    public void getCaptureRegion() {
        // given:
        assumeThat(PlatformAdapter.getOs(), is(not(OS.mac)));
        assumeThat(System.getProperty("prism.order", ""), is(not(equalTo("d3d"))));

        // when:
        Bounds bounds = BoundsQueryUtils.boundsOnScreen(region);
        Image regionImage = robotAdapter.getCaptureRegion(new Rectangle2D(bounds.getMinX(), bounds.getMinY(),
                bounds.getWidth(), bounds.getHeight()));

        // then:
        assertThat(regionImage.getPixelReader().getColor((int) regionImage.getWidth() / 2,
                (int) regionImage.getHeight() / 2), is(Color.web("magenta")));
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
