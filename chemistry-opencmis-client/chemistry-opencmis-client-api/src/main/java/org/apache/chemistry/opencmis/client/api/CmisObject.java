/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.chemistry.opencmis.client.api;

import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.CmisExtensionElement;
import org.apache.chemistry.opencmis.commons.enums.AclPropagation;
import org.apache.chemistry.opencmis.commons.enums.ExtensionLevel;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;

/**
 * Base interface for all CMIS objects.
 */
public interface CmisObject extends ObjectId, CmisObjectProperties {

    // object

    /**
     * Returns the allowable actions if they have been fetched for this object.
     * 
     * @cmis 1.0
     */
    AllowableActions getAllowableActions();

    /**
     * Returns the relationships if they have been fetched for this object.
     * 
     * @cmis 1.0
     */
    List<Relationship> getRelationships();

    /**
     * Returns the ACL if it has been fetched for this object.
     * 
     * @cmis 1.0
     */
    Acl getAcl();

    // object service

    /**
     * Deletes this object. If this object is a document, the whole version
     * series is deleted.
     * 
     * @cmis 1.0
     */
    void delete();

    /**
     * Deletes this object.
     * 
     * @param allVersions
     *            if this object is a document this parameter defines whether
     *            only this version ({@code false}) or all versions ({@code true}
     *            ) should be deleted, the parameter is ignored for all other
     *            object types
     * 
     * @cmis 1.0
     */
    void delete(boolean allVersions);

    /**
     * Updates the provided properties and refreshes this object afterwards. If
     * the repository created a new object, for example a new version, this new
     * object is returned. Otherwise the current object is returned.
     * 
     * @param properties
     *            the properties to update
     * 
     * @return the updated object
     * 
     * @cmis 1.0
     */
    CmisObject updateProperties(Map<String, ?> properties);

    /**
     * Updates the provided properties. If the repository created a new object,
     * for example a new version, the object ID of the new object is returned.
     * Otherwise the object ID of the current object is returned.
     * 
     * @param properties
     *            the properties to update
     * @param refresh
     *            {@code true} if this object should be refresh after the
     *            update, {@code false} if not
     * 
     * @return the object ID of the updated object
     * 
     * @cmis 1.0
     */
    ObjectId updateProperties(Map<String, ?> properties, boolean refresh);

    /**
     * Renames this object (changes the value of {@code cmis:name}). If the
     * repository created a new object, for example a new version, this new
     * object is returned. Otherwise the current object is returned.
     * 
     * @param newName
     *            the new name, not {@code null} or empty
     * 
     * @return the updated object
     * 
     * @cmis 1.0
     */
    CmisObject rename(String newName);

    /**
     * Renames this object (changes the value of {@code cmis:name}). If the
     * repository created a new object, for example a new version, the object id
     * of the new object is returned. Otherwise the object id of the current
     * object is returned.
     * 
     * @param newName
     *            the new name, not {@code null} or empty
     * @param refresh
     *            {@code true} if this object should be refresh after the
     *            update, {@code false} if not
     * 
     * @return the object ID of the updated object
     * 
     * @cmis 1.0
     */
    ObjectId rename(String newName, boolean refresh);

    // renditions

    /**
     * Returns the renditions if they have been fetched for this object.
     * 
     * @cmis 1.0
     */
    List<Rendition> getRenditions();

    // policy service

    /**
     * Applies the provided policies and refreshes this object afterwards.
     * 
     * @param policyIds
     *            the IDs of the policies to be applied
     * 
     * @cmis 1.0
     */
    void applyPolicy(ObjectId... policyIds);

    /**
     * Removes the provided policies and refreshes this object afterwards.
     * 
     * @param policyIds
     *            the IDs of the policies to be removed
     * 
     * @cmis 1.0
     */
    void removePolicy(ObjectId... policyIds);

    /**
     * Returns the applied policies if they have been fetched for this object.
     * 
     * @cmis 1.0
     */
    List<Policy> getPolicies();

    // ACL service

    /**
     * Adds and removes ACEs to the object and refreshes this object afterwards.
     * 
     * @return the new ACL of this object
     * 
     * @cmis 1.0
     */
    Acl applyAcl(List<Ace> addAces, List<Ace> removeAces, AclPropagation aclPropagation);

    /**
     * Adds ACEs to the object and refreshes this object afterwards.
     * 
     * @return the new ACL of this object
     * 
     * @cmis 1.0
     */
    Acl addAcl(List<Ace> addAces, AclPropagation aclPropagation);

    /**
     * Removes ACEs to the object and refreshes this object afterwards.
     * 
     * @return the new ACL of this object
     * 
     * @cmis 1.0
     */
    Acl removeAcl(List<Ace> removeAces, AclPropagation aclPropagation);

    /**
     * Removes the direct ACE of this object, sets the provided ACEs to the
     * object and refreshes this object afterwards.
     * 
     * @return the new ACL of this object
     * 
     * @cmis 1.0
     */
    Acl setAcl(List<Ace> aces);

    // extensions

    /**
     * Returns the extensions for the given level.
     * 
     * @param level
     *            the level
     * 
     * @return the extensions at that level or {@code null} if there no
     *         extensions
     * 
     * @cmis 1.0
     */
    List<CmisExtensionElement> getExtensions(ExtensionLevel level);

    // adapters

    /**
     * Returns an adapter based on the given interface.
     * 
     * @return an adapter object or {@code null} if no adapter object could be
     *         created
     */
    <T> T getAdapter(Class<T> adapterInterface);

    // session handling

    /**
     * Returns the timestamp of the last refresh.
     * 
     * @return the difference, measured in milliseconds, between the last
     *         refresh time and midnight, January 1, 1970 UTC.
     */
    long getRefreshTimestamp();

    /**
     * Reloads this object from the repository.
     * 
     * @throws CmisObjectNotFoundException
     *             if the object doesn't exist anymore in the repository
     */
    void refresh();

    /**
     * Reloads the data from the repository if the last refresh did not occur
     * within {@code durationInMillis}.
     * 
     * @throws CmisObjectNotFoundException
     *             if the object doesn't exist anymore in the repository
     */
    void refreshIfOld(long durationInMillis);
}
