package org.jahia.modules.contentreports.service;

import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Resolves the component node types a module declares.
 *
 * A component is a concrete node type extending {@code jnt:content}: something an editor can place
 * in a page or a content folder. Mixins, abstract types, templates and page types are excluded.
 *
 * Stateless and thread-safe; reads the shared {@link NodeTypeRegistry}.
 */
public final class ModuleComponentTypes {

    private static final Logger logger = LoggerFactory.getLogger(ModuleComponentTypes.class);
    private static final String JNT_CONTENT = "jnt:content";

    private ModuleComponentTypes() {
    }

    /**
     * @param moduleId the module id, which is also the registry's system id for the types it declares
     * @return the module's component types sorted by name, empty when it declares none
     */
    public static List<ExtendedNodeType> componentTypesOf(String moduleId) {
        List<ExtendedNodeType> types = new ArrayList<>();
        try {
            for (ExtendedNodeType type : NodeTypeRegistry.getInstance().getNodeTypes(moduleId)) {
                if (isComponent(type)) {
                    types.add(type);
                }
            }
        } catch (RuntimeException e) {
            // A module with no CND has nothing registered under its id; report it as declaring nothing.
            logger.debug("No node types resolved for module {}", moduleId, e);
        }

        types.sort(Comparator.comparing(ExtendedNodeType::getName));
        return types;
    }

    /**
     * @param type a registered node type
     * @return true when the type is something an editor can instantiate as content
     */
    private static boolean isComponent(ExtendedNodeType type) {
        return !type.isMixin() && !type.isAbstract() && type.isNodeType(JNT_CONTENT);
    }
}
