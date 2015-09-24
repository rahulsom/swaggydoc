package com.github.rahulsom.swaggydoc

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * Represents an operation on a path
 *
 * @author Rahul
 */
@CompileStatic
@ToString(includes = ['method'], includePackage = false)
@EqualsAndHashCode(includes = ['method', 'parameters'])
class Operation {
    String method
    String summary
    String notes
    String nickname
    Parameter[] parameters
    String type
    Item items
    ResponseMessage[] responseMessages = []
    String[] produces
    String[] consumes

    Operation leftShift(Operation operation) {

        if (operation.method) this.method = operation.method
        if (operation.summary) this.summary = operation.summary
        if (operation.notes) this.notes = operation.notes
        if (operation.nickname) this.nickname = operation.nickname
        if (operation.parameters) this.parameters = operation.parameters
        if (operation.type) this.type = operation.type
        if (operation.items) this.items = operation.items
        if (operation.responseMessages) this.responseMessages = operation.responseMessages
        if (operation.produces) this.produces = operation.produces
        if (operation.consumes) this.consumes = operation.consumes

        this
    }
}
