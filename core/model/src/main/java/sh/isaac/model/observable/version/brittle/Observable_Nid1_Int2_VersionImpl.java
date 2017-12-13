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
package sh.isaac.model.observable.version.brittle;

import javafx.beans.property.IntegerProperty;
import sh.isaac.api.component.semantic.version.SemanticVersion;
import sh.isaac.api.observable.semantic.ObservableSemanticChronology;
import sh.isaac.model.observable.version.ObservableSemanticVersionImpl;
import sh.isaac.api.observable.semantic.version.brittle.Observable_Nid1_Int2_Version;

/**
 *
 * @author kec
 */
public class Observable_Nid1_Int2_VersionImpl 
        extends ObservableSemanticVersionImpl
         implements Observable_Nid1_Int2_Version {

   IntegerProperty nid1Property;
   IntegerProperty int2Property;

   public Observable_Nid1_Int2_VersionImpl(SemanticVersion stampedVersion, ObservableSemanticChronology chronology) {
      super(stampedVersion, chronology);
   }

   @Override
   public IntegerProperty nid1Property() {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
   }

   @Override
   public IntegerProperty int2Property() {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
   }

   @Override
   public int getNid1() {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
   }

   @Override
   public int getInt2() {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
   }

   @Override
   public void setNid1(int nid) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
   }

   @Override
   public void setInt2(int value) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
   }

   
}
