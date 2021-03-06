/*
 * Copyright 2017 Organizations participating in ISAAC, ISAAC's KOMET, and SOLOR development include the
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
package sh.isaac.solor.direct;

import java.time.format.DateTimeFormatter;
import static java.time.temporal.ChronoField.INSTANT_SECONDS;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import sh.isaac.MetaData;
import sh.isaac.api.AssemblageService;
import sh.isaac.api.Get;
import sh.isaac.api.IdentifierService;
import sh.isaac.api.LookupService;
import sh.isaac.api.Status;
import sh.isaac.api.bootstrap.TermAux;
import sh.isaac.api.chronicle.Chronology;
import sh.isaac.api.chronicle.VersionType;
import sh.isaac.api.commit.StampService;
import sh.isaac.api.index.IndexBuilderService;
import sh.isaac.api.task.TimedTaskWithProgressTracker;
import sh.isaac.api.util.UuidT3Generator;
import sh.isaac.api.util.UuidT5Generator;
import sh.isaac.model.configuration.LanguageCoordinates;
import sh.isaac.model.semantic.SemanticChronologyImpl;
import sh.isaac.model.semantic.version.DescriptionVersionImpl;
import sh.isaac.model.semantic.version.StringVersionImpl;

/**
 *
 * @author kec
 */
public class DescriptionWriter extends TimedTaskWithProgressTracker<Void> {

   /*
id	effectiveTime	active	moduleId	conceptId	languageCode	typeId	term	caseSignificanceId
101013	20020131	1	900000000000207008	126813005	en	900000000000013009	Neoplasm of anterior aspect of epiglottis	900000000000020002
101013	20170731	1	900000000000207008	126813005	en	900000000000013009	Neoplasm of anterior aspect of epiglottis	900000000000448009
102018	20020131	1	900000000000207008	126814004	en	900000000000013009	Neoplasm of junctional region of epiglottis	900000000000020002
102018	20170731	1	900000000000207008	126814004	en	900000000000013009	Neoplasm of junctional region of epiglottis	900000000000448009
    */

   private static final int RF2_DESCRIPITON_SCT_ID_INDEX = 0;
   private static final int RF2_EFFECTIVE_TIME_INDEX = 1;
   private static final int RF2_ACTIVE_INDEX = 2; // 0 == false, 1 == true
   private static final int RF2_MODULE_SCTID_INDEX = 3;
   private static final int RF2_REFERENCED_CONCEPT_SCT_ID_INDEX = 4;
   private static final int RF2_LANGUGE_CODE_INDEX = 5;
   private static final int RF2_DESCRIPTION_TYPE_SCT_ID_INDEX = 6;
   private static final int RF2_DESCRIPTION_TEXT_INDEX = 7;
   private static final int RF2_CASE_SIGNIFICANCE_INDEX = 8;

   private static final int SRF_ID_INDEX = 0;
   private static final int SRF_STATUS_INDEX = 1;
   private static final int SRF_TIME_INDEX = 2; // 0 == false, 1 == true
   private static final int SRF_AUTHOR_INDEX = 3;
   private static final int SRF_MODULE_INDEX = 4;
   private static final int SRF_PATH_INDEX = 5;
   private static final int SRF_REFERENCED_CONCEPT_ID_INDEX = 6;
   private static final int SRF_LANGUAGE_CODE_INDEX = 7;
   private static final int SRF_DESCRIPTION_TYPE_ID_INDEX = 8;
   private static final int SRF_TERM_INDEX = 9;
   private static final int SRF_CASE_SIGNIFICANCE_ID_INDEX = 10;

   private final List<String[]> descriptionRecords;
   private final Semaphore writeSemaphore;
   private final List<IndexBuilderService> indexers;
   private final ImportType importType;
   private final boolean solorReleaseFormat;

   public DescriptionWriter(List<String[]> descriptionRecords, 
           Semaphore writeSemaphore, String message, ImportType importType, boolean solorReleaseFormat) {
      this.descriptionRecords = descriptionRecords;
      this.writeSemaphore = writeSemaphore;
      this.writeSemaphore.acquireUninterruptibly();
      this.solorReleaseFormat = solorReleaseFormat;
      indexers = LookupService.get().getAllServices(IndexBuilderService.class);
      updateTitle("Importing description batch of size: " + descriptionRecords.size());
      updateMessage(message);
      addToTotalWork(descriptionRecords.size());
      this.importType = importType;
      Get.activeTasks().add(this);
   }
   
   private void index(Chronology chronicle) {
      for (IndexBuilderService indexer: indexers) {
         indexer.indexNow(chronicle);
      }
   }
   @Override
   protected Void call() throws Exception {
      try {
         AssemblageService assemblageService = Get.assemblageService();
         IdentifierService identifierService = Get.identifierService();
         StampService stampService = Get.stampService();
         int identifierAssemblageNid;
         int authorNid = 1;
         int pathNid = 1;

         if(this.solorReleaseFormat){
            identifierAssemblageNid = MetaData.UUID____SOLOR.getNid(); //TODO Needs to be SOLOR Identifier or SOLORID :)
         }else{
            identifierAssemblageNid = TermAux.SNOMED_IDENTIFIER.getNid();
            authorNid = TermAux.USER.getNid();
            pathNid = TermAux.DEVELOPMENT_PATH.getNid();
         }

         for (String[] descriptionRecord : descriptionRecords) {
            final Status state = this.solorReleaseFormat
                    ? Status.fromZeroOneToken(descriptionRecord[SRF_STATUS_INDEX])
                    : Status.fromZeroOneToken(descriptionRecord[RF2_ACTIVE_INDEX]);
            if (state == Status.INACTIVE && importType == ImportType.ACTIVE_ONLY) {
                continue;
            }
            UUID referencedConceptUuid = this.solorReleaseFormat
                    ? UUID.fromString(descriptionRecord[SRF_REFERENCED_CONCEPT_ID_INDEX])
                    : UuidT3Generator.fromSNOMED(descriptionRecord[RF2_REFERENCED_CONCEPT_SCT_ID_INDEX]);
            if (importType == ImportType.ACTIVE_ONLY) {
                if (!identifierService.hasUuid(referencedConceptUuid)) {
                    // if concept was not imported because inactive, then skip
                    continue;
                }
            }

            int descriptionAssemblageNid, languageNid;
            UUID descriptionUuid, moduleUuid, caseSignificanceUuid, descriptionTypeUuid;
            TemporalAccessor accessor;

            if(this.solorReleaseFormat){
               authorNid = identifierService.getNidForUuids(UUID.fromString(descriptionRecord[SRF_AUTHOR_INDEX]));
               pathNid = identifierService.getNidForUuids(UUID.fromString(descriptionRecord[SRF_PATH_INDEX]));
               descriptionAssemblageNid = LanguageCoordinates.iso639toDescriptionAssemblageNid(descriptionRecord[SRF_LANGUAGE_CODE_INDEX]);
               languageNid = LanguageCoordinates.iso639toConceptNid(descriptionRecord[SRF_LANGUAGE_CODE_INDEX]);
               descriptionUuid = UUID.fromString(descriptionRecord[SRF_ID_INDEX]);
               moduleUuid = UUID.fromString(descriptionRecord[SRF_MODULE_INDEX]);
               caseSignificanceUuid = UUID.fromString(descriptionRecord[SRF_CASE_SIGNIFICANCE_ID_INDEX]);
               descriptionTypeUuid = UUID.fromString(descriptionRecord[SRF_DESCRIPTION_TYPE_ID_INDEX]);
               accessor = DateTimeFormatter.ISO_INSTANT.parse(DirectImporter.getIsoInstant(descriptionRecord[SRF_TIME_INDEX]));
            }else{
               descriptionAssemblageNid = LanguageCoordinates.iso639toDescriptionAssemblageNid(descriptionRecord[RF2_LANGUGE_CODE_INDEX]);
               languageNid = LanguageCoordinates.iso639toConceptNid(descriptionRecord[RF2_LANGUGE_CODE_INDEX]);
               descriptionUuid = UuidT3Generator.fromSNOMED(descriptionRecord[RF2_DESCRIPITON_SCT_ID_INDEX]);
               moduleUuid = UuidT3Generator.fromSNOMED(descriptionRecord[RF2_MODULE_SCTID_INDEX]);
               caseSignificanceUuid = UuidT3Generator.fromSNOMED(descriptionRecord[RF2_CASE_SIGNIFICANCE_INDEX]);
               descriptionTypeUuid = UuidT3Generator.fromSNOMED(descriptionRecord[RF2_DESCRIPTION_TYPE_SCT_ID_INDEX]);
               accessor = DateTimeFormatter.ISO_INSTANT.parse(DirectImporter.getIsoInstant(descriptionRecord[RF2_EFFECTIVE_TIME_INDEX]));
            }

            long time = accessor.getLong(INSTANT_SECONDS) * 1000;
            
            // add to description assemblage
            int moduleNid = identifierService.getNidForUuids(moduleUuid);
            int referencedConceptNid = identifierService.getNidForUuids(referencedConceptUuid);
            int caseSignificanceNid = identifierService.getNidForUuids(caseSignificanceUuid);
            int descriptionTypeNid = identifierService.getNidForUuids(descriptionTypeUuid);
                        
            SemanticChronologyImpl descriptionToWrite = 
                    new SemanticChronologyImpl(VersionType.DESCRIPTION, descriptionUuid, descriptionAssemblageNid, referencedConceptNid);
            int conceptStamp = stampService.getStampSequence(state, time, authorNid, moduleNid, pathNid);
            DescriptionVersionImpl descriptionVersion = descriptionToWrite.createMutableVersion(conceptStamp);
            descriptionVersion.setCaseSignificanceConceptNid(caseSignificanceNid);
            descriptionVersion.setDescriptionTypeConceptNid(descriptionTypeNid);
            descriptionVersion.setLanguageConceptNid(languageNid);
            descriptionVersion.setText(this.solorReleaseFormat
                    ? descriptionRecord[SRF_TERM_INDEX]
                    : descriptionRecord[RF2_DESCRIPTION_TEXT_INDEX]);
            
            index(descriptionToWrite);
            assemblageService.writeSemanticChronology(descriptionToWrite);

            // add to sct identifier assemblage
            UUID identifierUuid;

            if(this.solorReleaseFormat){
               identifierUuid = UuidT5Generator.get(MetaData.UUID____SOLOR.getPrimordialUuid(),
                       descriptionRecord[SRF_ID_INDEX]);
            }else{
               identifierUuid = UuidT5Generator.get(TermAux.SNOMED_IDENTIFIER.getPrimordialUuid(),
                       descriptionRecord[RF2_DESCRIPITON_SCT_ID_INDEX]);
            }

            SemanticChronologyImpl sctIdentifierToWrite = new SemanticChronologyImpl(VersionType.STRING,
                               identifierUuid,
                               identifierAssemblageNid,
                               descriptionToWrite.getNid());
            
            StringVersionImpl idVersion = sctIdentifierToWrite.createMutableVersion(conceptStamp);
            idVersion.setString(this.solorReleaseFormat
                    ? descriptionRecord[SRF_ID_INDEX]
                    : descriptionRecord[RF2_DESCRIPITON_SCT_ID_INDEX]);
            index(sctIdentifierToWrite);
            assemblageService.writeSemanticChronology(sctIdentifierToWrite);
            completedUnitOfWork();
         }

         return null;
      } finally {
         this.writeSemaphore.release();
         Get.activeTasks().remove(this);
      }
   }
}
