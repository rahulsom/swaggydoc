package com.github.rahulsom.swaggydoc

import groovy.transform.CompileStatic
import groovy.transform.ToString
import groovy.transform.TupleConstructor

/**
 * Represents a path that could have multiple operations
 */
@CompileStatic
@TupleConstructor
@ToString(includePackage = false)
class MethodDocumentation {
    String path
    List<Operation> operations
}
