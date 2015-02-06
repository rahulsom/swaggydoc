package com.github.rahulsom.swaggydoc

import grails.converters.JSON

class ApiController {

    def swaggyDataService

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
        render(swaggyDataService.resources() as JSON)
    }


    def show() {
        header 'Access-Control-Allow-Origin', '*'
        render(swaggyDataService.apiDetails(params.id) as JSON)
    }


}
