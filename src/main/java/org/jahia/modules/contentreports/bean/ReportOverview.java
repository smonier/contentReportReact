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
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.query.QueryWrapper;
import org.jahia.services.templates.JahiaTemplateManagerService;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * The ReportOverview Class
 *
 * Created by Juan Carlos Rodas.
 */
public class ReportOverview extends BaseReport {
    private static Logger logger = LoggerFactory.getLogger(ReportOverview.class);
    protected static final String BUNDLE = "resources.contentReportReact";

    /** Node type the activity list is restricted to, so it stays consistent with the activity counters. */
    private static final String JMIX_EDITORIAL_CONTENT = "jmix:editorialContent";
    /** Upper bound on the number of detailed activity rows returned, to keep the payload reasonable. */
    private static final int MAX_ACTIVITY_ITEMS = 500;
    /** Key of the internal sort field carrying each row's most recent activity. */
    private static final String LAST_ACTIVITY_TIMESTAMP = "lastActivityTimestamp";
    /** ISO 8601 formatter, parseable by {@code new Date(...)} on the front-end. Immutable, so shared. */
    private static final DateTimeFormatter ISO_DATE_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private Integer pagesNumber;
    private Integer templatesNumber;
    private Integer usersNumber;
    private Integer contentsNumber;
    private Integer editorialContentsNumber;
    private Integer workflowTasksNumber;
    private Integer filesNumber;
    private Integer imagesNumber;
    private List<String> languages;
    
    // Content Activity metrics (last 30 days)
    private Integer newContentLast30Days;
    private Integer modifiedContentLast30Days;
    private Integer publishedContentLast30Days;
    private Integer unpublishedNodes;
    private Integer publishedNodes;
    private Double averageTimeToPublish; // in days
    private List<Map<String, Object>> topContributors;
    private List<Map<String, Object>> recentActivity;
    private Integer recentActivityTotal;


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
        
        // Initialize activity metrics
        this.newContentLast30Days = 0;
        this.modifiedContentLast30Days = 0;
        this.publishedContentLast30Days = 0;
        this.unpublishedNodes = 0;
        this.publishedNodes = 0;
        this.averageTimeToPublish = 0.0;
        this.topContributors = new ArrayList<>();
        this.recentActivity = new ArrayList<>();
        this.recentActivityTotal = 0;
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
        String pageQueryStr = countOf("jnt:page") + descendantOfSite();
        QueryWrapper q = session.getWorkspace().getQueryManager().createQuery(pageQueryStr, Query.JCR_SQL2);
        this.pagesNumber = (int) q.execute().getRows().nextRow().getValue("count").getLong();
        
        /* getting the jnt:content count */
        String contentQueryStr = countOf("jnt:content") + descendantOfSite();
        QueryWrapper contentQuery = session.getWorkspace().getQueryManager().createQuery(contentQueryStr, Query.JCR_SQL2);
        this.contentsNumber = (int) contentQuery.execute().getRows().nextRow().getValue("count").getLong();
        
        /* getting the jmix:editorialContent count */
        String editorialQueryStr = countOf(JMIX_EDITORIAL_CONTENT) + descendantOfSite();
        QueryWrapper editorialQuery = session.getWorkspace().getQueryManager().createQuery(editorialQueryStr, Query.JCR_SQL2);
        this.editorialContentsNumber = (int) editorialQuery.execute().getRows().nextRow().getValue("count").getLong();
        
        /* getting the pending workflow tasks count - nodes with workflow processes */
        String workflowQueryStr = "SELECT [rep:count(item,skipChecks=1)] FROM [jmix:workflow] AS item WHERE [j:processId] is not null AND ISDESCENDANTNODE(item,['" + siteNode.getPath() + "'])";
        QueryWrapper workflowQuery = session.getWorkspace().getQueryManager().createQuery(workflowQueryStr, Query.JCR_SQL2);
        this.workflowTasksNumber = (int) workflowQuery.execute().getRows().nextRow().getValue("count").getLong();
        
        /* getting the files count (all assets) */
        String filesQueryStr = countOf("jnt:file") + descendantOfSite();
        QueryWrapper filesQuery = session.getWorkspace().getQueryManager().createQuery(filesQueryStr, Query.JCR_SQL2);
        this.filesNumber = (int) filesQuery.execute().getRows().nextRow().getValue("count").getLong();
        
        /* getting the images count */
        String imagesQueryStr = countOf("jmix:image") + descendantOfSite();
        QueryWrapper imagesQuery = session.getWorkspace().getQueryManager().createQuery(imagesQueryStr, Query.JCR_SQL2);
        this.imagesNumber = (int) imagesQuery.execute().getRows().nextRow().getValue("count").getLong();
        
        /* getting the site languages */
        Set<String> languageSet = siteNode.getLanguages();
        if (languageSet != null && !languageSet.isEmpty()) {
            this.languages = new ArrayList<>(languageSet);
            Collections.sort(this.languages);
        }
        
        // Calculate Content Activity metrics (last 30 days)
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -30);
        String thirtyDaysAgo = dateFormat.format(cal.getTime());
        
        /* getting new content in last 30 days */
        String newContentQueryStr = countOf(JMIX_EDITORIAL_CONTENT) +
                descendantOfSite() + " " +
                "AND [jcr:created] >= " + dateLiteral(thirtyDaysAgo);
        QueryWrapper newContentQuery = session.getWorkspace().getQueryManager().createQuery(newContentQueryStr, Query.JCR_SQL2);
        this.newContentLast30Days = (int) newContentQuery.execute().getRows().nextRow().getValue("count").getLong();
        
        /* getting modified content in last 30 days */
        String modifiedContentQueryStr = countOf(JMIX_EDITORIAL_CONTENT) +
                descendantOfSite() + " " +
                "AND [jcr:lastModified] >= " + dateLiteral(thirtyDaysAgo);
        QueryWrapper modifiedContentQuery = session.getWorkspace().getQueryManager().createQuery(modifiedContentQueryStr, Query.JCR_SQL2);
        this.modifiedContentLast30Days = (int) modifiedContentQuery.execute().getRows().nextRow().getValue("count").getLong();
        
        /* getting published content in last 30 days (nodes with j:lastPublished in last 30 days) */
        String publishedContentQueryStr = countOf("jmix:lastPublished") +
                descendantOfSite() + " " +
                "AND [j:lastPublished] >= " + dateLiteral(thirtyDaysAgo);
        QueryWrapper publishedContentQuery = session.getWorkspace().getQueryManager().createQuery(publishedContentQueryStr, Query.JCR_SQL2);
        this.publishedContentLast30Days = (int) publishedContentQuery.execute().getRows().nextRow().getValue("count").getLong();
        
        /* getting unpublished vs published nodes count */
        String publishedNodesQueryStr = countOf(JMIX_EDITORIAL_CONTENT) +
                descendantOfSite() + " " +
                "AND [j:published] = true";
        QueryWrapper publishedNodesQuery = session.getWorkspace().getQueryManager().createQuery(publishedNodesQueryStr, Query.JCR_SQL2);
        this.publishedNodes = (int) publishedNodesQuery.execute().getRows().nextRow().getValue("count").getLong();
        
        // Unpublished = total editorial content - published
        this.unpublishedNodes = this.editorialContentsNumber - this.publishedNodes;
        
        /* Calculate average time from creation to publication */
        try {
            String avgTimeQueryStr = "SELECT item.[jcr:created] AS created, item.[j:lastPublished] AS published " +
                    "FROM [jmix:lastPublished] AS item " +
                    descendantOfSite() + " " +
                    "AND item.[j:lastPublished] is not null " +
                    "AND item.[jcr:created] is not null";
            QueryWrapper avgTimeQuery = session.getWorkspace().getQueryManager().createQuery(avgTimeQueryStr, Query.JCR_SQL2);
            javax.jcr.query.RowIterator rows = avgTimeQuery.execute().getRows();
            
            long totalDiff = 0;
            int count = 0;
            while (rows.hasNext()) {
                javax.jcr.query.Row row = rows.nextRow();
                try {
                    Calendar created = row.getValue("created").getDate();
                    Calendar published = row.getValue("published").getDate();
                    long diffInMillis = published.getTimeInMillis() - created.getTimeInMillis();
                    long diffInDays = diffInMillis / (1000 * 60 * 60 * 24);
                    if (diffInDays >= 0) { // Only positive differences
                        totalDiff += diffInDays;
                        count++;
                    }
                } catch (Exception e) {
                    // Skip rows with invalid dates
                }
            }
            
            if (count > 0) {
                this.averageTimeToPublish = (double) totalDiff / count;
            }
        } catch (Exception e) {
            logger.warn("Error calculating average time to publish", e);
            this.averageTimeToPublish = 0.0;
        }
        
        /* Getting top contributors (authors/editors) */
        try {
            // Query all editorial content and aggregate by author manually
            String topContributorsQueryStr = "SELECT item.[jcr:createdBy] AS author " +
                    "FROM [jmix:editorialContent] AS item " +
                    descendantOfSite();
            QueryWrapper topContributorsQuery = session.getWorkspace().getQueryManager().createQuery(topContributorsQueryStr, Query.JCR_SQL2);
            javax.jcr.query.RowIterator rows = topContributorsQuery.execute().getRows();
            
            // Collect contributors with their counts manually
            Map<String, Integer> contributorMap = new HashMap<>();
            while (rows.hasNext()) {
                javax.jcr.query.Row row = rows.nextRow();
                try {
                    String author = row.getValue("author").getString();
                    if (author != null && !author.isEmpty()) {
                        contributorMap.put(author, contributorMap.getOrDefault(author, 0) + 1);
                    }
                } catch (Exception e) {
                    // Skip invalid rows
                    logger.debug("Skipping row without valid author", e);
                }
            }
            
            // Sort by count (descending) and get top 5
            List<Map.Entry<String, Integer>> sortedContributors = new ArrayList<>(contributorMap.entrySet());
            sortedContributors.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
            
            this.topContributors = new ArrayList<>();
            int topLimit = Math.min(5, sortedContributors.size());
            for (int i = 0; i < topLimit; i++) {
                Map.Entry<String, Integer> entry = sortedContributors.get(i);
                Map<String, Object> contributor = new HashMap<>();
                contributor.put("username", entry.getKey());
                contributor.put("contentCount", entry.getValue());
                this.topContributors.add(contributor);
            }
            
            logger.info("Found {} top contributors from {} total authors", topLimit, contributorMap.size());
        } catch (Exception e) {
            logger.error("Error calculating top contributors", e);
            this.topContributors = new ArrayList<>();
        }

        /* Getting the detailed list of content touched in the last 30 days */
        collectRecentActivity(session, thirtyDaysAgo);
    }

    /**
     * @param nodeType the node type to count
     * @return the {@code SELECT} prefix of a JCR-SQL2 count query over that type
     */
    private static String countOf(String nodeType) {
        return "SELECT [rep:count(item,skipChecks=1)] FROM [" + nodeType + "] AS item ";
    }

    /**
     * @return the {@code WHERE} clause restricting a query to descendants of this report's site
     */
    private String descendantOfSite() {
        return "WHERE ISDESCENDANTNODE(item,['" + siteNode.getPath() + "'])";
    }

    /**
     * @param isoDay a {@code yyyy-MM-dd} day
     * @return a JCR-SQL2 date literal for midnight UTC on that day
     */
    private static String dateLiteral(String isoDay) {
        return "CAST('" + isoDay + "T00:00:00.000Z' AS DATE)";
    }

    /**
     * Builds the detailed list of editorial content created, modified or published within the activity window.
     *
     * Two queries are needed because publishing a node updates neither {@code jcr:created} nor
     * {@code jcr:lastModified}: a node published in the window may fall outside the created/modified query.
     * Results are merged by node identifier and sorted by most recent activity first.
     *
     * @param session    the JCR session to query, never held beyond this call
     * @param cutoffDate the start of the activity window, formatted as {@code yyyy-MM-dd}
     */
    private void collectRecentActivity(JCRSessionWrapper session, String cutoffDate) {
        try {
            String cutoffLiteral = dateLiteral(cutoffDate);
            // Same instant as the literal above, so the per-row flags agree with what the queries selected.
            long cutoffMillis = parseUtcMidnight(cutoffDate);
            Map<String, Map<String, Object>> activityMap = new LinkedHashMap<>();

            String createdOrModifiedQueryStr = "SELECT * FROM [" + JMIX_EDITORIAL_CONTENT + "] AS item " +
                    descendantOfSite() + " " +
                    "AND (item.[jcr:created] >= " + cutoffLiteral + " " +
                    "OR item.[jcr:lastModified] >= " + cutoffLiteral + ")";
            collectActivityItems(session, createdOrModifiedQueryStr, cutoffMillis, activityMap, false);

            String publishedQueryStr = "SELECT * FROM [jmix:lastPublished] AS item " +
                    descendantOfSite() + " " +
                    "AND item.[j:lastPublished] >= " + cutoffLiteral;
            collectActivityItems(session, publishedQueryStr, cutoffMillis, activityMap, true);

            List<Map<String, Object>> activityList = new ArrayList<>(activityMap.values());
            activityList.sort((left, right) -> Long.compare(
                    (Long) right.get(LAST_ACTIVITY_TIMESTAMP),
                    (Long) left.get(LAST_ACTIVITY_TIMESTAMP)));

            this.recentActivityTotal = activityList.size();
            this.recentActivity = activityList.size() > MAX_ACTIVITY_ITEMS ?
                    new ArrayList<>(activityList.subList(0, MAX_ACTIVITY_ITEMS)) :
                    activityList;

            logger.debug("Content activity list: {} items returned out of {} matching nodes",
                    this.recentActivity.size(), this.recentActivityTotal);
        } catch (Exception e) {
            logger.error("Error building the content activity list", e);
            this.recentActivity = new ArrayList<>();
            this.recentActivityTotal = 0;
        }
    }

    /**
     * Runs one activity query and merges its rows into the accumulator, keyed by node identifier.
     *
     * @param session               the JCR session to query
     * @param queryStr              the JCR-SQL2 statement to execute
     * @param cutoffMillis          the start of the activity window, as epoch milliseconds
     * @param activityMap           accumulator of activity items, first writer for an identifier wins
     * @param requireEditorialType  when true, discard nodes that are not {@code jmix:editorialContent},
     *                              so the list matches the activity counters
     * @throws RepositoryException if the query cannot be executed
     */
    private void collectActivityItems(JCRSessionWrapper session, String queryStr, long cutoffMillis,
                                      Map<String, Map<String, Object>> activityMap, boolean requireEditorialType)
            throws RepositoryException {
        QueryWrapper query = session.getWorkspace().getQueryManager().createQuery(queryStr, Query.JCR_SQL2);
        NodeIterator nodes = query.execute().getNodes();
        while (nodes.hasNext()) {
            try {
                JCRNodeWrapper node = (JCRNodeWrapper) nodes.nextNode();
                if (requireEditorialType && !node.isNodeType(JMIX_EDITORIAL_CONTENT)) {
                    continue;
                }

                String identifier = node.getIdentifier();
                if (!activityMap.containsKey(identifier)) {
                    activityMap.put(identifier, buildActivityItem(node, cutoffMillis));
                }
            } catch (RepositoryException e) {
                logger.debug("Skipping a node while building the content activity list", e);
            }
        }
    }

    /**
     * Maps one node to a flat activity row: identity, the three activity dates and their authors.
     *
     * @param node          the node to describe
     * @param cutoffMillis  the start of the activity window, used to flag which events happened in it
     * @return a mutable map of primitive values, safe to serialise once the session is gone
     * @throws RepositoryException if the node identity cannot be read
     */
    private Map<String, Object> buildActivityItem(JCRNodeWrapper node, long cutoffMillis) throws RepositoryException {
        Calendar created = getDateProperty(node, "jcr:created");
        Calendar modified = getDateProperty(node, "jcr:lastModified");
        Calendar published = getDateProperty(node, "j:lastPublished");

        Map<String, Object> item = new HashMap<>();
        item.put("name", node.getDisplayableName());
        item.put("path", node.getPath());
        item.put("type", node.getPrimaryNodeType().getAlias());
        item.put("created", formatIsoDate(created));
        item.put("createdBy", getStringProperty(node, "jcr:createdBy"));
        item.put("lastModified", formatIsoDate(modified));
        item.put("lastModifiedBy", getStringProperty(node, "jcr:lastModifiedBy"));
        item.put("lastPublished", formatIsoDate(published));
        item.put("lastPublishedBy", getStringProperty(node, "j:lastPublishedBy"));
        item.put("isNew", isWithinWindow(created, cutoffMillis));
        item.put("isModified", isWithinWindow(modified, cutoffMillis));
        item.put("isPublished", isWithinWindow(published, cutoffMillis));
        item.put(LAST_ACTIVITY_TIMESTAMP, mostRecentTimestamp(created, modified, published));
        return item;
    }

    /**
     * Reads a date property, returning null when it is absent or unreadable.
     *
     * @param node         the node to read from
     * @param propertyName the property name
     * @return the property value, or null
     */
    private Calendar getDateProperty(JCRNodeWrapper node, String propertyName) {
        try {
            return node.hasProperty(propertyName) ? node.getProperty(propertyName).getDate() : null;
        } catch (RepositoryException e) {
            logger.debug("Cannot read date property {}", propertyName, e);
            return null;
        }
    }

    /**
     * Reads a string property, returning an empty string when it is absent or unreadable.
     *
     * @param node         the node to read from
     * @param propertyName the property name
     * @return the property value, or an empty string
     */
    private String getStringProperty(JCRNodeWrapper node, String propertyName) {
        try {
            return node.hasProperty(propertyName) ? node.getProperty(propertyName).getString() : "";
        } catch (RepositoryException e) {
            logger.debug("Cannot read string property {}", propertyName, e);
            return "";
        }
    }

    /**
     * @param cutoffDate a {@code yyyy-MM-dd} date
     * @return midnight UTC of that date, as epoch milliseconds
     * @throws java.time.format.DateTimeParseException if the date cannot be parsed
     */
    private long parseUtcMidnight(String cutoffDate) {
        return LocalDate.parse(cutoffDate).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    /**
     * Formats a date as ISO 8601 so the front-end can parse it directly.
     *
     * @param calendar the date to format, may be null
     * @return the ISO 8601 representation, or an empty string when the date is null
     */
    private String formatIsoDate(Calendar calendar) {
        if (calendar == null) {
            return "";
        }

        Instant instant = calendar.toInstant();
        return ISO_DATE_FORMAT.format(instant.atZone(calendar.getTimeZone().toZoneId()));
    }

    /**
     * @param calendar     the date to test, may be null
     * @param cutoffMillis the start of the activity window, as epoch milliseconds
     * @return true when the date falls inside the activity window
     */
    private boolean isWithinWindow(Calendar calendar, long cutoffMillis) {
        return calendar != null && calendar.getTimeInMillis() >= cutoffMillis;
    }

    /**
     * @param calendars the dates to compare, null entries ignored
     * @return the most recent timestamp among the given dates, or 0 when they are all null
     */
    private long mostRecentTimestamp(Calendar... calendars) {
        long mostRecent = 0L;
        for (Calendar calendar : calendars) {
            if (calendar != null && calendar.getTimeInMillis() > mostRecent) {
                mostRecent = calendar.getTimeInMillis();
            }
        }

        return mostRecent;
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
        
        // Content Activity metrics
        jsonObject.put("newContentLast30Days", newContentLast30Days);
        jsonObject.put("modifiedContentLast30Days", modifiedContentLast30Days);
        jsonObject.put("publishedContentLast30Days", publishedContentLast30Days);
        jsonObject.put("unpublishedNodes", unpublishedNodes);
        jsonObject.put("publishedNodes", publishedNodes);
        jsonObject.put("averageTimeToPublish", averageTimeToPublish);
        jsonObject.put("topContributors", topContributors);
        jsonObject.put("recentActivity", recentActivity);
        jsonObject.put("recentActivityTotal", recentActivityTotal);
        
        return jsonObject;
    }

}
