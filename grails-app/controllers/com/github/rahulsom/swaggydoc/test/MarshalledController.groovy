package com.github.rahulsom.swaggydoc.test

import com.wordnik.swagger.annotations.Api
import grails.rest.RestfulController

@Api(value = "/marshalled")
class MarshalledController extends RestfulController {

    static responseFormats = ['json', 'xml']

    MarshalledController() {
        super(Marshalled)
    }
}
