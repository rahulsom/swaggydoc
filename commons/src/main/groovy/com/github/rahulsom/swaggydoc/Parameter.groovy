package com.github.rahulsom.swaggydoc

import com.wordnik.swagger.annotations.ApiImplicitParam
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.util.logging.Log4j
import org.codehaus.groovy.antlr.EnumHelper

/**
 * A parameter that could be serialized in swagger json spec
 *
 * @author Rahul
 */
@CompileStatic
@EqualsAndHashCode(includes = ['name'])
@ToString(includePackage = false, includes = ['name', 'paramType', 'required'])
@Log4j
class Parameter {
    String name
    String description
    String paramType
    String type
    boolean required
    String defaultValue
    String[] _enum
    boolean allowMultiple
    String format

    Parameter(String name, String description, String paramType, String type, boolean required = false) {
        this.name = name
        this.description = description
        this.paramType = paramType
        this.type = type
        this.required = required
        if (type == 'int') {
            this.type = 'integer'
            format = 'int64'
        }
    }

    Parameter(ApiImplicitParam param, Set<Class> classes = []) {
        this(param.name(), param.value(), param.paramType()?:'query', param.dataType() ?: 'string', param.required())
        defaultValue = param.defaultValue()
        if (param.allowableValues() && !param.allowableValues().isEmpty()) {
            if (param.allowableValues()[0] == '[' && param.allowableValues()[-1] == ']') {
                _enum = param.allowableValues()[1..-2].split(/,(?=([^\"]*\"[^\"]*\")*[^\"]*$)/)*.trim().collect { String value ->
                    (value[0] == value[-1] && (value[0] == '"' || value[0] == "'")) ? value[1..-2] : value
                }
            } else {
                Parameter.log.error("Bad value for allowable values: '${param.allowableValues()}'")
            }
        } else if (classes.find{Class clazz -> clazz.simpleName == param.dataType()}) {
            def clazz = classes.find{Class clazz -> clazz.simpleName == param.dataType()}
            if (clazz.isEnum()) {
                this.type = 'string'
                _enum = clazz.enumConstants
            } else {
                Parameter.log.error("Non enum class ${param.dataType()} sent as param - ${param.name()}")
            }
        }
        allowMultiple = param.allowMultiple()
    }
}
