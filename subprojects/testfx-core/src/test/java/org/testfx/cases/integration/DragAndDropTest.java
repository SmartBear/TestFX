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
package org.testfx.cases.integration;

import javafx.scene.control.ListView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.testfx.api.FxToolkit;
import org.testfx.cases.TestCaseBase;
import org.testfx.util.WaitForAsyncUtils;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.testfx.api.FxAssert.verifyThat;

public class DragAndDropTest extends TestCaseBase {

    //---------------------------------------------------------------------------------------------
    // FIELDS.
    //---------------------------------------------------------------------------------------------

    public ListView<String> leftListView;
    public ListView<String> rightListView;

    //---------------------------------------------------------------------------------------------
    // FIXTURE METHODS.
    //---------------------------------------------------------------------------------------------

    @Before
    public void setup() throws Exception {
        FxToolkit.setupSceneRoot(() -> {
            leftListView = new ListView<>();
            rightListView = new ListView<>();
            leftListView.getItems().addAll("L1", "L2", "L3");
            rightListView.getItems().addAll("R1", "R2", "R3");
            ImmutableList.of(leftListView, rightListView).forEach(this::setupListView);
            return new HBox(leftListView, rightListView);
        });
        FxToolkit.setupStage(Stage::show);
    }

    public void setupListView(ListView<String> listView) {
        listView.setOnDragDetected(event -> {
            String selectedItem = listView.getSelectionModel().getSelectedItem();
            ClipboardContent content = new ClipboardContent();
            content.putString(selectedItem);
            Dragboard dragboard = listView.startDragAndDrop(TransferMode.MOVE);
            dragboard.setContent(content);
            event.consume();
        });

        listView.setOnDragEntered(event -> {
            event.acceptTransferModes(TransferMode.MOVE);
        });

        listView.setOnDragOver(event -> {
            event.acceptTransferModes(TransferMode.MOVE);
        });

        listView.setOnDragDropped(event -> {
            event.acceptTransferModes(TransferMode.MOVE);
            Dragboard dragboard = event.getDragboard();
            String acceptedItem = dragboard.getString();
            if (acceptedItem != null) {
                listView.getItems().add(acceptedItem);
                event.setDropCompleted(true);
                event.consume();
            }
        });

        listView.setOnDragDone(event -> {
            String selectedItem = listView.getSelectionModel().getSelectedItem();
            listView.getItems().remove(selectedItem);
        });
    }

    //---------------------------------------------------------------------------------------------
    // FEATURE METHODS.
    //---------------------------------------------------------------------------------------------

    @Test
    public void should_have_initialized_items() {
        // expect:
        verifyThat(leftListView.getItems(), hasSize(3));
        verifyThat(rightListView.getItems(), hasSize(3));
        verifyThat(leftListView.getItems(), hasItems("L1", "L2", "L3"));
        verifyThat(rightListView.getItems(), hasItems("R1", "R2", "R3"));
    }

    @Test
    public void should_drag_and_drop_from_left_to_right() {
        // when:
        drag("L1");
        moveTo("R1");
        drop();

        // then:
        verifyThat(leftListView.getItems(), hasSize(2));
        verifyThat(rightListView.getItems(), hasSize(4));
        verifyThat(leftListView.getItems(), hasItems("L2", "L3"));
        verifyThat(rightListView.getItems(), hasItems("L1", "R1", "R2", "R3"));
    }

    @Test
    public void should_drag_and_drop_from_right_to_left() {
        // when:
        drag("R3");
        dropTo("L2");
        WaitForAsyncUtils.waitForFxEvents();

        // then:
        verifyThat(leftListView.getItems(), hasSize(4));
        verifyThat(rightListView.getItems(), hasSize(2));
        verifyThat(leftListView.getItems(), hasItems("L1", "L2", "L3", "R3"));
        verifyThat(rightListView.getItems(), hasItems("R1", "R2"));
    }

    @Test
    public void should_drag_and_drop_from_left_to_left() {
        // when:
        drag("L3");
        dropTo("L2");
        WaitForAsyncUtils.waitForFxEvents();

        // then:
        verifyThat(leftListView.getItems(), hasSize(3));
        verifyThat(rightListView.getItems(), hasSize(3));
        verifyThat(leftListView.getItems(), hasItems("L1", "L2", "L3"));
        verifyThat(rightListView.getItems(), hasItems("R1", "R2", "R3"));
    }

}
