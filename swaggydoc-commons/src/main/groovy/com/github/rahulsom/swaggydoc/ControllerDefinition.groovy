package com.github.rahulsom.swaggydoc

/**
 * Represents a Resource which can have multiple APIs.
 *
 * @author Rahul Somasunderam
 */
class ControllerDefinition {
    String apiVersion
    String swaggerVersion
    String basePath
    String resourcePath
    String[] produces
    String[] consumes
    MethodDocumentation[] apis
    Map models
}
