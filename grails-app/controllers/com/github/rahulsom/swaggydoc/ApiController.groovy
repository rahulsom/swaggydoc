package com.github.rahulsom.swaggydoc

import grails.converters.JSON

class ApiController {

    def swaggyDataService

    def index() { /* Render GSP as HTML */ }

    def images() {
        response.sendRedirect(g.resource(dir: 'images', file: 'throbber.gif').toString())
    }

    /**
     * Renders the Swagger Resources.
     * @return
     */
    def resources() {
        //// START hacky workaround
        // Fix for awful rendering bug in Grails 2.4.4.
        // Keeping the code here as to not muddy up the service code
        Map response = swaggyDataService.resources()
        response.apis = response.apis?.toArray()
        render response as JSON
        //// END hacky workaround (remove and uncomment next line after Grails bug is fixed)
//        render(swaggyDataService.resources() as JSON)
    }


    def show(String id) {
        header 'Access-Control-Allow-Origin', '*'
        //// START hacky workaround
        // Fix for awful rendering bug in Grails 2.4.4.
        // Keeping the code here as to not muddy up the service code
        Map response = swaggyDataService.apiDetails(id)
        response.apis = response.apis?.collect { api ->
            api.operations = api.operations.collect { op ->
                op.parameters = op.parameters?.toArray()
                op.responseMessages = op.responseMessages?.toArray()
                op
            }.toArray()
            api
        }?.toArray()
        response.models.values().each { model ->
            model.required = model.required?.toArray()
        }
        render response as JSON
        //// END hacky workaround (remove and uncomment next line after Grails bug is fixed)
//        render(swaggyDataService.apiDetails(params.id) as JSON)
    }


}
