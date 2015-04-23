package com.github.rahulsom.swaggydoc.test

import com.wordnik.swagger.annotations.Api
import grails.rest.RestfulController

@Api("album")
class AlbumController extends RestfulController<Album> {

    @Override
    Object show() {
        return super.show()
    }

    @Override
    Object index(Integer max) {
        return super.index(max)
    }

    @Override
    Object save() {
        return super.save()
    }

    @Override
    Object edit() {
        return super.edit()
    }

    @Override
    Object patch() {
        return super.patch()
    }

    @Override
    Object update() {
        return super.update()
    }

    @Override
    Object delete() {
        return super.delete()
    }
}
