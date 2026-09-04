package org.jahia.modules.contentreports.service;

import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.exceptions.JahiaException;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.templates.JahiaTemplateManagerService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lists the modules deployed on a site and how each one got there.
 *
 * Every module is classified once: the site's template set, a module the site installs directly
 * (Jahia's {@code j:installedModules}), or a dependency pulled in transitively by one of those.
 *
 * Stateless and thread-safe; reads the shared template manager service.
 */
public final class SiteModuleInventory {

    /** How a module ended up deployed on the site. Ordinal order is the display order. */
    public enum Dependency {
        TEMPLATE_SET("templateSet"),
        INSTALLED("installed"),
        DEPENDENCY("dependency");

        private final String key;

        Dependency(String key) {
            this.key = key;
        }

        /** @return a stable, lower-camel key suitable for the front-end */
        public String key() {
            return key;
        }
    }

    /** One deployed module and its classification. */
    public static final class Entry {
        private final JahiaTemplatesPackage module;
        private final Dependency dependency;

        Entry(JahiaTemplatesPackage module, Dependency dependency) {
            this.module = module;
            this.dependency = dependency;
        }

        public JahiaTemplatesPackage getModule() {
            return module;
        }

        public Dependency getDependency() {
            return dependency;
        }
    }

    private SiteModuleInventory() {
    }

    /**
     * @param site the site to inventory
     * @return the deployed modules, template set first, then installed modules and dependencies,
     *         each group sorted by module id
     * @throws JahiaException if the template manager cannot resolve the site's modules
     */
    public static List<Entry> forSite(JCRSiteNode site) throws JahiaException {
        JahiaTemplateManagerService templateService = ServicesRegistry.getInstance().getJahiaTemplateManagerService();
        List<JahiaTemplatesPackage> deployed = templateService.getInstalledModulesForSite(site.getSiteKey(), true, true, true);

        String templateSetId = site.getTemplatePackage() != null ? site.getTemplatePackage().getId() : site.getTemplatePackageName();
        Set<String> installed = new HashSet<>(site.getInstalledModules());

        // Keyed by id so a module reachable through several dependency paths is listed once.
        Map<String, Entry> byId = new LinkedHashMap<>();
        for (JahiaTemplatesPackage module : deployed) {
            byId.putIfAbsent(module.getId(), new Entry(module, classify(module.getId(), templateSetId, installed)));
        }

        List<Entry> entries = new ArrayList<>(byId.values());
        entries.sort(Comparator
                .comparing((Entry e) -> e.getDependency().ordinal())
                .thenComparing(e -> e.getModule().getId(), String.CASE_INSENSITIVE_ORDER));
        return entries;
    }

    private static Dependency classify(String moduleId, String templateSetId, Set<String> installed) {
        if (moduleId.equals(templateSetId)) {
            return Dependency.TEMPLATE_SET;
        }

        return installed.contains(moduleId) ? Dependency.INSTALLED : Dependency.DEPENDENCY;
    }
}
