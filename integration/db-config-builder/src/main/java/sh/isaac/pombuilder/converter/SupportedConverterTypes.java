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



package sh.isaac.pombuilder.converter;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

import java.util.Arrays;
import java.util.List;

//~--- non-JDK imports --------------------------------------------------------

import sh.isaac.pombuilder.FileUtil;

//~--- enums ------------------------------------------------------------------

/**
 * {@link SupportedConverterTypes}.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public enum SupportedConverterTypes {
   /** The loinc. */
   LOINC("loinc-src-data", new String[] {}, new String[] {}, new UploadFileInfo[] {
      // (?i) and (?-i) constructs are not supported in JavaScript (they are in Ruby)
      new UploadFileInfo("",
                         "https://loinc.org/downloads/loinc",
                         "LOINC_2.54_Text.zip",
                         "The primary LOINC file is the 'LOINC Table File' in the csv format'.  This should be a zip file that contains a file named 'loinc.csv'." +
                         "  Additionally, the zip file may (optionally) contain 'map_to.csv' and 'source_organization.csv'." +
                         "  The zip file must contain 'text' within its name.",
                         ".*text.*\\.zip$",
                         true), new UploadFileInfo("",
                               "https://loinc.org/downloads/files/loinc-multiaxial-hierarchy",
                               "LOINC_2.54_MULTI-AXIAL_HIERARCHY.zip",
                               "The Multiaxial Hierarchy file is a zip file that contains a file named *multi-axial_hierarchy.csv.  The zip file containing the multiaxial hierarchy" +
                               " must contain 'multi-axial_hierarchy' within its name",
                               ".*multi\\-axial_hierarchy.*\\.zip$",
                               true), new UploadFileInfo("",
                                     "https://loinc.org/downloads/loinc",
                                     "LOINC_ReleaseNotes.txt",
                                     "The LOINC Release Notes file must be included for recent versions of LOINC.",
                                     ".*releasenotes\\.txt$",
                                     true)
   }, "loinc-mojo", "loinc-ibdf", "convert-loinc-to-ibdf", "sh.isaac.terminology.source.loinc", "LOINC",
      new String[] { "shared/licenses/loinc.xml" },
      new String[] { "shared/noticeAdditions/loinc-NOTICE-addition.txt" }),

   /** The loinc tech preview. */
   LOINC_TECH_PREVIEW("loinc-src-data-tech-preview", new String[] { "loinc-src-data" },
                      new String[] { "rf2-ibdf-sct" }, new UploadFileInfo[] { new UploadFileInfo("",
                            "https://www.nlm.nih.gov/healthit/snomedct/international.html",
                            "SnomedCT_LOINC_AlphaPhase3_INT_20160401.zip",
                            "  The expected file is the RF2 release (NOT the Human Readable release nor the OWL release). " +
                            "The file must be a zip file, which ends with .zip",
                            ".*\\.zip$",
                            true) }, "loinc-mojo", "loinc-ibdf-tech-preview", "convert-loinc-tech-preview-to-ibdf",
                                     "sh.isaac.terminology.source.loinc", "LOINC Tech Preview",
                                     new String[] { "shared/licenses/loinc.xml",
                                           "shared/licenses/sct.xml" }, new String[] {
                                           "shared/noticeAdditions/loinc-tech-preview-NOTICE-addition.txt",
                                                 "shared/noticeAdditions/loinc-NOTICE-addition.txt",
                                                 "shared/noticeAdditions/rf2-sct-NOTICE-addition.txt" }),

   /** The sct. */
   SCT("rf2-src-data-sct", new String[] {}, new String[] {}, new UploadFileInfo[] { new UploadFileInfo("",
         "https://www.nlm.nih.gov/healthit/snomedct/international.html",
         "SnomedCT_RF2Release_INT_20160131.zip",
         "The expected file is the RF2 release zip file.  The filename must end with .zip, and must contain the release date in the Snomed standard" +
         " naming convention (4 digit year, 2 digit month, 2 digit day).",
         ".*_\\d{8}.*\\.zip$",
         true) }, "rf2-mojo", "rf2-ibdf-sct", "convert-RF2-to-ibdf", "sh.isaac.terminology.source.rf2", "SnomedCT",
                  new String[] { "shared/licenses/sct.xml" },
                  new String[] { "shared/noticeAdditions/rf2-sct-NOTICE-addition.txt" }),

   /** The sct extension. */
   SCT_EXTENSION(
      "rf2-src-data-*-extension", new String[] {}, new String[] { "rf2-ibdf-sct" }, new UploadFileInfo[] { new UploadFileInfo(
         "Snomed Extensions come from a variety of sources.  Note that the NLM has choosen to stop advertising the download links to the " +
         " US Extension, but still publishes it.  The current download pattern is: " +
         "http://download.nlm.nih.gov/mlb/utsauth/USExt/SnomedCT_Release_US1000124_YYYYMMDD_Extension.zip",
         "",
         "SnomedCT_Release_US1000124_20160301_Extension.zip",
         "The expected file is the RF2 release zip file.  The filename must end with .zip, and must contain the release date in the Snomed standard" +
         " naming convention (4 digit year, 2 digit month, 2 digit day).",
         ".*_\\d{8}.*\\.zip$",
         true) }, "rf2-mojo", "rf2-ibdf-", "convert-RF2-to-ibdf", "sh.isaac.terminology.source.rf2",
                  "SnomedCT Extension", new String[] { "shared/licenses/sct.xml" },
                  new String[] { "shared/noticeAdditions/rf2-sct-NOTICE-addition.txt" }),

   /** The vhat. */
   VHAT("vhat-src-data", new String[] {}, new String[] {},
        new UploadFileInfo[] { new UploadFileInfo("VHAT content is typically exported from a VETs system.  ",
              "",
              "VHAT.xml",
              "Any XML file that is valid per the VETs TerminologyData.xsd schema.  The file name is ignored",
              ".*\\.xml$",
              true) }, "vhat-mojo", "vhat-ibdf", "convert-VHAT-to-ibdf", "sh.isaac.terminology.source.vhat", "VHAT",
                       new String[] { "" }, new String[] { "" }),

   /** The rxnorm. */
   RXNORM("rxnorm-src-data", new String[] {}, new String[] { "rf2-ibdf-sct" },
          new UploadFileInfo[] { new UploadFileInfo("",
                "https://www.nlm.nih.gov/research/umls/rxnorm/docs/rxnormfiles.html",
                "RxNorm_full_06062016.zip",
                "The file must be a zip file, which starts with 'rxnorm_full' and ends with .zip",
                "rxnorm_full.*\\.zip$",
                true) }, "rxnorm-mojo", "rxnorm-ibdf", "convert-rxnorm-to-ibdf", "sh.isaac.terminology.source.rxnorm",
                         "RxNorm", new String[] { "shared/licenses/rxnorm.xml" },
                         new String[] { "shared/noticeAdditions/rxnorm-NOTICE-addition.txt" }),

// RXNORM_SOLOR("rxnorm-src-data", new String[] {}, new String[] {"rf2-ibdf-sct"}, new UploadFileInfo[] {
//                 new UploadFileInfo("", "https://www.nlm.nih.gov/research/umls/rxnorm/docs/rxnormfiles.html", 
//                                 "RxNorm_full_06062016.zip",
//                                 "The file must be a zip file, which starts with 'rxnorm_full' and ends with .zip", "rxnorm_full.*\\.zip$", true)
// }, "rxnorm-mojo", "rxnorm-ibdf-solor", "convert-rxnorm-solor-to-ibdf", "sh.isaac.terminology.source.rxnorm", "RxNorm Solor", 
//                 new String[] {"shared/licenses/rxnorm.xml"}, 

   /** The HL 7 v 3. */
//                 new String[] {"shared/noticeAdditions/rxnorm-NOTICE-addition.txt"}),
   HL7v3("hl7v3-src-data", new String[] {}, new String[] {}, new UploadFileInfo[] { new UploadFileInfo("",
         "http://gforge.hl7.org/gf/project/design-repos/frs/?action=FrsReleaseBrowse&frs_package_id=30",
         "hl7-rimRepos-2.47.7.zip",
         "The file must be a zip file, which should have 'rimRepos' in the file name and end with '.zip'.  This uploaded zip file" +
         " MUST contain a file that has 'DEFN=UV=VO' in the file name, and ends with .coremif",
         ".*rim.*\\.zip$",
         true) }, "hl7v3-mojo", "hl7v3-ibdf", "convert-hl7v3-to-ibdf", "sh.isaac.terminology.source.hl7v3", "HL7v3",
                  new String[] { "shared/licenses/hl7v3.xml" },
                  new String[] { "shared/noticeAdditions/hl7v3-NOTICE-addition.txt" }),

   /** The nucc. */
   NUCC("nucc-src-data", new String[] {}, new String[] {}, new UploadFileInfo[] { new UploadFileInfo("",
         "http://www.nucc.org/index.php/code-sets-mainmenu-41/provider-taxonomy-mainmenu-40/csv-mainmenu-57",
         "nucc_taxonomy_170.csv",
         "The file name is ignored - it just needs to be a csv file which ends with .csv.",
         ".*\\.csv$",
         true) }, "nucc-mojo", "nucc-ibdf", "convert-NUCC-to-ibdf", "sh.isaac.terminology.source.nucc",
                  "National Uniform Claim Committee", new String[] { "" },  // TODO
                  new String[] { "" }),

   /** The cvx. */

   // TODO
   CVX("cvx-src-data", new String[] {}, new String[] {}, new UploadFileInfo[] { new UploadFileInfo("",
         "https://www2a.cdc.gov/vaccines/iis/iisstandards/vaccines.asp?rpt=cvx",
         "cvx.xml",
         "The file name is ignored - it just needs to be an xml file which ends with .xml.  Download the 'XML-new format' type, " +
         "and store it into a file with the extension .xml",
         ".*\\.xml$",
         true) }, "cvx-mojo", "cvx-ibdf", "convert-CVX-to-ibdf", "sh.isaac.terminology.source.cvx",
                  "Current Vaccines Administered", new String[] { "" },  // TODO
                  new String[] { "" }),

   /** The mvx. */

   // TODO
   MVX("mvx-src-data", new String[] {}, new String[] {}, new UploadFileInfo[] { new UploadFileInfo("",
         "https://www2a.cdc.gov/vaccines/iis/iisstandards/vaccines.asp?rpt=mvx",
         "mvx.xml",
         "The file name is ignored - it just needs to be an xml file which ends with .xml.  Download the 'XML-new format' type, " +
         "and store it into a file with the extension .xml",
         ".*\\.xml$",
         true) }, "mvx-mojo", "mvx-ibdf", "convert-MVX-to-ibdf", "sh.isaac.terminology.source.mvx",
                  "Manufacturers of Vaccines", new String[] { "" },  // TODO
                  new String[] { "" }),

   /** The cpt. */

   // TODO
   CPT("cpt-src-data", new String[] {}, new String[] {}, new UploadFileInfo[] { new UploadFileInfo("",
         "File a bug, assign to Dan",  // TODO dan fix... this is a stub for the moment
         "TBD",
         "File a bug, assign to Dan",
         ".*$",
         true) }, "cpt-mojo", "cpt-ibdf", "convert-CPT-to-ibdf", "sh.isaac.terminology.source.cpt",
                  "Current Procedural Terminology", new String[] { "" },  // TODO
                  new String[] { "" })

   /** The converter group id. */

   // TODO
   ;

   private final String converterGroupId = "sh.isaac.terminology.converters";

   /** The src artifact id. */
   private String srcArtifactId;

   /** The artifact src dependencies. */
   private List<String> artifactSrcDependencies;

   /** The artifact IBDF dependencies. */
   private List<String> artifactIBDFDependencies;

   /** The upload file info. */
   private List<UploadFileInfo> uploadFileInfo;  // If we were really clever, we would pull this from an options file published with the converter itself.

   /** The converter artifact id. */
   private String converterArtifactId;

   /** The converter output artifact id. */
   private String converterOutputArtifactId;

   /** The converter mojo name. */
   private String converterMojoName;  // Must match the value from the mojo - aka - @ Mojo( name = "convert-loinc-to-ibdf", defaultPhase... used as the goal in the pom.

   /** The source upload group id. */
   private String sourceUploadGroupId;

   /** The nice name. */
   private String niceName;

   /** The license information. */
   private String[] licenseInformation;

   /** The notice information. */
   private String[] noticeInformation;

   //~--- constructors --------------------------------------------------------

   /**
    * Instantiates a new supported converter types.
    *
    * @param artifactId the artifact id
    * @param artifactSourceDependencies the artifact source dependencies
    * @param artifactIBDFDependencies the artifact IBDF dependencies
    * @param uploadFileInfo the upload file info
    * @param converterArtifactId the converter artifact id
    * @param converterOutputArtifactId the converter output artifact id
    * @param converterMojoName the converter mojo name
    * @param sourceUploadGroupId the source upload group id
    * @param niceName the nice name
    * @param licenseFilePaths the license file paths
    * @param noticeFilePaths the notice file paths
    */

   /*
    * unfortunately, that gets tricky, because the user needs to populate these when they are uploading, without necessarily knowing what particular
    * version of the converter will execute against this uploaded content.  So, will hardcode them here for now, and developers will have to manually
    * update these if the patterns change in the future.
    */
   private SupportedConverterTypes(String artifactId,
                                   String[] artifactSourceDependencies,
                                   String[] artifactIBDFDependencies,
                                   UploadFileInfo[] uploadFileInfo,
                                   String converterArtifactId,
                                   String converterOutputArtifactId,
                                   String converterMojoName,
                                   String sourceUploadGroupId,
                                   String niceName,
                                   String[] licenseFilePaths,
                                   String[] noticeFilePaths) {
      this.srcArtifactId             = artifactId;
      this.artifactSrcDependencies   = Arrays.asList(artifactSourceDependencies);
      this.artifactIBDFDependencies  = Arrays.asList(artifactIBDFDependencies);
      this.uploadFileInfo            = Arrays.asList(uploadFileInfo);
      this.converterArtifactId       = converterArtifactId;
      this.converterOutputArtifactId = converterOutputArtifactId;
      this.converterMojoName         = converterMojoName;
      this.sourceUploadGroupId       = sourceUploadGroupId;
      this.niceName                  = niceName;
      this.licenseInformation         = new String[licenseFilePaths.length];
      this.noticeInformation          = new String[noticeFilePaths.length];

      try {
         for (int i = 0; i < licenseFilePaths.length; i++) {
            if (org.apache.commons.lang3.StringUtils.isBlank(licenseFilePaths[i])) {
               this.licenseInformation[i] = "";
            } else {
               this.licenseInformation[i] = FileUtil.readFile(licenseFilePaths[i]);
            }
         }

         for (int i = 0; i < noticeFilePaths.length; i++) {
            if (org.apache.commons.lang3.StringUtils.isBlank(noticeFilePaths[i])) {
               this.noticeInformation[i] = "";
            } else {
               this.noticeInformation[i] = FileUtil.readFile(noticeFilePaths[i]);
            }
         }
      } catch (final IOException e) {
         throw new RuntimeException(e);
      }
   }

   //~--- get methods ---------------------------------------------------------

   /**
    * In order to execute a conversion of the specified type, you must also provide dependencies for each of the listed
    * Source artifact identifiers.
    *
    * This is used during SOURCE UPLOAD
    *
    * @return the artifact dependencies
    */
   public List<String> getArtifactDependencies() {
      return this.artifactSrcDependencies;
   }

   /**
    * Note that the artifactID may include a wildcard ('*') for some, such as SCT_EXTENSION - note - this is the pattern
    * for the source artifact upload, not the artifact id related to the converter.
    *
    * This is used during SOURCE UPLOAD
    *
    * @return the artifact id
    */
   public String getArtifactId() {
      return this.srcArtifactId;
   }

   /**
    * Not for PRISME.
    *
    * @return the converter artifact id
    */
   protected String getConverterArtifactId() {
      return this.converterArtifactId;
   }

   /**
    * Not for PRISME.
    *
    * @return the converter group id
    */
   protected String getConverterGroupId() {
      return this.converterGroupId;
   }

   /**
    * Not for PRISME.
    *
    * @return the converter mojo name
    */
   protected String getConverterMojoName() {
      return this.converterMojoName;
   }

   /**
    * Not for PRISME.
    *
    * @return the converter output artifact id
    */
   protected String getConverterOutputArtifactId() {
      return this.converterOutputArtifactId;
   }

   /**
    * In order to execute a conversion of the specified type, you must also provide dependencies for each of the listed
    * IBDF artifact identifiers.
    *
    * This is used during IBDF CONVERSION
    *
    * @return the IBDF dependencies
    */
   public List<String> getIBDFDependencies() {
      return this.artifactIBDFDependencies;
   }

   /**
    * Not for PRISME.
    *
    * @return the license information
    */
   public String[] getLicenseInformation() {
      return this.licenseInformation;
   }

   /**
    * Not for PRISME (but you can use it if you want).
    *
    * @return the nice name
    */
   public String getNiceName() {
      return this.niceName;
   }

   /**
    * Not for PRISME.
    *
    * @return the notice information
    */
   public String[] getNoticeInformation() {
      return this.noticeInformation;
   }

   /**
    * Not for PRISME.
    *
    * @return the source upload group id
    */
   public String getSourceUploadGroupId() {
      return this.sourceUploadGroupId;
   }

   /**
    * The information describing the files that an end user must upload into the system to allow the execution of a particular converter.
    *
    * This is used during SOURCE UPLOAD
    *
    * @return the upload file info
    */
   public List<UploadFileInfo> getUploadFileInfo() {
      return this.uploadFileInfo;
   }
}

