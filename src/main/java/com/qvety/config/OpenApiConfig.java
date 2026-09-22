package com.qvety.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import java.util.Locale;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The three reference controllers inherit their six methods from one generic base, so springdoc sees the
 * same Java method name three times and falls back to list, list_1, list_2 — names that depend on the
 * order Spring happened to scan the controllers in, and that mean nothing in the generated Angular client.
 *
 * This names them after what they do and what they return: listRooms, createRoom, activateService. The
 * entity name comes from the operation's own response schema, so a fourth reference entity is still one
 * migration, one entity, one config object, with nothing to add here.
 */
@Configuration
public class OpenApiConfig {

    private static final String REFERENCE_PREFIX = "/api/v1/reference/";

    @Bean
    OpenApiCustomizer referenceOperationIds() {
        return openApi -> openApi.getPaths().forEach((path, item) -> {
            if (!path.startsWith(REFERENCE_PREFIX)) {
                return;
            }
            item.readOperationsMap().forEach((method, operation) -> {
                var entity = responseSchemaName(operation);
                if (entity != null) {
                    operation.setOperationId(verb(method, path) + entity + (isList(method, path) ? "s" : ""));
                }
            });
        });
    }

    private static String verb(PathItem.HttpMethod method, String path) {
        if (path.endsWith("/deactivate")) return "deactivate";
        if (path.endsWith("/activate")) return "activate";
        return switch (method) {
            case POST -> "create";
            case PUT -> "update";
            default -> isList(method, path) ? "list" : "get";
        };
    }

    /** The collection endpoint is the one without a path variable. */
    private static boolean isList(PathItem.HttpMethod method, String path) {
        return method == PathItem.HttpMethod.GET && !path.endsWith("}");
    }

    /**
     * "Room" from the 200 or 201 response, whether it is the object itself or an array of it. A reference
     * list answers with an array; every other operation answers with one row.
     */
    private static String responseSchemaName(Operation operation) {
        for (var code : new String[] {"200", "201"}) {
            var response = operation.getResponses() == null ? null : operation.getResponses().get(code);
            var content = response == null ? null : response.getContent();
            if (content == null) {
                continue;
            }
            for (var media : content.values()) {
                var schema = media.getSchema();
                if (schema == null) {
                    continue;
                }
                var ref = schema.get$ref() != null ? schema.get$ref()
                    : schema.getItems() == null ? null : schema.getItems().get$ref();
                if (ref != null) {
                    var name = ref.substring(ref.lastIndexOf('/') + 1);
                    return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
                }
            }
        }
        return null;
    }
}
