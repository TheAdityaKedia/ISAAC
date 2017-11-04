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



package sh.isaac.provider.bdb.taxonomy;

//~--- JDK imports ------------------------------------------------------------

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.IntStream;

//~--- non-JDK imports --------------------------------------------------------

import sh.isaac.api.Get;
import sh.isaac.model.collections.SpinedIntObjectMap;
import sh.isaac.api.coordinate.ManifoldCoordinate;
import sh.isaac.api.task.TimedTaskWithProgressTracker;
import sh.isaac.api.tree.Tree;
import sh.isaac.model.tree.HashTreeBuilder;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author kec
 */
public class TreeBuilderTask
        extends TimedTaskWithProgressTracker<Tree> {
   private final AtomicInteger             conceptsProcessed = new AtomicInteger();
   private String                          message           = "setting up taxonomy collection";
   private final int                       conceptCount;
   private final StampedLock               stampedLock;
   private final SpinedIntObjectMap<int[]> originDestinationTaxonomyRecordMap;
   private final ManifoldCoordinate        manifoldCoordinate;
   private final int                       conceptAssemblageNid;

   //~--- constructors --------------------------------------------------------

   public TreeBuilderTask(SpinedIntObjectMap<int[]> originDestinationTaxonomyRecordMap,
                          ManifoldCoordinate manifoldCoordinate,
                          StampedLock stampedLock) {
      this.originDestinationTaxonomyRecordMap = originDestinationTaxonomyRecordMap;
      this.manifoldCoordinate                 = manifoldCoordinate;
      this.conceptAssemblageNid               = manifoldCoordinate.getLogicCoordinate()
            .getConceptAssemblageNid();
      this.conceptCount = (int) Get.identifierService()
                                   .getNidsForAssemblage(conceptAssemblageNid)
                                   .count();
      this.stampedLock                        = stampedLock;
      this.addToTotalWork(conceptCount);
      this.addToTotalWork(conceptCount / 10);  // adding a buffer for the DFS with cycle detection at the end...
      this.updateTitle("Generating " + manifoldCoordinate.getTaxonomyType() + " snapshot");
      this.setProgressMessageGenerator(
          (task) -> {
             updateMessage(message);
          });
      setCompleteMessageGenerator(
          (task) -> {
             updateMessage(getState() + " in " + getFormattedDuration());
          });
      Get.activeTasks()
         .add(this);
   }

   //~--- methods -------------------------------------------------------------

   @Override
   protected Tree call()
            throws Exception {
      try {
         long stamp = this.stampedLock.tryOptimisticRead();
         Tree tree  = compute();

         if (this.stampedLock.validate(stamp)) {
            return tree;
         }

         stamp = this.stampedLock.readLock();
         this.addToTotalWork(conceptCount);

         try {
            return compute();
         } finally {
            this.stampedLock.unlock(stamp);
         }
      } finally {
         Get.activeTasks()
            .remove(this);
      }
   }

   private Tree compute() {
      GraphCollector  collector = new GraphCollector(this.originDestinationTaxonomyRecordMap, this.manifoldCoordinate);
      IntStream       conceptNidStream = Get.identifierService()
                                            .getNidsForAssemblage(conceptAssemblageNid);
      HashTreeBuilder graphBuilder     = conceptNidStream.filter(
                                             (conceptNid) -> {
               completedUnitOfWork();
               return true;
            })
                                                         .collect(
                                                               () -> new HashTreeBuilder(
                                                                     this.manifoldCoordinate,
                                                                           this.conceptAssemblageNid),
                                                                     collector,
                                                                     collector);

      message = "searching for redundancies and cycles";

      Tree tree = graphBuilder.getSimpleDirectedGraph();

      message = "complete";
      completedUnitsOfWork(conceptCount / 10);
      return tree;
   }
}

