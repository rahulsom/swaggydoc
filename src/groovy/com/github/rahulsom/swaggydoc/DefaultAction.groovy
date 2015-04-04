package com.github.rahulsom.swaggydoc

import groovy.transform.Canonical

/**
 * Represents a default action from grails conventions
 *
 * @author Rahul
 */
@Canonical
class DefaultAction {
    Class swaggyAnnotation
    ArrayList<Parameter> parameters
    ArrayList<ResponseMessage> responseMessages
}
