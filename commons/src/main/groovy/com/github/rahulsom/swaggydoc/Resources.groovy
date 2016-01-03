package com.github.rahulsom.swaggydoc

import groovy.transform.TupleConstructor

/**
 * Represents the top level resource listing for an application
 *
 * @author Rahul Somasunderam
 */
@TupleConstructor
class Resources {
    String apiVersion
    String swaggerVersion
    ApiInfo info
    ApiDeclaration[] apis
}
