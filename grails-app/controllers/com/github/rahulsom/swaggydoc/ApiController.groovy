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
        Resources response = swaggyDataService.resources()
        render response as JSON
    }


    def show(String id) {
        header 'Access-Control-Allow-Origin', '*'
        //// START hacky workaround
        // Fix for awful rendering bug in Grails 2.4.4.
        // Keeping the code here as to not muddy up the service code
        ControllerDefinition response = swaggyDataService.apiDetails(id)
        response.models.values().each { model ->
            model.required = model.required?.toArray()
        }
        render response as JSON
        //// END hacky workaround (remove and uncomment next line after Grails bug is fixed)
//        render(swaggyDataService.apiDetails(params.id) as JSON)
    }


}
