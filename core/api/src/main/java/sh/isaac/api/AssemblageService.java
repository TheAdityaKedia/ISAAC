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



package sh.isaac.api;

//~--- JDK imports ------------------------------------------------------------

import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

//~--- non-JDK imports --------------------------------------------------------

import org.jvnet.hk2.annotations.Contract;

import sh.isaac.api.collections.NidSet;
import sh.isaac.api.collections.SememeSequenceSet;
import sh.isaac.api.component.sememe.SememeChronology;
import sh.isaac.api.component.sememe.SememeConstraints;
import sh.isaac.api.component.sememe.SememeServiceTyped;
import sh.isaac.api.component.sememe.SememeSnapshotService;
import sh.isaac.api.component.sememe.SememeType;
import sh.isaac.api.component.sememe.version.SememeVersion;
import sh.isaac.api.coordinate.StampCoordinate;
import sh.isaac.api.coordinate.StampPosition;

//~--- interfaces -------------------------------------------------------------

/**
 * The Interface AssemblageService.
 *
 * @author kec
 */
@Contract
public interface AssemblageService
        extends DatabaseServices {
   /**
    * Of type.
    *
    * @param <V> the value type
    * @param versionType the version type
    * @return the sememe service typed
    */
   <V extends SememeVersion> SememeServiceTyped ofType(SememeType versionType);

   /**
    * Write a sememe to the sememe service. Will not overwrite a sememe if one already exists, rather it will
    * merge the written sememe with the provided sememe.
    *
    *
    * The persistence of the concept is dependent on the persistence
    * of the underlying service.
    *
    * @param sememeChronicle the sememe chronicle
    * @param constraints the constraints
    */
   void writeSememe(SememeChronology sememeChronicle, SememeConstraints... constraints);

   //~--- get methods ---------------------------------------------------------

   /**
    * Gets the assemblage types.
    *
    * @return the sequence identifiers of all assemblage concepts that are actually in use by a sememe
    */
   Stream<Integer> getAssemblageTypes();

   /**
    * Gets the descriptions for component.
    *
    * @param componentNid the component nid
    * @return the descriptions for component
    */
   Stream<SememeChronology> getDescriptionsForComponent(int componentNid);

   /**
    * Gets the optional sememe.
    *
    * @param sememeId sequence or nid for a sememe
    * @return the identified {@code SememeChronology}
    */
   Optional<? extends SememeChronology> getOptionalSememe(int sememeId);

   /**
    * Gets the parallel sememe stream.
    *
    * @return the parallel sememe stream
    */
   Stream<SememeChronology> getParallelSememeStream();

   /**
    * Gets the sememe.
    *
    * @param sememeId sequence or nid for a sememe
    * @return the identified {@code SememeChronology}
    */
   SememeChronology getSememe(int sememeId);

   /**
    * Use in circumstances when not all sememes may have been loaded to find out if a sememe is present,
    * without incurring the overhead of reading back the object.
    * @param sememeId Either a nid or sememe sequence
    * @return true if present, false otherwise
    */
   boolean hasSememe(int sememeId);

   /**
    * Gets the sememe chronology stream.
    *
    * @return the sememe chronology stream
    */
   Stream<SememeChronology> getSememeChronologyStream();

   /**
    * Gets the sememe count.
    *
    * @return the sememe count
    */
   int getSememeCount();

   /**
    * Gets the sememe key parallel stream.
    *
    * @return the sememe key parallel stream
    */
   IntStream getSememeKeyParallelStream();

   /**
    * Gets the sememe key stream.
    *
    * @return the sememe key stream
    */
   IntStream getSememeKeyStream();

   /**
    * Gets the sememe sequences for component.
    *
    * @param componentNid the component nid
    * @return the sememe sequences for component
    */
   SememeSequenceSet getSememeSequencesForComponent(int componentNid);

   /**
    * Gets the sememe sequences for component from assemblage.
    *
    * @param componentNid the component nid
    * @param assemblageConceptSequence the assemblage concept sequence
    * @return the sememe sequences for component from assemblage
    */
   SememeSequenceSet getSememeSequencesForComponentFromAssemblage(int componentNid, int assemblageConceptSequence);

   /**
    * Gets the sememe sequences for component from assemblages.
    *
    * @param componentNid The component nid that the sememes must reference
    * @param allowedAssemblageSequences The (optional) set of assemblage types to limit the return to.  If empty or null, no assemblage filter is applied.
    * @return the sememe sequences for component from assemblages
    */
   SememeSequenceSet getSememeSequencesForComponentFromAssemblages(int componentNid,
         Set<Integer> allowedAssemblageSequences);

   /**
    * Gets the sememe sequences for components from assemblage.
    *
    * @param componentNidSet the component nid set
    * @param assemblageConceptSequence the assemblage concept sequence
    * @return the sememe sequences for components from assemblage
    */
   SememeSequenceSet getSememeSequencesForComponentsFromAssemblage(NidSet componentNidSet,
         int assemblageConceptSequence);

   /**
    * Gets the sememe sequences for components from assemblage modified after position.
    *
    * @param componentNidSet the component nid set
    * @param assemblageConceptSequence the assemblage concept sequence
    * @param position the position
    * @return the sememe sequences for components from assemblage modified after position
    */
   SememeSequenceSet getSememeSequencesForComponentsFromAssemblageModifiedAfterPosition(NidSet componentNidSet,
         int assemblageConceptSequence,
         StampPosition position);

   /**
    * Gets the sememe sequences from assemblage.
    *
    * @param assemblageConceptSequence the assemblage concept sequence
    * @return the sememe sequences from assemblage
    */
   SememeSequenceSet getSememeSequencesFromAssemblage(int assemblageConceptSequence);

   /**
    * Gets the sememes for component.
    *
    * @param <C>
    * @param componentNid the component nid
    * @return the sememes for component
    */
   <C extends SememeChronology> Stream<C> getSememesForComponent(int componentNid);

   /**
    * Gets the sememes for component from assemblage.
    *
    * @param <C>
    * @param componentNid the component nid
    * @param assemblageConceptSequence the assemblage concept sequence
    * @return the sememes for component from assemblage
    */
   <C extends SememeChronology> Stream<C> getSememesForComponentFromAssemblage(int componentNid,
         int assemblageConceptSequence);

   /**
    * Gets the sememes for component from assemblages.
    *
    * @param <C>
    * @param componentNid The component nid that the sememes must reference
    * @param allowedAssemblageSequences The (optional) set of assemblage types to limit the return to.  If empty or null, no assemblage filter is applied.
    * @return the sememes for component from assemblages
    */
   <C extends SememeChronology> Stream<C> getSememesForComponentFromAssemblages(int componentNid,
         Set<Integer> allowedAssemblageSequences);

   /**
    * Gets the sememes from assemblage.
    *
    * @param <C>
    * @param assemblageConceptSequence the assemblage concept sequence
    * @return the sememes from assemblage
    */
   <C extends SememeChronology> Stream<C> getSememesFromAssemblage(int assemblageConceptSequence);

   /**
    * Gets the snapshot.
    *
    * @param <V> the value type
    * @param versionType the version type
    * @param stampCoordinate the stamp coordinate
    * @return the snapshot
    */
   <V extends SememeVersion> SememeSnapshotService<V> getSnapshot(Class<V> versionType,
         StampCoordinate stampCoordinate);
}
