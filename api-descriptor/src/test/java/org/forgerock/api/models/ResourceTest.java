/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.api.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.api.jackson.TranslationModule;
import org.forgerock.api.jackson.TranslationSerializer;
import org.forgerock.api.util.Translator;
import org.forgerock.services.context.SecurityContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import org.forgerock.api.ApiValidationException;
import org.forgerock.api.annotations.Actions;
import org.forgerock.api.annotations.Queries;
import org.forgerock.api.annotations.RequestHandler;
import org.forgerock.api.enums.CountPolicy;
import org.forgerock.api.enums.CreateMode;
import org.forgerock.api.enums.PagingMode;
import org.forgerock.api.enums.PatchOperation;
import org.forgerock.api.enums.QueryType;
import org.forgerock.api.enums.Stability;
import java.util.Locale;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

public class ResourceTest {

    private final String description = "My Description";
    public static final String TRANSLATED_DISCRIPTION_TWICE_REGEX =
            ".*(If you see this it has been translated).*(If you see this it has been translated).*";
    private final String i18nDescription = Translator.TRANSLATION_KEY_PREFIX + "api-dictionary#description_test";
    private Schema schema;
    private Create create;
    private Read read;
    private Update update;
    private Delete delete;
    private Patch patch;
    private Action action1;
    private Action action2;
    private Query query1;
    private Query query2;

    @BeforeClass
    public void beforeClass() {
        schema = Schema.schema()
                .schema(json(object()))
                .build();
        create = Create.create()
                .mode(CreateMode.ID_FROM_SERVER)
                .mvccSupported(true)
                .build();
        read = Read.read()
                .build();
        update = Update.update()
                .mvccSupported(true)
                .build();
        delete = Delete.delete()
                .mvccSupported(true)
                .build();
        patch = Patch.patch()
                .mvccSupported(true)
                .operations(PatchOperation.ADD, PatchOperation.COPY)
                .build();
        action1 = Action.action()
                .name("action1")
                .response(schema)
                .build();
        action2 = Action.action()
                .name("action2")
                .response(schema)
                .build();
        query1 = Query.query()
                .type(QueryType.ID)
                .queryId("q1")
                .build();
        query2 = Query.query()
                .type(QueryType.ID)
                .queryId("q2")
                .build();
    }

    /**
     * Test the {@Resource} builder with builder-methods that do <em>not</em> take arrays as arguments.
     */
    @Test
    public void testBuilderWithNonArrayMethods() {
        final Resource resource = Resource.resource()
                .description(description)
                .resourceSchema(schema)
                .create(create)
                .read(read)
                .update(update)
                .delete(delete)
                .patch(patch)
                .action(action1)
                .action(action2)
                .query(query1)
                .query(query2)
                .build();

        assertTestBuilder(resource);
    }

    /**
     * Test the {@Resource} builder with builder-methods that take arrays as arguments.
     */
    @Test
    public void testBuilderWithArrayMethods() {
        final Resource resource = Resource.resource()
                .description(description)
                .resourceSchema(schema)
                .create(create)
                .read(read)
                .update(update)
                .delete(delete)
                .patch(patch)
                .actions(asList(action1, action2))
                .queries(asList(query1, query2))
                .build();

        assertTestBuilder(resource);
    }

    @Test
    public void testBuilderWithOperationsArray() {
        final Resource resource = Resource.resource()
                .description(description)
                .resourceSchema(schema)
                .operations(create, read, update, delete, patch, action1, action2, query1, query2)
                .build();

        assertTestBuilder(resource);
    }

    private void assertTestBuilder(final Resource resource) {
        assertThat(resource.getDescription()).isEqualTo(description);
        assertThat(resource.getResourceSchema()).isEqualTo(schema);
        assertThat(resource.getCreate()).isEqualTo(create);
        assertThat(resource.getRead()).isEqualTo(read);
        assertThat(resource.getUpdate()).isEqualTo(update);
        assertThat(resource.getDelete()).isEqualTo(delete);
        assertThat(resource.getPatch()).isEqualTo(patch);
        assertThat(resource.getActions()).contains(action1, action2);
        assertThat(resource.getQueries()).contains(query1, query2);
    }

    @Test(expectedExceptions = ApiValidationException.class)
    public void testEmptyResource() {
        Resource.resource().build();
    }

    @Test
    public void testSimpleAnnotatedHandler() throws Exception {
        ApiDescription<?> descriptor = ApiDescription.apiDescription().id("frapi:test").build();
        final Resource resource = Resource.fromAnnotatedType(SimpleAnnotatedHandler.class, false, descriptor);
        assertThat(resource.getResourceSchema()).isNull();
        assertThat(resource.getActions()).hasSize(1);
        Action action = resource.getActions()[0];
        assertThat(action.getName()).isEqualTo("myAction");
        assertThat(action.getErrors()).isEmpty();
        assertThat(action.getParameters()).isEmpty();
        assertThat(descriptor.getErrors().getErrors()).isEmpty();
        assertThat(descriptor.getDefinitions().getDefinitions()).isEmpty();
    }

    @RequestHandler
    private static final class SimpleAnnotatedHandler {
        @org.forgerock.api.annotations.Action(
                operationDescription = @org.forgerock.api.annotations.Operation)
        public void myAction() {

        }
    }

    @Test
    public void testReferencedSchema() throws Exception {
        ApiDescription<?> descriptor = ApiDescription.apiDescription().id("frapi:test").build();
        final Resource resource = Resource.fromAnnotatedType(ReferencedSchemaHandler.class, false, descriptor);
        assertThat(resource.getRead()).isNotNull();
        assertThat(resource.getResourceSchema()).isNotNull();
        assertThat(resource.getResourceSchema().getReference().getValue()).isEqualTo("#/definitions/frapi:response");
        assertThat(descriptor.getDefinitions().getDefinitions()).hasSize(1).containsKeys("frapi:response");
    }

    @RequestHandler(resourceSchema = @org.forgerock.api.annotations.Schema(fromType = IdentifiedResponse.class))
    private static final class ReferencedSchemaHandler {
        @org.forgerock.api.annotations.Read(
                operationDescription = @org.forgerock.api.annotations.Operation(
                        description = "A read resource operation."
                ))
        public void read() {

        }
    }

    @Test
    public void testReferencedError() throws Exception {
        ApiDescription<?> descriptor = ApiDescription.apiDescription().id("frapi:test").build();
        final Resource resource = Resource.fromAnnotatedType(ReferencedErrorHandler.class, false, descriptor);
        assertThat(resource.getRead()).isNotNull();
        assertThat(resource.getRead().getErrors()).hasSize(1);
        assertThat(resource.getRead().getErrors()[0].getReference().getValue()).isEqualTo("#/errors/frapi:myerror");
        assertThat(descriptor.getErrors().getErrors()).hasSize(1).containsKeys("frapi:myerror");
    }

    @RequestHandler(resourceSchema = @org.forgerock.api.annotations.Schema(fromType = IdentifiedResponse.class))
    private static final class ReferencedErrorHandler {
        @org.forgerock.api.annotations.Read(
                operationDescription = @org.forgerock.api.annotations.Operation(
                        description = "A read resource operation.",
                        errors = @org.forgerock.api.annotations.Error(id = "frapi:myerror", code = 500,
                                description = "Our bad.")
                ))
        public void read() {

        }
    }

    @Test
    public void testCreateAnnotatedHandler() throws Exception {
        ApiDescription<?> descriptor = ApiDescription.apiDescription().id("frapi:test").build();
        final Resource resource = Resource.fromAnnotatedType(CreateAnnotatedHandler.class, false, descriptor);
        assertThat(resource.getResourceSchema()).isNotNull();
        assertThat(resource.getCreate()).isNotNull();
        Create create = resource.getCreate();
        assertThat(create.isMvccSupported()).isTrue();
        assertThat(create.getDescription()).isEqualTo("A create resource operation.");
        assertThat(create.getErrors()).hasSize(2);
        assertThat(create.getParameters()).hasSize(1);
        assertThat(create.getSupportedLocales()).hasSize(2);
        assertThat(create.getSupportedContexts()).hasSize(1);
        assertThat(create.getStability()).isEqualTo(Stability.EVOLVING);
        assertThat(create.getMode()).isEqualTo(CreateMode.ID_FROM_SERVER);
    }

    @RequestHandler(resourceSchema = @org.forgerock.api.annotations.Schema(fromType = Response.class))
    private static final class CreateAnnotatedHandler {
        @org.forgerock.api.annotations.Create(
                operationDescription = @org.forgerock.api.annotations.Operation(
                        contexts = SecurityContext.class,
                        description = "A create resource operation.",
                        errors = {
                                @org.forgerock.api.annotations.Error
                                        (code = 403, description = "You're forbidden from creating these resources"),
                                @org.forgerock.api.annotations.Error
                                        (code = 400
                                                , description = "You can't create these resources using too much jam")
                        },
                        parameters = {
                                @org.forgerock.api.annotations.Parameter
                                        (name = "id", type = "string", description = "Identifier for the created")
                        },
                        locales = {"en-GB", "en-US"},
                        stability = Stability.EVOLVING
                ),
                mvccSupported = true)
        public void create() {

        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testResourceSchemaRequiredForCrudpq() throws Exception {
        ApiDescription<?> descriptor = ApiDescription.apiDescription().id("frapi:test").build();
        Resource.fromAnnotatedType(ResourceSchemaRequiredAnnotatedHandler.class, false, descriptor);
    }

    @RequestHandler
    private static final class ResourceSchemaRequiredAnnotatedHandler {
        @org.forgerock.api.annotations.Create(
                operationDescription = @org.forgerock.api.annotations.Operation(
                        description = "A create resource operation."
                ),
                mvccSupported = true)
        public void create() {

        }
    }

    @Test
    public void testReadAnnotatedHandler() throws Exception {
        ApiDescription<?> descriptor = ApiDescription.apiDescription().id("frapi:test").build();
        final Resource resource = Resource.fromAnnotatedType(ReadAnnotatedHandler.class, false, descriptor);
        assertThat(resource.getResourceSchema()).isNotNull();
        assertThat(resource.getRead()).isNotNull();
        Read read = resource.getRead();
        assertThat(read.getDescription()).isEqualTo("A read resource operation.");
        assertThat(read.getErrors()).hasSize(0);
        assertThat(read.getParameters()).hasSize(0);
        assertThat(read.getSupportedLocales()).hasSize(0);
        assertThat(read.getSupportedContexts()).hasSize(0);
        assertThat(read.getStability()).isEqualTo(Stability.STABLE);
    }

    @RequestHandler(resourceSchema = @org.forgerock.api.annotations.Schema(fromType = Response.class))
    private static final class ReadAnnotatedHandler {
        @org.forgerock.api.annotations.Read(
                operationDescription = @org.forgerock.api.annotations.Operation(
                        description = "A read resource operation."
                ))
        public void read() {

        }
    }

    @Test
    public void testUpdateAnnotatedHandler() throws Exception {
        ApiDescription<?> descriptor = ApiDescription.apiDescription().id("frapi:test").build();
        final Resource resource = Resource.fromAnnotatedType(UpdateAnnotatedHandler.class, false, descriptor);
        assertThat(resource.getResourceSchema()).isNotNull();
        assertThat(resource.getUpdate()).isNotNull();
        Update update = resource.getUpdate();
        assertThat(update.isMvccSupported()).isTrue();
        assertThat(update.getDescription()).isEqualTo("An update resource operation.");
        assertThat(update.getErrors()).hasSize(0);
        assertThat(update.getParameters()).hasSize(0);
        assertThat(update.getSupportedLocales()).hasSize(0);
        assertThat(update.getSupportedContexts()).hasSize(0);
        assertThat(update.getStability()).isEqualTo(Stability.STABLE);
    }

    @RequestHandler(resourceSchema = @org.forgerock.api.annotations.Schema(fromType = Response.class))
    private static final class UpdateAnnotatedHandler {
        @org.forgerock.api.annotations.Update(
                operationDescription = @org.forgerock.api.annotations.Operation(
                        description = "An update resource operation."
                ),
                mvccSupported = true)
        public void update() {

        }
    }

    @Test
    public void testDeleteAnnotatedHandler() throws Exception {
        ApiDescription<?> descriptor = ApiDescription.apiDescription().id("frapi:test").build();
        final Resource resource = Resource.fromAnnotatedType(DeleteAnnotatedHandler.class, false, descriptor);
        assertThat(resource.getResourceSchema()).isNotNull();
        assertThat(resource.getDelete()).isNotNull();
        Delete delete = resource.getDelete();
        assertThat(delete.isMvccSupported()).isTrue();
        assertThat(delete.getDescription()).isEqualTo("A delete resource operation.");
        assertThat(delete.getErrors()).hasSize(0);
        assertThat(delete.getParameters()).hasSize(0);
        assertThat(delete.getSupportedLocales()).hasSize(0);
        assertThat(delete.getSupportedContexts()).hasSize(0);
        assertThat(delete.getStability()).isEqualTo(Stability.STABLE);
    }

    @RequestHandler(resourceSchema = @org.forgerock.api.annotations.Schema(fromType = Response.class))
    private static final class DeleteAnnotatedHandler {
        @org.forgerock.api.annotations.Delete(
                operationDescription = @org.forgerock.api.annotations.Operation(
                        description = "A delete resource operation."
                ),
                mvccSupported = true)
        public void delete() {

        }
    }

    @Test
    public void testPatchAnnotatedHandler() throws Exception {
        ApiDescription<?> descriptor = ApiDescription.apiDescription().id("frapi:test").build();
        final Resource resource = Resource.fromAnnotatedType(PatchAnnotatedHandler.class, false, descriptor);
        assertThat(resource.getResourceSchema()).isNotNull();
        assertThat(resource.getPatch()).isNotNull();
        Patch patch = resource.getPatch();
        assertThat(patch.isMvccSupported()).isTrue();
        assertThat(patch.getDescription()).isEqualTo("A patch resource operation.");
        assertThat(patch.getErrors()).hasSize(0);
        assertThat(patch.getParameters()).hasSize(0);
        assertThat(patch.getSupportedLocales()).hasSize(0);
        assertThat(patch.getSupportedContexts()).hasSize(0);
        assertThat(patch.getStability()).isEqualTo(Stability.STABLE);
        assertThat(patch.getOperations()).hasSize(2);
        assertThat(patch.getOperations()).contains(PatchOperation.INCREMENT, PatchOperation.TRANSFORM);
    }

    @RequestHandler(resourceSchema = @org.forgerock.api.annotations.Schema(fromType = Response.class))
    private static final class PatchAnnotatedHandler {
        @org.forgerock.api.annotations.Patch(
                operationDescription = @org.forgerock.api.annotations.Operation(
                        description = "A patch resource operation."
                ),
                mvccSupported = true,
                operations = {PatchOperation.INCREMENT, PatchOperation.TRANSFORM})
        public void patch() {

        }
    }

    @DataProvider
    public Object[][] actionAnnotations() {
        return new Object[][]{{ActionAnnotatedHandler.class}, {ActionsAnnotatedHandler.class}};
    }

    @Test(dataProvider = "actionAnnotations")
    public void testActionAnnotatedHandler(Class<?> type) throws Exception {
        ApiDescription<?> descriptor = ApiDescription.apiDescription().id("frapi:test").build();
        final Resource resource = Resource.fromAnnotatedType(type, false, descriptor);
        assertThat(resource.getResourceSchema()).isNull();
        assertThat(resource.getActions()).isNotNull();
        assertThat(resource.getActions()).hasSize(2);
        Action action1 = resource.getActions()[0];
        assertThat(action1.getDescription()).isEqualTo("An action resource operation.");
        assertThat(action1.getErrors()).hasSize(2);
        assertThat(action1.getParameters()).hasSize(1);
        assertThat(action1.getSupportedLocales()).hasSize(2);
        assertThat(action1.getSupportedContexts()).hasSize(1);
        assertThat(action1.getStability()).isEqualTo(Stability.EVOLVING);
        assertThat(action1.getName()).isEqualTo("action1");
        assertThat(action1.getRequest()).isNotNull();
        assertThat(action1.getResponse()).isNotNull();

        Action action2 = resource.getActions()[1];
        assertThat(action2.getDescription()).isEqualTo("An action resource operation.");
        assertThat(action2.getErrors()).hasSize(2);
        assertThat(action2.getParameters()).hasSize(1);
        assertThat(action2.getSupportedLocales()).hasSize(2);
        assertThat(action2.getSupportedContexts()).hasSize(1);
        assertThat(action2.getStability()).isEqualTo(Stability.EVOLVING);
        assertThat(action2.getName()).isEqualTo("action2");
        assertThat(action2.getRequest()).isNotNull();
        assertThat(action2.getResponse()).isNotNull();
    }

    @RequestHandler
    private static final class ActionAnnotatedHandler {
        @org.forgerock.api.annotations.Action(
                operationDescription = @org.forgerock.api.annotations.Operation(
                        contexts = SecurityContext.class,
                        description = "An action resource operation.",
                        errors = {
                                @org.forgerock.api.annotations.Error
                                        (code = 403, description = "Action forbidden"),
                                @org.forgerock.api.annotations.Error
                                        (code = 400, description = "Malformed action request")
                        },
                        parameters = {
                                @org.forgerock.api.annotations.Parameter
                                        (name = "id", type = "string", description = "Identifier for the action")
                        },
                        locales = {"en-GB", "en-US"},
                        stability = Stability.EVOLVING
                ),
                name = "action1",
                request = @org.forgerock.api.annotations.Schema(fromType = Request.class),
                response = @org.forgerock.api.annotations.Schema(fromType = Response.class))
        public void action1() {

        }

        @org.forgerock.api.annotations.Action(
                operationDescription = @org.forgerock.api.annotations.Operation(
                        contexts = SecurityContext.class,
                        description = "An action resource operation.",
                        errors = {
                                @org.forgerock.api.annotations.Error
                                        (code = 403, description = "Action forbidden"),
                                @org.forgerock.api.annotations.Error
                                        (code = 400, description = "Malformed action request")
                        },
                        parameters = {
                                @org.forgerock.api.annotations.Parameter
                                        (name = "id", type = "string", description = "Identifier for the action")
                        },
                        locales = {"en-GB", "en-US"},
                        stability = Stability.EVOLVING
                ),
                name = "action2",
                request = @org.forgerock.api.annotations.Schema(fromType = Request.class),
                response = @org.forgerock.api.annotations.Schema(fromType = Response.class))
        public void action2() {

        }
    }

    @RequestHandler
    private static final class ActionsAnnotatedHandler {
        @Actions({
                @org.forgerock.api.annotations.Action(
                        operationDescription = @org.forgerock.api.annotations.Operation(
                                contexts = SecurityContext.class,
                                description = "An action resource operation.",
                                errors = {
                                        @org.forgerock.api.annotations.Error
                                                (code = 403, description = "Action forbidden"),
                                        @org.forgerock.api.annotations.Error
                                                (code = 400, description = "Malformed action request")
                                },
                                parameters = {
                                        @org.forgerock.api.annotations.Parameter
                                                (name = "id", type = "string",
                                                        description = "Identifier for the action")
                                },
                                locales = {"en-GB", "en-US"},
                                stability = Stability.EVOLVING
                        ),
                        name = "action1",
                        request = @org.forgerock.api.annotations.Schema(fromType = Request.class),
                        response = @org.forgerock.api.annotations.Schema(fromType = Response.class)),
                @org.forgerock.api.annotations.Action(
                        operationDescription = @org.forgerock.api.annotations.Operation(
                                contexts = SecurityContext.class,
                                description = "An action resource operation.",
                                errors = {
                                        @org.forgerock.api.annotations.Error
                                                (code = 403, description = "Action forbidden"),
                                        @org.forgerock.api.annotations.Error
                                                (code = 400, description = "Malformed action request")
                                },
                                parameters = {
                                        @org.forgerock.api.annotations.Parameter
                                                (name = "id", type = "string",
                                                        description = "Identifier for the action")
                                },
                                locales = {"en-GB", "en-US"},
                                stability = Stability.EVOLVING
                        ),
                        name = "action2",
                        request = @org.forgerock.api.annotations.Schema(fromType = Request.class),
                        response = @org.forgerock.api.annotations.Schema(fromType = Response.class))})
        public void actions() {

        }
    }

    @DataProvider
    public Object[][] queryAnnotations() {
        return new Object[][]{{QueryAnnotatedHandler.class}, {QueriesAnnotatedHandler.class}};
    }

    @Test(dataProvider = "queryAnnotations")
    public void testQueryAnnotatedHandler(Class<?> type) throws Exception {
        ApiDescription<?> descriptor = ApiDescription.apiDescription().id("frapi:test").build();
        final Resource resource = Resource.fromAnnotatedType(type, false, descriptor);
        assertThat(resource.getQueries()).isNotNull();
        assertThat(resource.getQueries()).hasSize(2);
        Query query1 = resource.getQueries()[0];
        assertThat(query1.getDescription()).isEqualTo("A query resource operation.");
        assertThat(query1.getErrors()).hasSize(2);
        assertThat(query1.getParameters()).hasSize(1);
        assertThat(query1.getSupportedLocales()).hasSize(2);
        assertThat(query1.getSupportedContexts()).hasSize(1);
        assertThat(query1.getStability()).isEqualTo(Stability.EVOLVING);
        assertThat(query1.getType()).isEqualTo(QueryType.ID);
        assertThat(query1.getQueryId()).isEqualTo("query1");
        assertThat(query1.getCountPolicy()[0]).isEqualTo(CountPolicy.ESTIMATE);
        assertThat(query1.getPagingMode()[0]).isEqualTo(PagingMode.COOKIE);
        assertThat(query1.getPagingMode()[1]).isEqualTo(PagingMode.OFFSET);
        assertThat(query1.getQueryableFields()[0]).isEqualTo("field1");
        assertThat(query1.getQueryableFields()[1]).isEqualTo("field2");
        assertThat(query1.getSupportedSortKeys()[0]).isEqualTo("key1");
        assertThat(query1.getSupportedSortKeys()[1]).isEqualTo("key2");
        assertThat(query1.getSupportedSortKeys()[2]).isEqualTo("key3");

        Query query2 = resource.getQueries()[1];
        assertThat(query2.getDescription()).isEqualTo("A query resource operation.");
        assertThat(query2.getErrors()).hasSize(2);
        assertThat(query2.getParameters()).hasSize(1);
        assertThat(query2.getSupportedLocales()).hasSize(2);
        assertThat(query2.getSupportedContexts()).hasSize(1);
        assertThat(query2.getStability()).isEqualTo(Stability.EVOLVING);
        assertThat(query2.getType()).isEqualTo(QueryType.ID);
        assertThat(query2.getQueryId()).isEqualTo("query2");
        assertThat(query2.getCountPolicy()[0]).isEqualTo(CountPolicy.ESTIMATE);
        assertThat(query2.getPagingMode()[0]).isEqualTo(PagingMode.COOKIE);
        assertThat(query2.getPagingMode()[1]).isEqualTo(PagingMode.OFFSET);
        assertThat(query2.getQueryableFields()[0]).isEqualTo("field1");
        assertThat(query2.getQueryableFields()[1]).isEqualTo("field2");
        assertThat(query2.getSupportedSortKeys()[0]).isEqualTo("key1");
        assertThat(query2.getSupportedSortKeys()[1]).isEqualTo("key2");
        assertThat(query2.getSupportedSortKeys()[2]).isEqualTo("key3");
    }

    @RequestHandler(resourceSchema = @org.forgerock.api.annotations.Schema(fromType = Response.class))
    private static final class QueryAnnotatedHandler {
        @org.forgerock.api.annotations.Query(
                operationDescription = @org.forgerock.api.annotations.Operation(
                        contexts = SecurityContext.class,
                        description = "A query resource operation.",
                        errors = {
                                @org.forgerock.api.annotations.Error
                                        (code = 403, description = "Query forbidden"),
                                @org.forgerock.api.annotations.Error
                                        (code = 400, description = "Malformed query request")
                        },
                        parameters = {
                                @org.forgerock.api.annotations.Parameter
                                        (name = "id", type = "string", description = "Identifier for the queried")
                        },
                        locales = {"en-GB", "en-US"},
                        stability = Stability.EVOLVING
                ),
                type = QueryType.ID,
                id = "query1",
                countPolicies = {CountPolicy.ESTIMATE},
                pagingModes = {PagingMode.COOKIE, PagingMode.OFFSET},
                queryableFields = {"field1", "field2"},
                sortKeys = {"key1", "key2", "key3"})
        public void query1() {

        }
        @org.forgerock.api.annotations.Query(
                operationDescription = @org.forgerock.api.annotations.Operation(
                        contexts = SecurityContext.class,
                        description = "A query resource operation.",
                        errors = {
                                @org.forgerock.api.annotations.Error
                                        (code = 403, description = "Query forbidden"),
                                @org.forgerock.api.annotations.Error
                                        (code = 400, description = "Malformed query request")
                        },
                        parameters = {
                                @org.forgerock.api.annotations.Parameter
                                        (name = "id", type = "string", description = "Identifier for the queried")
                        },
                        locales = {"en-GB", "en-US"},
                        stability = Stability.EVOLVING
                ),
                type = QueryType.ID,
                countPolicies = {CountPolicy.ESTIMATE},
                pagingModes = {PagingMode.COOKIE, PagingMode.OFFSET},
                queryableFields = {"field1", "field2"},
                sortKeys = {"key1", "key2", "key3"})
        public void query2() {

        }
    }

    @RequestHandler(resourceSchema = @org.forgerock.api.annotations.Schema(fromType = Response.class))
    private static final class QueriesAnnotatedHandler {
        @Queries({
                @org.forgerock.api.annotations.Query(
                        operationDescription = @org.forgerock.api.annotations.Operation(
                                contexts = SecurityContext.class,
                                description = "A query resource operation.",
                                errors = {
                                        @org.forgerock.api.annotations.Error
                                                (code = 403, description = "Query forbidden"),
                                        @org.forgerock.api.annotations.Error
                                                (code = 400, description = "Malformed query request")
                                },
                                parameters = {
                                        @org.forgerock.api.annotations.Parameter
                                                (name = "id", type = "string",
                                                        description = "Identifier for the queried")
                                },
                                locales = {"en-GB", "en-US"},
                                stability = Stability.EVOLVING
                        ),
                        type = QueryType.ID,
                        id = "query1",
                        countPolicies = {CountPolicy.ESTIMATE},
                        pagingModes = {PagingMode.COOKIE, PagingMode.OFFSET},
                        queryableFields = {"field1", "field2"},
                        sortKeys = {"key1", "key2", "key3"}),
                @org.forgerock.api.annotations.Query(
                        operationDescription = @org.forgerock.api.annotations.Operation(
                                contexts = SecurityContext.class,
                                description = "A query resource operation.",
                                errors = {
                                        @org.forgerock.api.annotations.Error
                                                (code = 403, description = "Query forbidden"),
                                        @org.forgerock.api.annotations.Error
                                                (code = 400, description = "Malformed query request")
                                },
                                parameters = {
                                        @org.forgerock.api.annotations.Parameter
                                                (name = "id", type = "string",
                                                        description = "Identifier for the queried")
                                },
                                locales = {"en-GB", "en-US"},
                                stability = Stability.EVOLVING
                        ),
                        type = QueryType.ID,
                        id = "query2",
                        countPolicies = {CountPolicy.ESTIMATE},
                        pagingModes = {PagingMode.COOKIE, PagingMode.OFFSET},
                        queryableFields = {"field1", "field2"},
                        sortKeys = {"key1", "key2", "key3"})})
        public void queryies() {

        }
    }

    @Test
    public void testResourceCustomSerializer() throws JsonProcessingException {

        final Read readLocal = Read.read()
                .description(i18nDescription)
                .error(Error.error().code(12).description(i18nDescription).build())
                .build();

        final Resource resource = Resource.resource()
                .description(description)
                .resourceSchema(schema)
                .operations(create, readLocal, update, delete, patch, action1, action2, query1, query2)
                .build();

        ObjectMapper mapper = new ObjectMapper();

        Translator translator = new Translator();
        translator.setLocale(Locale.ENGLISH);

        TranslationModule module = new TranslationModule();
        module.addSerializer(new TranslationSerializer(translator));

        mapper.registerModule(module);

        String serialized = mapper.writeValueAsString(resource);

        assertThat(serialized.matches(TRANSLATED_DISCRIPTION_TWICE_REGEX));

    }

    private static final class Request {
        public String id;
        public Integer field;
    }

    private static final class Response {
        public String id;
        public Integer field;
    }

    @org.forgerock.api.annotations.Schema(id = "frapi:response")
    private static final class IdentifiedResponse {
        public String id;
        public Integer field;
    }

}
