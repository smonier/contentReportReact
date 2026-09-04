package org.jahia.modules.contentreports.bean;

import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.exceptions.JahiaException;
import org.jahia.modules.contentreports.service.ModuleComponentTypes;
import org.jahia.modules.contentreports.service.SiteModuleInventory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.query.QueryWrapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Report 30: component usage per module.
 *
 * For every module deployed on the site, lists each component type it declares and how many
 * nodes of exactly that type exist under the site. Types with zero instances are listed too;
 * finding them is the point of the report.
 *
 * "Exactly that type" means the node's primary type, so an instance of a subtype counts for the
 * subtype only. Counts are read through the caller's session, so JCR ACLs apply.
 */
public class ReportComponentUsage extends BaseReport {

    private static final Logger logger = LoggerFactory.getLogger(ReportComponentUsage.class);

    /** One component type of one module, with its instance count. */
    private static final class Row {
        final String moduleId;
        final String moduleName;
        final String typeName;
        final String typeLabel;
        /** Null when the count query failed for this type; rendered as unknown rather than zero. */
        final Long usage;

        Row(String moduleId, String moduleName, String typeName, String typeLabel, Long usage) {
            this.moduleId = moduleId;
            this.moduleName = moduleName;
            this.typeName = typeName;
            this.typeLabel = typeLabel;
            this.usage = usage;
        }
    }

    private final List<Row> rows = new ArrayList<>();

    /**
     * @param siteNode the site to report on
     */
    public ReportComponentUsage(JCRSiteNode siteNode) {
        super(siteNode);
    }

    @Override
    public void execute(JCRSessionWrapper session, int offset, int limit) throws RepositoryException, JSONException, JahiaException {
        rows.clear();
        for (SiteModuleInventory.Entry entry : SiteModuleInventory.forSite(siteNode)) {
            JahiaTemplatesPackage module = entry.getModule();
            for (ExtendedNodeType type : ModuleComponentTypes.componentTypesOf(module.getId())) {
                rows.add(new Row(module.getId(), module.getName(), type.getName(), type.getLabel(locale),
                        countInstances(session, type.getName())));
            }
        }

        // Module order first (template set, installed, dependencies), then most-used first within a module.
        rows.sort(Comparator
                .comparing((Row r) -> r.moduleId, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(r -> r.usage == null ? Long.MIN_VALUE : -r.usage)
                .thenComparing(r -> r.typeName));
    }

    /**
     * Counts nodes whose primary type is exactly the given type, anywhere under the site.
     *
     * @param session  the caller's session
     * @param typeName a registered node type name, taken from the registry rather than user input
     * @return the count, or null when the query failed
     */
    private Long countInstances(JCRSessionWrapper session, String typeName) {
        String queryStr = "SELECT [rep:count(item,skipChecks=1)] FROM [" + typeName + "] AS item " +
                "WHERE item.[jcr:primaryType] = '" + typeName + "' " +
                "AND ISDESCENDANTNODE(item,['" + siteNode.getPath() + "'])";
        try {
            QueryWrapper query = session.getWorkspace().getQueryManager().createQuery(queryStr, Query.JCR_SQL2);
            return query.execute().getRows().nextRow().getValue("count").getLong();
        } catch (RepositoryException e) {
            logger.warn("Cannot count instances of {} under {}", typeName, siteNode.getPath(), e);
            return null;
        }
    }

    @Override
    public JSONObject getJson() throws JSONException, RepositoryException {
        JSONArray data = new JSONArray();
        for (Row row : rows) {
            JSONArray item = new JSONArray();
            item.put(row.moduleId);
            item.put(row.moduleName);
            item.put(row.typeName);
            item.put(row.typeLabel);
            item.put(row.usage == null ? JSONObject.NULL : row.usage);
            data.put(item);
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
