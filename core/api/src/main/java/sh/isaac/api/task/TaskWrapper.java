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



package sh.isaac.api.task;

//~--- JDK imports ------------------------------------------------------------

import java.util.function.Function;
import javafx.application.Platform;

//~--- non-JDK imports --------------------------------------------------------


import javafx.concurrent.Task;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author kec
 * @param <R> The type of object returned by this wrapper
 * @param <T> The type of object returned by the wrapped task
 */
public class TaskWrapper<R, T> extends Task<R> {
   final Task<T> wrappedTask;
   final Function<T,R> adapter;

   //~--- constructors --------------------------------------------------------

   public TaskWrapper(Task<T> wrappedTask, Function<T,R> adapter, String title) {
      this.wrappedTask = wrappedTask;
      this.adapter = adapter;
      
      if (Platform.isFxApplicationThread()) {
         linkProperties(title);
      } else {
         Platform.runLater(() -> this.linkProperties(title));
      }
      
   }
   //~--- methods -------------------------------------------------------------

   private void linkProperties(String title) {
      updateTitle(title);
      // Wire up all the properties so they use this wrapped task
      this.wrappedTask.messageProperty().addListener((observable, oldValue, newValue) -> {
         this.updateMessage(newValue);
      });
      this.updateMessage(this.wrappedTask.getMessage());
      this.wrappedTask.workDoneProperty().addListener((observable, oldValue, newValue) -> {
         this.updateProgress(newValue.doubleValue(), this.wrappedTask.getTotalWork());
      });
      this.wrappedTask.totalWorkProperty().addListener((observable, oldValue, newValue) -> {
         this.updateProgress(this.wrappedTask.getWorkDone(), newValue.doubleValue());
      });
      this.updateProgress(this.wrappedTask.getWorkDone(), this.wrappedTask.getTotalWork());
   }

   @Override
   protected R call() throws Exception {
      return adapter.apply(wrappedTask.get());
   }

   @Override
   protected void updateProgress(double workDone, double max) {
      super.updateProgress(workDone, max); //To change body of generated methods, choose Tools | Templates.
   }

   @Override
   protected void updateProgress(long workDone, long max) {
      super.updateProgress(workDone, max); //To change body of generated methods, choose Tools | Templates.
   }

}

