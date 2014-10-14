package com.github.rahulsom.swaggydoc.test

class Domain {

    String name
    String description

    static hasMany = [subdomains: Subdomain]
    static constraints = {
        description nullable: true
    }
}
