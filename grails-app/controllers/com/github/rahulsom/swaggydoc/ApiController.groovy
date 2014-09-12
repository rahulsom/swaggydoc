package com.github.rahulsom.swaggydoc

import com.wordnik.swagger.annotations.*
import grails.converters.JSON
import org.codehaus.groovy.grails.commons.GrailsClass

import java.lang.reflect.AccessibleObject
import java.lang.reflect.Field
import java.lang.reflect.Method

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
                apis          : apis,
                info          : infoObject
        ] as JSON)
    }

    /**
     * Converts a controller to an api declaration
     * @param controller
     */
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
    private Api getApi(GrailsClass controller) {
        controller.clazz.annotations.find { it.annotationType() == Api }
    }

    /**
     * Finds an annotation of given type in an object
     *
     * @param clazz
     * @param object
     * @return
     */
    private <T> T findAnnotation(Class<T> clazz, AccessibleObject object) {
        object.annotations.find { it.annotationType() == clazz }
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
        List<Method> apiMethods = theControllerClazz.declaredMethods.
                findAll { findAnnotation(ApiOperation, it) } as List<Method>

        def allAnnotations = apiMethods*.annotations.flatten()
        List<ApiOperation> apiOperationAnnotations = allAnnotations.findAll {
            it.annotationType() == ApiOperation
        } as List<ApiOperation>
        def modelTypes = apiOperationAnnotations*.response()

        def apis = apiMethods.
                collect { documentMethod(it, theController) }.
                groupBy { Map it -> it.path }.
                collect { p, a -> [path: p, operations: (a as List<Map>).collect { it.operations }.flatten()] }

        def models = modelTypes.unique().collectEntries { Class model ->
            def props = model.declaredFields.findAll {
                !it.toString().contains(' static ') &&
                        !it.toString().contains(' transient ') &&
                        it.name != 'errors'
            }

            def modelDescription = [
                    id        : model.simpleName,
                    properties: props.collectEntries { Field f -> [f.name, getTypeDescriptor(f)] }
            ]
            [model.simpleName, modelDescription]
        }
        render([
                apiVersion    : config.apiVersion ?: grailsApplication.metadata['app.version'],
                swaggerVersion: '1.2',
                basePath      : api.basePath() ?: absoluteBasePath,
                resourcePath  : resourcePath - basePath,
                produces      : api.produces()?.split(',') ?: ['application/json'],
                consumes      : api.consumes()?.split(',') ?: ['application/json'],
                apis          : apis,
                models        : models,

        ] as JSON)
    }

    private Map documentMethod(Method method, GrailsClass theController) {
        def basePath = g.createLink(uri: '')
        def apiOperation = findAnnotation(ApiOperation, method)
        def apiResponses = findAnnotation(ApiResponses, method)
        def apiParams = findAnnotation(ApiImplicitParams, method).value()

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
                                parameters      : parameters,
                                type            : apiOperation.response() == Void ? 'void' : apiOperation.response().simpleName,
                                responseMessages: apiResponses?.value()?.collect { ApiResponse apiResponse ->
                                    [code: apiResponse.code(), message: apiResponse.message()]
                                }
                        ]
                ]
        ]
    }

    private String getHttpMethod(GrailsClass theController, Method method) {
        try {
            theController.referenceInstance.allowedMethods[method.name] ?: 'GET'
        } catch (Exception e) {
            'GET'
        }
    }

    /**
     * Gets the type descriptor for a field in a domain class
     * @param f
     * @return
     */
    private Map getTypeDescriptor(Field f) {
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
        } else {
            [type: f.type.simpleName]
        }

    }

    /**
     * Converts a param to a map for rendering
     *
     * @param apiParam
     * @return
     */
    private Map paramToMap(ApiImplicitParam apiParam) {
        [
                name       : apiParam.name(),
                description: apiParam.value(),
                required   : apiParam.required(),
                type       : apiParam.dataType() ?: (apiParam.paramType() == 'body' ? 'demo' : 'string'),
                paramType  : apiParam.paramType(),
        ]
    }
}
