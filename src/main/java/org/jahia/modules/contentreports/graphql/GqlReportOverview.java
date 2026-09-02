package org.jahia.modules.contentreports.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

@GraphQLDescription("Overview metrics for the site")
public class GqlReportOverview {

    private final JSONObject payload;

    public GqlReportOverview(JSONObject payload) {
        this.payload = payload;
    }

    @GraphQLField
    @GraphQLDescription("Technical site name")
    public String getSiteName() {
        return payload.optString("siteName");
    }

    @GraphQLField
    @GraphQLDescription("Display name of the site")
    public String getSiteDisplayableName() {
        return payload.optString("siteDisplayableName");
    }

    @GraphQLField
    @GraphQLDescription("Number of pages under the site")
    public int getNbPages() {
        return payload.optInt("nbPages");
    }

    @GraphQLField
    @GraphQLDescription("Number of templates installed for the site")
    public int getNbTemplates() {
        return payload.optInt("nbTemplates");
    }

    @GraphQLField
    @GraphQLDescription("Number of users registered for the site")
    public int getNbUsers() {
        return payload.optInt("nbUsers");
    }

    @GraphQLField
    @GraphQLDescription("Number of jnt:content nodes under the site")
    public int getNbContents() {
        return payload.optInt("nbContents");
    }

    @GraphQLField
    @GraphQLDescription("Number of jmix:editorialContent nodes under the site")
    public int getNbEditorialContents() {
        return payload.optInt("nbEditorialContents");
    }

    @GraphQLField
    @GraphQLDescription("Number of pending workflow tasks under the site")
    public int getNbWorkflowTasks() {
        return payload.optInt("nbWorkflowTasks");
    }

    @GraphQLField
    @GraphQLDescription("Number of files (jnt:file) under the site")
    public int getNbFiles() {
        return payload.optInt("nbFiles");
    }

    @GraphQLField
    @GraphQLDescription("Number of images (jmix:image) under the site")
    public int getNbImages() {
        return payload.optInt("nbImages");
    }

    @GraphQLField
    @GraphQLDescription("List of language codes available for the site")
    public List<String> getLanguages() {
        JSONArray languagesArray = payload.optJSONArray("languages");
        List<String> languages = new ArrayList<>();
        if (languagesArray != null) {
            for (int i = 0; i < languagesArray.length(); i++) {
                languages.add(languagesArray.optString(i));
            }
        }
        return languages;
    }

    @GraphQLField
    @GraphQLDescription("Number of languages available for the site")
    public int getNbLanguages() {
        return payload.optInt("nbLanguages");
    }

    // Content Activity metrics
    
    @GraphQLField
    @GraphQLDescription("Number of new content items created in the last 30 days")
    public int getNewContentLast30Days() {
        return payload.optInt("newContentLast30Days");
    }

    @GraphQLField
    @GraphQLDescription("Number of content items modified in the last 30 days")
    public int getModifiedContentLast30Days() {
        return payload.optInt("modifiedContentLast30Days");
    }

    @GraphQLField
    @GraphQLDescription("Number of content items published in the last 30 days")
    public int getPublishedContentLast30Days() {
        return payload.optInt("publishedContentLast30Days");
    }

    @GraphQLField
    @GraphQLDescription("Number of unpublished editorial content nodes")
    public int getUnpublishedNodes() {
        return payload.optInt("unpublishedNodes");
    }

    @GraphQLField
    @GraphQLDescription("Number of published editorial content nodes")
    public int getPublishedNodes() {
        return payload.optInt("publishedNodes");
    }

    @GraphQLField
    @GraphQLDescription("Average time in days from content creation to publication")
    public double getAverageTimeToPublish() {
        return payload.optDouble("averageTimeToPublish", 0.0);
    }

    @GraphQLField
    @GraphQLDescription("Top 5 contributors by content count")
    public List<GqlContributor> getTopContributors() {
        JSONArray contributorsArray = payload.optJSONArray("topContributors");
        List<GqlContributor> contributors = new ArrayList<>();
        if (contributorsArray != null) {
            for (int i = 0; i < contributorsArray.length(); i++) {
                JSONObject contributorObj = contributorsArray.optJSONObject(i);
                if (contributorObj != null) {
                    contributors.add(new GqlContributor(
                        contributorObj.optString("username"),
                        contributorObj.optInt("contentCount")
                    ));
                }
            }
        }
        return contributors;
    }

    @GraphQLField
    @GraphQLDescription("Total number of content items created, modified or published in the last 30 days")
    public int getRecentActivityTotal() {
        return payload.optInt("recentActivityTotal");
    }

    @GraphQLField
    @GraphQLDescription("Content items created, modified or published in the last 30 days, most recent first (capped at 500)")
    public List<GqlActivityItem> getRecentActivity() {
        JSONArray activityArray = payload.optJSONArray("recentActivity");
        List<GqlActivityItem> activity = new ArrayList<>();
        if (activityArray != null) {
            for (int i = 0; i < activityArray.length(); i++) {
                JSONObject activityObj = activityArray.optJSONObject(i);
                if (activityObj != null) {
                    activity.add(new GqlActivityItem(activityObj));
                }
            }
        }
        return activity;
    }

    // Inner class for one row of the content activity list
    @GraphQLDescription("A content item touched within the activity window")
    public static class GqlActivityItem {

        private final JSONObject item;

        public GqlActivityItem(JSONObject item) {
            this.item = item;
        }

        private String optNullableString(String key) {
            String value = item.optString(key);
            return value == null || value.isEmpty() ? null : value;
        }

        @GraphQLField
        @GraphQLDescription("Displayable name of the content item")
        public String getName() {
            return item.optString("name");
        }

        @GraphQLField
        @GraphQLDescription("JCR path of the content item")
        public String getPath() {
            return item.optString("path");
        }

        @GraphQLField
        @GraphQLDescription("Primary node type alias of the content item")
        public String getType() {
            return item.optString("type");
        }

        @GraphQLField
        @GraphQLDescription("Creation date, ISO 8601")
        public String getCreated() {
            return optNullableString("created");
        }

        @GraphQLField
        @GraphQLDescription("Username of the author who created the content item")
        public String getCreatedBy() {
            return optNullableString("createdBy");
        }

        @GraphQLField
        @GraphQLDescription("Last modification date, ISO 8601")
        public String getLastModified() {
            return optNullableString("lastModified");
        }

        @GraphQLField
        @GraphQLDescription("Username of the author who last modified the content item")
        public String getLastModifiedBy() {
            return optNullableString("lastModifiedBy");
        }

        @GraphQLField
        @GraphQLDescription("Last publication date, ISO 8601, null when never published")
        public String getLastPublished() {
            return optNullableString("lastPublished");
        }

        @GraphQLField
        @GraphQLDescription("Username of the author who last published the content item")
        public String getLastPublishedBy() {
            return optNullableString("lastPublishedBy");
        }

        @GraphQLField
        @GraphQLName("isNew")
        @GraphQLDescription("True when the content item was created within the activity window")
        public boolean isNew() {
            return item.optBoolean("isNew");
        }

        @GraphQLField
        @GraphQLName("isModified")
        @GraphQLDescription("True when the content item was modified within the activity window")
        public boolean isModified() {
            return item.optBoolean("isModified");
        }

        @GraphQLField
        @GraphQLName("isPublished")
        @GraphQLDescription("True when the content item was published within the activity window")
        public boolean isPublished() {
            return item.optBoolean("isPublished");
        }
    }

    // Inner class for contributor data
    public static class GqlContributor {
        private final String username;
        private final int contentCount;

        public GqlContributor(String username, int contentCount) {
            this.username = username;
            this.contentCount = contentCount;
        }

        @GraphQLField
        @GraphQLDescription("Username of the contributor")
        public String getUsername() {
            return username;
        }

        @GraphQLField
        @GraphQLDescription("Number of content items created by this contributor")
        public int getContentCount() {
            return contentCount;
        }
    }
}
