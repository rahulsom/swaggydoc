package swaggydoc.grails2.example

class Subdomain {

    String name
    static belongsTo = [domain: Domain]
    static constraints = {
    }
}
