package com.github.rahulsom.swaggydoc.test

import com.wordnik.swagger.annotations.Api
import grails.rest.RestfulController

@Api(value = 'mappedAsResource')
class MappedController extends RestfulController {

    static responseFormats = ['json', 'xml']

    MappedController() {
        super(Domain)
    }
}
