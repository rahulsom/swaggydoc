package com.github.rahulsom.swaggydoc

import com.github.rahulsom.swaggydoc.test.Marshalled
import com.github.rahulsom.swaggydoc.test.Subdomain
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Specification

@TestFor(SwaggyDataService)
@Mock(Marshalled)
class SwaggyDataServiceSpec extends Specification {

    def "if domain is null, getConstraintsInformation returns an empty map"(){
        given:
        def domain = null
        def propertyName = "prefix"

        when:
        def m = service.getConstraintsInformation(domain, propertyName)

        then:
        [:] == m
    }

    def "if propertyName is null, getConstraintsInformation returns an empty map"(){
        given:
        def domain = new Marshalled()
        def propertyName = null

        when:
        def m = service.getConstraintsInformation(domain, propertyName)

        then:
        [:] == m
    }

    def "getConstraintsInformation returns nullable: false for Marshalled#firstName"(){
        given:
        def domain = new Marshalled()
        def propertyName = "firstName"

        when:
        def m = service.getConstraintsInformation(domain, propertyName)

        then:
        [nullable: false] == m
    }

    def "getConstraintsInformation returns nullable: true for Marshalled#prefix"(){
        given:
        def domain = new Marshalled()
        def propertyName = "prefix"

        when:
        def m = service.getConstraintsInformation(domain, propertyName)

        then:
        [nullable: true] == m
    }

    def "getConstraintsInformation returns an empty map for Subdomain#name"(){
        given:
        def domain = new Subdomain()
        def propertyName = "name"

        when:
        def m = service.getConstraintsInformation(domain, propertyName)

        then:
        [:] == m
    }
}
