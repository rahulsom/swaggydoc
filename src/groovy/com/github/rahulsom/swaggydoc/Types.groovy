package com.github.rahulsom.swaggydoc

import groovy.transform.TupleConstructor

/* Item classes */

class Item {}

class RefItem extends Item {
    String $ref

    RefItem(String $ref) {
        this.$ref = $ref
    }
}

class TypeItem extends Item {
    String type
    String format

    TypeItem(String type, String format) {
        this.type = type
        this.format = format
    }
}

/* Field classes */

class Field {
    String description
}

class BooleanField extends Field {
    def getType() { 'boolean' }
}

class StringField extends Field {

    def getType() { 'string' }
    String format
    String[] _enum
    String defaultValue

    StringField(String[] _enum) {
        this._enum = _enum
    }

    StringField(String format) {
        this.format = format
    }

    StringField() {}
}

@TupleConstructor(includeSuperFields = true)
class NumberField extends Field {
    String type
    String format
    Integer minimum
    Integer maximum
    Integer defaultValue

    NumberField(String type, String format) {
        this.type = type
        this.format = format
    }
}

class ContainerField extends Field {
    def getType() { 'array' }
    Boolean uniqueItems
    Item items

    ContainerField(Item items) {
        this.items = items
    }

    ContainerField(Boolean uniqueItems, Item items) {
        this.uniqueItems = uniqueItems
        this.items = items
    }
}

class RefField extends Field {
    String $ref

    RefField(String $ref) {
        this.$ref = $ref
    }
}
