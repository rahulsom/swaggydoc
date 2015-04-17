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

class Field {}

class BooleanField extends Field {
    def getType() { 'boolean' }
}

class StringField extends Field {

    def getType() { 'string' }
    String format
    String[] _enum
    String defaultValue

    StringField(String format, String[] _enum, String defaultValue) {
        this.format = format
        this._enum = _enum
        this.defaultValue = defaultValue
    }

    StringField(String[] _enum) {
        this._enum = _enum
    }

    StringField(String format) {
        this.format = format
    }

    StringField() {}
}

class NumberField extends Field {
    def getType() { 'number' }
    String format
    Integer defaultValue

    NumberField(String format) {
        this.format = format
    }

    NumberField(String format, Integer defaultValue) {
        this.format = format
        this.defaultValue = defaultValue
    }
}

@TupleConstructor(includeSuperFields = true)
class IntegerField extends Field {
    def getType() { 'integer' }
    String format
    Integer minimum
    Integer maximum
    Integer defaultValue

    IntegerField(String format, Integer minimum, Integer maximum, Integer defaultValue) {
        this.format = format
        this.minimum = minimum
        this.maximum = maximum
        this.defaultValue = defaultValue
    }

    IntegerField(String format, Integer defaultValue) {
        this.format = format
        this.defaultValue = defaultValue
    }

    IntegerField(String format, Integer minimum, Integer maximum) {
        this.format = format
        this.minimum = minimum
        this.maximum = maximum
    }

    IntegerField(String format) {
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
