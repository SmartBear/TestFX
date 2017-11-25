/*
 * Copyright 2013-2014 SmartBear Software
 * Copyright 2014-2017 The TestFX Contributors
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
package org.testfx.matcher.control;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import javafx.scene.Node;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.testfx.api.annotation.Unstable;
import org.testfx.util.ColorUtils;

import static org.testfx.matcher.base.GeneralMatchers.typeSafeMatcher;
import static org.testfx.util.ColorUtils.getClosestNamedColor;

/**
 * TestFX matchers for {@link TextFlow} controls.
 */
@Unstable(reason = "needs more tests")
public class TextFlowMatchers {

    private TextFlowMatchers() {}

    //---------------------------------------------------------------------------------------------
    // STATIC METHODS.
    //---------------------------------------------------------------------------------------------

    /**
     * Creates a matcher that matches all ({@link TextFlow}s whose "text" (the result of combining all of
     * its {@link Text} children's {@link Text#getText() text} together) equals the given {@code string}.
     */
    @Factory
    public static Matcher<TextFlow> hasText(String string) {
        String descriptionText = "has text \"" + string + "\"";
        return typeSafeMatcher(TextFlow.class, descriptionText,
            textFlow -> "TextFlow containing text: \"" + getText(textFlow) + "\"",
            node -> hasText(node, string));
    }

    /**
     * Allows one to verify both the content and color of the text that makes
     * up a TextFlow. The color is matched by using the closest named color,
     * as described further on.
     * <p>
     * Colors are specified using the following markup:
     *
     * <pre><code><COLOR>text</COLOR></code></pre>
     * <p>
     * Where {@literal COLOR} is one of JavaFX's named colors.
     * <p>
     * Here is an example for verifying that a TextFlow contains the text
     * "hello" and that the named color that has the closest value to the
     * color of the text is {@literal Colors.RED}:
     * <p>
     * <pre><code>
     *   Text text = new Text("hello");
     *   text.setFill(Colors.RED);
     *   TextFlow textFlow = new TextFlow(text);
     *   assertThat(textFlow, TextFlowMatchers.hasColoredText("<RED>hello</RED>"));
     * </code></pre>
     *
     * @param coloredTextMarkup the text contained in the TextFlow with color markup that
     * specifies the expected color of the text
     * @return a match if the text contained in the TextFlow has the same content
     * and colors that match by the "closest named color" criteria
     * @see <a href="https://docs.oracle.com/javase/9/docs/api/javafx/scene/doc-files/cssref.html#typecolor">Named Colors</a>
     */
    @Factory
    public static Matcher<TextFlow> hasColoredText(String coloredTextMarkup) {
        String descriptionText = "has colored text \"" + coloredTextMarkup + "\"";
        return typeSafeMatcher(TextFlow.class, descriptionText,
            textFlow -> "TextFlow with colored text: \"" + getColoredTextMarkup(textFlow, false) + "\"",
            node -> hasColoredText(node, coloredTextMarkup, false));
    }

    /**
     * Allows one to verify both the content and color of the text that makes
     * up a TextFlow. The color is matched in an exact way, as described fruther
     * on.
     * <p>
     * Colors are specified using the following markup:
     * <p>
     * <pre><code><COLOR>text</COLOR></code></pre>
     * <p>
     * Where {@literal COLOR} is one of JavaFX's named colors.
     * <p>
     * Here is an example for verifying that a TextFlow contains the text
     * "hello" and that the color of the text is *exactly* {@literal Colors.BLUE}
     * (that is, it has an RGB value of (0, 0, 255)).
     *
     * <pre><code>
     *   Text text = new Text("hello");
     *   text.setFill(Colors.BLUE); // or: text.setFill(Colors.rgb(0, 0, 255));
     *   TextFlow textFlow = new TextFlow(text);
     *   assertThat(textFlow, TextFlowMatchers.hasExactlyColoredText("<BLUE>hello</BLUE>"));
     * </code></pre>
     *
     * @param coloredTextMarkup the text contained in the TextFlow with color markup that
     * specifies the expected color of the text
     * @return a match if the text contained in the TextFlow has the same content
     * and the exactly matching colors
     * @see <a href="https://docs.oracle.com/javase/9/docs/api/javafx/scene/doc-files/cssref.html#typecolor">Named Colors</a>
     */
    @Factory
    public static Matcher<TextFlow> hasExactlyColoredText(String coloredTextMarkup) {
        String descriptionText = "has exactly colored text \"" + coloredTextMarkup + "\"";
        return typeSafeMatcher(TextFlow.class, descriptionText, textFlow -> {
            for (Node child : textFlow.getChildren()) {
                if (Text.class.isAssignableFrom(child.getClass())) {
                    Text text = (Text) child;
                    String textColor = text.getFill().toString().substring(2, 8);
                    if (!ColorUtils.getNamedColor(textColor).isPresent()) {
                        return "impossible to exactly match TextFlow containing colored text: \"" +
                                ((Text) child).getText() + "\" which has color: \"" + textColor + "\".\n" +
                                "This is not a named color. The closest named color is: \"" +
                                getClosestNamedColor(textColor).toUpperCase(Locale.US) + "\".\nSee: " +
                                "https://docs.oracle.com/javase/9/docs/api/javafx/scene/doc-files" +
                                "/cssref.html#typecolor";
                    }
                }
            }
            return getColoredTextMarkup(textFlow, true);
        },
            node -> hasColoredText(node, coloredTextMarkup, true));
    }

    //---------------------------------------------------------------------------------------------
    // PRIVATE STATIC METHODS.
    //---------------------------------------------------------------------------------------------

    private static String getText(TextFlow textFlow) {
        StringBuilder textBuilder = new StringBuilder();

        for (Node child : textFlow.getChildren()) {
            if (Text.class.isAssignableFrom(child.getClass())) {
                textBuilder.append(((Text) child).getText());
            }
        }
        return textBuilder.toString();
    }

    private static boolean hasText(TextFlow textFlow, String string) {
        return Objects.equals(string, getText(textFlow));
    }

    private static boolean hasColoredText(TextFlow textFlow, String string, boolean exact) {
        String textColorMarkup = getColoredTextMarkup(textFlow, exact);
        return textColorMarkup != null && Objects.equals(string, textColorMarkup);
    }

    private static String getColoredTextMarkup(TextFlow textFlow, boolean exact) {
        StringBuilder coloredTextMarkup = new StringBuilder();

        for (Node child : textFlow.getChildren()) {
            if (Text.class.isAssignableFrom(child.getClass())) {
                Text text = (Text) child;
                final String color;
                if (exact) {
                    Optional<String> colorOptional = ColorUtils.getNamedColor(
                            text.getFill().toString().substring(2, 8));
                    if (colorOptional.isPresent()) {
                        color = colorOptional.get().toUpperCase(Locale.US);
                    } else {
                        return null;
                    }
                } else {
                    color = getClosestNamedColor(text.getFill().toString()
                            .substring(2, 8)).toUpperCase(Locale.US);
                }

                // Instead of comparing to BLACK we should compare to the color nearest the
                // -fx-text-fill applied to the TextFlow.
                if (!color.equals("BLACK")) {
                    coloredTextMarkup.append("<").append(color).append(">");
                }
                coloredTextMarkup.append(text.getText());

                if (!color.equals("BLACK")) {
                    coloredTextMarkup.append("</").append(color).append(">");
                }
            }
        }

        return coloredTextMarkup.toString();
    }
}
