/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.contentreports.bean;


import org.apache.commons.lang.WordUtils;
import org.apache.jackrabbit.core.NodeImpl;
import org.jahia.exceptions.JahiaException;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * ReportByAllDate Class.
 * <p>
 * Created by Juan Carlos Rodas.
 */
public class ReportByUnstranslated extends QueryReport {

    private static final String BUNDLE = "resources.content-reports";
    private Map<Integer, Map<Integer, Map<String, Integer>>> dataMap;
    private Map<String, Map<String, Object>> pageMap;
    private Boolean useSystemUser;
    private Integer totalPages = 0;
    private Integer totalContent = 0;
    private String searchPath;
    private String typeSearch;


    String searchLanguage;

    public ReportByUnstranslated(JCRSiteNode siteNode, String searchLanguage, String searchPath, String typeSearch) {
        super(siteNode);
        this.searchPath = searchPath;
        this.searchLanguage = searchLanguage;
        this.typeSearch = typeSearch;

        this.setDataMap(new HashMap<Integer, Map<Integer, Map<String, Integer>>>());
        this.setPageMap(new HashMap<String, Map<String, Object>>());
    }

    @Override
    public void execute(JCRSessionWrapper session, int offset, int limit) throws RepositoryException, JSONException, JahiaException {
        String query;
        String nodetype;
        if (typeSearch.equals("pages")) { nodetype = "jnt:page"; } else { nodetype = "jmix:editorialContent"; }

        query = "SELECT * FROM [" + nodetype + "] AS item WHERE ISDESCENDANTNODE(item,['" + searchPath + "']) ";
        fillReport(session, query, offset, 1000); // temporary limit until a real server side pagination is implemented
        setTotalContent(pageMap.size());
    }

    /**
     * addItem
     *
     * @param node {@link JCRNodeWrapper}
     * @throws RepositoryException
     */

    public void addItem(JCRNodeWrapper node) throws RepositoryException {
        String propertyName = "";
        Date itemDate = null;
        if (node.hasNode("j:translation_"+searchLanguage)) {
            return;
        }

        boolean hasJTranslationNodes = false;
        NodeIterator iterator = node.getRealNode().getNodes();
        while (iterator.hasNext()) {
            if (((NodeImpl)iterator.next()).getName().contains("j:translation_")) {
                hasJTranslationNodes = true;
            }

        }
        if (!hasJTranslationNodes)
            return;

        pageMap.put(node.getIdentifier(), new HashMap<String, Object>());
        Map<String, Object> nodeEntry = pageMap.get(node.getIdentifier());
        nodeEntry.put("name",  WordUtils.abbreviate(node.getDisplayableName(),90,130,"..."));
        nodeEntry.put("path", node.getPath());
        nodeEntry.put("type", node.getPrimaryNodeType().getAlias());
        nodeEntry.put("date", node.getPropertyAsString("jcr:created"));

    }



    /**
     * getJson
     *
     * @return {@link JSONObject}
     * @throws JSONException
     */
    public JSONObject getJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        JSONArray jArray = new JSONArray();
        JSONObject jsonObjectItem;

        for (String content : pageMap.keySet()) {

                jsonObjectItem = new JSONObject();
                jsonObjectItem.put("title", pageMap.get(content).get("name"));
                jsonObjectItem.put("path", pageMap.get(content).get("path"));
                jsonObjectItem.put("type", pageMap.get(content).get("type"));
                jsonObjectItem.put("date", pageMap.get(content).get("date"));
                /* setting each item to the json object */
                jArray.put(jsonObjectItem);

        }

        jsonObject.put("totalContent", jArray.length());
        jsonObject.put("items", jArray);

        return jsonObject;
    }

    /**
     * getDataMap
     *
     * @return {@link Map}
     */
    public Map<Integer, Map<Integer, Map<String, Integer>>> getDataMap() {
        return dataMap;
    }

    /**
     * setDataMap
     *
     * @param dataMap {@link Map}
     */
    public void setDataMap(Map<Integer, Map<Integer, Map<String, Integer>>> dataMap) {
        this.dataMap = dataMap;
    }

    /**
     * getTotalPages
     *
     * @return {@link Integer}
     */
    public Integer getTotalPages() {
        return totalPages;
    }

    /**
     * setTotalContent
     *
     * @param totalContent {@link Integer}
     */
    public void setTotalContent(Integer totalContent) {
        this.totalContent = totalContent;
    }

    /**
     * getTotalContent
     *
     * @return {@link Integer}
     */
    public Integer getTotalContent() {
        return totalContent;
    }

    /**
     * setTotalPages
     *
     * @param totalPages {@link Integer}
     */
    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public Map<String, Map<String, Object>> getPageMap() {
        return pageMap;
    }

    public void setPageMap(Map<String, Map<String, Object>> pageMap) {
        this.pageMap = pageMap;
    }

}
