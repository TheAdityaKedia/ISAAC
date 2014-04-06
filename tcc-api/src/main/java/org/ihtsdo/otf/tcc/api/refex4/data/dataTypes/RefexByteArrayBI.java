/*
 * Copyright 2010 International Health Terminology Standards Development Organisation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ihtsdo.otf.tcc.api.refex4.data.dataTypes;

import javafx.beans.property.ReadOnlyObjectProperty;

import org.ihtsdo.otf.tcc.api.refex4.data.RefexDataBI;

/**
 * 
 * {@link RefexByteArrayBI}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public interface RefexByteArrayBI extends RefexDataBI
{

	public byte[] getDataByteArray();
	
	public ReadOnlyObjectProperty<Byte[]> getDataByteArrayProperty();
}
