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
            def newJson = removeUnderscores(
                    new JsonSlurper().parseText(
                            (controllerDefinition as JSON).toString()
                    ) as Map
            )

            newJson.apis.each { api ->
                api.operations.each {op ->
                    op.responseMessages.each {rm ->
                        if (rm.responseModel == 'void') {
                            rm.remove('responseModel')
                        }
                    }
                }
            }

            //// START hacky workaround
            // Fix for awful rendering bug in Grails 2.4.4.
            // Keeping the code here as to not muddy up the service code
            // N.B., this bug is FIXED in Grails 2.5.1. See https://github.com/grails/grails-core/issues/615
            mapListToArrays(newJson)
            // Now fix model.required (broken above by call "controllerDefinition as JSON")
            controllerDefinition.models.each { key, model ->
                newJson.models[key].required = model.required?.toArray()
            }
            //// END hacky workaround (remove and uncomment next line after Grails bug is fixed)

            render newJson as JSON
        }
    }

    //// START hacky workaround
    // Fix for awful rendering bug in Grails 2.4.4.
    // Keeping the code here as to not muddy up the service code
    // N.B., this bug is FIXED in Grails 2.5.1. See https://github.com/grails/grails-core/issues/615
    private void mapListToArrays(Map map) {
        List listKeys = []
        map.each { key, entry ->
            if (entry instanceof Map) {
                mapListToArrays(entry)
            } else if (entry instanceof Collection) {
                listKeys.push(key)
                listListToArrays(entry)
            }
        }
        listKeys.each { key ->
            map[key] = map[key].toArray()
        }
    }
    private void listListToArrays(Collection col) {
        def idxs = []
        col.eachWithIndex { entry, idx ->
            if (entry instanceof Map) {
                mapListToArrays(entry)
            } else if (entry instanceof Collection) {
                idxs.push(idx)
                listListToArrays(entry)
            }
        }
        idxs.each { idx ->
            col[idx] = col[idx].toArray()
        }
    }
    //// END hacky workaround (remove and uncomment next line after Grails bug is fixed)

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
