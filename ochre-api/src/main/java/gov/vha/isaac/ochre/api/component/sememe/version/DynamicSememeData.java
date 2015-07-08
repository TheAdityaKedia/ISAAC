/*
 * Copyright 2015 U.S. Department of Veterans Affairs.
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
package gov.vha.isaac.ochre.api.component.sememe.version;

import javafx.beans.property.ReadOnlyObjectProperty;

/**
 *
 * @author kec
 */
public interface DynamicSememeData {

    /**
     * @return The data object itself, in its most compact, serialized form. You
     *         probably don't want this method unless you are doing something clever....
     *         For a getData() method that doesn't require deserialization, see the {@link #getDataObject()} method.
     *         For a method that doesn't require casting the output, see the getDataXXX() method available within
     *         implementations of the {@link RefexDynamicDataBI} interface.
     */
    byte[] getData();

    /**
     * @return The data object itself.
     *         For a getData() method that doesn't  require casting of the output, see the getDataXXX() method
     *         available within implementations of the {@link RefexDynamicDataBI} interface.
     */
    Object getDataObject();

    /**
     * @return The data object itself.
     *         For a getDataProperty() method that doesn't  require casting of the output, see the getDataXXXProperty() methods
     *         available within implementations of the {@link RefexDynamicDataBI} interface.
     */
    ReadOnlyObjectProperty<?> getDataObjectProperty();

    /**
     * This is only intended to be used by {@link RefexDynamicCAB}.
     * Please ignore.
     * @param name
     */
    void setNameIfAbsent(String name);
    
}