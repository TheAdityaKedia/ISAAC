/*
 * Copyright 2018 Organizations participating in ISAAC, ISAAC's KOMET, and SOLOR development include the
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
package sh.isaac.model.xml;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import sh.isaac.api.ConceptProxy;
import sh.isaac.api.coordinate.StampPrecedence;

/**
 *
 * @author kec
 */
public class StampPrecedenceAdaptor extends XmlAdapter<ConceptProxy, StampPrecedence> {

    @Override
    public StampPrecedence unmarshal(ConceptProxy v) throws Exception {
        return StampPrecedence.from(v);
    }

    @Override
    public ConceptProxy marshal(StampPrecedence v) throws Exception {
        return (ConceptProxy) v.getSpecifyingConcept();
    }
    
}
