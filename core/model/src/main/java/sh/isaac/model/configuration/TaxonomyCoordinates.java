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



package sh.isaac.model.configuration;

//~--- non-JDK imports --------------------------------------------------------

import sh.isaac.api.Get;
import sh.isaac.api.coordinate.LanguageCoordinate;
import sh.isaac.api.coordinate.LogicCoordinate;
import sh.isaac.api.coordinate.PremiseType;
import sh.isaac.api.coordinate.StampCoordinate;
import sh.isaac.api.coordinate.TaxonomyCoordinate;
import sh.isaac.model.coordinate.TaxonomyCoordinateImpl;

//~--- classes ----------------------------------------------------------------

/**
 * The Class TaxonomyCoordinates.
 *
 * @author kec
 */
public class TaxonomyCoordinates {
   /**
    * Uses the default logic coordinate.
    *
    * @param stampCoordinate the stamp coordinate
    * @param languageCoordinate the language coordinate
    * @return the inferred taxonomy coordinate
    */
   public static TaxonomyCoordinate getInferredTaxonomyCoordinate(StampCoordinate stampCoordinate,
         LanguageCoordinate languageCoordinate) {
      return new TaxonomyCoordinateImpl(PremiseType.INFERRED,
                                        stampCoordinate,
                                        languageCoordinate,
                                        Get.configurationService().getDefaultLogicCoordinate());
   }

   /**
    * Gets the inferred taxonomy coordinate.
    *
    * @param stampCoordinate the stamp coordinate
    * @param languageCoordinate the language coordinate
    * @param logicCoordinate the logic coordinate
    * @return the inferred taxonomy coordinate
    */
   public static TaxonomyCoordinate getInferredTaxonomyCoordinate(StampCoordinate stampCoordinate,
         LanguageCoordinate languageCoordinate,
         LogicCoordinate logicCoordinate) {
      return new TaxonomyCoordinateImpl(PremiseType.INFERRED, stampCoordinate, languageCoordinate, logicCoordinate);
   }

   /**
    * Uses the default logic coordinate.
    *
    * @param stampCoordinate the stamp coordinate
    * @param languageCoordinate the language coordinate
    * @return the stated taxonomy coordinate
    */
   public static TaxonomyCoordinate getStatedTaxonomyCoordinate(StampCoordinate stampCoordinate,
         LanguageCoordinate languageCoordinate) {
      return new TaxonomyCoordinateImpl(PremiseType.STATED,
                                        stampCoordinate,
                                        languageCoordinate,
                                        Get.configurationService().getDefaultLogicCoordinate());
   }

   /**
    * Gets the stated taxonomy coordinate.
    *
    * @param stampCoordinate the stamp coordinate
    * @param languageCoordinate the language coordinate
    * @param logicCoordinate the logic coordinate
    * @return the stated taxonomy coordinate
    */
   public static TaxonomyCoordinate getStatedTaxonomyCoordinate(StampCoordinate stampCoordinate,
         LanguageCoordinate languageCoordinate,
         LogicCoordinate logicCoordinate) {
      return new TaxonomyCoordinateImpl(PremiseType.STATED, stampCoordinate, languageCoordinate, logicCoordinate);
   }
}

