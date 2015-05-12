/*
 * Copyright 2015 kec.
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
package gov.vha.isaac.ochre.model.sememe.version;

import gov.vha.isaac.ochre.api.component.sememe.version.MutableSememeVersion;
import gov.vha.isaac.ochre.model.DataBuffer;
import gov.vha.isaac.ochre.model.ObjectVersionImpl;
import gov.vha.isaac.ochre.model.sememe.SememeChronicleImpl;
import gov.vha.isaac.ochre.api.component.sememe.SememeType;

/**
 *
 * @author kec
  * @param <V>
 */
public class SememeVersionImpl<V extends SememeVersionImpl> extends ObjectVersionImpl<SememeChronicleImpl<V>, V> implements MutableSememeVersion {


    public SememeVersionImpl(SememeChronicleImpl<V> container, int stampSequence, DataBuffer db) {
        super(container, stampSequence);
    }

    public SememeVersionImpl(SememeChronicleImpl<V> container, int stampSequence) {
        super(container, stampSequence);
    }
    
    public SememeType getSememeType() {
        return SememeType.MEMBER;
    };
    
    @Override
    protected void writeVersionData(DataBuffer data) {
        super.writeVersionData(data);
    }

    @Override
    public String toString() {
        return getSememeType().toString() + super.toString();
    }

}
