package org.jahia.modules.contentreports.bean;

import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.exceptions.JahiaException;
import org.jahia.modules.contentreports.service.ModuleComponentTypes;
import org.jahia.modules.contentreports.service.SiteModuleInventory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;

/**
 * Report 29: the modules deployed on the site.
 *
 * One row per module: id, name, version, module type, how it is deployed (template set,
 * installed, or transitive dependency), bundle state, and how many component types it declares.
 * The list is small, so it is returned whole and paginated on the client like the other
 * parameter-less reports.
 */
public class ReportSiteModules extends BaseReport {

    private final List<JSONArray> rows = new ArrayList<>();

    /**
     * @param siteNode the site to report on
     */
    public ReportSiteModules(JCRSiteNode siteNode) {
        super(siteNode);
    }

    @Override
    public void execute(JCRSessionWrapper session, int offset, int limit) throws RepositoryException, JSONException, JahiaException {
        rows.clear();
        for (SiteModuleInventory.Entry entry : SiteModuleInventory.forSite(siteNode)) {
            JahiaTemplatesPackage module = entry.getModule();

            JSONArray row = new JSONArray();
            row.put(module.getId());
            row.put(module.getName());
            row.put(module.getVersion() != null ? module.getVersion().toString() : "");
            row.put(module.getModuleType());
            row.put(entry.getDependency().key());
            row.put(module.getState() != null && module.getState().getState() != null ? module.getState().getState().name() : "");
            row.put(ModuleComponentTypes.componentTypesOf(module.getId()).size());
            rows.add(row);
        }
    }

    @Override
    public JSONObject getJson() throws JSONException, RepositoryException {
        JSONArray data = new JSONArray();
        for (JSONArray row : rows) {
            data.put(row);
        }

        JSONObject json = new JSONObject();
        json.put("recordsTotal", rows.size());
        json.put("recordsFiltered", rows.size());
        json.put("siteName", siteNode.getName());
        json.put("siteDisplayableName", siteNode.getDisplayableName());
        json.put("data", data);
        return json;
    }
}
