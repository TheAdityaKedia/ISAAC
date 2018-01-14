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
package sh.isaac.api.coordinate;

//~--- JDK imports ------------------------------------------------------------
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

//~--- non-JDK imports --------------------------------------------------------
import sh.isaac.api.Get;
import sh.isaac.api.bootstrap.TermAux;
import sh.isaac.api.chronicle.LatestVersion;
import sh.isaac.api.component.semantic.version.DescriptionVersion;
import sh.isaac.api.component.semantic.SemanticChronology;

//~--- interfaces -------------------------------------------------------------
/**
 * Coordinate to manage the retrieval and display of language and dialect information.
 *
 * Created by kec on 2/16/15.
 */
public interface LanguageCoordinate extends Coordinate {

    /**
     * If the current language coordinate fails to return a requested description, 
     * then the next priority language coordinate will be tried until a description is found, 
     * or until there are no next priority language coordinates left. 
     * 
     * @return 
     */
    Optional<LanguageCoordinate> getNextProrityLanguageCoordinate();
    
    /**
     * 
     * @param languageCoordinate the next in priority language coordinate. 
     */
    void setNextProrityLanguageCoordinate(LanguageCoordinate languageCoordinate);
   /**
    * Return the latestDescription according to the type and dialect preferences of this {@code LanguageCoordinate}.
    *
    * @param descriptionList descriptions to consider
    * @param stampCoordinate the stamp coordinate
    * @return an optional latestDescription best matching the {@code LanguageCoordinate} constraints.
    */
   LatestVersion<DescriptionVersion> getDescription(
           List<SemanticChronology> descriptionList,
           StampCoordinate stampCoordinate);

   /**
    * Return the latestDescription according to the type and dialect preferences of this {@code LanguageCoordinate}.
    *
    * @param conceptNid the concept nid. 
    * @param stampCoordinate the stamp coordinate
    * @return an optional latestDescription best matching the {@code LanguageCoordinate} constraints.
    */
   default LatestVersion<DescriptionVersion> getDescription(int conceptNid, StampCoordinate stampCoordinate) {
      return getDescription(Get.conceptService().getConceptDescriptions(conceptNid), stampCoordinate);
   }

   /**
    * Gets the latestDescription type preference list.
    *
    * @return the latestDescription type preference list
    */
   int[] getDescriptionTypePreferenceList();

   /**
    * Gets the latestDescription type preference list.
    *
    * @param descriptionTypePreferenceList
    */
   void setDescriptionTypePreferenceList(int[] descriptionTypePreferenceList);

   /**
    * Gets the dialect assemblage preference list.
    *
    * @return the dialect assemblage preference list
    */
   int[] getDialectAssemblagePreferenceList();

   /**
    * Convenience method - returns true if FQN is at the top of the latestDescription list.
    *
    * @return true, if FQN preferred
    */
   public default boolean isFQNPreferred() {
      for (final int descType : getDescriptionTypePreferenceList()) {
         if (descType
                 == Get.identifierService().getNidForUuids(
                         TermAux.FULLY_QUALIFIED_NAME_DESCRIPTION_TYPE.getPrimordialUuid())) {
            return true;
         }

         break;
      }

      return false;
   }

   /**
    * Gets the fully specified latestDescription.
    *
    * @param descriptionList the latestDescription list
    * @param stampCoordinate the stamp coordinate
    * @return the fully specified latestDescription
    */
   LatestVersion<DescriptionVersion> getFullySpecifiedDescription(
           List<SemanticChronology> descriptionList,
           StampCoordinate stampCoordinate);

   /**
    * Gets the fully specified latestDescription.
    *
    * @param conceptId the conceptId to get the fully specified latestDescription for
    * @param stampCoordinate the stamp coordinate
    * @return the fully specified latestDescription
    */
   default LatestVersion<DescriptionVersion> getFullySpecifiedDescription(
           int conceptId,
           StampCoordinate stampCoordinate) {
      return getFullySpecifiedDescription(Get.conceptService().getConceptDescriptions(conceptId), stampCoordinate);
   }

   /**
    * Gets the fully specified latestDescription text.
    *
    * @param conceptId the conceptId to get the fully specified latestDescription for
    * @param stampCoordinate the stamp coordinate
    * @return the fully specified latestDescription
    */
   default String getFullySpecifiedDescriptionText(
           int conceptId,
           StampCoordinate stampCoordinate) {
      LatestVersion<DescriptionVersion> latestDescription
              = getFullySpecifiedDescription(Get.conceptService().getConceptDescriptions(conceptId), stampCoordinate);
      if (latestDescription.isPresent()) {
         return latestDescription.get().getText();
      } else {
         return "No description for: " + conceptId;
      }
   }

   /**
    * Gets the language concept sequence.
    *
    * @return the language concept sequence
    */
   int getLanguageConceptNid();

   /**
    * Gets the preferred latestDescription.
    *
    * @param descriptionList the latestDescription list
    * @param stampCoordinate the stamp coordinate
    * @return the preferred latestDescription
    */
   LatestVersion<DescriptionVersion> getPreferredDescription(
           List<SemanticChronology> descriptionList,
           StampCoordinate stampCoordinate);

   /**
    * Gets the preferred description.
    *
    * @param conceptId the conceptId to get the fully specified latestDescription for
    * @param stampCoordinate the stamp coordinate
    * @return the fully specified latestDescription
    */
   default LatestVersion<DescriptionVersion> getPreferredDescription(
           int conceptId,
           StampCoordinate stampCoordinate) {
      return getPreferredDescription(Get.conceptService().getConceptDescriptions(conceptId), stampCoordinate);
   }

   /**
    * Gets the preferred description text.
    *
    * @param conceptId the conceptId to get the fully specified latestDescription for
    * @param stampCoordinate the stamp coordinate
    * @return the fully specified latestDescription
    */
   default String getPreferredDescriptionText(
           int conceptId,
           StampCoordinate stampCoordinate) {
      if (conceptId < 0) {
         switch (Get.identifierService().getObjectTypeForComponent(conceptId)) {
            case CONCEPT:
               // returned below
               break;
            case SEMANTIC:
               return Get.assemblageService().getSemanticChronology(conceptId).getVersionType().toString();
            case UNKNOWN:
               return "unknown for nid: " + conceptId;
            default:
               return "unknown type for nid: " + conceptId;
         }
      }
       try {
           LatestVersion<DescriptionVersion> latestDescription
                   = getPreferredDescription(Get.conceptService().getConceptDescriptions(conceptId), stampCoordinate);
           if (latestDescription.isPresent()) {
               return latestDescription.get().getText();
           } else {
               return "No description for: " + conceptId;
           }
       } catch (NoSuchElementException e) {
//           List<UUID> uuids = Get.identifierService().getUuidsForNid(conceptId);
//           return "No concept for: " + conceptId + " " + uuids;
             return "No concept for: " + conceptId;
       }
   }

   @Override
   public LanguageCoordinate deepClone();

}
