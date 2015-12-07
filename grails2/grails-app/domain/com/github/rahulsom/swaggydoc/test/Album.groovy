package com.github.rahulsom.swaggydoc.test

class Album {

    static constraints = {
    }

    static hasMany = [
            photos: Photo
    ]
}
