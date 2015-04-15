package com.github.rahulsom.swaggydoc

import groovy.transform.TupleConstructor

/**
 * Created by rahul on 4/14/15.
 */
@TupleConstructor
class Pair<L,R> {
    L left
    R right
}
