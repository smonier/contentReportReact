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

import org.jahia.exceptions.JahiaException;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.jcr.RepositoryException;
import java.util.Locale;

/**
 * ReportByLanguage Class.
 *
 * Created by Juan Carlos Rodas.
 */
public class ReportByLanguage extends BaseReport {

    private static Logger logger = LoggerFactory.getLogger(ReportByLanguage.class);

    /**
     * The constructor for the class.
     *
     * @param siteNode {@link JCRSiteNode}
     */
    public ReportByLanguage(JCRSiteNode siteNode) {
        super(siteNode);
    }

    @Override
    public void execute(JCRSessionWrapper session, int offset, int limit) throws RepositoryException, JSONException, JahiaException {
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
        JSONArray jArray = new JSONArray();
        JSONObject jsonObjectItem;

        for(Locale locale :  siteNode.getLanguagesAsLocales()){
            jsonObjectItem = new JSONObject();
            jsonObjectItem.put("language", locale.getLanguage());
            jsonObjectItem.put("displayLanguage", locale.getDisplayLanguage());
            jsonObjectItem.put("country", locale.getCountry());
            jsonObjectItem.put("displayCountry", locale.getDisplayCountry());
            jsonObjectItem.put("locale", locale.toString());
            jsonObjectItem.put("displayName", locale.getDisplayName());
            jsonObjectItem.put("displayScript", locale.getDisplayScript());
            jsonObjectItem.put("displayVariant", locale.getDisplayVariant());
            jsonObjectItem.put("availableEdit", !siteNode.getInactiveLanguages().contains(locale.toString()));
            jsonObjectItem.put("availableLive", !siteNode.getInactiveLiveLanguages().contains(locale.toString()));
            jArray.put(jsonObjectItem);
        }

        jsonObject.put("siteName", siteNode.getName());
        jsonObject.put("siteDisplayableName", siteNode.getDisplayableName());
        jsonObject.put("languageItems", jArray);
        return jsonObject;
    }

}
