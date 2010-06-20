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
package org.apache.chemistry.opencmis.inmemory.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.tree.Tree;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.inmemory.TypeManager;
import org.apache.chemistry.opencmis.inmemory.TypeValidator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * QueryObject is a class used to encapsulate a CMIS query. It is created from an ANTLR
 * parser on an incoming query string. During parsing varoius informations are collected
 * and stored in objects suitable for evaluating the query (like selected properties,
 * effected types and order statements. A query evaluator can use this information to
 * perform the query and build the result.  
 *
 */
public class QueryObject {
    
    private static Log LOG = LogFactory.getLog(QueryObject.class);
    
    // For error handling see: http://www.antlr.org/pipermail/antlr-interest/2008-April/027600.html
    // select part 
    private TypeManager typeMgr;
    private List<CmisSelector> selectReferences = new ArrayList<CmisSelector> ();
    private List<CmisSelector> whereReferences = new ArrayList<CmisSelector> ();
    private List<CmisSelector> joinReferences = new ArrayList<CmisSelector> (); // --> Join not implemented yet
    private Map<String, CmisSelector> colOrFuncAlias = new HashMap<String, CmisSelector>();
    private IQueryConditionProcessor queryProcessor;
    
    // from part
    /** 
     * map from alias name to type query name
     */
    private Map<String,String> froms = new LinkedHashMap<String,String>();

    // where part
//    private IdentityHashMap<Object, CmisSelector> columnReferences = new IdentityHashMap<Object, CmisSelector>(); 
    private Map<Integer, CmisSelector> columnReferences = new HashMap<Integer, CmisSelector>(); 

    // order by part
    private List<SortSpec> sortSpecs = new ArrayList<SortSpec>();

    public class SortSpec {
        private  boolean ascending;
        private Integer colRefKey; // key in columnReferencesMap point to column descriptions
        
        public SortSpec(Integer key, boolean ascending) {
            this.colRefKey = key;
            this.ascending = ascending;
        }
        
        public CmisSelector getSelector() {
            return columnReferences.get(colRefKey);
        }
        
        public boolean isAscending() {
            return ascending;
        }
    };
    
    public QueryObject() {
    }
    
    public QueryObject(TypeManager tm, IQueryConditionProcessor wp) {
        typeMgr = tm;        
        queryProcessor = wp;
    }
    
    public Map<Integer, CmisSelector> getColumnReferences() {
        return Collections.unmodifiableMap(columnReferences);
    }
    
    public CmisSelector getColumnReference (Integer token) {
        return columnReferences.get(token);
    }
    
    
    /////////////////////////////////////////////////////////
    // SELECT part
    
    // public accessor methods
    public List<CmisSelector> getSelectReferences() {
        return selectReferences;
    }

    void addSelectReference(Object node, CmisSelector selRef) {
        selectReferences.add(selRef);        
        columnReferences.put(((Tree)node).getTokenStartIndex(), selRef);
    }

    void addAlias(String aliasName, CmisSelector aliasRef) {
        LOG.debug("add alias: " + aliasName + " for: " + aliasRef);
        if (colOrFuncAlias.containsKey(aliasName)) {
            throw new CmisInvalidArgumentException("You cannot use name " + aliasName + " more than once as alias in a select.");
        } else {
            aliasRef.setAliasName(aliasName);
            colOrFuncAlias.put(aliasName, aliasRef);
        }
    }
    
    CmisSelector getSelectAlias(String aliasName) {
        return colOrFuncAlias.get(aliasName);
    }

    /////////////////////////////////////////////////////////
    // FROM part

    void addType(String aliasName, String typeQueryName) {
        LOG.debug("add alias: " + aliasName + " for: " + typeQueryName);
        if (froms.containsKey(aliasName)) {
            throw new CmisInvalidArgumentException ("You cannot use name " + aliasName + " more than once as alias in a from part.");
        } else {
            if (null != aliasName)
                froms.put(aliasName, typeQueryName);
            else
                froms.put(typeQueryName, typeQueryName);
        }
    }

    public Map<String,String> getTypes() {
        return Collections.unmodifiableMap(froms);
    }
    
    public String getTypeQueryName(String alias) {
        return froms.get(alias);
    }
    
    public TypeDefinition getTypeDefinitionFromQueryName(String queryName) {
        return typeMgr.getTypeByQueryName(queryName);       
    }
    
    public TypeDefinition getParentType(TypeDefinition td) {
        String parentType = td.getParentTypeId();
        return parentType==null ? null : typeMgr.getTypeById(parentType).getTypeDefinition();
    }
    
    public TypeDefinition getParentType(String typeId) {
        TypeDefinition td = typeMgr.getTypeById(typeId).getTypeDefinition();
        String parentType = td == null ? null : td.getParentTypeId();
        return parentType==null ? null : typeMgr.getTypeById(parentType).getTypeDefinition();
    }
    
    public TypeDefinition getMainFromName() {
        String queryName = froms.values().iterator().next(); // as we don't support JOINS take first type
        TypeDefinition td = getTypeDefinitionFromQueryName(queryName);
        return td;
    }
    
    public Map<String, String> getRequestedProperties() {
        Map<String, String> res = new HashMap<String, String> ();
        for (CmisSelector sel : selectReferences) {
            if (sel instanceof ColumnReference) {
                ColumnReference colRef = (ColumnReference) sel;
                String key = colRef.getPropertyId();
                if (null == key)
                    key = colRef.getPropertyQueryName(); // happens for *
                String propDescr = colRef.getAliasName() == null ? colRef.getPropertyQueryName() : colRef.getAliasName();
                res.put(key, propDescr);
            }
        }
        return res;
    }
    
    public Map<String, String> getRequestedFuncs() {
        Map<String, String> res = new HashMap<String, String> ();
        for (CmisSelector sel : selectReferences) {
            if (sel instanceof FunctionReference) {
                FunctionReference funcRef = (FunctionReference) sel;
                String propDescr = funcRef.getAliasName() == null ? funcRef.getName() : funcRef.getAliasName();
                res.put(funcRef.getName(), propDescr);
            }
        }
        return res;
    }
    
    /////////////////////////////////////////////////////////
    // JOINS
    
    void addJoinReference(Object node, CmisSelector reference) {
        columnReferences.put(((Tree)node).getTokenStartIndex(), reference);
        joinReferences.add(reference);
    }
    
    public List<CmisSelector> getJoinReferences() {
        return Collections.unmodifiableList(joinReferences);
    }

    
    /////////////////////////////////////////////////////////
    // WHERE part
    
    void addWhereReference(Object node, CmisSelector reference) {
        LOG.debug("add node to where: " + System.identityHashCode(node));

        columnReferences.put(((Tree)node).getTokenStartIndex(), reference);
        whereReferences.add(reference);
    }
    
    public List<CmisSelector> getWhereReferences() {
        return Collections.unmodifiableList(whereReferences);
    }

    
    /////////////////////////////////////////////////////////
    // ORDER_BY part
    
    public List<SortSpec> getOrderBys() {
        return Collections.unmodifiableList(sortSpecs);
    }
    
    public void addSortCriterium(Tree node, ColumnReference colRef, boolean ascending) {
        LOG.debug("addSortCriterium: " + colRef + " ascending: " + ascending);
        columnReferences.put(node.getTokenStartIndex(), colRef);
        sortSpecs.add(new SortSpec(node.getTokenStartIndex(), ascending));
    }

    /////////////////////////////////////////////////////////
    // resolve types after first pass traversing the AST is complete
    
    public void resolveTypes() {
        LOG.debug("First pass of query traversal is complete, resolving types");
        if (null == typeMgr)
            return;
        
        // First resolve all alias names defined in SELECT:
        for (CmisSelector alias : colOrFuncAlias.values()) {
            if (alias instanceof ColumnReference) { 
                ColumnReference colRef = ((ColumnReference) alias);
                resolveTypeForAlias(colRef);
            }
        }
        
        // Then replace all aliases used somewhere by their resolved column reference:
        for (Integer obj : columnReferences.keySet()) {
            CmisSelector selector = columnReferences.get(obj);
            String key = selector.getName();
            if (colOrFuncAlias.containsKey(key)) { // it is an alias
              CmisSelector resolvedReference = colOrFuncAlias.get(key);
              columnReferences.put(obj, resolvedReference);
                // Note: ^ This may replace the value in the map with the same value, but this does not harm.
                //       Otherwise we need to check if it is resolved or not which causes two more ifs: 
//                if (selector instanceof ColumnReference) {
//                    ColumnReference colRef = ((ColumnReference) selector);
//                    if (colRef.getTypeDefinition() == null) // it is not yet resolved
//                        // replace unresolved column reference by resolved on from alias map
//                        columnReferences.put(obj, colOrFuncAlias.get(selector.getAliasName()));
//                } else
//                    columnReferences.put(obj, colOrFuncAlias.get(selector.getAliasName()));
              if (whereReferences.remove(selector))
                  whereReferences.add(resolvedReference); // replace unresolved by resolved reference
              if (joinReferences.remove(selector))
                  joinReferences.add(resolvedReference); // replace unresolved by resolved reference
            }
        }
        
        // The replace all remaining column references not using an alias
        for (CmisSelector select : columnReferences.values()) {
            // ignore functions here
            if (select instanceof ColumnReference) { 
                ColumnReference colRef = ((ColumnReference) select);
                if (colRef.getTypeDefinition() == null) { // not yet resolved
                    if (colRef.getTypeQueryName() == null) {
                        // unqualified select: SELECT p FROM
                        resolveTypeForColumnReference(colRef);
                    } else {
                        // qualified select: SELECT t.p FROM
                        validateColumnReferenceAndResolveType(colRef);
                    }
                }
            }
        }
    }
    
    
    private void resolveTypeForAlias(ColumnReference colRef) {
        String aliasName = colRef.getAliasName();
        
        if (colOrFuncAlias.containsKey(aliasName)) {
            CmisSelector selector = colOrFuncAlias.get(aliasName);
            if (selector instanceof ColumnReference) {
                colRef = (ColumnReference) selector; // alias target
                if (colRef.getTypeQueryName() == null) {
                    // unqualified select: SELECT p FROM
                    resolveTypeForColumnReference(colRef);
                } else {
                    // qualified select: SELECT t.p FROM
                    validateColumnReferenceAndResolveType(colRef);
                }
            }
            // else --> ignore FunctionReference
        }
    }
        
        
    // for a select x from y, z ... find the type in type manager for x
    private void resolveTypeForColumnReference(ColumnReference colRef) {
        String propName = colRef.getPropertyQueryName();
        boolean isStar = propName.equals("*");
        
        // it is property query name without a type, so find type
        int noFound = 0;
        TypeDefinition tdFound = null;
        for (String typeQueryName : froms.values()) {
            TypeDefinition td = typeMgr.getTypeByQueryName(typeQueryName);
            if (null == td) 
                throw new CmisInvalidArgumentException(typeQueryName + " is neither a type query name nor an alias.");
            else if (isStar) {
                ++noFound;
                tdFound = null;
            } else if (TypeValidator.typeContainsPropertyWithQueryName(td, propName)) {
                ++noFound;
                tdFound = td;
            }
        }
        if (noFound == 0)
            throw new CmisInvalidArgumentException(propName + " is not a property query name in any of the types in from ...");
        else if (noFound > 1 && !isStar)
            throw new CmisInvalidArgumentException(propName + " is not a unique property query name within the types in from ...");
        else {
            if (null != tdFound) // can be null in select * from t1 JOIN t2....
                validateColumnReferenceAndResolveType(tdFound, colRef);
        }
    }

    // for a select x.y from x ... check that x has property y and that x is in from
    private void validateColumnReferenceAndResolveType(ColumnReference colRef) {
        
        String typeQueryName = getReferencedTypeQueryName(colRef.getTypeQueryName()); // either same name or mapped alias
        TypeDefinition td = typeMgr.getTypeByQueryName(typeQueryName);
        if (null == td) 
            throw new CmisInvalidArgumentException(colRef.getTypeQueryName() + " is neither a type query name nor an alias.");
        
        validateColumnReferenceAndResolveType(td, colRef);
    }

    private void validateColumnReferenceAndResolveType(TypeDefinition td, ColumnReference colRef) {
        
        // type found, check if property exists
        boolean hasProp;
        if (colRef.getPropertyQueryName().equals("*"))
            hasProp = true;
        else
            hasProp = TypeValidator.typeContainsPropertyWithQueryName(td, colRef.getPropertyQueryName());
        if (!hasProp)
            throw new CmisInvalidArgumentException(colRef.getPropertyQueryName() + " is not a valid property query name in type " + td.getId() + ".");
        
        colRef.setTypeDefinition(typeMgr.getPropertyIdForQueryName(td, colRef.getPropertyQueryName()), td);
    }
    
    // return type query name for a referenced column (which can be the name itself or an alias
    private String getReferencedTypeQueryName(String typeQueryNameOrAlias) {
        String typeQueryName = froms.get(typeQueryNameOrAlias);
        if (null == typeQueryName) {
            // if an alias was defined but still the original is used we have to search case: SELECT T.p FROM T AS TAlias 
            for (String tqn : froms.values()) {
              if (typeQueryNameOrAlias.equals(tqn))
                  return tqn;
            }
            return null;
        } else
            return typeQueryName;
    }
    
    
    public void processWhereClause(Tree whereRoot)
    {
        if (null!=queryProcessor && null != whereRoot) {
            queryProcessor.onStartProcessing(whereRoot);
            processWhereNode(whereRoot);
            queryProcessor.onStopProcessing();
        }
    }
    
    private void processWhereNode(Tree root) {
        int count = root.getChildCount();
        for (int i=0; i<count; i++) {
            Tree child = root.getChild(i);
            processWhereNode(child);
            evalWhereNode(child); // recursive descent
        }
    }
    
    /////////////////////////////////////////////////////////
    // Processing the WHERE clause
    
    void evalWhereNode(Tree node) {
        // Ensure that we receive only valid tokens and nodes in the where clause:
            LOG.debug("evaluating node: " + node.toString());
            switch (node.getType()) {
            case CMISQLLexerStrict.WHERE:;
                break; // ignore
            case CMISQLLexerStrict.EQ:
                queryProcessor.onEquals(node, node.getChild(0), node.getChild(1));
                break;
            case CMISQLLexerStrict.NEQ:
                queryProcessor.onNotEquals(node, node.getChild(0), node.getChild(1));
                break;
            case CMISQLLexerStrict.GT:
                queryProcessor.onGreaterThan(node, node.getChild(0), node.getChild(1));
                break;
            case CMISQLLexerStrict.GTEQ:
                queryProcessor.onGreaterOrEquals(node, node.getChild(0), node.getChild(1));
                break;
            case CMISQLLexerStrict.LT:
                queryProcessor.onLessThan(node, node.getChild(0), node.getChild(1));
                break;
            case CMISQLLexerStrict.LTEQ:
                queryProcessor.onLessOrEquals(node, node.getChild(0), node.getChild(1));
                break;
                
            case CMISQLLexerStrict.NOT:
                queryProcessor.onNot(node, node.getChild(0));
                break;
            case CMISQLLexerStrict.AND:
                queryProcessor.onAnd(node, node.getChild(0), node.getChild(1));
                break;
            case CMISQLLexerStrict.OR:
                queryProcessor.onOr(node, node.getChild(0), node.getChild(1));
                break;
                
            // Multi-value:
            case CMISQLLexerStrict.IN:
                queryProcessor.onIn(node, node.getChild(0), node.getChild(1));
                break;
            case CMISQLLexerStrict.NOT_IN:
                queryProcessor.onNotIn(node, node.getChild(0), node.getChild(1));
                break;
            case CMISQLLexerStrict.IN_ANY:
                queryProcessor.onInAny(node, node.getChild(0), node.getChild(1));
                break;
            case CMISQLLexerStrict.NOT_IN_ANY:
                queryProcessor.onNotInAny(node, node.getChild(0), node.getChild(1));
                break;
            case CMISQLLexerStrict.EQ_ANY:
                queryProcessor.onEqAny(node, node.getChild(0), node.getChild(1));
                break;

            // Null comparisons:
            case CMISQLLexerStrict.IS_NULL:
                queryProcessor.onIsNull(node, node.getChild(0));
                break;
            case CMISQLLexerStrict.IS_NOT_NULL:
                queryProcessor.onIsNotNull(node, node.getChild(0));
                break;

                // String matching
            case CMISQLLexerStrict.LIKE:
                queryProcessor.onIsLike(node, node.getChild(0), node.getChild(1));
                break;
            case CMISQLLexerStrict.NOT_LIKE:
                queryProcessor.onIsNotLike(node, node.getChild(0), node.getChild(1));
                break;

                // Functions
            case CMISQLLexerStrict.CONTAINS:
                if (node.getChildCount()==1)
                    queryProcessor.onContains(node, null, node.getChild(0));
                else
                    queryProcessor.onContains(node, node.getChild(0), node.getChild(1));
                break;
            case CMISQLLexerStrict.IN_FOLDER:
                if (node.getChildCount()==1)
                    queryProcessor.onInFolder(node, null, node.getChild(0));
                else
                    queryProcessor.onInFolder(node, node.getChild(0), node.getChild(1));
                break;
            case CMISQLLexerStrict.IN_TREE:
                if (node.getChildCount()==1)
                    queryProcessor.onInTree(node, null, node.getChild(0));
                else
                    queryProcessor.onInTree(node, node.getChild(0), node.getChild(1));
                break;
            case CMISQLLexerStrict.SCORE:
                queryProcessor.onScore(node, node.getChild(0));
                break;
                
            default:
               // do nothing;
            }
    }
}