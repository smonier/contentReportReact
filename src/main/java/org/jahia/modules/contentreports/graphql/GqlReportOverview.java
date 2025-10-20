package org.jahia.modules.contentreports.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
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
}
