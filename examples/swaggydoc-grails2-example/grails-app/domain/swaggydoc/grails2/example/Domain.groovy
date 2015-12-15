package swaggydoc.grails2.example

class Domain {
    String name

    static hasMany = [subdomains: Subdomain]
    static constraints = {
    }
}
