package com.github.rahulsom.swaggydoc

import com.wordnik.swagger.annotations.*
import grails.util.Holders
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.codehaus.groovy.grails.web.mapping.UrlMapping
import org.codehaus.groovy.grails.web.mapping.UrlMappings
import org.codehaus.groovy.grails.web.mime.MimeUtility

import java.lang.annotation.Annotation
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Method

import static org.springframework.http.HttpStatus.*

class SwaggyDataService {

    public static final Closure<DefaultAction> actionFallback = { new DefaultAction() }
    DefaultGrailsApplication grailsApplication
    LinkGenerator grailsLinkGenerator
    UrlMappings grailsUrlMappingsHolder
    MimeUtility grailsMimeUtility

    static final String SwaggerVersion = '1.2'
    static final List<String> DefaultResponseContentTypes = ['application/json', 'application/xml', 'text/html']
    static final List<String> DefaultRequestContentTypes = [
            'application/json', 'application/xml', 'application/x-www-form-urlencoded'
    ]
    /*
     * These don't get documented in the listing of models.
     */
    static final List knownTypes = [
            int, Integer, long, Long, float, Float, double, Double, BigInteger, BigDecimal,
            String, boolean, Boolean, Date, byte, Byte, void
    ]

    public static final Map<String, Closure<DefaultAction>> DefaultActionComponents = [
            index : { String domainName ->
                new DefaultAction(SwaggyList, domainName, [
                        new Parameter('offset', 'Records to skip. Empty means 0.', 'query', 'int'),
                        new Parameter('max', 'Max records to return. Empty means 10.', 'query', 'int'),
                        new Parameter('sort', 'Field to sort by. Empty means id if q is empty. If q is provided, empty ' +
                                'means relevance.', 'query', 'string'),
                        new Parameter('order', 'Order to sort by. Empty means asc if q is empty. If q is provided, empty ' +
                                'means desc.', 'query', 'string').with {
                            _enum = ['asc', 'desc']
                            it
                        },
                ], [], true)
            },
            show  : { String domainName ->
                new DefaultAction(SwaggyShow, domainName, [new Parameter('id', 'Identifier to look for', 'path', 'string', true)],
                        [
                                new ResponseMessage(BAD_REQUEST, 'Bad Request'),
                                new ResponseMessage(NOT_FOUND, "Could not find ${domainName} with that Id"),
                        ]
                )
            },
            save  : { String domainName ->
                new DefaultAction(SwaggySave, domainName, [new Parameter('body', "Description of ${domainName}", 'body', domainName, true)],
                        [
                                new ResponseMessage(CREATED, "New ${domainName} created"),
                                new ResponseMessage(UNPROCESSABLE_ENTITY, 'Malformed Entity received'),
                        ]
                )
            },
            update: { String domainName ->
                new DefaultAction(SwaggyUpdate, domainName,
                        [
                                new Parameter('id', "Id to update", 'path', 'string', true),
                                new Parameter('body', "Description of ${domainName}", 'body', domainName, true),
                        ],
                        [
                                new ResponseMessage(BAD_REQUEST, 'Bad Request'),
                                new ResponseMessage(NOT_FOUND, "Could not find ${domainName} with that Id"),
                                new ResponseMessage(UNPROCESSABLE_ENTITY, 'Malformed Entity received'),
                        ]
                )
            },
            patch : { String domainName ->
                new DefaultAction(SwaggyPatch, domainName,
                        [
                                new Parameter('id', "Id to patch", 'path', 'string', true),
                                new Parameter('body', "Description of ${domainName}", 'body', domainName, true),
                        ],
                        [
                                new ResponseMessage(BAD_REQUEST, 'Bad Request'),
                                new ResponseMessage(NOT_FOUND, "Could not find ${domainName} with that Id"),
                                new ResponseMessage(UNPROCESSABLE_ENTITY, 'Malformed Entity received'),
                        ]
                )
            },
            delete: { String domainName ->
                new DefaultAction(SwaggyDelete, 'void', [new Parameter('id', "Id to delete", 'path', 'string', true)],
                        [
                                new ResponseMessage(NO_CONTENT, 'Delete successful'),
                                new ResponseMessage(BAD_REQUEST, 'Bad Request'),
                                new ResponseMessage(NOT_FOUND, "Could not find ${domainName} with that Id"),
                        ]
                )
            }
    ]

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
    Resources resources() {
        ApiDeclaration[] apis = grailsApplication.controllerClasses.
                findAll { SwaggyDataService.getApi(it) }.
                sort { SwaggyDataService.getApi(it).position() }.
                collect { controllerToApi(it) }.
                toArray([] as ApiDeclaration[])

        new Resources(
                apiVersion: apiVersion,
                swaggerVersion: SwaggerVersion,
                info: infoObject,
                apis: apis,
        )
    }

    /**
     * Generates details about a particular API given a controller name
     * @param controllerName
     * @return Map
     */
    ControllerDefinition apiDetails(String controllerName) {
        GrailsControllerClass theController = grailsApplication.controllerClasses.find {
            it.logicalPropertyName == controllerName && SwaggyDataService.getApi(it)
        } as GrailsControllerClass
        if (!theController) {
            return null
        }
        Class theControllerClazz = theController.referenceInstance.class

        Api api = getApi(theController)

        def absoluteBasePath = grailsLinkGenerator.link(uri: '', absolute: true)
        def basePath = grailsLinkGenerator.link(uri: '')
        def resourcePath = grailsLinkGenerator.link(controller: theController.logicalPropertyName)
        def domainName = slugToDomain(controllerName)

        // These preserve the path components supporting hierarchical paths discovered through URL mappings
        List<String> resourcePathParts
        List<Parameter> resourcePathParams
        Map<String, MethodDocumentation> apis = grailsUrlMappingsHolder.
                urlMappings.
                findAll { it.controllerName == controllerName }.
                collectEntries { mapping ->

                    log.debug("Mapping: $mapping")
                    def paths = populatePaths(mapping)
                    List<String> pathParts = paths.left
                    List<Parameter> pathParams = paths.right
                    // Capture resource path candidates
                    if (!resourcePathParts || resourcePathParts.size() > pathParts.size()) {
                        resourcePathParts = pathParts
                        resourcePathParams = pathParams
                    }

                    def actionMethod = DefaultActionComponents.get(mapping.actionName)
                    DefaultAction defaults = (actionMethod ?: actionFallback)(domainName)
                    log.debug "defaults?.parameters: ${defaults?.parameters}"
                    log.debug "pathParams: ${pathParams}"
                    List<Parameter> parameters = (defaults?.parameters?.clone() ?: []) + pathParams
                    if (pathParts[-1] != "{id}") {
                        // Special case: defaults may include 'id' for single resource paths
                        parameters.removeAll { it.name == 'id' }
                    }
                    [
                            mapping.actionName,
                            defineAction('/' + pathParts.join('/'), mapping.httpMethod, defaults.responseType,
                                    "${mapping.httpMethod.toLowerCase()}${controllerName}${mapping.actionName}",
                                    parameters, defaults?.responseMessages ?: [], "${mapping.actionName} ${domainName}"
                            ).with {
                                if (defaults.isList) {
                                    it.operations[0].type = 'array'
                                    it.operations[0].items = new RefItem(defaults.responseType)
                                }
                                it
                            }
                    ]
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

        def updateDocFromUrlMappings = { String action, MethodDocumentation documentation ->
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

        def updateDocWithoutUrlMappings = { String action, MethodDocumentation documentation ->
            if (apis.containsKey(action)) {
                apis[action].operations = (
                        apis[action].operations.toList() +
                                documentation.operations.toList()).unique() as Operation[]
            } else {
                apis[action] = documentation
            }
        }

        def updateDocumentation = apis ? updateDocFromUrlMappings : updateDocWithoutUrlMappings

        // Update APIs with low-level method annotations
        apiMethods.each { method ->
            documentMethodWithSwaggerAnnotations(method, theController, modelTypes).each {
                updateDocumentation(method.name, it)
            }
        }

        // Update APIs with swaggydoc method annotations
        DefaultActionComponents.
                each { action, defaultsFactory ->
                    def defaults = defaultsFactory(domainName)
                    SwaggyDataService.methodsOfType(defaults.swaggyAnnotation, theControllerClazz).
                            collectMany { method ->
                                generateMethodFromSwaggyAnnotations(action, method, theController).
                                        collect { updateDocumentation(method.name, it) }
                            }
                }

        log.debug("Apis: $apis")

        def groupedApis = apis.
                findAll { k, v -> k && v && v.operations.any { op -> op.method != '*' } }.
                values().
                groupBy { MethodDocumentation it -> it.path }.
                collect { path, methodDocs ->
                    new MethodDocumentation(path, null, methodDocs*.operations.flatten().unique() as Operation[])
                }

        return new ControllerDefinition(
                apiVersion: apiVersion,
                swaggerVersion: SwaggerVersion,
                basePath: api?.basePath() ?: absoluteBasePath,
                resourcePath: resourcePath - basePath,
                produces: api?.produces()?.tokenize(',') ?: responseContentTypes(theControllerClazz),
                consumes: api?.consumes()?.tokenize(',') ?: DefaultRequestContentTypes,
                apis: groupedApis,
                models: models
        )
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
        mapping.urlData.tokens.eachWithIndex { String token, int idx ->
            if (token.matches(/^\(.*[\*\+]+.*\)$/)) {
                def param = (idx == mapping.urlData.tokens.size() - 1) ? 'id' :
                        mapping.constraints[constraintIdx]?.propertyName
                constraintIdx++
                if (param != 'id') {
                    // Don't push 'id' as it is one of the default pathParams
                    pathParams.push(makePathParam(param))
                }
                pathParts.push("{" + param + "}")
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
    private ApiInfo getInfoObject() {
        config.with { new ApiInfo(contact, description, license, licenseUrl, termsOfServiceUrl, title) }
    }

    /**
     * Obtains an Api Annotation from a controller
     *
     * @param controller
     * @return
     */
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
    private static <T> T findAnnotation(Class<T> clazz, AccessibleObject object) {
        object.annotations.find { it.annotationType() == clazz } as T
    }

    private static List<Method> methodsOfType(Class annotation, Class theControllerClazz) {
        theControllerClazz.methods.findAll { SwaggyDataService.findAnnotation(annotation, it) } as List<Method>
    }

    /**
     * Provides optional support for the "marshallers" grails plugin when building models from domains
     */
    private Map _marshallingConfig

    private def getMarshallingConfigForDomain(String domainName) {
        if (_marshallingConfig == null) {
            _marshallingConfig = [:]
            if (Holders.pluginManager.hasGrailsPlugin('marshallers')) {
                def MarshallingConfigBuilder = grailsApplication.getClassLoader().
                        loadClass("org.grails.plugins.marshallers.config.MarshallingConfigBuilder")
                grailsApplication.domainClasses.each {
                    def clazz = it.clazz
                    Closure mc = GrailsClassUtils.getStaticPropertyValue(clazz, 'marshalling') as Closure
                    if (mc) {
                        def builder = MarshallingConfigBuilder.newInstance(clazz)
                        mc.delegate = builder
                        //noinspection UnnecessaryQualifiedReference
                        mc.resolveStrategy = Closure.DELEGATE_FIRST
                        mc()
                        _marshallingConfig[clazz.name] = builder.config
                    }
                }
            }
        }
        return _marshallingConfig[domainName]
    }

    private def processMarshallingConfig(mConfig, props, addProp, domainClass) {
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
            GrailsDomainClass domainClass = grailsApplication.domainClasses.find {
                it.clazz == model
            } as GrailsDomainClass
            /** Duck typing here:
             * if model has a GrailsDomainClass then props will be list of GrailsDomainClassProperty objects
             * otherwise props will be a list of Field objects
             * Interface for these two classes are similar enough to duck type for our purposes
             */
            def fieldSource = []
            if (domainClass) {
                fieldSource = [domainClass.identifier, domainClass.version] + domainClass.persistentProperties.toList()
            } else {
                Class tmpClass = model
                while (tmpClass != null) {
                    fieldSource.addAll(Arrays.asList(tmpClass.declaredFields))
                    tmpClass = tmpClass.superclass
                }
            }

            def props = fieldSource.
                    findAll {
                        !it.toString().contains(' static ') &&
                                !it.toString().contains(' transient ') &&
                                it.name != 'errors'
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
                log.debug "Property ${f.name} is ${f.type}"
                if (!models.containsKey(f.type.simpleName) && !m.contains(f.type) && !knownTypes.contains(f.type)) {
                    if (f.type.isAssignableFrom(List) || f.type.isAssignableFrom(Set)) {
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
                            m.add(f.genericType.actualTypeArguments[0])
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

    private List<MethodDocumentation> generateMethodFromSwaggyAnnotations(
            String action, Method method, GrailsClass theController) {
        def basePath = grailsLinkGenerator.link(uri: '')
        def slug = theController.logicalPropertyName
        def domainName = slugToDomain(slug)
        DefaultAction defaults = DefaultActionComponents[action](domainName)
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
        httpMethods.collect { httpMethod ->
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
        new MethodDocumentation(link, null, [
                new Operation(
                        method: httpMethod,
                        summary: summary,
                        nickname: inferredNickname,
                        parameters: parameters as Parameter[],
                        type: responseType,
                        responseMessages: (responseMessages ?: []) as ResponseMessage[],
                )
        ] as Operation[])
    }

    private List<MethodDocumentation> documentMethodWithSwaggerAnnotations(Method method, GrailsClass theController, Set<Class> modelTypes) {
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

            new MethodDocumentation(link, null, [
                    new Operation(
                            method: httpMethod,
                            summary: apiOperation.value(),
                            notes: apiOperation.notes(),
                            nickname: apiOperation.nickname() ?: inferredNickname,
                            parameters: parameters as Parameter[],
                            type: responseIsArray.isEmpty() ? responseType : responseIsArray,
                            responseMessages: apiResponses?.value()?.collect { new ResponseMessage(it) } ?: [],
                            produces: apiOperation.produces().split(',')*.trim().findAll { it } ?: null,
                            consumes: apiOperation.consumes().split(',')*.trim().findAll { it } ?: null,
                    ).with {
                        if (responseIsArray) {
                            it.items = new RefItem(responseType)
                        }
                        it
                    }
            ] as Operation[])
        }

    }

    @SuppressWarnings("GrMethodMayBeStatic")
    private List<String> getHttpMethod(GrailsClass theController, Method method) {
        try {
            def retval = theController.referenceInstance.allowedMethods[method.name] ?: 'GET'
            if (retval instanceof String) {
                log.debug "[Returned] ${method.name} supports [$retval]"
                [retval]
            } else if (retval instanceof Collection<String>) {
                def list = removeBoringMethods(retval.toList(), ['GET', 'POST'])
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

    private static List<String> removeBoringMethods(List<String> methods, List<String> boringMethods) {
        boringMethods.each { method ->
            if (methods.size() > 1 && methods.contains(method)) {
                methods.remove(method)
            }
        }
        methods
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    private Field getTypeDescriptor(def f, GrailsDomainClass gdc) {

        String fieldName = f.name
        def declaredField = gdc?.clazz?.getDeclaredFields()?.find { it.name == fieldName }
        declaredField = declaredField ?: (f instanceof java.lang.reflect.Field ? f : null)
        def apiModelProperty = declaredField ? findAnnotation(ApiModelProperty, declaredField) : null
        def constrainedProperty = gdc?.constraints?.getAt(fieldName) as ConstrainedProperty
        Class type = f.type
        Field field
        Field primitiveField = getPrimitiveType(type, constrainedProperty)
        if (primitiveField) {
            field = primitiveField
        } else if (type.isAssignableFrom(Set) || type.isAssignableFrom(List)) {
            Class genericType = null
            if (f instanceof GrailsDomainClassProperty) {
                genericType = gdc?.associationMap?.getAt(fieldName) as Class
                if (genericType) {
                    field = new ContainerField(new RefItem(genericType.simpleName))
                } else {
                    log.warn "Unknown type for property ${fieldName}, please specify it in the domain's class hasMany"
                    field = new ContainerField(null, null)
                }
            } else {
                genericType = genericType ?: f.genericType.actualTypeArguments[0] as Class
                Field listPrimitiveField = getPrimitiveType(genericType)
                if (listPrimitiveField) {
                    field = new ContainerField(new TypeItem(listPrimitiveField.type, listPrimitiveField.format))
                } else {
                    field = new ContainerField(new RefItem(genericType.simpleName))
                }
            }
        } else {
            field = new RefField(type.simpleName)
        }
        field.description = apiModelProperty?.value()
        return field
    }

    private Field getPrimitiveType(Class type, ConstrainedProperty constrainedProperty = null) {
        Field field
        if (type.isAssignableFrom(String)) {
            field = constrainedProperty?.inList ? new StringField(constrainedProperty.inList as String[]) : new StringField()
        } else if (type.isEnum()) {
            field = new StringField(type.values()*.name() as String[])
        } else if (type.isAssignableFrom(Double) || type.isAssignableFrom(Float) || type.isAssignableFrom(double) ||
                type.isAssignableFrom(float) || type.isAssignableFrom(BigDecimal)) {
            field = addNumericConstraints(constrainedProperty, new NumberField('number', 'double'))
        } else if (type.isAssignableFrom(Long) || type.isAssignableFrom(Integer) || type.isAssignableFrom(long) ||
                type.isAssignableFrom(int) || type.isAssignableFrom(BigInteger)) {
            field = addNumericConstraints(constrainedProperty, new NumberField('integer', 'int64'))
        } else if (type.isAssignableFrom(Date)) {
            field = new StringField('date-time')
        } else if (type.isAssignableFrom(byte) || type.isAssignableFrom(Byte)) {
            field = new StringField('byte')
        } else if (type.isAssignableFrom(Boolean) || type.isAssignableFrom(boolean)) {
            field = new BooleanField()
        }
        return field
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    private NumberField addNumericConstraints(ConstrainedProperty constrainedProperty, NumberField retval) {
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

    private static String slugToDomain(String slug) {
        slug.with { it.replaceFirst(it[0], it[0].toUpperCase()) }
    }

    private List<String> responseContentTypes(Class controller) {
        GrailsClassUtils.getStaticPropertyValue(controller, 'responseFormats')?.
                collect { String it ->
                    grailsMimeUtility.getMimeTypeForExtension(it)?.name ?: it.contains('/') ? it : null
                }?.
                grep() as List<String> ?: DefaultResponseContentTypes
    }
}
