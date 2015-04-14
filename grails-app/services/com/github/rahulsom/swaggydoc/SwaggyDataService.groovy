package com.github.rahulsom.swaggydoc

import com.wordnik.swagger.annotations.*
import grails.util.Holders
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClass
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.codehaus.groovy.grails.web.mapping.UrlMapping
import org.codehaus.groovy.grails.web.mapping.UrlMappings
import org.codehaus.groovy.grails.web.mime.MimeUtility

import java.lang.reflect.AccessibleObject
import java.lang.reflect.Method

import static org.springframework.http.HttpStatus.*

class SwaggyDataService {

    DefaultGrailsApplication grailsApplication
    LinkGenerator grailsLinkGenerator
    UrlMappings grailsUrlMappingsHolder
    MimeUtility grailsMimeUtility

    static final String SwaggerVersion = '1.2'
    static final List<String> DefaultResponseContentTypes = ['application/json', 'application/xml', 'text/html']
    static final List<String> DefaultRequestContentTypes = [
            'application/json', 'application/xml', 'application/x-www-form-urlencoded'
    ]
    static final List knownTypes = [int, Integer, long, Long, float, Float, double, Double, String]

    @Newify([Parameter, ResponseMessage, DefaultAction])
    public static final Map<String, Closure<DefaultAction>> DefaultActionComponents = [
            index : { String domainName ->
                DefaultAction(SwaggyList, [
                        Parameter('offset', 'Records to skip. Empty means 0.', 'query', 'int'),
                        Parameter('max', 'Max records to return. Empty means 10.', 'query', 'int'),
                        Parameter('sort', 'Field to sort by. Empty means id if q is empty. If q is provided, empty means relevance.', 'query', 'string'),
                        Parameter('order', 'Order to sort by. Empty means asc if q is empty. If q is provided, empty means desc.', 'query', 'string'),
                ], [])
            },
            show  : { String domainName ->
                DefaultAction(SwaggyShow, [Parameter('id', 'Identifier to look for', 'path', 'string', true)],
                        [
                                ResponseMessage(BAD_REQUEST, 'Bad Request'),
                                ResponseMessage(NOT_FOUND, "Could not find ${domainName} with that Id"),
                        ]
                )
            },
            save  : { String domainName ->
                DefaultAction(SwaggySave, [Parameter('body', "Description of ${domainName}", 'body', domainName, true)],
                        [
                                ResponseMessage(CREATED, "New ${domainName} created"),
                                ResponseMessage(UNPROCESSABLE_ENTITY, 'Malformed Entity received'),
                        ]
                )
            },
            update: { String domainName ->
                DefaultAction(SwaggyUpdate,
                        [
                                Parameter('id', "Id to update", 'path', 'string', true),
                                Parameter('body', "Description of ${domainName}", 'body', domainName, true),
                        ],
                        [
                                ResponseMessage(BAD_REQUEST, 'Bad Request'),
                                ResponseMessage(NOT_FOUND, "Could not find ${domainName} with that Id"),
                                ResponseMessage(UNPROCESSABLE_ENTITY, 'Malformed Entity received'),
                        ]
                )
            },
            patch : { String domainName ->
                DefaultAction(SwaggyPatch,
                        [
                                Parameter('id', "Id to patch", 'path', 'string', true),
                                Parameter('body', "Description of ${domainName}", 'body', domainName, true),
                        ],
                        [
                                ResponseMessage(BAD_REQUEST, 'Bad Request'),
                                ResponseMessage(NOT_FOUND, "Could not find ${domainName} with that Id"),
                                ResponseMessage(UNPROCESSABLE_ENTITY, 'Malformed Entity received'),
                        ]
                )
            },
            delete: { String domainName ->
                DefaultAction(SwaggyDelete, [Parameter('id', "Id to delete", 'path', 'string', true)],
                        [
                                ResponseMessage(NO_CONTENT, 'Delete successful'),
                                ResponseMessage(BAD_REQUEST, 'Bad Request'),
                                ResponseMessage(NOT_FOUND, "Could not find ${domainName} with that Id"),
                        ]
                )
            }
    ]

    private static Parameter makePathParam(String pathParam) {
        new Parameter(pathParam, "$pathParam identifier", 'path', 'string', true)
    }

    private ConfigObject getConfig() { grailsApplication.config.swaggydoc }
    private String getApiVersion() {config.apiVersion ?: grailsApplication.metadata['app.version']}

    /**
     * Generates map of Swagger Resources.
     * @return Map
     */
    Map resources() {
        [
                apiVersion    : apiVersion,
                swaggerVersion: SwaggerVersion,
                info          : infoObject,
                apis          : grailsApplication.controllerClasses.
                        findAll { getApi(it) }.
                        sort { getApi(it).position() }.
                        collect { controllerToApi(it) },
        ]
    }

    /**
     * Generates details about a particular API given a controller name
     * @param controllerName
     * @return Map
     */
    synchronized Map apiDetails(String controllerName) {
        def theController = grailsApplication.controllerClasses.find {
            it.logicalPropertyName == controllerName
        }
        def theControllerClazz = theController.referenceInstance.class

        Api api = getApi(theController)

        def absoluteBasePath = grailsLinkGenerator.link(uri: '', absolute: true)
        def basePath = grailsLinkGenerator.link(uri: '')
        def resourcePath = grailsLinkGenerator.link(controller: theController.logicalPropertyName)
        def domainName = slugToDomain(controllerName)

        // These preserve the path components supporting hierarchical paths discovered through URL mappings
        List resourcePathParts
        List resourcePathParams
        Map<String, MethodDocumentation> apis = grailsUrlMappingsHolder.urlMappings.
                findAll { it.controllerName == controllerName }.
                collectEntries { mapping ->
                    def (List pathParts, List pathParams) = populatePaths(mapping)
                    // Capture resource path candidates
                    if (!resourcePathParts || resourcePathParts.size() > pathParts.size()) {
                        resourcePathParts = pathParts
                        resourcePathParams = pathParams
                    }
                    DefaultAction defaults = (DefaultActionComponents.get(mapping.actionName) ?:
                            { new DefaultAction() })(domainName)
                    List<Parameter> parameters = (defaults?.parameters?.clone() ?: []) + pathParams
                    if (pathParts[-1] != "{id}") {
                        // Special case: defaults may include 'id' for single resource paths
                        parameters.removeAll { it.name == 'id' }
                    }
                    [
                            mapping.actionName,
                            defineAction('/' + pathParts.join('/'), mapping.httpMethod, domainName,
                                    "${mapping.httpMethod.toLowerCase()}${controllerName}${mapping.actionName}",
                                    parameters, defaults?.responseMessages ?: [], "${mapping.actionName} ${domainName}"
                            )
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
        List<ApiOperation> apiOperationAnnotations = allAnnotations.
                findAll { it.annotationType() == ApiOperation } as List<ApiOperation>

        def modelTypes = (
                apiOperationAnnotations*.response() +
                        grailsApplication.domainClasses.
                                find { it.logicalPropertyName == theController.logicalPropertyName }?.
                                clazz
        ).grep()

        Map models = getModels(modelTypes)

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
                apis[action].operations[0].parameters.addAll(0, resourcePathParams)
            }
        }

        def updateDocWithoutUrlMappings = { String action, MethodDocumentation documentation ->
            if (apis.containsKey(action)) {
                apis[action].operations = (apis[action].operations + documentation.operations).unique()
            } else {
                apis[action] = documentation
            }
        }

        def updateDocumentation = apis ? updateDocFromUrlMappings : updateDocWithoutUrlMappings

        // Update APIs with low-level method annotations
        apiMethods.each { method ->
            documentMethod(method, theController).each {
                updateDocumentation(method.name, it)
            }
        }

        // Update APIs with swaggydoc method annotations
        DefaultActionComponents.
                each { action, defaultsFactory ->
                    def defaults = defaultsFactory(domainName)
                    methodsOfType(defaults.swaggyAnnotation, theControllerClazz).
                            collectMany { method ->
                                generateMethod(action, method, theController).collect {
                                    updateDocumentation(method.name, it)
                                }
                            }
                }

        def groupedApis = apis.values().
                groupBy { MethodDocumentation it -> it.path }.
                collect { path, methodDocs -> new MethodDocumentation(path, methodDocs*.operations.flatten().unique()) }

        return [
                apiVersion    : apiVersion,
                swaggerVersion: SwaggerVersion,
                basePath      : api?.basePath() ?: absoluteBasePath,
                resourcePath  : resourcePath - basePath,
                produces      : api?.produces()?.tokenize(',') ?: responseContentTypes(theControllerClazz),
                consumes      : api?.consumes()?.tokenize(',') ?: DefaultRequestContentTypes,
                apis          : groupedApis,
                models        : models
        ]
    }

    private static List populatePaths(UrlMapping mapping) {
        List pathParams = []
        List pathParts = []
        def constraintIdx = 0
        mapping.urlData.tokens.eachWithIndex { String token, idx ->
            if (token.matches(/^\(.*[\*\+]+.*\)$/)) {
                def param = (idx == mapping.urlData.tokens.size() - 1) ? 'id' : mapping.constraints[constraintIdx]?.propertyName
                constraintIdx++
                if (param != 'id')
                // Don't push 'id' as it is one of the default pathParams
                    pathParams.push(makePathParam(param))
                pathParts.push("{" + param + "}")
            } else {
                pathParts.push(token)
            }
        }
        [pathParts, pathParams]
    }

    /**
     * Converts a controller to an api declaration
     * @param controller
     */
    @SuppressWarnings("GrMethodMayBeStatic")
    private Map controllerToApi(GrailsClass controller) {
        def name = controller.logicalPropertyName
        [
                path       : grailsLinkGenerator.link(controller: 'api', action: 'show', id: name, absolute: true),
                description: getApi(controller).description() ?: controller.naturalName
        ]
    }

    /**
     * Provides an Info Object for Swagger
     * @return
     */
    private Map<String, Object> getInfoObject() {
        [
                contact          : config.contact,
                description      : config.description,
                license          : config.license,
                licenseUrl       : config.licenseUrl,
                termsOfServiceUrl: config.termsOfServiceUrl,
                title            : config.title
        ].findAll { k, v ->
            v
        }
    }

    /**
     * Obtains an Api Annotation from a controller
     *
     * @param controller
     * @return
     */
    private static Api getApi(GrailsClass controller) {
        controller.clazz.annotations.find { it.annotationType() == Api } as Api
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
        theControllerClazz.methods.findAll { findAnnotation(annotation, it) } as List<Method>
    }

    /**
     * Provides optional support for the "marshallers" grails plugin when building models from domains
     */
    private Map _marshallingConfig

    private def getMarshallingConfigForDomain(String domainName) {
        if (_marshallingConfig == null) {
            _marshallingConfig = [:]
            if (Holders.pluginManager.hasGrailsPlugin('marshallers')) {
                def MarshallingConfigBuilder = grailsApplication.getClassLoader().loadClass("org.grails.plugins.marshallers.config.MarshallingConfigBuilder")
                grailsApplication.domainClasses.each {
                    def clazz = it.clazz
                    Closure mc = GCU.getStaticPropertyValue(clazz, 'marshalling') as Closure
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

    private Map getModels(Collection<Class<?>> modelTypes) {
        Queue m = modelTypes as Queue
        def models = [:]
        while (m.size()) {
            Class model = m.poll()
            log.debug "Getting model for class ${model}"
            def domainClass = grailsApplication.domainClasses.find { it.clazz == model } as GrailsDomainClass
            /** Duck typing here:
             * if model has a GrailsDomainClass then props will be list of GrailsDomainClassProperty objects
             * otherwise props will be a list of Field objects
             * Interface for these two classes are similar enough to duck type for our purposes
             */
            def fieldSource = domainClass ?
                    [domainClass.identifier, domainClass.version] + domainClass.persistentProperties.toList()
                    : model.declaredFields

            def props = fieldSource.
                    findAll {
                        !it.toString().contains(' static ') &&
                                !it.toString().contains(' transient ') &&
                                it.name != 'errors'
                    }

            def optional = domainClass?.constrainedProperties?.findAll { k, v -> v.isNullable() }

            if (domainClass) {
                // Check for marshalling config
                def marshallingConfig = getMarshallingConfigForDomain(domainClass.fullName)
                if (marshallingConfig) {
//                    Set deepProps = [] as Set
                    def addProp = { fn ->
                        props.add([name: fn, type: String])
                        optional[fn] = true
                    }
                    Closure processMarshallingConfig;
                    processMarshallingConfig = { config ->
                        if (config.name != "default")
                        // Currently we only support default marshalling, as that is conventional pattern
                            return
                        // N.B. we are not checking config.type, so we will conflate json and xml together
                        // adding or suppressing fields for only one response type is an anti-pattern anyway
                        if (!config.shouldOutputIdentifier) {
                            props.remove(domainClass.identifier)
                        }
                        if (!config.shouldOutputVersion) {
                            props.remove(domainClass.version)
                        }
                        if (config.shouldOutputClass) {
                            addProp("class")
                        }
                        config.ignore?.each { fn ->
                            props.removeAll { it.name == fn }
                        }
                        config.virtual?.keySet()?.each(addProp)
//                        deepProps.addAll(config.deep ?: [])
                        config.children?.each(processMarshallingConfig)
                    }
                    processMarshallingConfig(marshallingConfig)
                    // FIXME: Handle "deep" the way marshallers does (may be a bit tricky)
//                    props.findAll { f ->
//                        !(deepProps.contains(f.name) || knownTypes.contains(f.type))
//                    }.each { f ->
//                    }
                }
            }

            def required = props.collect { f -> f.name } - optional*.key

            def modelDescription = [
                    id        : model.simpleName,
                    required  : required,
                    properties: props.collectEntries { f -> [f.name, getTypeDescriptor(f, domainClass)] }
            ]

            models[model.simpleName] = modelDescription
            props.each { f ->
                if (!models.containsKey(f.type.simpleName) && !m.contains(f.type) && !knownTypes.contains(f.type)) {
                    if (f.type.isAssignableFrom(List) || f.type.isAssignableFrom(Set)) {
                        def typeArgs = domainClass?.associationMap?.getAt(f.name) ?: f.genericType.actualTypeArguments[0]
                        m.add(typeArgs)
                    } else {
                        m.add(f.type)
                    }

                }
            }
        }
        models
    }

    private List<MethodDocumentation> generateMethod(String action, Method method, GrailsClass theController) {
        def basePath = grailsLinkGenerator.link(uri: '')
        def slug = theController.logicalPropertyName
        def domainName = slugToDomain(slug)
        DefaultAction defaults = DefaultActionComponents[action](domainName)
        List<Parameter> parameters = defaults.parameters.clone() as List<Parameter>
        if (defaults.swaggyAnnotation.metaClass.getMetaMethod('searchParam')
                && findAnnotation(defaults.swaggyAnnotation, method).searchParam()) {
            parameters << new Parameter('q', 'Query. Follows Lucene Query Syntax.', 'query', 'string')
        }
        def pathParams = parameters.
                findAll { it.paramType == 'path' }.
                collect { it.name }.
                collectEntries {
                    [it, "{${it}}"]
                }
        def fullLink = grailsLinkGenerator.link(controller: slug, action: method.name, params: pathParams) as String
        def link = fullLink.replace('%7B', '{').replace('%7D', '}') - basePath
        def httpMethods = getHttpMethod(theController, method)
        httpMethods.collect { httpMethod ->
            def inferredNickname = "${httpMethod.toLowerCase()}${slug}${method.name}"
            defineAction(link, httpMethod, domainName, inferredNickname, parameters, defaults.responseMessages, "${action} ${domainName}")
        }
    }

    private static MethodDocumentation defineAction(
            String link, String httpMethod, String domainName, String inferredNickname,
            List<Parameter> parameters, List<ResponseMessage> responseMessages, String summary) {
        new MethodDocumentation(
                link,
                [
                        new Operation(
                                method: httpMethod,
                                summary: summary,
                                nickname: inferredNickname,
                                parameters: parameters,
                                type: domainName,
                                responseMessages: responseMessages,
                        )
                ]
        )
    }

    private List<MethodDocumentation> documentMethod(Method method, GrailsClass theController) {
        def basePath = grailsLinkGenerator.link(uri: '')
        def apiOperation = findAnnotation(ApiOperation, method)
        def apiResponses = findAnnotation(ApiResponses, method)
        def apiParams = findAnnotation(ApiImplicitParams, method)?.value() ?: []

        def pathParasAnnotations = apiParams.findAll { it.paramType() == 'path' } as List<ApiImplicitParam>
        def pathParams = pathParasAnnotations*.name().collectEntries { [it, "{${it}}"] }

        def slug = theController.logicalPropertyName

        def fullLink = grailsLinkGenerator.link(controller: slug, action: method.name, params: pathParams) as String
        def link = fullLink.replace('%7B', '{').replace('%7D', '}') - basePath
        def httpMethods = getHttpMethod(theController, method)
        httpMethods.collect { httpMethod ->
            List<Parameter> parameters = apiParams?.collect { new Parameter(it as ApiImplicitParam) } ?: []
            def inferredNickname = "${httpMethod.toLowerCase()}${slug}${method.name}"

            new MethodDocumentation(link,
                    [
                            new Operation(
                                    method: httpMethod,
                                    summary: apiOperation.value(),
                                    notes: apiOperation.notes(),
                                    nickname: apiOperation.nickname() ?: inferredNickname,
                                    parameters: parameters,
                                    type: apiOperation.response() == Void ? 'void' : apiOperation.response().simpleName,
                                    responseMessages: apiResponses?.value()?.collect { new ResponseMessage(it) }
                            )
                    ]
            )
        }

    }

    private static List<String> getHttpMethod(GrailsClass theController, Method method) {
        try {
            def retval = theController.referenceInstance.allowedMethods[method.name] ?: 'GET'
            if (retval instanceof String) {
                [retval]
            } else if (retval instanceof Collection<String>) {
                retval.toList()
            } else {
                ['GET']
            }
        } catch (Exception ignored) {
            ['GET']
        }
    }

    /**
     * Gets the type descriptor for a field in a domain class
     * @param f
     * @return
     */
    private static Map getTypeDescriptor(def f, GrailsDomainClass gdc) {
        if (f.type.isAssignableFrom(String)) {
            [type: 'string']
        } else if (f.type.isAssignableFrom(Double) || f.type.isAssignableFrom(Float)) {
            [type: 'number', format: 'double']
        } else if (f.type.isAssignableFrom(Long) || f.type.isAssignableFrom(Integer)) {
            [type: 'integer', format: 'int64']
        } else if (f.type.isAssignableFrom(Date)) {
            [type: 'string', format: 'date-time']
        } else if (f.type.isAssignableFrom(Boolean)) {
            [type: 'boolean']
        } else if (f.type.isAssignableFrom(Set) || f.type.isAssignableFrom(List)) {
            def genericType = null
            if(f instanceof GrailsDomainClassProperty) {
                genericType = gdc?.associationMap?.getAt(f.name)
                assert genericType, "Unknown type for property ${f.name}, please specify it in the domain's class hasMany"
            }
            genericType = genericType ?: f.genericType.actualTypeArguments[0]
            def clazzName = genericType.simpleName
            [
                    type : 'array',
                    items: ['$ref': clazzName]
            ]
        } else {
            ['$ref': f.type.simpleName]
        }

    }

    private static String slugToDomain(String slug) {
        slug.with { it.replaceFirst(it[0], it[0].toUpperCase()) }
    }

    private List<String> responseContentTypes(Class controller) {
        GCU.getStaticPropertyValue(controller, 'responseFormats')?.
                collect { String it ->
                    grailsMimeUtility.getMimeTypeForExtension(it)?.name ?: it.contains('/') ? it : null
                }?.
                grep() as List<String> ?: DefaultResponseContentTypes
    }
}
