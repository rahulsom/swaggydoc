package com.github.rahulsom.swaggydoc

import spock.lang.Specification

class OperationSpec extends Specification {

    void "When the operator left shift is used, the fields with value should be copied"() {

        given: "An operation as a source"
        Operation operationSource = new Operation()

        and: "An operation target"
        Operation operationTarget = new Operation()

        when: "Only the source has value"
        operationSource."$field" = sourceValue
        operationTarget << operationSource

        then: "The value is copied to the target"
        operationTarget."$field" == operationSource."$field"
        operationTarget."$field" == sourceValue

        when: "The source has value and the target has value"
        operationSource."$field" = sourceValue
        operationTarget."$field" = targetValue
        operationTarget << operationSource

        then: "The value in the target is overridden"
        operationTarget."$field" == operationSource."$field"
        operationTarget."$field" == sourceValue

        when: "The source is null or empty"
        operationSource."$field" = null
        operationTarget."$field" = targetValue
        operationTarget << operationSource

        then: "The value in the target is kept"
        operationTarget."$field" != operationSource."$field"
        operationTarget."$field" == targetValue

        where:
        field              | sourceValue                                                                | targetValue
        "method"           | "methodTestSource"                                                         | "methodTestTarget"
        "summary"          | "summaryTestSource"                                                        | "summaryTestTarget"
        "notes"            | "notesTestSource"                                                          | "notesTestTarget"
        "nickname"         | "nicknameTestSource"                                                       | "nicknameTestTarget"
        "parameters"       | [new Parameter('nameTestSource', 'descriptionTestSource', 'query', 'int')] | [new Parameter('nameTestTarget', 'descriptionTestTarget', 'query', 'int')]
        "type"             | "typeTestSource"                                                           | "typeTestTarget"
        "items"            | new TypeItem("typeTestSource", "formatTestSource")                         | new TypeItem("typeTestTarget", "formatTestTarget")
        "responseMessages" | [new ResponseMessage(1, "messageTestSource", "responseModelTestSource")]   | [new ResponseMessage(1, "messageTestTarget", "responseModelTestTarget")]
        "produces"         | ["producesTestSource"]                                                     | ["producesTestTarget"]
        "consumes"         | ["consumesTestSource"]                                                     | ["consumesTestTarget"]

    }
}
