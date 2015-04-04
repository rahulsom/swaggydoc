package com.github.rahulsom.swaggydoc

import com.wordnik.swagger.annotations.ApiImplicitParam

/**
 * A parameter that could be serialized in swagger json spec
 *
 * @author Rahul
 */
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

    Parameter(ApiImplicitParam apiParam) {
        this(apiParam.name(), apiParam.value(), apiParam.paramType(),
                apiParam.dataType() ?: (apiParam.paramType() == 'body' ? 'demo' : 'string'),
                apiParam.required())
        defaultValue = apiParam.defaultValue()
    }
}
