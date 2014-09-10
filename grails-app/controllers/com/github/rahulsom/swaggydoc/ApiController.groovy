package com.github.rahulsom.swaggydoc

import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiImplicitParam
import com.wordnik.swagger.annotations.ApiImplicitParams
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiResponses
import grails.converters.JSON
import org.codehaus.groovy.grails.commons.GrailsClass

import java.lang.reflect.Field

class ApiController {

    def index() {
    }

    def resources() {
        def documentedControllers = grailsApplication.controllerClasses.findAll { controller ->
            getApi(controller)
        }.sort { controller ->
            getApi(controller).position()
        }

        def apis = documentedControllers.collect { controller ->
            def name = controller.logicalPropertyName
            [
                    path       : g.createLink(controller: 'api', action: 'show', id: name, absolute: true),
                    description: getApi(controller).description() ?: controller.naturalName
            ]
        }
        def config = grailsApplication.config.swaggydoc
        def info = [
                contact          : config.contact,
                description      : config.description,
                license          : config.license,
                licenseUrl       : config.licenseUrl,
                termsOfServiceUrl: config.termsOfServiceUrl,
                title            : config.title
        ].findAll { k, v ->
            v
        }

        header 'Access-Control-Allow-Origin', '*'

        render([
                apiVersion    : config.apiVersion ?: grailsApplication.metadata['app.version'],
                swaggerVersion: '1.2',
                apis          : apis,
                info          : info
        ] as JSON)
    }

    private static Api getApi(GrailsClass controller) {
        controller.clazz.annotations.find { ann ->
            ann.annotationType() == Api
        }
    }

    def show() {
        header 'Access-Control-Allow-Origin', '*'
        def config = grailsApplication.config.swaggydoc
        def theController = grailsApplication.controllerClasses.find {
            it.logicalPropertyName == params.id
        }

        Api api = getApi(theController)

        def absoluteBasePath = g.createLink(uri: '', absolute: true)
        def basePath = g.createLink(uri: '')
        def resourcePath = g.createLink(controller: theController.logicalPropertyName)

        def apiMethods = theController.referenceInstance.class.declaredMethods.findAll { method ->
            method.annotations.find { ann -> ann.annotationType() == ApiOperation }
        }

        def modelTypes = (apiMethods*.annotations.flatten().findAll {
            ann -> ann.annotationType() == ApiOperation
        } as List<ApiOperation>)*.response()

        def apis = apiMethods.collect { method ->
            def apiParams =  (method.annotations.find {
                it.annotationType() == ApiImplicitParams
            } as ApiImplicitParams).value()
            def pathParams = apiParams.findAll { Api
                it.paramType() == 'path'
            }.collectEntries {
                [it.name(), "{${it.name()}}"]
            }
            def link = g.createLink(controller: theController.logicalPropertyName, action: method.name,
                    params: pathParams).replace('%7B','{').replace('%7D', '}') - basePath
            String httpMethod = theController.referenceInstance.allowedMethods[method.name] ?: 'GET'
            ApiOperation apiOperation = method.annotations.find {
                it.annotationType() == ApiOperation
            } as ApiOperation
            ApiResponses apiResponses = method.annotations.find {
                it.annotationType() == ApiResponses
            } as ApiResponses
            def parameters = apiParams?.collect { apiParam ->
                paramToMap(apiParam)
            } ?: []
            def inferredNickname = "${httpMethod.toLowerCase()}${theController.logicalPropertyName}${method.name}"
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
                                responseMessages: apiResponses?.value()?.collect { apiResponse ->
                                    [code: apiResponse.code(), message: apiResponse.message()]
                                }
                        ]
                    ]
            ]
        }.groupBy {it.path}.collect {p, a ->
          [path: p, operations: a*.operations.flatten() ]
        }

        def models = modelTypes.unique().collectEntries { model ->
            def props = model.declaredFields.findAll {
                !it.toString().contains(' static ') && !it.toString().contains(' transient ') && it.name != 'errors'
            }

            def modelDescription = [
                    id : model.simpleName,
                    properties: props.collectEntries { Field f ->
                        [f.name, getTypeDescriptor(f)]
                    }
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

    private LinkedHashMap<String, Serializable> paramToMap(ApiImplicitParam apiParam) {
        [
                name       : apiParam.name(),
                description: apiParam.value(),
                required   : apiParam.required(),
                type       : apiParam.dataType() ?: (apiParam.paramType() == 'body' ? 'demo' : 'string'),
                paramType  : apiParam.paramType(),
        ]
    }
}
