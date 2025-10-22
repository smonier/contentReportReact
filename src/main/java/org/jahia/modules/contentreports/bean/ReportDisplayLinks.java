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

import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRValueWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.jcr.*;
import javax.jcr.nodetype.PropertyDefinition;

/**
 * Short description of the class
 *
 * @author tdubreucq
 */
public class ReportDisplayLinks extends BaseReport {
    protected static final String BUNDLE = "resources.contentReportReact";
    private String originPath;
    private String destinationPath;
    private JSONArray dataList = new JSONArray();

    public ReportDisplayLinks(JCRSiteNode siteNode, String originPath, String destinationPath) {
        super(siteNode);
        this.originPath = originPath;
        this.destinationPath = destinationPath;
    }

    @Override
    public void execute(JCRSessionWrapper session, int offset, int limit) throws RepositoryException, JSONException {
        JCRNodeWrapper originNode;
        originNode = session.getNode(originPath);
        linkReferencesToNode(originNode);
    }

    private void linkReferencesToNode (JCRNodeWrapper node) throws RepositoryException {
        PropertyIterator propIt = node.getProperties() ;
        while (propIt.hasNext()) {
            Property actualProperty = propIt.nextProperty();
            PropertyDefinition actualPropertyDefinition = actualProperty.getDefinition();
            if (actualPropertyDefinition.getRequiredType() == PropertyType.REFERENCE || actualPropertyDefinition.getRequiredType() == PropertyType.WEAKREFERENCE) {
                if (actualProperty.isMultiple()) {
                    Value[] values = actualProperty.getValues();
                    for (Value value : values) {
                        JCRValueWrapper propertyValue = (JCRValueWrapper) value;
                        addItem(node, propertyValue.getNode());
                    }
                } else {
                    addItem(node, (JCRNodeWrapper) actualProperty.getNode());
                }
            }
        }

        for (JCRNodeWrapper childNode : node.getNodes()) {
            linkReferencesToNode(childNode);
        }
    }

    private void addItem(JCRNodeWrapper referenceNode, JCRNodeWrapper referencedNode) throws RepositoryException {

        if (referencedNode != null && referencedNode.getPath().startsWith(destinationPath + "/")) {
            JCRNodeWrapper scopeNode = JCRContentUtils.getParentOfType(referenceNode, "jnt:page");
            // Avoid adding nodes that can't be rendered such as users nodes for example
            if (scopeNode == null) {
                return;
            }
            JSONArray dataItem = new JSONArray();
            dataItem.put(referencedNode.getPrimaryNodeTypeName());
            dataItem.put(referencedNode.getPath());
            dataItem.put(referenceNode.getPath());
            dataItem.put(referencedNode.getPropertyAsString("jcr:lastModified"));
            dataItem.put(scopeNode.getPath());
            dataList.put(dataItem);
        }
    }

    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("recordsTotal", dataList.length());
        jsonObject.put("recordsFiltered", dataList.length());
        jsonObject.put("siteName", siteNode.getName());
        jsonObject.put("siteDisplayableName", siteNode.getDisplayableName());
        jsonObject.put("data", dataList);
        return jsonObject;
    }
}
