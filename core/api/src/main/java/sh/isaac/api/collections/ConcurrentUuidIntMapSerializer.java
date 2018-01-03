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



package sh.isaac.api.collections;

//~--- JDK imports ------------------------------------------------------------

import java.io.DataInput;
import java.io.DataOutput;
import java.io.Serializable;

//~--- non-JDK imports --------------------------------------------------------

import sh.isaac.api.DataSerializer;
import sh.isaac.api.collections.uuidnidmap.ConcurrentUuidToIntHashMap;

//~--- classes ----------------------------------------------------------------

/**
 * Created by kec on 7/20/14.
 * @deprecated use method direct on ConcurrentUuidToIntHashMap instead
 */
public class ConcurrentUuidIntMapSerializer
         implements DataSerializer<ConcurrentUuidToIntHashMap>, Serializable {
   /** The Constant serialVersionUID. */
   private static final long serialVersionUID = 1L;

   //~--- methods -------------------------------------------------------------

   /**
    * Deserialize.
    *
    * @param input the input
    * @return the concurrent uuid to int hash map
    */
   @Override
   public ConcurrentUuidToIntHashMap deserialize(DataInput input) {
      return ConcurrentUuidToIntHashMap.deserialize(input);
   }

   /**
    * Serialize.
    *
    * @param out the out
    * @param map the map
    */
   @Override
   public void serialize(DataOutput out, ConcurrentUuidToIntHashMap map) {
      map.serialize(out);
   }
}

