package com.github.rahulsom.swaggydoc

/**
 * Represents the top level resource listing for an application
 *
 * @author Rahul Somasunderam
 */
class Resources {
    String apiVersion
    String swaggerVersion
    ApiInfo info
    ApiDeclaration[] apis
}
