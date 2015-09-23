package com.github.rahulsom.swaggydoc.test

class Marshalled {

    static constraints = {
        firstName(nullable: false)
        lastName(nullable: false)
        prefix(nullable: true)
        suffix(nullable: true)
    }

    static marshalling = {
        ignore 'sortOrder'
        virtual {
            fullName { value,json-> json.value(value.generateRawAdbUrl()) }
        }
    }

    String firstName
    String lastName
    String prefix
    String suffix
    Integer sortOrder
}
