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



package sh.isaac.model.sememe.dataTypes;

//~--- JDK imports ------------------------------------------------------------

import java.nio.ByteBuffer;

import java.util.UUID;

//~--- non-JDK imports --------------------------------------------------------

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import sh.isaac.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeUUID;

//~--- classes ----------------------------------------------------------------

/**
 * {@link DynamicSememeUUIDImpl}.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class DynamicSememeUUIDImpl
        extends DynamicSememeDataImpl
         implements DynamicSememeUUID {
   /** The property. */
   private ObjectProperty<UUID> property;

   //~--- constructors --------------------------------------------------------

   /**
    * Instantiates a new dynamic sememe UUID impl.
    *
    * @param data the data
    */
   protected DynamicSememeUUIDImpl(byte[] data) {
      super(data);
   }

   /**
    * Instantiates a new dynamic sememe UUID impl.
    *
    * @param uuid the uuid
    */
   public DynamicSememeUUIDImpl(UUID uuid) {
      super();

      if (uuid == null) {
         throw new RuntimeException("The uuid value cannot be null", null);
      }

      final ByteBuffer b = ByteBuffer.allocate(16);

      b.putLong(uuid.getMostSignificantBits());
      b.putLong(uuid.getLeastSignificantBits());
      this.data = b.array();
   }

   /**
    * Instantiates a new dynamic sememe UUID impl.
    *
    * @param data the data
    * @param assemblageSequence the assemblage sequence
    * @param columnNumber the column number
    */
   protected DynamicSememeUUIDImpl(byte[] data, int assemblageSequence, int columnNumber) {
      super(data, assemblageSequence, columnNumber);
   }

   //~--- get methods ---------------------------------------------------------

   /**
    * Gets the data object.
    *
    * @return the data object
    * @see org.ihtsdo.otf.tcc.api.refexDynamic.data.RefexDynamicDataBI#getDataObject()
    */
   @Override
   public Object getDataObject() {
      return getDataUUID();
   }

   /**
    * Gets the data object property.
    *
    * @return the data object property
    * @see org.ihtsdo.otf.tcc.api.refexDynamic.data.RefexDynamicDataBI#getDataObjectProperty()
    */
   @Override
   public ReadOnlyObjectProperty<?> getDataObjectProperty() {
      return getDataUUIDProperty();
   }

   /**
    * Gets the data UUID.
    *
    * @return the data UUID
    * @see org.ihtsdo.otf.tcc.api.refexDynamic.data.dataTypes.RefexDynamicUUIDBI#getDataUUID()
    */
   @Override
   public UUID getDataUUID() {
      final ByteBuffer b     = ByteBuffer.wrap(this.data);
      final long       most  = b.getLong();
      final long       least = b.getLong();

      return new UUID(most, least);
   }

   /**
    * Gets the data UUID property.
    *
    * @return the data UUID property
    * @see org.ihtsdo.otf.tcc.api.refexDynamic.data.dataTypes.RefexDynamicUUIDBI#getDataUUIDProperty()
    */
   @Override
   public ReadOnlyObjectProperty<UUID> getDataUUIDProperty() {
      if (this.property == null) {
         this.property = new SimpleObjectProperty<>(null, getName(), getDataUUID());
      }

      return this.property;
   }
}

