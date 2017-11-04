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



package sh.isaac.provider.assemblage;

//~--- non-JDK imports --------------------------------------------------------

import sh.isaac.api.externalizable.ByteArrayDataBuffer;
import sh.isaac.api.externalizable.IsaacObjectType;
import sh.isaac.model.semantic.SemanticChronologyImpl;
import sh.isaac.model.waitfree.WaitFreeMergeSerializer;

//~--- classes ----------------------------------------------------------------

/**
 * The Class AssemblageSerializer.
 *
 * @author kec
 */
public class AssemblageSerializer
         implements WaitFreeMergeSerializer<SemanticChronologyImpl> {

   public AssemblageSerializer() {

   }
   /**
    * Deserialize.
    *
    * @param db the db
    * @return the sememe chronology impl
    */
   @Override
   public SemanticChronologyImpl deserialize(ByteArrayDataBuffer db) {
      IsaacObjectType.SEMANTIC.readAndValidateHeader(db);
      return SemanticChronologyImpl.make(db);
   }

   /**
    * Merge.
    *
    * @param a the a
    * @param b the b
    * @param writeSequence the write sequence
    * @return the sememe chronology impl
    */
   @Override
   public SemanticChronologyImpl merge(SemanticChronologyImpl a,
         SemanticChronologyImpl b,
         int writeSequence) {
      final byte[]              dataBytes = a.mergeData(writeSequence, b.getDataToWrite(writeSequence));
      final ByteArrayDataBuffer db        = new ByteArrayDataBuffer(dataBytes);
      IsaacObjectType.SEMANTIC.readAndValidateHeader(db);
      return SemanticChronologyImpl.make(db);
   }

   /**
    * Serialize.
    *
    * @param d the d
    * @param a the a
    */
   @Override
   public void serialize(ByteArrayDataBuffer d, SemanticChronologyImpl a) {
      final byte[] data = a.getDataToWrite();

      d.put(data, 0, data.length);
   }
}

