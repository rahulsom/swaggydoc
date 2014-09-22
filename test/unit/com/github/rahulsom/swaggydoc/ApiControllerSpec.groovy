package com.github.rahulsom.swaggydoc

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(ApiController)
class ApiControllerSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "test something"() {
        when:
        controller.resources()

        then:
        controller.response.text.contains "Found 2 results"
    }
}
