package com.github.rahulsom.swaggydoc

/**
 * Created by rahul on 4/3/15.
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
}
