/* 
 * Licensed under the Apache License, Version 2.0 (the "License");
 *
 * You may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributions from 2013-2017 where performed either by US government 
 * employees, or under US Veterans Health Administration contracts. 
 *
 * US Veterans Health Administration contributions by government employees
 * are work of the U.S. Government and are not subject to copyright
 * protection in the United States. Portions contributed by government 
 * employees are USGovWork (17USC §105). Not subject to copyright. 
 * 
 * Contribution by contractors to the US Veterans Health Administration
 * during this period are contractually contributed under the
 * Apache License, Version 2.0.
 *
 * See: https://www.usa.gov/government-works
 * 
 * Contributions prior to 2013:
 *
 * Copyright (C) International Health Terminology Standards Development Organisation.
 * Licensed under the Apache License, Version 2.0.
 *
 */
package sh.komet.gui.control;

//~--- non-JDK imports --------------------------------------------------------

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;

import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import sh.isaac.api.chronicle.CategorizedVersions;
import sh.isaac.api.observable.ObservableCategorizedVersion;
import sh.isaac.api.observable.ObservableChronology;
import sh.isaac.api.observable.ObservableVersion;

import sh.komet.gui.manifold.Manifold;
import sh.komet.gui.style.StyleClasses;

//~--- classes ----------------------------------------------------------------
/**
 *
 * @author kec
 */
public final class ComponentPanel
        extends BadgedVersionPanel {

   private int extraGridRows = 1;
   private final CategorizedVersions<ObservableCategorizedVersion> categorizedVersions;
   private final AnchorPane extensionHeaderPanel = setupHeaderPanel("Extensions:");
   private final AnchorPane versionHeaderPanel = setupHeaderPanel("Change history:", "Revert");

   private static AnchorPane setupHeaderPanel(String... strings) {
      AnchorPane anchorPane = new AnchorPane();
      anchorPane.getStyleClass().setAll("header-panel");
      Text leftText = new Text(strings[0]);
      AnchorPane.setLeftAnchor(leftText, 3.0);
      AnchorPane.setTopAnchor(leftText, 3.0);
      AnchorPane.setBottomAnchor(leftText, 3.0);
      anchorPane.getChildren().add(leftText);
      if (strings.length > 1) {
         Text rightText = new Text(strings[1]);
         AnchorPane.setRightAnchor(rightText, 3.0);
      AnchorPane.setTopAnchor(rightText, 3.0);
      AnchorPane.setBottomAnchor(rightText, 3.0);
         anchorPane.getChildren().add(rightText);
      }

      return anchorPane;
   }
   //~--- constructors --------------------------------------------------------

   public ComponentPanel(Manifold manifold, CategorizedVersions<ObservableCategorizedVersion> categorizedVersions) {
      super(manifold, categorizedVersions.getLatestVersion()
              .get());

      if (categorizedVersions.getLatestVersion()
              .isAbsent()) {
         throw new IllegalStateException("Must have a latest version: " + categorizedVersions);
      }

      this.categorizedVersions = categorizedVersions;

      // gridpane.gridLinesVisibleProperty().set(true);
      this.getStyleClass()
              .add(StyleClasses.COMPONENT_PANEL.toString());

      ObservableVersion observableVersion = getCategorizedVersion().getObservableVersion();

      isContradiction.set(this.categorizedVersions.getLatestVersion()
              .isContradicted());

      if (this.categorizedVersions.getLatestVersion()
              .isContradicted()) {
         this.categorizedVersions.getLatestVersion()
                 .contradictions()
                 .forEach(
                         (contradiction) -> {
                            versionPanels.add(new VersionPanel(manifold, contradiction));
                         });
      }

      this.categorizedVersions.getHistoricVersions()
              .forEach(
                      (historicVersion) -> {
                         versionPanels.add(new VersionPanel(manifold, historicVersion));
                      });
      observableVersion.getChronology()
              .getObservableSememeList()
              .forEach(
                      (osc) -> {
                         switch (osc.getSememeType()) {
                            case DESCRIPTION:
                            case LOGIC_GRAPH:
                               break;  // Ignore, description and logic graph where already added as an independent panel

                            default:
                               addChronology(osc);
                         }
                      });
      expandControl.setVisible(!versionPanels.isEmpty() || !extensionPanels.isEmpty());
   }

   //~--- methods -------------------------------------------------------------
   @Override
   public void addExtras() {
      switch (expandControl.getExpandAction()) {
         case HIDE_CHILDREN:
            if (!versionPanels.isEmpty()) {
               gridpane.getChildren()
                               .remove(versionHeaderPanel);
            }
            versionPanels.forEach(
                    (panel) -> {
                       gridpane.getChildren()
                               .remove(panel);
                    });
            if (!extensionPanels.isEmpty()) {
               gridpane.getChildren()
                               .remove(extensionHeaderPanel);
            }
            extensionPanels.forEach(
                    (panel) -> {
                       gridpane.getChildren()
                               .remove(panel);
                    });
            break;

         case SHOW_CHILDREN:
            if (!versionPanels.isEmpty()) {
               addPanel(versionHeaderPanel);
            }
            versionPanels.forEach(this::addPanel);
            if (!extensionPanels.isEmpty()) {
               addPanel(extensionHeaderPanel);
            }
            extensionPanels.forEach(this::addPanel);
            break;

         default:
            throw new UnsupportedOperationException("Can't handle: " + expandControl.getExpandAction());
      }
   }

   private void addChronology(ObservableChronology observableChronology) {
      CategorizedVersions<ObservableCategorizedVersion> oscCategorizedVersions
              = observableChronology.getCategorizedVersions(
                      getManifold());

      if (oscCategorizedVersions.getLatestVersion()
              .isPresent()) {
         ComponentPanel newPanel = new ComponentPanel(getManifold(), oscCategorizedVersions);

         newPanel.setBackground(new Background(new BackgroundFill(Color.ORANGE, CornerRadii.EMPTY, Insets.EMPTY)));
         extensionPanels.add(newPanel);
      }
   }

   private void addPanel(Node panel) {
      extraGridRows++;
      gridpane.getChildren()
              .remove(panel);
      GridPane.setConstraints(
              panel,
              1,
              getRows() + extraGridRows,
              getColumns() - 1,
              1,
              HPos.LEFT,
              VPos.CENTER,
              Priority.ALWAYS,
              Priority.NEVER,
              new Insets(2));
      gridpane.getChildren()
              .add(panel);
   }

   @Override
   protected boolean isLatestPanel() {
      return true;
   }

}