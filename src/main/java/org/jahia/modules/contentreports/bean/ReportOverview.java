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

import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.exceptions.JahiaException;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.query.QueryWrapper;
import org.jahia.services.templates.JahiaTemplateManagerService;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * The ReportOverview Class
 *
 * Created by Juan Carlos Rodas.
 */
public class ReportOverview extends BaseReport {
    private static Logger logger = LoggerFactory.getLogger(ReportOverview.class);
    protected static final String BUNDLE = "resources.content-reports";

    protected DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private Integer pagesNumber;
    private Integer templatesNumber;
    private Integer usersNumber;
    private Integer contentsNumber;
    private Integer editorialContentsNumber;
    private Integer workflowTasksNumber;
    private Integer filesNumber;
    private Integer imagesNumber;
    private List<String> languages;


    /**
     * Instantiates a new Report overview.
     *
     * @param siteNode the site node {@link JCRSiteNode}
     */
    public ReportOverview(JCRSiteNode siteNode) {
        super(siteNode);

        this.pagesNumber     = 0;
        this.templatesNumber = 0;
        this.usersNumber     = 0;
        this.contentsNumber  = 0;
        this.editorialContentsNumber = 0;
        this.workflowTasksNumber = 0;
        this.filesNumber = 0;
        this.imagesNumber = 0;
        this.languages = new ArrayList<>();
    }

    @Override
    public void execute(JCRSessionWrapper session, int offset, int limit) throws RepositoryException, JSONException, JahiaException {
        /* getting the templates for site */
        JahiaTemplateManagerService templateService = ServicesRegistry.getInstance().getJahiaTemplateManagerService();
        List<JahiaTemplatesPackage>  tpack = templateService.getInstalledModulesForSite(siteNode.getSiteKey(), true, true, false);
        this.templatesNumber = tpack.size();

        /* getting the users for site */
        JahiaUserManagerService userService = ServicesRegistry.getInstance().getJahiaUserManagerService();
        List<String> uList = userService.getUserList(siteNode.getSiteKey());
        this.usersNumber = uList.size();

        /* getting the pages count */
        String pageQueryStr = "SELECT [rep:count(item,skipChecks=1)] FROM [jnt:page] AS item WHERE ISDESCENDANTNODE(item,['" + siteNode.getPath() + "'])";
        QueryWrapper q = session.getWorkspace().getQueryManager().createQuery(pageQueryStr, Query.JCR_SQL2);
        this.pagesNumber = (int) q.execute().getRows().nextRow().getValue("count").getLong();
        
        /* getting the jnt:content count */
        String contentQueryStr = "SELECT [rep:count(item,skipChecks=1)] FROM [jnt:content] AS item WHERE ISDESCENDANTNODE(item,['" + siteNode.getPath() + "'])";
        QueryWrapper contentQuery = session.getWorkspace().getQueryManager().createQuery(contentQueryStr, Query.JCR_SQL2);
        this.contentsNumber = (int) contentQuery.execute().getRows().nextRow().getValue("count").getLong();
        
        /* getting the jmix:editorialContent count */
        String editorialQueryStr = "SELECT [rep:count(item,skipChecks=1)] FROM [jmix:editorialContent] AS item WHERE ISDESCENDANTNODE(item,['" + siteNode.getPath() + "'])";
        QueryWrapper editorialQuery = session.getWorkspace().getQueryManager().createQuery(editorialQueryStr, Query.JCR_SQL2);
        this.editorialContentsNumber = (int) editorialQuery.execute().getRows().nextRow().getValue("count").getLong();
        
        /* getting the pending workflow tasks count - nodes with workflow processes */
        String workflowQueryStr = "SELECT [rep:count(item,skipChecks=1)] FROM [jmix:workflow] AS item WHERE [j:processId] is not null AND ISDESCENDANTNODE(item,['" + siteNode.getPath() + "'])";
        QueryWrapper workflowQuery = session.getWorkspace().getQueryManager().createQuery(workflowQueryStr, Query.JCR_SQL2);
        this.workflowTasksNumber = (int) workflowQuery.execute().getRows().nextRow().getValue("count").getLong();
        
        /* getting the files count (all assets) */
        String filesQueryStr = "SELECT [rep:count(item,skipChecks=1)] FROM [jnt:file] AS item WHERE ISDESCENDANTNODE(item,['" + siteNode.getPath() + "'])";
        QueryWrapper filesQuery = session.getWorkspace().getQueryManager().createQuery(filesQueryStr, Query.JCR_SQL2);
        this.filesNumber = (int) filesQuery.execute().getRows().nextRow().getValue("count").getLong();
        
        /* getting the images count */
        String imagesQueryStr = "SELECT [rep:count(item,skipChecks=1)] FROM [jmix:image] AS item WHERE ISDESCENDANTNODE(item,['" + siteNode.getPath() + "'])";
        QueryWrapper imagesQuery = session.getWorkspace().getQueryManager().createQuery(imagesQueryStr, Query.JCR_SQL2);
        this.imagesNumber = (int) imagesQuery.execute().getRows().nextRow().getValue("count").getLong();
        
        /* getting the site languages */
        Set<String> languageSet = siteNode.getLanguages();
        if (languageSet != null && !languageSet.isEmpty()) {
            this.languages = new ArrayList<>(languageSet);
            Collections.sort(this.languages);
        }
    }

    /**
     * getJson
     *
     * @return {@link JSONObject}
     * @throws JSONException
     * @throws RepositoryException
     */
    public JSONObject getJson() throws JSONException, RepositoryException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("siteName", siteNode.getName());
        jsonObject.put("siteDisplayableName", siteNode.getDisplayableName());
        jsonObject.put("nbPages", pagesNumber);
        jsonObject.put("nbTemplates", templatesNumber);
        jsonObject.put("nbUsers", usersNumber);
        jsonObject.put("nbContents", contentsNumber);
        jsonObject.put("nbEditorialContents", editorialContentsNumber);
        jsonObject.put("nbWorkflowTasks", workflowTasksNumber);
        jsonObject.put("nbFiles", filesNumber);
        jsonObject.put("nbImages", imagesNumber);
        jsonObject.put("languages", languages);
        jsonObject.put("nbLanguages", languages.size());
        return jsonObject;
    }

}
