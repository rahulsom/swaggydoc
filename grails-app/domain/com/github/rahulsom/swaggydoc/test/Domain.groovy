package com.github.rahulsom.swaggydoc.test

class Domain {

    String name
    String description

    List subdomainsWithoutGenerics
    List<Subdomain> subdomainsWithGenerics

    static hasMany = [
            subdomainsWithoutGenerics: Subdomain,
            subdomainsWithGenerics   : Subdomain,
            implicitSubdomains       : Subdomain,
    ]
    static constraints = {
        description nullable: true
    }
}
