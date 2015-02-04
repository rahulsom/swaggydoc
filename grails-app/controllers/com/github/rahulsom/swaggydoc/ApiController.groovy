package com.github.rahulsom.swaggydoc

import com.wordnik.swagger.annotations.*
import grails.converters.JSON
import org.codehaus.groovy.grails.commons.GrailsClass
import org.codehaus.groovy.grails.commons.GrailsDomainClass

import java.lang.reflect.AccessibleObject
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Type

class ApiController {

    /**
     * Empty Method. Needed for rendering GSP as HTML.
     */
    def index() {
    }

    /**
     * Empty Method. Needed for rendering GSP as HTML.
     */
    def images() {
        response.sendRedirect(g.resource(dir: 'images', file: 'throbber.gif').toString())
    }

    /**
     * Renders the Swagger Resources.
     * @return
     */
    def resources() {
        def apis = grailsApplication.controllerClasses.
                findAll { getApi(it) }.
                sort { getApi(it).position() }.
                collect { controllerToApi(it) }

        ConfigObject config = grailsApplication.config.swaggydoc

        render([
                apiVersion    : config.apiVersion ?: grailsApplication.metadata['app.version'],
                swaggerVersion: '1.2',
                apis          : apis?.toArray(),
                info          : infoObject
        ] as JSON)
    }

    /**
     * Converts a controller to an api declaration
     * @param controller
     */
    @SuppressWarnings("GrMethodMayBeStatic")
    private Map controllerToApi(GrailsClass controller) {
        def name = controller.logicalPropertyName
        [
                path       : g.createLink(controller: 'api', action: 'show', id: name, absolute: true),
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

    private List<Map> getSwaggyApis(GrailsClass theController) {
        def theControllerClazz = theController.referenceInstance.class

        def listMethods = methodsOfType(SwaggyList, theControllerClazz)
        def showMethods = methodsOfType(SwaggyShow, theControllerClazz)
        def saveMethods = methodsOfType(SwaggySave, theControllerClazz)
        def updateMethods = methodsOfType(SwaggyUpdate, theControllerClazz)
        def deleteMethods = methodsOfType(SwaggyDelete, theControllerClazz)
        def patchMethods = methodsOfType(SwaggyPatch, theControllerClazz)

        listMethods.collect { generateListMethod(it, theController) } +
                showMethods.collect { generateShowMethod(it, theController) } +
                saveMethods.collect { generateSaveMethod(it, theController) } +
                updateMethods.collect { generateUpdateMethod(it, theController) } +
                patchMethods.collect { generatePatchMethod(it, theController) } +
                deleteMethods.collect { generateDeleteMethod(it, theController) }
    }

    def show() {
        header 'Access-Control-Allow-Origin', '*'
        ConfigObject config = grailsApplication.config.swaggydoc
        def theController = grailsApplication.controllerClasses.find {
            it.logicalPropertyName == params.id
        }

        Api api = getApi(theController)

        def absoluteBasePath = g.createLink(uri: '', absolute: true)
        def basePath = g.createLink(uri: '')
        def resourcePath = g.createLink(controller: theController.logicalPropertyName)

        def theControllerClazz = theController.referenceInstance.class

        def apiMethods = methodsOfType(ApiOperation, theControllerClazz)

        def allAnnotations = apiMethods*.annotations.flatten()
        List<ApiOperation> apiOperationAnnotations = allAnnotations.findAll {
            it.annotationType() == ApiOperation
        } as List<ApiOperation>
        def modelTypes = apiOperationAnnotations*.response() + grailsApplication.domainClasses.find {
            it.logicalPropertyName == theController.logicalPropertyName
        }?.clazz

        def apis = apiMethods.collect { documentMethod(it, theController) } + getSwaggyApis(theController)

        Map models = getModels(modelTypes)

        def groupedApis = apis.
                groupBy { Map it -> it.path }.
                collect { p, a -> [path: p, operations: (a as List<Map>).collect { it.operations }.flatten().unique()?.toArray()] }

        render([
                apiVersion    : config.apiVersion ?: grailsApplication.metadata['app.version'],
                swaggerVersion: '1.2',
                basePath      : api.basePath() ?: absoluteBasePath,
                resourcePath  : resourcePath - basePath,
                produces      : (api.produces()?.tokenize(',') ?: ['application/json', 'application/xml', 'text/html']).toArray(),
                consumes      : (api.consumes()?.tokenize(',') ?: ['application/json', 'application/xml', 'application/x-www-form-urlencoded']).toArray(),
                apis          : groupedApis?.toArray(),
                models        : models,

        ] as JSON)
    }

    private Map getModels(Collection<Class<?>> modelTypes) {
        Queue m = modelTypes.findAll { it } as Queue
        def models = [:]
        while (m.size()) {
            Class model = m.poll()
            def props = model.declaredFields.findAll {
                !it.toString().contains(' static ') &&
                        !it.toString().contains(' transient ') &&
                        it.name != 'errors'
            }

            def grailsDomainClass = grailsApplication.domainClasses.find { it.clazz == model } as GrailsDomainClass
            def optional = grailsDomainClass?.constrainedProperties?.findAll { k, v -> v.isNullable() }
            def required = props.collect { Field f -> f.name } - optional*.key

            def modelDescription = [
                    id        : model.simpleName,
                    required  : required?.toArray(),
                    properties: props.collectEntries { Field f -> [f.name, getTypeDescriptor(f, grailsDomainClass)] }
            ]

            models[model.simpleName] = modelDescription
            def knownTypes = [int, Integer, long, Long, float, Float, double, Double, String]
            props.each { Field f ->
                if (!models.containsKey(f.type.simpleName) && !m.contains(f.type) && !knownTypes.contains(f.type)) {
                    if (f.type.isAssignableFrom(List) || f.type.isAssignableFrom(Set)) {
                        def typeArgs = grailsDomainClass.associationMap[f.name] ?: f.genericType.actualTypeArguments[0]
                        m.add(typeArgs)
                    } else {
                        m.add(f.type)
                    }

                }
            }
        }
        models
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    private Map generateListMethod(Method method, GrailsClass theController) {
        def basePath = g.createLink(uri: '')
        def swaggyList = findAnnotation(SwaggyList, method)
        def slug = theController.logicalPropertyName

        def parameters = [
                [name: 'offset', description: 'Records to skip. Empty means 0.', paramType: 'query', type: 'int'],
                [name: 'max', description: 'Max records to return. Empty means 10.', paramType: 'query', type: 'int'],
                [name: 'sort', description: 'Field to sort by. Empty means id if q is empty. If q is provided, empty means relevance.', paramType: 'query', type: 'string'],
                [name: 'order', description: 'Order to sort by. Empty means asc if q is empty. If q is provided, empty means desc.', paramType: 'query', type: 'string'],
        ]
        if (swaggyList.searchParam()) {
            parameters << [name: 'q', description: 'Query. Follows Lucene Query Syntax.', paramType: 'query', type: 'string']
        }
        def pathParams = parameters.findAll { it.paramType == 'path' }.collect { it.name }.collectEntries {
            [it, "{${it}}"]
        }
        def fullLink = g.createLink(controller: slug, action: method.name, params: pathParams) as String

        def link = fullLink.replace('%7B', '{').replace('%7D', '}') - basePath
        def httpMethod = getHttpMethod(theController, method)
        def domainName = slugToDomain(slug)
        def inferredNickname = "${httpMethod.toLowerCase()}${slug}${method.name}"

        defineMethod(link, httpMethod, domainName, inferredNickname, parameters, [], "List ${domainName}s")
    }

    private static String slugToDomain(String slug) {
        slug.with { it.replaceFirst(it[0], it[0].toUpperCase()) }
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    private Map generateShowMethod(Method method, GrailsClass theController) {
        def basePath = g.createLink(uri: '')
        def swaggyShow = findAnnotation(SwaggyShow, method)
        def slug = theController.logicalPropertyName

        def parameters = [
                [name: 'id', description: 'Identifier to look for', paramType: 'path', type: 'string', required: true],
        ]
        def pathParams = parameters.findAll { it.paramType == 'path' }.collect { it.name }.collectEntries {
            [it, "{${it}}"]
        }

        def fullLink = g.createLink(controller: slug, action: method.name, params: pathParams) as String
        def link = fullLink.replace('%7B', '{').replace('%7D', '}') - basePath
        def httpMethod = getHttpMethod(theController, method)
        def domainName = slugToDomain(slug)
        def inferredNickname = "${httpMethod.toLowerCase()}${slug}${method.name}"
        def responseMessages = [
                [code: 400, message: 'Bad Id provided'],
                [code: 404, message: "Could not find ${domainName} with that Id"],
        ]

        defineMethod(link, httpMethod, domainName, inferredNickname, parameters, responseMessages, "Show ${domainName}")
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    private Map generateSaveMethod(Method method, GrailsClass theController) {
        def basePath = g.createLink(uri: '')
        def swaggySave = findAnnotation(SwaggySave, method)
        def slug = theController.logicalPropertyName
        def domainName = slugToDomain(slug)

        def parameters = [
                [name: 'body', description: "Description of ${domainName}", paramType: 'body', type: domainName, required: true],
        ]
        def pathParams = parameters.findAll { it.paramType == 'path' }.collect { it.name }.collectEntries {
            [it, "{${it}}"]
        }

        def fullLink = g.createLink(controller: slug, action: method.name, params: pathParams) as String
        def link = fullLink.replace('%7B', '{').replace('%7D', '}') - basePath
        def httpMethod = getHttpMethod(theController, method)
        def inferredNickname = "${httpMethod.toLowerCase()}${slug}${method.name}"
        def responseMessages = [
                [code: 422, message: 'Bad Entity received'],
        ]

        defineMethod(link, httpMethod, domainName, inferredNickname, parameters, responseMessages, "Save ${domainName}")
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    private Map generateUpdateMethod(Method method, GrailsClass theController) {
        def basePath = g.createLink(uri: '')
        def swaggySave = findAnnotation(SwaggySave, method)
        def slug = theController.logicalPropertyName
        def domainName = slugToDomain(slug)

        def parameters = [
                [name: 'id', description: "Id to update", paramType: 'path', type: 'string', required: true],
                [name: 'body', description: "Description of ${domainName}", paramType: 'body', type: domainName, required: true],
        ]
        def pathParams = parameters.findAll { it.paramType == 'path' }.collect { it.name }.collectEntries {
            [it, "{${it}}"]
        }

        def fullLink = g.createLink(controller: slug, action: method.name, params: pathParams) as String
        def link = fullLink.replace('%7B', '{').replace('%7D', '}') - basePath
        def httpMethod = getHttpMethod(theController, method)
        def inferredNickname = "${httpMethod.toLowerCase()}${slug}${method.name}"
        def responseMessages = [
                [code: 400, message: 'Bad Id provided'],
                [code: 404, message: "Could not find ${domainName} with that Id"],
                [code: 422, message: 'Bad Entity received'],
        ]

        defineMethod(link, httpMethod, domainName, inferredNickname, parameters, responseMessages, "Save ${domainName}")
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    private Map generatePatchMethod(Method method, GrailsClass theController) {
        def basePath = g.createLink(uri: '')
        def swaggySave = findAnnotation(SwaggyPatch, method)
        def slug = theController.logicalPropertyName
        def domainName = slugToDomain(slug)

        def parameters = [
                [name: 'id', description: "Id to patch", paramType: 'path', type: 'string', required: true],
                [name: 'body', description: "Description of ${domainName}", paramType: 'body', type: domainName, required: true],
        ]
        def pathParams = parameters.findAll { it.paramType == 'path' }.collect { it.name }.collectEntries {
            [it, "{${it}}"]
        }

        def fullLink = g.createLink(controller: slug, action: method.name, params: pathParams) as String
        def link = fullLink.replace('%7B', '{').replace('%7D', '}') - basePath
        def httpMethod = getHttpMethod(theController, method)
        def inferredNickname = "${httpMethod.toLowerCase()}${slug}${method.name}"
        def responseMessages = [
                [code: 400, message: 'Bad Id provided'],
                [code: 404, message: "Could not find ${domainName} with that Id"],
                [code: 422, message: 'Bad Entity received'],
        ]

        defineMethod(link, httpMethod, domainName, inferredNickname, parameters, responseMessages, "Save ${domainName}")
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    private Map generateDeleteMethod(Method method, GrailsClass theController) {
        def basePath = g.createLink(uri: '')
        def swaggySave = findAnnotation(SwaggyDelete, method)
        def slug = theController.logicalPropertyName
        def domainName = slugToDomain(slug)

        def parameters = [
                [name: 'id', description: "Id to delete", paramType: 'path', type: 'string', required: true],
        ]
        def pathParams = parameters.findAll { it.paramType == 'path' }.collect { it.name }.collectEntries {
            [it, "{${it}}"]
        }

        def fullLink = g.createLink(controller: slug, action: method.name, params: pathParams) as String
        def link = fullLink.replace('%7B', '{').replace('%7D', '}') - basePath
        def httpMethod = getHttpMethod(theController, method)
        def inferredNickname = "${httpMethod.toLowerCase()}${slug}${method.name}"
        def responseMessages = [
                [code: 400, message: 'Bad Id provided'],
                [code: 404, message: "Could not find ${domainName} with that Id"],
        ]

        defineMethod(link, httpMethod, 'void', inferredNickname, parameters, responseMessages, "Delete ${domainName}")
    }

    private static LinkedHashMap<String, Serializable> defineMethod(
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
                                parameters      : parameters?.toArray(),
                                type            : domainName,
                                responseMessages: responseMessages?.toArray()
                        ]
                ].toArray()
        ]
    }

    private Map documentMethod(Method method, GrailsClass theController) {
        def basePath = g.createLink(uri: '')
        def apiOperation = findAnnotation(ApiOperation, method)
        def apiResponses = findAnnotation(ApiResponses, method)
        def apiParams = findAnnotation(ApiImplicitParams, method)?.value() ?: []

        def pathParasAnnotations = apiParams.findAll { it.paramType() == 'path' } as List<ApiImplicitParam>
        def pathParams = pathParasAnnotations*.name().collectEntries { [it, "{${it}}"] }

        def slug = theController.logicalPropertyName

        def fullLink = g.createLink(controller: slug, action: method.name, params: pathParams) as String
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
                                parameters      : parameters?.toArray(),
                                type            : apiOperation.response() == Void ? 'void' : apiOperation.response().simpleName,
                                responseMessages: apiResponses?.value()?.collect { ApiResponse apiResponse ->
                                    [code: apiResponse.code(), message: apiResponse.message()]
                                }?.toArray()
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
    private static Map getTypeDescriptor(Field f, GrailsDomainClass gdc) {
        if (f.type.isAssignableFrom(String)) {
            [type: 'string']
        } else if (f.type.isAssignableFrom(Double)) {
            [type: 'number', format: 'double']
        } else if (f.type.isAssignableFrom(Long)) {
            [type: 'integer', format: 'int64']
        } else if (f.type.isAssignableFrom(Date)) {
            [type: 'string', format: 'date-time']
        } else if (f.type.isAssignableFrom(Boolean)) {
            [type: 'boolean']
        } else if (f.type.isAssignableFrom(Set) || f.type.isAssignableFrom(List)) {
            def genericType = gdc.associationMap[f.name] ?:  f.genericType.actualTypeArguments[0]
            def clazzName = genericType.simpleName
            [
                    type : 'array',
                    items: ['$ref': clazzName]
            ]
        } else {
            ['$ref': f.type.simpleName]
        }

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
}
