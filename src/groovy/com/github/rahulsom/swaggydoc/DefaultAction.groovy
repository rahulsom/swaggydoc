package com.github.rahulsom.swaggydoc

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.ToString
import groovy.transform.TupleConstructor

/**
 * Represents a default action from grails conventions
 *
 * @author Rahul
 */
@TupleConstructor
@ToString(includePackage = false)
@CompileStatic
class DefaultAction {
    Class swaggyAnnotation
    ArrayList<Parameter> parameters
    ArrayList<ResponseMessage> responseMessages
}
