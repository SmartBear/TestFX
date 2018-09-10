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
package org.testfx.robot.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import org.testfx.robot.KeyboardRobot;
import org.testfx.robot.SleepRobot;
import org.testfx.robot.TypeRobot;

public class TypeRobotImpl implements TypeRobot {

    static final long SLEEP_AFTER_KEY_CODE_IN_MILLIS_DEFAULT = 25;
    static final long SLEEP_AFTER_KEY_CODE_IN_MILLIS_AGGRESSIVE = 0;
    static final long SLEEP_AFTER_KEY_CODE_IN_MILLIS_DEBUG = 50;
    static long SLEEP_AFTER_KEY_CODE_IN_MILLIS = SLEEP_AFTER_KEY_CODE_IN_MILLIS_DEFAULT;

    private final KeyboardRobot keyboardRobot;
    private final SleepRobot sleepRobot;


    /**
     * Sets all timing relevant values to the defined default values
     */
    public static void setDefaultTiming() {
        SLEEP_AFTER_KEY_CODE_IN_MILLIS = SLEEP_AFTER_KEY_CODE_IN_MILLIS_DEFAULT;
    }
    /**
     * Sets all timing relevant values to be very fast. Timing may not be guaranteed in all cases,
     * violations may occur. This setup shouldn't generally be used. It is mainly used for testing. 
     */
    public static void setAggressiveTiming() {
        SLEEP_AFTER_KEY_CODE_IN_MILLIS = SLEEP_AFTER_KEY_CODE_IN_MILLIS_AGGRESSIVE;
    }
    /**
     * Sets all timing relevant values to a value, that allows the user to follow the test
     * on screen for debugging. This option may also be used to identify timing issues in 
     * a test
     */
    public static void setDebugTiming() {
        SLEEP_AFTER_KEY_CODE_IN_MILLIS = SLEEP_AFTER_KEY_CODE_IN_MILLIS_DEBUG;
    }
    
    
    public TypeRobotImpl(KeyboardRobot keyboardRobot, SleepRobot sleepRobot) {
        Objects.requireNonNull(keyboardRobot, "keyboardRobot must not be null");
        Objects.requireNonNull(sleepRobot, "sleepRobot must not be null");
        this.keyboardRobot = keyboardRobot;
        this.sleepRobot = sleepRobot;
    }

    @Override
    public void push(KeyCode... combination) {
        pushKeyCodeCombination(combination);
    }

    @Override
    public void push(KeyCodeCombination combination) {
        pushKeyCodeCombination(combination);
    }

    @Override
    public void type(KeyCode... keys) {
        for (KeyCode keyCode : keys) {
            pushKeyCode(keyCode);
            if (SLEEP_AFTER_KEY_CODE_IN_MILLIS > 0) {
                sleepRobot.sleep(SLEEP_AFTER_KEY_CODE_IN_MILLIS);
            }
        }
    }

    @Override
    public void type(KeyCode key, int times) {
        for (int index = 0; index < times; index++) {
            pushKeyCode(key);
            if (SLEEP_AFTER_KEY_CODE_IN_MILLIS > 0) {
                sleepRobot.sleep(SLEEP_AFTER_KEY_CODE_IN_MILLIS);
            }
        }
    }

    private void pushKeyCode(KeyCode keyCode) {
        keyboardRobot.press(keyCode);
        keyboardRobot.release(keyCode);
    }

    private void pushKeyCodeCombination(KeyCode... keyCodeCombination) {
        List<KeyCode> keyCodesForwards = Arrays.asList(keyCodeCombination);
        List<KeyCode> keyCodesBackwards = new ArrayList<>(keyCodesForwards);
        Collections.reverse(keyCodesBackwards);
        keyboardRobot.press(keyCodesForwards.toArray(new KeyCode[0]));
        keyboardRobot.release(keyCodesBackwards.toArray(new KeyCode[0]));
    }

    private void pushKeyCodeCombination(KeyCodeCombination keyCodeCombination) {
        List<KeyCode> keyCodes = filterKeyCodes(keyCodeCombination);
        pushKeyCodeCombination(keyCodes.toArray(new KeyCode[0]));
    }

    private List<KeyCode> filterKeyCodes(KeyCodeCombination keyCombination) {
        List<KeyCode> modifierKeyCodes = new ArrayList<>();
        if (keyCombination.getShift() == KeyCombination.SHIFT_DOWN.getValue()) {
            modifierKeyCodes.add(KeyCode.SHIFT);
        }
        if (keyCombination.getAlt() == KeyCombination.ALT_DOWN.getValue()) {
            modifierKeyCodes.add(KeyCode.ALT);
        }
        if (keyCombination.getMeta() == KeyCombination.META_DOWN.getValue()) {
            modifierKeyCodes.add(KeyCode.META);
        }
        if (keyCombination.getShortcut() == KeyCombination.SHORTCUT_DOWN.getValue()) {
            modifierKeyCodes.add(KeyCode.SHORTCUT);
        }
        if (keyCombination.getControl() == KeyCombination.CONTROL_DOWN.getValue()) {
            modifierKeyCodes.add(KeyCode.CONTROL);
        }
        modifierKeyCodes.add(keyCombination.getCode());
        return Collections.unmodifiableList(modifierKeyCodes);
    }

}
