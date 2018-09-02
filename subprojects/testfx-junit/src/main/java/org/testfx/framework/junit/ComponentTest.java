package org.testfx.framework.junit;

import javafx.scene.Node;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.testfx.api.FxRobot;
import org.testfx.testcase.api.ComponentTestBase;

/**
 * The base class that your JUnit test classes should extend from that interact
 * with and/or verify the state of a JavaFX <b>component</b>. Such test classes,
 * containing one or more {@code @Test}-annotated methods (individual test
 * cases), can interact with a JavaFX UI using the {@link FxRobot} methods that
 * test class will inherit from {@code FxRobot}. Verifying the state of the UI
 * can be accomplished by using either the
 * <a href="http://hamcrest.org/">Hamcrest</a> based
 * {@link org.testfx.api.FxAssert#verifyThat} or the
 * <a href="http://joel-costigliola.github.io/assertj/">AssertJ</a> based
 * {@link org.testfx.assertions.api.Assertions#assertThat(Node)}.
 * <p>
 * Example:
 * 
 * <pre>
 * {@code
 * public class TestComponentTest extends ComponentTest<ColorPicker> {
 *
 *     
 *     {@literal @}Override
 *     public ColorPicker createComponent() {
 *             return new ColorPicker(Color.BLUE);
 *     }
 *
 *     {@literal @}Test
 *     public void shouldAllowUserToChangeColor() {
 *         // when:
 *         clickOn(getComponent());
 *         moveBy(30, 70);
 *         clickOn(MouseButton.PRIMARY);
 *
 *         // then:
 *         assertThat(getComponent().getValue(),equalTo(Color.web("#b31a1aff")));
 *     }
 * }
 * }
 * </pre>
 * 
 */
public abstract class ComponentTest<T extends Node> extends ComponentTestBase<T> {

    @BeforeClass
    public static void beforeAll() throws Throwable {
        ComponentTestBase.beforeAll();
    }

    @AfterClass
    public static void afterAll() throws Throwable {
        ComponentTestBase.afterAll();
    }

    @Override
    @Before
    public void beforeTest() throws Throwable {
        super.beforeTest();
    }

    @Override
    @After
    public void afterTest() throws Throwable {
        super.afterTest();
    }

}