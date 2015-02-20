package com.github.rahulsom.swaggydoc.test

import grails.rest.RestfulController

class MarshalledController extends RestfulController {

    static responseFormats = ['json', 'xml']

    MarshalledController() {
        super(Marshalled)
    }
}
