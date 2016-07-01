package com.github.rahulsom.swaggydoc

import groovy.transform.Immutable

/**
 * Represents the top level resource listing for an application
 *
 * @author Rahul Somasunderam
 */
@Immutable(knownImmutableClasses = [ApiInfo, ApiDeclaration])
class Resources {
    String apiVersion
    String swaggerVersion
    ApiInfo info
    ApiDeclaration[] apis
}
