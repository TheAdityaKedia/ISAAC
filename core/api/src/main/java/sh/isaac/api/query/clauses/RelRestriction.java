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



package sh.isaac.api.query.clauses;

//~--- JDK imports ------------------------------------------------------------

import java.util.EnumSet;
import java.util.Map;

//~--- non-JDK imports --------------------------------------------------------

import sh.isaac.api.Get;
import sh.isaac.api.bootstrap.TermAux;
import sh.isaac.api.collections.NidSet;
import sh.isaac.api.component.concept.ConceptSpecification;
import sh.isaac.api.component.concept.ConceptVersion;
import sh.isaac.api.query.ClauseComputeType;
import sh.isaac.api.query.ClauseSemantic;
import sh.isaac.api.query.LeafClause;
import sh.isaac.api.query.Query;
import sh.isaac.api.query.WhereClause;
import sh.isaac.api.coordinate.ManifoldCoordinate;

//~--- classes ----------------------------------------------------------------

/**
 * Allows the user to define a restriction on the destination set of a
 * relationship query. Also allows the user to specify subsumption on the
 * destination restriction and relType.
 *
 * @author dylangrald
 */
public class RelRestriction
        extends LeafClause {
   /** The rel type key. */
   String relTypeKey;

   /** The destination spec key. */
   String destinationSpecKey;

   /** The view coordinate key. */
   String viewCoordinateKey;

   /** The destination subsumption key. */
   String destinationSubsumptionKey;

   /** The rel type subsumption key. */
   String relTypeSubsumptionKey;

   /** The destination set. */
   NidSet destinationSet;

   /** The rel type set. */
   NidSet relTypeSet;

   //~--- constructors --------------------------------------------------------

   /**
    * Instantiates a new rel restriction.
    */
   public RelRestriction() {}

   /**
    * Instantiates a new rel restriction.
    *
    * @param enclosingQuery the enclosing query
    * @param relTypeKey the rel type key
    * @param destinationSpecKey the destination spec key
    * @param viewCoordinateKey the view coordinate key
    * @param destinationSubsumptionKey the destination subsumption key
    * @param relTypeSubsumptionKey the rel type subsumption key
    */
   public RelRestriction(Query enclosingQuery,
                         String relTypeKey,
                         String destinationSpecKey,
                         String viewCoordinateKey,
                         String destinationSubsumptionKey,
                         String relTypeSubsumptionKey) {
      super(enclosingQuery);
      this.destinationSpecKey        = destinationSpecKey;
      this.relTypeKey                = relTypeKey;
      this.viewCoordinateKey         = viewCoordinateKey;
      this.relTypeSubsumptionKey     = relTypeSubsumptionKey;
      this.destinationSubsumptionKey = destinationSubsumptionKey;
   }

   //~--- methods -------------------------------------------------------------

   /**
    * Compute possible components.
    *
    * @param incomingPossibleComponents the incoming possible components
    * @return the nid set
    */
   @Override
   public Map<ConceptSpecification, NidSet> computePossibleComponents(Map<ConceptSpecification, NidSet> incomingPossibleComponents) {
//    System.out.println("Let declerations: " + enclosingQuery.getLetDeclarations());
      final ManifoldCoordinate manifoldCoordinate = (ManifoldCoordinate) this.enclosingQuery.getLetDeclarations()
                                                                                            .get(this.viewCoordinateKey);
      final ConceptSpecification destinationSpec = (ConceptSpecification) this.enclosingQuery.getLetDeclarations()
                                                                                             .get(this.destinationSpecKey);
      final ConceptSpecification relType = (ConceptSpecification) this.enclosingQuery.getLetDeclarations()
                                                                                     .get(this.relTypeKey);
      Boolean relTypeSubsumption = (Boolean) this.enclosingQuery.getLetDeclarations()
                                                                .get(this.relTypeSubsumptionKey);
      Boolean destinationSubsumption = (Boolean) this.enclosingQuery.getLetDeclarations()
                                                                    .get(this.destinationSubsumptionKey);

      // The default is to set relTypeSubsumption and destinationSubsumption to true.
      if (relTypeSubsumption == null) {
         relTypeSubsumption = true;
      }

      if (destinationSubsumption == null) {
         destinationSubsumption = true;
      }

      this.relTypeSet = new NidSet();
      this.relTypeSet.add(relType.getNid());

      if (relTypeSubsumption) {
         this.relTypeSet.or(Get.taxonomyService().getSnapshot(manifoldCoordinate).getKindOfConceptNidSet(relType.getNid()));
      }

      this.destinationSet = new NidSet();
      this.destinationSet.add(destinationSpec.getNid());

      if (destinationSubsumption) {
         this.destinationSet.or(Get.taxonomyService().getSnapshot(manifoldCoordinate).getKindOfConceptNidSet(destinationSpec.getNid()));
      }

      return incomingPossibleComponents;
   }

   //~--- get methods ---------------------------------------------------------

   /**
    * Gets the compute phases.
    *
    * @return the compute phases
    */
   @Override
   public EnumSet<ClauseComputeType> getComputePhases() {
      return PRE_ITERATION_AND_ITERATION;
   }

   /**
    * Gets the query matches.
    *
    * @param conceptVersion the concept version
    */
   @Override
   public void getQueryMatches(ConceptVersion conceptVersion) {
      final ManifoldCoordinate manifoldCoordinate = (ManifoldCoordinate) this.enclosingQuery.getLetDeclarations()
                                                                                            .get(this.viewCoordinateKey);

      throw new UnsupportedOperationException("Reimplement with new taxonomy service. ");
//      for (int destinationSequence: Get.taxonomyService()
//         .getAllRelationshipDestinationSequencesOfType(conceptVersion.getChronology()
//               .getConceptSequence(),
//               this.relTypeSet,
//               manifoldCoordinate)) {
//                     if (this.destinationSet.contains(destinationSequence)) {
//                        getResultsCache().add(conceptVersion.getChronology()
//                              .getNid());
//                     }
//      }
   }
    @Override
    public ClauseSemantic getClauseSemantic() {
        return ClauseSemantic.REL_RESTRICTION;
    }
   

   /**
    * Gets the where clause.
    *
    * @return the where clause
    */
   @Override
   public WhereClause getWhereClause() {
      final WhereClause whereClause = new WhereClause();

      whereClause.setSemantic(ClauseSemantic.REL_RESTRICTION);
      whereClause.getLetKeys()
                 .add(this.relTypeKey);
      whereClause.getLetKeys()
                 .add(this.destinationSpecKey);
      whereClause.getLetKeys()
                 .add(this.viewCoordinateKey);
      whereClause.getLetKeys()
                 .add(this.destinationSubsumptionKey);
      whereClause.getLetKeys()
                 .add(this.relTypeSubsumptionKey);

//    System.out.println("Where clause size: " + whereClause.getLetKeys().size());
      return whereClause;
   }
   
   @Override
   public ConceptSpecification getClauseConcept() {
      return TermAux.REL_RESTRICTION_QUERY_CLAUSE;
   }
   
}

