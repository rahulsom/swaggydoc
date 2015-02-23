package com.github.rahulsom.swaggydoc

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.web.mapping.DefaultUrlMappingEvaluator
import org.codehaus.groovy.grails.web.mapping.DefaultUrlMappingsHolder
import org.springframework.web.context.WebApplicationContext
import spock.lang.Specification

class MappedUrlsApiSpec extends Specification {

    def grailsApplication = new DefaultGrailsApplication()
    final DefaultUrlMappingEvaluator mappingEvaluator = new DefaultUrlMappingEvaluator((WebApplicationContext) grailsApplication.mainContext);
    def controller
    def service

    def setup() {
        controller = new ApiController()
        service = controller.swaggyDataService
    }

    def cleanup() {
    }

    def withUrlMappings(Closure mappings) {
        service.grailsUrlMappingsHolder = new DefaultUrlMappingsHolder(mappingEvaluator.evaluateMappings(mappings))
    }

    void "mapped resource has correct path and operations"() {
        given:
        withUrlMappings {
            "/altPath"(resources: "mapped", includes: ['index', 'show', 'delete', 'save'])
        }

        when:
        controller.params.id = 'mapped'
        controller.show()

        then:
        def json = controller.response.json
        json
        json.apiVersion == '1.0'
        json.swaggerVersion == '1.2'
        json.basePath == "http://localhost"
        json.resourcePath == "/altPath"
        json.produces == ['application/json', 'text/xml']
        json.consumes == ['application/json', 'application/xml', 'application/x-www-form-urlencoded']
        json.apis.size() == 2
        json.apis.find {it.path == '/altPath'}.operations.size() == 2
        json.apis.find {it.path == '/altPath'}.operations.find { it.method == 'GET' }.parameters.collect { it.name }.toSet() == ['offset', 'max', 'sort', 'order'] as Set
        json.apis.find {it.path == '/altPath'}.operations.find { it.method == 'POST' }.parameters.collect { it.name }.toSet() == ['body'] as Set
        json.apis.find {it.path == '/altPath/{id}'}.operations.size() == 2
        json.apis.find {it.path == '/altPath/{id}'}.operations.find { it.method == 'GET' }.parameters.collect { it.name }.toSet() == ['id'] as Set
        json.apis.find {it.path == '/altPath/{id}'}.operations.find { it.method == 'DELETE' }.parameters.collect { it.name }.toSet() == ['id'] as Set
    }

    void "mapped resource with Controller annotations overriding default behavior"() {
        given:
        withUrlMappings {
            "/somePath"(resources: "mappedWithAnnotations", includes: ['index', 'show', 'delete', 'save', 'patch'])
        }

        when:
        controller.params.id = 'mappedWithAnnotations'
        controller.show()

        then:
        def json = controller.response.json
        json
        json.apiVersion == '1.0'
        json.swaggerVersion == '1.2'
        json.basePath == "http://localhost"
        json.resourcePath == "/somePath"
        json.produces == ['application/json', 'text/xml']
        json.consumes == ['application/json', 'application/xml', 'application/x-www-form-urlencoded']
        json.apis.size() == 2
        json.apis.find {it.path == '/somePath'}.operations.size() == 2
        json.apis.find {it.path == '/somePath'}.operations.find { it.method == 'GET' }.parameters.collect { it.name }.toSet() == ['offset', 'max', 'sort', 'order'] as Set
        json.apis.find {it.path == '/somePath'}.operations.find { it.method == 'POST' }.parameters.collect { it.name }.toSet() == ['body'] as Set
        json.apis.find {it.path == '/somePath/{id}'}.operations.size() == 4
        json.apis.find {it.path == '/somePath/{id}'}.operations.find { it.method == 'GET' }.parameters.collect { it.name }.toSet() == ['id'] as Set
        json.apis.find {it.path == '/somePath/{id}'}.operations.find { it.method == 'PUT' }.parameters.collect { it.name }.toSet() == ['id', 'body'] as Set
        json.apis.find {it.path == '/somePath/{id}'}.operations.find { it.method == 'DELETE' }.parameters.collect { it.name }.toSet() == ['id'] as Set
        json.apis.find {it.path == '/somePath/{id}'}.operations.find { it.method == 'PATCH' }.parameters.collect { it.name }.toSet() == ['id', 'body'] as Set
    }

    void "mapped resource with hierarchical sub-resource"() {
        given:
        withUrlMappings {
            "/outerResource"(resources: "mappedWithAnnotations", includes: ['index', 'show']) {
                "/innerResource"(resources: "mapped", includes: ['index', 'show', 'delete', 'save', 'patch'])
            }
        }

        when:
        controller.params.id = 'mapped'
        controller.show()

        then:
        def json = controller.response.json
        json
        json.apiVersion == '1.0'
        json.swaggerVersion == '1.2'
        json.basePath == "http://localhost"
        json.resourcePath == "/outerResource/{mappedWithAnnotationsId}/innerResource"
        json.produces == ['application/json', 'text/xml']
        json.consumes == ['application/json', 'application/xml', 'application/x-www-form-urlencoded']
        json.apis.size() == 2
        json.apis.find {it.path == '/outerResource/{mappedWithAnnotationsId}/innerResource'}.operations.size() == 2
        json.apis.find {it.path == '/outerResource/{mappedWithAnnotationsId}/innerResource'}.operations.find { it.method == 'GET' }.parameters.collect { it.name }.toSet() == ['mappedWithAnnotationsId', 'offset', 'max', 'sort', 'order'] as Set
        json.apis.find {it.path == '/outerResource/{mappedWithAnnotationsId}/innerResource'}.operations.find { it.method == 'POST' }.parameters.collect { it.name }.toSet() == ['mappedWithAnnotationsId', 'body'] as Set
        json.apis.find {it.path == '/outerResource/{mappedWithAnnotationsId}/innerResource/{id}'}.operations.size() == 3
        json.apis.find {it.path == '/outerResource/{mappedWithAnnotationsId}/innerResource/{id}'}.operations.find { it.method == 'GET' }.parameters.collect { it.name }.toSet() == ['mappedWithAnnotationsId', 'id'] as Set
        json.apis.find {it.path == '/outerResource/{mappedWithAnnotationsId}/innerResource/{id}'}.operations.find { it.method == 'DELETE' }.parameters.collect { it.name }.toSet() == ['mappedWithAnnotationsId', 'id'] as Set
        json.apis.find {it.path == '/outerResource/{mappedWithAnnotationsId}/innerResource/{id}'}.operations.find { it.method == 'PATCH' }.parameters.collect { it.name }.toSet() == ['mappedWithAnnotationsId', 'id', 'body'] as Set
    }

    void "mapped resource with hierarchical sub-resource with annotations"() {
        given:
        withUrlMappings {
            "/outerResource"(resources: "mapped", includes: ['index', 'show']) {
                "/innerResource"(resources: "mappedWithAnnotations", includes: ['index', 'show', 'delete', 'save', 'patch'])
            }
        }

        when:
        controller.params.id = 'mappedWithAnnotations'
        controller.show()

        then:
        def json = controller.response.json
        json
        json.apiVersion == '1.0'
        json.swaggerVersion == '1.2'
        json.basePath == "http://localhost"
        json.resourcePath == "/outerResource/{mappedId}/innerResource"
        json.produces == ['application/json', 'text/xml']
        json.consumes == ['application/json', 'application/xml', 'application/x-www-form-urlencoded']
        json.apis.size() == 2
        json.apis.find {it.path == '/outerResource/{mappedId}/innerResource'}.operations.size() == 2
        json.apis.find {it.path == '/outerResource/{mappedId}/innerResource'}.operations.find { it.method == 'GET' }.parameters.collect { it.name }.toSet() == ['mappedId', 'offset', 'max', 'sort', 'order'] as Set
        json.apis.find {it.path == '/outerResource/{mappedId}/innerResource'}.operations.find { it.method == 'POST' }.parameters.collect { it.name }.toSet() == ['mappedId', 'body'] as Set
        json.apis.find {it.path == '/outerResource/{mappedId}/innerResource/{id}'}.operations.size() == 4
        json.apis.find {it.path == '/outerResource/{mappedId}/innerResource/{id}'}.operations.find { it.method == 'GET' }.parameters.collect { it.name }.toSet() == ['mappedId', 'id'] as Set
        json.apis.find {it.path == '/outerResource/{mappedId}/innerResource/{id}'}.operations.find { it.method == 'DELETE' }.parameters.collect { it.name }.toSet() == ['mappedId', 'id'] as Set
        json.apis.find {it.path == '/outerResource/{mappedId}/innerResource/{id}'}.operations.find { it.method == 'PATCH' }.parameters.collect { it.name }.toSet() == ['mappedId', 'id', 'body'] as Set
        json.apis.find {it.path == '/outerResource/{mappedId}/innerResource/{id}'}.operations.find { it.method == 'PUT' }.parameters.collect { it.name }.toSet() == ['mappedId', 'id', 'body'] as Set
    }

    void "mapped resource with deeply nested hierarchical sub-resource"() {
        given:
        withUrlMappings {
            "/domain"(resources: "domain", includes: ['index', 'show']) {
                "/insideDomain"(resources: "subdomain", includes: ['index', 'show']) {
                    "/nestingdoll"(resources: "mappedWithAnnotations", includes: ['index', 'show']) {
                        "/innermostResource"(resources: "mapped", includes: ['index', 'show', 'delete', 'save', 'patch'])
                    }
                }
            }
        }

        when:
        controller.params.id = 'mapped'
        controller.show()

        then:
        def json = controller.response.json
        json
        json.apiVersion == '1.0'
        json.swaggerVersion == '1.2'
        json.basePath == "http://localhost"
        json.resourcePath == "/domain/{domainId}/insideDomain/{subdomainId}/nestingdoll/{mappedWithAnnotationsId}/innermostResource"
        json.produces == ['application/json', 'text/xml']
        json.consumes == ['application/json', 'application/xml', 'application/x-www-form-urlencoded']
        json.apis.size() == 2
        json.apis.find {it.path == '/domain/{domainId}/insideDomain/{subdomainId}/nestingdoll/{mappedWithAnnotationsId}/innermostResource'}.operations.size() == 2
        json.apis.find {it.path == '/domain/{domainId}/insideDomain/{subdomainId}/nestingdoll/{mappedWithAnnotationsId}/innermostResource'}.operations.find { it.method == 'GET' }.parameters.collect { it.name }.toSet() == ['domainId', 'subdomainId', 'mappedWithAnnotationsId', 'offset', 'max', 'sort', 'order'] as Set
        json.apis.find {it.path == '/domain/{domainId}/insideDomain/{subdomainId}/nestingdoll/{mappedWithAnnotationsId}/innermostResource'}.operations.find { it.method == 'POST' }.parameters.collect { it.name }.toSet() == ['domainId', 'subdomainId', 'mappedWithAnnotationsId', 'body'] as Set
        json.apis.find {it.path == '/domain/{domainId}/insideDomain/{subdomainId}/nestingdoll/{mappedWithAnnotationsId}/innermostResource/{id}'}.operations.size() == 3
        json.apis.find {it.path == '/domain/{domainId}/insideDomain/{subdomainId}/nestingdoll/{mappedWithAnnotationsId}/innermostResource/{id}'}.operations.find { it.method == 'GET' }.parameters.collect { it.name }.toSet() == ['domainId', 'subdomainId', 'mappedWithAnnotationsId', 'id'] as Set
        json.apis.find {it.path == '/domain/{domainId}/insideDomain/{subdomainId}/nestingdoll/{mappedWithAnnotationsId}/innermostResource/{id}'}.operations.find { it.method == 'DELETE' }.parameters.collect { it.name }.toSet() == ['domainId', 'subdomainId', 'mappedWithAnnotationsId', 'id'] as Set
        json.apis.find {it.path == '/domain/{domainId}/insideDomain/{subdomainId}/nestingdoll/{mappedWithAnnotationsId}/innermostResource/{id}'}.operations.find { it.method == 'PATCH' }.parameters.collect { it.name }.toSet() == ['domainId', 'subdomainId', 'mappedWithAnnotationsId', 'id', 'body'] as Set
    }

    void "alternate controller endpoint"() {
        given:
        withUrlMappings {
            "/regulador/especial"(controller: "mapped", method: 'GET', action: 'especial')
        }

        when:
        controller.params.id = 'mapped'
        controller.show()

        then:
        def json = controller.response.json
        json
        json.apiVersion == '1.0'
        json.swaggerVersion == '1.2'
        json.basePath == "http://localhost"
        json.resourcePath == "/regulador/especial"
        json.produces == ['application/json', 'text/xml']
        json.consumes == ['application/json', 'application/xml', 'application/x-www-form-urlencoded']
        json.apis.size() == 1
        json.apis.find {it.path == '/regulador/especial'}.operations.size() == 1
        json.apis.find {it.path == '/regulador/especial'}.operations.find { it.method == 'GET' }.parameters.size() == 0
    }

    void "mapped resource and alternate controller endpoint"() {
        given:
        withUrlMappings {
            "/regulador"(resources: "mapped", includes: ['index', 'show', 'delete', 'save'])
            "/regulador/especial"(controller: "mapped", method: 'GET', action: 'especial')
        }

        when:
        controller.params.id = 'mapped'
        controller.show()

        then:
        def json = controller.response.json
        json
        json.apiVersion == '1.0'
        json.swaggerVersion == '1.2'
        json.basePath == "http://localhost"
        json.resourcePath == "/regulador"
        json.produces == ['application/json', 'text/xml']
        json.consumes == ['application/json', 'application/xml', 'application/x-www-form-urlencoded']
        json.apis.size() == 3
        json.apis.find {it.path == '/regulador'}.operations.size() == 2
        json.apis.find {it.path == '/regulador'}.operations.find { it.method == 'GET' }.parameters.collect { it.name }.toSet() == ['offset', 'max', 'sort', 'order'] as Set
        json.apis.find {it.path == '/regulador'}.operations.find { it.method == 'POST' }.parameters.collect { it.name }.toSet() == ['body'] as Set
        json.apis.find {it.path == '/regulador/{id}'}.operations.size() == 2
        json.apis.find {it.path == '/regulador/{id}'}.operations.find { it.method == 'GET' }.parameters.collect { it.name }.toSet() == ['id'] as Set
        json.apis.find {it.path == '/regulador/{id}'}.operations.find { it.method == 'DELETE' }.parameters.collect { it.name }.toSet() == ['id'] as Set
        json.apis.find {it.path == '/regulador/especial'}.operations.size() == 1
        json.apis.find {it.path == '/regulador/especial'}.operations.find { it.method == 'GET' }.parameters.size() == 0
    }

    void "mapped resource and alternate controller endpoint with annotated method"() {
        given:
        withUrlMappings {
            "/regulador"(resources: "mappedWithAnnotatedSpecial", includes: ['index', 'show', 'delete', 'save'])
            "/regulador/especial"(controller: "mappedWithAnnotatedSpecial", method: 'GET', action: 'especial')
        }

        when:
        controller.params.id = 'mappedWithAnnotatedSpecial'
        controller.show()

        then:
        def json = controller.response.json
        json
        json.apiVersion == '1.0'
        json.swaggerVersion == '1.2'
        json.basePath == "http://localhost"
        json.resourcePath == "/regulador"
        json.produces == ['application/json', 'text/xml', 'application/x-gleeborp']
        json.consumes == ['application/json', 'application/xml', 'application/x-www-form-urlencoded']
        json.apis.size() == 3
        json.apis.find {it.path == '/regulador'}.operations.size() == 2
        json.apis.find {it.path == '/regulador'}.operations.find { it.method == 'GET' }.parameters.collect { it.name }.toSet() == ['offset', 'max', 'sort', 'order'] as Set
        json.apis.find {it.path == '/regulador'}.operations.find { it.method == 'POST' }.parameters.collect { it.name }.toSet() == ['body'] as Set
        json.apis.find {it.path == '/regulador/{id}'}.operations.size() == 2
        json.apis.find {it.path == '/regulador/{id}'}.operations.find { it.method == 'GET' }.parameters.collect { it.name }.toSet() == ['id'] as Set
        json.apis.find {it.path == '/regulador/{id}'}.operations.find { it.method == 'DELETE' }.parameters.collect { it.name }.toSet() == ['id'] as Set
        json.apis.find {it.path == '/regulador/especial'}.operations.size() == 1
        json.apis.find {it.path == '/regulador/especial'}.operations.find { it.method == 'GET' }.parameters.collect { it.name }.toSet() == ['dingbat'] as Set
    }

    void "mapped resource with only one target (no IDs) does not include IDs in parameters"() {
        given:
        withUrlMappings {
            "/justOne"(resource: "mapped", includes: ['show'])
        }

        when:
        controller.params.id = 'mapped'
        controller.show()

        then:
        def json = controller.response.json
        json
        json.apiVersion == '1.0'
        json.swaggerVersion == '1.2'
        json.basePath == "http://localhost"
        json.resourcePath == "/justOne"
        json.produces == ['application/json', 'text/xml']
        json.consumes == ['application/json', 'application/xml', 'application/x-www-form-urlencoded']
        json.apis.size() == 1
        json.apis.find {it.path == '/justOne'}.operations.size() == 1
        json.apis.find {it.path == '/justOne'}.operations.find { it.method == 'GET' }.parameters.size() == 0
    }}
