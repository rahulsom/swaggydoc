package com.github.rahulsom.swaggydoc

import com.wordnik.swagger.annotations.ApiImplicitParam
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * A parameter that could be serialized in swagger json spec
 *
 * @author Rahul
 */
@CompileStatic
@EqualsAndHashCode(includes = ['name'])
@ToString(includePackage = false, includes = ['name', 'paramType', 'required'])
class Parameter {
    String name
    String description
    String paramType
    String type
    boolean required
    String defaultValue

    Parameter(String name, String description, String paramType, String type, boolean required = false) {
        this.name = name
        this.description = description
        this.paramType = paramType
        this.type = type
        this.required = required
    }

    Parameter(ApiImplicitParam param) {
        this(param.name(), param.value(), param.paramType(), param.dataType() ?: 'string', param.required())
        defaultValue = param.defaultValue()
    }
}
