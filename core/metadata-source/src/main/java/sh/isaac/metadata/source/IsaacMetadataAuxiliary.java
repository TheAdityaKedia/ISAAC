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



package sh.isaac.metadata.source;

//~--- JDK imports ------------------------------------------------------------

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.UnsupportedEncodingException;

import java.security.NoSuchAlgorithmException;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Singleton;

//~--- non-JDK imports --------------------------------------------------------

import org.glassfish.hk2.api.Rank;

import org.jvnet.hk2.annotations.Service;

import sh.isaac.api.IsaacTaxonomy;
import sh.isaac.api.bootstrap.TermAux;
import sh.isaac.api.component.concept.ConceptBuilder;
import sh.isaac.api.component.sememe.version.dynamicSememe.DynamicSememeColumnInfo;
import sh.isaac.api.component.sememe.version.dynamicSememe.DynamicSememeDataType;
import sh.isaac.api.constants.DynamicSememeConstants;
import sh.isaac.api.constants.MetadataDynamicSememeConstant;
import sh.isaac.api.logic.NodeSemantic;
import sh.isaac.model.observable.ObservableFields;

import static sh.isaac.model.observable.ObservableFields.ALLOWED_STATES_FOR_STAMP_COORDINATE;
import static sh.isaac.model.observable.ObservableFields.ASSEMBLAGE_SEQUENCE_FOR_SEMEME_CHRONICLE;
import static sh.isaac.model.observable.ObservableFields.AUTHOR_SEQUENCE_FOR_EDIT_COORDINATE;
import static sh.isaac.model.observable.ObservableFields.AUTHOR_SEQUENCE_FOR_VERSION;
import static sh.isaac.model.observable.ObservableFields.CASE_SIGNIFICANCE_CONCEPT_SEQUENCE_FOR_DESCRIPTION;
import static sh.isaac.model.observable.ObservableFields.CLASSIFIER_SEQUENCE_FOR_LOGIC_COORDINATE;
import static sh.isaac.model.observable.ObservableFields.COMMITTED_STATE_FOR_CHRONICLE;
import static sh.isaac.model.observable.ObservableFields.COMMITTED_STATE_FOR_VERSION;
import static sh.isaac.model.observable.ObservableFields.CONCEPT_SEQUENCE_FOR_CHRONICLE;
import static sh.isaac.model.observable.ObservableFields.DESCRIPTION_LIST_FOR_CONCEPT;
import static sh.isaac.model.observable.ObservableFields.DESCRIPTION_LOGIC_PROFILE_SEQUENCE_FOR_LOGIC_COORDINATE;
import static sh.isaac.model.observable.ObservableFields.DESCRIPTION_TYPE_FOR_DESCRIPTION;
import static sh.isaac.model.observable.ObservableFields
   .DESCRIPTION_TYPE_SEQUENCE_PREFERENCE_LIST_FOR_LANGUAGE_COORDINATE;
import static sh.isaac.model.observable.ObservableFields
   .DIALECT_ASSEMBLAGE_SEQUENCE_PREFERENCE_LIST_FOR_LANGUAGE_COORDINATE;
import static sh.isaac.model.observable.ObservableFields.INFERRED_ASSEMBLAGE_SEQUENCE_FOR_LOGIC_COORDINATE;
import static sh.isaac.model.observable.ObservableFields.LANGUAGE_CONCEPT_SEQUENCE_FOR_DESCRIPTION;
import static sh.isaac.model.observable.ObservableFields.LANGUAGE_COORDINATE_FOR_TAXONOMY_COORDINATE;
import static sh.isaac.model.observable.ObservableFields.LANGUAGE_SEQUENCE_FOR_LANGUAGE_COORDINATE;
import static sh.isaac.model.observable.ObservableFields.LOGIC_COORDINATE_FOR_TAXONOMY_COORDINATE;
import static sh.isaac.model.observable.ObservableFields.MODULE_SEQUENCE_ARRAY_FOR_STAMP_COORDINATE;
import static sh.isaac.model.observable.ObservableFields.MODULE_SEQUENCE_FOR_EDIT_COORDINATE;
import static sh.isaac.model.observable.ObservableFields.MODULE_SEQUENCE_FOR_VERSION;
import static sh.isaac.model.observable.ObservableFields.NATIVE_ID_FOR_CHRONICLE;
import static sh.isaac.model.observable.ObservableFields.PATH_ORIGIN_LIST_FOR_STAMP_PATH;
import static sh.isaac.model.observable.ObservableFields.PATH_SEQUENCE_FOR_EDIT_CORDINATE;
import static sh.isaac.model.observable.ObservableFields.PATH_SEQUENCE_FOR_STAMP_PATH;
import static sh.isaac.model.observable.ObservableFields.PATH_SEQUENCE_FOR_STAMP_POSITION;
import static sh.isaac.model.observable.ObservableFields.PATH_SEQUENCE_FOR_VERSION;
import static sh.isaac.model.observable.ObservableFields.PREMISE_TYPE_FOR_TAXONOMY_COORDINATE;
import static sh.isaac.model.observable.ObservableFields.PRIMORDIAL_UUID_FOR_CHRONICLE;
import static sh.isaac.model.observable.ObservableFields.SEMEME_LIST_FOR_CHRONICLE;
import static sh.isaac.model.observable.ObservableFields.SEMEME_SEQUENCE_FOR_CHRONICLE;
import static sh.isaac.model.observable.ObservableFields.STAMP_COORDINATE_FOR_TAXONOMY_COORDINATE;
import static sh.isaac.model.observable.ObservableFields.STAMP_POSITION_FOR_STAMP_COORDINATE;
import static sh.isaac.model.observable.ObservableFields.STAMP_PRECEDENCE_FOR_STAMP_COORDINATE;
import static sh.isaac.model.observable.ObservableFields.STAMP_SEQUENCE_FOR_VERSION;
import static sh.isaac.model.observable.ObservableFields.STATED_ASSEMBLAGE_SEQUENCE_FOR_LOGIC_COORDINATE;
import static sh.isaac.model.observable.ObservableFields.STATUS_FOR_VERSION;
import static sh.isaac.model.observable.ObservableFields.TEXT_FOR_DESCRIPTION;
import static sh.isaac.model.observable.ObservableFields.TIME_FOR_STAMP_POSITION;
import static sh.isaac.model.observable.ObservableFields.TIME_FOR_VERSION;
import static sh.isaac.model.observable.ObservableFields.UUID_FOR_TAXONOMY_COORDINATE;
import static sh.isaac.model.observable.ObservableFields.UUID_LIST_FOR_CHRONICLE;
import static sh.isaac.model.observable.ObservableFields.VERSION_LIST_FOR_CHRONICLE;

//~--- classes ----------------------------------------------------------------

/**
 * The Class IsaacMetadataAuxiliary.
 *
 * @author kec
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
@Service
@Rank(value = 10)
@Singleton
public class IsaacMetadataAuxiliary
        extends IsaacTaxonomy {
   /** The Constant METADATA_SEMANTIC_TAG. */
   public static final String METADATA_SEMANTIC_TAG = "SOLOR";

   //~--- constructors --------------------------------------------------------

   /**
    * If you are looking for the code that creates / uses this, see the class {@link ExportTaxonomy}
    * To override this class with a different taxonomy, provide another implementation with a higher rank.
    *
    * @throws NoSuchAlgorithmException the no such algorithm exception
    * @throws UnsupportedEncodingException the unsupported encoding exception
    */
   public IsaacMetadataAuxiliary()
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
      super(TermAux.DEVELOPMENT_PATH, TermAux.USER, TermAux.SOLOR_MODULE, TermAux.IS_A, METADATA_SEMANTIC_TAG);

      try {
         createConcept(TermAux.SOLOR_ROOT);
         pushParent(current());
         createConcept("Health concept").setPrimordialUuid("ee9ac5d2-a07c-3981-a57a-f7f26baf38d8");
         pushParent(current());
         createConcept("Phenomenon");
         popParent();
         createConcept(TermAux.SOLOR_METADATA);
         pushParent(current());
         createConcept("Module").mergeFromSpec(TermAux.UNSPECIFIED_MODULE);
         pushParent(current());
         createConcept(TermAux.SOLOR_MODULE);
         createConcept("SNOMED CT core modules").setPrimordialUuid("1b4f1ba5-b725-390f-8c3b-33ec7096bdca");
         createConcept("US Extension modules");
         createConcept("LOINC modules");
         createConcept("LOINC Solor modules");
         createConcept("RxNorm modules");
         createConcept("RxNorm Solor modules");
         createConcept("Generated administration of module");
         createConcept("SOLOR quality assurance rule module");
         createConcept("SOLOR automation rule module");

         // The second UUID here was the old value from the TermAux - but this was an orphan.  to best fix the bug that resulted,
         // the type5 UUID from here was moved to TermAux, and the old UUID was added here as an additional.
         createConcept(TermAux.VHA_MODULES).addUuids(UUID.fromString("1f201520-960e-11e5-8994-feff819cdc9f"));

         // The second UUID here was the old value from the TermAux - but this was an orphan.  to best fix the bug that resulted,
         // the type5 UUID from here was moved to TermAux, and the old UUID was added here as an additional.
         createConcept(TermAux.SOLOR_OVERLAY_MODULE).addUuids(UUID.fromString("1f2016a6-960e-11e5-8994-feff819cdc9f"));
         createConcept("HL7v3 modules");
         createConcept("NUCC modules");
         createConcept("CVX modules");
         createConcept("MVX modules");
         createConcept("CPT modules");
         popParent();
         createConcept(TermAux.USER);
         createConcept(TermAux.PATH);
         pushParent(current());

         final ConceptBuilder developmentPath = createConcept(TermAux.DEVELOPMENT_PATH);
         final ConceptBuilder masterPath      = createConcept(TermAux.MASTER_PATH);

         masterPath.addUuids(UUID.fromString("2faa9260-8fb2-11db-b606-0800200c9a66"));  // UUID from WB_AUX_PATH
         popParent();
         createConcept("SNOMED DL concept operator");
         pushParent(current());
         createConcept(TermAux.SUFFICIENT_CONCEPT_DEFINITION);
         createConcept(TermAux.NECESSARY_BUT_NOT_SUFFICIENT_CONCEPT_DEFINITION);
         popParent();
         createConcept("EL profile set operator");
         pushParent(current());
         createConcept("Sufficient set").setPrimordialUuid(NodeSemantic.SUFFICIENT_SET.getSemanticUuid());
         createConcept("Necessary set").setPrimordialUuid(NodeSemantic.NECESSARY_SET.getSemanticUuid());
         popParent();
         createConcept("Identifier source");
         pushParent(current());
         createConcept("SCTID").mergeFromSpec(TermAux.SNOMED_IDENTIFIER);
         createConcept("Generated UUID").setPrimordialUuid("2faa9262-8fb2-11db-b606-0800200c9a66");
         createConcept(new MetadataDynamicSememeConstant("LOINC_NUM",
               null,
               "LOINC Identifier",
               "Carries the LOINC_NUM native identifier",
               new DynamicSememeColumnInfo[] { new DynamicSememeColumnInfo(0,
                     DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMN_VALUE.getPrimordialUuid(),
                     DynamicSememeDataType.STRING,
                     null,
                     true,
                     true) }));
         createConcept("RXCUI").setPrimordialUuid(
             "617761d2-80ef-5585-83a0-60851dd44158");  // comes from the algorithm in the rxnorm econ loader
         createConcept("VUID", "Vets Unique Identifier");
         createConcept("OID", "HL7 Object Identifier");
         createConcept("Code").setPrimordialUuid(
             "803af596-aea8-5184-b8e1-45f801585d17");  // comes from the algorithm in the VHAT econ loader
         createConcept("CVXCode", "CVX Unique Identifier");
         createConcept("MVX_CODE", "MVX Unique Identifier");
         popParent();
         createConcept("language");
         pushParent(current());
         createConcept(TermAux.ENGLISH_LANGUAGE);
         createConcept(TermAux.SPANISH_LANGUAGE);
         createConcept(TermAux.FRENCH_LANGUAGE);
         createConcept(TermAux.DANISH_LANGUAGE);
         createConcept(TermAux.POLISH_LANGUAGE);
         createConcept(TermAux.DUTCH_LANGUAGE);
         createConcept(TermAux.LITHUANIAN_LANGUAGE);
         createConcept(TermAux.CHINESE_LANGUAGE);
         createConcept(TermAux.JAPANESE_LANGUAGE);
         createConcept(TermAux.SWEDISH_LANGUAGE);
         popParent();
         createConcept("Assemblage membership type");
         pushParent(current());
         createConcept("Normal member").setPrimordialUuid("cc624429-b17d-4ac5-a69e-0b32448aaf3c");
         createConcept("Marked parent").setPrimordialUuid("125f3d04-de17-490e-afec-1431c2a39e29");
         popParent();
         createConcept("Annotation type");
         pushParent(current());
         createConcept("Content issue");
         createConcept("Komet issue");
         createConcept("Quality assurance rule issue");
         createConcept("Automation issue");
         popParent();
         createConcept(TermAux.ASSEMBLAGE);
         pushParent(current());
         createConcept("Issue managment assemblage");
         pushParent(current());
            createConcept("Content issue assemblage");
            createConcept("KOMET issue assemblage");
            createConcept("Quality assurance rule issue assemblage");
            createConcept("Automation issue assemblage");
            createConcept("Clinical statement issue assemblage");
            createConcept("SNOMED issue assemblage");
            createConcept("LOINC issue assemblage");
            createConcept("RxNorm issue assemblage");
            createConcept("SOLOR issue assemblage");
         popParent();
         createConcept(TermAux.DESCRIPTION_ASSEMBLAGE);
         pushParent(current());
         createConcept(TermAux.ENGLISH_DESCRIPTION_ASSEMBLAGE);
         createConcept(TermAux.SPANISH_DESCRIPTION_ASSEMBLAGE);
         createConcept(TermAux.FRENCH_DESCRIPTION_ASSEMBLAGE);
         createConcept(TermAux.DANISH_DESCRIPTION_ASSEMBLAGE);
         createConcept(TermAux.POLISH_DESCRIPTION_ASSEMBLAGE);
         createConcept(TermAux.DUTCH_DESCRIPTION_ASSEMBLAGE);
         createConcept(TermAux.LITHUANIAN_DESCRIPTION_ASSEMBLAGE);
         createConcept(TermAux.CHINESE_DESCRIPTION_ASSEMBLAGE);
         createConcept(TermAux.JAPANESE_DESCRIPTION_ASSEMBLAGE);
         createConcept(TermAux.SWEDISH_DESCRIPTION_ASSEMBLAGE);
         popParent();
         createConcept("Dialect assemblage");
         pushParent(current());
         createConcept("US English dialect").mergeFromSpec(TermAux.US_DIALECT_ASSEMBLAGE);
         createConcept("GB English dialect").mergeFromSpec(TermAux.GB_DIALECT_ASSEMBLAGE);
         createConcept("US Nursing dialect");
         popParent();
         createConcept("Logic assemblage");
         pushParent(current());
         createConcept(TermAux.EL_PLUS_PLUS_STATED_ASSEMBLAGE);
         createConcept(TermAux.EL_PLUS_PLUS_INFERRED_ASSEMBLAGE);
         popParent();
         createConcept("Rule assemblage");
         pushParent(current());
         createConcept("Module assemblage");
         createConcept("Quality assurance rule assemblage");
         createConcept("Automation rule assemblage");
         popParent();
         createConcept("Assemblage related to path management");
         pushParent(current());

         final ConceptBuilder paths = createConcept("Paths assemblage");

         paths.mergeFromSpec(TermAux.PATH_ASSEMBLAGE);
         addPath(paths, masterPath);
         addPath(paths, developmentPath);

         final ConceptBuilder pathOrigins = createConcept("Path origins assemblage");

         pathOrigins.mergeFromSpec(TermAux.PATH_ORIGIN_ASSEMBLAGE);

         // addPathOrigin(pathOrigins, developmentPath, masterPath);
         popParent();
         createConcept("SOLOR assemblage").setPrimordialUuid("7a9b495e-69c1-53e5-a2d5-41be2429c146");
         createConcept("SOLOR Content Metadata");
         pushParent(current());
         createConcept(TermAux.DATABASE_UUID);
         createConcept("Source Artifact Version");
         createConcept("Source Release Date");
         createConcept("Converter Version");
         createConcept("Converted IBDF Artifact Version");
         createConcept("Converted IBDF Artifact Classifier");
         popParent();
         popParent();
         createConcept("Axiom origin");
         pushParent(current());

         final ConceptBuilder stated = createConcept("Stated");

         stated.setPrimordialUuid(
             UUID.fromString(
                "3b0dbd3b-2e53-3a30-8576-6c7fa7773060"));  // merge with "stated relationship" SCT ID:    900000000000010007
         stated.addUuids(
             UUID.fromString(
                 "3fde38f6-e079-3cdc-a819-eda3ec74732d"));  // merge with "stated (defining characteristic type)"

         final ConceptBuilder inferred = createConcept("Inferred");

         inferred.setPrimordialUuid(
             "1290e6ba-48d0-31d2-8d62-e133373c63f5");  // merge with "Inferred" SCT ID:    900000000000011006
         inferred.addUuids(UUID.fromString("a4c6bf72-8fb6-11db-b606-0800200c9a66"));  // merge with ""defining"
         popParent();
         createConcept("Description type");
         pushParent(current());

         final ConceptBuilder fsn = createConcept("Fully specified name");

         fsn.mergeFromSpec(TermAux.FULLY_SPECIFIED_DESCRIPTION_TYPE);
         fsn.addUuids(UUID.fromString("5e1fe940-8faf-11db-b606-0800200c9a66"));       // RF1 FSN

         final ConceptBuilder syn = createConcept(TermAux.SYNONYM_DESCRIPTION_TYPE);

         syn.addUuids(UUID.fromString("d6fad981-7df6-3388-94d8-238cc0465a79"));
         createConcept("Definition description type").mergeFromSpec(TermAux.DEFINITION_DESCRIPTION_TYPE);
         createConcept("Unknown description type");
         
         popParent();
         createConcept(
             TermAux.DESCRIPTION_TYPE_IN_SOURCE_TERMINOLOGY);  // LOINC and RxNorm description types are created under this node
         createConcept(
             TermAux.RELATIONSHIP_TYPE_IN_SOURCE_TERMINOLOGY);  // RxNorm relationship types are created under this node
         createConcept("Description case significance");
         pushParent(current());
         createConcept(TermAux.DESCRIPTION_CASE_SENSITIVE);
         createConcept(TermAux.DESCRIPTION_NOT_CASE_SENSITIVE);
         createConcept(TermAux.DESCRIPTION_INITIAL_CHARACTER_SENSITIVE);
         popParent();
         createConcept("Description acceptability");
         pushParent(current());
         createConcept(TermAux.ACCEPTABLE);
         createConcept(TermAux.PREFERRED);
         popParent();
         createConcept("Taxonomy operator");
         pushParent(current());

         final ConceptBuilder isa = createConcept("Is-a");

         isa.setPrimordialUuid(TermAux.IS_A.getPrimordialUuid());
         isa.addUuids(
             UUID.fromString("c93a30b9-ba77-3adb-a9b8-4589c9f8fb25"));  // merge with "Is a (attribute)" //SCTID 116680003
         popParent();
         createConcept("Connective operator");
         pushParent(current());
         createConcept("And").setPrimordialUuid(NodeSemantic.AND.getSemanticUuid());
         createConcept("Or").setPrimordialUuid(NodeSemantic.OR.getSemanticUuid());
         createConcept("Disjoint with").setPrimordialUuid(NodeSemantic.DISJOINT_WITH.getSemanticUuid());
         createConcept("Definition root").setPrimordialUuid(NodeSemantic.DEFINITION_ROOT.getSemanticUuid());
         popParent();
         createConcept("Node operator");
         pushParent(current());
         createConcept("Template merge");
         createConcept("Field substitution");
         pushParent(current());
         createConcept("Concept substitution").setPrimordialUuid(NodeSemantic.SUBSTITUTION_CONCEPT.getSemanticUuid());
         createConcept("Boolean substitution").setPrimordialUuid(NodeSemantic.SUBSTITUTION_BOOLEAN.getSemanticUuid());
         createConcept("Float substitution").setPrimordialUuid(NodeSemantic.SUBSTITUTION_FLOAT.getSemanticUuid());
         createConcept("Instant substitution").setPrimordialUuid(NodeSemantic.SUBSTITUTION_INSTANT.getSemanticUuid());
         createConcept("Integer substitution").setPrimordialUuid(NodeSemantic.SUBSTITUTION_INTEGER.getSemanticUuid());
         createConcept("string substitution").setPrimordialUuid(NodeSemantic.SUBSTITUTION_STRING.getSemanticUuid());
         popParent();
         createConcept("Concept reference").setPrimordialUuid(NodeSemantic.CONCEPT.getSemanticUuid());
         popParent();
         createConcept("Template concept").setPrimordialUuid(NodeSemantic.TEMPLATE.getSemanticUuid());
         pushParent(current());
         createConcept("Skin of region template");

         // add annotations for order and labels
         // create template
         popParent();
         createConcept("Role operator");
         pushParent(current());
         createConcept("Universal restriction").setPrimordialUuid(NodeSemantic.ROLE_ALL.getSemanticUuid());
         createConcept("Existential restriction").setPrimordialUuid(NodeSemantic.ROLE_SOME.getSemanticUuid());
         popParent();
         createConcept("Feature").setPrimordialUuid(NodeSemantic.FEATURE.getSemanticUuid());
         createConcept("Literal value");
         pushParent(current());
         createConcept("Boolean literal").setPrimordialUuid(NodeSemantic.LITERAL_BOOLEAN.getSemanticUuid());
         createConcept("Float literal").setPrimordialUuid(NodeSemantic.LITERAL_FLOAT.getSemanticUuid());
         createConcept("Instant literal").setPrimordialUuid(NodeSemantic.LITERAL_INSTANT.getSemanticUuid());
         createConcept("Integer literal").setPrimordialUuid(NodeSemantic.LITERAL_INTEGER.getSemanticUuid());
         createConcept("String literal").setPrimordialUuid(NodeSemantic.LITERAL_STRING.getSemanticUuid());
         popParent();
         createConcept("Concrete domain operator");
         pushParent(current());
         createConcept("Greater than");
         createConcept("Greater than or equal to");
         createConcept("Equal to");
         createConcept("Less than or equal to");
         createConcept("Less than");
         popParent();
         createConcept("Description-logic profile");
         pushParent(current());
         createConcept("EL++ profile").mergeFromSpec(TermAux.EL_PLUS_PLUS_LOGIC_PROFILE);
         createConcept("SH profile");
         popParent();
         createConcept("Description-logic classifier");
         pushParent(current());
         createConcept(TermAux.IHTSDO_CLASSIFIER);
         createConcept("SnoRocket classifier").mergeFromSpec(TermAux.SNOROCKET_CLASSIFIER);
         createConcept("ConDOR classifier");
         popParent();
         createConcept("role").setPrimordialUuid("6155818b-09ed-388e-82ce-caa143423e99");
         pushParent(current());
         createConcept("Has strength");
         popParent();
         pushParent(current());
         createConcept("Intrinsic role");
         pushParent(current());
         createConcept(TermAux.ROLE_GROUP);
         popParent();
         popParent();
         createConcept("Unmodeled concept");
         pushParent(current());
         createConcept("Anonymous concept");
         createConcept("Unmodeled role concept");
         createConcept("Unmodeled feature concept");
         createConcept("Unmodeled taxonomic concept");
         popParent();
         createConcept("Object properties");
         pushParent(current());
         createConcept("Coordinate properties");
         pushParent(current());
         createConcept(AUTHOR_SEQUENCE_FOR_EDIT_COORDINATE);
         createConcept(MODULE_SEQUENCE_FOR_EDIT_COORDINATE);
         createConcept(PATH_SEQUENCE_FOR_EDIT_CORDINATE);
         createConcept(LANGUAGE_SEQUENCE_FOR_LANGUAGE_COORDINATE);
         createConcept(DIALECT_ASSEMBLAGE_SEQUENCE_PREFERENCE_LIST_FOR_LANGUAGE_COORDINATE);
         createConcept(DESCRIPTION_TYPE_SEQUENCE_PREFERENCE_LIST_FOR_LANGUAGE_COORDINATE);
         createConcept(STATED_ASSEMBLAGE_SEQUENCE_FOR_LOGIC_COORDINATE);
         createConcept(INFERRED_ASSEMBLAGE_SEQUENCE_FOR_LOGIC_COORDINATE);
         createConcept(DESCRIPTION_LOGIC_PROFILE_SEQUENCE_FOR_LOGIC_COORDINATE);
         createConcept(CLASSIFIER_SEQUENCE_FOR_LOGIC_COORDINATE);
         createConcept(STAMP_PRECEDENCE_FOR_STAMP_COORDINATE);
         createConcept(STAMP_POSITION_FOR_STAMP_COORDINATE);
         createConcept(ALLOWED_STATES_FOR_STAMP_COORDINATE);
         createConcept(MODULE_SEQUENCE_ARRAY_FOR_STAMP_COORDINATE);
         createConcept(PATH_SEQUENCE_FOR_STAMP_PATH);
         createConcept(PATH_ORIGIN_LIST_FOR_STAMP_PATH);
         createConcept(TIME_FOR_STAMP_POSITION);
         createConcept(PATH_SEQUENCE_FOR_STAMP_POSITION);
         createConcept(PREMISE_TYPE_FOR_TAXONOMY_COORDINATE);
         createConcept(UUID_FOR_TAXONOMY_COORDINATE);
         createConcept(STAMP_COORDINATE_FOR_TAXONOMY_COORDINATE);
         createConcept(LANGUAGE_COORDINATE_FOR_TAXONOMY_COORDINATE);
         createConcept(LOGIC_COORDINATE_FOR_TAXONOMY_COORDINATE);
         popParent();
         createConcept("Version properties");
         pushParent(current());
         createConcept(STATUS_FOR_VERSION);
         createConcept(TIME_FOR_VERSION);
         createConcept(AUTHOR_SEQUENCE_FOR_VERSION);
         createConcept(MODULE_SEQUENCE_FOR_VERSION);
         createConcept(PATH_SEQUENCE_FOR_VERSION);
         createConcept(COMMITTED_STATE_FOR_VERSION);
         createConcept(STAMP_SEQUENCE_FOR_VERSION);
         createConcept("Description version properties");
         pushParent(current());
         createConcept(CASE_SIGNIFICANCE_CONCEPT_SEQUENCE_FOR_DESCRIPTION);
         createConcept(LANGUAGE_CONCEPT_SEQUENCE_FOR_DESCRIPTION);
         createConcept(TEXT_FOR_DESCRIPTION);
         createConcept(DESCRIPTION_TYPE_FOR_DESCRIPTION);
         popParent();
         popParent();
         createConcept("Chronicle properties");
         pushParent(current());
         createConcept(VERSION_LIST_FOR_CHRONICLE);
         createConcept(NATIVE_ID_FOR_CHRONICLE);
         createConcept(CONCEPT_SEQUENCE_FOR_CHRONICLE);
         createConcept(SEMEME_SEQUENCE_FOR_CHRONICLE);
         createConcept(PRIMORDIAL_UUID_FOR_CHRONICLE);
         createConcept(UUID_LIST_FOR_CHRONICLE);
         createConcept(COMMITTED_STATE_FOR_CHRONICLE);
         createConcept(SEMEME_LIST_FOR_CHRONICLE);
         createConcept(ASSEMBLAGE_SEQUENCE_FOR_SEMEME_CHRONICLE);
         popParent();
         createConcept("Concept properties");
         pushParent(current());
         createConcept(DESCRIPTION_LIST_FOR_CONCEPT);
         popParent();
         createConcept("Sememe properties");
         pushParent(current());
         createConcept(ObservableFields.STRING_VALUE_FOR_SEMEME);
         createConcept(ObservableFields.COMPONENT_NID_FOR_SEMEME);
         createConcept(ObservableFields.LOGIC_GRAPH_FOR_SEMEME);
         createConcept(ObservableFields.LONG_VALUE_FOR_SEMEME);
         popParent();
         
         
         popParent();
         createConcept("Clinical statement properties");
         pushParent(current());
         createConcept("Topic");
         createConcept("Circumstance");
         popParent();
         createConcept("Circumstance properties");
         pushParent(current());
         createConcept("Measurement circumstance properties");
         createConcept("Goal circumstance properties");
         createConcept("Request circumstance properties");
         createConcept("Performance circumstance properties");
         popParent();

         createConcept("Query clauses");
         pushParent(current());
   createConcept(TermAux.ACTIVE_QUERY_CLAUSE);
   createConcept(TermAux.INACTIVE_QUERY_CLAUSE);
   createConcept(TermAux.AND_QUERY_CLAUSE);

   createConcept(TermAux.NOT_QUERY_CLAUSE);
   createConcept(TermAux.AND_NOT_QUERY_CLAUSE);
   createConcept(TermAux.OR_QUERY_CLAUSE);
   createConcept(TermAux.XOR_QUERY_CLAUSE);
   createConcept(TermAux.CHANGED_FROM_PREVIOUS_VERSION_QUERY_CLAUSE);
   createConcept(TermAux.CONCEPT_IS_QUERY_CLAUSE);
   createConcept(TermAux.CONCEPT_IS_KIND_OF_QUERY_CLAUSE);
   createConcept(TermAux.DESCRIPTION_LUCENE_MATCH_QUERY_CLAUSE);
   createConcept(TermAux.PREFERRED_NAME_FOR_CONCEPT_QUERY_CLAUSE);
   createConcept(TermAux.RELATIONSHIP_IS_CIRCULAR_QUERY_CLAUSE);
   createConcept(TermAux.CONCEPT_IS_CHILD_OF_QUERY_CLAUSE);
   createConcept(TermAux.DESCRIPTION_REGEX_MATCH_QUERY_CLAUSE);
   createConcept(TermAux.CONCEPT_FOR_COMPONENT_QUERY_CLAUSE);
   createConcept(TermAux.CONCEPT_IS_DESCENDENT_OF_QUERY_CLAUSE);
   createConcept(TermAux.FULLY_SPECIFIED_NAME_FOR_CONCEPT_QUERY_CLAUSE);
   
   createConcept(TermAux.ASSEMBLAGE_CONTAINS_STRING_QUERY_CLAUSE);
   createConcept(TermAux.ASSEMBLAGE_CONTAINS_CONCEPT_QUERY_CLAUSE);
   createConcept(TermAux.ASSEMBLAGE_CONTAINS_COMPONENT_QUERY_CLAUSE);
   createConcept(TermAux.ASSEMBLAGE_LUCENE_MATCH_QUERY_CLAUSE);
   createConcept(TermAux.ASSEMBLAGE_CONTAINS_KIND_OF_CONCEPT_QUERY_CLAUSE);
   createConcept(TermAux.REL_RESTRICTION_QUERY_CLAUSE);
   createConcept(TermAux.REL_TYPE_QUERY_CLAUSE);

         popParent();
         
         
         popParent(); // ISAAC root should still be parent on stack... 
         
         createConcept("Clinical statement");
         
         pushParent(current());
         
         createConcept("Phenomenon statement");
         
         pushParent(current());
         createConcept("Phenomenon measurement");
         createConcept("Phenomenon goal");
         popParent();
         
         createConcept("Action statement");
         pushParent(current());
         createConcept("Action request");
         createConcept("Action performance");
         popParent();

         pushParent(current());
         popParent();
         
         popParent();
         
//         createConcept("test concept");
//         pushParent(current());
//         ConceptBuilder parentOneBuilder = createConcept("parent one");
//         pushParent(current());
//         ConceptBuilder multiParentBuilder = createConcept("multi-parent");
//         popParent();
//         ConceptBuilder parentTwoBuilder = createConcept("parent two");
         
         
         

         
         
         popParent();
         
         // Note that we leave this method with the root concept set as parent (on purpose) - we don't call popParent the last time.
         // This way, if createConcept(...) is called again, the new concepts go under root.
         // this nasty oversight took _far_ too long to recognize.
         // MetaData concepts must have CONSISTENT UUIDs. The default concept builder creates random
         // UUIDs for anything that doesn't have a UUID listed here, causing them to be random, which
         // breaks things in interesting ways when we have ibdf files that references the UUIDs from a
         // MetaData file....
         generateStableUUIDs();

//         final LogicalExpressionBuilderService expressionBuilderService =
//            LookupService.getService(LogicalExpressionBuilderService.class);
//         final LogicalExpressionBuilder defBuilder = expressionBuilderService.getLogicalExpressionBuilder();
//                  NecessarySet(And(ConceptAssertion(parentOneBuilder.getNid(), defBuilder), 
//                          ConceptAssertion(parentTwoBuilder.getNid(), defBuilder)));
//         
//         final LogicalExpression logicalExpression = defBuilder.build();
//         multiParentBuilder.setLogicalExpression(logicalExpression);

      } catch (final Exception ex) {
         Logger.getLogger(IsaacMetadataAuxiliary.class.getName())
               .log(Level.SEVERE, null, ex);
      }
   }

   //~--- methods -------------------------------------------------------------

   /**
    * The main method.
    *
    * @param args the arguments
    */
   public static void main(String[] args) {
      try {
         final IsaacMetadataAuxiliary aux = new IsaacMetadataAuxiliary();

         aux.export(new DataOutputStream(new ByteArrayOutputStream(10240)));
      } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
         Logger.getLogger(IsaacMetadataAuxiliary.class.getName())
               .log(Level.SEVERE, null, ex);
      }
   }
}

