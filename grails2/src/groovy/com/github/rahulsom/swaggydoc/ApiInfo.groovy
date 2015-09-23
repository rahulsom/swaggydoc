package com.github.rahulsom.swaggydoc

import groovy.transform.CompileStatic

/**
 * Represents the informational section of an application
 *
 * @author Rahul Somasunderam
 */
@CompileStatic
class ApiInfo {
    String contact
    String description
    String license
    String licenseUrl
    String termsOfServiceUrl
    String title

    ApiInfo(def contact, def description, def license, def licenseUrl, def termsOfServiceUrl, def title) {
        this.contact  = ifString contact
        this.description  = ifString description
        this.license  = ifString license
        this.licenseUrl  = ifString licenseUrl
        this.termsOfServiceUrl  = ifString termsOfServiceUrl
        this.title  = ifString title
    }
    
    static ifString(def input) {
        input instanceof String ? input : null
    }
}
