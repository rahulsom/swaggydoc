package com.github.rahulsom.swaggydoc.test

import com.github.rahulsom.swaggydoc.SwaggyDelete
import com.github.rahulsom.swaggydoc.SwaggyList
import com.github.rahulsom.swaggydoc.SwaggyPatch
import com.github.rahulsom.swaggydoc.SwaggySave
import com.github.rahulsom.swaggydoc.SwaggyShow
import com.github.rahulsom.swaggydoc.SwaggyUpdate
import com.wordnik.swagger.annotations.*
import grails.rest.RestfulController
import grails.transaction.Transactional

@Transactional(readOnly = true)
@Api(value = 'demo without matching domain')
class DomainlessController extends RestfulController {

    static responseFormats = ['json', 'xml']

    DomainlessController() {
        super(Domain)
    }

    @Override
    @SwaggyList(searchParam = false)
    def index() {
        super.index()
    }

    @Override
    @SwaggyShow
    def show() {
        super.show()
    }

    @SwaggySave
    @Override
    def save() {
        super.save()
    }

    @Override
    @SwaggyUpdate
    def update() {
        super.update()
    }

    @Override
    @SwaggyDelete
    def delete() {
        super.delete()
    }

    @Override
    @SwaggyPatch
    Object patch() {
        return super.patch()
    }
}
