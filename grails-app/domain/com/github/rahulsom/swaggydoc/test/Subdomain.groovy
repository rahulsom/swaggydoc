package com.github.rahulsom.swaggydoc.test

class Subdomain {

    String name
    static belongsTo = [domain: Domain]

    static constraints = {
    }
}
