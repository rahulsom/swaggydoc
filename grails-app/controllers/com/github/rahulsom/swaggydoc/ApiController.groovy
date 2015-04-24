package com.github.rahulsom.swaggydoc

import grails.converters.JSON
import groovy.json.JsonSlurper

import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletResponse

class ApiController {

    def swaggyDataService

    def index() { /* Render GSP as HTML */ }

    def images() { response.sendRedirect(g.resource(dir: 'images', file: 'throbber.gif').toString()) }

    /**
     * Renders the Swagger Resources.
     * @return
     */
    def resources() {
        log.info "Presenting resource listing"
        render swaggyDataService.resources() as JSON
    }


    def show(String id) {
        header 'Access-Control-Allow-Origin', '*'
        log.info "Presenting definition for $id"
        ControllerDefinition controllerDefinition = swaggyDataService.apiDetails(id)
        if (!controllerDefinition) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
        } else {
            //// START hacky workaround
            // Fix for awful rendering bug in Grails 2.4.4.
            // Keeping the code here as to not muddy up the service code
            controllerDefinition.models.values().each { model ->
                model.required = model.required?.toArray()
            }
            //// END hacky workaround (remove and uncomment next line after Grails bug is fixed)

            def newJson = removeUnderscores(
                    new JsonSlurper().parseText(
                            (controllerDefinition as JSON).toString()
                    ) as Map
            )
            render newJson as JSON
        }
    }

    private Map removeUnderscores (Map<String,Object> map) {
        map.collectEntries {String k, Object v ->
            def k1 = k == '_enum' ? 'enum' : k
            def v1
            if (v instanceof Map) {
                v1 = removeUnderscores(v)
            } else if (v instanceof List) {
                v1 = v.collect { v2 ->
                    v2 instanceof Map ? removeUnderscores(v2): v2
                }
            } else {
                v1 = v
            }
            [k1, v1]
        }.findAll {String k, Object v ->
            k != 'enum' || k == 'enum' && v instanceof List && v.size() > 0
        }
    }

}
