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
@Api(
        value = 'demo'/*,
        description = 'Demo API',
        position = 0,
        produces = 'application/json,application/xml,text/html',
        consumes = 'application/json,application/xml,application/x-www-form-urlencoded' */
)
class DomainController extends RestfulController {

    static responseFormats = ['json', 'xml']
    static allowedMethods = [
            delete: ['POST', 'DELETE'],
            update: ['POST', 'PUT'],
            patch: ['POST', 'PATCH'],
    ]

    DomainController() {
        super(Domain)
    }

    @Override/*
    @ApiOperation(value = 'List demos', response = Demo, responseContainer = 'list')
    @ApiImplicitParams([
            @ApiImplicitParam(name = 'offset', value = 'Records to skip', defaultValue = '0', paramType = 'query', dataType = 'int'),
            @ApiImplicitParam(name = 'max', value = 'Max records to return', defaultValue = '10', paramType = 'query', dataType = 'int'),
            @ApiImplicitParam(name = 'sort', value = 'Field to sort by', defaultValue = 'id', paramType = 'query', dataType = 'string'),
            @ApiImplicitParam(name = 'order', value = 'Order to sort by', defaultValue = 'asc', paramType = 'query', dataType = 'string'),
            @ApiImplicitParam(name = 'q', value = 'Query', paramType = 'query', dataType = 'string'),
    ])*/
    @SwaggyList
    def index() {
        super.index()
    }

    @Override/*
    @ApiOperation(value = "Show Demo", response = Demo)
    @ApiResponses([
            @ApiResponse(code = 400, message = 'Bad Id provided'),
            @ApiResponse(code = 404, message = 'Could not find Demo with that Id'),
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name = 'id', value = 'Id to fetch', paramType = 'path', dataType = 'int', required = true),
    ])*/@SwaggyShow
    def show() {
        super.show()
    }

    /*@ApiOperation(value = "Save Demo", response = Demo)
    @ApiResponses([
            @ApiResponse(code = 422, message = 'Bad Entity Received'),
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name = 'body', paramType = 'body', required = true, dataType = 'Demo'),
    ])*/@SwaggySave
    @Override
    def save() {
        super.save()
    }

    @Override/*
    @ApiOperation(value = "Update Demo", response = Demo)
    @ApiResponses([
            @ApiResponse(code = 400, message = 'Bad Id provided'),
            @ApiResponse(code = 404, message = 'Could not find Demo with that Id'),
            @ApiResponse(code = 422, message = 'Bad Entity Received'),
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name = 'id', value = 'Id to update', paramType = 'path', dataType = 'int', required = true),
            @ApiImplicitParam(name = 'body', paramType = 'body', required = true, dataType = 'Demo')
    ])*/@SwaggyUpdate
    def update() {
        super.update()
    }

    @Override/*
    @ApiOperation(value = "Delete Demo")
    @ApiResponses([
            @ApiResponse(code = 400, message = 'Bad Id provided'),
            @ApiResponse(code = 404, message = 'Could not find Demo with that Id'),
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name = 'id', value = 'Id to delete', paramType = 'path', dataType = 'int', required = true),
    ])*/@SwaggyDelete
    def delete() {
        super.delete()
    }

    @Override/*
    @ApiOperation(value = "Patch Demo", response = Demo)
    @ApiResponses([
            @ApiResponse(code = 400, message = 'Bad Id provided'),
            @ApiResponse(code = 404, message = 'Could not find Demo with that Id'),
            @ApiResponse(code = 422, message = 'Bad Entity Received'),
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name = 'id', value = 'Id to patch', paramType = 'path', dataType = 'int', required = true),
            @ApiImplicitParam(name = 'body', paramType = 'body', required = true, dataType = 'Demo')
    ])*/@SwaggyPatch
    Object patch() {
        return super.patch()
    }
}
