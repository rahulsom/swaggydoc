package com.github.rahulsom.swaggydoc

import com.wordnik.swagger.annotations.*
import grails.util.Holders
import groovy.transform.CompileStatic
import groovy.transform.Memoized
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.codehaus.groovy.grails.web.mapping.UrlMapping
import org.codehaus.groovy.grails.web.mapping.UrlMappings
import org.codehaus.groovy.grails.web.mime.MimeUtility

import java.lang.annotation.Annotation
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Method
import java.util.function.BiConsumer
import java.util.function.Function

class SwaggyDataService {

    private static final String SwaggerVersion = '1.2'
    private static final List<String> DefaultResponseContentTypes = [
            'application/json', 'application/xml', 'text/html'
    ]
    private static final List<String> DefaultRequestContentTypes = [
            'application/json', 'application/xml', 'application/x-www-form-urlencoded'
    ]
    private static final Function<String, DefaultAction> actionFallback = new Function<String, DefaultAction>() {
        @Override
        DefaultAction apply(String s) {
            new DefaultAction()
        }
    }
    private static final String MARSHALLING_CONFIG_BUILDER =
            'org.grails.plugins.marshallers.config.MarshallingConfigBuilder'

    DefaultGrailsApplication grailsApplication
    LinkGenerator grailsLinkGenerator
    UrlMappings grailsUrlMappingsHolder
    MimeUtility grailsMimeUtility

    @CompileStatic
    private static Parameter makePathParam(String pathParam) {
        new Parameter(pathParam, "$pathParam identifier", 'path', 'string', true)
    }

    @CompileStatic
    private ConfigObject getConfig() { grailsApplication.config['swaggydoc'] as ConfigObject ?: new ConfigObject() }

    @CompileStatic
    private String getApiVersion() { config.apiVersion ?: grailsApplication.metadata['app.version'] }

    /**
     * Generates map of Swagger Resources.
     * @return Map
     */
    @SuppressWarnings("UnnecessaryQualifiedReference")
    Resources resources() {
        def apis = grailsApplication.controllerClasses.
                findAll { SwaggyDataService.getApi(it) }.
                sort(false) { SwaggyDataService.getApi(it).position() }.
                collect { controllerToApi(it) } as ApiDeclaration[]

        new Resources(apiVersion, SwaggerVersion, infoObject, apis)
    }

    /**
     * Generates details about a particular API given a controller name
     * @param controller
     * @return Map
     */
    @SuppressWarnings("UnnecessaryQualifiedReference")
    ControllerDefinition apiDetails(String controller) {
        GrailsControllerClass theController = grailsApplication.controllerClasses.
                find { it.logicalPropertyName == controller && SwaggyDataService.getApi(it) } as GrailsControllerClass

        if (!theController) {
            return null
        }
        Class theControllerClazz = theController.referenceInstance.class

        Api api = getApi(theController)

        final String absoluteBasePath = grailsLinkGenerator.link(uri: '', absolute: true)
        final String basePath = grailsLinkGenerator.link(uri: '')
        String resourcePath = grailsLinkGenerator.link(controller: theController.logicalPropertyName)
        final String domainName = ServiceDefaults.slugToDomain(controller)

        // These preserve the path components supporting hierarchical paths discovered through URL mappings
        def mappingsForController = grailsUrlMappingsHolder.urlMappings.findAll { it.controllerName == controller }
        def resourcePathsPair = mappingsForController.
                collect { mapping -> populatePaths(mapping) }.
                sort(false) { it.left?.size() ?: 0 }.
                reverse().
                find()

        List<String> resourcePathParts = resourcePathsPair?.left
        List<Parameter> resourcePathParams = resourcePathsPair?.right

        Map<String, MethodDocumentation> apis = mappingsForController.
                collectEntries { mapping ->

                    def paths = populatePaths(mapping)
                    List<String> pathParts = paths.left
                    List<Parameter> pathParams = paths.right

                    def actionMethod = ServiceDefaults.DefaultActionComponents.get(mapping.actionName)
                    DefaultAction defaults = (actionMethod ?: actionFallback).apply(domainName)
                    List<Parameter> parameters = getParameters(defaults, pathParams, pathParts)

                    def httpMethod = mapping.httpMethod
                    def md = methodDoc(pathParts, httpMethod, defaults, mapping, parameters, controller, domainName)
                    [mapping.actionName, md]
                }

        if (resourcePathParts?.size()) {
            // UrlMappings may override the resourcePath
            if (resourcePathParts[-1].matches(/^\{.+\}$/)) {
                resourcePathParts.pop()
            }
            resourcePath = '/' + resourcePathParts.join('/')
        }
//        grailsApplication.getArtefactByLogicalPropertyName('Controller', urlMappings[2].controllerName)

        def apiMethods = methodsOfType(ApiOperation, theControllerClazz)

        def allAnnotations = apiMethods*.annotations.flatten()

        List<ApiOperation> apiOperationAnnotations = filter(allAnnotations, ApiOperation)
        List<ApiResponses> apiResponseAnnotations = filter(allAnnotations, ApiResponses)
        List<SwaggyAdditionalClasses> additionalClassesAnnotations = filter(allAnnotations, SwaggyAdditionalClasses)

        def successTypes = apiOperationAnnotations*.response()
        def inferredTypes = grailsApplication.domainClasses.
                find { it.logicalPropertyName == theController.logicalPropertyName }?.
                clazz
        def additionalTypes = additionalClassesAnnotations*.value().flatten()
        def errorTypes = apiResponseAnnotations*.value().flatten()*.response().flatten()

        def modelTypes = (successTypes + inferredTypes + additionalTypes + errorTypes).grep() as Set<Class>

        modelTypes.addAll(additionalClassesAnnotations*.value().flatten())

        log.debug "modelTypes: $modelTypes"
        Map models = getModels(modelTypes.findAll { !it.isEnum() && it != Void })

        def updateDocFromUrlMappings = createUrlMappingsClosure(apis, resourcePath, resourcePathParams)
        def updateDocWithoutUrlMappings = createNonUrlMappingsClosure(apis)
        def updateDocumentation = apis ? updateDocFromUrlMappings : updateDocWithoutUrlMappings


        updateDocumentationForController(apiMethods, theController, modelTypes, updateDocumentation, domainName, theControllerClazz)

        log.debug("Apis: $apis")

        defineController(api, absoluteBasePath, resourcePath, basePath, theControllerClazz, groupApis(apis), models)
    }

    @CompileStatic
    private void updateDocumentationForController(
            List<Method> apiMethods, GrailsClass theController, Set<Class> modelTypes,
            BiConsumer<String, MethodDocumentation> updateDocumentation, String domainName, Class theControllerClazz) {

        // Update APIs with low-level method annotations
        apiMethods.each { method ->
            documentMethodWithSwaggerAnnotations(method, theController, modelTypes).each {
                updateDocumentation.accept(method.name, it)
            }
        }

        // Update APIs with swaggydoc method annotations
        ServiceDefaults.DefaultActionComponents.
                each { action, defaultsFactory ->
                    def defaults = (defaultsFactory as Function<String, DefaultAction>).apply(domainName)
                    methodsOfType(defaults.swaggyAnnotation, theControllerClazz).
                            each { method ->
                                generateMethodFromSwaggyAnnotations(action, method, theController).
                                        each { updateDocumentation.accept(method.name, it) }
                            }
                }
    }

    @CompileStatic
    private ControllerDefinition defineController(
            Api api, String absoluteBasePath, String resourcePath, String basePath, Class<Object> theControllerClazz,
            List<MethodDocumentation> groupedApis, Map<String, ModelDescription> models) {
        new ControllerDefinition(
                apiVersion: apiVersion,
                swaggerVersion: SwaggerVersion,
                basePath: api?.basePath() ?: absoluteBasePath,
                resourcePath: resourcePath - basePath,
                produces: (api?.produces()?.tokenize(',') ?: responseContentTypes(theControllerClazz)) as String[],
                consumes: (api?.consumes()?.tokenize(',') ?: DefaultRequestContentTypes) as String[],
                apis: groupedApis as MethodDocumentation[],
                models: models
        )
    }

    @CompileStatic
    private static List<Parameter> getParameters(
            DefaultAction defaults, List<Parameter> pathParams, List<String> pathParts) {
        def parameters = defaults?.parameters + pathParams
        if (pathParts[-1] != "{id}") {
            // Special case: defaults may include 'id' for single resource paths
            parameters.removeAll { it.name == 'id' }
        }
        parameters as List<Parameter>
    }

    @CompileStatic
    private static MethodDocumentation methodDoc(
            List<String> pathParts, String httpMethod, DefaultAction defaults, UrlMapping mapping,
            List<Parameter> parameters, String controllerName, String domainName) {
        def relativeLink = '/' + pathParts.join('/')
        def nickName = "${httpMethod.toLowerCase()}${controllerName}${mapping.actionName}"
        def summary = "${mapping.actionName} ${domainName}"
        def responseType = defaults.responseType
        def responseMessages = defaults?.responseMessages ?: [] as List<ResponseMessage>
        def methodDocumentation =
                defineAction(relativeLink, httpMethod, responseType, nickName, parameters, responseMessages, summary).
                        with {
                            if (defaults.isList) {
                                it.operations[0].type = 'array'
                                it.operations[0].items = new RefItem(responseType)
                            }
                            it
                        }
        methodDocumentation
    }

    private List<MethodDocumentation> groupApis(Map<String, MethodDocumentation> apis) {
        def groupedApis = apis.
                findAll { k, v -> k && v && v.operations.any { op -> op.method != '*' } }.values().
                groupBy { MethodDocumentation it -> it.path }.
                collect { path, methodDocs -> createMethodDoc(path, methodDocs) }
        groupedApis
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    private MethodDocumentation createMethodDoc(String path, List<MethodDocumentation> methodDocs) {
        new MethodDocumentation(path, null, methodDocs*.operations.flatten().unique() as Operation[])
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    @CompileStatic
    private BiConsumer<String, MethodDocumentation> createNonUrlMappingsClosure(Map<String, MethodDocumentation> apis) {
        return new BiConsumer<String, MethodDocumentation>() {
            @Override
            void accept(String action, MethodDocumentation documentation) {
                if (apis.containsKey(action)) {
                    apis[action].operations = uniqOperations([apis[action], documentation])
                } else {
                    apis[action] = documentation
                }
            }
        }
    }

    private static Operation[] uniqOperations(List<MethodDocumentation> a) {
        a*.operations.flatten().unique() as Operation[]
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    @CompileStatic
    private BiConsumer<String, MethodDocumentation> createUrlMappingsClosure(
            Map<String, MethodDocumentation> apis, String resourcePath, List<Parameter> resourcePathParams) {
        return new BiConsumer<String, MethodDocumentation>() {
            @Override
            void accept(String action, MethodDocumentation documentation) {
                if (apis.containsKey(action)) {
                    // leave the path alone, update everything else
                    apis[action].operations[0] << documentation.operations[0]
                } else {
                    documentation.path = documentation.path.replaceFirst(/^.+(?=\/)/, resourcePath)
                    apis[action] = documentation
                }

                if (resourcePathParams) {
                    // Add additional params needed to support hierarchical path mappings
                    def parameters = apis[action].operations[0].parameters.toList()
                    int idx = 0
                    resourcePathParams.each { rpp ->
                        if (!parameters.find { it.name == rpp.name })
                            parameters.add(idx++, rpp)
                    }
                    apis[action].operations[0].parameters = parameters as Parameter[]
                }
            }
        }
    }

    @CompileStatic
    @SuppressWarnings("GrMethodMayBeStatic")
    private <T> List<T> filter(ArrayList<Annotation> allAnnotations, Class<T> clazz) {
        allAnnotations.findAll { Annotation annotation -> annotation.annotationType() == clazz } as List<T>
    }

    @CompileStatic
    @SuppressWarnings("GrMethodMayBeStatic")
    private Pair<List<String>, List<Parameter>> populatePaths(UrlMapping mapping) {
        List<Parameter> pathParams = []
        List<String> pathParts = []
        def constraintIdx = 0
        log.debug "Tokens for mapping: ${mapping.urlData.tokens}"
        mapping.urlData.tokens.
                eachWithIndex { String token, int idx ->
                    if (token.matches(/^\(.*[\*\+]+.*\)$/)) {
                        def param = (idx == mapping.urlData.tokens.size() - 1) ? 'id' :
                                mapping.constraints[constraintIdx]?.propertyName
                        constraintIdx++
                        if (param != 'id') {
                            // Don't push 'id' as it is one of the default pathParams
                            pathParams.push(makePathParam(param))
                        }
                        pathParts.push("{$param}".toString())
                    } else {
                        pathParts.push(token)
                    }
                }
        log.debug "PathParts: ${pathParts}"
        log.debug "PathParams: ${pathParams}"
        new Pair(pathParts, pathParams)
    }

    /**
     * Converts a controller to an api declaration
     * @param controller
     */
    @CompileStatic
    @SuppressWarnings("GrMethodMayBeStatic")
    private ApiDeclaration controllerToApi(GrailsClass controller) {
        def name = controller.logicalPropertyName
        new ApiDeclaration(
                path: grailsLinkGenerator.link(controller: 'api', action: 'show', id: name, absolute: true),
                description: getApi(controller).description() ?: controller.naturalName
        )
    }

    /**
     * Provides an Info Object for Swagger
     * @return
     */
    @CompileStatic
    private ApiInfo getInfoObject() {
        config.with { new ApiInfo(contact, description, license, licenseUrl, termsOfServiceUrl, title) }
    }

    /**
     * Obtains an Api Annotation from a controller
     *
     * @param controller
     * @return
     */
    @CompileStatic
    private static Api getApi(GrailsClass controller) {
        controller.clazz.annotations.find { Annotation annotation -> annotation.annotationType() == Api } as Api
    }

    /**
     * Finds an annotation of given type in an object
     *
     * @param clazz
     * @param object
     * @return
     */
    @CompileStatic
    private static <T> T findAnnotation(Class<T> clazz, AccessibleObject object) {
        object.annotations.find { it.annotationType() == clazz } as T
    }

    @CompileStatic
    private static List<Method> methodsOfType(Class annotation, Class theControllerClazz) {
        theControllerClazz.methods.findAll { findAnnotation(annotation, it) } as List<Method>
    }

    @SuppressWarnings("UnnecessaryQualifiedReference")
    @CompileStatic
    private def getMarshallingConfigForDomain(String domainName) {
        getMarshallingConfig()[domainName]
    }

    @Memoized
    private Map getMarshallingConfig() {
        def retval = [:]
        if (Holders.pluginManager.hasGrailsPlugin('marshallers')) {
            def MarshallingConfigBuilder = grailsApplication.getClassLoader().loadClass(MARSHALLING_CONFIG_BUILDER)
            grailsApplication['domainClasses'].each { GrailsClass it ->
                def clazz = it.clazz
                Closure mc = GrailsClassUtils.getStaticPropertyValue(clazz, 'marshalling') as Closure
                if (mc) {
                    def builder = MarshallingConfigBuilder.newInstance(clazz)
                    mc.delegate = builder
                    mc.resolveStrategy = Closure.DELEGATE_FIRST
                    mc()
                    retval[clazz.name] = builder['config']
                }
            }
        }
        retval
    }

    private void processMarshallingConfig(mConfig, props, addProp, domainClass) {
        if (mConfig.name != "default")
        // Currently we only support default marshalling, as that is conventional pattern
            return
        // N.B. we are not checking config.type, so we will conflate json and xml together
        // adding or suppressing fields for only one response type is an anti-pattern anyway
        if (!mConfig.shouldOutputIdentifier) {
            props.remove(domainClass.identifier)
        }
        if (!mConfig.shouldOutputVersion) {
            props.remove(domainClass.version)
        }
        if (mConfig.shouldOutputClass) {
            addProp("class")
        }
        mConfig.ignore?.each { fn ->
            props.removeAll { it.name == fn }
        }
        mConfig.virtual?.keySet()?.each(addProp)
//                        deepProps.addAll(config.deep ?: [])
        mConfig.children?.each { processMarshallingConfig(it, props, addProp, domainClass) }
    }

    private Map<String, ModelDescription> getModels(Collection<Class<?>> modelTypes) {
        Queue m = modelTypes as Queue
        def models = [:] as Map<String, ModelDescription>
        while (m.size()) {
            Class model = m.poll()
            log.debug "Getting model for class ${model}"
            GrailsDomainClass domainClass = grailsApplication.domainClasses.
                    find { it.clazz == model } as GrailsDomainClass
            /** Duck typing here:
             * if model has a GrailsDomainClass then props will be list of GrailsDomainClassProperty objects
             * otherwise props will be a list of Field objects
             * Interface for these two classes are similar enough to duck type for our purposes
             */
            Collection<GrailsDomainClassProperty> fieldSource = getFieldSource(domainClass, model)

            def props = fieldSource.findAll {
                !it.toString().contains(' static ') && !it.toString().contains(' transient ') && it.name != 'errors'
            }

            Map<String, ConstrainedProperty> constrainedProperties =
                    domainClass?.constrainedProperties ?:
                            model.declaredMethods.find { it.name == 'getConstraints' } ? model.constraints : null

            def optional = constrainedProperties?.findAll { k, v -> v.isNullable() }

            if (domainClass) {
                // Check for marshalling config
                def marshallingConfig = getMarshallingConfigForDomain(domainClass.fullName)
                if (marshallingConfig) {
//                    Set deepProps = [] as Set
                    def addProp = { fn ->
                        props.add([name: fn, type: String])
                        optional[fn] = true
                    }
                    processMarshallingConfig(marshallingConfig, props, addProp, domainClass)
                    // FIXME: Handle "deep" the way marshallers does (may be a bit tricky)
//                    props.findAll { f ->
//                        !(deepProps.contains(f.name) || knownTypes.contains(f.type))
//                    }.each { f ->
//                    }
                }
            }

            def required = props.collect { f -> f.name } - optional*.key

            def modelDescription = new ModelDescription(
                    id: model.simpleName,
                    required: required,
                    properties: props.collectEntries { f -> [f.name, getTypeDescriptor(f, domainClass)] }
            )

            models[model.simpleName] = modelDescription
            log.debug "Added ${model.simpleName} to models"
            props.each { f ->
                log.debug("Processing field ${f} of type ${f.type}")
                if (!models.containsKey(f.type.simpleName) && !m.contains(f.type) && !ServiceDefaults.knownTypes.contains(f.type)) {
                    if (List.isAssignableFrom(f.type) || Set.isAssignableFrom(f.type)) {
                        if (f instanceof GrailsDomainClassProperty) {
                            Class genericType = domainClass?.associationMap?.getAt(f.name)
                            if (genericType) {
                                log.debug "Add #1"
                                if (!models.containsKey(genericType.simpleName)) {
                                    m.add(genericType)
                                }
                            } else {
                                log.warn "No type args found for ${f.name}"
                            }
                        } else {
                            log.debug "Add #2"
                            if (model != f.genericType.actualTypeArguments[0]) {
                                m.add(f.genericType.actualTypeArguments[0])
                            }
                        }
                    } else {
                        log.debug "Add #3"
                        m.add(f.type)
                    }

                }
            }
        }
        models
    }

    @CompileStatic
    private static Collection<GrailsDomainClassProperty> getFieldSource(GrailsDomainClass domainClass, Class<?> model) {
        if (domainClass) {
            [domainClass.identifier, domainClass.version] + domainClass.persistentProperties.toList()
        } else {
            def fieldSource = []
            Class tmpClass = model
            while (tmpClass != null) {
                fieldSource.addAll(Arrays.asList(tmpClass.declaredFields))
                tmpClass = tmpClass.superclass
            }
            fieldSource
        }
    }

    private List<MethodDocumentation> generateMethodFromSwaggyAnnotations(
            String action, Method method, GrailsClass theController) {
        def basePath = grailsLinkGenerator.link(uri: '')
        def slug = theController.logicalPropertyName
        def domainName = ServiceDefaults.slugToDomain(slug)
        DefaultAction defaults = ServiceDefaults.DefaultActionComponents[action].apply(domainName)
        List<Parameter> parameters = defaults.parameters.clone() as List<Parameter>
        if (defaults.swaggyAnnotation.metaClass.getMetaMethod('searchParam')
                && findAnnotation(defaults.swaggyAnnotation, method).searchParam()) {
            parameters << new Parameter('q', 'Query. Follows Lucene Query Syntax.', 'query', 'string')
        }
        if (defaults.swaggyAnnotation.metaClass.getMetaMethod('extraParams')
                && findAnnotation(defaults.swaggyAnnotation, method).extraParams()) {
            findAnnotation(defaults.swaggyAnnotation, method).extraParams().each { ApiImplicitParam param ->
                parameters << new Parameter(param)
            }
        }
        def pathParams = parameters.
                findAll { it.paramType == 'path' }.
                collect { it.name }.
                collectEntries { [it, "{${it}}"] }
        def fullLink = grailsLinkGenerator.link(controller: slug, action: method.name, params: pathParams) as String
        def link = fullLink.replace('%7B', '{').replace('%7D', '}') - basePath
        def httpMethods = getHttpMethod(theController, method)
        log.debug "Link: $link - ${httpMethods}"
        httpMethods.
                collect { httpMethod ->
                    def inferredNickname = method.name
                    log.debug "Generating ${inferredNickname}"
                    defineAction(link, httpMethod, domainName, inferredNickname, parameters, defaults.responseMessages,
                            "${action} ${domainName}")
                }
    }

    @CompileStatic
    private static MethodDocumentation defineAction(
            String link, String httpMethod, String responseType, String inferredNickname,
            List<Parameter> parameters, List<ResponseMessage> responseMessages, String summary) {

        def operation = new Operation(
                method: httpMethod,
                summary: summary,
                nickname: inferredNickname,
                parameters: parameters as Parameter[],
                type: responseType,
                responseMessages: (responseMessages ?: []) as ResponseMessage[],
        )

        new MethodDocumentation(link, null, [operation] as Operation[])
    }

    private List<MethodDocumentation> documentMethodWithSwaggerAnnotations(
            Method method, GrailsClass theController, Set<Class> modelTypes) {
        def basePath = grailsLinkGenerator.link(uri: '')
        def apiOperation = findAnnotation(ApiOperation, method)
        def apiResponses = findAnnotation(ApiResponses, method)
        def apiParams = findAnnotation(ApiImplicitParams, method)?.value() ?: []
        def pathParamsAnnotations = apiParams.findAll { it.paramType() == 'path' } as List<ApiImplicitParam>
        def pathParams = pathParamsAnnotations*.name().collectEntries { [it, "{${it}}"] }

        log.debug "## pathParams: ${pathParams}"

        def slug = theController.logicalPropertyName

        def fullLink = grailsLinkGenerator.link(controller: slug, action: method.name, params: pathParams) as String
        def link = fullLink.replace('%7B', '{').replace('%7D', '}') - basePath
        def httpMethods = getHttpMethod(theController, method)
        log.debug "Link: $link - ${httpMethods}"
        httpMethods.collect { httpMethod ->
            List<Parameter> parameters = apiParams?.collect { new Parameter(it as ApiImplicitParam, modelTypes) } ?: []
            log.debug "## parameters: ${parameters}"
            def inferredNickname = "${method.name}"
            log.debug "Generating ${inferredNickname}"

            def responseType = apiOperation.response() == Void ? 'void' : apiOperation.response().simpleName
            def responseIsArray = apiOperation.responseContainer()

            def operation = new Operation(
                    method: httpMethod,
                    summary: apiOperation.value(),
                    notes: apiOperation.notes(),
                    nickname: apiOperation.nickname() ?: inferredNickname,
                    parameters: parameters as Parameter[],
                    type: responseIsArray.isEmpty() ? responseType : responseIsArray,
                    responseMessages: apiResponses?.value()?.collect { new ResponseMessage(it) } ?: [],
                    produces: apiOperation.produces().split(',')*.trim().findAll { it } ?: null,
                    consumes: apiOperation.consumes().split(',')*.trim().findAll { it } ?: null,
                    items: responseIsArray ? new RefItem(responseType) : null
            )

            new MethodDocumentation(link, null, [operation] as Operation[])
        }

    }

    private List<String> getHttpMethod(GrailsClass theController, Method method) {
        try {
            def retval = theController.referenceInstance.allowedMethods[method.name] ?: 'GET'
            if (retval instanceof String) {
                log.debug "[Returned] ${method.name} supports [$retval]"
                [retval]
            } else if (retval instanceof Collection<String>) {
                def list = ServiceDefaults.removeBoringMethods(retval.toList(), ['GET', 'POST'])
                log.debug "[Returned] ${method.name} supports $list"
                list
            } else {
                log.debug "[Fallback] ${method.name} supports ['GET']"
                ['GET']
            }
        } catch (Exception ignored) {
            log.debug "[Exception] ${method.name} supports ['GET']"
            ['GET']
        }
    }


    @SuppressWarnings("GrMethodMayBeStatic")
    private Field getTypeDescriptor(def f, GrailsDomainClass gdc) {

        String fieldName = f.name
        def declaredField = gdc?.clazz?.getDeclaredFields()?.find { it.name == fieldName }
        declaredField = declaredField ?: (f instanceof java.lang.reflect.Field ? f : null)
        def apiModelProperty = declaredField ? findAnnotation(ApiModelProperty, declaredField) : null
        def constrainedProperty = gdc?.constrainedProperties?.getAt(fieldName) as ConstrainedProperty
        Class type = f.type
        Field field = getPrimitiveType(type, constrainedProperty) ?:
                type.isAssignableFrom(Set) || type.isAssignableFrom(List) ?
                        (f instanceof GrailsDomainClassProperty ? getField(gdc, fieldName) : getField(f)) :
                        new RefField(type.simpleName)

        field.description = apiModelProperty?.value()
        return field
    }

    private ContainerField getField(f) {
        Class genericType = f.genericType.actualTypeArguments[0] as Class
        Field listPrimitiveField = getPrimitiveType(genericType)
        if (listPrimitiveField) {
            new ContainerField(new TypeItem(listPrimitiveField.type, listPrimitiveField.format))
        } else {
            new ContainerField(new RefItem(genericType.simpleName))
        }
    }

    private ContainerField getField(GrailsDomainClass gdc, String fieldName) {
        Class genericType = gdc?.associationMap?.getAt(fieldName) as Class
        if (genericType) {
            new ContainerField(new RefItem(genericType.simpleName))
        } else {
            log.warn "Unknown type for property ${fieldName}, please specify it in the domain's class hasMany"
            new ContainerField(null, null)
        }
    }

    private static Field getPrimitiveType(Class type, ConstrainedProperty constrainedProperty = null) {
        if (type.isAssignableFrom(String)) {
            constrainedProperty?.inList ? new StringField(constrainedProperty.inList as String[]) : new StringField()
        } else if (type.isEnum()) {
            new StringField(type.values()*.name() as String[])
        } else if ([Double, double, Float, float, BigDecimal].any { type.isAssignableFrom(it) }) {
            addNumericConstraints(constrainedProperty, new NumberField('number', 'double'))
        } else if ([Long, long, Integer, int, BigInteger].any { type.isAssignableFrom(it) }) {
            addNumericConstraints(constrainedProperty, new NumberField('integer', 'int64'))
        } else if (type.isAssignableFrom(Date)) {
            new StringField('date-time')
        } else if (type.isAssignableFrom(byte) || type.isAssignableFrom(Byte)) {
            new StringField('byte')
        } else if (type.isAssignableFrom(Boolean) || type.isAssignableFrom(boolean)) {
            new BooleanField()
        }
    }

    @CompileStatic
    private static NumberField addNumericConstraints(ConstrainedProperty constrainedProperty, NumberField retval) {
        if (constrainedProperty?.range) {
            retval.maximum = constrainedProperty.range.to as int
            retval.minimum = constrainedProperty.range.from as int
        } else if (constrainedProperty?.min) {
            retval.minimum = constrainedProperty.min as int
        } else if (constrainedProperty?.max) {
            retval.maximum = constrainedProperty.max as int
        }
        retval
    }

    @CompileStatic
    private List<String> responseContentTypes(Class controller) {
        GrailsClassUtils.getStaticPropertyValue(controller, 'responseFormats')?.
                collect { String it ->
                    grailsMimeUtility.getMimeTypeForExtension(it)?.name ?: it.contains('/') ? it : null
                }?.
                grep() as List<String> ?: DefaultResponseContentTypes
    }
}
