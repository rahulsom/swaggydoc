package com.github.rahulsom.swaggydoc

import grails.converters.JSON
import groovy.json.JsonSlurper

class ApiController {

    def swaggyDataService

    def index() { /* Render GSP as HTML */ }

    def images() { response.sendRedirect(g.resource(dir: 'images', file: 'throbber.gif').toString()) }

    /**
     * Renders the Swagger Resources.
     * @return
     */
    def resources() { render swaggyDataService.resources() as JSON }


    def show(String id) {
        header 'Access-Control-Allow-Origin', '*'
        //// START hacky workaround
        // Fix for awful rendering bug in Grails 2.4.4.
        // Keeping the code here as to not muddy up the service code
        ControllerDefinition response = swaggyDataService.apiDetails(id)
        response.models.values().each { model ->
            model.required = model.required?.toArray()
        }

        def newJson = removeUnderscores(
                new JsonSlurper().parseText(
                        (response as JSON).toString()
                ) as Map
        )
        render newJson as JSON
        //// END hacky workaround (remove and uncomment next line after Grails bug is fixed)
    }

    private Map removeUnderscores (Map<String,Object> map) {
        map.collectEntries {String k, Object v ->
            def k1 = k == '_enum' ? 'enum' : k
            def v1 = v instanceof Map ? removeUnderscores(v) : v
            [k1, v1]
        }
    }

}
