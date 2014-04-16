/**
 * Copyright Notice
 *
 * This is a work of the U.S. Government and is not subject to copyright
 * protection in the United States. Foreign copyrights may apply.
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
package org.ihtsdo.otf.tcc.api.blueprint;

import static org.ihtsdo.otf.tcc.api.blueprint.ComponentProperty.ASSEMBLAGE_ID;
import static org.ihtsdo.otf.tcc.api.blueprint.ComponentProperty.COMPONENT_ID;
import static org.ihtsdo.otf.tcc.api.blueprint.ComponentProperty.REFERENCED_COMPONENT_ID;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;
import org.ihtsdo.otf.tcc.api.chronicle.ComponentBI;
import org.ihtsdo.otf.tcc.api.concept.ConceptVersionBI;
import org.ihtsdo.otf.tcc.api.contradiction.ContradictionException;
import org.ihtsdo.otf.tcc.api.coordinate.StandardViewCoordinates;
import org.ihtsdo.otf.tcc.api.coordinate.Status;
import org.ihtsdo.otf.tcc.api.coordinate.ViewCoordinate;
import org.ihtsdo.otf.tcc.api.metadata.binding.RefexDynamic;
import org.ihtsdo.otf.tcc.api.metadata.binding.SnomedMetadataRf2;
import org.ihtsdo.otf.tcc.api.refexDynamic.RefexDynamicBuilderBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.RefexDynamicVersionBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.RefexDynamicDataBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.RefexDynamicDataType;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.dataTypes.RefexDynamicIntegerBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.dataTypes.RefexDynamicStringBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.dataTypes.RefexDynamicUUIDBI;
import org.ihtsdo.otf.tcc.api.store.TerminologyStoreDI;
import org.ihtsdo.otf.tcc.api.store.Ts;
import org.ihtsdo.otf.tcc.api.uuid.UuidT5Generator;

/**
 * {@link RefexDynamicCAB} 
 * 
 * The creation mechanism for building a {@link RefexDynamicVersionBI}
 * <br>
 * <br>
 * Note that in the new RefexDynanamicAPI - there are strict requirements on the structure of the 
 * assemblage concept - and these will be validated by this code.
 * <br>
 * <br>
 * The assemblage concept must define the combination of data columns being used within this Refex. 
 * To do this, the assemblage concept must itself contain 0 or more {@link RefexDynamicVersionBI} annotation(s) with
 * an assemblage concept that is {@link RefexDynamic#REFEX_DYNAMIC_DEFINITION} and the attached data is<br>
 * [{@link RefexDynamicIntegerBI}, {@link RefexDynamicUUIDBI}, {@link RefexDynamicStringBI}] 
 * 
 * <ul>
 * <li>The int value is used to align the column order with the data array here.  The column number should be 0 indexed.
 * <li>The UUID is a concept reference where the concept should have a preferred semantic name / FSN that is
 * suitable for describing its usage as a DynamicRefex data column.
 * <li>A string column which can be parsed as a member of the {@link RefexDynamicDataType} class, which represents
 * the type of the column.
 * </ul>
 * 
 * Note that while 0 is allowed, this would not allow the attachment of any data.
 * 
 * The assemblage concept must also contain a description of type {@link SnomedMetadataRf2#SYNONYM_RF2} which 
 * itself has a refex extension of type {@link RefexDynamic#REFEX_DYNAMIC_DEFINITION_DESCRIPTION} - the value of 
 * this description should explain the the overall purpose of this Refex.
 * 
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class RefexDynamicCAB extends CreateOrAmendBlueprint
{
	//TODO QUESTION - Should we require them to specify whether or not a column is optional?  Or treat all columns as optional?
	private static final UUID refexDynamicNamespace = RefexDynamic.REFEX_DYNAMIC_NAMESPACE.getUuids()[0];

	/**
	 * Computes the uuid of the refex member and sets the member uuid property.
	 * Uses {@link #computeMemberContentUuid()} to compute the uuid.
	 *
	 * @return the uuid of the refex member
	 * @throws InvalidCAB if the any of the values in blueprint to make are invalid
	 * @throws IOException 
	 */
	public UUID computeMemberUuid() throws InvalidCAB, IOException
	{
		UUID memberContentUuid = computeMemberContentUuid();
		setComponentUuidNoRecompute(memberContentUuid);
		return memberContentUuid;
	}

	/**
	 * Computes the uuid of a the refex member based on the refex properties and data.
	 * 
	 * DOES NOT set the value - only returns it.
	 *
	 * @return A <code>UUID</code> based on a Type 5 generator that uses the content fields of the refex.
	 * @throws InvalidCAB if the any of the values in blueprint to make are invalid
	 * @throws IOException 
	 */
	private UUID computeMemberContentUuid() throws InvalidCAB, IOException
	{
		try
		{
			StringBuilder sb = new StringBuilder();
			sb.append(getPrimordialUuidStringForNidProp(ComponentProperty.ASSEMBLAGE_ID));
			sb.append(getPrimordialUuidStringForNidProp(ComponentProperty.REFERENCED_COMPONENT_ID));
			if (properties.get(ComponentProperty.DYNAMIC_REFEX_DATA ) != null)
			{
				RefexDynamicDataBI data = (RefexDynamicDataBI)properties.get(ComponentProperty.DYNAMIC_REFEX_DATA);
				sb.append(data.getRefexDataType());
				sb.append(data.getData());
			}
			return UuidT5Generator.get(refexDynamicNamespace, sb.toString());
		}
		catch (NoSuchAlgorithmException | UnsupportedEncodingException ex)
		{
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Recomputes the refex member uuid component
	 *
	 * @throws InvalidCAB if the any of the values in blueprint to make are invalid
	 * @throws ContradictionException 
	 * @throws IOException 
	 */
	@Override
	public void recomputeUuid() throws InvalidCAB, IOException, ContradictionException
	{
		switch (idDirective)
		{
			case PRESERVE_CONCEPT_REST_HASH:
				if (getComponentUuid() == null)
				{
					//only set if it hasn't yet been set (preseve is already set)
					setComponentUuidNoRecompute(computeMemberContentUuid());
				}
				break;
			case GENERATE_RANDOM_CONCEPT_REST_HASH:
			case GENERATE_HASH:
			case GENERATE_REFEX_CONTENT_HASH:   //No real reason that I can see to ever not compute a hash across the data, if the data is present.
				setComponentUuidNoRecompute(computeMemberContentUuid());
				break;
			case GENERATE_RANDOM:
				setComponentUuidNoRecompute(UUID.randomUUID());
				break;

			case PRESERVE:
			default:
				// nothing to do...
		}

		//Recompute children
		for (RefexCAB annotBp : getAnnotationBlueprints())
		{
			annotBp.setReferencedComponentUuid(getComponentUuid());
			annotBp.recomputeUuid();
		}
	}
	
	/**
	 * Gets a string representing the primordial uuid of the specified nid-based <code>refexProperty</code>.
	 *
	 * @param refexProperty the refexProperty representing the nid-bsed property
	 * @return a String representing the primordial uuid of the refex property
	 * @throws IOException signals that an I/O exception has occurred
	 * @throws InvalidCAB if the any of the values in blueprint to make are invalid
	 */
	@SuppressWarnings("deprecation")
	private String getPrimordialUuidStringForNidProp(ComponentProperty refexProperty) throws IOException, InvalidCAB
	{
		Object idObj = properties.get(refexProperty);
		if (idObj == null)
		{
			throw new InvalidCAB("No data for: " + refexProperty);
		}
		if (idObj instanceof UUID)
		{
			return ((UUID) idObj).toString();
		}
		if (idObj instanceof Integer)
		{
			int nid = (Integer) idObj;
			ComponentBI component = Ts.get().getComponent(nid);
			if (component != null)
			{
				return component.getPrimordialUuid().toString();
			}
			List<UUID> uuids = Ts.get().getUuidsForNid(nid);
			if (uuids.size() >= 1)
			{
				return uuids.get(0).toString();
			}
			throw new InvalidCAB("Can't find nid for: " + refexProperty + " props: " + this.properties);
		}
		throw new InvalidCAB("Unexpected data type in '" + refexProperty + "' : '" + idObj + "'");
	}
	
	/**
	 * Instantiates a new refex blueprint using nid values and a given <code>refexVersion</code>. Uses the given <code>memberUuid</code> as the refex
	 * member uuid.
	 *
	 * @param referencedComponentUuid the UUID of the referenced component
	 * @param assemblageNid the nid of the refex collection concept
	 * @param refexVersion the refex version to use as a pattern
	 * @param viewCoordinate the view coordinate specifying which versions are active and inactive
	 * @param idDirective - typically {@link IdDirective#GENERATE_REFEX_CONTENT_HASH}
	 * @param refexDirective - Should the refex values of the refexVersion being used as a pattern be included, or excluded?
	 * @throws IOException signals that an I/O exception has occurred
	 * @throws InvalidCAB if the any of the values in blueprint to make are invalid
	 * @throws ContradictionException if more than one version is found for a given position or view
	 * coordinate
	 */
	public RefexDynamicCAB(UUID referencedComponentUuid, int assemblageNid, RefexDynamicVersionBI<?> refexVersion, ViewCoordinate viewCoordinate,
			IdDirective idDirective, RefexDirective refexDirective) throws IOException, InvalidCAB, ContradictionException
	{
		super(null, refexVersion, viewCoordinate, idDirective, refexDirective);
		setReferencedComponentUuid(referencedComponentUuid);
		setAssemblageNid(assemblageNid);
		setStatus(Status.ACTIVE);
		recomputeUuid();
	}
	
	/**
	 * Calls {@link #RefexDynamicCAB(UUID, int, RefexDynamicVersionBI, ViewCoordinate, IdDirective, RefexDirective)
	 * with null for RefexDynamicVersionBI, ViewCoordinate and RefexDirective
	 * IDDirective is set to {@link IdDirective#GENERATE_REFEX_CONTENT_HASH}
	 * 
	 * @param referencedComponentNid the nid of the referenced component
	 * @param assemblageNid the nid of the refex collection concept
	 * @throws IOException signals that an I/O exception has occurred
	 * @throws InvalidCAB if the any of the values in blueprint to make are invalid
	 * @throws ContradictionException if more than one version is found for a given position or view
	 */
	@SuppressWarnings("deprecation")
	public RefexDynamicCAB(UUID referencedComponentUUID, UUID assemblageUuid) throws IOException,
			InvalidCAB, ContradictionException
	{
		this(referencedComponentUUID, Ts.get().getNidForUuids(assemblageUuid), null, null, IdDirective.GENERATE_REFEX_CONTENT_HASH, null);
	}
	
	/**
	 * Instantiates a new refex blueprint using nid values. 
	 * Calls {@link #RefexDynamicCAB(UUID, int, RefexDynamicVersionBI, ViewCoordinate, IdDirective, RefexDirective)
	 * with null for RefexDynamicVersionBI, ViewCoordinate and RefexDirective
	 * IDDirective is set to {@link IdDirective#GENERATE_REFEX_CONTENT_HASH}
	 *
	 * @param referencedComponentNid the nid of the referenced component
	 * @param assemblageNid the nid of the refex collection concept
	 * @throws IOException signals that an I/O exception has occurred
	 * @throws InvalidCAB if the any of the values in blueprint to make are invalid
	 * @throws ContradictionException if more than one version is found for a given position or view
	 * coordinate
	 */
	@SuppressWarnings("deprecation")
	public RefexDynamicCAB(int referencedComponentNid, int assemblageNid) throws IOException,
			InvalidCAB, ContradictionException
	{
		this(Ts.get().getUuidPrimordialForNid(referencedComponentNid), assemblageNid, null, null, IdDirective.GENERATE_REFEX_CONTENT_HASH, null);
	}
	
	/**
	 * Sets the uuid for the referenced component associated with this refex blueprint.
	 *
	 * @param referencedComponentUuid the uuid of the referenced component
	 */
	public void setReferencedComponentUuid(UUID referencedComponentUuid)
	{
		properties.put(ComponentProperty.REFERENCED_COMPONENT_ID, referencedComponentUuid);
	}
	
	public void setAssemblageNid(int assemblageNid)
	{
		properties.put(ComponentProperty.ASSEMBLAGE_ID, assemblageNid);
	}
	
	/**
	 * Sets the refex member uuid associated with this refex blueprint.
	 * 
	 * This also changes the {@link IdDirective} to {@link IdDirective#PRESERVE} 
	 *
	 * @param memberUuid the refex member uuid
	 */
	public void setMemberUuid(UUID memberUuid)
	{
		setComponentUuid(memberUuid);
	}
	
	/**
	 * Generates a string representation of this refex blueprint. Includes the refex member type and the
	 * properties.
	 *
	 * @return a string representation of this refex blueprint
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(this.getClass().getSimpleName());
		sb.append(" COMPONENT_ID: ");
		sb.append(properties.get(COMPONENT_ID));
		@SuppressWarnings("deprecation")
		TerminologyStoreDI s = Ts.get();
		for (Entry<ComponentProperty, Object> entry : properties.entrySet())
		{
			if (entry.getKey() != COMPONENT_ID)
			{
				sb.append("  \n");
				sb.append(entry.getKey());
				sb.append(": ");
				if (s != null)
				{
					sb.append(s.informAboutId(entry.getValue()));
				}
				else
				{
					sb.append(entry.getValue());
				}
			}
		}

		return sb.toString();
	}
	
	/**
	 * Writes this refex member blueprint to the given <code>refex</code>.
	 *
	 * @param refex the refex analog to write this refex blueprint to
	 * @param includeStamp write the stamp if true, otherwise skip stamp attributes
	 * @throws PropertyVetoException if the new value is not valid
	 * @throws IOException signals that an I/O exception has occurred
	 * @throws InvalidCAB 
	 */
	public void writeTo(RefexDynamicBuilderBI refexAnalog, boolean includeStamp) throws PropertyVetoException, IOException, InvalidCAB
	{
		for (Entry<ComponentProperty, Object> entry : properties.entrySet())
		{
			switch (entry.getKey())
			{
				case COMPONENT_ID:
				{
					refexAnalog.setNid(getInt(ComponentProperty.COMPONENT_ID));
					break;
				}
				case REFERENCED_COMPONENT_ID:
				{
					refexAnalog.setReferencedComponentNid(getInt(ComponentProperty.REFERENCED_COMPONENT_ID));
					break;
				}
				case ASSEMBLAGE_ID:
				{
					refexAnalog.setAssemblageNid((Integer) entry.getValue());
					break;
				}
				case DYNAMIC_REFEX_DATA:
				{
					refexAnalog.setData((RefexDynamicDataBI[])entry.getValue());
					break;
				}
				case STATUS:
				{
					if (includeStamp)
					{
						refexAnalog.setStatus(getStatus());
					}
					break;
				}
				case TIME_IN_MS:
				{
					if (includeStamp)
					{
						refexAnalog.setTime((Long) entry.getValue());
					}
					break;
				}
				case AUTHOR_ID:
				{
					if (includeStamp)
					{
						refexAnalog.setAuthorNid(getInt(ComponentProperty.AUTHOR_ID));
					}
					break;
				}
				case MODULE_ID:
				{
					if (includeStamp)
					{
						refexAnalog.setModuleNid(getInt(ComponentProperty.MODULE_ID));
					}
					break;
				}
				case PATH_ID:
				{
					if (includeStamp)
					{
						refexAnalog.setPathNid(getInt(ComponentProperty.PATH_ID));
					}
					break;
				}
				default:
				{
					throw new InvalidCAB("Can't handle: " + entry.getKey());
				}
			}
		}
	}
	
	/**
	 * Checks to see if this refex blueprint's properties are equivalent to the given <code>refexVersion</code>
	 * excluding time, author, module and path.
	 *
	 * @param refexVersion the refex version to compare against
	 * @return <code>true</code>, if this refex blueprint's properties are equal to the specified refexVersion - 
	 * while not checking time, author, module or path.
	 * @throws InvalidCAB 
	 */
	public boolean equivalent(RefexDynamicVersionBI<?> refexVersion) throws InvalidCAB
	{
		for (Entry<ComponentProperty, Object> entry : properties.entrySet())
		{
			switch (entry.getKey())
			{
				case REFERENCED_COMPONENT_ID:
				{
					if (getInt(REFERENCED_COMPONENT_ID) != refexVersion.getReferencedComponentNid())
					{
						return false;
					}
					break;
				}
				case ASSEMBLAGE_ID:
				{
					if (getInt(ASSEMBLAGE_ID) != refexVersion.getAssemblageNid())
					{
						return false;
					}
					break;
				}
				case COMPONENT_ID:
				{
					if (refexVersion.getNid() != getInt(COMPONENT_ID))
					{
						return false;
					}
					break;
				}
				case DYNAMIC_REFEX_DATA:
				{
					if (!refexVersion.dataFieldsEqual((RefexDynamicDataBI[])properties.get(ComponentProperty.DYNAMIC_REFEX_DATA)))
					{
						return false;
					}
					break;
				}
				case STATUS:
				{
					if (getStatus() != refexVersion.getStatus())
					{
						return false;
					}
					break;
				}
				case TIME_IN_MS:
				case AUTHOR_ID:
				case MODULE_ID:
				case PATH_ID:
				{
					break;  //dont' check these...
				}
				default:
				{
					throw new InvalidCAB("Can't handle: " + entry.getKey());
				}
			}
		}
		return true;
	}
	
	/**
	 * Gets the nid of the assemblage concept associated with this refex blueprint.
	 *
	 * @return the refex collection nid
	 */
	public int getRefexAssemblageNid()
	{
		return getInt(ComponentProperty.ASSEMBLAGE_ID);
	}

	/**
	 * Gets the UUID of the assemblage concept associated with this refex blueprint.
	 *
	 * @return the refex collection UUID
	 */
	public UUID getRefexAssemblageUuid()
	{
		return getUuid(ComponentProperty.ASSEMBLAGE_ID);
	}
	
	/**
	 * Gets the uuid of the referenced component associated with this refex blueprint.
	 *
	 * @return the referenced component uuid
	 */
	public UUID getReferencedComponentUuid()
	{
		return getUuid(ComponentProperty.REFERENCED_COMPONENT_ID);
	}
	
	/**
	 * Gets the refex member uuid of this refex blueprint.
	 *
	 * @return the refex member uuid
	 */
	public UUID getMemberUUID()
	{
		return getUuid(ComponentProperty.COMPONENT_ID);
	}
	
	/**
	 * returns true, if the RefexDynamicCAB as currently specified can be turned into a valid {@link RefexDynamicVersionBI}
	 * otherwise 
	 * throws @InvalidCAB
	 * @throws IOException 
	 */
	public void validate() throws InvalidCAB, IOException
	{
		if (getMemberUUID() == null)
		{
			throw new InvalidCAB("The Member ID is required");
		}
		if (getRefexAssemblageUuid() == null)
		{
			throw new InvalidCAB("The Assemblage ID is required");
		}
		if (getReferencedComponentUuid() == null)
		{
			throw new InvalidCAB("The Referenced Component ID is required");
		}
		
		validateData((RefexDynamicDataBI[])properties.get(ComponentProperty.DYNAMIC_REFEX_DATA));
	}
	
	/**
	 * The data (if any) that is to be stored with this Refex.  The data columns and types _must_ align with the definition 
	 * within the assemblage concept.  See the class description for more details
	 * @param data
	 * @throws IOException 
	 * @throws InvalidCAB 
	 */
	public void setData(RefexDynamicDataBI[] data) throws IOException, InvalidCAB
	{
		validateData(data);
		properties.put(ComponentProperty.DYNAMIC_REFEX_DATA, data);
	}
	
	/**
	 * Validate the supplied data against the Refex Definition.  Throws in InvalidCAB exception
	 * if the data is invalid.
	 */
	private void validateData(RefexDynamicDataBI[] data) throws IOException, InvalidCAB
	{
		TreeMap<Integer, RefexDynamicDataType> allowedColumnInfo = new TreeMap<>();
		@SuppressWarnings("deprecation")
		ConceptVersionBI assemblageConcept = Ts.get().getConceptVersion(StandardViewCoordinates.getSnomedInferredThenStatedLatest(), getRefexAssemblageNid());
		for (RefexDynamicVersionBI<?> rd : assemblageConcept.getRefexesDynamicActive(StandardViewCoordinates.getSnomedInferredThenStatedLatest()))
		{
			if (rd.getAssemblageNid() == RefexDynamic.REFEX_DYNAMIC_DEFINITION.getNid())
			{
				RefexDynamicDataBI[] refexDefinitionData = rd.getData();
				if (refexDefinitionData == null || refexDefinitionData.length != 3)
				{
					throw new InvalidCAB("The Assemblage concept: " + assemblageConcept + " is not correctly assembled for use as an Assemblage for " 
							+ "a RefexDynamicData Refex Type.  It must contain a 3 column RefexDynamicDataBI attachment.");
				}
				
				//col 0 is the column number, 
				//col 1 is the concept with col name 
				//col 2 is the column data type, stored as a string.
				try
				{
					int column = (Integer)refexDefinitionData[0].getDataObject();
					allowedColumnInfo.put(column, RefexDynamicDataType.valueOf((String)refexDefinitionData[2].getDataObject()));
				}
				catch (Exception e)
				{
					throw new InvalidCAB("The Assemblage concept: " + assemblageConcept + " is not correctly assembled for use as an Assemblage for " 
							+ "a RefexDynamicData Refex Type.  The first column must have a data type of integer, and the third column must be a string "
							+ "that is parseable as a RefexDynamicDataType");
				}
			}
		}
		
		int i = 0;
		for (int key : allowedColumnInfo.keySet())
		{
			if (key !=  i++)
			{
				throw new InvalidCAB("The Assemblage concept: " + assemblageConcept + " is not correctly assembled for use as an Assemblage for " 
						+ "a RefexDynamicData Refex Type.  It must contain sequential column numbers, with no gaps, which start at 0.");
			}
		}
		
		//Note, this could be done before the code above, but I'd rather ensure that the Assemblage concept is properly configured, 
		//even if they are not providing data.
		if (data == null)
		{
			return;
		}
		
		if (data.length != allowedColumnInfo.size())
		{
			throw new InvalidCAB("The Assemblage concept: " + assemblageConcept + " specifies " + allowedColumnInfo.size() + 
					" columns of data, while the provided data contains " + data.length + " columns.  The data size array must match (but null values are allowed"
					+ " within the array)");
		}
		
		for (int dataColumn = 0; i < data.length; i++)
		{
			RefexDynamicDataType allowedDT = allowedColumnInfo.get(dataColumn);
			if (data[dataColumn] != null && data[dataColumn].getRefexDataType() != allowedDT)
			{
				throw new InvalidCAB("The supplied data for column " + dataColumn + " is of type " + data[dataColumn].getRefexDataType() + 
						" but the assemblage concept declares that it must be " + allowedDT);
			}
		}
	}
}
