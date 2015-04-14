package com.github.rahulsom.swaggydoc

import grails.util.Holders
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU

import static org.springframework.http.HttpStatus.*

import com.wordnik.swagger.annotations.*
import org.codehaus.groovy.grails.commons.GrailsClass
import org.codehaus.groovy.grails.commons.GrailsDomainClass

import java.lang.reflect.AccessibleObject
import java.lang.reflect.Method

class SwaggyDataService {

    def grailsApplication
    def grailsLinkGenerator
    def grailsUrlMappingsHolder
    def grailsMimeUtility

    static final ArrayList<String> DefaultResponseContentTypes = ['application/json', 'application/xml', 'text/html']
    static final ArrayList<String> DefaultRequestContentTypes = ['application/json', 'application/xml', 'application/x-www-form-urlencoded']
    static final List knownTypes = [int, Integer, long, Long, float, Float, double, Double, String]

    public static final Map<String, Map> DefaultActionComponents = [
        index: { domainName -> [
            swaggyAnnotation: SwaggyList,
            parameters: [
                [name: 'offset', description: 'Records to skip. Empty means 0.', paramType: 'query', type: 'int'],
                [name: 'max', description: 'Max records to return. Empty means 10.', paramType: 'query', type: 'int'],
                [name: 'sort', description: 'Field to sort by. Empty means id if q is empty. If q is provided, empty means relevance.', paramType: 'query', type: 'string'],
                [name: 'order', description: 'Order to sort by. Empty means asc if q is empty. If q is provided, empty means desc.', paramType: 'query', type: 'string'],
            ],
            responseMessages: [],
        ]},
        show: { domainName -> [
            swaggyAnnotation: SwaggyShow,
            parameters: [
                [name: 'id', description: 'Identifier to look for', paramType: 'path', type: 'string', required: true],
            ],
            responseMessages: [
                [code: BAD_REQUEST.value(), message: 'Bad Request'],
                [code: NOT_FOUND.value(), message: "Could not find ${domainName} with that Id"],
            ]
        ]},
        save: { domainName -> [
            swaggyAnnotation: SwaggySave,
            parameters: [
                [name: 'body', description: "Description of ${domainName}", paramType: 'body', type: domainName, required: true],
            ],
            responseMessages: [
                [code: CREATED.value(), message: "New ${domainName} created"],
                [code: UNPROCESSABLE_ENTITY.value(), message: 'Malformed Entity received'],
            ]
        ]},
        update: { domainName -> [
            swaggyAnnotation: SwaggyUpdate,
            parameters: [
                [name: 'id', description: "Id to update", paramType: 'path', type: 'string', required: true],
                [name: 'body', description: "Description of ${domainName}", paramType: 'body', type: domainName, required: true],
            ],
            responseMessages: [
                [code: BAD_REQUEST.value(), message: 'Bad Request'],
                [code: NOT_FOUND.value(), message: "Could not find ${domainName} with that Id"],
                [code: UNPROCESSABLE_ENTITY.value(), message: 'Malformed Entity received'],
            ]
        ]},
        patch: { domainName -> [
            swaggyAnnotation: SwaggyPatch,
            parameters: [
                [name: 'id', description: "Id to patch", paramType: 'path', type: 'string', required: true],
                [name: 'body', description: "Description of ${domainName}", paramType: 'body', type: domainName, required: true],
            ],
            responseMessages: [
                [code: BAD_REQUEST.value(), message: 'Bad Request'],
                [code: NOT_FOUND.value(), message: "Could not find ${domainName} with that Id"],
                [code: UNPROCESSABLE_ENTITY.value(), message: 'Malformed Entity received'],
            ]
        ]},
        delete: { domainName -> [
            swaggyAnnotation: SwaggyDelete,
            parameters: [
                [name: 'id', description: "Id to delete", paramType: 'path', type: 'string', required: true],
            ],
            responseMessages: [
                [code: NO_CONTENT.value(), message: 'Delete successful'],
                [code: BAD_REQUEST.value(), message: 'Bad Request'],
                [code: NOT_FOUND.value(), message: "Could not find ${domainName} with that Id"],
            ]
        ]}
    ]

    /**
     * Generates map of Swagger Resources.
     * @return Map
     */
    Map resources() {
        def apis = grailsApplication.controllerClasses.
                findAll { getApi(it) }.
                sort { getApi(it).position() }.
                collect { controllerToApi(it) }

        ConfigObject config = grailsApplication.config.swaggydoc

        return [
                apiVersion    : config.apiVersion ?: grailsApplication.metadata['app.version'],
                swaggerVersion: '1.2',
                apis          : apis,
                info          : infoObject
        ]
    }

    /**
     * Generates details about a particular API given a controller name
     * @param controllerName
     * @return Map
     */
    Map apiDetails(controllerName) {
        ConfigObject config = grailsApplication.config.swaggydoc
        def theController = grailsApplication.controllerClasses.find {
            it.logicalPropertyName == controllerName
        }
        def theControllerClazz = theController.referenceInstance.class

        Api api = getApi(theController)

        def absoluteBasePath = grailsLinkGenerator.link(uri: '', absolute: true)
        def basePath = grailsLinkGenerator.link(uri: '')
        def resourcePath = grailsLinkGenerator.link(controller: theController.logicalPropertyName)
        def domainName = slugToDomain(controllerName)

        def makePathParam = { pathParam ->
            [
                name: pathParam,
                description: pathParam + " identifier",
                paramType: "path",
                type: "string",
                required: true
            ]
        }

        // These preserve the path components supporting hierarchical paths discovered through URL mappings
        List resourcePathParts
        List resourcePathParams
        def apis = grailsUrlMappingsHolder.urlMappings.findAll {
                it.controllerName == controllerName
            }.
            collectEntries { mapping ->
                // Determine path and path arguments
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
                // Capture resource path candidates
                if (!resourcePathParts || resourcePathParts.size() > pathParts.size()) {
                    resourcePathParts = pathParts
                    resourcePathParams = pathParams
                }
                def defaults = (DefaultActionComponents.get(mapping.actionName) ?: {[:]})(domainName)
                def parameters = (defaults?.parameters?.clone() ?: []) + pathParams
                if (pathParts[-1] != "{id}") {
                    // Special case: defaults may include 'id' for single resource paths
                    parameters.removeAll { it.name == 'id' }
                }
                [
                    mapping.actionName,
                    defineAction(
                        '/' + pathParts.join('/'),
                        mapping.httpMethod,
                        domainName,
                        "${mapping.httpMethod.toLowerCase()}${controllerName}${mapping.actionName}",
                        parameters,
                        defaults?.responseMessages ?: [],
                        "${mapping.actionName} ${domainName}"
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
        List<ApiOperation> apiOperationAnnotations = allAnnotations.findAll {
            it.annotationType() == ApiOperation
        } as List<ApiOperation>
        def modelTypes = (apiOperationAnnotations*.response() + grailsApplication.domainClasses.find {
            it.logicalPropertyName == theController.logicalPropertyName
        }?.clazz).grep()
        Map models = getModels(modelTypes)

        def updateDocumentation = apis ?
            // This code is used if UrlMappings were used
            { action, documentation ->
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
            } :
            // This code is used if there were no matching UrlMappings
            { action, documentation ->
                apis[action] = documentation
            }
        // Update APIs with low-level method annotations
        apiMethods.each { method ->
            updateDocumentation(method.name, documentMethod(method, theController))
        }

        // Update APIs with swaggydoc method annotations
        DefaultActionComponents.collectEntries { action, defaultsFactory ->
            def defaults = defaultsFactory(domainName)
            methodsOfType(defaults.swaggyAnnotation, theControllerClazz).collectEntries {
                [ it.name, generateMethod(action, it, theController) ]
            }
        }.each(updateDocumentation)

        def groupedApis = apis.values().
                groupBy { Map it -> it.path }.
                collect { p, a -> [path: p, operations: (a as List<Map>).collect { it.operations }.flatten().unique()] }

        return [
                apiVersion    : config.apiVersion ?: grailsApplication.metadata['app.version'],
                swaggerVersion: '1.2',
                basePath      : api?.basePath() ?: absoluteBasePath,
                resourcePath  : resourcePath - basePath,
                produces      : api?.produces()?.tokenize(',') ?: responseContentTypes(theControllerClazz),
                consumes      : api?.consumes()?.tokenize(',') ?: DefaultRequestContentTypes,
                apis          : groupedApis,
                models        : models
        ]
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
        ConfigObject config = grailsApplication.config.swaggydoc
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
        controller.clazz.annotations.find { it.annotationType() == Api }
    }

    /**
     * Finds an annotation of given type in an object
     *
     * @param clazz
     * @param object
     * @return
     */
    private static <T> T findAnnotation(Class<T> clazz, AccessibleObject object) {
        object.annotations.find { it.annotationType() == clazz }
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
                    Closure mc = GCU.getStaticPropertyValue(clazz, 'marshalling')
                    if (mc) {
                        def builder = MarshallingConfigBuilder.newInstance(clazz)
                        mc.delegate = builder
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
            def domainClass = grailsApplication.domainClasses.find { it.clazz == model } as GrailsDomainClass
            /** Duck typing here:
             * if model has a GrailsDomainClass then props will be list of GrailsDomainClassProperty objects
             * otherwise props will be a list of Field objects
             * Interface for these two classes are similar enough to duck type for our purposes
             */
            def props = (domainClass ?
                [domainClass.identifier, domainClass.version] + domainClass.persistentProperties.toList()
                : model.declaredFields
            ).findAll {
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
                    def processMarshallingConfig;
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
                        config.virtual?.keySet().each(addProp)
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

    private Map generateMethod(String action, Method method, GrailsClass theController) {
        def basePath = grailsLinkGenerator.link(uri: '')
        def slug = theController.logicalPropertyName
        def domainName = slugToDomain(slug)
        def defaults = DefaultActionComponents[action](domainName)
        def parameters = defaults.parameters.clone()
        if (defaults.swaggyAnnotation.metaClass.getMetaMethod('searchParam')
                && findAnnotation(defaults.swaggyAnnotation, method).searchParam()) {
            parameters << [name: 'q', description: 'Query. Follows Lucene Query Syntax.', paramType: 'query', type: 'string']
        }
        def pathParams = parameters.findAll { it.paramType == 'path' }.collect { it.name }.collectEntries {
            [it, "{${it}}"]
        }
        def fullLink = grailsLinkGenerator.link(controller: slug, action: method.name, params: pathParams) as String
        def link = fullLink.replace('%7B', '{').replace('%7D', '}') - basePath
        def httpMethod = getHttpMethod(theController, method)
        def inferredNickname = "${httpMethod.toLowerCase()}${slug}${method.name}"
        defineAction(link, httpMethod, domainName, inferredNickname, parameters, defaults.responseMessages, "${action} ${domainName}")
    }

    private static LinkedHashMap<String, Serializable> defineAction(
            String link, String httpMethod, domainName, GString inferredNickname,
            ArrayList<LinkedHashMap<String, Serializable>> parameters,
            ArrayList<LinkedHashMap<String, Serializable>> responseMessages,
            String summary) {
        [
                path      : link,
                operations: [
                        [
                                method          : httpMethod,
                                summary         : summary,
                                nickname        : inferredNickname,
                                parameters      : parameters,
                                type            : domainName,
                                responseMessages: responseMessages,
                        ]
                ]
        ]
    }

    private Map documentMethod(Method method, GrailsClass theController) {
        def basePath = grailsLinkGenerator.link(uri: '')
        def apiOperation = findAnnotation(ApiOperation, method)
        def apiResponses = findAnnotation(ApiResponses, method)
        def apiParams = findAnnotation(ApiImplicitParams, method)?.value() ?: []

        def pathParasAnnotations = apiParams.findAll { it.paramType() == 'path' } as List<ApiImplicitParam>
        def pathParams = pathParasAnnotations*.name().collectEntries { [it, "{${it}}"] }

        def slug = theController.logicalPropertyName

        def fullLink = grailsLinkGenerator.link(controller: slug, action: method.name, params: pathParams) as String
        def link = fullLink.replace('%7B', '{').replace('%7D', '}') - basePath
        def httpMethod = getHttpMethod(theController, method)
        def parameters = apiParams?.collect { ApiImplicitParam it -> paramToMap(it) } ?: []
        def inferredNickname = "${httpMethod.toLowerCase()}${slug}${method.name}"

        [
                path      : link,
                operations: [
                        [
                                method          : httpMethod,
                                summary         : apiOperation.value(),
                                notes           : apiOperation.notes(),
                                nickname        : apiOperation.nickname() ?: inferredNickname,
                                parameters      : parameters,
                                type            : apiOperation.response() == Void ? 'void' : apiOperation.response().simpleName,
                                responseMessages: apiResponses?.value()?.collect { ApiResponse apiResponse ->
                                    [code: apiResponse.code(), message: apiResponse.message()]
                                }
                        ]
                ]
        ]
    }

    private static String getHttpMethod(GrailsClass theController, Method method) {
        try {
            theController.referenceInstance.allowedMethods[method.name] ?: 'GET'
        } catch (Exception ignored) {
            'GET'
        }
    }

    /**
     * Gets the type descriptor for a field in a domain class
     * @param f
     * @return
     */
    private static Map getTypeDescriptor(def f, GrailsDomainClass gdc) {
        Map descriptor = getConstraintsInformation(gdc, f.name)

        if (f.type.isAssignableFrom(String)) {
            descriptor << [type: 'string']
        } else if (f.type.isAssignableFrom(Double) || f.type.isAssignableFrom(Float)) {
            descriptor << [type: 'number', format: 'double']
        } else if (f.type.isAssignableFrom(Long) || f.type.isAssignableFrom(Integer)) {
            descriptor << [type: 'integer', format: 'int64']
        } else if (f.type.isAssignableFrom(Date)) {
            descriptor << [type: 'string', format: 'date-time']
        } else if (f.type.isAssignableFrom(Boolean)) {
            descriptor << [type: 'boolean']
        } else if (f.type.isAssignableFrom(Set) || f.type.isAssignableFrom(List)) {
            def genericType = gdc?.associationMap?.getAt(f.name) ?: f.genericType.actualTypeArguments[0]
            def clazzName = genericType.simpleName
            descriptor << [
                    type : 'array',
                    items: ['$ref': clazzName]
            ]
        } else {
            descriptor << ['$ref': f.type.simpleName]
        }

        return descriptor
    }

    private static Map getConstraintsInformation(GrailsDomainClass gdc, String propertyName){
        Map constraintsInfo = [:]
        def constraintName

        if(gdc && gdc.constraints && gdc.constraints[propertyName]){
            gdc.constraints[propertyName].appliedConstraints.each { constraint ->
                constraintName = Introspector.decapitalize(constraint.class.simpleName - "Constraint")
                constraintsInfo << ["$constraintName": constraint.constraintParameter]
            }
        }

        return constraintsInfo
    }

    private static String slugToDomain(String slug) {
        slug.with { it.replaceFirst(it[0], it[0].toUpperCase()) }
    }

    /**
     * Converts a param to a map for rendering
     *
     * @param apiParam
     * @return
     */
    private static Map paramToMap(ApiImplicitParam apiParam) {
        [
                name        : apiParam.name(),
                description : apiParam.value(),
                required    : apiParam.required(),
                type        : apiParam.dataType() ?: (apiParam.paramType() == 'body' ? 'demo' : 'string'),
                paramType   : apiParam.paramType(),
                defaultValue: apiParam.defaultValue()
        ]
    }

    private List<String> responseContentTypes(Class controller) {
        GCU.getStaticPropertyValue(controller, 'responseFormats')?.collect {
            grailsMimeUtility.getMimeTypeForExtension(it)?.name ?: it.contains('/') ? it : null
        }.grep() as List<String> ?: DefaultResponseContentTypes
    }
}
