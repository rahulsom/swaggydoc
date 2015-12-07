package com.github.rahulsom.swaggydoc.test

import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiImplicitParam
import com.wordnik.swagger.annotations.ApiImplicitParams
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiResponses
import grails.transaction.Transactional

@Transactional(readOnly = true)
@Api(
        value = 'pogo',
        description = 'Pogo API',
        position = 0,
        produces = 'application/json,application/xml,text/html',
        consumes = 'application/json,application/xml,application/x-www-form-urlencoded'
)
class PogoController {

    static allowedMethods = [
            save: 'POST'
    ]

    @ApiOperation(value = "Save Demo", response = Pogo, produces = "text/csv, application/json, application/xml")
    @ApiResponses([
            @ApiResponse(code = 422, message = 'Bad Entity Received'),
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name = 'body', paramType = 'body', required = true, dataType = 'Pogo'),
    ])
    def save() {

    }
}
