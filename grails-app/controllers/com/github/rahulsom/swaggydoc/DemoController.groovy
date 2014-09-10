package com.github.rahulsom.swaggydoc

import grails.rest.RestfulController
import grails.transaction.Transactional
import com.wordnik.swagger.annotations.*

@Transactional(readOnly = true)
@Api(
        value='demo',
        description = 'Demo API',
        position = 0,
        produces = 'application/json,application/xml,text/html',
        consumes = 'application/json,application/xml'
)
class DemoController extends RestfulController<Demo> {

    static responseFormats = ['json', 'xml']

    DemoController() {
        super(Demo)
    }

    @Override
    @ApiOperation(value = 'list demos')
    @ApiImplicitParams([
            @ApiImplicitParam(name = 'offset', value = 'Records to skip', defaultValue = '0', paramType = 'query', dataType = 'int'),
            @ApiImplicitParam(name = 'max', value = 'Max records to return', defaultValue = '10', paramType = 'query', dataType = 'int'),
            @ApiImplicitParam(name = 'sort', value = 'Field to sort by', defaultValue = 'id', paramType = 'query', dataType = 'string'),
            @ApiImplicitParam(name = 'order', value = 'Order to sort by', defaultValue = 'asc', paramType = 'query', dataType = 'string'),
            @ApiImplicitParam(name = 'q', value = 'Query', paramType = 'query', dataType = 'string'),
    ])
    def index() {
        super.index()
    }

    @Override
    @ApiOperation(value = "Show Demo")
    @ApiResponses([
            @ApiResponse(code = 400, message = 'Bad Id provided'),
            @ApiResponse(code = 404, message = 'Could not find Demo with that Id'),
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name = 'id', value = 'Id to fetch', paramType = 'path', dataType = 'int', required = true),
    ])
    def show() {
        super.show()
    }

    @ApiOperation(value = "Save Demo")
    @ApiImplicitParams([
            @ApiImplicitParam(name='body', paramType = 'body', required = true),
    ])
    @Override
    def save() {
        super.save()
    }

    @Override
    @ApiOperation(value = "Update Demo")
    @ApiResponses([
            @ApiResponse(code = 400, message = 'Bad Id provided'),
            @ApiResponse(code = 404, message = 'Could not find Demo with that Id'),
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name = 'id', value = 'Id to fetch', paramType = 'path', dataType = 'int', required = true),
            @ApiImplicitParam(name='body', paramType = 'body', required = true)
    ])
    def update() {
        super.update()
    }

    @Override
    @ApiOperation(value = "Delete Demo")
    @ApiResponses([
            @ApiResponse(code = 400, message = 'Bad Id provided'),
            @ApiResponse(code = 404, message = 'Could not find Demo with that Id'),
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name = 'id', value = 'Id to fetch', paramType = 'path', dataType = 'int', required = true),
    ])
    def delete() {
        super.delete()
    }

    @Override
    @ApiOperation(value = "Patch Demo")
    @ApiResponses([
            @ApiResponse(code = 400, message = 'Bad Id provided'),
            @ApiResponse(code = 404, message = 'Could not find Demo with that Id'),
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name = 'id', value = 'Id to fetch', paramType = 'path', dataType = 'int', required = true),
            @ApiImplicitParam(name='body', paramType = 'body', required = true)
    ])
    Object patch() {
        return super.patch()
    }
}
