/*
 * Copyright 2018 Organizations participating in ISAAC, ISAAC's KOMET, and SOLOR development include the
         US Veterans Health Administration, OSHERA, and the Health Services Platform Consortium..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sh.isaac.api.query.clauses;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import sh.isaac.api.Get;
import sh.isaac.api.bootstrap.TermAux;
import sh.isaac.api.chronicle.Chronology;
import sh.isaac.api.collections.NidSet;
import sh.isaac.api.component.concept.ConceptSpecification;
import sh.isaac.api.component.concept.ConceptVersion;
import sh.isaac.api.coordinate.StampCoordinate;
import sh.isaac.api.query.ClauseComputeType;
import sh.isaac.api.query.ClauseSemantic;
import sh.isaac.api.query.LeafClause;
import sh.isaac.api.query.Query;
import sh.isaac.api.query.WhereClause;

/**
 *
 * @author kec
 */
public class ComponentIsActive extends LeafClause {

    /**
     * The view coordinate key.
     */
    StampCoordinate stampCoordinate;

    //~--- constructors --------------------------------------------------------
    /**
     * Instantiates a new component is active clause.
     */
    public ComponentIsActive() {
    }

    /**
     * Instantiates a new component is active clause.
     *
     * @param enclosingQuery the enclosing query
     * @param stampCoordinate the view coordinate key
     */
    public ComponentIsActive(Query enclosingQuery, StampCoordinate stampCoordinate) {
        super(enclosingQuery);
        this.stampCoordinate = stampCoordinate;
    }

    //~--- methods -------------------------------------------------------------
    @Override
    public final Map<ConceptSpecification, NidSet> computeComponents(Map<ConceptSpecification, NidSet> incomingComponents) {

        getResultsCache().and(incomingComponents.get(this.getAssemblageForIteration()));
                
        getResultsCache().stream().forEach((nid) -> {
            final Optional<? extends Chronology> chronology
                    = Get.identifiedObjectService()
                            .getChronology(nid);

            if (chronology.isPresent()) {
                if (!chronology.get()
                        .isLatestVersionActive(stampCoordinate)) {
                    getResultsCache().remove(nid);
                }
            } else {
                getResultsCache().remove(nid);
            }
        });
        HashMap<ConceptSpecification, NidSet> resultsMap = new HashMap<>(incomingComponents);
        resultsMap.put(this.getAssemblageForIteration(), getResultsCache());
        return resultsMap;
    }

    /**
     * Compute possible components.
     *
     * @param incomingPossibleComponents the incoming possible components
     * @return the nid set
     */
    @Override
    public final Map<ConceptSpecification, NidSet> computePossibleComponents(Map<ConceptSpecification, NidSet> incomingPossibleComponents) {
        NidSet possibleComponents = NidSet.of(Get.identifierService().getNidsForAssemblage(this.getAssemblageForIteration()));
            getResultsCache().or(possibleComponents);
        if (incomingPossibleComponents.get(this.getAssemblageForIteration()) != null) {
            getResultsCache().or(incomingPossibleComponents.get(this.getAssemblageForIteration()));
        }
        incomingPossibleComponents.put(this.getAssemblageForIteration(), getResultsCache());
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
        return ITERATION;
    }

    /**
     * Gets the query matches.
     *
     * @param conceptVersion the concept version
     */
    @Override
    public void getQueryMatches(ConceptVersion conceptVersion) {
        getResultsCache();
    }

    @Override
    public ClauseSemantic getClauseSemantic() {
        return ClauseSemantic.COMPONENT_IS_ACTIVE;
    }

    /**
     * Gets the where clause.
     *
     * @return the where clause
     */
    @Override
    public WhereClause getWhereClause() {
        final WhereClause whereClause = new WhereClause();

        whereClause.setSemantic(ClauseSemantic.COMPONENT_IS_ACTIVE);
        return whereClause;
    }

    @Override
    public ConceptSpecification getClauseConcept() {
        return TermAux.ACTIVE_QUERY_CLAUSE;
    }

    public StampCoordinate getStampCoordinate() {
        return stampCoordinate;
    }

    public void setStampCoordinate(StampCoordinate stampCoordinate) {
        this.stampCoordinate = stampCoordinate;
    }
}
